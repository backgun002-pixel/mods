package com.example.cosmod.client;

import com.example.cosmod.inventory.CosmeticInventory;
import com.example.cosmod.inventory.CosmeticInventoryHolder;
import com.example.cosmod.item.CosmeticItem;
import com.example.cosmod.network.CosmeticSyncNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CosmeticSlotHandler {

    public static final int SLOT_SIZE    = 18;
    public static final int SLOT_PADDING = 2;

    public static void register() { /* 버튼은 Mixin init에서 추가 */ }

    public static void handleClick(int slotIndex) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        CosmeticInventory inv =
            ((CosmeticInventoryHolder) player).cosmod$getCosmeticInventory();
        ItemStack carried   = player.containerMenu.getCarried();
        ItemStack slotStack = inv.get(slotIndex);

        if (!carried.isEmpty()) {
            if (!(carried.getItem() instanceof CosmeticItem cosItem)) return;
            if (CosmeticInventory.indexForSlot(cosItem.getCosmeticSlot()) != slotIndex) return;
            ItemStack toSlot = carried.copyWithCount(1);
            ItemStack toHand = slotStack.isEmpty() ? ItemStack.EMPTY : slotStack.copy();
            inv.set(slotIndex, toSlot);
            player.containerMenu.setCarried(toHand);
            carried.shrink(1);
            CosmeticSyncNetwork.sendCosmeticUpdate(slotIndex, toSlot);
        } else if (!slotStack.isEmpty()) {
            player.containerMenu.setCarried(slotStack.copy());
            inv.set(slotIndex, ItemStack.EMPTY);
            CosmeticSyncNetwork.sendCosmeticUpdate(slotIndex, ItemStack.EMPTY);
        }
    }
}
