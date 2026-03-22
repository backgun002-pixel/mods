package com.example.cosmod.entity;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 서버 → 클라이언트: 상점 GUI 오픈 신호
 * 1.21.11 CustomPacketPayload 방식
 */
public record OpenShopPayload() implements CustomPacketPayload {

    public static final Type<OpenShopPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "open_shop_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopPayload> CODEC =
        StreamCodec.unit(new OpenShopPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
