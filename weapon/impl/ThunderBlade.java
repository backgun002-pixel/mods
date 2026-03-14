package com.example.cosmod.weapon.impl;

import com.example.cosmod.job.JobClass;
import com.example.cosmod.weapon.SkillContext;
import com.example.cosmod.weapon.WeaponSkill;
import com.example.cosmod.weapon.WeaponSkillItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ThunderBlade extends WeaponSkillItem {

    private static final List<WeaponSkill> SKILLS = List.of(

        // 좌클릭: 번개 베기
        new WeaponSkill() {
            @Override public String name() { return "번개 베기"; }
            @Override public Trigger trigger() { return Trigger.LEFT_CLICK; }
            @Override public int cooldownTicks() { return 20; }
            @Override public String description() { return "7 피해 + 넉백"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!ctx.hasTarget()) return;
                LivingEntity target = ctx.getTarget();
                target.hurt(player.level().damageSources().playerAttack(player), 7.0f);
                Vec3 knockback = player.getLookAngle().scale(1.5);
                target.setDeltaMovement(knockback.x, 0.4, knockback.z);
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.5, 0.3, 0.15);
                }
                player.displayClientMessage(Component.literal("§e번개 베기!"), true);
            }
        },

        // 우클릭: 뇌광 질주
        new WeaponSkill() {
            @Override public String name() { return "뇌광 질주"; }
            @Override public Trigger trigger() { return Trigger.RIGHT_CLICK; }
            @Override public int cooldownTicks() { return 120; }
            @Override public String description() { return "바라보는 방향 5칸 순간이동 후 주변 공격"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                Vec3 look = player.getLookAngle();
                Vec3 dest = player.position().add(look.scale(5.0));

                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1, player.getZ(), 15, 0.2, 0.5, 0.2, 0.1);
                }
                player.teleportTo(dest.x, dest.y, dest.z);

                List<LivingEntity> targets = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(player.blockPosition()).inflate(2.5),
                    e -> e != player && e.isAlive()
                );
                for (LivingEntity t : targets) {
                    t.hurt(player.level().damageSources().playerAttack(player), 9.0f);
                }
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
                }
                player.displayClientMessage(Component.literal("§e뇌광 질주!"), true);
            }
        },

        // 쉬프트+우클릭: 천벌
        new WeaponSkill() {
            @Override public String name() { return "천벌"; }
            @Override public Trigger trigger() { return Trigger.SHIFT_RIGHT_CLICK; }
            @Override public int cooldownTicks() { return 300; }
            @Override public String description() { return "대상 위치에 낙뢰 소환"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!ctx.hasTarget()) {
                    player.displayClientMessage(Component.literal("§c대상이 없습니다."), true);
                    return;
                }
                LivingEntity target = ctx.getTarget();
                if (player.level() instanceof ServerLevel sl) {
                    // 1.21.11: create(ServerLevel, Consumer, BlockPos, EntitySpawnReason, boolean, boolean)
                    var bolt = EntityType.LIGHTNING_BOLT.create(
                        sl,
                        null,
                        target.blockPosition(),
                        EntitySpawnReason.TRIGGERED,
                        false,
                        false
                    );
                    if (bolt != null) {
                        bolt.setCause(player instanceof ServerPlayer sp ? sp : null);
                        sl.addFreshEntity(bolt);
                    }
                }
                player.displayClientMessage(Component.literal("§e§l천벌!"), true);
            }
        },

        // 패시브: 번개의 반사신경
        new WeaponSkill() {
            @Override public String name() { return "번개의 반사신경"; }
            @Override public Trigger trigger() { return Trigger.PASSIVE; }
            @Override public int cooldownTicks() { return 0; }
            @Override public String description() { return "장착 중 이동속도 +10%"; }
            @Override public void execute(Player p, ItemStack s, SkillContext c) {}

            @Override
            public void onTick(Player player, ItemStack stack) {
                // 1.21.11: SPEED (구 MOVEMENT_SPEED)
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false));
            }
        }
    );

    public ThunderBlade(Properties props) { super(props, JobClass.ARCHER, 9); }

    @Override public List<WeaponSkill> getSkills() { return SKILLS; }
    @Override public String getWeaponName() { return "뇌명도"; }
    @Override public String getWeaponLore() { return "번개를 깃든 신속의 도. 눈 깜짝할 사이에 적을 베인다."; }
}
