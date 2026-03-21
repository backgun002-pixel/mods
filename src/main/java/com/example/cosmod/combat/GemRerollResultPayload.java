package com.example.cosmod.combat;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record GemRerollResultPayload(ItemStack gem, int slotIdx) implements CustomPacketPayload {
    public static final Type<GemRerollResultPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "gem_reroll_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GemRerollResultPayload> CODEC =
        StreamCodec.composite(
            ItemStack.STREAM_CODEC, GemRerollResultPayload::gem,
            ByteBufCodecs.INT,      GemRerollResultPayload::slotIdx,
            GemRerollResultPayload::new
        );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
