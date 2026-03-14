package com.example.cosmod.job;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.example.cosmod.entity.CosmodEntities;
import com.example.cosmod.storage.StorageNpcEntity;

/**
 * /cosmod <명령어> 통합 커맨드
 *
 * /cosmod joblevel <레벨>     → 전투직업 레벨 설정
 * /cosmod lifejoblevel <레벨> → 생활직업 레벨 설정
 * /cosmod setjob <직업명>     → 직업 강제 설정 (warrior/archer/farmer/miner)
 * /cosmod exp <양>            → 현재 전투직업 EXP 추가
 * /cosmod coins <양>          → 코인 추가 (TODO: 코인 시스템 연동 시)
 */
public class JobCheatCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                Commands.literal("cosmod")

                // /cosmod joblevel <1~100>
                .then(Commands.literal("joblevel")
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int lv = IntegerArgumentType.getInteger(ctx, "level");
                            PlayerJobData data = PlayerJobManager.get(player);
                            JobClass combat = data.getCombatJob();
                            if (combat == null) {
                                player.displayClientMessage(Component.literal("§c전투직업이 없습니다."), false);
                                return 0;
                            }
                            int prevLv = data.getLevel(combat);
                            data.setLevel(combat, lv);
                            PlayerJobManager.save(player);
                            PlayerJobManager.syncSkillHud(player);
                            if (lv > prevLv) {
                                // 레벨업: 1레벨씩 연출
                                for (int lvl = prevLv + 1; lvl <= lv; lvl++) {
                                    data.setLevel(combat, lvl);
                                    PlayerJobManager.syncExpHud(player, true);
                                }
                            } else {
                                // 레벨 다운: 연출 없이 그냥 동기화
                                data.setLevel(combat, lv);
                                PlayerJobManager.syncExpHud(player, false);
                            }
                            data.setLevel(combat, lv);
                            PlayerJobManager.save(player);
                            player.displayClientMessage(
                                Component.literal("§a[Cosmod] §f" + combat.displayName + " → §eLv" + lv), false);
                            return 1;
                        })))

                // /cosmod lifejoblevel <1~100>
                .then(Commands.literal("lifejoblevel")
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int lv = IntegerArgumentType.getInteger(ctx, "level");
                            PlayerJobData data = PlayerJobManager.get(player);
                            JobClass life = data.getLifeJob();
                            if (life == null) {
                                player.displayClientMessage(Component.literal("§c생활직업이 없습니다."), false);
                                return 0;
                            }
                            int prevLvL = data.getLevel(life);
                            data.setLevel(life, lv);
                            PlayerJobManager.save(player);
                            if (lv > prevLvL) {
                                for (int lvl = prevLvL + 1; lvl <= lv; lvl++) {
                                    data.setLevel(life, lvl);
                                    PlayerJobManager.syncExpHud(player, true);
                                }
                            } else {
                                data.setLevel(life, lv);
                                PlayerJobManager.syncExpHud(player, false);
                            }
                            data.setLevel(life, lv);
                            PlayerJobManager.save(player);
                            player.displayClientMessage(
                                Component.literal("§a[Cosmod] §f" + life.displayName + " → §eLv" + lv), false);
                            return 1;
                        })))

                // /cosmod setjob <직업명>
                .then(Commands.literal("setjob")
                    .then(Commands.argument("job", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String jobName = StringArgumentType.getString(ctx, "job").toUpperCase();
                            JobClass target = null;
                            for (JobClass j : JobClass.values()) {
                                if (j.name().equals(jobName)) { target = j; break; }
                            }
                            if (target == null) {
                                player.displayClientMessage(
                                    Component.literal("§c직업명 오류. 사용 가능: warrior, archer, farmer, miner"), false);
                                return 0;
                            }
                            PlayerJobData data = PlayerJobManager.get(player);
                            if (target.category == JobClass.JobCategory.COMBAT) data.setCombatJob(target);
                            else                                                   data.setLifeJob(target);
                            PlayerJobManager.save(player);
                            player.displayClientMessage(
                                Component.literal("§a[Cosmod] 직업 설정: §e" + target.displayName), false);
                            return 1;
                        })))

                // /cosmod exp <양>
                .then(Commands.literal("exp")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, 99999))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            PlayerJobData data = PlayerJobManager.get(player);
                            JobClass combat = data.getCombatJob();
                            if (combat == null) {
                                player.displayClientMessage(Component.literal("§c전투직업이 없습니다."), false);
                                return 0;
                            }
                            int gained = data.addExp(combat, amount);
                            player.displayClientMessage(
                                Component.literal("§a[Cosmod] EXP +" + amount
                                    + (gained > 0 ? " §e(레벨업 +" + gained + ")" : "")), false);
                            return 1;
                        })))

                // /cosmod spawnstorage
                .then(Commands.literal("spawnstorage")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        StorageNpcEntity npc = new StorageNpcEntity(CosmodEntities.STORAGE_NPC, player.level());
                        npc.setPos(player.getX() + 1, player.getY(), player.getZ());
                        player.level().addFreshEntity(npc);
                        player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§a[Cosmod] 아이템 보관소 NPC 소환!"), false);
                        return 1;
                    }))

                // /cosmod status
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        PlayerJobData data = PlayerJobManager.get(player);
                        JobClass combat = data.getCombatJob();
                        JobClass life   = data.getLifeJob();
                        player.displayClientMessage(Component.literal("§7━━━━━━━━━━━━━━"), false);
                        player.displayClientMessage(Component.literal("§e[Cosmod 직업 현황]"), false);
                        if (combat != null)
                            player.displayClientMessage(Component.literal(
                                "§b전투: §f" + combat.displayName
                                + " §eLv" + data.getLevel(combat)
                                + " §7(" + data.getExp(combat) + "/" + data.getExpToNext(data.getLevel(combat)) + " EXP)"), false);
                        else
                            player.displayClientMessage(Component.literal("§b전투: §8없음"), false);
                        if (life != null)
                            player.displayClientMessage(Component.literal(
                                "§a생활: §f" + life.displayName
                                + " §eLv" + data.getLevel(life)
                                + " §7(" + data.getExp(life) + "/" + data.getExpToNext(data.getLevel(life)) + " EXP)"), false);
                        else
                            player.displayClientMessage(Component.literal("§a생활: §8없음"), false);
                        player.displayClientMessage(Component.literal("§7━━━━━━━━━━━━━━"), false);
                        return 1;
                    }))
            );
        });
    }
}
