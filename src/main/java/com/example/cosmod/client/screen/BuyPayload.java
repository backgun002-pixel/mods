package com.example.cosmod.client.screen;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * 클라이언트 → 서버: 구매 요청 패킷
 */
public record BuyPayload(Item item, int amount) implements CustomPacketPayload {

    public static final Type<BuyPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "buy_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuyPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM), BuyPayload::item,
            ByteBufCodecs.INT, BuyPayload::amount,
            BuyPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
