package com.example.cosmod.weapon;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 서버 → 클라이언트: 스킬 쿨타임 바 업데이트
 * skillName, maxTicks, remainingTicks 전송
 */
public record WeaponCooldownPayload(String skillName, int maxTicks, int remainingTicks)
    implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WeaponCooldownPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("cosmod", "weapon_cooldown"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponCooldownPayload> CODEC =
        StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.skillName());
                buf.writeInt(p.maxTicks());
                buf.writeInt(p.remainingTicks());
            },
            buf -> new WeaponCooldownPayload(buf.readUtf(), buf.readInt(), buf.readInt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
    }

    public static void send(ServerPlayer player, String skillName, int maxTicks, int remainingTicks) {
        ServerPlayNetworking.send(player, new WeaponCooldownPayload(skillName, maxTicks, remainingTicks));
    }
}
