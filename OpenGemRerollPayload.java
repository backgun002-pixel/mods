package com.example.cosmod.combat;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenGemRerollPayload(int slotIdx) implements CustomPacketPayload {
    public static final Type<OpenGemRerollPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "open_gem_reroll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenGemRerollPayload> CODEC =
        StreamCodec.composite(ByteBufCodecs.INT, OpenGemRerollPayload::slotIdx, OpenGemRerollPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
