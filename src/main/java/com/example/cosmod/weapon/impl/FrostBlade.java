package com.example.cosmod.weapon.impl;

import com.example.cosmod.job.JobClass;
import com.example.cosmod.weapon.SkillContext;
import com.example.cosmod.weapon.WeaponSkill;
import com.example.cosmod.weapon.WeaponSkillItem;
import com.example.cosmod.weapon.WeaponSkillManager;
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

public class FrostBlade extends WeaponSkillItem {

    private static final List<WeaponSkill> SKILLS = List.of(

        // 좌클릭: 냉기 참격
        new WeaponSkill() {
            @Override public String name() { return "냉기 참격"; }
            @Override public Trigger trigger() { return Trigger.LEFT_CLICK; }
            @Override public int cooldownTicks() { return 40; }
            @Override public String description() { return "피해 6|슬로우 5초"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!ctx.hasTarget()) return;
                LivingEntity target = ctx.getTarget();
                target.hurt(player.level().damageSources().playerAttack(player), 6.0f);
                // 1.21.11: SLOWNESS (구 MOVEMENT_SLOWDOWN)
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        target.getX(), target.getY() + 1, target.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
                }
                player.displayClientMessage(Component.literal("§b냉기 참격!"), true);
            }
        },

        // 우클릭: 빙결 폭발
        new WeaponSkill() {
            @Override public String name() { return "빙결 폭발"; }
            @Override public Trigger trigger() { return Trigger.RIGHT_CLICK; }
            @Override public int cooldownTicks() { return 80; }
            @Override public String description() { return "범위 3칸, 피해 4|슬로우"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                List<LivingEntity> targets = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(player.blockPosition()).inflate(3.0),
                    e -> e != player && e.isAlive()
                );
                for (LivingEntity t : targets) {
                    t.hurt(player.level().damageSources().playerAttack(player), 4.0f);
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
                }
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        player.getX(), player.getY() + 1, player.getZ(), 40, 1.5, 0.5, 1.5, 0.1);
                }
                player.displayClientMessage(Component.literal("§b빙결 폭발! §f(" + targets.size() + "명)"), true);
            }
        },

        // 쉬프트+우클릭: 절대영도 (비기) ── 60초 쿨타임
        new WeaponSkill() {
            @Override public String name() { return "절대영도"; }
            @Override public Trigger trigger() { return Trigger.SHIFT_RIGHT_CLICK; }
            @Override public int cooldownTicks() { return 1200; } // 60초
            @Override public String description() {
                return "범위 15x15 얼음 필드 소환 (3초)|0.5초마다 틱 피해 · 강한 슬로우";
            }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!(player.level() instanceof ServerLevel sl)) return;

                double cx = player.getX(), cy = player.getY(), cz = player.getZ();

                // ── 1단계: 중앙 폭발 연출 ──────────────────────────────
                // 중앙 눈 폭발
                sl.sendParticles(ParticleTypes.SNOWFLAKE,
                    cx, cy + 0.5, cz, 150, 0.3, 0.5, 0.3, 0.35);
                sl.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    cx, cy + 1.0, cz, 80, 0.2, 0.2, 0.2, 0.5);
                // 중앙 크리스탈 터지는 느낌
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    cx, cy + 0.3, cz, 60, 0.5, 0.3, 0.5, 0.2);

                // ── 2단계: 원형 경계 파동 ──────────────────────────────
                int ringPoints = 36;
                for (int i = 0; i < ringPoints; i++) {
                    double angle = i * Math.PI * 2.0 / ringPoints;
                    double rx = cx + Math.cos(angle) * 7.5;
                    double rz = cz + Math.sin(angle) * 7.5;
                    sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        rx, cy + 0.1, rz, 8, 0.1, 0.6, 0.1, 0.02);
                    sl.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        rx, cy + 0.1, rz, 4, 0.1, 0.3, 0.1, 0.05);
                }

                // ── 3단계: 얼음 기둥 솟아오르는 파티클 ────────────────
                // 외곽 기둥 (24개)
                for (int i = 0; i < 24; i++) {
                    double angle = i * Math.PI * 2.0 / 24;
                    double r = 6.5 + Math.random() * 1.0;
                    double px = cx + Math.cos(angle) * r;
                    double pz = cz + Math.sin(angle) * r;
                    // 위로 솟는 기둥 파티클
                    sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        px, cy, pz, 12, 0.15, 1.5, 0.15, 0.01);
                    sl.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        px, cy + 1.0, pz, 6, 0.1, 0.5, 0.1, 0.02);
                }
                // 중간 기둥 (16개)
                for (int i = 0; i < 16; i++) {
                    double angle = i * Math.PI * 2.0 / 16 + 0.2;
                    double r = 3.5 + Math.random() * 1.5;
                    double px = cx + Math.cos(angle) * r;
                    double pz = cz + Math.sin(angle) * r;
                    sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        px, cy, pz, 8, 0.1, 1.0, 0.1, 0.01);
                }
                // 안쪽 랜덤 기둥
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * 3.0;
                    sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        cx + Math.cos(angle)*r, cy, cz + Math.sin(angle)*r,
                        6, 0.1, 0.8, 0.1, 0.01);
                }

                // ── 4단계: 바닥 서리 퍼져나가는 효과 ──────────────────
                // 안에서 밖으로 퍼지는 눈 레이어
                for (int ring = 1; ring <= 7; ring++) {
                    int pts = ring * 8;
                    for (int i = 0; i < pts; i++) {
                        double angle = i * Math.PI * 2.0 / pts;
                        double rx = cx + Math.cos(angle) * ring;
                        double rz = cz + Math.sin(angle) * ring;
                        sl.sendParticles(ParticleTypes.SNOWFLAKE,
                            rx, cy + 0.05, rz, 3, 0.3, 0.05, 0.3, 0.01);
                    }
                }

                // 얼음 필드 등록 (3초 = 60틱, 0.5초마다 = 10틱 간격, 틱당 3 피해)
                WeaponSkillManager.addIceField(
                    player.getUUID(), sl,
                    player.position(),
                    7.5,   // radius
                    60,    // 3초
                    10,    // 0.5초마다
                    3.0f
                );

                player.displayClientMessage(
                    Component.literal("§b❄ 절대영도!"), true);
            }
        },

        // 패시브: 냉기의 심장
        new WeaponSkill() {
            @Override public String name() { return "냉기의 심장"; }
            @Override public Trigger trigger() { return Trigger.PASSIVE; }
            @Override public int cooldownTicks() { return 0; }
            @Override public String description() { return "장착 중 불 피해 면역"; }
            @Override public void execute(Player player, ItemStack stack, SkillContext ctx) {}

            @Override
            public void onTick(Player player, ItemStack stack) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
            }
        }
    );

    public FrostBlade(Properties props) { super(props, JobClass.WARRIOR, 8); }

    @Override public List<WeaponSkill> getSkills() { return SKILLS; }
    @Override public String getWeaponName() { return "서리한"; }
    @Override public String getWeaponLore() { return "북극의 냉기를 담은 단검. 적의 움직임을 얼린다."; }
}
