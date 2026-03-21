package com.example.cosmod.client;

import com.example.cosmod.client.screen.BuyScreen;
import com.example.cosmod.client.screen.EnhanceScreen;
import com.example.cosmod.client.screen.GemRerollScreen;
import com.example.cosmod.client.screen.SellScreen;
import com.example.cosmod.combat.EnhanceResultPayload;
import com.example.cosmod.combat.GemRerollResultPayload;
import com.example.cosmod.combat.OpenGemRerollPayload;
import com.example.cosmod.combat.OpenEnhancePayload;
import com.example.cosmod.economy.BuyPayload;
import com.example.cosmod.economy.OpenShopPayload;
import com.example.cosmod.economy.SellPayload;
import com.example.cosmod.job.OpenJobGuiPayload;
import com.example.cosmod.client.screen.JobScreen;
import com.example.cosmod.client.screen.ShopScreen;
import com.example.cosmod.network.CosmeticUpdatePayload;
import com.example.cosmod.item.CosmeticInventory;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class CosmodClientNetwork {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenJobGuiPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(new JobScreen())));

        ClientPlayNetworking.registerGlobalReceiver(OpenShopPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(new ShopScreen(payload.entries()))));

        ClientPlayNetworking.registerGlobalReceiver(BuyPayload.TYPE, (payload, ctx) -> {});
        ClientPlayNetworking.registerGlobalReceiver(SellPayload.TYPE, (payload, ctx) -> {});

        ClientPlayNetworking.registerGlobalReceiver(CosmeticUpdatePayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() ->
                CosmeticInventory.updateFromPayload(payload)));

        ClientPlayNetworking.registerGlobalReceiver(OpenEnhancePayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(new EnhanceScreen())));

        // 강화 결과 — 서버에서 강화된 아이템 직접 전달
        ClientPlayNetworking.registerGlobalReceiver(EnhanceResultPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() -> {
                net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
                if (screen instanceof EnhanceScreen es) {
                    es.setResultMessage(payload.message(), payload.color(), payload.isSpecial(),
                                        payload.gearSlotIdx(), payload.resultGear());
                }
            }));

        ClientPlayNetworking.registerGlobalReceiver(OpenGemRerollPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() -> {
                Minecraft mc = ctx.client();
                var gem = mc.player.getInventory().getItem(payload.slotIdx());
                if (!gem.isEmpty()) mc.setScreen(new GemRerollScreen(gem, payload.slotIdx()));
            }));

        ClientPlayNetworking.registerGlobalReceiver(GemRerollResultPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() -> {
                Minecraft mc = ctx.client();
                mc.player.getInventory().setItem(payload.slotIdx(), payload.gem());
                if (mc.screen instanceof GemRerollScreen grs) grs.updateGem(payload.gem());
            }));
    }
}
