package com.aiannotoke.pangolinsurvival.mixin;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import com.aiannotoke.pangolinsurvival.state.BurrowPhase;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    public abstract Entity getFocusedEntity();

    @Inject(method = "getSubmersionType", at = @At("HEAD"), cancellable = true)
    private void pangolinsurvival$clearGhostBurrowSubmersion(CallbackInfoReturnable<CameraSubmersionType> cir) {
        Entity focusedEntity = getFocusedEntity();
        if (!(focusedEntity instanceof PlayerEntity player)) {
            return;
        }

        BurrowPhase phase = PangolinSurvivalMod.getClientState().phase();
        if (phase == BurrowPhase.ENTERING_GHOST || phase == BurrowPhase.GHOST_ACTIVE) {
            cir.setReturnValue(CameraSubmersionType.NONE);
        }
    }
}
