package com.aiannotoke.pangolinsurvival.mixin;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("HEAD"), cancellable = true)
    private void pangolinsurvival$overrideBreakingSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        if (PangolinSurvivalMod.getService() == null) {
            return;
        }

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (PangolinSurvivalMod.getService().shouldOverrideMining(player)) {
            cir.setReturnValue(PangolinSurvivalMod.getService().getCustomBreakingSpeed(player, state));
        }
    }

    @Inject(method = "canHarvest", at = @At("HEAD"), cancellable = true)
    private void pangolinsurvival$overrideHarvestCheck(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (PangolinSurvivalMod.getService() == null) {
            return;
        }

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (PangolinSurvivalMod.getService().shouldOverrideMining(player)) {
            cir.setReturnValue(PangolinSurvivalMod.getService().canObservedHarvest(player, state));
        }
    }

    @Inject(method = "canFoodHeal", at = @At("HEAD"), cancellable = true)
    private void pangolinsurvival$disableNaturalHealing(CallbackInfoReturnable<Boolean> cir) {
        if (PangolinSurvivalMod.getService() == null) {
            return;
        }

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (PangolinSurvivalMod.getService().shouldDisableNaturalHealing(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void pangolinsurvival$applyBurrowAirTick(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (PangolinSurvivalMod.getService() == null) {
            return;
        }

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getEntityWorld().isClient()) {
            PangolinSurvivalMod.getService().tickPlayerBurrowAir(player);
        }
    }
}
