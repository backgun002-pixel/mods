package com.example.cosmod.combat;

import com.example.cosmod.CosmodMod;
import com.example.cosmod.block.EnhanceTableBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class CombatItems {

    // ── 무기 ──────────────────────────────────────────────────────
    public static GearItem DAGGER;
    public static GearItem SWORD;
    public static GearItem GREATSWORD;
    public static GearItem SHORTBOW;
    public static GearItem LONGBOW;

    // ── 장세현 세트 ───────────────────────────────────────────────
    public static GearItem JSH_HELMET;
    public static GearItem JSH_CHESTPLATE;
    public static GearItem JSH_LEGGINGS;
    public static GearItem JSH_BOOTS;

    // ── 박준혁 세트 ───────────────────────────────────────────────
    public static GearItem PJH_HELMET;
    public static GearItem PJH_CHESTPLATE;
    public static GearItem PJH_LEGGINGS;
    public static GearItem PJH_BOOTS;

    // ── 유영진 세트 ───────────────────────────────────────────────
    public static GearItem YYJ_HELMET;
    public static GearItem YYJ_CHESTPLATE;
    public static GearItem YYJ_LEGGINGS;
    public static GearItem YYJ_BOOTS;

    // ── 기존 호환 별칭 ────────────────────────────────────────────
    public static GearItem IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS;
    public static GearItem DARK_HELMET, DARK_CHESTPLATE, DARK_LEGGINGS, DARK_BOOTS;

    // ── 기타 ──────────────────────────────────────────────────────
    public static Item              ENHANCE_STONE;
    public static EnhanceTableBlock ENHANCE_TABLE;
    public static GearBox           WEAPON_BOX;
    public static GearBox           ARMOR_BOX;

    public static void register() {
        // 무기
        DAGGER     = gear("dagger",     GearItem.GearType.WEAPON, null);
        SWORD      = gear("sword",      GearItem.GearType.WEAPON, null);
        GREATSWORD = gear("greatsword", GearItem.GearType.WEAPON, null);
        SHORTBOW   = gear("shortbow",   GearItem.GearType.WEAPON, null);
        LONGBOW    = gear("longbow",    GearItem.GearType.WEAPON, null);

        // 장세현 세트
        JSH_HELMET     = gear("iron_gear_helmet",     GearItem.GearType.HELMET,     SetBonus.JSH);
        JSH_CHESTPLATE = gear("iron_gear_chestplate", GearItem.GearType.CHESTPLATE, SetBonus.JSH);
        JSH_LEGGINGS   = gear("iron_gear_leggings",   GearItem.GearType.LEGGINGS,   SetBonus.JSH);
        JSH_BOOTS      = gear("iron_gear_boots",      GearItem.GearType.BOOTS,      SetBonus.JSH);
        IRON_HELMET = JSH_HELMET; IRON_CHESTPLATE = JSH_CHESTPLATE;
        IRON_LEGGINGS = JSH_LEGGINGS; IRON_BOOTS = JSH_BOOTS;

        // 박준혁 세트
        PJH_HELMET     = gear("dark_helmet",     GearItem.GearType.HELMET,     SetBonus.PJH);
        PJH_CHESTPLATE = gear("dark_chestplate", GearItem.GearType.CHESTPLATE, SetBonus.PJH);
        PJH_LEGGINGS   = gear("dark_leggings",   GearItem.GearType.LEGGINGS,   SetBonus.PJH);
        PJH_BOOTS      = gear("dark_boots",      GearItem.GearType.BOOTS,      SetBonus.PJH);
        DARK_HELMET = PJH_HELMET; DARK_CHESTPLATE = PJH_CHESTPLATE;
        DARK_LEGGINGS = PJH_LEGGINGS; DARK_BOOTS = PJH_BOOTS;

        // 유영진 세트
        YYJ_HELMET     = gear("yyj_helmet",     GearItem.GearType.HELMET,     SetBonus.YYJ);
        YYJ_CHESTPLATE = gear("yyj_chestplate", GearItem.GearType.CHESTPLATE, SetBonus.YYJ);
        YYJ_LEGGINGS   = gear("yyj_leggings",   GearItem.GearType.LEGGINGS,   SetBonus.YYJ);
        YYJ_BOOTS      = gear("yyj_boots",      GearItem.GearType.BOOTS,      SetBonus.YYJ);

        // 강화석
        ENHANCE_STONE = plain("enhance_stone");

        // 강화대
        ResourceKey<net.minecraft.world.level.block.Block> tableBlockKey =
            ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "enhance_table"));
        ResourceKey<Item> tableItemKey =
            ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "enhance_table"));
        ENHANCE_TABLE = new EnhanceTableBlock(EnhanceTableBlock.createProperties(tableBlockKey));
        Registry.register(BuiltInRegistries.BLOCK, tableBlockKey, ENHANCE_TABLE);
        Registry.register(BuiltInRegistries.ITEM, tableItemKey,
            new BlockItem(ENHANCE_TABLE, new Item.Properties().setId(tableItemKey)));

        // 랜덤박스
        WEAPON_BOX = box("weapon_box", GearBox.BoxType.WEAPON);
        ARMOR_BOX  = box("armor_box",  GearBox.BoxType.ARMOR);
        GearBox.initPools();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────
    private static GearItem gear(String id, GearItem.GearType type, SetBonus set) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, id));
        GearItem item = set != null
            ? new GearItem(new Item.Properties().setId(key), type, set)
            : new GearItem(new Item.Properties().setId(key), type, id);
        Registry.register(BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, id), item);
        return item;
    }

    private static Item plain(String id) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, id));
        Item item = new Item(new Item.Properties().setId(key));
        Registry.register(BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, id), item);
        return item;
    }

    private static GearBox box(String id, GearBox.BoxType type) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, id));
        GearBox item = new GearBox(new Item.Properties().setId(key), type);
        Registry.register(BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, id), item);
        return item;
    }
}
