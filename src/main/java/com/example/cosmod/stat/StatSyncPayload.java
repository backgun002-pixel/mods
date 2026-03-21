package com.example.cosmod.stat;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StatSyncPayload(
    float atkFlat, float atkPct, float def, float maxHp,
    float spdPct, float critChance, float critDmg, float jump,
    int coins, String jobName, int jobLevel, String lifeJobName
) implements CustomPacketPayload {

    public static final Type<StatSyncPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "stat_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StatSyncPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.FLOAT,  StatSyncPayload::atkFlat,
            ByteBufCodecs.FLOAT,  StatSyncPayload::atkPct,
            ByteBufCodecs.FLOAT,  StatSyncPayload::def,
            ByteBufCodecs.FLOAT,  StatSyncPayload::maxHp,
            ByteBufCodecs.FLOAT,  StatSyncPayload::spdPct,
            ByteBufCodecs.FLOAT,  StatSyncPayload::critChance,
            ByteBufCodecs.FLOAT,  StatSyncPayload::critDmg,
            ByteBufCodecs.FLOAT,  StatSyncPayload::jump,
            ByteBufCodecs.INT,    StatSyncPayload::coins,
            ByteBufCodecs.STRING_UTF8, StatSyncPayload::jobName,
            ByteBufCodecs.INT,    StatSyncPayload::jobLevel,
            ByteBufCodecs.STRING_UTF8, StatSyncPayload::lifeJobName,
            StatSyncPayload::new
        );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
