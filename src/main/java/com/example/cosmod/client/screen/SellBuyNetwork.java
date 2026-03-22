package com.example.cosmod.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.Item;

public class SellBuyNetwork {

    public static void sendSell(Item item, int amount) {
        // 1.21.11: ClientPlayNetworking.send(payload)
        ClientPlayNetworking.send(new SellPayload(item, amount));
    }

    public static void sendBuy(Item item, int amount) {
        ClientPlayNetworking.send(new BuyPayload(item, amount));
    }
}
