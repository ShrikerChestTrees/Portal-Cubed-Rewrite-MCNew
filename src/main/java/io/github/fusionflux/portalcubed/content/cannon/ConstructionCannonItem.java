package io.github.fusionflux.portalcubed.content.cannon;

import io.github.fusionflux.portalcubed.framework.construct.ConfiguredConstruct;
import io.github.fusionflux.portalcubed.framework.construct.set.ConstructSet;
import io.github.fusionflux.portalcubed.framework.extension.CustomHoldPoseItem;
import io.github.fusionflux.portalcubed.framework.construct.ConstructManager;
import io.github.fusionflux.portalcubed.framework.construct.ConstructPlacementContext;
import io.github.fusionflux.portalcubed.content.cannon.data.CannonSettings;
import io.github.fusionflux.portalcubed.framework.item.TagTranslation;
import io.github.fusionflux.portalcubed.packet.PortalCubedPackets;
import io.github.fusionflux.portalcubed.packet.clientbound.OpenCannonConfigPacket;
import io.github.fusionflux.portalcubed.packet.clientbound.ShootCannonPacket;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.List;
import java.util.Objects;

public class ConstructionCannonItem extends Item implements @ClientOnly CustomHoldPoseItem {
	public static final String MATERIAL_TOOLTIP_KEY = "item.portalcubed.construction_cannon.material";
	public static final String CONSTRUCT_TOOLTIP_KEY = "item.portalcubed.construction_cannon.construct_set";
	public static final int PARTICLES = 10;

	public ConstructionCannonItem(Properties settings) {
		super(settings);
	}

	@Override
	@NotNull
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack held = user.getItemInHand(hand);
		if (user.isSecondaryUseActive()) {
			if (user instanceof ServerPlayer serverPlayer) {
				PortalCubedPackets.sendToClient(serverPlayer, new OpenCannonConfigPacket(hand));
			}
			return InteractionResultHolder.success(held);
		}

		// do nothing
		return InteractionResultHolder.pass(held);
	}

	@Override
	@NotNull
	public InteractionResult useOn(UseOnContext context) {
		if (context.isSecondaryUseActive())
			return InteractionResult.PASS; // fall back to use

		Player player = context.getPlayer();
		// player is required for material consumption
		if (player == null)
			return InteractionResult.PASS;

		CannonUseResult result = this.tryPlace(context);

		if (player instanceof ServerPlayer serverPlayer) {
			PortalCubedPackets.sendToClient(serverPlayer, new ShootCannonPacket(context.getHand(), result));
		}

		result.sound().ifPresent(player::playSound);

		if (result == CannonUseResult.PLACED) {
			// kaboom
			player.playSound(SoundEvents.GENERIC_EXPLODE, 0.4f, player.getRandom().nextIntBetweenInclusive(120, 270) / 100f);
			if (context.getLevel() instanceof ServerLevel level) {
				Vec3 source = getParticleSource(player);
				level.sendParticles(
						new DustParticleOptions(Vec3.fromRGB24(0xFFFFFF).toVector3f(), 1),
						source.x, source.y, source.z,
						PARTICLES,
						0.1, 0.1, 0.1,
						0.1
				);
			}
			return InteractionResult.CONSUME;
		}

		return InteractionResult.FAIL;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		CannonSettings settings = getCannonSettings(stack);
		if (settings == null)
			return;

		if (settings.material().isPresent()) {
			Component name = TagTranslation.translate(settings.material().get());
			tooltip.add(Component.translatable(MATERIAL_TOOLTIP_KEY, name));
		}
		if (settings.construct().isPresent()) {
			Component name = ConstructSet.getName(settings.construct().get());
			tooltip.add(Component.translatable(CONSTRUCT_TOOLTIP_KEY, name));
		}
	}

	@ClientOnly
	@Override
	public ArmPose getHoldPose(ItemStack stack) {
		return ArmPose.CROSSBOW_HOLD;
	}

	protected CannonUseResult tryPlace(UseOnContext ctx) {
		ItemStack stack = ctx.getItemInHand();
		CannonSettings settings = getCannonSettings(stack);
		if (settings == null) // invalid state
			return CannonUseResult.INVALID;

		CannonSettings.Configured configured = settings.validate();
		if (configured == null) // not configured
			return CannonUseResult.NOT_CONFIGURED;

		ConstructSet constructSet = ConstructManager.INSTANCE.getConstructSet(configured.construct());
		if (constructSet == null) // fake construct
			return CannonUseResult.INVALID;

		ConfiguredConstruct construct = constructSet.choose(ConstructPlacementContext.of(ctx));

		BlockPos clicked = new BlockPlaceContext(ctx).getClickedPos();

		BoundingBox bounds = construct.getAbsoluteBounds(clicked);
		if (!this.mayBuild(ctx, bounds))
			return CannonUseResult.NO_PERMS;

		if (construct.isObstructed(ctx.getLevel(), clicked))
			return CannonUseResult.OBSTRUCTED;

		Player player = Objects.requireNonNull(ctx.getPlayer()); // null is checked on use
		if (!this.consumeMaterials(player, constructSet.material, constructSet.cost))
			return CannonUseResult.MISSING_MATERIALS;

		if (ctx.getLevel() instanceof ServerLevel level) {
			construct.place(level, clicked, player, stack);
		}

		return CannonUseResult.PLACED;
	}

	protected boolean mayBuild(UseOnContext ctx, BoundingBox box) {
		Player player = ctx.getPlayer();
		if (player == null)
			return true;

		Level level = ctx.getLevel();
		return BlockPos.betweenClosedStream(box).allMatch(
				pos -> player.mayInteract(level, pos)
		);
	}

	@SuppressWarnings("deprecation")
	protected boolean consumeMaterials(Player player, TagKey<Item> tag, int count) {
		// creative players always have enough.
		if (player.isCreative())
			return true;

		try (Transaction t = Transaction.openOuter()) {
			PlayerInventoryStorage storage = PlayerInventoryStorage.of(player);
			for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
				ItemVariant variant = view.getResource();
				//noinspection deprecation - builtInRegistryHolder
				if (variant.getItem().builtInRegistryHolder().is(tag)) {
					// matches
					long extract = Math.min(count, view.getAmount());
					long extracted = view.extract(variant, extract, t);
					count -= (int) extracted;

					if (count <= 0) {
						// got enough
						t.commit();
						return true;
					}
				}
			}
		}
		// did not find enough resources.
		return false;
	}

	@Nullable
	public static CannonSettings getCannonSettings(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		if (nbt != null && nbt.contains(CannonSettings.NBT_KEY, Tag.TAG_COMPOUND)) {
			CompoundTag dataNbt = nbt.getCompound(CannonSettings.NBT_KEY);
			return CannonSettings.CODEC.parse(NbtOps.INSTANCE, dataNbt).result().orElse(null);
		}
		return null;
	}

	public static void setCannonSettings(ItemStack stack, CannonSettings settings) {
		CannonSettings.CODEC.encodeStart(NbtOps.INSTANCE, settings).result()
				.ifPresent(nbt -> stack.addTagElement(CannonSettings.NBT_KEY, nbt));
	}

	private static Vec3 getParticleSource(Player player) {
		var offset = new Vec3(-.5f, -.4f, 1f)
			.xRot(-player.getXRot() * Mth.DEG_TO_RAD)
			.yRot(-player.getYRot() * Mth.DEG_TO_RAD);
		return player.getEyePosition().add(offset);
	}
}
