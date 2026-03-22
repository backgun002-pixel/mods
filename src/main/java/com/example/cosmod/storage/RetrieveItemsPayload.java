package com.example.cosmod.storage;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 클라이언트→서버: 아이템 회수 요청 */
public record RetrieveItemsPayload() implements CustomPacketPayload {

    public static final Type<RetrieveItemsPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "retrieve_items"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RetrieveItemsPayload> CODEC =
        StreamCodec.unit(new RetrieveItemsPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
