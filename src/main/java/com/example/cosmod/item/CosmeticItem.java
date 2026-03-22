package com.example.cosmod.item;

import net.minecraft.world.item.Item;

public class CosmeticItem extends Item {

    public enum CosmeticSlot { HEAD, CHEST, LEGS, FEET }

    private final CosmeticSlot slot;

    // Properties는 외부에서 setId() 적용 후 전달받음 (1.21.2+ 패턴)
    public CosmeticItem(Properties properties, CosmeticSlot slot) {
        super(properties);
        this.slot = slot;
    }

    public CosmeticSlot getCosmeticSlot() { return slot; }
}
