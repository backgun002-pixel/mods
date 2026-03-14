package com.example.cosmod.weapon.impl;

import com.example.cosmod.job.JobClass;
import com.example.cosmod.weapon.SkillContext;
import com.example.cosmod.weapon.WeaponSkill;
import com.example.cosmod.weapon.WeaponSkillItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class LavaMaul extends WeaponSkillItem {

    private static final List<WeaponSkill> SKILLS = List.of(

        // 좌클릭: 용암 강타
        new WeaponSkill() {
            @Override public String name() { return "용암 강타"; }
            @Override public Trigger trigger() { return Trigger.LEFT_CLICK; }
            @Override public int cooldownTicks() { return 40; }
            @Override public String description() { return "15 피해 + 주변 용암 파편 (범위 2칸)"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!ctx.hasTarget()) return;
                LivingEntity target = ctx.getTarget();
                target.hurt(player.level().damageSources().playerAttack(player), 15.0f);
                target.setRemainingFireTicks(40);

                List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(target.blockPosition()).inflate(2.0),
                    e -> e != player && e != target && e.isAlive()
                );
                for (LivingEntity t : nearby) {
                    t.hurt(player.level().damageSources().playerAttack(player), 5.0f);
                    t.setRemainingFireTicks(20);
                }
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.LAVA,
                        target.getX(), target.getY() + 0.5, target.getZ(), 30, 0.5, 0.3, 0.5, 0.1);
                }
                player.displayClientMessage(Component.literal("§6용암 강타!"), true);
            }
        },

        // 우클릭: 지진
        new WeaponSkill() {
            @Override public String name() { return "지진"; }
            @Override public Trigger trigger() { return Trigger.RIGHT_CLICK; }
            @Override public int cooldownTicks() { return 160; }
            @Override public String description() { return "주변 4칸 적 8 피해 + 넉백 + 슬로우"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                List<LivingEntity> targets = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(player.blockPosition()).inflate(4.0),
                    e -> e != player && e.isAlive()
                );
                for (LivingEntity t : targets) {
                    t.hurt(player.level().damageSources().playerAttack(player), 8.0f);
                    // 1.21.11: NAUSEA (구 CONFUSION), SLOWNESS (구 MOVEMENT_SLOWDOWN)
                    t.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 40, 1));
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 4));
                    t.setDeltaMovement(
                        (t.getX() - player.getX()) * 0.3,
                        0.5,
                        (t.getZ() - player.getZ()) * 0.3
                    );
                }
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.EXPLOSION,
                        player.getX(), player.getY(), player.getZ(), 3, 2.0, 0.1, 2.0, 0);
                    sl.sendParticles(ParticleTypes.LAVA,
                        player.getX(), player.getY() + 0.5, player.getZ(), 60, 2.0, 0.3, 2.0, 0.15);
                }
                player.displayClientMessage(Component.literal("§6지진! §f(" + targets.size() + "명)"), true);
            }
        },

        // 쉬프트+우클릭: 용암 분출
        new WeaponSkill() {
            @Override public String name() { return "용암 분출"; }
            @Override public Trigger trigger() { return Trigger.SHIFT_RIGHT_CLICK; }
            @Override public int cooldownTicks() { return 400; }
            @Override public String description() { return "6칸 범위 25 피해 + 점화"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                List<LivingEntity> targets = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(player.blockPosition()).inflate(6.0),
                    e -> e != player && e.isAlive()
                );
                for (LivingEntity t : targets) {
                    t.hurt(player.level().damageSources().playerAttack(player), 25.0f);
                    t.setRemainingFireTicks(120);
                }
                if (player.level() instanceof ServerLevel sl) {
                    for (int i = 0; i < 12; i++) {
                        double angle = i * Math.PI / 6;
                        for (double r = 1; r <= 6; r += 1.5) {
                            sl.sendParticles(ParticleTypes.LAVA,
                                player.getX() + Math.cos(angle) * r, player.getY() + 0.5,
                                player.getZ() + Math.sin(angle) * r, 3, 0.2, 0.2, 0.2, 0.05);
                        }
                    }
                    sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
                }
                player.displayClientMessage(Component.literal("§6§l용암 분출!"), true);
            }
        },

        // 패시브: 용암의 핵
        new WeaponSkill() {
            @Override public String name() { return "용암의 핵"; }
            @Override public Trigger trigger() { return Trigger.PASSIVE; }
            @Override public int cooldownTicks() { return 0; }
            @Override public String description() { return "장착 중 불 피해 면역 + 야간 투시"; }
            @Override public void execute(Player p, ItemStack s, SkillContext c) {}

            @Override
            public void onTick(Player player, ItemStack stack) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 40, 0, false, false));
            }
        }
    );

    public LavaMaul(Properties props) { super(props, JobClass.WARRIOR, 18); }

    @Override public List<WeaponSkill> getSkills() { return SKILLS; }
    @Override public String getWeaponName() { return "용암망치"; }
    @Override public String getWeaponLore() { return "화산의 심장에서 단조된 파괴의 망치."; }
}
