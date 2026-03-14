package com.example.cosmod.weapon;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class WeaponAttackHandler {

    public static void register() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            // 1.21.11: isClientSide() 메서드 호출
            if (level.isClientSide()) return InteractionResult.PASS;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof WeaponSkillItem weapon)) return InteractionResult.PASS;
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
