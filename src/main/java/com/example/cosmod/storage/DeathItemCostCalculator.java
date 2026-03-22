package com.example.cosmod.storage;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import java.util.List;

/** 아이템 희귀도/종류별 회수 비용 계산 */
public class DeathItemCostCalculator {

    public static int calculateCost(List<ItemStack> items) {
        int total = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                total += getItemCost(stack) * stack.getCount();
            }
        }
        // 최소 10코인, 최대 5000코인
        return Math.max(10, Math.min(5000, total));
    }

    private static int getItemCost(ItemStack stack) {
        var item = stack.getItem();

        // ── 특수 아이템 ───────────────────────────────────────────
        if (item == Items.NETHER_STAR)       return 500;
        if (item == Items.ELYTRA)            return 400;
        if (item == Items.BEACON)            return 300;
        if (item == Items.DRAGON_EGG)        return 500;
        if (item == Items.TOTEM_OF_UNDYING)  return 200;
        if (item == Items.ENCHANTED_GOLDEN_APPLE) return 150;

        // ── 네더라이트 ────────────────────────────────────────────
        if (item == Items.NETHERITE_SWORD    || item == Items.NETHERITE_AXE)     return 120;
        if (item == Items.NETHERITE_HELMET   || item == Items.NETHERITE_CHESTPLATE
         || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS)   return 100;
        if (item == Items.NETHERITE_PICKAXE  || item == Items.NETHERITE_SHOVEL
         || item == Items.NETHERITE_HOE)                                          return 80;
        if (item == Items.NETHERITE_INGOT)   return 60;
        if (item == Items.NETHERITE_SCRAP)   return 30;

        // ── 다이아몬드 ────────────────────────────────────────────
        if (item == Items.DIAMOND_SWORD      || item == Items.DIAMOND_AXE)       return 50;
        if (item == Items.DIAMOND_HELMET     || item == Items.DIAMOND_CHESTPLATE
         || item == Items.DIAMOND_LEGGINGS   || item == Items.DIAMOND_BOOTS)     return 40;
        if (item == Items.DIAMOND_PICKAXE    || item == Items.DIAMOND_SHOVEL
         || item == Items.DIAMOND_HOE)                                            return 30;
        if (item == Items.DIAMOND)           return 20;

        // ── 황금 ──────────────────────────────────────────────────
        if (item == Items.GOLDEN_SWORD       || item == Items.GOLDEN_AXE)        return 15;
        if (item == Items.GOLDEN_HELMET      || item == Items.GOLDEN_CHESTPLATE
         || item == Items.GOLDEN_LEGGINGS    || item == Items.GOLDEN_BOOTS)      return 12;
        if (item == Items.GOLD_INGOT)        return 8;
        if (item == Items.GOLD_BLOCK)        return 72;

        // ── 철 ────────────────────────────────────────────────────
        if (item == Items.IRON_SWORD         || item == Items.IRON_AXE)          return 10;
        if (item == Items.IRON_HELMET        || item == Items.IRON_CHESTPLATE
         || item == Items.IRON_LEGGINGS      || item == Items.IRON_BOOTS)        return 8;
        if (item == Items.IRON_PICKAXE       || item == Items.IRON_SHOVEL
         || item == Items.IRON_HOE)                                               return 6;
        if (item == Items.IRON_INGOT)        return 3;

        // ── 희귀도 기반 기본값 ─────────────────────────────────────
        Rarity rarity = stack.getRarity();
        return switch (rarity) {
            case EPIC   -> 30;
            case RARE   -> 15;
            case UNCOMMON -> 5;
            default     -> 1;
        };
    }
}
