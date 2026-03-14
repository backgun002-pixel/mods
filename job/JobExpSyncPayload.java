package com.example.cosmod.job;

import com.example.cosmod.CosmodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 서버 → 클라이언트: 직업 EXP/레벨 동기화 */
public record JobExpSyncPayload(int level, int exp, int expToNext, boolean levelUp)
        implements CustomPacketPayload {

    public static final Type<JobExpSyncPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "job_exp_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JobExpSyncPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT,     JobExpSyncPayload::level,
            ByteBufCodecs.INT,     JobExpSyncPayload::exp,
            ByteBufCodecs.INT,     JobExpSyncPayload::expToNext,
            ByteBufCodecs.BOOL,    JobExpSyncPayload::levelUp,
            JobExpSyncPayload::new
        );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
