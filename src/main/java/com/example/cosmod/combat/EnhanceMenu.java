package com.example.cosmod.combat;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EnhanceMenu extends AbstractContainerMenu {

    // EnhanceScreen layout: VIZ_H(80) + MSG_H(12) + PAD(4) + BTN_H(18) + GAP(8) = 122
    public static final int INV_TOP  = 122;
    public static final int HOTBAR_Y = 180;

    public EnhanceMenu(int syncId, Inventory playerInv) {
        super(EnhanceMenuType.TYPE, syncId);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, 9 + row * 9 + col,
                    8 + col * 18, INV_TOP + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }
}
