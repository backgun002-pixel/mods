package com.example.cosmod.network;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.world.item.ItemStack;

public record CosmeticUpdatePayload(int slotIndex, ItemStack stack)
        implements CustomPacketPayload {

    public static final Type<CosmeticUpdatePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "cosmetic_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticUpdatePayload> CODEC =
        StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT,
            CosmeticUpdatePayload::slotIndex,
            ItemStack.OPTIONAL_STREAM_CODEC,  // 빈 스택 허용
            CosmeticUpdatePayload::stack,
            CosmeticUpdatePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
