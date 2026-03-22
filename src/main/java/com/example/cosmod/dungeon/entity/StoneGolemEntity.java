package com.example.cosmod.dungeon.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import com.example.cosmod.dungeon.DungeonDropTable;
import net.minecraft.server.level.ServerLevel;
import com.example.cosmod.dungeon.DungeonDropTable;
import net.minecraft.server.level.ServerLevel;
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
 * 돌골렘 - 느리고 강함, 4초마다 근접 지진 넉백
 */
public class StoneGolemEntity extends Monster {

    private static final int QUAKE_COOLDOWN = 80;
    private int quakeTick = 0;

    public StoneGolemEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("§8돌골렘"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH,           70.0)
            .add(Attributes.ATTACK_DAMAGE,        10.0)
            .add(Attributes.ARMOR,                10.0)
            .add(Attributes.MOVEMENT_SPEED,       0.12)
            .add(Attributes.KNOCKBACK_RESISTANCE,  0.9)
            .add(Attributes.FOLLOW_RANGE,         20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.8, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (quakeTick > 0) quakeTick--;
        if (quakeTick == 0 && this.getTarget() instanceof Player) {
            if (this.distanceTo(this.getTarget()) < 4.0) {
                performQuake();
                quakeTick = QUAKE_COOLDOWN;
            }
        }
    }

    private void performQuake() {
        AABB area = this.getBoundingBox().inflate(4.0);
        for (Player player : this.level().getEntitiesOfClass(Player.class, area)) {
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0) {
                player.push(dx / len * 1.2, 0.4, dz / len * 1.2);
                player.hurt(this.damageSources().mobAttack(this), 6.0f);
            }
        }
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GENERIC_EXPLODE.value(), this.getSoundSource(), 0.6f, 0.5f);
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl)
            DungeonDropTable.onMonsterDrop(this, sl);
        super.die(source);
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return SoundEvents.IRON_GOLEM_STEP; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
}
