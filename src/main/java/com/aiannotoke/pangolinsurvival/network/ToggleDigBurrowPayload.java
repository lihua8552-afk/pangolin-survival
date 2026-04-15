package com.aiannotoke.pangolinsurvival.network;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ToggleDigBurrowPayload() implements CustomPayload {

    public static final Id<ToggleDigBurrowPayload> ID =
            new Id<>(Identifier.of(PangolinSurvivalMod.MOD_ID, "toggle_dig"));
    public static final PacketCodec<RegistryByteBuf, ToggleDigBurrowPayload> CODEC =
            PacketCodec.of((payload, buf) -> {
            }, buf -> new ToggleDigBurrowPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
