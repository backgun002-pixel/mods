package com.example.cosmod.dungeon.entity;

import com.example.cosmod.dungeon.DungeonGimmickHandler;
import com.example.cosmod.dungeon.DungeonManager;
import com.example.cosmod.dungeon.DungeonDropTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.animation.state.AnimationTest;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DungeonGuardianEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_IDLE     = RawAnimation.begin().thenLoop("animation.dungeon_guardian.idle");
    private static final RawAnimation ANIM_SPAWN    = RawAnimation.begin().thenPlay("animation.dungeon_guardian.spawn");
    private static final RawAnimation ANIM_FIREBALL = RawAnimation.begin().thenPlay("animation.dungeon_guardian.fireball");
    private static final RawAnimation ANIM_ROAR     = RawAnimation.begin().thenPlay("animation.dungeon_guardian.roar");
    private static final RawAnimation ANIM_HURT     = RawAnimation.begin().thenPlay("animation.dungeon_guardian.hurt");
    private static final RawAnimation ANIM_DEATH    = RawAnimation.begin().thenPlay("animation.dungeon_guardian.death");
    private static final RawAnimation ANIM_PHASE2   = RawAnimation.begin()
            .thenPlay("animation.dungeon_guardian.phase2_activate")
            .thenLoop("animation.dungeon_guardian.idle");

    public enum AnimState { IDLE, SPAWN, FIREBALL, ROAR, HURT, DEATH, PHASE2 }
    private AnimState currentAnim = AnimState.SPAWN;

    private double spawnX, spawnY, spawnZ;
    private boolean spawnSaved = false;
    private int phase = 1;
    private int fireballTick  = 60;
    private int roarTick      = 0;
    private int animResetTick = 0;

    private static final int GUARD_CD_P1      = 600;
    private static final int GUARD_CD_P2      = 500;
    private static final int GUARD_VARIANCE   = 60;
    private static final int GUARD_DURATION   = 100;
    private static final int GUARD_WARN_TICKS = 40;
    private int     guardTick     = GUARD_CD_P1;
    private boolean guardWarned   = false;
    private boolean guardActive   = false;
    private int     guardDuration = 0;

    private static final int STALAGTITE_CD = 400;
    private int stalagtiteTick = STALAGTITE_CD;
    private final Map<Integer, double[]> fallingDripstones = new HashMap<>();

    private final Map<Integer, double[]> trackedFireballs = new HashMap<>();
    private static final int FIREBALL_CD_P1 = 140;
    private static final int FIREBALL_CD_P2 = 80;
    private static final int ROAR_CD_P2     = 300;

    private int lastHurtTime = 0;
    private int spawnTick    = 80;

    public DungeonGuardianEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,          350.0)
                .add(Attributes.ATTACK_DAMAGE,        12.0)
                .add(Attributes.ARMOR,                14.0)
                .add(Attributes.MOVEMENT_SPEED,        0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE,  1.0)
                .add(Attributes.FOLLOW_RANGE,          40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.0, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 30f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DungeonGuardianEntity>(
                "main_controller", 5, this::animationHandler));
    }

    private PlayState animationHandler(final AnimationTest<DungeonGuardianEntity> animTest) {
        return switch (currentAnim) {
            case SPAWN    -> animTest.setAndContinue(ANIM_SPAWN);
            case FIREBALL -> animTest.setAndContinue(ANIM_FIREBALL);
            case PHASE2   -> animTest.setAndContinue(ANIM_PHASE2);
            default       -> animTest.setAndContinue(ANIM_IDLE);
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    public void playAnim(AnimState anim) {
        this.currentAnim = anim;
        if (anim == AnimState.HURT || anim == AnimState.FIREBALL || anim == AnimState.ROAR)
            this.animResetTick = 40;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        if (!spawnSaved) { spawnX = getX(); spawnY = getY(); spawnZ = getZ(); spawnSaved = true; }
        double dx = getX()-spawnX, dz = getZ()-spawnZ;
        if (dx*dx+dz*dz > 0.01) { setPos(spawnX,spawnY,spawnZ); setDeltaMovement(0,0,0); }

        if (!isAlive()) return;

        Player nearestPlayer = level().getNearestPlayer(this, 40);
        if (nearestPlayer != null) {
            double ddx = nearestPlayer.getX() - getX();
            double ddz = nearestPlayer.getZ() - getZ();
            float targetYaw = (float)(Math.atan2(ddz, ddx) * (180.0 / Math.PI)) - 90f;
            this.yBodyRot = targetYaw;
            this.yHeadRot = targetYaw;
            this.setYRot(targetYaw);
        }

        if (hurtTime > 0 && lastHurtTime == 0) onHurtDetected();
        lastHurtTime = hurtTime;

        if (animResetTick > 0 && --animResetTick == 0)
            currentAnim = (phase == 2 ? AnimState.PHASE2 : AnimState.IDLE);

        if (phase == 1 && getHealth() <= getMaxHealth() * 0.5f) { phase = 2; activatePhase2(); }

        handleSkills();
        tickFallingDripstones();
        tickFireballs();
    }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource src, float amount) {
        if (guardActive) amount = 0.0001f;
        super.actuallyHurt(level, src, amount);
    }

    private void onHurtDetected() {
        if (currentAnim != AnimState.DEATH && currentAnim != AnimState.FIREBALL)
            playAnim(AnimState.HURT);
    }

    @Override
    public void die(DamageSource src) {
        playAnim(AnimState.DEATH);
        broadcast("§6§l[수호자] §f전투가 끝났다! §7무를을 꿇었다.");
        if (level() instanceof ServerLevel sl) {
            DungeonGimmickHandler.onBossDefeated(sl, blockPosition());
            if (src.getEntity() instanceof ServerPlayer killer)
                DungeonDropTable.onBossDrop(killer, sl);
        }
        super.die(src);
    }

    private void handleSkills() {
        if (spawnTick > 0) { spawnTick--; return; }
        if (currentAnim == AnimState.SPAWN) currentAnim = AnimState.IDLE;

        if (--fireballTick <= 0) {
            fireballTick = (phase == 2 ? FIREBALL_CD_P2 : FIREBALL_CD_P1);
            doFireball();
        }

        if (guardActive) {
            if (--guardDuration <= 0) guardActive = false;
        } else {
            if (--guardTick <= 0) {
                int baseCD = (phase == 2 ? GUARD_CD_P2 : GUARD_CD_P1);
                guardTick = baseCD + (int)((Math.random()*2-1)*GUARD_VARIANCE);
                guardWarned = false;
                activateGuard();
            } else if (!guardWarned && guardTick == GUARD_WARN_TICKS) {
                guardWarned = true;
                broadcast("§8[수호자] §7무언가 단단해지는 느낌이...");
            }
        }

        if (phase == 2) {
            if (--stalagtiteTick <= 0) {
                stalagtiteTick = STALAGTITE_CD / 2;
                doStalagmiteDrop();
            }
            if (--roarTick <= 0) {
                roarTick = ROAR_CD_P2;
                doRoar();
            }
        }
    }
    private void doFireball() {
        playAnim(AnimState.FIREBALL);
        playSound(SoundEvents.BLAZE_SHOOT, 2f, 0.7f);
        if (!(level() instanceof ServerLevel sl) || getTarget() == null) return;
        LivingEntity target = getTarget();

        double tx = target.getX() - getX();
        double ty = (target.getY() + target.getBbHeight()/2) - (getY() + getBbHeight()/2);
        double tz = target.getZ() - getZ();
        double len = Math.sqrt(tx*tx + ty*ty + tz*tz);
        if (len < 0.001) return;
        double nx = tx/len, ny = ty/len, nz = tz/len;

        double bx = getX();
        double by = getY() + getBbHeight()/2;
        double bz = getZ();

        // 1페이즈: 단일 화염구, 2페이즈: 3방향 동시
        int shots = (phase == 2) ? 3 : 1;
        for (int i = 0; i < shots; i++) {
            double spreadX = nx, spreadZ = nz;
            if (shots > 1) {
                double angle = Math.toRadians(-20 + i * 20);
                spreadX = nx * Math.cos(angle) - nz * Math.sin(angle);
                spreadZ = nx * Math.sin(angle) + nz * Math.cos(angle);
            }
            net.minecraft.world.entity.projectile.SmallFireball fb =
                new net.minecraft.world.entity.projectile.SmallFireball(sl, this, new net.minecraft.world.phys.Vec3(spreadX, ny, spreadZ));
            fb.setPos(bx, by, bz);
            fb.setPower(spreadX * 0.2, ny * 0.2, spreadZ * 0.2);
            sl.addFreshEntity(fb);
            int fbId = fb.getId();
            trackedFireballs.put(fbId, new double[]{bx, by, bz, spreadX, ny, spreadZ, 60});
        }
    }

    private void tickFireballs() {
        Iterator<Map.Entry<Integer,double[]>> it = trackedFireballs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer,double[]> e = it.next();
            double[] d = e.getValue();
            d[6]--;
            if (d[6] <= 0) it.remove();
        }
    }

    private void activateGuard() {
        guardActive   = true;
        guardDuration = GUARD_DURATION;
        broadcast("§8[수호자] §7투명한 방어막이 활성화된다!");
        playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.5f, 0.6f);
        if (level() instanceof ServerLevel sl) {
            AABB area = getBoundingBox().inflate(20);
            for (Player p : sl.getEntitiesOfClass(Player.class, area))
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, GUARD_DURATION + 20, 1, false, true));
        }
    }

    private void doStalagmiteDrop() {
        if (!(level() instanceof ServerLevel sl)) return;
        Player target = level().getNearestPlayer(this, 30);
        if (target == null) return;

        broadcast("§8[수호자] §7낙하하는 돌..!");
        for (int i = 0; i < (phase == 2 ? 5 : 3); i++) {
            double ox = (Math.random() - 0.5) * 10;
            double oz = (Math.random() - 0.5) * 10;
            double dropX = target.getX() + ox;
            double dropZ = target.getZ() + oz;
            double dropY = target.getY() + 15;

            BlockPos dropPos = new BlockPos((int)dropX, (int)dropY, (int)dropZ);
            sl.setBlock(dropPos, Blocks.POINTED_DRIPSTONE.defaultBlockState()
                .setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN), 3);

            FallingBlockEntity falling = FallingBlockEntity.fall(sl, dropPos,
                Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    .setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN));
            falling.dropItem = false;
            falling.setHurtsEntities(8.0f, 40);
            sl.addFreshEntity(falling);
            fallingDripstones.put(falling.getId(), new double[]{dropX, dropY, dropZ, 100});
        }
    }

    private void tickFallingDripstones() {
        Iterator<Map.Entry<Integer,double[]>> it = fallingDripstones.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer,double[]> e = it.next();
            double[] d = e.getValue();
            d[3]--;
            if (d[3] <= 0) {
                if (level() instanceof ServerLevel sl) {
                    net.minecraft.world.entity.Entity ent = sl.getEntity(e.getKey());
                    if (ent != null) ent.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
                it.remove();
            }
        }
    }

    private void doRoar() {
        playAnim(AnimState.ROAR);
        playSound(SoundEvents.RAVAGER_ROAR, 2.0f, 0.5f);
        broadcast("§4[수호자] §c대지가 떨린다!!");
        if (!(level() instanceof ServerLevel sl)) return;
        AABB area = getBoundingBox().inflate(15);
        for (Player p : sl.getEntitiesOfClass(Player.class, area)) {
            double dx = p.getX() - getX();
            double dz = p.getZ() - getZ();
            double len = Math.sqrt(dx*dx + dz*dz);
            if (len > 0) p.push(dx/len * 2.5, 0.6, dz/len * 2.5);
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, true));
        }
    }

    private void activatePhase2() {
        stalagtiteTick = STALAGTITE_CD / 2;
        playAnim(AnimState.PHASE2);
        broadcast("§4§l[수호자] §c나를... 감히 여기까지..?!");
        playSound(SoundEvents.WITHER_SHOOT, 2.0f, 0.5f);
        this.addEffect(new MobEffectInstance(MobEffects.SPEED, Integer.MAX_VALUE, 1, false, false));
        if (level() instanceof ServerLevel sl) {
            AABB area = getBoundingBox().inflate(20);
            for (Player p : sl.getEntitiesOfClass(Player.class, area))
                p.displayClientMessage(Component.literal("§4§l[수호자] 2페이즈 진입!"), false);
        }
    }

    private void broadcast(String msg) {
        if (!(level() instanceof ServerLevel sl)) return;
        AABB area = getBoundingBox().inflate(60);
        for (Player p : sl.getEntitiesOfClass(Player.class, area))
            p.displayClientMessage(Component.literal(msg), false);
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
    @Override protected float getSoundVolume() { return 2.0f; }
}
