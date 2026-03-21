package com.example.cosmod.weapon;

import com.example.cosmod.job.JobClass;
import com.example.cosmod.combat.WeaponGrade;
import com.example.cosmod.combat.GearItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
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

    public WeaponSkill getSkillForPlayer(Player player, WeaponSkill.Trigger trigger) {
        List<WeaponSkill> skills = (this instanceof com.example.cosmod.weapon.impl.FlameKingSword fks)
            ? fks.getSkillsForPlayer(player)
            : getSkills();
        for (WeaponSkill skill : skills)
            if (skill.trigger() == trigger) return skill;
        return null;
    }

    public boolean canUse(Player player) { return true; }

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
        if (skill != null) WeaponSkillManager.tryActivate(player, stack, skill, SkillContext.EMPTY);
        return InteractionResult.SUCCESS;
    }

    public static ItemStack createWithPrefix(WeaponSkillItem item) {
        ItemStack stack = new ItemStack(item);
        java.util.Random rng = new java.util.Random();
        WeaponGrade grade = WeaponGrade.roll(rng);
        String weaponName = item.getWeaponName();
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(grade.formatName(weaponName, 0)));
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isGear", true);
        tag.putString("grade", grade.name());
        tag.putInt("enhance_level", 0);
        tag.putString("base_name", weaponName);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.literal("§7" + getWeaponLore()));
        consumer.accept(Component.literal(""));
        consumer.accept(Component.literal("§f기본 공격력: §c" + baseDamage));
        CustomData wcd = stack.get(DataComponents.CUSTOM_DATA);
        if (wcd != null) {
            CompoundTag wTag = wcd.copyTag();
            if (wTag.contains("grade")) {
                try {
                    WeaponGrade p = WeaponGrade.valueOf(wTag.getString("grade").orElse("NORMAL"));
                    consumer.accept(Component.literal(p.color + "■ " + p.displayName + " 등급"));
                    if (p.atkPercent > 0) consumer.accept(Component.literal("§f  공격력 +" + p.atkPercent + "%"));
                    if (p.atkFlat > 0)    consumer.accept(Component.literal("§f  공격력 +" + p.atkFlat));
                } catch (Exception ignored) {}
            }
            int enhLevel = wTag.contains("enhance_level") ? wTag.getInt("enhance_level").orElse(0) : 0;
            if (enhLevel > 0) consumer.accept(Component.literal("§e강화 단계: §f+" + enhLevel));
        }
        consumer.accept(Component.literal(""));
        for (WeaponSkill skill : getSkills()) {
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
                String cdStr = skill.cooldownTicks() > 0 ? " §8(쿨: " + (skill.cooldownTicks() / 20.0f) + "초)" : "";
                consumer.accept(Component.literal(tl + " §f" + skill.name() + cdStr));
            }
            for (String line : skill.description().split("\\|"))
                consumer.accept(Component.literal("  §7" + line.trim()));
        }
    }
}
