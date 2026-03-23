package com.example.cosmod.client;

import com.example.cosmod.client.screen.EnhanceScreen;
import com.example.cosmod.client.screen.GemRerollScreen;
import com.example.cosmod.client.screen.JobScreen;
import com.example.cosmod.client.screen.ShopScreen;
import com.example.cosmod.combat.EnhanceResultPayload;
import com.example.cosmod.combat.GemRerollResultPayload;
import com.example.cosmod.combat.OpenGemRerollPayload;
import com.example.cosmod.combat.OpenEnhancePayload;
import com.example.cosmod.job.OpenJobGuiPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class CosmodClientNetwork {

    public static void register() {
        // 직업 GUI
        ClientPlayNetworking.registerGlobalReceiver(OpenJobGuiPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(new JobScreen())));

        // 강화 창 열기
        ClientPlayNetworking.registerGlobalReceiver(OpenEnhancePayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() ->
                ctx.client().setScreen(new EnhanceScreen())));

        // 강화 결과
        ClientPlayNetworking.registerGlobalReceiver(EnhanceResultPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() -> {
                net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
                if (screen instanceof EnhanceScreen es) {
                    es.setResultMessage(
                        payload.message(), payload.color(),
                        payload.isSpecial(), payload.resultGear());
                }
            }));

        // 보석 감정 창 열기
        ClientPlayNetworking.registerGlobalReceiver(OpenGemRerollPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() -> {
                Minecraft mc = ctx.client();
                if (mc.player == null) return;
                var gem = mc.player.getInventory().getItem(payload.slotIdx());
                if (!gem.isEmpty()) mc.setScreen(new GemRerollScreen(gem, payload.slotIdx()));
            }));

        // 보석 감정 결과
        ClientPlayNetworking.registerGlobalReceiver(GemRerollResultPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() -> {
                Minecraft mc = ctx.client();
                if (mc.player == null) return;
                mc.player.getInventory().setItem(payload.slotIdx(), payload.gem());
                if (mc.screen instanceof GemRerollScreen grs)
                    grs.updateGem(payload.gem());
            }));

        // 상점 — 미사용 payload는 등록만
        try {
            var buyClass = Class.forName("com.example.cosmod.client.screen.BuyPayload");
            var typeField = buyClass.getField("TYPE");
            // 등록은 ShopScreen이 처리
        } catch (Exception ignored) {}
    }
}
