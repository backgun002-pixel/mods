package com.example.cosmod.client.screen;

import com.example.cosmod.economy.CosmodEconomy;
import com.example.cosmod.economy.ShopEntry;
import com.example.cosmod.economy.ShopRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@Environment(EnvType.CLIENT)
public class SellScreen extends Screen {

    private static final int BG_W = 320;
    private static final int BG_H = 220;
    private static final int ROW_H = 24;

    private ShopEntry.Category currentTab = ShopEntry.Category.CROP;
    private List<ShopEntry> displayed;

    public SellScreen() {
        super(Component.literal("판매하기"));
    }

    @Override
    protected void init() { rebuildList(); }

    private void rebuildList() {
        this.clearWidgets();
        int cx = this.width / 2, cy = this.height / 2;
        int x = cx - BG_W / 2,   y = cy - BG_H / 2;

        this.addRenderableWidget(Button.builder(Component.literal("🌾 농작물"), btn -> {
            currentTab = ShopEntry.Category.CROP; rebuildList();
        }).bounds(x + 10, y + 30, 80, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("⛏ 광물"), btn -> {
            currentTab = ShopEntry.Category.ORE; rebuildList();
        }).bounds(x + 95, y + 30, 80, 18).build());

        displayed = ShopRegistry.getByCategory(currentTab);
        for (int i = 0; i < displayed.size(); i++) {
            ShopEntry entry = displayed.get(i);
            int rowY = y + 58 + i * ROW_H;
            if (rowY + ROW_H > y + BG_H - 30) break;
            final ShopEntry e = entry;
            this.addRenderableWidget(Button.builder(Component.literal("판매"), btn ->
                SellBuyNetwork.sendSell(e.getItem(), 1)
            ).bounds(x + BG_W - 55, rowY, 45, 18).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("◀ 돌아가기"), btn -> {
            this.onClose();
            Minecraft.getInstance().setScreen(new ShopScreen());
        }).bounds(cx - 40, cy + BG_H / 2 - 24, 80, 18).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        int cx = this.width / 2, cy = this.height / 2;
        int x = cx - BG_W / 2,   y = cy - BG_H / 2;

        // 1. MC 배경
        this.renderMenuBackground(g);

        // 2. 패널 배경 + 텍스트
        g.fill(x, y, x + BG_W, y + BG_H, 0xDD332211);
        g.renderOutline(x, y, BG_W, BG_H, 0xFFFF6644);
        g.drawCenteredString(font, "§c§l판매하기", cx, y + 10, 0xFFFFFF);

        int coins = CosmodEconomy.getCoins(Minecraft.getInstance().player);
        g.drawString(font, "§e🪙 " + coins + " 코인", x + BG_W - 100, y + 10, 0xFFFFFF, false);
        g.fill(x + 5, y + 52, x + BG_W - 5, y + 53, 0xFFFF6644);

        if (displayed != null) {
            for (int i = 0; i < displayed.size(); i++) {
                ShopEntry entry = displayed.get(i);
                int rowY = y + 58 + i * ROW_H;
                if (rowY + ROW_H > y + BG_H - 30) break;

                g.renderItem(new ItemStack(entry.getItem()), x + 10, rowY + 2);
                String name = entry.getItem().getName(new ItemStack(entry.getItem())).getString();
                g.drawString(font, "§f" + name, x + 30, rowY + 6, 0xFFFFFF, false);

                int held = countItem(entry);
                g.drawString(font, "§f보유: §e" + held, x + 140, rowY + 6, 0xFFFFFF, false);

                int price = entry.getSellPrice();
                String col = price > entry.getBasePrice() ? "§a" : price < entry.getBasePrice() ? "§c" : "§f";
                g.drawString(font, col + price + "🪙", x + 210, rowY + 6, 0xFFFFFF, false);
            }
        }

        // 3. 버튼 (맨 위)
        super.render(g, mx, my, delta);
    }

    private int countItem(ShopEntry entry) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        int count = 0;
        // 1.21.11: Inventory는 Iterable<ItemStack>
        for (ItemStack s : player.getInventory()) {
            if (!s.isEmpty() && s.getItem() == entry.getItem()) count += s.getCount();
        }
        return count;
    }

    @Override public boolean isPauseScreen() { return false; }
}
