package com.example.cosmod.stat;

import com.example.cosmod.economy.CosmodEconomy;
import com.example.cosmod.job.JobClass;
import com.example.cosmod.job.PlayerJobData;
import com.example.cosmod.job.PlayerJobManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StatSyncHandler {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(StatSyncPayload.TYPE, StatSyncPayload.CODEC);

        // 20틱(1초)마다 스탯 동기화
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 60 != 0) return; // SetBonusHandler에서 5틱마다 호출하므로 여기선 3초 주기 폴백만
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sync(player);
            }
        });
    }

    public static void sync(ServerPlayer player) {
        PlayerJobData data = PlayerJobManager.get(player);
        JobClass combat   = data.getCombatJob();
        JobClass life     = data.getLifeJob();

        // 공격력 (기본 + 수정자 합산)
        float atk = 0;
        var atkInst = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkInst != null) atk = (float) atkInst.getValue();

        // 방어력
        float def = 0;
        var defInst = player.getAttribute(Attributes.ARMOR);
        if (defInst != null) def = (float) defInst.getValue();

        // 최대 체력
        float maxHp = (float) player.getMaxHealth();

        // 이동속도 (기본 0.1 → % 환산: (현재-기본)/기본*100)
        float spdPct = 0;
        var spdInst = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spdInst != null) {
            double base = spdInst.getBaseValue(); // 0.1
            double curr = spdInst.getValue();
            spdPct = (float) ((curr - base) / base * 100.0);
        }

        // 점프력
        float jump = 0;
        var jumpInst = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpInst != null) jump = (float) jumpInst.getValue();

        // 코인
        int coins = CosmodEconomy.getCoins(player);

        // 직업 정보
        String jobName    = combat   != null ? combat.displayName   : "없음";
        int    jobLevel   = combat   != null ? data.getLevel(combat) : 0;
        String lifeJobName = life    != null ? life.displayName      : "없음";

        // 공격력% - ATTACK_DAMAGE Attribute의 ADD_MULTIPLIED_BASE 수정자 합산
        float atkPct = 0;
        if (atkInst != null) {
            double baseDmg = atkInst.getBaseValue();
            for (var mod : atkInst.getModifiers()) {
                if (mod.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                    atkPct += (float)(mod.amount() * 100.0);
                }
            }
        }
        float critChance = 0, critDmg = 0;

        ServerPlayNetworking.send(player, new StatSyncPayload(
            atk, atkPct, def, maxHp, spdPct, critChance, critDmg,
            jump, coins, jobName, jobLevel, lifeJobName
        ));
    }
}
