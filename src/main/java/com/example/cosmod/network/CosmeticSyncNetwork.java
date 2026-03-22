package com.example.cosmod.network;

import com.example.cosmod.inventory.CosmeticInventory;
import com.example.cosmod.inventory.CosmeticInventoryHolder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.ItemStack;

public class CosmeticSyncNetwork {

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(
            CosmeticUpdatePayload.TYPE,
            CosmeticUpdatePayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
            CosmeticUpdatePayload.TYPE,
            (payload, ctx) -> {
                ctx.server().execute(() -> {
                    var player = ctx.player();
                    CosmeticInventory inv =
                        ((CosmeticInventoryHolder) player).cosmod$getCosmeticInventory();
                    inv.set(payload.slotIndex(), payload.stack());
                });
            }
        );
    }

    // 클라이언트에서 호출
    public static void sendCosmeticUpdate(int slotIndex, ItemStack stack) {
        ClientPlayNetworking.send(new CosmeticUpdatePayload(slotIndex, stack));
    }
}
