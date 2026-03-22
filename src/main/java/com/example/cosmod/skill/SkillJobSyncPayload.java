package com.example.cosmod.skill;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 서버 → 클라이언트: 직업 변경 시 스킬 HUD 업데이트 */
public record SkillJobSyncPayload(String combatJob, int jobLevel) implements CustomPacketPayload {
    public static final Type<SkillJobSyncPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "skill_job_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillJobSyncPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SkillJobSyncPayload::combatJob,
            ByteBufCodecs.INT,         SkillJobSyncPayload::jobLevel,
            SkillJobSyncPayload::new
        );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
