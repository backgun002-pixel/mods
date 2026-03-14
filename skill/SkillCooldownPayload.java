package com.example.cosmod.skill;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 서버 → 클라이언트: 스킬별 쿨타임 개별 전송 */
public record SkillCooldownPayload(int slot, int cd, int maxCd) implements CustomPacketPayload {
    public static final Type<SkillCooldownPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "skill_cooldown"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillCooldownPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, SkillCooldownPayload::slot,
            ByteBufCodecs.INT, SkillCooldownPayload::cd,
            ByteBufCodecs.INT, SkillCooldownPayload::maxCd,
            SkillCooldownPayload::new
        );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
