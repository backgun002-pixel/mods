package com.example.cosmod.entity;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class ShopNpcNetwork {

    public static void sendOpenShopGui(ServerPlayer player) {
        // 1.21.11: ServerPlayNetworking.send(player, payload)
        ServerPlayNetworking.send(player, new OpenShopPayload());
    }
}
