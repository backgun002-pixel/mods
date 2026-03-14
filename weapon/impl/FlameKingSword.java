package com.example.cosmod.weapon.impl;

import com.example.cosmod.job.JobClass;
import com.example.cosmod.weapon.SkillContext;
import com.example.cosmod.weapon.WeaponSkill;
import com.example.cosmod.weapon.WeaponSkillItem;
import com.example.cosmod.weapon.WeaponSkillManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FlameKingSword extends WeaponSkillItem {

    // ══════════════════════════════════════════════════════
    //  일반 형태 스킬셋
    // ══════════════════════════════════════════════════════
    public static final List<WeaponSkill> NORMAL_SKILLS = List.of(

        new WeaponSkill() {
            @Override public String name() { return "화염 참격"; }
            @Override public Trigger trigger() { return Trigger.LEFT_CLICK; }
            @Override public int cooldownTicks() { return 30; }
            @Override public String description() { return "피해 8|점화 3초"; }
            @Override public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!ctx.hasTarget()) return;
                LivingEntity target = ctx.getTarget();
                target.hurt(player.level().damageSources().playerAttack(player), 8.0f);
                target.setRemainingFireTicks(60);
                if (player.level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY()+1, target.getZ(), 25, 0.3, 0.5, 0.3, 0.08);
                player.displayClientMessage(Component.literal("§c화염 참격!"), true);
            }
        },

        new WeaponSkill() {
            @Override public String name() { return "불꽃 폭풍"; }
            @Override public Trigger trigger() { return Trigger.RIGHT_CLICK; }
            @Override public int cooldownTicks() { return 100; }
            @Override public String description() { return "전방 부채꼴, 피해 12|점화"; }
            @Override public void execute(Player player, ItemStack stack, SkillContext ctx) {
                Vec3 look = player.getLookAngle();
                List<LivingEntity> targets = player.level().getEntitiesOfClass(
                    LivingEntity.class, new AABB(player.blockPosition()).inflate(5.0),
                    e -> {
                        if (e == player || !e.isAlive()) return false;
                        return look.dot(e.position().subtract(player.position()).normalize()) > 0.5;
                    });
                for (LivingEntity t : targets) {
                    t.hurt(player.level().damageSources().playerAttack(player), 12.0f);
                    t.setRemainingFireTicks(80);
                }
                if (player.level() instanceof ServerLevel sl)
                    for (int i = 0; i < 8; i++) {
                        double d = i * 0.6;
                        sl.sendParticles(ParticleTypes.FLAME,
                            player.getX()+look.x*d, player.getY()+1, player.getZ()+look.z*d,
                            6, 0.25, 0.2, 0.25, 0.05);
                    }
                player.displayClientMessage(Component.literal("§c불꽃 폭풍! §f("+targets.size()+"명)"), true);
            }
        },

        // 쉬프트+우클릭: 폭염 형태 전환 (쿨타임은 WeaponSkillManager에서 관리)
        new WeaponSkill() {
            @Override public String name() { return "폭염 형태"; }
            @Override public Trigger trigger() { return Trigger.SHIFT_RIGHT_CLICK; }
            @Override public int cooldownTicks() { return 900; } // 45초 - tryActivate가 쿨 체크
            @Override public String description() { return "30초간 폭염 형태 전환|공격속도 증가 · 스킬 변경"; }
            @Override public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!(player instanceof ServerPlayer sp)) return;
                if (FlameKingSwordManager.isTransformed(sp.getUUID())) {
                    sp.displayClientMessage(Component.literal("§c이미 폭염 형태입니다."), true);
                    return;
                }
                if (player.level() instanceof ServerLevel sl) {
                    for (int i = 0; i < 20; i++) {
                        double angle = i * Math.PI * 2.0 / 20;
                        sl.sendParticles(ParticleTypes.FLAME,
                            player.getX()+Math.cos(angle)*0.8, player.getY()+1.0,
                            player.getZ()+Math.sin(angle)*0.8, 3, 0.1, 0.3, 0.1, 0.1);
                    }
                    sl.sendParticles(ParticleTypes.LAVA,
                        player.getX(), player.getY()+0.5, player.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
                }
                FlameKingSwordManager.startTransform(sp);
            }
        },

        new WeaponSkill() {
            @Override public String name() { return "불꽃의 가호"; }
            @Override public Trigger trigger() { return Trigger.PASSIVE; }
            @Override public int cooldownTicks() { return 0; }
            @Override public String description() { return "장착 중 불 피해 면역"; }
            @Override public void execute(Player p, ItemStack s, SkillContext c) {}
            @Override public void onTick(Player player, ItemStack stack) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
            }
        }
    );

    // ══════════════════════════════════════════════════════
    //  폭염(변신) 형태 스킬셋
    // ══════════════════════════════════════════════════════
    public static final List<WeaponSkill> THIN_SKILLS = List.of(

        // 좌클릭: 섬광 참격 (빠른 2연격)
        new WeaponSkill() {
            @Override public String name() { return "뇌화 참격"; }
            @Override public Trigger trigger() { return Trigger.LEFT_CLICK; }
            @Override public int cooldownTicks() { return 15; }
            @Override public String description() { return "피해 5 × 2연격|점화"; }
            @Override public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!ctx.hasTarget()) return;
                LivingEntity target = ctx.getTarget();
                target.hurt(player.level().damageSources().playerAttack(player), 5.0f);
                target.hurt(player.level().damageSources().playerAttack(player), 5.0f);
                target.setRemainingFireTicks(40);
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY()+1, target.getZ(), 15, 0.2, 0.3, 0.2, 0.12);
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY()+0.5, target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                }
                player.displayClientMessage(Component.literal("§e⚡ 뇌화 참격!"), true);
            }
        },

        // 우클릭: 화염 쇄도 (전방 돌진)
        new WeaponSkill() {
            @Override public String name() { return "화염 쇄도"; }
            @Override public Trigger trigger() { return Trigger.RIGHT_CLICK; }
            @Override public int cooldownTicks() { return 60; }
            @Override public String description() { return "전방 4칸 돌진, 경로 피해 10|경로 위 적 점화"; }
            @Override public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!(player.level() instanceof ServerLevel sl)) return;
                Vec3 look = player.getLookAngle().normalize();
                List<LivingEntity> hit = new ArrayList<>();
                for (int step = 1; step <= 8; step++) {
                    double d = step * 0.5;
                    Vec3 pos = player.position().add(look.x*d, 0, look.z*d);
                    List<LivingEntity> nearby = sl.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(pos.x-0.8, pos.y-0.5, pos.z-0.8, pos.x+0.8, pos.y+2.0, pos.z+0.8),
                        e -> e != player && e.isAlive() && !hit.contains(e));
                    for (LivingEntity t : nearby) {
                        t.hurt(sl.damageSources().playerAttack(player), 10.0f);
                        t.setRemainingFireTicks(60);
                        hit.add(t);
                    }
                    sl.sendParticles(ParticleTypes.FLAME,
                        pos.x, pos.y+0.5, pos.z, 4, 0.2, 0.2, 0.2, 0.05);
                }
                Vec3 dest = player.position().add(look.x*4, 0, look.z*4);
                player.teleportTo(dest.x, dest.y, dest.z);
                sl.sendParticles(ParticleTypes.FLAME, dest.x, dest.y+0.5, dest.z, 40, 1.0, 0.3, 1.0, 0.08);
                sl.sendParticles(ParticleTypes.LAVA,  dest.x, dest.y+0.5, dest.z, 15, 0.5, 0.3, 0.5, 0.05);
                player.displayClientMessage(Component.literal("§c🔥 화염 쇄도! §f("+hit.size()+"명)"), true);
            }
        },

        // 쉬프트+우클릭: 비기 - 화룡참 (수직 회전 베기 → 십자 검기)
        // 쿨타임 없음 (사용하면 바로 원형태 복귀 + 폭염 쿨타임 시작)
        new WeaponSkill() {
            @Override public String name() { return "화룡참"; }
            @Override public Trigger trigger() { return Trigger.SHIFT_RIGHT_CLICK; }
            @Override public int cooldownTicks() { return 0; }
            @Override public String description() { return "제자리 수직 회전 베기 후|시선 기준 상하좌우 십자 검기 · 시전 후 원형태 복귀"; }

            @Override
            public void execute(Player player, ItemStack stack, SkillContext ctx) {
                if (!(player instanceof ServerPlayer sp)) return;
                if (!(player.level() instanceof ServerLevel sl)) return;

                // 타겟 불필요 - 시선 방향 기반으로 동작
                WeaponSkillManager.startSevenSlash(sp, sl, null);
            }
        },

        // 패시브: 폭염의 심장 (강화)
        new WeaponSkill() {
            @Override public String name() { return "폭염의 심장"; }
            @Override public Trigger trigger() { return Trigger.PASSIVE; }
            @Override public int cooldownTicks() { return 0; }
            @Override public String description() { return "불 피해 면역 · 이동속도 소폭 증가"; }
            @Override public void execute(Player p, ItemStack s, SkillContext c) {}
            @Override public void onTick(Player player, ItemStack stack) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false));
            }
        }
    );

    public FlameKingSword(Properties props) { super(props, JobClass.WARRIOR, 12); }

    @Override
    public List<WeaponSkill> getSkills() { return NORMAL_SKILLS; }

    public List<WeaponSkill> getSkillsForPlayer(Player player) {
        if (player instanceof ServerPlayer sp &&
            FlameKingSwordManager.isTransformed(sp.getUUID())) {
            return THIN_SKILLS;
        }
        return NORMAL_SKILLS;
    }

    // ── 툴팁: 일반 스킬 + 폭염 스킬 모두 표시 ───────────────────
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.literal("§7" + getWeaponLore()));
        consumer.accept(Component.literal(""));
        consumer.accept(Component.literal("§f기본 공격력: §c" + getBaseDamage()));
        if (getRequiredJob() != null)
            consumer.accept(Component.literal("§f필요 직업: §e" + getRequiredJob().displayName));
        consumer.accept(Component.literal(""));

        // 일반 형태
        consumer.accept(Component.literal("§6▶ 일반 형태"));
        appendSkillList(NORMAL_SKILLS, consumer);

        // 폭염 형태
        consumer.accept(Component.literal(""));
        consumer.accept(Component.literal("§c▶ 폭염 형태 §7(쉬프트+우클릭으로 전환)"));
        appendSkillList(THIN_SKILLS, consumer);
    }

    private void appendSkillList(List<WeaponSkill> skills, Consumer<Component> consumer) {
        for (WeaponSkill skill : skills) {
            if (skill.trigger() == WeaponSkill.Trigger.PASSIVE) {
                consumer.accept(Component.literal("§b[패시브] §f" + skill.name()));
            } else {
                String tl = switch (skill.trigger()) {
                    case LEFT_CLICK        -> "§a[좌클릭]";
                    case RIGHT_CLICK       -> "§a[우클릭]";
                    case SHIFT_RIGHT_CLICK -> "§6[쉬프트+우클릭]";
                    default -> "";
                };
                String cd = skill.cooldownTicks() > 0
                    ? " §8(쿨: " + (skill.cooldownTicks() / 20.0f) + "초)" : " §8(쿨: 없음)";
                consumer.accept(Component.literal(tl + " §f" + skill.name() + cd));
            }
            for (String line : skill.description().split("\\|"))
                consumer.accept(Component.literal("  §7" + line.trim()));
        }
    }

    @Override public String getWeaponName() { return "염왕검"; }
    @Override public String getWeaponLore() { return "불꽃의 왕이 사용했던 전설의 대검."; }
}
