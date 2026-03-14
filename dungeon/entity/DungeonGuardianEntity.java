package com.example.cosmod.dungeon.entity;

import com.example.cosmod.dungeon.DungeonGimmickHandler;
import com.example.cosmod.dungeon.DungeonManager;
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

    // 가드 관련
    private static final int GUARD_CD_P1      = 600;  // 1페이즈: 30초
    private static final int GUARD_CD_P2      = 500;  // 2페이즈: 25초
    private static final int GUARD_VARIANCE   = 60;
    private static final int GUARD_DURATION   = 100;
    private static final int GUARD_WARN_TICKS = 40;
    private int     guardTick     = GUARD_CD_P1;
    private boolean guardWarned   = false;
    private boolean guardActive   = false;
    private int     guardDuration = 0;

    // 점적석 낙하 관련
    private static final int STALAGTITE_CD = 400;
    private int stalagtiteTick = STALAGTITE_CD;
    private final Map<Integer, double[]> fallingDripstones = new HashMap<>();

    // 화염구 추적 Map: entityId → [spawnX, spawnY, spawnZ, dirX, dirY, dirZ, lifeTick]
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
        broadcast("§6§l[수호자] §f전투가 끝났다! §7무릎을 꿇었다.");
        if (level() instanceof ServerLevel sl)
            DungeonGimmickHandler.onBossDefeated(sl, blockPosition());
        super.die(src);
    }

    private void handleSkills() {
        if (spawnTick > 0) {
            spawnTick--;
            return;
        }
        if (currentAnim == AnimState.SPAWN) {
            currentAnim = AnimState.IDLE;
        }

        if (--fireballTick <= 0) {
            fireballTick = (phase == 2 ? FIREBALL_CD_P2 : FIREBALL_CD_P1);
            doFireball();
        }

        if (guardActive) {
            if (--guardDuration <= 0) {
                guardActive = false;
            }
        } else {
            if (--guardTick <= 0) {
                int baseCD = (phase == 2 ? GUARD_CD_P2 : GUARD_CD_P1);
                guardTick = baseCD + (int)((Math.random() * 2 - 1) * GUARD_VARIANCE);
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
        }

        if (phase == 2 && --roarTick <= 0) {
            roarTick = ROAR_CD_P2;
            doRoar();
        }
    }

    private void doFireball() {
        playAnim(AnimState.FIREBALL);
        playSound(SoundEvents.BLAZE_SHOOT, 2f, 0.7f);
        if (!(level() instanceof ServerLevel sl) || getTarget() == null) return;
        LivingEntity target = getTarget();

        double tx = target.getX() - getX();
        double ty = target.getEyeY() - getEyeY();
        double tz = target.getZ() - getZ();
        double len = Math.sqrt(tx*tx + ty*ty + tz*tz);
        if (len < 0.1) return;
        double nx = tx/len, ny = ty/len, nz = tz/len;

        // 보스 입 앞 5블록 기준, 가운데 제거 -> 좌우 2발
        double baseX = getX() + nx * 5.0;
        double baseY = getEyeY();
        double baseZ = getZ() + nz * 5.0;

        for (int i : new int[]{-1, 1}) {
            double sideX = -nz * i * 1.2;
            double sideZ =  nx * i * 1.2;

            double fx = baseX + sideX;
            double fz = baseZ + sideZ;

            // 각 화염구가 플레이어를 정확히 향하도록 방향 재계산
            double atx = target.getX() - fx;
            double aty = target.getEyeY() - baseY;
            double atz = target.getZ() - fz;
            double alen = Math.sqrt(atx*atx + aty*aty + atz*atz);
            if (alen < 0.1) continue;
            double power = 0.5;
            double px = atx/alen * power;
            double py = aty/alen * power;
            double pz = atz/alen * power;

            // SmallFireball을 시각용으로 스폰, 자체 AI/충돌은 비활성화
            var fb = EntityType.SMALL_FIREBALL.create(sl, null,
                    BlockPos.containing(fx, baseY, fz),
                    EntitySpawnReason.MOB_SUMMONED, false, false);
            if (fb != null) {
                fb.setOwner(this);
                fb.setPos(fx, baseY, fz);
                fb.setDeltaMovement(0, 0, 0); // 직접 이동 제어
                fb.setNoGravity(true);
                sl.addFreshEntity(fb);
                // Map: entityId → [dx,dy,dz,age]
                trackedFireballs.put(fb.getId(),
                    new double[]{px, py, pz, 0});
            }
        }
    }

    private void activateGuard() {
        guardActive = true;
        guardDuration = GUARD_DURATION;
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.5f, 0.5f);
        broadcast("§8[수호자] §7몸을 굳혔다... §c[가드]");
    }

    private void doStalagmiteDrop() {
        if (!(level() instanceof ServerLevel sl)) return;

        List<Player> players = level().getEntitiesOfClass(Player.class,
                new AABB(blockPosition()).inflate(20));
        if (players.isEmpty()) return;

        Player target = players.get(sl.getRandom().nextInt(players.size()));

        broadcast("§8[수호자] §7천장이 흔들린다..!");
        playSound(SoundEvents.STONE_BREAK, 2f, 0.5f);

        double fy      = DungeonManager.DUNGEON_Y + 14 - 2;
        double floorY  = DungeonManager.DUNGEON_Y + 1.0;

        for (int ddx = -1; ddx <= 1; ddx++) {
            for (int ddz = -1; ddz <= 1; ddz++) {
                double fx = target.getX() + ddx;
                double fz = target.getZ() + ddz;

                BlockPos spawnPos = BlockPos.containing(fx, fy, fz);
                FallingBlockEntity falling = FallingBlockEntity.fall(
                        sl, spawnPos,
                        Blocks.POINTED_DRIPSTONE.defaultBlockState()
                                .setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN)
                );
                falling.dropItem = false;
                falling.setPos(fx, fy, fz);
                fallingDripstones.put(falling.getId(), new double[]{floorY, fx, fz});
            }
        }
    }

    private void tickFireballs() {
        if (!(level() instanceof ServerLevel sl)) return;
        if (trackedFireballs.isEmpty()) return;

        final double SPEED   = 0.5;  // 블록/틱
        final int    MAX_AGE = 100;  // 최대 수명(틱)
        final double HIT_R   = 0.8;  // 충돌 판정 (플레이어 크기 기준)
        final float  DAMAGE  = 20.0f;

        Iterator<Map.Entry<Integer, double[]>> it = trackedFireballs.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            int id = entry.getKey();
            double[] d = entry.getValue(); // [dx,dy,dz,age]
            d[3]++;

            net.minecraft.world.entity.Entity fb = sl.getEntity(id);

            // 엔티티 소멸 or 수명 초과
            if (fb == null || !fb.isAlive() || d[3] > MAX_AGE) {
                if (fb != null && fb.isAlive()) {
                    spawnExplosionEffect(sl, fb.getX(), fb.getY(), fb.getZ());
                    fb.discard();
                }
                it.remove();
                continue;
            }

            // 엔티티 직접 이동 (속도 고정)
            double nx = fb.getX() + d[0];
            double ny = fb.getY() + d[1];
            double nz = fb.getZ() + d[2];
            fb.setPos(nx, ny, nz);

            // 블록 충돌 체크
            BlockPos bp = BlockPos.containing(nx, ny, nz);
            if (!sl.getBlockState(bp).isAir()) {
                spawnExplosionEffect(sl, nx, ny, nz);
                fb.discard();
                it.remove();
                continue;
            }

            // 플레이어 충돌 체크 (엔티티 실제 위치 기준 → 피하기 가능)
            AABB box = new AABB(nx-HIT_R, ny-HIT_R, nz-HIT_R,
                                nx+HIT_R, ny+HIT_R, nz+HIT_R);
            boolean hit = false;
            for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
                p.hurt(sl.damageSources().fireball(null, this), DAMAGE);
                hit = true;
                break;
            }
            if (hit) {
                spawnExplosionEffect(sl, nx, ny, nz);
                fb.discard();
                it.remove();
            }
        }
    }

    private void spawnExplosionEffect(ServerLevel sl, double x, double y, double z) {
        // 블록 파괴 없이 폭발 효과만 (NONE = 블록 파괴 안 함)
        sl.explode(null, x, y, z, 3.0f, false,
            net.minecraft.world.level.Level.ExplosionInteraction.NONE);
    }

        private void tickFallingDripstones() {
        if (!(level() instanceof ServerLevel sl)) return;
        if (fallingDripstones.isEmpty()) return;

        Iterator<Map.Entry<Integer, double[]>> it = fallingDripstones.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, double[]> entry = it.next();
            net.minecraft.world.entity.Entity e = sl.getEntity(entry.getKey());
            double[] data = entry.getValue();

            boolean arrived = (e == null || e.getY() <= data[0] + 1.5);
            if (arrived) {
                double impactX = data[1];
                double impactZ = data[2];
                for (Player p : sl.getEntitiesOfClass(Player.class,
                        new AABB(impactX - 1.2, data[0] - 0.5, impactZ - 1.2,
                                 impactX + 1.2, data[0] + 3.0, impactZ + 1.2))) {
                    p.hurt(sl.damageSources().stalagmite(), 12.0f);
                }
                if (e != null) e.discard();
                it.remove();
            }
        }
    }

    private void activatePhase2() {
        stalagtiteTick = STALAGTITE_CD / 2;
        playAnim(AnimState.PHASE2);
        playSound(SoundEvents.WITHER_AMBIENT, 2f, 0.5f);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(20.0);
        getAttribute(Attributes.ARMOR).setBaseValue(8.0);
        AABB area = new AABB(blockPosition()).inflate(20);
        for (var p : level().getEntitiesOfClass(Player.class, area)) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        }
        broadcast("§c§l[수호자] §f임계점을 넘었다! §c전투 개시!");
    }

    private void doRoar() {
        playAnim(AnimState.ROAR);
        playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 2f, 0.6f);
        AABB area = new AABB(blockPosition()).inflate(15);
        for (var p : level().getEntitiesOfClass(Player.class, area)) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
            double nx = getX()-p.getX(), nz = getZ()-p.getZ();
            double l = Math.sqrt(nx*nx+nz*nz);
            if (l > 0) p.setDeltaMovement(-nx/l*1.5, 0.4, -nz/l*1.5);
        }
        broadcast("§4[수호자] §c대지가 흔들린다!");
    }

    private void broadcast(String msg) {
        AABB area = new AABB(blockPosition()).inflate(60);
        for (var p : level().getEntitiesOfClass(Player.class, area))
            if (p instanceof ServerPlayer sp)
                sp.displayClientMessage(Component.literal(msg), false);
    }

    public boolean isFireballAnim()      { return currentAnim == AnimState.FIREBALL; }
    public float   getFireballProgress() { return 0f; }
}
