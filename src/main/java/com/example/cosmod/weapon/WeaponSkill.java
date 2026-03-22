package com.example.cosmod.weapon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface WeaponSkill {

    enum Trigger {
        LEFT_CLICK,
        RIGHT_CLICK,
        SHIFT_LEFT_CLICK,
        SHIFT_RIGHT_CLICK,
        PASSIVE
    }

    String name();
    Trigger trigger();
    int cooldownTicks();
    String description();

    void execute(Player player, ItemStack stack, SkillContext context);

    default void onTick(Player player, ItemStack stack) {}
}
