package com.example.cosmod.economy;

import com.example.cosmod.CosmodMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class CosmodEconomyItems {

    public static CoinItem COSMO_COIN;

    public static void register() {
        ResourceKey<Item> key = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "cosmo_coin")
        );
        COSMO_COIN = new CoinItem(new Item.Properties().stacksTo(64).setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, COSMO_COIN);
        CosmodMod.LOGGER.info("[Cosmod] 코스모 코인 등록 완료");
    }
}
