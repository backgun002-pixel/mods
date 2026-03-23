package com.example.cosmod.combat;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * 강화 요청 페이로드
 * 클라이언트가 아이템 자체를 서버로 전송 — 슬롯 인덱스 방식 폐기
 * (클라이언트에서 슬롯을 비운 뒤 인덱스를 보내면 서버가 빈 슬롯을 읽는 문제 해결)
 */
public record EnhanceRequestPayload(ItemStack gearItem, ItemStack stoneItem, int stoneType)
        implements CustomPacketPayload {

    public static final Type<EnhanceRequestPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "enhance_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnhanceRequestPayload> CODEC =
        StreamCodec.composite(
            ItemStack.STREAM_CODEC,     EnhanceRequestPayload::gearItem,
            ItemStack.STREAM_CODEC,     EnhanceRequestPayload::stoneItem,
            ByteBufCodecs.INT,          EnhanceRequestPayload::stoneType,
            EnhanceRequestPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
