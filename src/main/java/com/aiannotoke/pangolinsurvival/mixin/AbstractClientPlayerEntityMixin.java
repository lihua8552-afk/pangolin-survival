package com.aiannotoke.pangolinsurvival.mixin;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import com.aiannotoke.pangolinsurvival.state.BurrowPhase;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getFovMultiplier", at = @At("HEAD"), cancellable = true)
    private void pangolinsurvival$stabilizeGhostBurrowFov(boolean firstPerson, float fovEffectScale, CallbackInfoReturnable<Float> cir) {
        BurrowPhase phase = PangolinSurvivalMod.getClientState().phase();
        if (phase == BurrowPhase.ENTERING_GHOST || phase == BurrowPhase.GHOST_ACTIVE) {
            cir.setReturnValue(1.0F);
        }
    }
}
