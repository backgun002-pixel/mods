package com.example.cosmod.weapon;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WeaponSkillManager {

    private static final Map<String, Integer> cooldowns = new HashMap<>();

    // ── 활성 필드 레코드 ──────────────────────────────────────────
    public record ActiveField(
        UUID ownerUuid,
        ServerLevel level,
        Vec3 center,
        double radius,    // inflate() 반경
        int durationTicks,
        int tickInterval, // 몇 틱마다 데미지
        float damagePerTick,
        int[] timer       // [0]=남은틱, [1]=다음데미지카운터 (배열로 가변참조)
    ) {}

    private static final List<ActiveField> activeFields = new ArrayList<>();

    /** 얼음 필드 등록 */
    public static void addIceField(UUID ownerUuid, ServerLevel level, Vec3 center,
                                   double radius, int durationTicks,
                                   int tickInterval, float damagePerTick) {
        activeFields.add(new ActiveField(
            ownerUuid, level, center, radius,
            durationTicks, tickInterval, damagePerTick,
            new int[]{ durationTicks, 0 }
        ));
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 쿨타임 감소
            cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
            cooldowns.entrySet().removeIf(e -> e.getValue() <= 0);

            // 패시브 스킬 틱
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ItemStack stack = player.getMainHandItem();
                if (!(stack.getItem() instanceof WeaponSkillItem weapon)) continue;
                if (!weapon.canUse(player)) continue;
                for (WeaponSkill skill : weapon.getSkills()) {
                    if (skill.trigger() == WeaponSkill.Trigger.PASSIVE) {
                        skill.onTick(player, stack);
                    }
                }
            }

            // 7단베기 콤보 틱
            tickSevenSlashes(server);

            // ── 활성 필드 틱 처리 ──────────────────────────────
            Iterator<ActiveField> it = activeFields.iterator();
            while (it.hasNext()) {
                ActiveField f = it.next();
                f.timer()[0]--;  // 남은 시간 감소
                double r = f.radius();

                // ── 경계 링 파티클 (매 틱) ──────────────────────────────
                int ringPts = 24;
                for (int i = 0; i < ringPts; i++) {
                    double angle = i * Math.PI * 2.0 / ringPts + (f.timer()[0] * 0.05);
                    double rx = f.center().x + Math.cos(angle) * r;
                    double rz = f.center().z + Math.sin(angle) * r;
                    f.level().sendParticles(ParticleTypes.SNOWFLAKE,
                        rx, f.center().y + 0.1, rz,
                        2, 0.15, 0.4, 0.15, 0.01);
                }
                // 내부 떠다니는 눈 파티클
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double pr = Math.random() * r;
                    f.level().sendParticles(ParticleTypes.SNOWFLAKE,
                        f.center().x + Math.cos(a)*pr,
                        f.center().y + 0.3 + Math.random() * 2.0,
                        f.center().z + Math.sin(a)*pr,
                        1, 0.3, 0.1, 0.3, 0.005);
                }
                // 기둥 파티클 (3틱마다 랜덤 위치)
                if (f.timer()[0] % 3 == 0) {
                    double a = Math.random() * Math.PI * 2;
                    double pr = r * (0.7 + Math.random() * 0.3);
                    f.level().sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        f.center().x + Math.cos(a)*pr,
                        f.center().y, f.center().z + Math.sin(a)*pr,
                        4, 0.1, 1.0, 0.1, 0.01);
                }

                // 데미지 틱
                f.timer()[1]++;
                if (f.timer()[1] >= f.tickInterval()) {
                    f.timer()[1] = 0;
                    AABB box = new AABB(
                        f.center().x - r, f.center().y - 1, f.center().z - r,
                        f.center().x + r, f.center().y + 3, f.center().z + r
                    );
                    List<LivingEntity> targets = f.level().getEntitiesOfClass(
                        LivingEntity.class, box,
                        e -> !e.getUUID().equals(f.ownerUuid()) && e.isAlive()
                    );
                    for (LivingEntity t : targets) {
                        t.hurt(f.level().damageSources().freeze(), f.damagePerTick());
                        t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2, false, false));
                    }
                    // 데미지 틱 연출 - 중앙 폭발 + 기둥들
                    f.level().sendParticles(ParticleTypes.SNOWFLAKE,
                        f.center().x, f.center().y + 0.5, f.center().z,
                        60, r * 0.5, 0.4, r * 0.5, 0.06);
                    f.level().sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        f.center().x, f.center().y + 0.3, f.center().z,
                        30, r * 0.4, 0.2, r * 0.4, 0.12);
                    // 경계에서 안쪽으로 모이는 파티클
                    for (int i = 0; i < 16; i++) {
                        double a = i * Math.PI * 2.0 / 16;
                        f.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            f.center().x + Math.cos(a) * r * 0.8,
                            f.center().y + 0.1,
                            f.center().z + Math.sin(a) * r * 0.8,
                            3, 0.2, 0.3, 0.2, 0.03);
                    }
                }

                if (f.timer()[0] <= 0) it.remove();
            }
        });
    }

    public static boolean tryActivate(Player player, ItemStack stack, WeaponSkill skill, SkillContext ctx) {
        if (!(player instanceof ServerPlayer sp)) return false;

        String key = sp.getUUID() + "_" + skill.name();
        int remaining = cooldowns.getOrDefault(key, 0);
        if (remaining > 0) {
            sp.displayClientMessage(
                Component.literal("§c[" + skill.name() + "] 쿨타임: §e"
                    + String.format("%.1f", remaining / 20.0f) + "초"), true);
            return false;
        }

        skill.execute(player, stack, ctx);

        if (skill.cooldownTicks() > 0) {
            cooldowns.put(key, skill.cooldownTicks());
            WeaponCooldownPayload.send(sp, skill.name(), skill.cooldownTicks(), skill.cooldownTicks());
        }
        return true;
    }

    public static int getRemaining(UUID uuid, String skillName) {
        return cooldowns.getOrDefault(uuid + "_" + skillName, 0);
    }

    /** 외부에서 직접 쿨타임 설정 (FlameKingSwordManager 등에서 사용) */
    public static void setCooldown(UUID uuid, String skillName, int ticks) {
        cooldowns.put(uuid + "_" + skillName, ticks);
    }

    /** 쿨타임 UI 동기화 전송 */
    public static void sendCooldown(net.minecraft.server.level.ServerPlayer player,
                                    String skillName, int total, int remaining) {
        WeaponCooldownPayload.send(player, skillName, total, remaining);
    }

    // ── 염왕 화룡참 (火龍斬) ────────────────────────────────────
    // Phase 0: 제자리 수직 회전 베기 (위→앞→아래→뒤 한 바퀴, 20틱)
    //          플레이어 위치 고정, 시선 기준 전방 부채꼴 범위에 데미지
    //          칼날 궤적 파티클 (수직 원형)
    //          20틱 완료 → 즉시 Phase 1
    // Phase 1: 플레이어 시선 기준 십자(상/하/좌/우) 검기 (즉시)
    private record HwaryongCombo(
        UUID playerUuid,
        ServerLevel level,
        int[] tick,
        int[] phase,
        double[] forwardX, // 시전 시 플레이어 시선 방향 X (고정)
        double[] forwardZ  // 시전 시 플레이어 시선 방향 Z (고정)
    ) {}

    private static final List<HwaryongCombo> activeHwaryong = new ArrayList<>();

    public static void startSevenSlash(net.minecraft.server.level.ServerPlayer player,
                                       ServerLevel level, LivingEntity ignored) {
        // 시전 시 플레이어 시선 방향 고정
        Vec3 look = player.getLookAngle();
        double lLen = Math.sqrt(look.x * look.x + look.z * look.z);
        double fx = lLen > 0.001 ? look.x / lLen : 0;
        double fz = lLen > 0.001 ? look.z / lLen : 1;

        player.displayClientMessage(Component.literal("§c§l🔥 화룡참 - 수직 회전 베기!"), true);
        activeHwaryong.add(new HwaryongCombo(
            player.getUUID(), level,
            new int[]{ 0 }, new int[]{ 0 },
            new double[]{ fx }, new double[]{ fz }
        ));
    }

    private static void tickSevenSlashes(net.minecraft.server.MinecraftServer server) {
        Iterator<HwaryongCombo> it = activeHwaryong.iterator();
        while (it.hasNext()) {
            HwaryongCombo combo = it.next();
            combo.tick()[0]++;
            int t = combo.tick()[0];

            net.minecraft.server.level.ServerPlayer player =
                server.getPlayerList().getPlayer(combo.playerUuid());

            if (player == null) {
                it.remove();
                continue;
            }

            // 고정된 시선 방향
            double fx = combo.forwardX()[0];
            double fz = combo.forwardZ()[0];
            // right 벡터 (수평, 시선의 오른쪽)
            double rx = -fz;
            double rz =  fx;

            // ─────────────────────────────────────────────────────
            //  Phase 0: 제자리 수직 회전 베기 (20틱)
            //  칼날이 위→앞→아래→뒤 순으로 수직 한 바퀴
            //  angle: 0=위, PI/2=앞(전방), PI=아래, 3PI/2=뒤
            // ─────────────────────────────────────────────────────
            if (combo.phase()[0] == 0) {
                double progress = (double) t / 20.0;
                double angle = progress * Math.PI * 2.0; // 0→2π

                // 플레이어 중심 (허리 높이)
                double px = player.getX();
                double py = player.getY() + 1.0;
                double pz = player.getZ();

                // 칼날 끝 위치: 플레이어 앞뒤(수직 원) + 약간 좌우 퍼짐
                // 수직 원: forward 방향이 수평축, UP이 수직축
                double bladeR = 1.8; // 칼날 반경
                // sin(angle): 앞(+)→아래(-)로 앞뒤 이동
                // cos(angle): 위(+)→아래(-)로 상하 이동 (0=위에서 시작)
                double bladeX = px + fx * Math.sin(angle) * bladeR;
                double bladeY = py + Math.cos(angle) * bladeR;
                double bladeZ = pz + fz * Math.sin(angle) * bladeR;

                // ── 수직 원 칼날 궤적 파티클 ──────────────────
                // 현재 칼날 위치 집중 파티클
                combo.level().sendParticles(ParticleTypes.FLAME,
                    bladeX, bladeY, bladeZ, 18, 0.2, 0.2, 0.2, 0.07);
                combo.level().sendParticles(ParticleTypes.LAVA,
                    bladeX, bladeY, bladeZ, 7, 0.15, 0.15, 0.15, 0.05);

                // 칼날 잔상 (이전 8스텝 궤적)
                for (int trail = 1; trail <= 8; trail++) {
                    double ta = angle - trail * (Math.PI * 2.0 / 20.0) * 0.6;
                    double trailX = px + fx * Math.sin(ta) * bladeR;
                    double trailY2 = py + Math.cos(ta) * bladeR;
                    double trailZ = pz + fz * Math.sin(ta) * bladeR;
                    double fade = 1.0 - trail * 0.1;
                    combo.level().sendParticles(ParticleTypes.FLAME,
                        trailX, trailY2, trailZ,
                        (int)(12 * fade), 0.15, 0.15, 0.15, 0.05);
                    if (trail <= 3)
                        combo.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            trailX, trailY2, trailZ,
                            (int)(5 * fade), 0.1, 0.1, 0.1, 0.04);
                }

                // 좌우 날개 파티클 (칼날 폭)
                for (int side = -1; side <= 1; side += 2) {
                    double wx = bladeX + rx * side * 0.4;
                    double wz = bladeZ + rz * side * 0.4;
                    combo.level().sendParticles(ParticleTypes.FLAME,
                        wx, bladeY, wz, 8, 0.1, 0.1, 0.1, 0.06);
                }

                // ── 데미지: 매 3틱, 전방 부채꼴 범위 ──────────
                // 칼날이 전방 하강 구간(앞쪽)에서 데미지 (sin>0 = 앞쪽)
                if (t % 3 == 0 && Math.sin(angle) > -0.3) {
                    // 전방 6칸 부채꼴
                    double sweepR = 4.0;
                    List<LivingEntity> nearby = combo.level().getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(px - sweepR, py - 2.5, pz - sweepR,
                                 px + sweepR, py + 2.5, pz + sweepR),
                        e -> {
                            if (e == player || !e.isAlive()) return false;
                            Vec3 toE = e.position().subtract(player.position());
                            double dot = toE.x * fx + toE.z * fz;
                            return dot > 0 && toE.length() < sweepR;
                        });
                    for (LivingEntity e : nearby) {
                        e.hurt(combo.level().damageSources().playerAttack(player), 14.0f);
                        e.setRemainingFireTicks(60);
                    }
                }

                // 20틱 완료 → 수직 회전 완료 연출 후 즉시 Phase 1
                if (t == 20) {
                    // 완료 폭발 연출
                    combo.level().sendParticles(ParticleTypes.EXPLOSION,
                        px, py, pz, 3, 0.5, 0.3, 0.5, 0);
                    for (int i = 0; i < 24; i++) {
                        double a = i * Math.PI * 2.0 / 24;
                        // 수직 원 링 파티클
                        double lx = px + fx * Math.sin(a) * bladeR;
                        double ly = py + Math.cos(a) * bladeR;
                        double lz = pz + fz * Math.sin(a) * bladeR;
                        combo.level().sendParticles(ParticleTypes.FLAME,
                            lx, ly, lz, 6, 0.1, 0.1, 0.1, 0.08);
                    }
                    combo.level().sendParticles(ParticleTypes.LAVA,
                        px, py, pz, 40, 1.2, 0.5, 1.2, 0.12);

                    player.displayClientMessage(
                        Component.literal("§c§l🔥 1타 - 수직 회전 베기!"), true);
                    combo.phase()[0] = 1;
                    combo.tick()[0] = 0;
                }

            // ─────────────────────────────────────────────────────
            //  Phase 1: 시선 기준 십자(상/하/좌/우) 검기 (즉시, t==1)
            //  플레이어 시선 기준: 상, 하, 좌, 우 4방향
            // ─────────────────────────────────────────────────────
            } else if (combo.phase()[0] == 1 && t == 1) {
                Vec3 pCenter = player.position().add(0, 1.0, 0);

                // 십자 방향: 상(UP), 하(DOWN), 좌(-right), 우(+right)
                // right = look × UP → 시선의 오른쪽
                double[][] dirs = {
                    {  0,  1,  0 },   // 상
                    {  0, -1,  0 },   // 하
                    {  rx, 0,  rz },  // 우
                    { -rx, 0, -rz },  // 좌
                };
                float[] damages = { 40.0f, 40.0f, 40.0f, 40.0f };
                int slashLen = 16; // 8칸 (0.5칸 단위)

                for (int d = 0; d < 4; d++) {
                    double dx = dirs[d][0], dy2 = dirs[d][1], dz2 = dirs[d][2];
                    double dLen = Math.sqrt(dx*dx + dy2*dy2 + dz2*dz2);
                    if (dLen > 0.001) { dx /= dLen; dy2 /= dLen; dz2 /= dLen; }

                    // 검기 파티클 (각 방향으로 뻗어나감)
                    for (int step = 1; step <= slashLen; step++) {
                        double dist = step * 0.5;
                        double bx = pCenter.x + dx * dist;
                        double by = pCenter.y + dy2 * dist;
                        double bz = pCenter.z + dz2 * dist;

                        combo.level().sendParticles(ParticleTypes.FLAME,
                            bx, by, bz, 20, 0.18, 0.18, 0.18, 0.09);
                        combo.level().sendParticles(ParticleTypes.LAVA,
                            bx, by, bz, 8, 0.1, 0.1, 0.1, 0.06);
                        if (step % 2 == 0)
                            combo.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                bx, by, bz, 5, 0.12, 0.12, 0.12, 0.05);
                        if (step % 4 == 0)
                            combo.level().sendParticles(ParticleTypes.EXPLOSION,
                                bx, by, bz, 1, 0, 0, 0, 0);
                    }

                    // 각 방향 타격 (8칸 판정)
                    final double fdx = dx, fdy2 = dy2, fdz2 = dz2;
                    List<LivingEntity> victims = combo.level().getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(pCenter.x - 1, pCenter.y - 2, pCenter.z - 1,
                                 pCenter.x + 1, pCenter.y + 2, pCenter.z + 1)
                            .expandTowards(fdx * 8, fdy2 * 8, fdz2 * 8),
                        e -> e != player && e.isAlive());
                    for (LivingEntity v : victims) {
                        v.hurt(combo.level().damageSources().playerAttack(player), damages[d]);
                        v.setRemainingFireTicks(120);
                    }
                }

                // 중심 폭발 연출
                combo.level().sendParticles(ParticleTypes.EXPLOSION,
                    pCenter.x, pCenter.y, pCenter.z, 5, 1.5, 0.5, 1.5, 0);
                for (int ring = 0; ring < 3; ring++) {
                    double rr = 0.8 + ring * 1.2;
                    for (int i = 0; i < 32; i++) {
                        double a = i * Math.PI * 2.0 / 32;
                        combo.level().sendParticles(ParticleTypes.FLAME,
                            pCenter.x + Math.cos(a) * rr, pCenter.y,
                            pCenter.z + Math.sin(a) * rr, 6, 0.1, 0.4, 0.1, 0.1);
                    }
                }
                combo.level().sendParticles(ParticleTypes.LAVA,
                    pCenter.x, pCenter.y, pCenter.z, 80, 3.0, 0.6, 3.0, 0.18);
                combo.level().sendParticles(ParticleTypes.LARGE_SMOKE,
                    pCenter.x, pCenter.y, pCenter.z, 40, 2.0, 0.4, 2.0, 0.05);

                player.displayClientMessage(
                    Component.literal("§c§l✚ 2타 - 염왕 십자참!"), true);

                it.remove();
                com.example.cosmod.weapon.impl.FlameKingSwordManager.endTransform(player, true);
            } else if (combo.phase()[0] == 1 && t > 5) {
                // 안전장치: 5틱 이상 Phase1에 머물면 강제 종료
                it.remove();
                com.example.cosmod.weapon.impl.FlameKingSwordManager.endTransform(player, true);
            }
        }
    }
}
