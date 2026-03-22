package com.example.cosmod.dungeon.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import com.example.cosmod.dungeon.DungeonDropTable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 정예 석상병사 - 미니보스
 * HP 30% 이하 격노 (저항+속도), 6초마다 돌진 공격
 */
public class EliteStoneGuardEntity extends Monster {

    private static final int CHARGE_COOLDOWN = 120;
    private int chargeTick = 40;
    private boolean enraged = false;

    public EliteStoneGuardEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("§c§l[미니보스] §7정예 석상병사"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH,           120.0)
            .add(Attributes.ATTACK_DAMAGE,         12.0)
            .add(Attributes.ARMOR,                  8.0)
            .add(Attributes.MOVEMENT_SPEED,         0.22)
            .add(Attributes.KNOCKBACK_RESISTANCE,   0.7)
            .add(Attributes.FOLLOW_RANGE,          28.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;

        // 격노 체크
        if (!enraged && this.getHealth() < this.getMaxHealth() * 0.3f) {
            enraged = true;
            // 1.21.11: RESISTANCE (구 DAMAGE_RESISTANCE), SPEED (구 MOVEMENT_SPEED)
            this.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            this.addEffect(new MobEffectInstance(MobEffects.SPEED, Integer.MAX_VALUE, 1, false, false));
            this.setCustomName(Component.literal("§4§l[미니보스] §c격노한 석상병사"));
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.RAVAGER_ROAR, this.getSoundSource(), 1.2f, 0.8f);
            AABB area = this.getBoundingBox().inflate(20);
            for (Player p : this.level().getEntitiesOfClass(Player.class, area))
                p.displayClientMessage(Component.literal("§c[던전] 정예 석상병사가 격노했다!"), false);
        }

        // 돌진 공격
        if (chargeTick > 0) chargeTick--;
        if (chargeTick == 0 && this.getTarget() instanceof Player target) {
            double dist = this.distanceTo(target);
            if (dist > 3.0 && dist < 12.0) {
                performCharge(target);
                chargeTick = CHARGE_COOLDOWN;
            }
        }
    }

    private void performCharge(Player target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        this.setDeltaMovement(dx / len * 1.8, 0.1, dz / len * 1.8);
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.RAVAGER_STEP, this.getSoundSource(), 1.0f, 1.0f);
        AABB chargeArea = this.getBoundingBox().inflate(2.5);
        for (Player p : this.level().getEntitiesOfClass(Player.class, chargeArea)) {
            p.hurt(this.damageSources().mobAttack(this), 14.0f);
            // SLOWNESS (구 MOVEMENT_SLOWDOWN)
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl)
            DungeonDropTable.onMonsterDrop(this, sl);
        super.die(source);
        AABB area = this.getBoundingBox().inflate(20);
        for (Player p : this.level().getEntitiesOfClass(Player.class, area))
            p.displayClientMessage(Component.literal("§a[던전] 정예 석상병사를 처치했다!"), false);
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.STONE_BREAK, this.getSoundSource(), 1.5f, 0.5f);
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return SoundEvents.STONE_STEP; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) { return SoundEvents.STONE_HIT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.STONE_BREAK; }
    @Override protected float getSoundVolume() { return 1.0f; }
}
