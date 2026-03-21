package com.example.cosmod.combat;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

public class GemRerollNetwork {

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(OpenGemRerollPayload.TYPE, OpenGemRerollPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GemRerollResultPayload.TYPE, GemRerollResultPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GemRerollRequestPayload.TYPE, GemRerollRequestPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(GemRerollRequestPayload.TYPE,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                int slotIdx = payload.slotIdx();
                var inv = player.getInventory();
                ItemStack gem = inv.getItem(slotIdx);

                if (gem.isEmpty() || !(gem.getItem() instanceof GemItem gi)) {
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§c보석 아이템이 아닙니다."), false);
                    return;
                }

                CompoundTag tag = GemItem.getGemTag(gem);
                int rerolls = tag.getInt("rerolls").orElse(0);
                if (rerolls >= gi.getTier().maxRerolls) {
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§c재설정 횟수가 모두 소진되었습니다."), false);
                    return;
                }

                // 재설정 실행
                gi.reroll(gem, new Random());
                inv.setItem(slotIdx, gem);

                // 결과 전송
                CompoundTag newTag = GemItem.getGemTag(gem);
                ServerPlayNetworking.send(player, new GemRerollResultPayload(gem, slotIdx));
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§b✦ 보석 재설정 완료!"), false);
            }));
    }
}
