package com.example.cosmod.codex;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public class CodexClientHandler {

    // 도감 패널이 열려있는지 여부 (전역 상태)
    public static boolean isOpen = false;
    public static CodexData.Tab activeTab = CodexData.Tab.FARMER;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (CodexKey.OPEN_CODEX == null) return;

            if (CodexKey.OPEN_CODEX.consumeClick()) {
                if (client.screen instanceof InventoryScreen) {
                    // 인벤토리 열려있으면 도감 패널 토글
                    isOpen = !isOpen;
                } else if (client.screen == null) {
                    // 아무것도 안 열려있으면 인벤토리 열고 도감 패널 ON
                    isOpen = true;
                    client.setScreen(new InventoryScreen(client.player));
                } else {
                    isOpen = false;
                }
            }

            // 인벤토리가 닫히면 도감도 닫기
            if (client.screen == null) isOpen = false;
        });
    }
}
