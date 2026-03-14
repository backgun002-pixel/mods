package com.example.cosmod.economy;

import net.minecraft.world.item.Item;

public class CoinItem extends Item {
    // Properties는 외부(CosmodEconomyItems)에서 setId 포함해서 전달
    public CoinItem(Properties properties) {
        super(properties);
    }
}
