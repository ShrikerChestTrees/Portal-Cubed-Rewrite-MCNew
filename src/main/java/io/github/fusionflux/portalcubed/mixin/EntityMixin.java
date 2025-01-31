package io.github.fusionflux.portalcubed.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.fusionflux.portalcubed.content.PortalCubedDamageSources;
import io.github.fusionflux.portalcubed.content.PortalCubedParticles;
import io.github.fusionflux.portalcubed.content.button.FloorButtonBlock;
import io.github.fusionflux.portalcubed.data.tags.PortalCubedEntityTags;
import io.github.fusionflux.portalcubed.framework.entity.HoldableEntity;

import io.github.fusionflux.portalcubed.framework.extension.EntityExt;

import io.github.fusionflux.portalcubed.packet.PortalCubedPackets;
import io.github.fusionflux.portalcubed.packet.clientbound.DisintegratePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import org.quiltmc.qsl.networking.api.PlayerLookup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.fusionflux.portalcubed.framework.extension.AmbientSoundEmitter;
import io.github.fusionflux.portalcubed.framework.extension.CollisionListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityExt {
	@Shadow
	@Final
	private static EntityDataAccessor<Boolean> DATA_SILENT;
	@Shadow
	public boolean horizontalCollision, verticalCollision, verticalCollisionBelow;

	@Shadow
	public abstract Level level();
	@Shadow
	public abstract SynchedEntityData getEntityData();

	@Shadow
	public abstract void discard();

	@Shadow
	public abstract void stopRiding();

	@Shadow
	public abstract void move(MoverType movementType, Vec3 movement);

	@Shadow
	public abstract Vec3 getDeltaMovement();

	@Shadow
	public abstract void setDeltaMovement(Vec3 velocity);

	@Shadow
	public abstract BlockState getFeetBlockState();

	@Shadow
	public abstract BlockPos blockPosition();

	@Shadow
	@Final
	protected RandomSource random;

	@Shadow
	public abstract float getBbWidth();

	@Shadow
	public abstract float getBbHeight();

	@Shadow
	public abstract double getX();

	@Shadow
	public abstract double getY();

	@Shadow
	public abstract double getZ();

	@Shadow
	public abstract EntityType<?> getType();

	@Shadow
	public abstract AABB getBoundingBox();

	@Shadow
	public abstract Vec3 position();

	@Unique
	private boolean isHorizontalColliding, isTopColliding, isBelowColliding;
	@Unique
	private boolean disintegrating;
	@Unique
	private int disintegrateTicks;

	@Override
	public boolean pc$disintegrate() {
		if (!this.disintegrating) {
			if (this.level() instanceof ServerLevel world && (Object) this instanceof Entity self) {
				world.gameEvent(self, GameEvent.ENTITY_DIE, this.position());

				// In portal buttons push back on the objects that are on them, disintegration makes objects lose all their mass, so they get ejected but we cant do that here so lets just apply a slight force
				BlockState feetState = this.getFeetBlockState();
				if (feetState.getBlock() instanceof FloorButtonBlock floorButton && floorButton.isEntityPressing(feetState, this.blockPosition(), self))
					this.setDeltaMovement(Vec3.atLowerCornerOf(feetState.getValue(FloorButtonBlock.FACING).getNormal()).scale(FloorButtonBlock.DISINTEGRATION_EJECTION_FORCE));

				this.disintegrateTicks = DISINTEGRATE_TICKS;
				Collection<ServerPlayer> tracking = (Object) this instanceof ServerPlayer serverPlayer ? PortalCubedPackets.trackingAndSelf(serverPlayer) : PlayerLookup.tracking(self);
				DisintegratePacket packet = new DisintegratePacket(self);
				for (ServerPlayer toUpdate : tracking) {
					PortalCubedPackets.sendToClient(toUpdate, packet);
				}
				this.stopRiding();
			}
			this.disintegrating = true;
			return true;
		}
		return false;
	}

	@Override
	public boolean pc$disintegrate(int ticks) {
		if (this.pc$disintegrate()) {
			this.disintegrateTicks = ticks;
			return true;
		}
		return false;
	}

	@Override
	public boolean pc$disintegrating() {
		return this.disintegrating;
	}

	@Override
	public int pc$disintegrateTicks() {
		return this.disintegrateTicks;
	}

	@Override
	public void pc$disintegrateTick() {
		Vec3 velocity = this.getDeltaMovement().scale(.91);
		this.move(MoverType.SELF, velocity);
		this.setDeltaMovement(velocity);

		Level world = this.level();
		if (--this.disintegrateTicks <= 0 && !world.isClientSide) {
			if ((Object) this instanceof LivingEntity livingEntity) {
				if (livingEntity.isDeadOrDying()) return;
				DamageSource damageSource = PortalCubedDamageSources.disintegration(world, livingEntity);
				livingEntity.getCombatTracker().recordDamage(damageSource, Float.MAX_VALUE);
				livingEntity.setHealth(0);
				livingEntity.die(damageSource);
			}
			if (!((Object) this instanceof Player)) this.discard();
			if ((Object) this instanceof ItemEntity itemEntity) itemEntity.getItem().onDestroyed(itemEntity);
		} else if (this.disintegrateTicks > TRANSLUCENCY_START_TICKS) {
			EntityType<?> type = this.getType();
			if (!type.is(PortalCubedEntityTags.FIZZLES_WITHOUT_DARK_PARTICLES)) { // Portal 1 props don't make ash when fizzled
				double volume = this.getBbWidth() * this.getBbWidth() * this.getBbHeight();
				for (int i = 0; i < Math.min(Math.max(Math.round(volume*15), 1), 100); i++) { // Capped to 100/tick so that fizzling something large doesn't instantly kill performance.  Use the largest of 15*volume/tick OR 1/tick, to prevent entities with small volumes (mug, item) from not making ash
					double xOffset = this.random.nextGaussian() * (this.getBbWidth() / 3);
					double yOffset = .2 + (this.random.nextGaussian() * (this.getBbHeight() / 3));
					double zOffset = this.random.nextGaussian() * (this.getBbWidth() / 3);
					double velocityX = this.random.nextGaussian();
					double velocityY = this.random.nextGaussian();
					double velocityZ = this.random.nextGaussian();
					world.addParticle(PortalCubedParticles.FIZZLE_DARK, this.getX() + xOffset, this.getY() + yOffset, this.getZ() + zOffset, velocityX, velocityY, velocityZ);
				}
			}
			// Some props don't make the bright particles, and Portal 1 props have an alternate bright particle type
			Vec3 center = this.getBoundingBox().getCenter();
			if (!type.is(PortalCubedEntityTags.FIZZLES_WITHOUT_BRIGHT_PARTICLES)) {
				if (!type.is(PortalCubedEntityTags.FIZZLES_WITH_ALTERNATE_BRIGHT_PARTICLES)) {
					for (int i = 0; i < 3; i++) {
						world.addParticle(PortalCubedParticles.FIZZLE_BRIGHT, center.x, center.y, center.z, 0, 0, 0);
					}
				} else {
					for (int i = 0; i < 3; i++) {
						double xOffset = this.random.nextGaussian() * (this.getBbWidth() / 3.8);
						double yOffset = .2 + (this.random.nextGaussian() * (this.getBbHeight() / 3.8));
						double zOffset = this.random.nextGaussian() * (this.getBbWidth() / 3.8);
						world.addParticle(PortalCubedParticles.FIZZLE_BRIGHT_ALTERNATE, this.getX() + xOffset, this.getY() + yOffset, this.getZ() + zOffset, 0, 0, 0);
					}
				}
			}
		}
	}

	@WrapOperation(method = "rideTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
	private void disintegrationTick(Entity instance, Operation<Void> original) {
		if (!this.pc$disintegrating()) {
			original.call(this);
		} else {
			this.pc$disintegrateTick();
		}
	}

	@Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V"))
	private void readDisintegrateTicks(CompoundTag tag, CallbackInfo ci) {
		this.disintegrateTicks = tag.getInt("portalcubed:disintegrate_ticks");
		this.disintegrating = this.disintegrateTicks != 0;
	}

	@Inject(method = "saveWithoutId", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V"))
	private void saveDisintegrateTicks(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
		if (this.disintegrating)
			tag.putInt("portalcubed:disintegrate_ticks", this.disintegrateTicks);
	}

	@Inject(method = "isNoGravity", at = @At("RETURN"), cancellable = true)
	private void noGravityIfDisintegrating(CallbackInfoReturnable<Boolean> cir) {
		if (this.disintegrating) cir.setReturnValue(true);
	}

	@Inject(method = "isSilent", at = @At("RETURN"), cancellable = true)
	private void silentIfDisintegrating(CallbackInfoReturnable<Boolean> cir) {
		if (this.disintegrating) cir.setReturnValue(true);
	}

	@Inject(method = "isInvulnerable", at = @At("RETURN"), cancellable = true)
	private void invulnerableIfDisintegrating(CallbackInfoReturnable<Boolean> cir) {
		if (this.disintegrating) cir.setReturnValue(true);
	}

	@Inject(method = "isAlive", at = @At("RETURN"), cancellable = true)
	private void notAliveIfDisintegrating(CallbackInfoReturnable<Boolean> cir) {
		if (this.disintegrating) cir.setReturnValue(false);
	}

	@Inject(method = "isIgnoringBlockTriggers", at = @At("RETURN"), cancellable = true)
	private void ignoreBlockTriggersIfDisintegrating(CallbackInfoReturnable<Boolean> cir) {
		if (this.disintegrating) cir.setReturnValue(true);
	}

	@Inject(method = "canRide", at = @At("RETURN"), cancellable = true)
	private void cantRideIfDisintegrating(CallbackInfoReturnable<Boolean> cir) {
		if (this.disintegrating) cir.setReturnValue(false);
	}

	@Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("RETURN"))
	private void startSoundWhenUnSilenced(EntityDataAccessor<?> data, CallbackInfo ci) {
		if (level().isClientSide && (Object) this instanceof AmbientSoundEmitter ambientSoundEmitter) {
			if (DATA_SILENT.equals(data) && !getEntityData().get(DATA_SILENT))
				ambientSoundEmitter.playAmbientSound();
		}
	}

	@Inject(
		method = "move",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
			shift = At.Shift.BEFORE
		)
	)
	private void listenForCollisions(MoverType movementType, Vec3 movement, CallbackInfo ci) {
		if (this instanceof CollisionListener collisionListener) {
			if (horizontalCollision) {
				if (!isHorizontalColliding) collisionListener.onCollision();
				isHorizontalColliding = true;
			} else {
				isHorizontalColliding = false;
			}

			if (verticalCollision) {
				if (!isTopColliding) collisionListener.onCollision();
				isTopColliding = true;
			} else {
				isTopColliding = false;
			}

			if (verticalCollisionBelow) {
				if (!isBelowColliding) collisionListener.onCollision();
				isBelowColliding = true;
			} else {
				isBelowColliding = false;
			}
		}
	}

	@Inject(method = "canCollideWith", at = @At("RETURN"), cancellable = true)
	private void dontCollideWithHeldProp(Entity other, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player self && self.getHeldEntity() == other) {
			cir.setReturnValue(false);
		} else if (this.pc$disintegrating() || other.pc$disintegrating()) {
			cir.setReturnValue(false);
		}
	}

	@SuppressWarnings({"ConstantValue", "UnreachableCode"})
	@Inject(method = "setRemoved", at = @At("HEAD"))
	private void dropHeldWhenRemoved(RemovalReason reason, CallbackInfo ci) {
		if (this.level().isClientSide)
			return;

		if ((Object) this instanceof Player player && player.getHeldEntity() != null) {
			player.getHeldEntity().drop();
		} else if ((Object) this instanceof HoldableEntity holdable && holdable.isHeld()) {
			holdable.drop();
		}
	}
}
