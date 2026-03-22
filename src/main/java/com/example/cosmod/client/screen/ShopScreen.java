package com.example.cosmod.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class ShopScreen extends Screen {

    private static final int BG_WIDTH  = 248;
    private static final int BG_HEIGHT = 160;
    private static final String DIALOGUE = "\"어서오세요! 무엇을 도와드릴까요?\"";

    public ShopScreen() {
        super(Component.literal("상점"));
    }

    @Override
    protected void init() {
        int cx = this.width  / 2;
        int cy = this.height / 2;

        this.addRenderableWidget(
            Button.builder(Component.literal("§c판매하기"), btn -> {
                this.onClose();
                this.minecraft.setScreen(new SellScreen());
            }).bounds(cx - 110, cy + 20, 100, 20).build()
        );
        this.addRenderableWidget(
            Button.builder(Component.literal("§a구매하기"), btn -> {
                this.onClose();
                this.minecraft.setScreen(new BuyScreen());
            }).bounds(cx + 10, cy + 20, 100, 20).build()
        );
        this.addRenderableWidget(
            Button.builder(Component.literal("닫기"), btn -> this.onClose())
                .bounds(cx - 40, cy + 50, 80, 20).build()
        );
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int cx = this.width  / 2;
        int cy = this.height / 2;
        int x  = cx - BG_WIDTH  / 2;
        int y  = cy - BG_HEIGHT / 2;

        // 1. MC 메뉴 배경
        this.renderMenuBackground(g);

        // 2. 패널 배경 + 텍스트 (버튼보다 먼저)
        g.fill(x, y, x + BG_WIDTH, y + BG_HEIGHT, 0xDD334455);
        g.renderOutline(x, y, BG_WIDTH, BG_HEIGHT, 0xFFFFD700);
        g.drawCenteredString(font, "§6§l잡화상인", cx, y + 14, 0xFFFFFF);
        g.fill(x + 10, y + 28, x + BG_WIDTH - 10, y + 29, 0xFFFFD700);
        g.drawCenteredString(font, "§f" + DIALOGUE, cx, cy - 10, 0xFFFFFF);

        // 3. 버튼 (맨 위)
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
