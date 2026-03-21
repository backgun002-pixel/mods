package com.example.cosmod.weapon;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class WeaponAttackHandler {

    public static int countWeapons(net.minecraft.world.entity.player.Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() instanceof WeaponSkillItem) count++;
        }
        return count;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) return InteractionResult.PASS;
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof WeaponSkillItem weapon)) return InteractionResult.PASS;
            if (countWeapons(player) > 2) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§c무기는 최대 2개까지만 보유할 수 있습니다!"), true);
                return InteractionResult.PASS;
            }
            if (!weapon.canUse(player)) return InteractionResult.PASS;
            WeaponSkill.Trigger trigger = player.isShiftKeyDown()
                ? WeaponSkill.Trigger.SHIFT_LEFT_CLICK
                : WeaponSkill.Trigger.LEFT_CLICK;
            WeaponSkill skill = weapon.getSkillForPlayer(player, trigger);
            if (skill == null) return InteractionResult.PASS;
            LivingEntity target = entity instanceof LivingEntity le ? le : null;
            boolean activated = WeaponSkillManager.tryActivate(
                player, stack, skill, new SkillContext(target));
            return activated ? InteractionResult.FAIL : InteractionResult.PASS;
        });
    }
}
