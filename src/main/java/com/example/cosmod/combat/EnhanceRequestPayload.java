package com.example.cosmod.combat;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EnhanceRequestPayload(int gearSlotIdx, int stoneSlotIdx, int stoneType)
        implements CustomPacketPayload {

    public static final Type<EnhanceRequestPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "enhance_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnhanceRequestPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, EnhanceRequestPayload::gearSlotIdx,
            ByteBufCodecs.INT, EnhanceRequestPayload::stoneSlotIdx,
            ByteBufCodecs.INT, EnhanceRequestPayload::stoneType,
            EnhanceRequestPayload::new
        );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
