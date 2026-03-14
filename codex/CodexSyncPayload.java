package com.example.cosmod.codex;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

/** 서버→클라이언트: 도감 등록 목록 동기화 */
public record CodexSyncPayload(Set<String> farmerIds, Set<String> minerIds)
        implements CustomPacketPayload {

    public static final Type<CodexSyncPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "codex_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CodexSyncPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8),
            CodexSyncPayload::farmerIds,
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8),
            CodexSyncPayload::minerIds,
            CodexSyncPayload::new
        );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
