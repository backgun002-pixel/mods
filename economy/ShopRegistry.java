package com.example.cosmod.economy;

import com.example.cosmod.combat.CombatItems;
import com.example.cosmod.item.CosmodItems;
import com.example.cosmod.crop.CosmodCrops;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 상점에 등록된 전체 아이템 목록 + 가격 관리
 * 서버 기동 시 한 번 초기화, 이후 거래마다 수요/공급 반영
 */
public class ShopRegistry {

    private static final List<ShopEntry> ENTRIES = new ArrayList<>();

    public static void init() {
        ENTRIES.clear();

        // ── 농작물 (CROP) ──────────────────────────────────────────
        add(Items.WHEAT,         ShopEntry.Category.CROP,  8,  0.4f);
        add(Items.CARROT,        ShopEntry.Category.CROP,  6,  0.4f);
        add(Items.POTATO,        ShopEntry.Category.CROP,  5,  0.4f);
        add(Items.BEETROOT,      ShopEntry.Category.CROP,  7,  0.4f);
        add(Items.MELON_SLICE,   ShopEntry.Category.CROP,  4,  0.3f);
        add(Items.PUMPKIN,       ShopEntry.Category.CROP, 12,  0.3f);
        add(Items.SUGAR_CANE,    ShopEntry.Category.CROP,  3,  0.2f);

        // ── 커스텀 작물 ────────────────────────────────────────────
        add(CosmodCrops.TOMATO,  ShopEntry.Category.CROP, 15, 0.5f);
        add(CosmodCrops.PEPPER,  ShopEntry.Category.CROP, 18, 0.5f);
        add(CosmodCrops.CORN,    ShopEntry.Category.CROP, 12, 0.4f);

        // ── 광물 (ORE) ────────────────────────────────────────────
        add(Items.IRON_INGOT,    ShopEntry.Category.ORE,  20,  0.5f);
        add(Items.GOLD_INGOT,    ShopEntry.Category.ORE,  35,  0.5f);
        add(Items.DIAMOND,       ShopEntry.Category.ORE, 100,  0.6f);
        add(Items.EMERALD,       ShopEntry.Category.ORE,  80,  0.6f);
        add(Items.LAPIS_LAZULI,  ShopEntry.Category.ORE,  10,  0.3f);
        add(Items.REDSTONE,      ShopEntry.Category.ORE,   5,  0.2f);
        add(Items.COAL,          ShopEntry.Category.ORE,   3,  0.2f);
        add(Items.NETHERITE_INGOT, ShopEntry.Category.ORE, 500, 0.8f);

        // ── 코디 아이템 (구매 전용) ───────────────────────────────
        add(CosmodItems.COSME_HAT,   ShopEntry.Category.COSME, 200, 0.1f);
        add(CosmodItems.COSME_SHIRT, ShopEntry.Category.COSME, 250, 0.1f);
        add(CosmodItems.COSME_PANTS, ShopEntry.Category.COSME, 220, 0.1f);
        add(CosmodItems.COSME_SHOES, ShopEntry.Category.COSME, 180, 0.1f);

        // ── 랜덤 상자 ─────────────────────────────────────────────
        add(CombatItems.WEAPON_BOX, ShopEntry.Category.WEAPON, 500, 0.1f);
        add(CombatItems.ARMOR_BOX,  ShopEntry.Category.ARMOR,  500, 0.1f);
        add(CombatItems.ENHANCE_STONE, ShopEntry.Category.WEAPON, 150, 0.2f);
    }

    private static void add(net.minecraft.world.item.Item item,
                            ShopEntry.Category cat, int price, float factor) {
        ENTRIES.add(new ShopEntry(item, cat, price, factor));
    }

    public static List<ShopEntry> getAll() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static List<ShopEntry> getByCategory(ShopEntry.Category cat) {
        return ENTRIES.stream().filter(e -> e.getCategory() == cat).toList();
    }

    /** 아이템으로 ShopEntry 검색 */
    public static ShopEntry findByItem(net.minecraft.world.item.Item item) {
        return ENTRIES.stream().filter(e -> e.getItem() == item).findFirst().orElse(null);
    }
}
