package com.aiannotoke.pangolinsurvival.network;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ToggleGhostBurrowPayload() implements CustomPayload {

    public static final Id<ToggleGhostBurrowPayload> ID =
            new Id<>(Identifier.of(PangolinSurvivalMod.MOD_ID, "toggle_ghost"));
    public static final PacketCodec<RegistryByteBuf, ToggleGhostBurrowPayload> CODEC =
            PacketCodec.of((payload, buf) -> {
            }, buf -> new ToggleGhostBurrowPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
