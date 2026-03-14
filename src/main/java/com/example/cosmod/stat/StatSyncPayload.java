package com.example.cosmod.stat;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 서버 → 클라이언트: 플레이어 스탯 동기화
 * atkFlat: 고정 공격력, atkPct: 공격력% 합계
 * def: 방어력, maxHp: 최대체력, spdPct: 이동속도%
 * critChance: 치명타 확률%, critDmg: 치명타 데미지%
 * jump: 점프력, coins: 보유 코인
 * jobName: 직업명, jobLevel: 직업 레벨
 */
public record StatSyncPayload(
    float atkFlat, float atkPct,
    float def, float maxHp,
    float spdPct, float critChance, float critDmg,
    float jump, int coins,
    String jobName, int jobLevel,
    String lifeJobName
) implements CustomPacketPayload {

    public static final Type<StatSyncPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "stat_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StatSyncPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.FLOAT,        StatSyncPayload::atkFlat,
            ByteBufCodecs.FLOAT,        StatSyncPayload::atkPct,
            ByteBufCodecs.FLOAT,        StatSyncPayload::def,
            ByteBufCodecs.FLOAT,        StatSyncPayload::maxHp,
            ByteBufCodecs.FLOAT,        StatSyncPayload::spdPct,
            ByteBufCodecs.FLOAT,        StatSyncPayload::critChance,
            ByteBufCodecs.FLOAT,        StatSyncPayload::critDmg,
            ByteBufCodecs.FLOAT,        StatSyncPayload::jump,
            ByteBufCodecs.INT,          StatSyncPayload::coins,
            ByteBufCodecs.STRING_UTF8,  StatSyncPayload::jobName,
            ByteBufCodecs.INT,          StatSyncPayload::jobLevel,
            ByteBufCodecs.STRING_UTF8,  StatSyncPayload::lifeJobName,
            StatSyncPayload::new
        );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
