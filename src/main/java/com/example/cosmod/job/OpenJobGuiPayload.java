package com.example.cosmod.job;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenJobGuiPayload() implements CustomPacketPayload {
    public static final Type<OpenJobGuiPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "open_job_gui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenJobGuiPayload> CODEC =
        StreamCodec.unit(new OpenJobGuiPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
