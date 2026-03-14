package com.example.cosmod.economy;

import com.example.cosmod.client.screen.BuyPayload;
import com.example.cosmod.client.screen.SellPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

public class ShopServerNetwork {

    public static void register() {
        // 1.21.11: PayloadTypeRegistry로 먼저 등록 후 핸들러 등록
        PayloadTypeRegistry.playC2S().register(SellPayload.TYPE, SellPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BuyPayload.TYPE,  BuyPayload.CODEC);

        // 판매 요청 수신
        ServerPlayNetworking.registerGlobalReceiver(SellPayload.TYPE,
            (payload, context) -> {
                Item item   = payload.item();
                int  amount = payload.amount();
                context.server().execute(() -> {
                    var player = context.player();
                    CosmodEconomy.TradeResult result = CosmodEconomy.sell(player, item, amount);
                    ShopEntry entry = ShopRegistry.findByItem(item);
                    switch (result) {
                        case SUCCESS -> player.displayClientMessage(Component.literal(
                            "§a판매 완료! §f+" + (entry != null ? entry.getSellPrice() * amount : 0) + "🪙"), false);
                        case NOT_FOUND -> player.displayClientMessage(
                            Component.literal("§c판매할 수 없는 아이템입니다."), false);
                        case INSUFFICIENT_ITEMS -> player.displayClientMessage(
                            Component.literal("§c아이템이 부족합니다."), false);
                        default -> player.displayClientMessage(
                            Component.literal("§c판매 실패."), false);
                    }
                });
            });

        // 구매 요청 수신
        ServerPlayNetworking.registerGlobalReceiver(BuyPayload.TYPE,
            (payload, context) -> {
                Item item   = payload.item();
                int  amount = payload.amount();
                context.server().execute(() -> {
                    var player = context.player();
                    CosmodEconomy.TradeResult result = CosmodEconomy.buy(player, item, amount);
                    ShopEntry entry = ShopRegistry.findByItem(item);
                    switch (result) {
                        case SUCCESS -> player.displayClientMessage(Component.literal(
                            "§a구매 완료! §f-" + (entry != null ? entry.getBuyPrice() * amount : 0) + "🪙"), false);
                        case NOT_FOUND -> player.displayClientMessage(
                            Component.literal("§c구매할 수 없는 아이템입니다."), false);
                        case INSUFFICIENT_COINS -> player.displayClientMessage(
                            Component.literal("§c코인이 부족합니다."), false);
                        default -> player.displayClientMessage(
                            Component.literal("§c구매 실패."), false);
                    }
                });
            });
    }
}
