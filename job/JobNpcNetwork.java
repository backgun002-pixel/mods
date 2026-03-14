package com.example.cosmod.job;

import com.example.cosmod.economy.CosmodEconomy;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.example.cosmod.skill.SkillJobSyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class JobNpcNetwork {

    private static final int JOB_COST = 200;

    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenJobGuiPayload.TYPE, OpenJobGuiPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectJobPayload.TYPE, SelectJobPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SelectJobPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                try {
                    JobClass job = JobClass.valueOf(payload.jobName());
                    PlayerJobData data = PlayerJobManager.get(player);

                    // ── 이미 같은 직업이면 차단 ───────────────────────
                    if (data.hasJob(job)) {
                        player.displayClientMessage(
                            Component.literal("§c이미 선택한 직업입니다."), false);
                        return;
                    }

                    // ── 코인 확인 ─────────────────────────────────────
                    int coins = CosmodEconomy.getCoins(player);
                    if (coins < JOB_COST) {
                        player.displayClientMessage(
                            Component.literal("§c코인이 부족합니다! §e(필요: "
                                + JOB_COST + "코인 / 보유: " + coins + "코인)"), false);
                        return;
                    }

                    // ── 기존 같은 카테고리 직업이 있으면 초기화 ──────
                    boolean isChange =
                        (job.category == JobClass.JobCategory.LIFE   && data.getLifeJob()   != null)
                     || (job.category == JobClass.JobCategory.COMBAT && data.getCombatJob() != null);

                    if (isChange) {
                        JobClass old = job.category == JobClass.JobCategory.LIFE
                            ? data.getLifeJob() : data.getCombatJob();
                        data.resetJob(job.category); // 레벨·EXP 초기화
                        player.displayClientMessage(
                            Component.literal("§c[" + old.displayName
                                + "] 직업의 경험치가 초기화되었습니다."), false);
                    }

                    // ── 코인 차감 + 직업 설정 ─────────────────────────
                    CosmodEconomy.takeCoins(player, JOB_COST);
                    data.setJob(job);
                    PlayerJobManager.save(player);

                    // 클라이언트에 스킬/EXP HUD 동기화
                    if (job.category == JobClass.JobCategory.COMBAT) {
                        ServerPlayNetworking.send(player, new com.example.cosmod.skill.SkillJobSyncPayload(
                            job.name(), data.getLevel(job)));
                        // EXP바도 즉시 동기화
                        PlayerJobManager.syncExpHud(player, false);
                    }

                    String msg = isChange
                        ? "§e직업을 §6" + job.displayName + "§e(으)로 변경했습니다! §7(-" + JOB_COST + "코인)"
                        : "§a직업 §6" + job.displayName + "§a을(를) 선택했습니다! §7(-" + JOB_COST + "코인)";
                    player.displayClientMessage(Component.literal(msg), false);
                    player.displayClientMessage(
                        Component.literal("§e잔여 코인: §f" + CosmodEconomy.getCoins(player) + "코인"), false);

                } catch (IllegalArgumentException e) {
                    player.displayClientMessage(
                        Component.literal("§c올바르지 않은 직업입니다."), false);
                }
            });
        });
    }

    public static void sendOpenJobGui(ServerPlayer player) {
        ServerPlayNetworking.send(player, new OpenJobGuiPayload());
    }
}
