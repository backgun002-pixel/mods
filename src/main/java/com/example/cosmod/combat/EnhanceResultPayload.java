package com.example.cosmod.combat;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S→C: 강화 결과 (메시지, 색상, 특별연출) */
public record EnhanceResultPayload(String message, int color, boolean isSpecial)
        implements CustomPacketPayload {

    public static final Type<EnhanceResultPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "enhance_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnhanceResultPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EnhanceResultPayload::message,
            ByteBufCodecs.INT,         EnhanceResultPayload::color,
            ByteBufCodecs.BOOL,        EnhanceResultPayload::isSpecial,
            EnhanceResultPayload::new
        );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
