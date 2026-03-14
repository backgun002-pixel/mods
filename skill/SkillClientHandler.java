package com.example.cosmod.skill;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SkillClientHandler {

    public static void register() {
        // S2C: 쿨타임 동기화
        ClientPlayNetworking.registerGlobalReceiver(SkillCooldownPayload.TYPE, (payload, ctx) -> {
            SkillHud.setCooldown(payload.slot(), payload.cd(), payload.maxCd());
        });

        // S2C: 직업 동기화
        ClientPlayNetworking.registerGlobalReceiver(SkillJobSyncPayload.TYPE, (payload, ctx) -> {
            String job = payload.combatJob();
            int lv = payload.jobLevel();
            if (job.equals("WARRIOR"))     SkillClientCache.setWarrior(lv);
            else if (job.equals("ARCHER")) SkillClientCache.setArcher(lv);
            else                           SkillClientCache.clear();
        });

        // S2C: 화면 이펙트
        ClientPlayNetworking.registerGlobalReceiver(SkillScreenEffectPayload.TYPE, (payload, ctx) -> {
            SkillClientCache.activeScreenEffect = payload.effect();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // 키 입력 처리
            if (SkillKey.SKILL_1.consumeClick()) {
                ClientPlayNetworking.send(new SkillPayload(1));
            }
            if (SkillKey.SKILL_2.consumeClick()) {
                ClientPlayNetworking.send(new SkillPayload(2));
            }
            if (SkillKey.SKILL_3.consumeClick()) {
                ClientPlayNetworking.send(new SkillPayload(3));
            }

            SkillHud.tick();
        });
    }
}
