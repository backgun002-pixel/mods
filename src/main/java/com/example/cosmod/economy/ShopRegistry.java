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
        entries.add(new ShopEntry(Items.DIAMOND,          ShopEntry.Category.ORE,    5,   0.5f));
        entries.add(new ShopEntry(Items.EMERALD,          ShopEntry.Category.ORE,    3,   0.5f));
        entries.add(new ShopEntry(Items.IRON_INGOT,       ShopEntry.Category.ORE,    1,   0.3f));
        entries.add(new ShopEntry(Items.GOLD_INGOT,       ShopEntry.Category.ORE,    2,   0.4f));

        // 던전 티켓
        entries.add(new ShopEntry(DungeonItems.DUNGEON_TICKET, ShopEntry.Category.ORE, 50, 0.1f));

        // 강화석 3종
        entries.add(new ShopEntry(CombatItems.ENHANCE_STONE_BASIC, ShopEntry.Category.ORE,  20, 0.1f));
        entries.add(new ShopEntry(CombatItems.ENHANCE_STONE_MID,   ShopEntry.Category.ORE,  50, 0.1f));
        entries.add(new ShopEntry(CombatItems.ENHANCE_STONE_HIGH,  ShopEntry.Category.ORE, 120, 0.1f));

        // 보석 3종
        entries.add(new ShopEntry(CombatItems.GEM_BASIC, ShopEntry.Category.ORE,  30, 0.1f));
        entries.add(new ShopEntry(CombatItems.GEM_MID,   ShopEntry.Category.ORE,  80, 0.1f));
        entries.add(new ShopEntry(CombatItems.GEM_HIGH,  ShopEntry.Category.ORE, 200, 0.1f));

        // 코스모드 아이템
        entries.add(new ShopEntry(CosmodItems.RUBY,        ShopEntry.Category.ORE, 100, 0.3f));
        entries.add(new ShopEntry(CosmodItems.SAPPHIRE,    ShopEntry.Category.ORE, 100, 0.3f));
        entries.add(new ShopEntry(CosmodItems.RED_DIAMOND, ShopEntry.Category.ORE, 300, 0.5f));
    }

    public static List<ShopEntry> getEntries() { return entries; }

    public static ShopEntry findByItem(net.minecraft.world.item.Item item) {
        for (ShopEntry e : entries)
            if (e.getItem() == item) return e;
        return null;
    }

    public static List<ShopEntry> getByCategory(ShopEntry.Category cat) {
        List<ShopEntry> result = new ArrayList<>();
        for (ShopEntry e : entries)
            if (e.getCategory() == cat) result.add(e);
        return result;
    }
}
