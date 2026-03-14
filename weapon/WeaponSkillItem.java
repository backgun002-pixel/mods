package com.example.cosmod.weapon;

import com.example.cosmod.job.JobClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public abstract class WeaponSkillItem extends Item {

    private final JobClass requiredJob;
    private final int baseDamage;

    public WeaponSkillItem(Properties props, JobClass requiredJob, int baseDamage) {
        super(props);
        this.requiredJob = requiredJob;
        this.baseDamage = baseDamage;
    }

    public abstract List<WeaponSkill> getSkills();
    public abstract String getWeaponName();
    public abstract String getWeaponLore();

    public JobClass getRequiredJob() { return requiredJob; }
    public int getBaseDamage() { return baseDamage; }

    public WeaponSkill getSkill(WeaponSkill.Trigger trigger) {
        for (WeaponSkill skill : getSkills())
            if (skill.trigger() == trigger) return skill;
        return null;
    }

    /** 형태 전환 무기(FlameKingSword 등)를 위한 플레이어 기준 스킬 조회 */
    public WeaponSkill getSkillForPlayer(Player player, WeaponSkill.Trigger trigger) {
        List<WeaponSkill> skills = (this instanceof com.example.cosmod.weapon.impl.FlameKingSword fks)
            ? fks.getSkillsForPlayer(player)
            : getSkills();
        for (WeaponSkill skill : skills)
            if (skill.trigger() == trigger) return skill;
        return null;
    }

    public boolean canUse(Player player) {
        if (requiredJob == null) return true;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return false;
        var data = com.example.cosmod.job.PlayerJobManager.get(sp);
        return data.getCombatJob() == requiredJob;
    }

    // 1.21.11: use() 반환타입 InteractionResult, isClientSide() 메서드 사용
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);

        if (!canUse(player)) {
            player.displayClientMessage(
                Component.literal("§c[" + getWeaponName() + "] §f전투 직업 §e" +
                    (requiredJob != null ? requiredJob.displayName : "?") + "§f이(가) 필요합니다."), true);
            return InteractionResult.FAIL;
        }

        WeaponSkill.Trigger trigger = player.isShiftKeyDown()
            ? WeaponSkill.Trigger.SHIFT_RIGHT_CLICK
            : WeaponSkill.Trigger.RIGHT_CLICK;

        WeaponSkill skill = getSkillForPlayer(player, trigger);
        if (skill != null) {
            WeaponSkillManager.tryActivate(player, stack, skill, SkillContext.EMPTY);
        }

        return InteractionResult.SUCCESS;
    }

    // 1.21.11: appendHoverText 시그니처 (TooltipDisplay + Consumer)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.literal("§7" + getWeaponLore()));
        consumer.accept(Component.literal(""));
        consumer.accept(Component.literal("§f기본 공격력: §c" + baseDamage));
        if (requiredJob != null)
            consumer.accept(Component.literal("§f필요 직업: §e" + requiredJob.displayName));
        consumer.accept(Component.literal(""));

        // 형태 전환 무기는 현재 형태 스킬셋 표시
        List<WeaponSkill> displaySkills = getSkills();
        for (WeaponSkill skill : displaySkills) {
            if (skill.trigger() == WeaponSkill.Trigger.PASSIVE) {
                consumer.accept(Component.literal("§b[패시브] §f" + skill.name()));
            } else {
                String tl = switch (skill.trigger()) {
                    case LEFT_CLICK        -> "§a[좌클릭]";
                    case RIGHT_CLICK       -> "§a[우클릭]";
                    case SHIFT_LEFT_CLICK  -> "§6[쉬프트+좌클릭]";
                    case SHIFT_RIGHT_CLICK -> "§6[쉬프트+우클릭]";
                    default -> "";
                };
                String cd = skill.cooldownTicks() > 0
                    ? " §8(쿨: " + (skill.cooldownTicks() / 20.0f) + "초)" : "";
                consumer.accept(Component.literal(tl + " §f" + skill.name() + cd));
            }
            // | 로 구분된 설명을 여러 줄로 출력 (CC효과 아랫줄)
            for (String line : skill.description().split("\\|")) {
                consumer.accept(Component.literal("  §7" + line.trim()));
            }
        }
    }
}
