package com.aiannotoke.pangolinsurvival.mixin;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getNextAirOnLand", at = @At("HEAD"), cancellable = true)
    private void pangolinsurvival$preventLandAirRecoveryWhileBurrowing(int air, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof PlayerEntity player
                && PangolinSurvivalMod.getService() != null
                && PangolinSurvivalMod.getService().shouldPreventAirRecovery(player)) {
            cir.setReturnValue(air);
        }
    }
}
