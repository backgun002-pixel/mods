package com.example.cosmod.combat;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record GemRerollRequestPayload(int slotIdx) implements CustomPacketPayload {
    public static final Type<GemRerollRequestPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "gem_reroll_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GemRerollRequestPayload> CODEC =
        StreamCodec.composite(ByteBufCodecs.INT, GemRerollRequestPayload::slotIdx, GemRerollRequestPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
