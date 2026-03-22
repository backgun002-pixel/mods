package com.example.cosmod.storage;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 서버→클라이언트: 보관소 UI 열기 (아이템 목록 + 비용 포함) */
public record OpenStoragePayload(java.util.List<net.minecraft.world.item.ItemStack> items, int cost)
        implements CustomPacketPayload {

    public static final Type<OpenStoragePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "open_storage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenStoragePayload> CODEC =
        StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.collection(
                java.util.ArrayList::new,
                net.minecraft.world.item.ItemStack.STREAM_CODEC),
            OpenStoragePayload::items,
            net.minecraft.network.codec.ByteBufCodecs.INT,
            OpenStoragePayload::cost,
            OpenStoragePayload::new
        );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
