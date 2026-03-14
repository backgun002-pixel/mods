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

    // ── 쿨타임 (틱) ──────────────────────────────────────────────
    public static final int CD_CHARGE       = 200;
    public static final int CD_BLADESTORM   = 400;
    public static final int CD_BERSERK      = 800;
    public static final int CD_DOUBLESHOT   = 200;
    public static final int CD_ARROWRAIN    = 400;
    public static final int CD_SHARPEYES    = 800;
    public static final int CD_MAGICMISSILE = 160;  // 8초
    public static final int CD_HEAL         = 400;  // 20초
    public static final int CD_INFINITY     = 800;  // 40초
    public static final int CD_JIKWON      = 160;   // 8초 정권지르기
    public static final int CD_PASHANG      = 400;  // 20초 파쇄장
    public static final int CD_MEDITATION   = 800;  // 40초 정신수양

    private static final Map<UUID, int[]>    cooldowns       = new HashMap<>();
    private static final Map<UUID, Integer>  bladestormTick  = new HashMap<>();
    private static final Map<UUID, Integer>  arrowRainTick   = new HashMap<>();
    private static final Map<UUID, Vec3>     arrowRainPos    = new HashMap<>();
    private static final Map<UUID, Integer>  berserkActive   = new HashMap<>();
    private static final Map<UUID, Integer>  sharpEyesActive = new HashMap<>();

    // 마법사
    // 인피니티: uuid -> 남은 틱
    private static final Map<UUID, Integer>  infinityActive  = new HashMap<>();
    // 매직 미사일 추적 데이터: uuid -> list of [entityId, targetUUID_msb, targetUUID_lsb, lifeTick]
    private static final Map<UUID, List<double[]>> missiles  = new HashMap<>();

    // 무도가
    // 정신수양: uuid -> 상태(0=경직중, 1=버프중), 남은틱
    private static final Map<UUID, int[]>    meditationState = new HashMap<>();
    // 정신수양 경직 중 고정 위치: uuid -> [x, y, z]
    private static final Map<UUID, double[]> meditationPos   = new HashMap<>();

    public static boolean hasBerserk(UUID uuid)   { return berserkActive.containsKey(uuid); }
    public static boolean hasSharpEyes(UUID uuid) { return sharpEyesActive.containsKey(uuid); }
    public static boolean hasInfinity(UUID uuid)  { return infinityActive.containsKey(uuid); }
    public static float getArrowDamageMultiplier(UUID uuid) {
        return sharpEyesActive.containsKey(uuid) ? 1.5f : 1.0f;
    }
    /** 인피니티 활성 시 최종 데미지 배율 (무기스킬 포함 모든 피해에 적용) */
    public static float getInfinityMultiplier(UUID uuid) {
        return infinityActive.containsKey(uuid) ? 1.8f : 1.0f;
    }
    /** 정신수양 버프 활성 여부 */
    public static boolean hasMeditation(UUID uuid) {
        int[] s = meditationState.get(uuid);
        return s != null && s[0] == 1;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(SkillPayload.TYPE, SkillPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SkillCooldownPayload.TYPE, SkillCooldownPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SkillScreenEffectPayload.TYPE, SkillScreenEffectPayload.CODEC);

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

            // 칼날폭풍
            Iterator<Map.Entry<UUID, Integer>> it = bladestormTick.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p == null) { it.remove(); continue; }
                tickBladestorm(p, e.getValue());
                e.setValue(e.getValue() + 1);
                if (e.getValue() >= 24) it.remove();
            }

            // 애로우레인
            Iterator<Map.Entry<UUID, Integer>> it2 = arrowRainTick.entrySet().iterator();
            while (it2.hasNext()) {
                var e = it2.next();
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p == null) { it2.remove(); arrowRainPos.remove(e.getKey()); continue; }
                tickArrowRain(p, e.getValue());
                e.setValue(e.getValue() + 1);
                if (e.getValue() >= 40) { it2.remove(); arrowRainPos.remove(e.getKey()); }
            }

            // 광폭화
            Iterator<Map.Entry<UUID, Integer>> it3 = berserkActive.entrySet().iterator();
            while (it3.hasNext()) {
                var e = it3.next();
                e.setValue(e.getValue() - 1);
                if (e.getValue() <= 0) {
                    it3.remove();
                    ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                    if (p != null) {
                        ServerPlayNetworking.send(p, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_NONE));
                        p.displayClientMessage(Component.literal("§7광폭화가 종료되었습니다."), true);
                    }
                }
            }

            // 샤프아이즈
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

            // 인피니티
            Iterator<Map.Entry<UUID, Integer>> it5 = infinityActive.entrySet().iterator();
            while (it5.hasNext()) {
                var e = it5.next();
                e.setValue(e.getValue() - 1);
                if (e.getValue() <= 0) {
                    it5.remove();
                    ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                    if (p != null) {
                        ServerPlayNetworking.send(p, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_NONE));
                        p.displayClientMessage(Component.literal("§7인피니티가 종료되었습니다."), true);
                    }
                }
            }

            // 매직 미사일 이동/추적
            Iterator<Map.Entry<UUID, List<double[]>>> it6 = missiles.entrySet().iterator();
            while (it6.hasNext()) {
                var e = it6.next();
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p == null) { it6.remove(); continue; }
                Iterator<double[]> mit = e.getValue().iterator();
                while (mit.hasNext()) {
                    double[] m = mit.next();
                    m[0]--; // lifeTick
                    if (m[0] <= 0) { mit.remove(); continue; }
                    tickMissile(p, m);
                }
                if (e.getValue().isEmpty()) it6.remove();
            }

            // 정신수양 상태 틱
            Iterator<Map.Entry<UUID, int[]>> it7 = meditationState.entrySet().iterator();
            while (it7.hasNext()) {
                var e = it7.next();
                int[] s = e.getValue();
                s[1]--;
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (s[1] <= 0) {
                    if (s[0] == 0) {
                        // 경직 끝 → 버프 시작
                        s[0] = 1; s[1] = 200; // 10초
                        if (p != null) {
                            applyMeditationBuff(p);
                            ServerPlayNetworking.send(p, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_BERSERK));
                            p.displayClientMessage(Component.literal("§b✦ 정신수양 완료! §e공격력·속도 강화!"), true);
                        }
                    } else {
                        // 버프 끝
                        it7.remove();
                        meditationPos.remove(e.getKey());
                        if (p != null) {
                            ServerPlayNetworking.send(p, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_NONE));
                            p.displayClientMessage(Component.literal("§7정신수양 버프가 종료되었습니다."), true);
                        }
                    }
                } else {
                    // 경직 중: 저장된 위치로 매 틱 강제 복원
                    if (s[0] == 0 && p != null) {
                        double[] pos = meditationPos.get(e.getKey());
                        if (pos != null) {
                            p.teleportTo(pos[0], pos[1], pos[2]);
                            p.setDeltaMovement(0, 0, 0);
                        }
                    }
                }
            }
        });
    }

    private static void handleSkill(ServerPlayer player, int slot) {
        PlayerJobData data = PlayerJobManager.get(player);
        JobClass combat = data.getCombatJob();
        if (combat == null) { player.displayClientMessage(Component.literal("§c전투직업이 없습니다."), true); return; }
        int[] cd = cooldowns.computeIfAbsent(player.getUUID(), k -> new int[]{0, 0, 0});
        int jobLevel = data.getLevel(combat);

        if      (combat == JobClass.WARRIOR && slot == 1) useCharge(player, cd);
        else if (combat == JobClass.WARRIOR && slot == 2) useBladestorm(player, cd, jobLevel);
        else if (combat == JobClass.WARRIOR && slot == 3) useBerserk(player, cd, jobLevel);
        else if (combat == JobClass.ARCHER  && slot == 1) useDoubleShot(player, cd, jobLevel);
        else if (combat == JobClass.ARCHER  && slot == 2) useArrowRain(player, cd, jobLevel);
        else if (combat == JobClass.ARCHER  && slot == 3) useSharpEyes(player, cd, jobLevel);
        else if (combat == JobClass.MAGE    && slot == 1) useMagicMissile(player, cd, jobLevel);
        else if (combat == JobClass.MAGE    && slot == 2) useHeal(player, cd, jobLevel);
        else if (combat == JobClass.MAGE    && slot == 3) useInfinity(player, cd, jobLevel);
        else if (combat == JobClass.MONK    && slot == 1) useJikwon(player, cd);
        else if (combat == JobClass.MONK    && slot == 2) usePashang(player, cd, jobLevel);
        else if (combat == JobClass.MONK    && slot == 3) useMeditation(player, cd, jobLevel);
    }

    // ══════════════════════════════════════════════════════════════
    // 전사 스킬
    // ══════════════════════════════════════════════════════════════

    private static void useCharge(ServerPlayer player, int[] cd) {
        if (cd[0] > 0) { player.displayClientMessage(Component.literal("§c돌진 쿨타임: §f" + cd[0]/20 + "초"), true); return; }
        cd[0] = CD_CHARGE;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(1, cd[0], CD_CHARGE));
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.5f);
        Vec3 look = player.getLookAngle().normalize();
        player.setDeltaMovement(look.x * 2.8, 0.25, look.z * 2.8);
        player.hurtMarked = true;
        for (int i = 0; i < 4; i++) {
            double px = player.getX() + look.x * i * 1.2, pz = player.getZ() + look.z * i * 1.2;
            level.sendParticles(ParticleTypes.CLOUD, px, player.getY()+0.5, pz, 4, 0.2,0.2,0.2,0.05);
            level.sendParticles(ParticleTypes.CRIT,  px, player.getY()+1.0, pz, 5, 0.3,0.3,0.3,0.1);
        }
        AABB area = player.getBoundingBox().expandTowards(look.x*4, 0, look.z*4).inflate(1.5,1,1.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (e == player || !(e instanceof Monster)) continue;
            e.setDeltaMovement(look.x*2.0, 0.5, look.z*2.0);
            e.hurt(player.damageSources().playerAttack(player), 8.0f);
            level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY()+1, e.getZ(), 12,0.3,0.3,0.3,0.15);
        }
        player.displayClientMessage(Component.literal("§b⚡ 돌진!"), true);
    }

    private static void useBladestorm(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 15) { player.displayClientMessage(Component.literal("§c전사 Lv15 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return; }
        if (cd[1] > 0) { player.displayClientMessage(Component.literal("§c칼날폭풍 쿨타임: §f" + cd[1]/20 + "초"), true); return; }
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
                double px = cx + Math.cos(angle)*radius, pz = cz + Math.sin(angle)*radius;
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
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, cx+Math.cos(a)*0.6, cy+tick*0.04, cz+Math.sin(a)*0.6, 1,0,0,0,0);
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
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, cx+Math.cos(a)*3, cy+0.5, cz+Math.sin(a)*3, 1,0,0,0,0);
            }
        }
    }

    private static void useBerserk(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 20) { player.displayClientMessage(Component.literal("§c전사 Lv20 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return; }
        if (cd[2] > 0) { player.displayClientMessage(Component.literal("§c광폭화 쿨타임: §f" + cd[2]/20 + "초"), true); return; }
        if (berserkActive.containsKey(player.getUUID())) { player.displayClientMessage(Component.literal("§c광폭화가 이미 활성화되어 있습니다."), true); return; }
        cd[2] = CD_BERSERK;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(3, cd[2], CD_BERSERK));
        berserkActive.put(player.getUUID(), 200);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, 200, 1, false, false));
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 1.0f);
        for (int i = 0; i < 16; i++) {
            double a = Math.toRadians(i*22.5);
            level.sendParticles(ParticleTypes.FLAME, player.getX()+Math.cos(a)*1.5, player.getY()+1, player.getZ()+Math.sin(a)*1.5, 3, 0.1,0.3,0.1,0.05);
        }
        ServerPlayNetworking.send(player, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_BERSERK));
        player.displayClientMessage(Component.literal("§c🔥 광폭화!"), true);
    }

    // ══════════════════════════════════════════════════════════════
    // 궁수 스킬
    // ══════════════════════════════════════════════════════════════

    private static void useDoubleShot(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 5) { player.displayClientMessage(Component.literal("§c궁수 Lv5 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return; }
        if (cd[0] > 0) { player.displayClientMessage(Component.literal("§c더블샷 쿨타임: §f" + cd[0]/20 + "초"), true); return; }
        cd[0] = CD_DOUBLESHOT;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(1, cd[0], CD_DOUBLESHOT));
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.1f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.8f, 0.9f);
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity target = findNearestMonster(player, level, 16.0);
        Vec3 dir1 = (target != null)
            ? target.position().add(0, target.getBbHeight()/2, 0).subtract(player.getEyePosition()).normalize()
            : look;
        Vec3 dir2 = look;
        float arrowDmg = 9.0f;
        boolean hit = false;
        for (Vec3 dir : new Vec3[]{dir1, dir2}) {
            for (int dist = 1; dist <= 16; dist++) {
                double tx = player.getX()+dir.x*dist, ty = player.getEyeY()+dir.y*dist, tz = player.getZ()+dir.z*dist;
                if (dist % 2 == 0) level.sendParticles(ParticleTypes.CRIT, tx, ty, tz, 2, 0.1,0.1,0.1,0.02);
                AABB hitBox = new AABB(tx-0.5, ty-0.5, tz-0.5, tx+0.5, ty+0.5, tz+0.5);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, hitBox)) {
                    if (e == player || !(e instanceof Monster)) continue;
                    int prev = e.invulnerableTime; e.invulnerableTime = 0;
                    e.hurt(player.damageSources().playerAttack(player), arrowDmg);
                    e.invulnerableTime = 0;
                    level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY()+1, e.getZ(), 8, 0.2,0.2,0.2,0.05);
                    hit = true; break;
                }
                if (hit) break;
            }
        }
        player.displayClientMessage(Component.literal("§a🏹 더블샷!" + (hit ? " §7적중!" : "")), true);
    }

    private static void useArrowRain(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 15) { player.displayClientMessage(Component.literal("§c궁수 Lv15 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return; }
        if (cd[1] > 0) { player.displayClientMessage(Component.literal("§c애로우레인 쿨타임: §f" + cd[1]/20 + "초"), true); return; }
        if (arrowRainTick.containsKey(player.getUUID())) return;
        cd[1] = CD_ARROWRAIN;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(2, cd[1], CD_ARROWRAIN));
        Vec3 look = player.getLookAngle().normalize();
        Vec3 center = player.getEyePosition().add(look.x*20, look.y*20, look.z*20);
        center = new Vec3(center.x, player.getY(), center.z);
        arrowRainPos.put(player.getUUID(), center);
        arrowRainTick.put(player.getUUID(), 0);
        ((ServerLevel)player.level()).playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.2f, 0.6f);
        player.displayClientMessage(Component.literal("§b☔ 애로우레인!"), true);
    }

    private static void tickArrowRain(ServerPlayer player, int tick) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = arrowRainPos.get(player.getUUID());
        if (center == null) return;
        if (tick < 20) {
            double h = 10 + (19-tick)*0.4;
            level.sendParticles(ParticleTypes.CRIT, center.x, center.y+h, center.z, 6, 1.8,0.3,1.8,0.01);
            return;
        }
        int rainTick = tick - 20;
        double radius = 3.0;
        for (int i = 0; i < 6; i++) {
            double angle = Math.random()*Math.PI*2, r = Math.sqrt(Math.random())*radius;
            double ax = center.x + Math.cos(angle)*r, az = center.z + Math.sin(angle)*r;
            double topY = center.y + 10.0;
            level.sendParticles(ParticleTypes.CRIT, ax, topY,     az, 1, 0,0,0,0);
            level.sendParticles(ParticleTypes.CRIT, ax, topY-1.5, az, 1, 0,0,0,0);
            level.sendParticles(ParticleTypes.CRIT, ax, topY-3.0, az, 1, 0,0,0,0);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, ax, center.y+0.1, az, 1, 0.1,0,0.1,0);
            AABB hitBox = new AABB(ax-0.5, center.y-0.5, az-0.5, ax+0.5, center.y+2.5, az+0.5);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, hitBox)) {
                if (e == player || !(e instanceof Monster)) continue;
                if (e.invulnerableTime <= 0)
                    e.hurt(player.damageSources().playerAttack(player), 5.0f);
            }
        }
        if (rainTick == 0) level.playSound(null, center.x, center.y, center.z, SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0f, 0.8f);
        if (rainTick % 5 == 0) level.playSound(null, center.x, center.y, center.z, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.4f, 1.3f);
    }

    private static void useSharpEyes(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 20) { player.displayClientMessage(Component.literal("§c궁수 Lv20 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return; }
        if (cd[2] > 0) { player.displayClientMessage(Component.literal("§c샤프아이즈 쿨타임: §f" + cd[2]/20 + "초"), true); return; }
        if (sharpEyesActive.containsKey(player.getUUID())) { player.displayClientMessage(Component.literal("§c이미 활성화되어 있습니다."), true); return; }
        cd[2] = CD_SHARPEYES;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(3, cd[2], CD_SHARPEYES));
        sharpEyesActive.put(player.getUUID(), 140);
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.5f);
        Vec3 look = player.getLookAngle().normalize();
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i*30);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX()+Math.cos(a)*1.2, player.getY()+1.5, player.getZ()+Math.sin(a)*1.2, 2, 0.1,0.2,0.1,0.03);
        }
        ServerPlayNetworking.send(player, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_SHARPEYES));
        player.displayClientMessage(Component.literal("§2👁 샤프아이즈! §a화살 피해 ×1.5 (7초)"), true);
    }

    // ══════════════════════════════════════════════════════════════
    // 마법사 스킬
    // ══════════════════════════════════════════════════════════════

    // ── 마법사1: 매직 미사일 (Lv5) ───────────────────────────────
    private static void useMagicMissile(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 5) { player.displayClientMessage(Component.literal("§c마법사 Lv5 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return; }
        if (cd[0] > 0) { player.displayClientMessage(Component.literal("§c매직 미사일 쿨타임: §f" + cd[0]/20 + "초"), true); return; }
        cd[0] = CD_MAGICMISSILE;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(1, cd[0], CD_MAGICMISSILE));
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.4f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.8f);

        // Lv20 이상: 3발, 그 미만: 1발
        int shotCount = (jobLevel >= 20) ? 3 : 1;
        List<double[]> list = missiles.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());

        // 가장 가까운 적 찾기
        LivingEntity primaryTarget = findNearestMonster(player, level, 24.0);

        for (int i = 0; i < shotCount; i++) {
            // [lifeTick, posX, posY, posZ, dirX, dirY, dirZ, targetId(long msb), targetId(long lsb)]
            Vec3 spawnOffset;
            if (shotCount == 1) {
                spawnOffset = new Vec3(0, 0, 0);
            } else {
                // 3발: 약간 다른 위치에서 발사
                double spread = (i - 1) * 0.5;
                spawnOffset = new Vec3(spread, 0, 0);
            }
            Vec3 startPos = player.getEyePosition()
                .add(player.getLookAngle().normalize().scale(1.0))
                .add(spawnOffset);

            Vec3 initDir;
            if (primaryTarget != null) {
                initDir = primaryTarget.position().add(0, primaryTarget.getBbHeight()/2, 0)
                    .subtract(startPos).normalize();
            } else {
                initDir = player.getLookAngle().normalize();
            }

            UUID tid = (primaryTarget != null) ? primaryTarget.getUUID() : null;
            double[] m = new double[]{
                40,                        // [0] lifeTick
                startPos.x, startPos.y, startPos.z, // [1][2][3] pos
                initDir.x, initDir.y, initDir.z,    // [4][5][6] dir
                tid != null ? tid.getMostSignificantBits()  : 0, // [7]
                tid != null ? tid.getLeastSignificantBits() : 0  // [8]
            };
            list.add(m);
        }
        String msg = (shotCount == 3) ? "§5✦ 매직 미사일 ×3!" : "§5✦ 매직 미사일!";
        player.displayClientMessage(Component.literal(msg), true);
    }

    private static void tickMissile(ServerPlayer owner, double[] m) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        double speed = 0.8;
        // 추적: 타겟이 살아있으면 방향 보정
        UUID targetUUID = null;
        if (m[7] != 0 || m[8] != 0) {
            targetUUID = new UUID((long)m[7], (long)m[8]);
        }
        if (targetUUID != null) {
            final UUID finalTargetUUID = targetUUID;
            AABB searchArea = new AABB(m[1]-30, m[2]-30, m[3]-30, m[1]+30, m[2]+30, m[3]+30);
            LivingEntity foundTarget = level.getEntitiesOfClass(LivingEntity.class, searchArea)
                .stream().filter(e -> e.getUUID().equals(finalTargetUUID)).findFirst().orElse(null);
            if (foundTarget instanceof LivingEntity le && le.isAlive()) {
                Vec3 toTarget = le.position().add(0, le.getBbHeight()/2, 0)
                    .subtract(m[1], m[2], m[3]).normalize();
                // 방향을 현재 dir에서 target 방향으로 10% 보정 (유도)
                m[4] = m[4]*0.85 + toTarget.x*0.15;
                m[5] = m[5]*0.85 + toTarget.y*0.15;
                m[6] = m[6]*0.85 + toTarget.z*0.15;
                double len = Math.sqrt(m[4]*m[4]+m[5]*m[5]+m[6]*m[6]);
                if (len > 0) { m[4]/=len; m[5]/=len; m[6]/=len; }
            }
        }
        // 이동
        m[1] += m[4] * speed;
        m[2] += m[5] * speed;
        m[3] += m[6] * speed;

        // 보라색 파티클 궤적
        level.sendParticles(ParticleTypes.WITCH,       m[1], m[2], m[3], 4, 0.15,0.15,0.15,0.02);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, m[1], m[2], m[3], 2, 0.1,0.1,0.1,0.01);

        // 명중 판정
        AABB hitBox = new AABB(m[1]-0.4, m[2]-0.4, m[3]-0.4, m[1]+0.4, m[2]+0.4, m[3]+0.4);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, hitBox)) {
            if (e == owner || !(e instanceof Monster)) continue;
            float dmg = 10.0f * getInfinityMultiplier(owner.getUUID());
            e.hurt(owner.damageSources().playerAttack(owner), dmg);
            level.sendParticles(ParticleTypes.WITCH,       e.getX(), e.getY()+1, e.getZ(), 20, 0.3,0.5,0.3,0.05);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, e.getX(), e.getY()+1, e.getZ(), 10, 0.2,0.3,0.2,0.03);
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.GENERIC_HURT, SoundSource.HOSTILE, 0.8f, 1.2f);
            m[0] = 0; // 명중 시 소멸
            break;
        }
    }

    // ── 마법사2: 힐 (Lv15) ───────────────────────────────────────
    private static void useHeal(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 15) { player.displayClientMessage(Component.literal("§c마법사 Lv15 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return; }
        if (cd[1] > 0) { player.displayClientMessage(Component.literal("§c힐 쿨타임: §f" + cd[1]/20 + "초"), true); return; }
        cd[1] = CD_HEAL;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(2, cd[1], CD_HEAL));
        ServerLevel level = (ServerLevel) player.level();

        // 체력 회복 (6하트 = 12)
        float healAmt = 12.0f;
        player.heal(healAmt);
        // 재생 이펙트 (3초)
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 2, false, true));
        // 흡수 (2초)
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 0, false, false));

        // 화려한 초록 파티클 연출
        for (int i = 0; i < 24; i++) {
            double a = Math.toRadians(i * 15);
            double r = 1.2;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                player.getX()+Math.cos(a)*r, player.getY()+1.0, player.getZ()+Math.sin(a)*r, 3, 0.1,0.3,0.1,0.04);
        }
        // 위로 솟구치는 초록 파티클
        for (int i = 0; i < 16; i++) {
            double ox = (Math.random()-0.5)*1.0, oz = (Math.random()-0.5)*1.0;
            level.sendParticles(ParticleTypes.COMPOSTER,
                player.getX()+ox, player.getY(), player.getZ()+oz, 1, 0.1,0.5,0.1,0.05);
        }
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY()+2.0, player.getZ(), 8, 0.5,0.3,0.5,0.05);

        // 사운드
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 0.8f);

        // 화면에 초록 이펙트
        ServerPlayNetworking.send(player, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_SHARPEYES));
        // 짧은 시간만 표시되도록 0.5초 후 제거
        // (틱 이벤트가 없으므로 다음 틱에서 즉시 해제용 플래그를 쓸 수 없어, 0.5초 뒤 패킷 재전송)
        // 대신 클라이언트 측에서 짧은 플래시로 처리: 서버에서 1틱 뒤 NONE 전송
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            ServerPlayNetworking.send(player, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_NONE));
        });

        player.displayClientMessage(Component.literal("§a✚ 힐! §f체력을 회복합니다."), true);
    }

    // ── 마법사3: 인피니티 (Lv20) ─────────────────────────────────
    private static void useInfinity(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 20) { player.displayClientMessage(Component.literal("§c마법사 Lv20 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return; }
        if (cd[2] > 0) { player.displayClientMessage(Component.literal("§c인피니티 쿨타임: §f" + cd[2]/20 + "초"), true); return; }
        if (infinityActive.containsKey(player.getUUID())) { player.displayClientMessage(Component.literal("§c이미 활성화되어 있습니다."), true); return; }
        cd[2] = CD_INFINITY;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(3, cd[2], CD_INFINITY));
        // 10초간 모든 마법 데미지 +80% (최종 곱연산)
        infinityActive.put(player.getUUID(), 200); // 200틱 = 10초
        ServerLevel level = (ServerLevel) player.level();
        // 폭발적인 보라 파티클
        for (int i = 0; i < 32; i++) {
            double a = Math.toRadians(i*11.25), r = 2.0;
            level.sendParticles(ParticleTypes.WITCH,
                player.getX()+Math.cos(a)*r, player.getY()+1, player.getZ()+Math.sin(a)*r, 3, 0.1,0.3,0.1,0.05);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                player.getX()+Math.cos(a)*r*0.5, player.getY()+1.5, player.getZ()+Math.sin(a)*r*0.5, 2, 0.1,0.2,0.1,0.03);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY()+1, player.getZ(), 1, 0,0,0,0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8f, 1.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.6f);
        // 화면 보라 이펙트 (광폭화 색이지만 마법사용으로 재활용)
        ServerPlayNetworking.send(player, new SkillScreenEffectPayload(SkillScreenEffectPayload.EFFECT_BERSERK));
        player.displayClientMessage(Component.literal("§d∞ 인피니티! §5모든 피해 ×1.8 (10초)"), true);
    }

    // ══════════════════════════════════════════════════════════════
    // 무도가 스킬
    // ══════════════════════════════════════════════════════════════

    // ── 무도가1: 정권지르기 (Lv5) ────────────────────────────────
    private static void useJikwon(ServerPlayer player, int[] cd) {
        if (cd[0] > 0) { player.displayClientMessage(Component.literal("§c정권지르기 쿨타임: §f" + cd[0]/20 + "초"), true); return; }
        cd[0] = CD_JIKWON;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(1, cd[0], CD_JIKWON));
        ServerLevel level = (ServerLevel) player.level();

        Vec3 look = player.getLookAngle().normalize();
        // 짧은 사거리(3블록) - MC 기본 공격 범위 수준
        float dmg = 14.0f * getInfinityMultiplier(player.getUUID());
        boolean hit = false;

        // 정면 1마리만 타격
        for (int dist = 1; dist <= 3; dist++) {
            double tx = player.getX()+look.x*dist, ty = player.getEyeY()-0.5+look.y*dist, tz = player.getZ()+look.z*dist;
            AABB hitBox = new AABB(tx-0.5, ty-0.5, tz-0.5, tx+0.5, ty+0.5, tz+0.5);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, hitBox)) {
                if (e == player || !(e instanceof Monster)) continue;
                e.hurt(player.damageSources().playerAttack(player), dmg);
                // 짧은 넉백
                e.setDeltaMovement(look.x*1.2, 0.3, look.z*1.2);
                // 임팩트 파티클
                level.sendParticles(ParticleTypes.CRIT,         e.getX(), e.getY()+1, e.getZ(), 15, 0.3,0.3,0.3,0.15);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,  e.getX(), e.getY()+1, e.getZ(), 5,  0.1,0.1,0.1,0.05);
                level.sendParticles(ParticleTypes.CLOUD,         e.getX(), e.getY()+1, e.getZ(), 8,  0.2,0.2,0.2,0.04);
                level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2f, 0.7f);
                level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 0.5f);
                hit = true; break;
            }
            if (hit) break;
        }

        // 타격 전방 파티클 (맞든 안맞든)
        for (int i = 1; i <= 3; i++) {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                player.getX()+look.x*i, player.getEyeY()-0.3, player.getZ()+look.z*i, 2, 0.1,0.1,0.1,0.02);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);

        player.displayClientMessage(Component.literal("§e👊 정권지르기!" + (hit ? "" : " §7(빗나감)")), true);
    }

    // ── 무도가2: 파쇄장 (Lv15) ───────────────────────────────────
    private static void usePashang(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 15) { player.displayClientMessage(Component.literal("§c무도가 Lv15 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return; }
        if (cd[1] > 0) { player.displayClientMessage(Component.literal("§c파쇄장 쿨타임: §f" + cd[1]/20 + "초"), true); return; }
        cd[1] = CD_PASHANG;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(2, cd[1], CD_PASHANG));
        ServerLevel level = (ServerLevel) player.level();

        Vec3 look = player.getLookAngle().normalize();
        float dmg = 10.0f * getInfinityMultiplier(player.getUUID());
        int hitCount = 0;
        // 부채꼴: 전방 3블록, 좌우 60도 (총 120도)
        double halfAngle = Math.toRadians(60);
        AABB scanArea = player.getBoundingBox().inflate(3.0, 1.0, 3.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, scanArea)) {
            if (e == player || !(e instanceof Monster)) continue;
            Vec3 toE = e.position().subtract(player.position()).normalize();
            double dot = look.x*toE.x + look.z*toE.z;
            double angle = Math.acos(Math.max(-1, Math.min(1, dot)));
            if (angle > halfAngle) continue;
            e.hurt(player.damageSources().playerAttack(player), dmg);
            // 넉백
            e.setDeltaMovement(toE.x*1.5, 0.4, toE.z*1.5);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY()+1, e.getZ(), 6, 0.2,0.2,0.2,0.05);
            level.sendParticles(ParticleTypes.CRIT,         e.getX(), e.getY()+1, e.getZ(), 8, 0.3,0.3,0.3,0.1);
            hitCount++;
        }
        // 부채꼴 파티클 연출
        for (int deg = -60; deg <= 60; deg += 10) {
            double rad = Math.toRadians(deg);
            double cos = Math.cos(rad), sin = Math.sin(rad);
            double fx = look.x*cos - look.z*sin, fz = look.x*sin + look.z*cos;
            for (int dist = 1; dist <= 3; dist++) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    player.getX()+fx*dist, player.getY()+1, player.getZ()+fz*dist, 2, 0.1,0.1,0.1,0.02);
                level.sendParticles(ParticleTypes.CLOUD,
                    player.getX()+fx*dist, player.getY()+1, player.getZ()+fz*dist, 1, 0.1,0.1,0.1,0.01);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
        player.displayClientMessage(Component.literal("§6掌 파쇄장! §7(" + hitCount + "명 적중)"), true);
    }

    // ── 무도가3: 정신수양 (Lv20) ─────────────────────────────────
    private static void useMeditation(ServerPlayer player, int[] cd, int jobLevel) {
        if (jobLevel < 20) { player.displayClientMessage(Component.literal("§c무도가 Lv20 달성 시 해금됩니다. (현재 Lv" + jobLevel + ")"), true); return; }
        if (cd[2] > 0) { player.displayClientMessage(Component.literal("§c정신수양 쿨타임: §f" + cd[2]/20 + "초"), true); return; }
        if (meditationState.containsKey(player.getUUID())) { player.displayClientMessage(Component.literal("§c이미 수행 중입니다."), true); return; }
        cd[2] = CD_MEDITATION;
        ServerPlayNetworking.send(player, new SkillCooldownPayload(3, cd[2], CD_MEDITATION));
        // 0 = 경직(2초), 1 = 버프(10초)
        meditationState.put(player.getUUID(), new int[]{0, 40}); // 40틱 = 2초 경직
        // 현재 위치 저장 (경직 중 이 위치로 매 틱 복원)
        meditationPos.put(player.getUUID(), new double[]{player.getX(), player.getY(), player.getZ()});

        ServerLevel level = (ServerLevel) player.level();
        // 경직 시작 파티클
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i*30);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                player.getX()+Math.cos(a)*0.8, player.getY()+1, player.getZ()+Math.sin(a)*0.8, 2, 0.05,0.2,0.05,0.02);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 0.5f);
        player.displayClientMessage(Component.literal("§b禪 정신수양... §72초간 집중 중"), true);
    }

    private static void applyMeditationBuff(ServerPlayer player) {
        // 공격력 +30% → Strength 이펙트로 근사 (Strength I ≈ +30% 근접 피해)
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0, false, true));
        // 공격속도 +30%
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, 200, 1, false, true));
        // 이동속도 +10%
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0, false, true));

        ServerLevel level = (ServerLevel) player.level();
        for (int i = 0; i < 20; i++) {
            double a = Math.toRadians(i*18);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                player.getX()+Math.cos(a)*1.0, player.getY()+1, player.getZ()+Math.sin(a)*1.0, 3, 0.1,0.3,0.1,0.04);
            level.sendParticles(ParticleTypes.CRIT,
                player.getX()+Math.cos(a)*1.5, player.getY()+0.5, player.getZ()+Math.sin(a)*1.5, 2, 0.1,0.2,0.1,0.03);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    // ══════════════════════════════════════════════════════════════
    // 유틸리티
    // ══════════════════════════════════════════════════════════════

    private static LivingEntity findNearestMonster(ServerPlayer player, ServerLevel level, double range) {
        AABB area = player.getBoundingBox().inflate(range);
        LivingEntity nearest = null; double minDist = Double.MAX_VALUE;
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
}
