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
        // 직업 GUI
        ClientPlayNetworking.registerGlobalReceiver(OpenJobGuiPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(new JobScreen())));

        // 상점 GUI
        ClientPlayNetworking.registerGlobalReceiver(OpenShopPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(new ShopScreen(payload.entries()))));

        // 구매/판매 페이로드 등록
        ClientPlayNetworking.registerGlobalReceiver(BuyPayload.TYPE,
            (payload, ctx) -> {});
        ClientPlayNetworking.registerGlobalReceiver(SellPayload.TYPE,
            (payload, ctx) -> {});

        // 코스메틱 동기화
        ClientPlayNetworking.registerGlobalReceiver(CosmeticUpdatePayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() ->
                CosmeticInventory.updateFromPayload(payload)));

        // 강화 GUI
        ClientPlayNetworking.registerGlobalReceiver(OpenEnhancePayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(new EnhanceScreen())));

        // 강화 결과
        ClientPlayNetworking.registerGlobalReceiver(EnhanceResultPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() -> {
                if (ctx.client().screen instanceof EnhanceScreen es) {
                    es.setResultMessage(payload.message(), payload.color(), payload.isSpecial());
                }
            }));

        // 보석 감정 GUI 열기
        ClientPlayNetworking.registerGlobalReceiver(OpenGemRerollPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() -> {
                Minecraft mc = ctx.client();
                var inv = mc.player.getInventory();
                var gem = inv.getItem(payload.slotIdx());
                if (!gem.isEmpty()) {
                    mc.setScreen(new GemRerollScreen(gem, payload.slotIdx()));
                }
            }));

        // 보석 감정 결과
        ClientPlayNetworking.registerGlobalReceiver(GemRerollResultPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() -> {
                Minecraft mc = ctx.client();
                // 인벤토리 업데이트
                mc.player.getInventory().setItem(payload.slotIdx(), payload.gem());
                // 화면 업데이트
                if (mc.screen instanceof GemRerollScreen grs) {
                    grs.updateGem(payload.gem());
                }
            }));
    }
}
