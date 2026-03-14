package com.example.cosmod.combat;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenEnhancePayload() implements CustomPacketPayload {

    public static final Type<OpenEnhancePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "open_enhance_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEnhancePayload> CODEC =
        StreamCodec.unit(new OpenEnhancePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
