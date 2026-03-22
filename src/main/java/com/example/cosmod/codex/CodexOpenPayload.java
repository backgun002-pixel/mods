package com.example.cosmod.codex;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CodexOpenPayload(String tab) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CodexOpenPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("cosmod", "codex_open"));

    public static final StreamCodec<FriendlyByteBuf, CodexOpenPayload> CODEC =
        StreamCodec.of(
            (buf, p) -> buf.writeUtf(p.tab()),
            buf -> new CodexOpenPayload(buf.readUtf())
        );

    @Override public CustomPacketPayload.Type<CodexOpenPayload> type() { return TYPE; }
}
