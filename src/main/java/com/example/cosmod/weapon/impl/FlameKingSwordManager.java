package com.example.cosmod.weapon.impl;

import com.example.cosmod.weapon.WeaponSkillManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlameKingSwordManager {

    // UUID → 남은 변신 틱
    private static final Map<UUID, Integer> transformTimers = new HashMap<>();

    public static final int TRANSFORM_TICKS = 600;  // 지속 30초
    public static final int COOLDOWN_TICKS  = 900;  // 쿨타임 45초
    public static final String TRANSFORM_SKILL_NAME = "폭염 형태";

    /** 변신 시작 */
    public static void startTransform(ServerPlayer player) {
        transformTimers.put(player.getUUID(), TRANSFORM_TICKS);
        setModel(player, true);
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, TRANSFORM_TICKS + 5, 1, false, false));
        player.displayClientMessage(
            Component.literal("§c§l[염왕검] §e§l폭염 형태 돌입! §7(30초)"), true);
    }

    /** 변신 강제 종료 - endWithCooldown=true면 폭염형태 쿨타임도 시작 */
    public static void endTransform(ServerPlayer player, boolean endWithCooldown) {
        transformTimers.remove(player.getUUID());
        setModel(player, false);
        player.removeEffect(MobEffects.HASTE);

        if (endWithCooldown) {
            // 폭염 형태 스킬에 쿨타임 45초 등록
            WeaponSkillManager.setCooldown(player.getUUID(), TRANSFORM_SKILL_NAME, COOLDOWN_TICKS);
            WeaponSkillManager.sendCooldown(player, TRANSFORM_SKILL_NAME, COOLDOWN_TICKS, COOLDOWN_TICKS);
            player.displayClientMessage(
                Component.literal("§c[염왕검] §7원래 형태 복귀 §8(쿨타임 45초)"), true);
        } else {
            player.displayClientMessage(
                Component.literal("§c[염왕검] §7폭염 형태 종료"), true);
        }
    }

    public static boolean isTransformed(UUID uuid) {
        return transformTimers.getOrDefault(uuid, 0) > 0;
    }

    /** CustomModelData: 0=기본(두꺼운), 1=폭염(날씬한) */
    public static void setModel(ServerPlayer player, boolean thin) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof FlameKingSword)) return;
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(
                List.of(thin ? 1.0f : 0.0f),
                List.of(),
                List.of(),
                List.of()
            ));
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            transformTimers.replaceAll((uuid, ticks) -> ticks - 1);

            transformTimers.entrySet().removeIf(entry -> {
                if (entry.getValue() <= 0) {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if (player != null) endTransform(player, true); // 시간 만료 → 쿨타임 시작
                    return true;
                }
                // 남은 5초 경고
                if (entry.getValue() == 100) {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if (player != null)
                        player.displayClientMessage(
                            Component.literal("§e[염왕검] §7폭염 형태 종료까지 §c5초"), true);
                }
                return false;
            });
        });
    }
}
