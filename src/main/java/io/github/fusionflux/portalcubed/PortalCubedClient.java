package io.github.fusionflux.portalcubed;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.entity.event.api.client.ClientEntityTickCallback;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;

import com.terraformersmc.terraform.boat.api.client.TerraformBoatClientHelper;

import io.github.fusionflux.portalcubed.content.PortalCubedEntities;
import io.github.fusionflux.portalcubed.content.PortalCubedFluids;
import io.github.fusionflux.portalcubed.content.PortalCubedItems;
import io.github.fusionflux.portalcubed.content.PortalCubedKeyMappings;
import io.github.fusionflux.portalcubed.content.PortalCubedSounds;
import io.github.fusionflux.portalcubed.content.cannon.ConstructPreviewRenderer;
import io.github.fusionflux.portalcubed.content.cannon.ConstructionCannonAnimator;
import io.github.fusionflux.portalcubed.content.misc.LemonadeItem;
import io.github.fusionflux.portalcubed.content.misc.LongFallBootsModel;
import io.github.fusionflux.portalcubed.content.misc.SourcePhysics;
import io.github.fusionflux.portalcubed.content.portal.PortalRenderer;
import io.github.fusionflux.portalcubed.content.prop.PropModelCache;
import io.github.fusionflux.portalcubed.framework.entity.FollowingSoundInstance;
import io.github.fusionflux.portalcubed.framework.model.PortalCubedModelLoadingPlugin;
import io.github.fusionflux.portalcubed.framework.model.emissive.EmissiveLoader;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.player.Player;

public class PortalCubedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient(ModContainer mod) {
		PortalRenderer.init();
		ConstructPreviewRenderer.init();
		PortalCubedKeyMappings.init();

		FluidRenderHandlerRegistry.INSTANCE.register(
				PortalCubedFluids.GOO,
				PortalCubedFluids.FLOWING_GOO,
				new SimpleFluidRenderHandler(PortalCubed.id("block/toxic_goo_still"), PortalCubed.id("block/toxic_goo_flow"))
		);

		ItemProperties.register(PortalCubedItems.LEMONADE, PortalCubed.id("armed"), (stack, level, entity, i) -> LemonadeItem.isArmed(stack) ? 1 : 0);

		LongFallBootsModel.init();
		TerraformBoatClientHelper.registerModelLayers(PortalCubedEntities.LEMON_BOAT.location(), false);
		PropModelCache.register();
		PreparableModelLoadingPlugin.register(EmissiveLoader.INSTANCE, PortalCubedModelLoadingPlugin.INSTANCE);

		HudRenderCallback.EVENT.register(SourcePhysics.DebugRenderer.INSTANCE);

		ClientTickEvents.END.register(ConstructionCannonAnimator::tick);

		ClientEntityTickCallback.EVENT.register((entity, isPassengerTick) -> {
			if (entity instanceof Player player) {
				boolean holdingPortalGun = player.getMainHandItem().is(PortalCubedItems.PORTAL_GUN);

				var soundManager = Minecraft.getInstance().getSoundManager();
				int grabSoundTimer = player.pc$grabSoundTimer();
				var holdLoopSound = (FollowingSoundInstance) player.pc$holdLoopSound();
				var grabSound = (FollowingSoundInstance) player.pc$grabSound();

				if (grabSound != null) {
					grabSoundTimer--;
					player.pc$grabSoundTimer(grabSoundTimer);

					if (!holdingPortalGun) {
						grabSound.forceStop();
						grabSound = null;
						player.pc$grabSound(grabSound);
					} else if (grabSoundTimer <= 0) {
						grabSound = null;
						player.pc$grabSound(grabSound);

						holdLoopSound = PortalCubedSounds.createPortalGunHoldLoop(player);
						soundManager.play(holdLoopSound);
						player.pc$holdLoopSound(holdLoopSound);
					}
				}

				if (holdLoopSound != null && !holdingPortalGun) {
					holdLoopSound.forceStop();
					player.pc$holdLoopSound(null);
				} else if (holdingPortalGun && (holdLoopSound == null && grabSound == null) && player.getHeldEntity() != null) {
					holdLoopSound = PortalCubedSounds.createPortalGunHoldLoop(player);
					soundManager.play(holdLoopSound);
					player.pc$holdLoopSound(holdLoopSound);
				}
			}
		});
	}
}
