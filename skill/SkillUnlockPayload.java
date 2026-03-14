package com.example.cosmod.skill;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 서버 → 클라이언트: Lv15 스킬 해금 연출 */
public record SkillUnlockPayload(String skillName) implements CustomPacketPayload {

    public static final Type<SkillUnlockPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "skill_unlock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillUnlockPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SkillUnlockPayload::skillName,
            SkillUnlockPayload::new
        );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
