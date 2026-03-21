package com.example.cosmod.economy;

import com.example.cosmod.combat.CombatItems;
import com.example.cosmod.dungeon.DungeonItems;
import com.example.cosmod.item.CosmodItems;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ShopRegistry {

    private static final List<ShopEntry> entries = new ArrayList<>();

    public static void init() {
        entries.clear();

        // 기본 재료
        entries.add(new ShopEntry(Items.DIAMOND,         5,   "다이아몬드"));
        entries.add(new ShopEntry(Items.EMERALD,         3,   "에메랄드"));
        entries.add(new ShopEntry(Items.IRON_INGOT,      1,   "철 주괴"));
        entries.add(new ShopEntry(Items.GOLD_INGOT,      2,   "금 주괴"));

        // 던전 티켓
        entries.add(new ShopEntry(DungeonItems.DUNGEON_TICKET, 50, "던전 입장권"));

        // 강화석 3종
        entries.add(new ShopEntry(CombatItems.ENHANCE_STONE_BASIC, 20, "기본 강화석 (성공률+0%)"));
        entries.add(new ShopEntry(CombatItems.ENHANCE_STONE_MID,   50, "중급 강화석 (성공률+10%)"));
        entries.add(new ShopEntry(CombatItems.ENHANCE_STONE_HIGH, 120, "고급 강화석 (성공률+20%)"));

        // 보석 3종
        entries.add(new ShopEntry(CombatItems.GEM_BASIC,  30, "하급 보석 (2줄 옵션)"));
        entries.add(new ShopEntry(CombatItems.GEM_MID,    80, "중급 보석 (2줄 옵션+)"));
        entries.add(new ShopEntry(CombatItems.GEM_HIGH,  200, "상급 보석 (3줄 옵션)"));

        // 코스모드 아이템
        entries.add(new ShopEntry(CosmodItems.RUBY,       100, "루비"));
        entries.add(new ShopEntry(CosmodItems.SAPPHIRE,   100, "사파이어"));
        entries.add(new ShopEntry(CosmodItems.RED_DIAMOND,300, "레드 다이아몬드"));
    }

    public static List<ShopEntry> getEntries() { return entries; }
}
