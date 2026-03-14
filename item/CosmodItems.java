package com.example.cosmod.item;

import com.example.cosmod.CosmodMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class CosmodItems {

    public static Item RUBY;
    public static Item SAPPHIRE;
    public static Item RED_DIAMOND;

    public static CosmeticItem COSME_HAT;
    public static CosmeticItem COSME_SHIRT;
    public static CosmeticItem COSME_PANTS;
    public static CosmeticItem COSME_SHOES;

    public static void register() {
        RUBY        = regItem("ruby");
        SAPPHIRE    = regItem("sapphire");
        RED_DIAMOND = regItem("red_diamond");

        COSME_HAT   = reg("cosme_hat",   CosmeticItem.CosmeticSlot.HEAD);
        COSME_SHIRT = reg("cosme_shirt", CosmeticItem.CosmeticSlot.CHEST);
        COSME_PANTS = reg("cosme_pants", CosmeticItem.CosmeticSlot.LEGS);
        COSME_SHOES = reg("cosme_shoes", CosmeticItem.CosmeticSlot.FEET);
        CosmodMod.LOGGER.info("[Cosmod] 코디 아이템 4종 등록 완료");
    }

    private static Item regItem(String name) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, name));
        Item item = new Item(new Item.Properties().setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    private static CosmeticItem reg(String name, CosmeticItem.CosmeticSlot slot) {
        ResourceKey<Item> key = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, name)
        );
        // setId()를 Properties에 먼저 적용한 후 생성자에 전달 (1.21.2+ 필수)
        Item.Properties props = new Item.Properties().stacksTo(1).setId(key);
        CosmeticItem item = new CosmeticItem(props, slot);
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }
}
