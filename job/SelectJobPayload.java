package com.example.cosmod.job;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectJobPayload(String jobName) implements CustomPacketPayload {
    public static final Type<SelectJobPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "select_job"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectJobPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SelectJobPayload::jobName,
            SelectJobPayload::new
        );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
