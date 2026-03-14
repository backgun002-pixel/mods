package com.example.cosmod.skill;

import com.example.cosmod.job.JobClass;
import com.example.cosmod.job.PlayerJobData;
import com.example.cosmod.job.PlayerJobManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.*;

public class SkillHandler {

    // 쿨타임 (틱)
    public static final int CD_CHARGE      = 200;  // 10초
    public static final int CD_BLADESTORM  = 400;  // 20초
    public static final int CD_BERSERK     = 800;  // 40초
    public static final int CD_DOUBLESHOT  = 200;  // 10초
    public static final int CD_ARROWRAIN   = 400;  // 20초
    public static final int CD_SHARPEYES   = 800;  // 40초

    // cd[0]=슬롯1, cd[1]=슬롯2, cd[2]=슬롯3
    private static final Map<UUID, int[]>   cooldowns      = new HashMap<>();
    private static final Map<UUID, Integer> bladestormTick = new HashMap<>();
    private static final Map<UUID, Integer> arrowRainTick  = new HashMap<>();
    private static final Map<UUID, Vec3>    arrowRainPos   = new HashMap<>();

    // 광폭화 활성 플레이어 (서버 측 상태)
    private static final Map<UUID, Integer> berserkActive  = new HashMap<>();
    // 샤프아이즈 활성 플레이어
    private static final Map<UUID, Integer> sharpEyesActive = new HashMap<>();

    // 광폭화 중 공격 배율 (서버에서 데미지 계산 시 참조)
    public static boolean hasBerserk(UUID uuid)    { return berserkActive.containsKey(uuid); }
    public static boolean hasSharpEyes(UUID uuid)  { return sharpEyesActive.containsKey(uuid); }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(SkillPayload.TYPE, SkillPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SkillCooldownPayload.TYPE, SkillCooldownPayload.CODEC);
        // SkillScreenEffectPayload는 CosmodMod에서 등록

        ServerPlayNetworking.registerGlobalReceiver(SkillPayload.TYPE,
            (payload, ctx) -> ctx.server().execute(() ->
                handleSkill(ctx.player(), payload.skillSlot())));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 쿨타임 감소
            cooldowns.forEach((uuid, cd) -> {
                if (cd[0] > 0) cd[0]--;
                if (cd[1] > 0) cd[1]--;
                if (cd[2] > 0) cd[2]--;
            });



            // 칼날폭풍 틱
            Iterator<Map.Entry<UUID, Integer>> it = bladestormTick.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p == null) { it.remove(); continue; }
                tickBladestorm(p, e.getValue());
                e.setValue(e.getValue() + 1);
                if (e.getValue() >= 24) it.remove();
            }

            // 애로우레인 틱
            Iterator<Map.Entry<UUID, Integer>> it2 = arrowRainTick.entrySet().iterator();
            while (it2.hasNext()) {
                var e = it2.next();
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p == null) { it2.remove(); arrowRainPos.remove(e.getKey()); continue; }
                tickArrowRain(p, e.getValue());
                e.setValue(e.getValue() + 1);
                if (e.getValue() >= 40) { it2.remove(); arrowRainPos.remove(e.getKey()); }
            }

            // 광폭화 타이머
            Iterator<Map.Entry<UUID, Integer>> it3 = berserkActive.entrySet().iterator();
            while (it3.hasNext()) {
                var e = it3.next();
                e.setValue(e.getValue() - 1);
                if (e.getValue() <= 0) {
                    it3.remove();
                    ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                    if (p != null) {
                        // 이펙트 제거 알림 (화면 효과 종료)
                        ServerPlayNetworking.send(p, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_NONE));
                        p.displayClientMessage(Component.literal("§7광폭화가 종료되었습니다."), true);
                    }
                }
            }

            // 샤프아이즈 타이머
            Iterator<Map.Entry<UUID, Integer>> it4 = sharpEyesActive.entrySet().iterator();
            while (it4.hasNext()) {
                var e = it4.next();
                e.setValue(e.getValue() - 1);
                if (e.getValue() <= 0) {
                    it4.remove();
                    ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                    if (p != null) {
                        ServerPlayNetworking.send(p, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_NONE));
                        p.displayClientMessage(Component.literal("§7샤프아이즈가 종료되었습니다."), true);
                    }
                }
            }
        });
    }

    private static void handleSkill(ServerPlayer player, int slot) {
        PlayerJobData data = PlayerJobManager.get(player);
        JobClass combat = data.getCombatJob();
        if (combat == null) {
            player.displayClientMessage(Component.literal("§c전투직업이 없습니다."), true); return;
        }
        int[] cd = cooldowns.computeIfAbsent(player.getUUID(), k -> new int[]{0, 0, 0});
        int jobLevel = data.getLevel(combat);

        if      (combat == JobClass.WARRIOR && slot == 1) useCharge(player, cd);
        else if (combat == JobClass.WARRIOR && slot == 2) useBladestorm(player, cd, jobLevel);
        else if (combat == JobClass.WARRIOR && slot == 3) useBerserk(player, cd, jobLevel);
        else if (combat == JobClass.ARCHER  && slot == 1) useDoubleShot(player, cd, jobLevel);
        else if (combat == JobClass.ARCHER  && slot == 2) useArrowRain(player, cd, jobLevel);
        else if (combat == JobClass.ARCHER  && slot == 3) useSharpEyes(player, cd, jobLevel);
    }

    // ── 전사1: 돌진 (Lv5) ────────────────────────────────────────
    private static void useCharge(ServerPlayer player, int[] cd) {
        if (cd[0] > 0) {
            player.displayClientMessage(Component.literal("§c돌진 쿨타임: §f" + cd[0]/20 + "초"), true); return;
        }
        cd[0] = CD_CHARGE;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(1, cd[0], CD_CHARGE));
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.5f);
        Vec3 look = player.getLookAngle().normalize();
        player.setDeltaMovement(look.x * 2.8, 0.25, look.z * 2.8);
        player.hurtMarked = true;
        for (int i = 0; i < 4; i++) {
            double px = player.getX() + look.x * i * 1.2;
            double pz = player.getZ() + look.z * i * 1.2;
            level.sendParticles(ParticleTypes.CLOUD, px, player.getY()+0.5, pz, 4, 0.2,0.2,0.2,0.05);
            level.sendParticles(ParticleTypes.CRIT,  px, player.getY()+1.0, pz, 5, 0.3,0.3,0.3,0.1);
        }
        AABB area = player.getBoundingBox().expandTowards(look.x*4, 0, look.z*4).inflate(1.5,1,1.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (e == player || !(e instanceof Monster)) continue;
            e.setDeltaMovement(look.x * 2.0, 0.5, look.z * 2.0);
            e.hurt(player.damageSources().playerAttack(player), 8.0f);
            level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY()+1, e.getZ(), 12,0.3,0.3,0.3,0.15);
        }
        player.displayClientMessage(Component.literal("§b⚡ 돌진!"), true);
    }

    // ── 전사2: 칼날폭풍 (Lv15) ───────────────────────────────────
    private static void useBladestorm(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 15) {
            player.displayClientMessage(Component.literal("§c전사 Lv15 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return;
        }
        if (cd[1] > 0) {
            player.displayClientMessage(Component.literal("§c칼날폭풍 쿨타임: §f" + cd[1]/20 + "초"), true); return;
        }
        if (bladestormTick.containsKey(player.getUUID())) return;
        cd[1] = CD_BLADESTORM;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(2, cd[1], CD_BLADESTORM));
        ((ServerLevel)player.level()).playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.6f);
        ((ServerLevel)player.level()).playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
        bladestormTick.put(player.getUUID(), 0);
        player.displayClientMessage(Component.literal("§6🌀 칼날폭풍!"), true);
    }

    private static void tickBladestorm(ServerPlayer player, int tick) {
        ServerLevel level = (ServerLevel) player.level();
        double cx = player.getX(), cy = player.getY()+0.5, cz = player.getZ();
        double baseAngle = tick * 15.0;
        double[] radii = {1.5 + tick*0.08, 2.5 + tick*0.05, 3.2};
        for (int layer = 0; layer < radii.length; layer++) {
            double radius = radii[layer];
            for (int blade = 0; blade < 3; blade++) {
                double angle = Math.toRadians(baseAngle + blade*120.0 + layer*30.0);
                double px = cx + Math.cos(angle)*radius;
                double pz = cz + Math.sin(angle)*radius;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,  px, cy,     pz, 1, 0,   0,   0,   0);
                level.sendParticles(ParticleTypes.CRIT,          px, cy+0.3, pz, 2, 0.1, 0.1, 0.1, 0.05);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, px, cy+0.6, pz, 2, 0.1, 0.2, 0.1, 0.03);
                if (tick % 3 == 0) {
                    level.sendParticles(ParticleTypes.CRIT, px, cy+1.2, pz, 1, 0.05,0.1,0.05,0.03);
                    level.sendParticles(ParticleTypes.CRIT, px, cy-0.2, pz, 1, 0.05,0.1,0.05,0.03);
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(baseAngle*2 + i*120);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                cx+Math.cos(a)*0.6, cy+tick*0.04, cz+Math.sin(a)*0.6, 1,0,0,0,0);
        }
        if (tick % 4 == 0) {
            AABB area = new AABB(cx-3.5, cy-1, cz-3.5, cx+3.5, cy+3, cz+3.5);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (e == player || !(e instanceof Monster)) continue;
                e.hurt(player.damageSources().playerAttack(player), 5.0f);
                level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY()+1, e.getZ(), 5,0.3,0.3,0.3,0.1);
            }
        }
        if (tick == 23) {
            level.sendParticles(ParticleTypes.EXPLOSION, cx, cy+0.5, cz, 1,0,0,0,0);
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(i*45);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    cx+Math.cos(a)*3, cy+0.5, cz+Math.sin(a)*3, 1,0,0,0,0);
            }
        }
    }

    // ── 전사3: 광폭화 (Lv20) ─────────────────────────────────────
    private static void useBerserk(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 20) {
            player.displayClientMessage(Component.literal("§c전사 Lv20 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return;
        }
        if (cd[2] > 0) {
            player.displayClientMessage(Component.literal("§c광폭화 쿨타임: §f" + cd[2]/20 + "초"), true); return;
        }
        if (berserkActive.containsKey(player.getUUID())) {
            player.displayClientMessage(Component.literal("§c광폭화가 이미 활성화되어 있습니다."), true); return;
        }
        cd[2] = CD_BERSERK;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(3, cd[2], CD_BERSERK));

        // 10초간 광폭화 활성
        berserkActive.put(player.getUUID(), 200); // 200틱 = 10초

        // 이동속도 +5%, 공격속도 +20% → MobEffect로 부여
        // 공격력+20%, 방어력-20% → 데미지 계산 시 hasBerserk()로 처리
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, 200, 1, false, false)); // 공격속도 대용

        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 1.0f);

        // 발동 파티클
        for (int i = 0; i < 16; i++) {
            double a = Math.toRadians(i * 22.5);
            double r = 1.5;
            level.sendParticles(ParticleTypes.FLAME,
                player.getX()+Math.cos(a)*r, player.getY()+1, player.getZ()+Math.sin(a)*r,
                3, 0.1, 0.3, 0.1, 0.05);
        }

        // 클라이언트에 화면 효과 전송
        ServerPlayNetworking.send(player, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_BERSERK));
        player.displayClientMessage(Component.literal("§c🔥 광폭화!"), true);
    }

    // ── 궁수1: 더블샷 (Lv5) ──────────────────────────────────────
    private static void useDoubleShot(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 5) {
            player.displayClientMessage(Component.literal("§c궁수 Lv5 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return;
        }
        if (cd[0] > 0) {
            player.displayClientMessage(Component.literal("§c더블샷 쿨타임: §f" + cd[0]/20 + "초"), true); return;
        }
        cd[0] = CD_DOUBLESHOT;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(1, cd[0], CD_DOUBLESHOT));

        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.1f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.8f, 0.9f);

        Vec3 look = player.getLookAngle().normalize();

        // 2발 발사 (유도: 가장 가까운 적 방향으로 살짝 보정)
        LivingEntity target = findNearestMonster(player, level, 16.0);
        Vec3 dir1, dir2;
        if (target != null) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight()/2, 0)
                .subtract(player.getEyePosition()).normalize();
            // 1발은 유도, 1발은 원래 방향
            dir1 = toTarget;
            dir2 = look;
        } else {
            dir1 = look;
            dir2 = look;
        }

        // 무적시간으로 두 발이 모두 맞도록: 첫 발 후 무적시간 0으로 리셋하는 방식은
        // 서버 틱 안에서 불가능하므로 총 데미지를 한 번에 적용
        // 2발 합산 데미지를 타겟에게 적용 (힘5 활 기준 ~9 데미지 × 2)
        float arrowDmg = 9.0f;
        boolean hit = false;

        for (Vec3 dir : new Vec3[]{dir1, dir2}) {
            for (int dist = 1; dist <= 16; dist++) {
                double tx = player.getX() + dir.x * dist;
                double ty = player.getEyeY() + dir.y * dist;
                double tz = player.getZ() + dir.z * dist;
                // 파티클 궤적
                if (dist % 2 == 0)
                    level.sendParticles(ParticleTypes.CRIT, tx, ty, tz, 2, 0.1, 0.1, 0.1, 0.02);
                AABB hitBox = new AABB(tx-0.5, ty-0.5, tz-0.5, tx+0.5, ty+0.5, tz+0.5);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, hitBox)) {
                    if (e == player || !(e instanceof Monster)) continue;
                    // 무적시간 무시: invulnerableTime 임시 0
                    int prevInvul = e.invulnerableTime;
                    e.invulnerableTime = 0;
                    e.hurt(player.damageSources().playerAttack(player), arrowDmg);
                    e.invulnerableTime = 0; // 두 발 모두 맞도록
                    level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY()+1, e.getZ(), 8, 0.2,0.2,0.2,0.05);
                    hit = true;
                    break;
                }
                if (hit) break;
            }
        }

        player.displayClientMessage(Component.literal("§a🏹 더블샷!" + (hit ? " §7적중!" : "")), true);
    }

    // ── 궁수2: 애로우레인 (Lv15) ─────────────────────────────────
    private static void useArrowRain(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 15) {
            player.displayClientMessage(Component.literal("§c궁수 Lv15 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return;
        }
        if (cd[1] > 0) {
            player.displayClientMessage(Component.literal("§c애로우레인 쿨타임: §f" + cd[1]/20 + "초"), true); return;
        }
        if (arrowRainTick.containsKey(player.getUUID())) return;
        cd[1] = CD_ARROWRAIN;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(2, cd[1], CD_ARROWRAIN));

        // 플레이어 시선 방향으로 최대 20블록 앞 지면 위치
        Vec3 look = player.getLookAngle().normalize();
        Vec3 rainCenter = player.getEyePosition().add(look.x*20, look.y*20, look.z*20);
        rainCenter = new Vec3(rainCenter.x, player.getY(), rainCenter.z);
        arrowRainPos.put(player.getUUID(), rainCenter);
        arrowRainTick.put(player.getUUID(), 0);

        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.2f, 0.6f);

        player.displayClientMessage(Component.literal("§b☔ 애로우레인!"), true);
    }

    private static void tickArrowRain(ServerPlayer player, int tick) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = arrowRainPos.get(player.getUUID());
        if (center == null) return;

        // 0~19틱: 준비 (화살 모이는 파티클)
        if (tick < 20) {
            double h = 10 + (19 - tick) * 0.4;
            level.sendParticles(ParticleTypes.CRIT,
                center.x, center.y + h, center.z, 6, 1.8, 0.3, 1.8, 0.01);
            return;
        }

        // 20~59틱: 화살비 (2초간) - 파티클로 화살 궤적 표현
        int rainTick = tick - 20;
        double radius = 3.0;

        // 매 틱 6개 화살 궤적
        for (int i = 0; i < 6; i++) {
            double angle = Math.random() * Math.PI * 2;
            double r = Math.sqrt(Math.random()) * radius;
            double ax = center.x + Math.cos(angle) * r;
            double az = center.z + Math.sin(angle) * r;
            double topY = center.y + 10.0;

            // 화살 궤적: 위에서 아래로 3개 점
            level.sendParticles(ParticleTypes.CRIT, ax, topY,     az, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CRIT, ax, topY-1.5, az, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CRIT, ax, topY-3.0, az, 1, 0, 0, 0, 0);
            // 땅 충돌 파티클
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, ax, center.y + 0.1, az, 1, 0.1, 0, 0.1, 0);

            // 데미지 판정
            AABB hitBox = new AABB(ax-0.5, center.y-0.5, az-0.5, ax+0.5, center.y+2.5, az+0.5);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, hitBox)) {
                if (e == player || !(e instanceof Monster)) continue;
                if (e.invulnerableTime <= 0) {
                    e.hurt(player.damageSources().playerAttack(player), 5.0f);
                    level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY()+1, e.getZ(), 6, 0.3,0.3,0.3,0.08);
                }
            }
        }

        // 사운드
        if (rainTick == 0) {
            level.playSound(null, center.x, center.y, center.z,
                SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
        if (rainTick % 5 == 0) {
            level.playSound(null, center.x, center.y, center.z,
                SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.4f, 1.3f);
        }
    }

    // ── 궁수3: 샤프아이즈 (Lv20) ────────────────────────────────
    private static void useSharpEyes(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 20) {
            player.displayClientMessage(Component.literal("§c궁수 Lv20 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return;
        }
        if (cd[2] > 0) {
            player.displayClientMessage(Component.literal("§c샤프아이즈 쿨타임: §f" + cd[2]/20 + "초"), true); return;
        }
        if (sharpEyesActive.containsKey(player.getUUID())) {
            player.displayClientMessage(Component.literal("§c샤프아이즈가 이미 활성화되어 있습니다."), true); return;
        }
        cd[2] = CD_SHARPEYES;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(3, cd[2], CD_SHARPEYES));

        // 7초간 샤프아이즈 활성
        sharpEyesActive.put(player.getUUID(), 140); // 140틱 = 7초

        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.5f);

        // 발동 파티클
        Vec3 look = player.getLookAngle().normalize();
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                player.getX()+Math.cos(a)*1.2, player.getY()+1.5, player.getZ()+Math.sin(a)*1.2,
                2, 0.1, 0.2, 0.1, 0.03);
        }

        // 클라이언트에 화면 효과 전송
        ServerPlayNetworking.send(player, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_SHARPEYES));
        player.displayClientMessage(Component.literal("§2👁 샤프아이즈! §a화살 피해 ×1.5 (7초)"), true);
    }

    // ── 유틸리티 ─────────────────────────────────────────────────
    private static LivingEntity findNearestMonster(ServerPlayer player, ServerLevel level, double range) {
        AABB area = player.getBoundingBox().inflate(range);
        LivingEntity nearest = null;
        double minDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (e == player || !(e instanceof Monster)) continue;
            double d = e.distanceToSqr(player);
            if (d < minDist) { minDist = d; nearest = e; }
        }
        return nearest;
    }

    public static int[] getCooldowns(UUID uuid) {
        return cooldowns.getOrDefault(uuid, new int[]{0, 0, 0});
    }

    /** 샤프아이즈 활성 시 화살 데미지 배율 */
    public static float getArrowDamageMultiplier(UUID uuid) {
        return sharpEyesActive.containsKey(uuid) ? 1.5f : 1.0f;
    }
}
