package com.example.cosmod.combat;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class EnhanceTableNetwork {

    // 서버에서 호출
    public static void sendOpenGui(ServerPlayer player) {
        ServerPlayNetworking.send(player, new OpenEnhancePayload());
    }

    // 서버 측 수신 등록
    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(OpenEnhancePayload.TYPE, OpenEnhancePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EnhanceRequestPayload.TYPE, EnhanceRequestPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(EnhanceRequestPayload.TYPE,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                ItemStack gear  = payload.gearStack();
                ItemStack stone = payload.stoneStack();

                // 검증: GearItem인지 + 강화석 보유 여부 + 미강화 상태
                if (!(gear.getItem() instanceof GearItem)) {
                    msg(player, "§c강화 가능한 장비가 아닙니다.");
                    return;
                }
                if (stone.isEmpty() || stone.getItem() != CombatItems.ENHANCE_STONE) {
                    msg(player, "§c강화석이 없습니다.");
                    return;
                }

                // 이미 강화됐는지 확인
                var tag = GearItem.getGearTag(gear);
                if (tag.contains("enhanced") && tag.getBoolean("enhanced").orElse(false)) {
                    msg(player, "§c이미 강화된 장비입니다.");
                    return;
                }

                // 인벤토리에서 강화석 1개 소비
                boolean removed = removeFromInventory(player, CombatItems.ENHANCE_STONE, 1);
                if (!removed) {
                    msg(player, "§c인벤토리에 강화석이 없습니다.");
                    return;
                }

                // 강화 수행 → 인벤토리에서 기어 교체
                ItemStack enhanced = GearItem.enhance(gear.copy());
                replaceInInventory(player, gear, enhanced);

                msg(player, "§b✦ 강화 성공! §f부옵션 2개가 해금되었습니다.");
            }));
    }

    private static boolean removeFromInventory(ServerPlayer player,
                                               net.minecraft.world.item.Item item, int count) {
        var inv = player.getInventory();
        int remaining = count;
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() == item) {
                int take = Math.min(s.getCount(), remaining);
                s.shrink(take);
                remaining -= take;
            }
        }
        return remaining == 0;
    }

    private static void replaceInInventory(ServerPlayer player,
                                           ItemStack original, ItemStack replacement) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            // CustomData 비교 (같은 아이템+같은 태그)
            if (ItemStack.isSameItemSameComponents(s, original)) {
                inv.setItem(i, replacement);
                return;
            }
        }
        // 못 찾으면 드롭
        player.drop(replacement, false);
    }

    private static void msg(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text), false);
    }
}
