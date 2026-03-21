package com.example.cosmod.combat;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record EnhanceResultPayload(String message, int color, boolean isSpecial,
                                   int gearSlotIdx, ItemStack resultGear)
        implements CustomPacketPayload {

    public static final Type<EnhanceResultPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "enhance_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnhanceResultPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,  EnhanceResultPayload::message,
            ByteBufCodecs.INT,          EnhanceResultPayload::color,
            ByteBufCodecs.BOOL,         EnhanceResultPayload::isSpecial,
            ByteBufCodecs.INT,          EnhanceResultPayload::gearSlotIdx,
            ItemStack.STREAM_CODEC,     EnhanceResultPayload::resultGear,
            EnhanceResultPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
