package com.example.cosmod.combat;


import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Random;
import java.util.function.Consumer;

public class GearBox extends Item {

    public enum BoxType { WEAPON, ARMOR }

    private final BoxType boxType;

    private static final GearItem[] WEAPON_POOL = new GearItem[5];
    private static final GearItem[] ARMOR_POOL  = new GearItem[12];

    public GearBox(Properties props, BoxType type) {
        super(props);
        this.boxType = type;
    }

    public static void initPools() {
        WEAPON_POOL[0] = CombatItems.DAGGER;
        WEAPON_POOL[1] = CombatItems.SWORD;
        WEAPON_POOL[2] = CombatItems.GREATSWORD;
        WEAPON_POOL[3] = CombatItems.SHORTBOW;
        WEAPON_POOL[4] = CombatItems.LONGBOW;

        // 장세현 세트
        ARMOR_POOL[0] = CombatItems.JSH_HELMET;
        ARMOR_POOL[1] = CombatItems.JSH_CHESTPLATE;
        ARMOR_POOL[2] = CombatItems.JSH_LEGGINGS;
        ARMOR_POOL[3] = CombatItems.JSH_BOOTS;
        // 박준혁 세트
        ARMOR_POOL[4] = CombatItems.PJH_HELMET;
        ARMOR_POOL[5] = CombatItems.PJH_CHESTPLATE;
        ARMOR_POOL[6] = CombatItems.PJH_LEGGINGS;
        ARMOR_POOL[7] = CombatItems.PJH_BOOTS;
        // 유영진 세트
        ARMOR_POOL[8]  = CombatItems.YYJ_HELMET;
        ARMOR_POOL[9]  = CombatItems.YYJ_CHESTPLATE;
        ARMOR_POOL[10] = CombatItems.YYJ_LEGGINGS;
        ARMOR_POOL[11] = CombatItems.YYJ_BOOTS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack heldStack = player.getItemInHand(hand);
        GearItem[] pool = boxType == BoxType.WEAPON ? WEAPON_POOL : ARMOR_POOL;
        Random rng = new Random();
        GearItem picked = pool[rng.nextInt(pool.length)];

        if (picked == null) return InteractionResult.FAIL;

        ItemStack reward = GearItem.createWithOptions(picked);

        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }

        level.playSound(null, player.blockPosition(),
            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
            SoundSource.PLAYERS, 0.5f, 1.2f);

        String typeName = boxType == BoxType.WEAPON ? "무기" : "방어구";
        String itemName = reward.getHoverName().getString();
        player.displayClientMessage(
            Component.literal("§6[" + typeName + " 상자] §f" + itemName + " §7획득!"), false);

        heldStack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                TooltipDisplay display,
                                Consumer<Component> consumer,
                                TooltipFlag flag) {
        consumer.accept(Component.literal("§e우클릭으로 개봉"));
        if (boxType == BoxType.WEAPON) {
            consumer.accept(Component.literal("§7획득 가능: §f단검, 한손검, 대검, 단궁, 장궁"));
        } else {
            consumer.accept(Component.literal("§7획득 가능: §f철갑/암흑 방어구 세트"));
        }
        consumer.accept(Component.literal("§a메인 옵션 2개 랜덤 부여"));
    }
}
