package com.aiannotoke.pangolinsurvival.mixin;

import com.aiannotoke.pangolinsurvival.client.PangolinSurvivalClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void pangolinsurvival$applyBurrowFlagsBeforeMovement(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        player.noClip = PangolinSurvivalClient.isMovementNoclipActive();
        player.setNoGravity(PangolinSurvivalClient.isSpectatorMotionEnabled());
    }
}
