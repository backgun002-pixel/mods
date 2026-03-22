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

/**
 * 석상병사 - 던전 전투방 일반 몬스터
 * - 피격 시 근접 공격자에게 슬로우 부여
 */
public class StoneGuardEntity extends Monster {

    public StoneGuardEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("§7석상병사"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH,           40.0)
            .add(Attributes.ATTACK_DAMAGE,         6.0)
            .add(Attributes.ARMOR,                 6.0)
            .add(Attributes.MOVEMENT_SPEED,        0.18)
            .add(Attributes.KNOCKBACK_RESISTANCE,  0.5)
            .add(Attributes.FOLLOW_RANGE,         24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        // 피격 후 근접 공격자에게 슬로우
        if (this.getLastHurtByMob() instanceof Player player
                && this.tickCount % 10 == 0
                && this.distanceTo(player) < 3.0
                && this.random.nextFloat() < 0.15f) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl)
            DungeonDropTable.onMonsterDrop(this, sl);
        super.die(source);
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.STONE_BREAK, this.getSoundSource(), 1.0f, 0.8f);
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return SoundEvents.STONE_STEP; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) { return SoundEvents.STONE_HIT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.STONE_BREAK; }
    @Override protected float getSoundVolume() { return 0.8f; }
}
