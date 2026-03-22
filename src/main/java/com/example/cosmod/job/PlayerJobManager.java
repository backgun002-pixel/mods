package com.example.cosmod.job;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.example.cosmod.codex.CodexNetwork;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.example.cosmod.skill.SkillUnlockPayload;

import java.util.*;

public class PlayerJobManager {

    private static final Map<UUID, PlayerJobData> cache = new HashMap<>();
    private static int autoSaveTick = 0;

    public static PlayerJobData get(ServerPlayer player) {
        return cache.computeIfAbsent(player.getUUID(), id -> new PlayerJobData());
    }

    public static PlayerJobData getFromCache(UUID uuid) { return cache.get(uuid); }
    public static PlayerJobData getOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> new PlayerJobData());
    }

    public static void save(ServerPlayer player) { CosmodAttachments.saveFromCache(player); }
    public static void remove(UUID id) { cache.remove(id); }

    /** 전투직업 스킬 HUD 동기화 */
    public static void syncSkillHud(ServerPlayer player) {
        PlayerJobData data = get(player);
        JobClass combat = data.getCombatJob();
        if (combat == null) return;
        ServerPlayNetworking.send(player,
            new com.example.cosmod.skill.SkillJobSyncPayload(
                combat.name(), data.getLevel(combat)));
    }

    /** 전투직업 EXP 바 동기화 */
    public static void syncExpHud(ServerPlayer player, boolean levelUp) {
        PlayerJobData data = get(player);
        JobClass combat = data.getCombatJob();
        if (combat == null) return;
        int lv  = data.getLevel(combat);
        int exp = data.getExp(combat);
        int etn = data.getExpToNext(lv);
        ServerPlayNetworking.send(player, new JobExpSyncPayload(lv, exp, etn, levelUp));
    }

    public static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CodexNetwork.sync((net.minecraft.server.level.ServerPlayer)handler.getPlayer());
            CosmodAttachments.loadToCache(handler.getPlayer());
            server.execute(() -> {
                syncSkillHud(handler.getPlayer());
                syncExpHud(handler.getPlayer(), false);
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            CosmodAttachments.saveFromCache(handler.getPlayer());
            cache.remove(handler.getPlayer().getUUID());
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            // ── 직업 데이터 복사 ──────────────────────────────────
            // 방법: Attachment를 직접 oldPlayer → newPlayer로 복사
            // (캐시 타이밍 문제를 완전히 우회)
            net.minecraft.nbt.CompoundTag jobTag = oldPlayer.getAttachedOrElse(
                CosmodAttachments.JOB_DATA, null);

            if (jobTag == null) {
                // Attachment 없으면 캐시에서 강제 저장 후 다시 시도
                CosmodAttachments.saveFromCache(oldPlayer);
                jobTag = oldPlayer.getAttachedOrElse(CosmodAttachments.JOB_DATA, null);
            }

            if (jobTag != null) {
                // newPlayer Attachment에 직접 복사
                newPlayer.setAttached(CosmodAttachments.JOB_DATA, jobTag.copy());
                // 캐시도 업데이트
                PlayerJobData newData = new PlayerJobData();
                newData.fromNbt(jobTag);
                cache.put(newPlayer.getUUID(), newData);
            } else {
                // 그래도 없으면 캐시에서라도 복사
                PlayerJobData old = cache.get(oldPlayer.getUUID());
                if (old != null) cache.put(newPlayer.getUUID(), old);
            }
            cache.remove(oldPlayer.getUUID());

            // ── 사망 보관 아이템 복사 ─────────────────────────────
            net.minecraft.nbt.CompoundTag deathTag = oldPlayer.getAttachedOrElse(
                com.example.cosmod.storage.DeathItemStorage.STORED_ITEMS, null);
            if (deathTag != null) {
                newPlayer.setAttached(com.example.cosmod.storage.DeathItemStorage.STORED_ITEMS, deathTag);
            }
        });

        // 리스폰 후 HUD 재동기화
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // 약간의 딜레이 후 동기화 (서버 틱 1회 후)
            ((net.minecraft.server.level.ServerLevel)newPlayer.level()).getServer().execute(() -> {
                CosmodAttachments.loadToCache(newPlayer);
                syncSkillHud(newPlayer);
                syncExpHud(newPlayer, false);
                CodexNetwork.sync(newPlayer);
            });
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++autoSaveTick >= 600) {
                autoSaveTick = 0;
                for (ServerPlayer p : server.getPlayerList().getPlayers())
                    CosmodAttachments.saveFromCache(p);
            }
        });
    }

    public static void giveExp(ServerPlayer player, JobClass job, int amount) {
        PlayerJobData data = get(player);
        int levelsBefore = data.getLevel(job);
        data.addExp(job, amount);
        save(player);

        int gained = data.getLevel(job) - levelsBefore;
        boolean leveledUp = gained > 0;

        // EXP 바 동기화 (매번)
        syncExpHud(player, leveledUp);

        if (leveledUp) {
            int newLevel = data.getLevel(job);
            // 레벨업 사운드
            ((net.minecraft.server.level.ServerLevel)player.level()).playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);

            player.displayClientMessage(
                Component.literal("§a[" + job.displayName + "] §e레벨 업! §7"
                    + levelsBefore + " §f→ §6" + newLevel + "레벨"), false);

            for (int bl : job.bonusLevels)
                if (bl > levelsBefore && bl <= newLevel) {
                    int idx = indexOf(job.bonusLevels, bl);
                    player.displayClientMessage(
                        Component.literal("§6[직업 보너스 해금] §f" + job.bonusDesc[idx]), false);
                }
        }
    }

    private static int indexOf(int[] arr, int val) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == val) return i;
        return 0;
    }
}
