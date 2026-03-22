package com.example.cosmod.codex;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CodexMenu extends AbstractContainerMenu {

    public final Inventory playerInventory;
    private final CodexData.Tab tab;

    // 슬롯 좌표를 생성 시점에 지정하는 커스텀 Slot
    private static class FixedSlot extends Slot {
        public FixedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }
    }

    public CodexMenu(int syncId, Inventory playerInv, CodexData.Tab tab) {
        super(tab == CodexData.Tab.FARMER
            ? CodexMenuType.FARMER_TYPE
            : CodexMenuType.MINER_TYPE, syncId);
        this.tab = tab;
        this.playerInventory = playerInv;

        // 메인 인벤토리 (슬롯 9~35) → 메뉴 인덱스 0~26
        // 좌표는 AbstractContainerScreen에서 leftPos+slot.x 로 계산됨
        // init()에서 leftPos = invX() 로 맞추므로 slot.x 는 상대좌표
        // MC 표준 인벤토리 좌표 (inventory.png 기준)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new FixedSlot(playerInv, 9 + row * 9 + col,
                    8 + col * 18, 84 + row * 18));
            }
        }
        // 핫바 (슬롯 0~8) → 메뉴 인덱스 27~35
        for (int col = 0; col < 9; col++) {
            addSlot(new FixedSlot(playerInv, col,
                8 + col * 18, 142));
        }
    }

    public CodexData.Tab getTab() { return tab; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) { return true; }
}
