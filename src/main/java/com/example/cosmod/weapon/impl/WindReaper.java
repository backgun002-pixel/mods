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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WindReaper extends WeaponSkillItem {

    private static final List<WeaponSkill> SKILLS = List.of(

        // ── 좌클릭: 바람 베기 ──────────────────────────────────────
        new WeaponSkill() {
            @Override public String name()            { return "바람 베기"; }
            @Override public Trigger trigger()        { return Trigger.LEFT_CLICK; }
            @Override public int cooldownTicks()      { return 30; }  // 1.5초
            @Override public String description()     { return "전방 부채꼴 피해 8 | 넉백"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!(player.level() instanceof ServerLevel sl)) return;
                Vec3 look = player.getLookAngle().normalize();
                double halfAngle = Math.toRadians(60); // 좌우 60도 = 120도 부채꼴
                int hit = 0;
                AABB area = player.getBoundingBox().inflate(4.0, 1.5, 4.0);

                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area)) {
                    if (e == player || !(e instanceof Monster)) continue;
                    Vec3 toE = e.position().subtract(player.position()).normalize();
                    double dot = look.x * toE.x + look.z * toE.z;
                    double angle = Math.acos(Math.max(-1, Math.min(1, dot)));
                    if (angle > halfAngle) continue;
                    e.hurt(sl.damageSources().playerAttack(player), 8.0f);
                    e.setDeltaMovement(toE.x * 1.8, 0.4, toE.z * 1.8);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY()+1, e.getZ(), 5, 0.2,0.2,0.2,0.05);
                    hit++;
                }

                // 부채꼴 바람 파티클
                for (int deg = -60; deg <= 60; deg += 12) {
                    double rad = Math.toRadians(deg);
                    double cos = Math.cos(rad), sin = Math.sin(rad);
                    double fx = look.x*cos - look.z*sin, fz = look.x*sin + look.z*cos;
                    for (int dist = 1; dist <= 4; dist++) {
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            player.getX()+fx*dist, player.getY()+1, player.getZ()+fz*dist,
                            2, 0.1,0.1,0.1,0.02);
                        sl.sendParticles(ParticleTypes.CLOUD,
                            player.getX()+fx*dist, player.getY()+1, player.getZ()+fz*dist,
                            1, 0.1,0.1,0.1,0.01);
                    }
                }
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.7f, false);
                player.displayClientMessage(Component.literal("§a⟳ 바람 베기! §7(" + hit + "명)"), true);
            }
        },

        // ── 우클릭: 폭풍 돌진 ──────────────────────────────────────
        new WeaponSkill() {
            @Override public String name()        { return "폭풍 돌진"; }
            @Override public Trigger trigger()    { return Trigger.RIGHT_CLICK; }
            @Override public int cooldownTicks()  { return 80; }  // 4초
            @Override public String description() { return "전방 돌진 + 충격파 피해 12"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!(player.level() instanceof ServerLevel sl)) return;
                Vec3 look = player.getLookAngle().normalize();

                // 돌진
                player.setDeltaMovement(look.x * 3.0, 0.25, look.z * 3.0);
                player.hurtMarked = true;

                // 돌진 파티클 궤적
                for (int i = 0; i < 5; i++) {
                    sl.sendParticles(ParticleTypes.CLOUD,
                        player.getX()+look.x*i*0.8, player.getY()+1, player.getZ()+look.z*i*0.8,
                        8, 0.2,0.2,0.2,0.04);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        player.getX()+look.x*i*0.8, player.getY()+0.5, player.getZ()+look.z*i*0.8,
                        3, 0.1,0.1,0.1,0.02);
                }

                // 도착지 충격파 (앞 5블록)
                double tx = player.getX() + look.x * 5;
                double tz = player.getZ() + look.z * 5;
                AABB impactArea = new AABB(tx-2, player.getY()-1, tz-2, tx+2, player.getY()+2.5, tz+2);
                int hit = 0;
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, impactArea)) {
                    if (e == player || !(e instanceof Monster)) continue;
                    e.hurt(sl.damageSources().playerAttack(player), 12.0f);
                    e.setDeltaMovement(look.x * 2.5, 0.6, look.z * 2.5);
                    sl.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY()+1, e.getZ(), 1, 0,0,0,0);
                    hit++;
                }
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tx, player.getY()+0.5, tz, 1, 0,0,0,0);
                player.level().playLocalSound(tx, player.getY(), tz,
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.6f, false);
                player.displayClientMessage(Component.literal("§b▶ 폭풍 돌진!" + (hit > 0 ? " §7(" + hit + "명 충격)" : "")), true);
            }
        },

        // ── 쉬프트+우클릭: 대선풍 ──────────────────────────────────
        new WeaponSkill() {
            @Override public String name()        { return "대선풍"; }
            @Override public Trigger trigger()    { return Trigger.SHIFT_RIGHT_CLICK; }
            @Override public int cooldownTicks()  { return 600; }  // 30초
            @Override public String description() { return "360도 회전 베기 피해 20 | 강넉백 | 슬로우"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!(player.level() instanceof ServerLevel sl)) return;
                double cx = player.getX(), cy = player.getY(), cz = player.getZ();

                // 범위 내 전체 타격
                AABB area = new AABB(cx-5, cy-1, cz-5, cx+5, cy+3, cz+5);
                int hit = 0;
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area)) {
                    if (e == player || !(e instanceof Monster)) continue;
                    double dist = e.distanceTo(player);
                    if (dist > 5.0) continue;
                    e.hurt(sl.damageSources().playerAttack(player), 20.0f);
                    // 강한 방사형 넉백
                    Vec3 dir = e.position().subtract(player.position()).normalize();
                    e.setDeltaMovement(dir.x * 3.0, 0.8, dir.z * 3.0);
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY()+1, e.getZ(), 8, 0.3,0.3,0.3,0.1);
                    hit++;
                }

                // 360도 회오리 파티클
                for (int ring = 0; ring < 3; ring++) {
                    double radius = 1.5 + ring * 1.5;
                    for (int i = 0; i < 36; i++) {
                        double angle = Math.toRadians(i * 10 + ring * 30);
                        double px = cx + Math.cos(angle) * radius;
                        double pz = cz + Math.sin(angle) * radius;
                        double py = cy + 0.5 + ring * 0.4;
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, px, py, pz, 1, 0,0,0,0);
                        sl.sendParticles(ParticleTypes.CLOUD, px, py, pz, 2, 0.1,0.2,0.1,0.03);
                    }
                }
                // 위로 솟구치는 바람
                for (int i = 0; i < 20; i++) {
                    double a = Math.random() * Math.PI * 2, r = Math.random() * 3;
                    sl.sendParticles(ParticleTypes.CLOUD,
                        cx + Math.cos(a)*r, cy, cz + Math.sin(a)*r,
                        3, 0.1, 1.5, 0.1, 0.02);
                }

                // 사운드
                player.level().playLocalSound(cx, cy, cz,
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.5f, false);
                player.level().playLocalSound(cx, cy, cz,
                    net.minecraft.sounds.SoundEvents.RAVAGER_ROAR,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f, false);

                player.displayClientMessage(
                    Component.literal("§a§l🌪 대선풍! §f" + hit + "명 피해"), true);
            }
        },

        // ── 패시브: 바람의 가호 ────────────────────────────────────
        new WeaponSkill() {
            @Override public String name()        { return "바람의 가호"; }
            @Override public Trigger trigger()    { return Trigger.PASSIVE; }
            @Override public int cooldownTicks()  { return 0; }
            @Override public String description() { return "장착 중 이동속도 +15%"; }
            @Override public void execute(Player player, ItemStack stack, SkillContext ctx) {}

            @Override
            public void onTick(Player player, ItemStack stack) {
                // 이동속도 +15%
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false));
            }
        }
    );

    public WindReaper(Properties props) {
        super(props, JobClass.WARRIOR, 12); // 기본 데미지 12
    }

    @Override public List<WeaponSkill> getSkills()  { return SKILLS; }
    @Override public String getWeaponName()          { return "윈드리퍼"; }
    @Override public String getWeaponLore()          { return "바람을 가르는 대형 낫. 적을 폭풍으로 몰아친다."; }
}
