package com.aiannotoke.pangolinsurvival.network;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpgradeStaminaPayload() implements CustomPayload {

    public static final Id<UpgradeStaminaPayload> ID =
            new Id<>(Identifier.of(PangolinSurvivalMod.MOD_ID, "upgrade_stamina"));
    public static final PacketCodec<RegistryByteBuf, UpgradeStaminaPayload> CODEC =
            PacketCodec.of((payload, buf) -> {
            }, buf -> new UpgradeStaminaPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
