package com.example.cosmod.storage;

import com.example.cosmod.economy.CosmodEconomy;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class StorageNpcNetwork {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenStoragePayload.TYPE, OpenStoragePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RetrieveItemsPayload.TYPE, RetrieveItemsPayload.CODEC);

        // 클라이언트→서버: 아이템 회수 요청 처리
        ServerPlayNetworking.registerGlobalReceiver(RetrieveItemsPayload.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            ctx.server().execute(() -> {
                if (!DeathItemStorage.hasItems(player)) {
                    player.displayClientMessage(
                        Component.literal("§c보관된 아이템이 없습니다."), false);
                    return;
                }
                int cost = DeathItemStorage.getCost(player);
                if (!CosmodEconomy.takeCoins(player, cost)) {
                    player.displayClientMessage(
                        Component.literal("§c코인이 부족합니다! §f(필요: §e" + cost + " §f코인, 보유: §e"
                            + CosmodEconomy.getCoins(player) + " §f코인)"), false);
                    return;
                }
                List<ItemStack> items = DeathItemStorage.getItems(player);
                for (ItemStack stack : items) {
                    if (!player.getInventory().add(stack.copy())) {
                        // 인벤토리가 꽉 찼으면 발 아래에 드롭
                        player.drop(stack.copy(), false);
                    }
                }
                DeathItemStorage.clearItems(player);
                player.displayClientMessage(
                    Component.literal("§a아이템을 회수했습니다! §e(-" + cost + " 코인)"), false);
            });
        });
    }

    public static void sendOpenStorageGui(ServerPlayer player) {
        List<ItemStack> items = DeathItemStorage.getItems(player);
        int cost = DeathItemStorage.getCost(player);
        ServerPlayNetworking.send(player, new OpenStoragePayload(items, cost));
    }
}
