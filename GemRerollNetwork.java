package com.example.cosmod.combat;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class GemRerollNetwork {

    public static void registerServer() {
        // S2C 등록
        PayloadTypeRegistry.playS2C().register(OpenGemRerollPayload.TYPE,   OpenGemRerollPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GemRerollResultPayload.TYPE, GemRerollResultPayload.CODEC);
        // C2S 등록
        PayloadTypeRegistry.playC2S().register(GemRerollRequestPayload.TYPE, GemRerollRequestPayload.CODEC);

        // 재설정 요청 수신
        ServerPlayNetworking.registerGlobalReceiver(GemRerollRequestPayload.TYPE,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                int slotIdx = payload.slotIdx();
                ItemStack gem = player.getInventory().getItem(slotIdx);

                if (!(gem.getItem() instanceof GemItem)) {
                    player.displayClientMessage(Component.literal("§c보석이 아닙니다."), false);
                    return;
                }
                if (GemItem.isMaxRerolled(gem)) {
                    player.displayClientMessage(Component.literal("§c최대 재설정 횟수에 도달했습니다."), false);
                    return;
                }

                GemItem.reroll(gem);
                player.getInventory().setItem(slotIdx, gem);

                // 결과를 클라이언트에 전송 (UI 업데이트)
                ServerPlayNetworking.send(player, new GemRerollResultPayload(gem.copy(), slotIdx));
            }));
    }
}
