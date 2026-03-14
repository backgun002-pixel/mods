package com.example.cosmod.combat;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SetBonusHandler {

    // Identifier 헬퍼 (removeModifier(Identifier) 용)
    private static Identifier atkId(String key) {
        return Identifier.fromNamespaceAndPath("cosmod", "set_atk_" + key.toLowerCase());
    }
    private static Identifier defId(String key) {
        return Identifier.fromNamespaceAndPath("cosmod", "set_def_" + key.toLowerCase());
    }
    private static Identifier spdId(String key) {
        return Identifier.fromNamespaceAndPath("cosmod", "set_spd_" + key.toLowerCase());
    }

    public static void register() {
        // 5틱마다 세트 보너스 갱신
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 5 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                applySetBonuses(player);
            }
        });
    }

    private static void applySetBonuses(ServerPlayer player) {
        for (SetBonus set : SetBonus.values()) {
            SetBonus.Effect effect = SetBonus.getActiveBonus(player, set);
            String key = set.name();

            // ── 공격력 ────────────────────────────────────────────
            AttributeInstance atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) {
                atk.removeModifier(atkId(key));
                if (effect != null && effect.atkBonus() > 0) {
                    atk.addTransientModifier(new AttributeModifier(
                        atkId(key), effect.atkBonus(),
                        AttributeModifier.Operation.ADD_VALUE));
                }
            }

            // ── 방어력 ────────────────────────────────────────────
            AttributeInstance def = player.getAttribute(Attributes.ARMOR);
            if (def != null) {
                def.removeModifier(defId(key));
                if (effect != null && effect.defBonus() > 0) {
                    def.addTransientModifier(new AttributeModifier(
                        defId(key), effect.defBonus(),
                        AttributeModifier.Operation.ADD_VALUE));
                }
            }

            // ── 이동속도 ──────────────────────────────────────────
            AttributeInstance spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spd != null) {
                spd.removeModifier(spdId(key));
                if (effect != null && effect.speedPercent() > 0) {
                    spd.addTransientModifier(new AttributeModifier(
                        spdId(key), effect.speedPercent() / 100.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }

            // ── 힘 포션 효과 ──────────────────────────────────────
            if (effect != null && effect.strengthLevel() > 0) {
                if (!player.hasEffect(MobEffects.STRENGTH)) {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.STRENGTH, 100, effect.strengthLevel() - 1,
                        false, false, true));
                }
            }
        }
    }
}
