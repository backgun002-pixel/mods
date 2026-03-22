package com.example.cosmod.client.mixin;

import com.example.cosmod.codex.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin {

    @Shadow protected Slot hoveredSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void cosmod$mouseClicked(MouseButtonEvent event, boolean bl,
                                     CallbackInfoReturnable<Boolean> cir) {
        if (!CodexClientHandler.isOpen) return;
        if (event.button() != 0) return;
        if (!(((Object)this) instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;

        CodexData.Tab tab = CodexClientHandler.activeTab;
        boolean farm = tab == CodexData.Tab.FARMER;
        java.util.List<CodexRegistry.Entry> list = farm ? CodexRegistry.FARMER : CodexRegistry.MINER;

        if (hoveredSlot != null) {
            ItemStack st = hoveredSlot.getItem();
            if (!st.isEmpty()) {
                for (var e : list) {
                    Item item = CodexRegistry.getItem(e);
                    boolean matches = (item != null)
                        ? st.getItem() == item
                        : net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getKey(st.getItem()).getPath().equals(e.id());
                    if (matches && !isReg(e.id(), tab)) {
                        ClientPlayNetworking.send(new CodexRegisterPayload(tab.name(), e.id()));
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }
    }

    private static boolean isReg(String id, CodexData.Tab t) {
        return t == CodexData.Tab.FARMER
            ? CodexClientCache.isFarmerRegistered(id)
            : CodexClientCache.isMinerRegistered(id);
    }
}
