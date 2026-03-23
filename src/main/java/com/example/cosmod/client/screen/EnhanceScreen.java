package com.example.cosmod.client.screen;

import com.example.cosmod.combat.CombatItems;
import com.example.cosmod.combat.EnhanceRequestPayload;
import com.example.cosmod.combat.GearItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class EnhanceScreen extends Screen {

    // ── 레이아웃 ─────────────────────────────────────────────────
    private static final int W       = 176;
    private static final int VIZ_H   = 80;
    private static final int MSG_H   = 12;
    private static final int BTN_H   = 18;
    private static final int S       = 18;   // 슬롯 크기
    private static final int PAD     = 7;
    // 전체 높이: VIZ + MSG + PAD + BTN + PAD + 인벤(3행+핫바구분+핫바)
    private static final int INV_ROWS = 4;
    private static final int H = VIZ_H + MSG_H + 4 + BTN_H + 6 + INV_ROWS * S + 4 + 6;

    // ── 색상 ─────────────────────────────────────────────────────
    private static final int C_BG     = 0xF0_1C1408;
    private static final int C_BORDER = 0xFF_9B7820;
    private static final int C_BDR2   = 0xFF_5A4010;
    private static final int C_GOLD   = 0xFFD4A847;
    private static final int C_GDIM   = 0xFF7A5C18;
    private static final int C_TEXT   = 0xFFCCBB99;
    private static final int C_TDIM   = 0xFF887766;
    private static final int C_SLOT   = 0xFF_0C0804;
    private static final int C_SLOT_N = 0xFF_575757;
    private static final int C_SLOT_B = 0xFF_373737;

    // ── 슬롯 상태 ────────────────────────────────────────────────
    private ItemStack gearSlot    = ItemStack.EMPTY;
    private ItemStack stoneSlot   = ItemStack.EMPTY;
    private ItemStack resultStack = ItemStack.EMPTY;
    private String  resultMsg     = null;
    private int     resultColor   = 0x55FF55;
    private boolean resultSpecial = false;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public EnhanceScreen() { super(Component.literal("장비 강화")); }

    private int bx() { return width  / 2 - W / 2; }
    private int by() { return height / 2 - H / 2; }

    @Override protected void init() { rebuildWidgets(); }

    protected void rebuildWidgets() {
        clearWidgets();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        int bx = bx(), by = by();

        // 장비 슬롯
        int[] gxy = gearXY();
        addRenderableWidget(Button.builder(Component.empty(), b -> handleGearSlotClick())
            .bounds(gxy[0], gxy[1], 22, 22).build());

        // 강화석 슬롯
        int[] sxy = stoneXY();
        addRenderableWidget(Button.builder(Component.empty(), b -> handleStoneSlotClick())
            .bounds(sxy[0], sxy[1], 14, 14).build());

        // 결과 슬롯
        int rsx = bx + W - 42, rsy = by + VIZ_H / 2 - 18;
        addRenderableWidget(Button.builder(Component.empty(), b -> handleResultSlotClick())
            .bounds(rsx, rsy, 36, 36).build());

        // 강화 버튼
        int btnY = by + VIZ_H + MSG_H + 4;
        addRenderableWidget(Button.builder(Component.literal("강화"), b -> doEnhance())
            .bounds(bx + PAD, btnY, W - PAD * 2, BTN_H).build());

        // 닫기
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
            .bounds(bx + W - 12, by + 2, 10, 10).build());

        // 인벤토리 슬롯 버튼 (36개)
        int invY0 = by + VIZ_H + MSG_H + 4 + BTN_H + 6;
        // 순서: 메인(9~35) 먼저, 핫바(0~8) 마지막
        int[] order = new int[36];
        for (int i = 0; i < 27; i++) order[i] = i + 9;
        for (int i = 0; i < 9;  i++) order[27 + i] = i;

        for (int i = 0; i < 36; i++) {
            final int invIdx = order[i];
            int col = i % 9, row = i / 9;
            int ix = bx + PAD + col * S;
            int iy = invY0 + row * S + (row == 3 ? 4 : 0); // 핫바 4px 간격
            addRenderableWidget(Button.builder(Component.empty(), b -> handleInvClick(player, invIdx))
                .bounds(ix, iy, S - 1, S - 1).build());
        }
    }

    // ── 클릭 핸들러 ──────────────────────────────────────────────
    private void handleGearSlotClick() {
        if (gearSlot.isEmpty()) return;
        Player p = Minecraft.getInstance().player; if (p == null) return;
        if (!p.getInventory().add(gearSlot.copy())) p.drop(gearSlot.copy(), false);
        gearSlot = ItemStack.EMPTY;
        rebuildWidgets();
    }

    private void handleStoneSlotClick() {
        if (stoneSlot.isEmpty()) return;
        Player p = Minecraft.getInstance().player; if (p == null) return;
        if (!p.getInventory().add(stoneSlot.copy())) p.drop(stoneSlot.copy(), false);
        stoneSlot = ItemStack.EMPTY;
        rebuildWidgets();
    }

    private void handleResultSlotClick() {
        if (resultStack.isEmpty()) return;
        Player p = Minecraft.getInstance().player; if (p == null) return;
        if (!p.getInventory().add(resultStack.copy())) p.drop(resultStack.copy(), false);
        resultStack = ItemStack.EMPTY; resultMsg = null;
        rebuildWidgets();
    }

    private void handleInvClick(Player player, int invIdx) {
        ItemStack s = player.getInventory().getItem(invIdx);
        if (s.isEmpty()) return;

        if (s.getItem() instanceof GearItem) {
            if (!gearSlot.isEmpty()) {
                if (!player.getInventory().add(gearSlot.copy())) player.drop(gearSlot.copy(), false);
            }
            gearSlot = s.copy();
            player.getInventory().setItem(invIdx, ItemStack.EMPTY);
            resultStack = ItemStack.EMPTY; resultMsg = null;

        } else if (isStone(s)) {
            if (!stoneSlot.isEmpty()) {
                if (!player.getInventory().add(stoneSlot.copy())) player.drop(stoneSlot.copy(), false);
            }
            ItemStack one = s.copy(); one.setCount(1);
            stoneSlot = one;
            ItemStack cur = player.getInventory().getItem(invIdx);
            if (cur.getCount() <= 1) player.getInventory().setItem(invIdx, ItemStack.EMPTY);
            else cur.shrink(1);
        }
        rebuildWidgets();
    }

    private boolean isStone(ItemStack s) {
        return s.getItem() == CombatItems.ENHANCE_STONE_BASIC
            || s.getItem() == CombatItems.ENHANCE_STONE_MID
            || s.getItem() == CombatItems.ENHANCE_STONE_HIGH
            || s.getItem() instanceof com.example.cosmod.combat.GemItem;
    }

    // ── 강화 실행 ────────────────────────────────────────────────
    private void doEnhance() {
        Player p = Minecraft.getInstance().player; if (p == null) return;
        if (gearSlot.isEmpty())  { p.displayClientMessage(Component.literal("§c장비 슬롯이 비어있습니다."),  false); return; }
        if (stoneSlot.isEmpty()) { p.displayClientMessage(Component.literal("§c강화석 슬롯이 비어있습니다."), false); return; }
        int st = -1;
        if (stoneSlot.getItem() == CombatItems.ENHANCE_STONE_BASIC)                st = 0;
        else if (stoneSlot.getItem() == CombatItems.ENHANCE_STONE_MID)             st = 1;
        else if (stoneSlot.getItem() == CombatItems.ENHANCE_STONE_HIGH)            st = 2;
        else if (stoneSlot.getItem() instanceof com.example.cosmod.combat.GemItem) st = 3;
        if (st < 0) { p.displayClientMessage(Component.literal("§c올바른 강화석이 아닙니다."), false); return; }

        resultMsg = "§7강화 요청 중..."; resultColor = 0xAAAAAA; resultSpecial = false;
        resultStack = gearSlot.copy();
        ClientPlayNetworking.send(new EnhanceRequestPayload(gearSlot.copy(), stoneSlot.copy(), st));
        gearSlot = ItemStack.EMPTY; stoneSlot = ItemStack.EMPTY;
        rebuildWidgets();
    }

    public void setResultMessage(String msg, int color, boolean special, ItemStack resultGear) {
        this.resultMsg = msg; this.resultColor = color;
        this.resultSpecial = special; this.resultStack = resultGear.copy();
        rebuildWidgets();
    }

    // ── 렌더링 ───────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        hoveredStack = ItemStack.EMPTY;
        int bx = bx(), by = by();
        Player player = Minecraft.getInstance().player;

        renderMenuBackground(g);

        // 창 배경
        g.fill(bx-1, by-1, bx+W+1, by+H+1, C_BORDER);
        g.fill(bx, by, bx+W, by+H, C_BG);
        g.renderOutline(bx+1, by+1, W-2, H-2, C_BDR2);

        super.render(g, mx, my, delta); // 버튼 렌더

        // 상단 비주얼
        renderViz(g, bx, by, mx, my);

        // VIZ 구분선
        g.fill(bx+4, by+VIZ_H, bx+W-4, by+VIZ_H+1, C_BDR2);

        // 메시지
        String dm; int dc;
        if (resultMsg != null)        { dm = resultMsg;                           dc = resultColor; }
        else if (!gearSlot.isEmpty()) { dm = gearSlot.getHoverName().getString(); dc = C_TEXT; }
        else                          { dm = "강화 장비를 선택해주세요.";           dc = C_TEXT; }
        if (resultSpecial && resultMsg != null) {
            long t = System.currentTimeMillis() / 200;
            int[] cols = {0xFFD700,0xFF8C00,0xFFFF00,0xFFA500};
            dc = cols[(int)(t % cols.length)];
        }
        g.drawCenteredString(font, Component.literal(dm), bx+W/2, by+VIZ_H+2, dc);

        // 강화 버튼 테두리
        int btnY = by + VIZ_H + MSG_H + 4;
        g.renderOutline(bx+PAD-1, btnY-1, W-PAD*2+2, BTN_H+2, C_BORDER);

        // 인벤토리 구분선
        int invY0 = btnY + BTN_H + 6;
        g.fill(bx+4, invY0-3, bx+W-4, invY0-2, C_BDR2);

        // 인벤토리 렌더
        if (player != null) renderInventory(g, bx, invY0, mx, my, player);

        // 툴팁
        if (!hoveredStack.isEmpty()) {
            List<net.minecraft.network.chat.Component> lines = hoveredStack.getTooltipLines(
                net.minecraft.world.item.Item.TooltipContext.EMPTY,
                player, net.minecraft.world.item.TooltipFlag.Default.NORMAL);
            List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> comps =
                lines.stream().map(l ->
                    net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
                        .create(l.getVisualOrderText()))
                .collect(Collectors.toList());
            g.renderTooltip(font, comps, mx, my,
                net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null);
        }
    }

    private void renderInventory(GuiGraphics g, int bx, int invY0, int mx, int my, Player player) {
        int[] order = new int[36];
        for (int i = 0; i < 27; i++) order[i] = i + 9;
        for (int i = 0; i < 9;  i++) order[27+i] = i;

        for (int i = 0; i < 36; i++) {
            int col = i % 9, row = i / 9;
            int ix = bx + PAD + col * S;
            int iy = invY0 + row * S + (row == 3 ? 4 : 0);

            // 핫바 구분선
            if (row == 3 && col == 0)
                g.fill(bx+PAD, iy-2, bx+W-PAD, iy-1, C_BDR2);

            // 슬롯 배경 (MC 스타일)
            g.fill(ix-1, iy-1, ix+S,   iy+S,   C_SLOT_B);
            g.fill(ix,   iy,   ix+S-1,  iy+S-1, 0xFF_8B8B8B);
            g.fill(ix+1, iy+1, ix+S-1,  iy+S-1, C_SLOT_N);

            ItemStack s = player.getInventory().getItem(order[i]);

            // 호버 하이라이트
            boolean hov = mx >= ix && mx < ix+S-1 && my >= iy && my < iy+S-1;
            if (hov) g.fill(ix+1, iy+1, ix+S-1, iy+S-1, 0x44_FFFFFF);

            if (!s.isEmpty()) {
                g.renderItem(s, ix+1, iy+1);
                g.renderItemDecorations(font, s, ix+1, iy+1);
                if (hov) hoveredStack = s;
            }
        }
    }

    private void renderViz(GuiGraphics g, int bx, int by, int mx, int my) {
        int cy = by + VIZ_H/2, lineY = cy + 2;

        // 장비 슬롯
        int[] gxy = gearXY();
        drawFancySlot(g, gxy[0], gxy[1], 22, gearSlot, !gearSlot.isEmpty());
        if (!gearSlot.isEmpty() && mx>=gxy[0] && mx<gxy[0]+22 && my>=gxy[1] && my<gxy[1]+22)
            hoveredStack = gearSlot;

        // 강화석 슬롯
        int[] sxy = stoneXY();
        drawFancySlot(g, sxy[0], sxy[1], 14, stoneSlot, !stoneSlot.isEmpty());
        if (!stoneSlot.isEmpty() && mx>=sxy[0] && mx<sxy[0]+14 && my>=sxy[1] && my<sxy[1]+14)
            hoveredStack = stoneSlot;

        // 연결선
        int lx1 = gxy[0]+24, dmx = bx+W/2+8;
        g.fill(lx1, lineY, bx+W-44, lineY+1, C_GDIM);
        drawDiamond(g, lx1+10, lineY, 3, C_GDIM, C_BG);
        drawDiamond(g, dmx,    lineY, 10, C_GDIM, C_BG);
        drawDiamond(g, dmx,    lineY,  7, C_BDR2, C_BG);
        drawDiamond(g, dmx,    lineY,  4, C_GDIM, C_BG);
        drawDiamond(g, dmx,    lineY,  1, C_GOLD, C_BG);

        // 결과 슬롯
        int rsx = bx+W-42, rsy = cy-18, rsz = 36;
        int rcx = rsx+rsz/2, rcy = rsy+rsz/2;
        drawDiamond(g, rcx, rcy, 24, C_BDR2, C_BG);
        drawDiamond(g, rcx, rcy, 20, C_BG,   C_BG);
        drawDiamond(g, rcx, rcy, 17, C_BDR2, C_BG);
        drawDiamond(g, rcx, rcy, 13, C_BG,   C_BG);
        boolean glow = !resultStack.isEmpty();
        if (glow) g.fill(rsx-1, rsy-1, rsx+rsz+1, rsy+rsz+1, 0x55_FFD700);
        g.fill(rsx-1, rsy-1, rsx+rsz+1, rsy+rsz+1, glow ? C_GOLD : C_BORDER);
        g.fill(rsx, rsy, rsx+rsz, rsy+rsz, C_SLOT);
        g.renderOutline(rsx+2, rsy+2, rsz-4, rsz-4, glow ? C_GDIM : C_BDR2);
        if (!resultStack.isEmpty()) {
            g.renderItem(resultStack, rsx+(rsz-16)/2, rsy+(rsz-16)/2);
            if (mx>=rsx && mx<rsx+rsz && my>=rsy && my<rsy+rsz) hoveredStack = resultStack;
            CompoundTag tag = GearItem.getGearTag(resultStack);
            int lv = tag.contains("enhance_level") ? tag.getInt("enhance_level").orElse(0) : 0;
            g.drawString(font, "§e+" + lv, bx+7, by+VIZ_H-12, 0xFFCCBB99, false);
        } else {
            drawDiamond(g, rcx, rcy, 5, C_BDR2, C_SLOT);
            drawDiamond(g, rcx, rcy, 1, C_GDIM, C_SLOT);
        }
        g.fill(dmx+11, lineY, rsx, lineY+1, C_GDIM);
    }

    private void drawFancySlot(GuiGraphics g, int x, int y, int sz, ItemStack stack, boolean active) {
        int bc = active ? C_GOLD : C_BORDER, d = 2;
        g.fill(x-2,y-2, x-2+d,y-1, bc); g.fill(x-2,y-2, x-1,y-2+d, bc);
        g.fill(x+sz-d+2,y-2, x+sz+2,y-1, bc); g.fill(x+sz+1,y-2, x+sz+2,y-2+d, bc);
        g.fill(x-2,y+sz+1, x-2+d,y+sz+2, bc); g.fill(x-2,y+sz-d+2, x-1,y+sz+2, bc);
        g.fill(x+sz-d+2,y+sz+1, x+sz+2,y+sz+2, bc); g.fill(x+sz+1,y+sz-d+2, x+sz+2,y+sz+2, bc);
        g.fill(x, y, x+sz, y+sz, C_SLOT);
        g.renderOutline(x, y, sz, sz, active ? C_GDIM : C_BDR2);
        if (stack.isEmpty()) { if (sz>=20) g.drawCenteredString(font, Component.literal("§8+"), x+sz/2, y+sz/2-3, 0x444444); }
        else g.renderItem(stack, x+(sz-16)/2, y+(sz-16)/2);
    }

    private void drawDiamond(GuiGraphics g, int cx, int cy, int r, int color, int bg) {
        for (int dy=-r; dy<=r; dy++) { int w=r-Math.abs(dy); g.fill(cx-w,cy+dy,cx+w+1,cy+dy+1,color); }
        for (int dy=-(r-1); dy<=r-1; dy++) { int w=r-1-Math.abs(dy); g.fill(cx-w,cy+dy,cx+w+1,cy+dy+1,bg); }
    }

    private int[] gearXY()  { return new int[]{ bx()+10, by()+VIZ_H/2-11 }; }
    private int[] stoneXY() { return new int[]{ bx()+10, by()+VIZ_H/2+15 }; }

    @Override
    public void onClose() {
        Player p = Minecraft.getInstance().player;
        if (p != null) {
            if (!gearSlot.isEmpty())   { if (!p.getInventory().add(gearSlot.copy()))   p.drop(gearSlot.copy(), false); }
            if (!stoneSlot.isEmpty())  { if (!p.getInventory().add(stoneSlot.copy()))  p.drop(stoneSlot.copy(), false); }
            if (!resultStack.isEmpty()){ if (!p.getInventory().add(resultStack.copy())) p.drop(resultStack.copy(), false); }
        }
        super.onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
}
