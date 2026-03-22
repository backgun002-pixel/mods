package com.example.cosmod.client.screen;

import com.example.cosmod.storage.RetrieveItemsPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;


import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class StorageScreen extends Screen {

    private static final int BG_W = 280;
    private static final int BG_H = 240;

    private final List<ItemStack> items;
    private final int cost;
    private int scrollOffset = 0;
    private static final int ITEMS_PER_ROW = 9;
    private static final int VISIBLE_ROWS  = 4;
    private int hoveredSlot = -1;

    public StorageScreen(List<ItemStack> items, int cost) {
        super(Component.literal("아이템 보관소"));
        this.items = items;
        this.cost  = cost;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int x  = cx - BG_W / 2;
        int y  = cy - BG_H / 2;

        // 회수 버튼
        this.addRenderableWidget(
            Button.builder(Component.literal("§a아이템 회수 §e(-" + cost + " 코인)"), btn -> {
                ClientPlayNetworking.send(new RetrieveItemsPayload());
                this.onClose();
            }).bounds(cx - 90, y + BG_H - 38, 180, 20).build()
        );

        // 닫기 버튼
        this.addRenderableWidget(
            Button.builder(Component.literal("§c닫기"), btn -> this.onClose())
                .bounds(cx - 30, y + BG_H - 14, 60, 12).build()
        );
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int x  = cx - BG_W / 2;
        int y  = cy - BG_H / 2;

        this.renderMenuBackground(g);

        // ── 패널 배경 ─────────────────────────────────────────────
        g.fill(x, y, x + BG_W, y + BG_H, 0xDD1A2233);
        g.renderOutline(x, y, BG_W, BG_H, 0xFF00CCFF);

        // ── 타이틀 ────────────────────────────────────────────────
        g.drawCenteredString(font, "§b§l아이템 보관소", cx, y + 8, 0xFFFFFF);
        g.fill(x + 8, y + 20, x + BG_W - 8, y + 21, 0xFF00CCFF);

        // ── NPC 대사 ──────────────────────────────────────────────
        g.drawCenteredString(font, "§f\"보관 중인 아이템을 찾으러 오셨군요.\"", cx, y + 26, 0xCCCCCC);

        // ── 보관 비용 안내 ────────────────────────────────────────
        g.fill(x + 8, y + 38, x + BG_W - 8, y + 50, 0x88000000);
        g.drawCenteredString(font, "§e총 회수 비용: §6" + cost + " §e코인", cx, y + 41, 0xFFFFFF);

        // ── 아이템 슬롯 그리드 ────────────────────────────────────
        int slotSize  = 18;
        int gridX     = x + (BG_W - ITEMS_PER_ROW * slotSize) / 2;
        int gridY     = y + 56;
        hoveredSlot   = -1;

        int startIdx = scrollOffset * ITEMS_PER_ROW;
        int endIdx   = Math.min(startIdx + ITEMS_PER_ROW * VISIBLE_ROWS, items.size());

        for (int i = startIdx; i < endIdx; i++) {
            int rel  = i - startIdx;
            int col  = rel % ITEMS_PER_ROW;
            int row  = rel / ITEMS_PER_ROW;
            int sx   = gridX + col * slotSize;
            int sy   = gridY + row * slotSize;

            // 슬롯 배경
            boolean hovered = mouseX >= sx && mouseX < sx + 16
                           && mouseY >= sy && mouseY < sy + 16;
            if (hovered) hoveredSlot = i;

            g.fill(sx, sy, sx + 16, sy + 16, hovered ? 0xAA334466 : 0x88222233);
            g.renderOutline(sx, sy, 16, 16, hovered ? 0xFF88CCFF : 0xFF445566);

            // 아이템 렌더링
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                g.renderItem(stack, sx, sy);
                g.renderItemDecorations(font, stack, sx, sy);
            }
        }

        // 빈 슬롯도 표시 (그리드 채우기)
        int totalSlots = ITEMS_PER_ROW * VISIBLE_ROWS;
        for (int i = endIdx - startIdx; i < totalSlots; i++) {
            int col = i % ITEMS_PER_ROW;
            int row = i / ITEMS_PER_ROW;
            int sx  = gridX + col * slotSize;
            int sy  = gridY + row * slotSize;
            g.fill(sx, sy, sx + 16, sy + 16, 0x44111122);
            g.renderOutline(sx, sy, 16, 16, 0xFF222233);
        }

        // ── 아이템 개수 표시 ──────────────────────────────────────
        g.drawCenteredString(font, "§7총 §f" + items.size() + "§7종 아이템 보관 중",
            cx, gridY + VISIBLE_ROWS * slotSize + 4, 0xAAAAAA);

        // ── 스크롤 표시 ───────────────────────────────────────────
        int totalRows = (int) Math.ceil(items.size() / (float) ITEMS_PER_ROW);
        if (totalRows > VISIBLE_ROWS) {
            int maxScroll = totalRows - VISIBLE_ROWS;
            g.drawCenteredString(font, "§7(" + (scrollOffset + 1) + "/" + (maxScroll + 1) + " 페이지  스크롤로 이동)",
                cx, gridY + VISIBLE_ROWS * slotSize + 14, 0x888888);
        }

        // 버튼 렌더
        super.render(g, mouseX, mouseY, delta);

        // ── 툴팁 ─────────────────────────────────────────────────
        if (hoveredSlot >= 0 && hoveredSlot < items.size()) {
            renderMcTooltip(g, buildTooltip(items.get(hoveredSlot)), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalRows = (int) Math.ceil(items.size() / (float) ITEMS_PER_ROW);
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        if (scrollY < 0) scrollOffset = Math.min(scrollOffset + 1, maxScroll);
        else             scrollOffset = Math.max(scrollOffset - 1, 0);
        return true;
    }


    private void renderMcTooltip(GuiGraphics g, List<net.minecraft.network.chat.Component> lines, int mx, int my) {
        if (lines.isEmpty()) return;
        int maxWidth = 0;
        for (var line : lines) maxWidth = Math.max(maxWidth, font.width(line));
        int lineH = font.lineHeight + 2;
        int totalH = lines.size() * lineH - 2;
        int padX = 4, padY = 4;
        int boxW = maxWidth + padX * 2;
        int boxH = totalH + padY * 2;
        int tx = Math.min(mx + 12, this.width - boxW - 4);
        int ty = Math.max(Math.min(my - 12, this.height - boxH - 4), 4);
        g.fill(tx, ty, tx + boxW, ty + boxH, 0xF0100010);
        int border = 0xFF5000FF;
        g.fill(tx, ty, tx + boxW, ty + 1, border);
        g.fill(tx, ty + boxH - 1, tx + boxW, ty + boxH, border);
        g.fill(tx, ty, tx + 1, ty + boxH, border);
        g.fill(tx + boxW - 1, ty, tx + boxW, ty + boxH, border);
        int ly = ty + padY;
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), tx + padX, ly, 0xFFFFFFFF, true);
            ly += lineH;
            if (i == 0 && lines.size() > 1) { g.fill(tx + padX, ly - 1, tx + boxW - padX, ly, 0xFF505050); ly++; }
        }
    }

    private List<net.minecraft.network.chat.Component> buildTooltip(ItemStack stack) {
        List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName().copy());
        stack.getItem().appendHoverText(
            stack,
            net.minecraft.world.item.Item.TooltipContext.EMPTY,
            new net.minecraft.world.item.component.TooltipDisplay(true, new java.util.LinkedHashSet<>()),
            lines::add,
            net.minecraft.world.item.TooltipFlag.Default.NORMAL
        );
        return lines;
    }

    @Override public boolean isPauseScreen() { return false; }
}
