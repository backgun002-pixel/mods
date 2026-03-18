package com.example.cosmod.client;

import com.example.cosmod.client.screen.EnhanceScreen;
import com.example.cosmod.client.screen.ShopScreen;
import com.example.cosmod.combat.OpenEnhancePayload;
import com.example.cosmod.combat.EnhanceResultPayload;
import com.example.cosmod.combat.OpenGemRerollPayload;
import com.example.cosmod.combat.GemRerollResultPayload;
import com.example.cosmod.client.screen.GemRerollScreen;
import com.example.cosmod.entity.OpenShopPayload;
import com.example.cosmod.job.OpenJobGuiPayload;
import com.example.cosmod.client.screen.JobScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;

public class CosmodClientNetwork {

    public static void register() {
        // 1.21.11: S2C 페이로드 등록
        PayloadTypeRegistry.playS2C().register(OpenShopPayload.TYPE, OpenShopPayload.CODEC);

        // 강화 GUI 오픈 패킷 수신 (등록은 서버에서 이미 함)
        ClientPlayNetworking.registerGlobalReceiver(OpenEnhancePayload.TYPE,
            (payload, context) -> context.client().execute(() ->
                Minecraft.getInstance().setScreen(new EnhanceScreen())
            ));

        // 상점 GUI 오픈 패킷 수신
        ClientPlayNetworking.registerGlobalReceiver(OpenShopPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() ->
                    Minecraft.getInstance().setScreen(new ShopScreen())
                );
            });

        // 직업 GUI 오픈 패킷 수신
        ClientPlayNetworking.registerGlobalReceiver(OpenJobGuiPayload.TYPE,
            (payload, context) -> context.client().execute(() ->
                Minecraft.getInstance().setScreen(new JobScreen())
            ));

        ClientPlayNetworking.registerGlobalReceiver(EnhanceResultPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() -> {
                net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
                if (screen instanceof EnhanceScreen es) {
                    es.setResultMessage(payload.message(), payload.color(), payload.isSpecial());
                }
            }));

        // 보석 감정 UI 열기
        ClientPlayNetworking.registerGlobalReceiver(OpenGemRerollPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() ->
                Minecraft.getInstance().setScreen(
                    new GemRerollScreen(
                        Minecraft.getInstance().player.getInventory().getItem(payload.slotIdx()),
                        payload.slotIdx()
                    ))));

        // 보석 재설정 결과 수신 → UI 업데이트
        ClientPlayNetworking.registerGlobalReceiver(GemRerollResultPayload.TYPE,
            (payload, ctx) -> ctx.client().execute(() -> {
                net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
                if (screen instanceof GemRerollScreen grs) {
                    grs.updateGem(payload.gem());
                }
            }));
    }
}