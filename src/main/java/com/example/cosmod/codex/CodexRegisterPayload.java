package com.example.cosmod.codex;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 클라이언트→서버: 아이템 도감 등록 요청 */
public record CodexRegisterPayload(String tab, String id)
        implements CustomPacketPayload {

    public static final Type<CodexRegisterPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "codex_register"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CodexRegisterPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CodexRegisterPayload::tab,
            ByteBufCodecs.STRING_UTF8, CodexRegisterPayload::id,
            CodexRegisterPayload::new
        );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
