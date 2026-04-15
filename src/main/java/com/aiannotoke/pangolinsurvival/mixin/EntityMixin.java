package com.aiannotoke.pangolinsurvival.mixin;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    private void pangolinsurvival$ignoreWallDamageWhileBurrowing(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player
                && PangolinSurvivalMod.getService() != null
                && PangolinSurvivalMod.getService().isBurrowing(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldTickBlockCollision", at = @At("HEAD"), cancellable = true)
    private void pangolinsurvival$skipBlockCollisionWhileBurrowing(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player
                && PangolinSurvivalMod.getService() != null
                && PangolinSurvivalMod.getService().isMovementNoclipActive(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tickBlockCollision", at = @At("HEAD"), cancellable = true)
    private void pangolinsurvival$cancelBlockCollisionTick(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player
                && PangolinSurvivalMod.getService() != null
                && PangolinSurvivalMod.getService().isMovementNoclipActive(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void pangolinsurvival$moveWithoutCollision(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player
                && PangolinSurvivalMod.getService() != null
                && PangolinSurvivalMod.getService().isMovementNoclipActive(player)) {
            if (!PangolinSurvivalMod.getService().canApplyBurrowMovement(player, movement)) {
                ci.cancel();
                return;
            }
            Entity entity = (Entity) (Object) this;
            entity.setPosition(entity.getX() + movement.x, entity.getY() + movement.y, entity.getZ() + movement.z);
            ci.cancel();
        }
    }
}
