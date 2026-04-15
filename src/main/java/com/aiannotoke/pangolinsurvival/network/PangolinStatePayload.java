package com.aiannotoke.pangolinsurvival.network;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PangolinStatePayload(
        boolean enabled,
        String mode,
        String phase,
        float stamina,
        int maxStamina,
        int cooldownTicks,
        String helmetTier,
        float digProgressPercent,
        boolean spectatorMotionEnabled,
        boolean movementNoclipActive,
        boolean burrowMediumRequired
) implements CustomPayload {

    public static final Id<PangolinStatePayload> ID =
            new Id<>(Identifier.of(PangolinSurvivalMod.MOD_ID, "state"));
    public static final PacketCodec<RegistryByteBuf, PangolinStatePayload> CODEC =
            PacketCodec.of(PangolinStatePayload::write, PangolinStatePayload::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(RegistryByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeString(mode);
        buf.writeString(phase);
        buf.writeFloat(stamina);
        buf.writeInt(maxStamina);
        buf.writeInt(cooldownTicks);
        buf.writeString(helmetTier);
        buf.writeFloat(digProgressPercent);
        buf.writeBoolean(spectatorMotionEnabled);
        buf.writeBoolean(movementNoclipActive);
        buf.writeBoolean(burrowMediumRequired);
    }

    public static PangolinStatePayload read(RegistryByteBuf buf) {
        return new PangolinStatePayload(
                buf.readBoolean(),
                buf.readString(),
                buf.readString(),
                buf.readFloat(),
                buf.readInt(),
                buf.readInt(),
                buf.readString(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }
}
