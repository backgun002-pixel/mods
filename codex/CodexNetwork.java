package com.example.cosmod.codex;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.FabricScreenHandlerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CodexNetwork {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(CodexSyncPayload.TYPE, CodexSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CodexRegisterPayload.TYPE, CodexRegisterPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CodexOpenPayload.TYPE, CodexOpenPayload.CODEC);

        // 클라이언트→서버: 도감 메뉴 열기 요청
        ServerPlayNetworking.registerGlobalReceiver(CodexOpenPayload.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            ctx.server().execute(() -> {
                CodexData.Tab tab;
                try { tab = CodexData.Tab.valueOf(payload.tab()); }
                catch (Exception e) { return; }
                CodexMenuType.openFor(player, tab);
            });
        });

        // 클라이언트→서버: 아이템 도감 등록
        ServerPlayNetworking.registerGlobalReceiver(CodexRegisterPayload.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            ctx.server().execute(() -> {
                CodexData.Tab tab;
                List<CodexRegistry.Entry> entries;
                try {
                    tab = CodexData.Tab.valueOf(payload.tab());
                    entries = tab == CodexData.Tab.FARMER
                        ? CodexRegistry.FARMER : CodexRegistry.MINER;
                } catch (Exception e) { return; }

                String id = payload.id();
                // 이미 등록됐으면 스킵
                if (CodexData.isRegistered(player, tab, id)) return;

                // 해당 항목 찾기
                CodexRegistry.Entry entry = entries.stream()
                    .filter(e -> e.id().equals(id))
                    .findFirst().orElse(null);
                if (entry == null) return;

                // 인벤토리에 해당 아이템 있는지 확인
                Item required = CodexRegistry.getItem(entry);
                if (required == null) {
                    // 커스텀 아이템 - ID로 직접 확인
                    boolean hasItem = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack s = player.getInventory().getItem(i);
                        if (!s.isEmpty()) {
                            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getKey(s.getItem()).getPath();
                            if (itemId.equals(id)) { hasItem = true; break; }
                        }
                    }
                    if (!hasItem) {
                        player.displayClientMessage(
                            Component.literal("§c" + entry.name() + "§f이(가) 인벤토리에 없습니다."), true);
                        return;
                    }
                } else {
                    // 인벤토리 또는 현재 들고있는 아이템 확인
                boolean has = player.getInventory().hasAnyOf(java.util.Set.of(required));
                if (!has) {
                    // containerMenu의 carried 아이템도 확인
                    net.minecraft.world.item.ItemStack carried = player.containerMenu.getCarried();
                    if (!carried.isEmpty() && carried.getItem() == required) has = true;
                }
                if (!has) {
                        player.displayClientMessage(
                            Component.literal("§c" + entry.name() + "§f이(가) 인벤토리에 없습니다."), true);
                        return;
                    }
                }

                // 등록!
                CodexData.register(player, tab, id);

                // 아이템 1개 소비
                if (required != null) {
                    // 바닐라 아이템: 인벤토리에서 1개 제거
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack s = player.getInventory().getItem(i);
                        if (!s.isEmpty() && s.getItem() == required) {
                            s.shrink(1);
                            player.getInventory().setItem(i, s);
                            break;
                        }
                    }
                } else {
                    // 커스텀 아이템: ID로 찾아서 1개 제거
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack s = player.getInventory().getItem(i);
                        if (!s.isEmpty()) {
                            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getKey(s.getItem()).getPath();
                            if (itemId.equals(id)) {
                                s.shrink(1);
                                player.getInventory().setItem(i, s);
                                break;
                            }
                        }
                    }
                }

                player.displayClientMessage(
                    Component.literal("§a[도감] §e" + entry.name() + " §f등록 완료!"), true);

                // 동기화
                sync(player);
            });
        });
    }

    public static void sync(ServerPlayer player) {
        var farmerIds = CodexData.getRegistered(player, CodexData.Tab.FARMER);
        var minerIds  = CodexData.getRegistered(player, CodexData.Tab.MINER);
        ServerPlayNetworking.send(player, new CodexSyncPayload(farmerIds, minerIds));
    }
}
