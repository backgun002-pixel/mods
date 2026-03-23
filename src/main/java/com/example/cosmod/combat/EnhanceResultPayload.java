package com.example.cosmod.combat;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// 서버 → 클라이언트: 강화 결과 + 강화된 아이템 전달
// 클라이언트는 이 아이템을 결과 슬롯에 표시, 클릭 시 인벤토리로 회수
public record EnhanceResultPayload(String message, int color, boolean isSpecial,
                                   ItemStack resultGear)
        implements CustomPacketPayload {

    public static final Type<EnhanceResultPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "enhance_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnhanceResultPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,  EnhanceResultPayload::message,
            ByteBufCodecs.INT,          EnhanceResultPayload::color,
            ByteBufCodecs.BOOL,         EnhanceResultPayload::isSpecial,
            ItemStack.STREAM_CODEC,     EnhanceResultPayload::resultGear,
            EnhanceResultPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
