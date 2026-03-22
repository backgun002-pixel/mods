package com.example.cosmod.inventory;

import com.example.cosmod.item.CosmeticItem;
import net.minecraft.world.item.ItemStack;

/**
 * 플레이어에게 붙는 코디 인벤토리 (슬롯 4개)
 * 0 = HEAD, 1 = CHEST, 2 = LEGS, 3 = FEET
 */
public class CosmeticInventory {

    public static final int SIZE = 4;
    private final ItemStack[] slots = new ItemStack[SIZE];

    public CosmeticInventory() {
        for (int i = 0; i < SIZE; i++) {
            slots[i] = ItemStack.EMPTY;
        }
    }

    // ─── 슬롯 인덱스 ────────────────────────────
    public static int indexForSlot(CosmeticItem.CosmeticSlot slot) {
        return switch (slot) {
            case HEAD  -> 0;
            case CHEST -> 1;
            case LEGS  -> 2;
            case FEET  -> 3;
        };
    }

    // ─── 읽기 / 쓰기 ────────────────────────────
    public ItemStack get(int index) {
        if (index < 0 || index >= SIZE) return ItemStack.EMPTY;
        return slots[index];
    }

    public ItemStack getForSlot(CosmeticItem.CosmeticSlot slot) {
        return get(indexForSlot(slot));
    }

    public void set(int index, ItemStack stack) {
        if (index < 0 || index >= SIZE) return;
        slots[index] = stack == null ? ItemStack.EMPTY : stack;
    }

    public void setForSlot(CosmeticItem.CosmeticSlot slot, ItemStack stack) {
        set(indexForSlot(slot), stack);
    }

    // ─── 슬롯에 아이템을 넣을 수 있는지 검증 ────
    public static boolean canInsert(int index, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (!(stack.getItem() instanceof CosmeticItem cosItem)) return false;
        return indexForSlot(cosItem.getCosmeticSlot()) == index;
    }

    public ItemStack[] getSlots() {
        return slots;
    }
}
