package com.example.cosmod.job;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric Data Attachment API로 플레이어 직업 데이터 영속 저장
 * 마크 플레이어 .dat 파일에 자동으로 같이 저장됨
 */
public class CosmodAttachments {

    // AttachmentType: PlayerJobData를 CompoundTag로 직렬화해서 저장
    public static final AttachmentType<CompoundTag> JOB_DATA =
        AttachmentRegistry.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC)
            .buildAndRegister(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("cosmod", "job_data"));

    /** 플레이어에서 직업 데이터 로드 (Attachment → PlayerJobData 캐시) */
    public static void loadToCache(ServerPlayer player) {
        CompoundTag tag = player.getAttachedOrElse(JOB_DATA, null);
        if (tag == null) return;
        PlayerJobData data = PlayerJobManager.getOrCreate(player.getUUID());
        data.fromNbt(tag);
    }

    /** 캐시에서 Attachment로 저장 (자동으로 .dat에 포함됨) */
    public static void saveFromCache(ServerPlayer player) {
        PlayerJobData data = PlayerJobManager.getFromCache(player.getUUID());
        if (data == null) return;
        player.setAttached(JOB_DATA, data.toNbt());
    }
}
