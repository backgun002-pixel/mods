package com.example.cosmod.client.screen;

import com.example.cosmod.combat.CombatItems;
import com.example.cosmod.combat.EnhanceRequestPayload;
import com.example.cosmod.combat.GearItem;
import com.example.cosmod.combat.GearOption;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EnhanceScreen extends Screen {



    // ── 레이아웃 상수 ─────────────────────────────────────────────
    private static final int INV_COLS = 9;
    private static final int INV_ROWS = 4;
    private static final int S        = 18;   // MC 표준 슬롯 크기
    private static final int PAD      = 7;
    private static final int W        = PAD * 2 + INV_COLS * S; // 176
    private static final int VIZ_H    = 96;
    private static final int MSG_H    = 14;
    private static final int BTN_H    = 18;
    private static final int INV_H    = INV_ROWS * S + 4;
    private static final int H        = VIZ_H + MSG_H + BTN_H + 6 + INV_H + 6;

    // ── 색상 ──────────────────────────────────────────────────────
    private static final int C_BG     = 0xF0_1C1408;
    private static final int C_BG2    = 0xFF_251A0C;
    private static final int C_BORDER = 0xFF_9B7820;
    private static final int C_BDR2   = 0xFF_5A4010;
    private static final int C_GOLD   = 0xFFD4A847;
    private static final int C_GDIM   = 0xFF7A5C18;
    private static final int C_TEXT   = 0xFFCCBB99;
    private static final int C_TDIM   = 0xFF887766;
    private static final int C_SLOT   = 0xFF_0C0804;
    private static final int C_GREEN  = 0xFF55CC77;

    // ── 슬롯 상태 ─────────────────────────────────────────────────
    private ItemStack gearSlot  = ItemStack.EMPTY;  // 장비 슬롯에 올려진 아이템
    private ItemStack stoneSlot = ItemStack.EMPTY;  // 강화석 슬롯에 올려진 아이템
    private int gearInvIdx  = -1;  // 인벤토리 슬롯 인덱스
    private int stoneInvIdx = -1;
    private ItemStack resultStack = ItemStack.EMPTY;
    private boolean enhanced = false;
    private String resultMsg = null;
    private int resultColor = 0x55FF55;
    private boolean resultSpecial = false;
    private int resultTick = 0;
    private ItemStack hoveredStack = ItemStack.EMPTY; // 툴팁용

    public EnhanceScreen() { super(Component.literal("장비 강화")); }

    private int bx() { return this.width  / 2 - W / 2; }
    private int by() { return this.height / 2 - H / 2; }

    @Override protected void init() { rebuildWidgets(); }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        int bx = bx(), by = by();

        // 장비 슬롯 버튼 (렌더는 drawFancySlot이 덮어씀)
        int[] gxy = gearXY();
        this.addRenderableWidget(
            Button.builder(Component.empty(), btn -> handleGearSlotClick())
                .bounds(gxy[0], gxy[1], 26, 26).build()
        );

        // 강화석 슬롯 버튼
        int[] sxy = stoneXY();
        this.addRenderableWidget(
            Button.builder(Component.empty(), btn -> handleStoneSlotClick())
                .bounds(sxy[0], sxy[1], 16, 16).build()
        );

        // 강화 버튼
        int btnY = by + VIZ_H + MSG_H + 3;
        this.addRenderableWidget(
            Button.builder(Component.literal("강화"), btn -> doEnhance())
                .bounds(bx + PAD, btnY, W - PAD * 2, BTN_H).build()
        );

        // 인벤토리 전체 36슬롯 버튼
        int invY0 = by + VIZ_H + MSG_H + BTN_H + 9;
        int[] displayOrder = new int[36];
        for (int i = 0; i < 27; i++) displayOrder[i] = i + 9;
        for (int i = 0; i < 9;  i++) displayOrder[27 + i] = i;
        for (int i = 0; i < 36; i++) {
            final int invIdx = displayOrder[i];
            int col = i % INV_COLS, row = i / INV_COLS;
            int ix = bx + PAD + col * S;
            int iy = invY0 + row * S + (row == 3 ? 4 : 0);
            this.addRenderableWidget(
                Button.builder(Component.empty(), btn -> handleInvClick(player, invIdx))
                    .bounds(ix, iy, S - 1, S - 1).build()
            );
        }

        // 닫기
        this.addRenderableWidget(
            Button.builder(Component.literal("✕"), btn -> this.onClose())
                .bounds(bx + W - 13, by + 3, 10, 10).build()
        );
    }
    // ── 슬롯 클릭 처리 ────────────────────────────────────────────

    // 장비 슬롯: 클릭 시 아이템 꺼내서 인벤토리로 반환
    private void handleGearSlotClick() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!gearSlot.isEmpty()) {
            // 슬롯에서 인벤토리로 반환
            if (!player.getInventory().add(gearSlot.copy()))
                player.drop(gearSlot.copy(), false);
            gearSlot = ItemStack.EMPTY;
            gearInvIdx = -1;
            resultStack = ItemStack.EMPTY;
            enhanced = false;
            rebuildWidgets();
        }
    }

    // 강화석 슬롯: 클릭 시 꺼내기
    private void handleStoneSlotClick() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!stoneSlot.isEmpty()) {
            if (!player.getInventory().add(stoneSlot.copy()))
                player.drop(stoneSlot.copy(), false);
            stoneSlot = ItemStack.EMPTY;
            stoneInvIdx = -1;
            rebuildWidgets();
        }
    }

    // 인벤토리 클릭: 장비 → 장비슬롯, 강화석 → 강화석슬롯, 나머지 무시
    private void handleInvClick(Player player, int invIdx) {
        ItemStack s = player.getInventory().getItem(invIdx);
        if (s.isEmpty()) return;

        if (s.getItem() instanceof GearItem) {
            // 기존 장비슬롯 아이템을 인벤토리로 반환
            if (!gearSlot.isEmpty()) {
                player.getInventory().setItem(invIdx, gearSlot.copy());
                gearSlot = s.copy();
                gearInvIdx = invIdx;
            } else {
                gearSlot = s.copy();
                gearInvIdx = invIdx;  // ← 빈 슬롯에 처음 넣을 때도 저장
                player.getInventory().setItem(invIdx, ItemStack.EMPTY);
            }
            resultStack = ItemStack.EMPTY;
            enhanced = false;
        } else if (s.getItem() == CombatItems.ENHANCE_STONE_BASIC
                || s.getItem() == CombatItems.ENHANCE_STONE_MID
                || s.getItem() == CombatItems.ENHANCE_STONE_HIGH
                || s.getItem() instanceof com.example.cosmod.combat.GemItem) {
            // 기존 강화석슬롯 반환
            if (!stoneSlot.isEmpty()) {
                ItemStack ret = stoneSlot.copy();
                if (!player.getInventory().add(ret))
                    player.drop(ret, false);
            }
            ItemStack one = s.copy(); one.setCount(1);
            stoneSlot = one;
            stoneInvIdx = invIdx;
            s.shrink(1);
        }
        rebuildWidgets();
    }



    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        int bx = bx(), by = by();
        hoveredStack = ItemStack.EMPTY; // 매 프레임 초기화
        Player player = Minecraft.getInstance().player;

        // 외곽 패널
        g.fill(bx - 1, by - 1, bx + W + 1, by + H + 1, C_BORDER);
        g.fill(bx, by, bx + W, by + H, C_BG);
        g.renderOutline(bx + 1, by + 1, W - 2, H - 2, C_BDR2);

        if (player == null) { super.render(g, mx, my, delta); return; }

        // super.render를 먼저 호출해 버튼 그린 뒤, 슬롯으로 덮어씀
        super.render(g, mx, my, delta);

        // 비주얼 영역 (버튼 위에 덮어 그림)
        renderViz(g, bx, by, mx, my);

        // 구분선
        g.fill(bx + 4, by + VIZ_H, bx + W - 4, by + VIZ_H + 1, C_BDR2);

        // 안내 메시지
        int msgY = by + VIZ_H + 3;
        String displayMsg;
        int displayColor;
        if (resultMsg != null && enhanced) {
            displayMsg = resultMsg;
            displayColor = resultColor;
        } else if (!gearSlot.isEmpty()) {
            displayMsg = gearSlot.getHoverName().getString();
            displayColor = C_TEXT;
        } else {
            displayMsg = "강화 장비를 선택해주세요.";
            displayColor = C_TEXT;
        }
        // 7강+ 특별 연출: 깜빡이는 효과
        if (resultSpecial && enhanced) {
            long tick = System.currentTimeMillis() / 200;
            int[] colors = {0xFFD700, 0xFF8C00, 0xFFFF00, 0xFFA500};
            displayColor = colors[(int)(tick % colors.length)];
        }
        g.drawCenteredString(font, displayMsg, bx + W / 2, msgY, displayColor);

        // 강화 버튼 테두리만 (fill 제거 - 버튼 텍스트가 가려지지 않게)
        int btnY = by + VIZ_H + MSG_H + 3;
        g.renderOutline(bx + PAD - 1, btnY - 1, W - PAD * 2 + 2, BTN_H + 2, C_BORDER);

        // 구분선
        int divY = btnY + BTN_H + 3;
        g.fill(bx + 4, divY, bx + W - 4, divY + 1, C_BDR2);

        // 인벤토리
        int invY0 = divY + 5;
        renderInventory(g, bx, invY0, mx, my, player);

        // 닫기 X
        g.drawString(font, "×", bx + W - 12, by + 4, C_TDIM, false);

        // 툴팁 렌더 - MC 2번 사진 스타일 (어두운 반투명 배경)
        if (!hoveredStack.isEmpty()) {
            renderMcTooltip(g, buildTooltip(hoveredStack, player), mx, my);
        }

    }

    private void renderViz(GuiGraphics g, int bx, int by, int mx, int my) {
        int cy    = by + VIZ_H / 2;
        int lineY = cy + 2;

        // 장비 슬롯
        int[] gxy = gearXY();
        drawFancySlot(g, gxy[0], gxy[1], 26, gearSlot, !gearSlot.isEmpty());
        if (!gearSlot.isEmpty() && mx >= gxy[0] && mx < gxy[0]+26 && my >= gxy[1] && my < gxy[1]+26)
            hoveredStack = gearSlot;

        // 강화석 슬롯
        int[] sxy = stoneXY();
        drawFancySlot(g, sxy[0], sxy[1], 16, stoneSlot, !stoneSlot.isEmpty());

        // 연결선
        int lx1 = gxy[0] + 28;
        int dmx  = bx + W / 2 + 10;
        g.fill(lx1, lineY, bx + W - 52, lineY + 1, C_GDIM);
        drawDiamond(g, lx1 + 12, lineY, 4, C_GDIM, C_BG);
        int circX = lx1 + 30;
        g.fill(circX - 3, lineY - 3, circX + 3, lineY + 3, C_BDR2);
        g.fill(circX - 1, lineY - 1, circX + 1, lineY + 1, C_GDIM);
        drawDiamond(g, dmx, lineY, 13, C_GDIM,  C_BG);
        drawDiamond(g, dmx, lineY, 10, C_BDR2,  C_BG);
        drawDiamond(g, dmx, lineY,  6, C_GDIM,  C_BG);
        drawDiamond(g, dmx, lineY,  2, C_GOLD,  C_BG);
        // 방사 장식
        for (int i = -2; i <= 2; i++) {
            int len = 5 - Math.abs(i);
            g.fill(dmx + i * 4, by + 8, dmx + i * 4 + 1, by + 8 + len, C_BDR2);
        }

        // 결과 슬롯
        int rsx = bx + W - 48, rsy = cy - 22, rsz = 42;
        int rcx = rsx + rsz / 2, rcy = rsy + rsz / 2;
        drawDiamond(g, rcx, rcy, 30, C_BDR2, C_BG);
        drawDiamond(g, rcx, rcy, 26, C_BG,   C_BG);
        drawDiamond(g, rcx, rcy, 23, C_BDR2, C_BG);
        drawDiamond(g, rcx, rcy, 19, C_BG,   C_BG);
        drawDiamond(g, rcx, rcy, 17, C_BORDER, C_BG);
        boolean glow = enhanced && !resultStack.isEmpty();
        if (glow) g.fill(rsx - 1, rsy - 1, rsx + rsz + 1, rsy + rsz + 1, 0x55_FFD700);
        g.fill(rsx - 1, rsy - 1, rsx + rsz + 1, rsy + rsz + 1, glow ? C_GOLD : C_BORDER);
        g.fill(rsx, rsy, rsx + rsz, rsy + rsz, C_SLOT);
        g.renderOutline(rsx + 2, rsy + 2, rsz - 4, rsz - 4, glow ? C_GDIM : C_BDR2);
        if (!resultStack.isEmpty()) {
            g.renderItem(resultStack, rsx + (rsz - 16) / 2, rsy + (rsz - 16) / 2);
            if (mx >= rsx && mx < rsx + rsz && my >= rsy && my < rsy + rsz)
                hoveredStack = resultStack;
        }
        else { drawDiamond(g, rcx, rcy, 6, C_BDR2, C_SLOT); drawDiamond(g, rcx, rcy, 2, C_GDIM, C_SLOT); }

        g.fill(dmx + 14, lineY, rsx, lineY + 1, C_GDIM);

        // 보석 옵션 표시
        if (enhanced && !resultStack.isEmpty()) {
            CompoundTag tag = GearItem.getGearTag(resultStack);
            int enhLv = tag.contains("enhance_level") ? tag.getInt("enhance_level").orElse(0) : 0;
            int ly = by + VIZ_H - 28;
            g.fill(bx + 4, ly - 2, bx + W - 4, ly - 1, C_BDR2);
            g.drawString(font, "강화: §e+" + enhLv, bx + PAD + 2, ly, C_TEXT, false);
            ly += 11;
            for (int i = 0; i < 2; i++) {
                if (!tag.contains("gem_opt_" + i)) continue;
                try {
                    GearOption opt = GearOption.valueOf(tag.getString("gem_opt_" + i).orElse(""));
                    int val = tag.getInt("gem_val_" + i).orElse(0);
                    g.drawString(font, "◆ " + opt.displayName, bx + PAD + 2, ly, C_TEXT, false);
                    g.drawString(font, "+" + val + opt.unit, bx + W - 40, ly, C_GREEN, false);
                    ly += 11;
                } catch (Exception ignored) {}
            }
        }
    }

    private void renderInventory(GuiGraphics g, int bx, int invY0, int mx, int my, Player player) {
        // MC 기본 인벤토리: 슬롯 0~26 (메인 3줄) + 27~35 (핫바)
        // 화면에는 27~35(핫바) 먼저, 그 위에 0~26(메인) 순으로 표시 (MC 기본과 동일)
        int[] displayOrder = new int[36];
        // 메인(0~26) → row 0~2
        for (int i = 0; i < 27; i++) displayOrder[i] = i + 9; // MC 내부 슬롯 인덱스
        // 핫바(27~35) → row 3
        for (int i = 0; i < 9; i++) displayOrder[27 + i] = i;

        for (int i = 0; i < 36; i++) {
            int col = i % INV_COLS;
            int row = i / INV_COLS;
            int ix = bx + PAD + col * S;
            int iy = invY0 + row * S + (row == 3 ? 4 : 0); // 핫바 약간 아래

            // 핫바 구분선
            if (row == 3 && col == 0)
                g.fill(bx + PAD, iy - 2, bx + W - PAD, iy - 1, C_BDR2);

            int invIdx = displayOrder[i];
            ItemStack s = player.getInventory().getItem(invIdx);

            // 슬롯 배경 (MC 스타일: 밝은 테두리)
            g.fill(ix - 1, iy - 1, ix + S,     iy + S,     0xFF_373737); // 어두운 테두리
            g.fill(ix,     iy,     ix + S - 1,  iy + S - 1, 0xFF_8B8B8B); // 밝은 안쪽
            g.fill(ix + 1, iy + 1, ix + S - 1,  iy + S - 1, 0xFF_575757); // 슬롯 본체

            // 선택된 아이템 강조
            if (!s.isEmpty() && (s.getItem() instanceof GearItem || s.getItem() == CombatItems.ENHANCE_STONE_BASIC)) {
                boolean isGear = s.getItem() instanceof GearItem;
                boolean sel = isGear && invIdx == findInvIdx(player, gearSlot)
                           || !isGear && invIdx == findInvIdx(player, stoneSlot);
                if (sel) g.fill(ix - 1, iy - 1, ix + S, iy + S, 0x88_D4A847);
                // 클릭 가능 하이라이트
                if (mx >= ix && mx < ix + S - 1 && my >= iy && my < iy + S - 1)
                    g.fill(ix + 1, iy + 1, ix + S - 1, iy + S - 1, 0x44_FFFFFF);
            }

            if (!s.isEmpty()) {
                g.renderItem(s, ix + 1, iy + 1);
                g.renderItemDecorations(font, s, ix + 1, iy + 1);
                // 강화됨 표시
                CompoundTag tag = GearItem.getGearTag(s);
                if (tag.contains("enhanced") && tag.getBoolean("enhanced").orElse(false))
                    g.drawString(font, "✦", ix + 10, iy, 0xFF88CCFF, false);
                // 호버 시 툴팁 저장 (render 끝에 한번에 그림)
                if (mx >= ix && mx < ix + S - 1 && my >= iy && my < iy + S - 1)
                    hoveredStack = s;
            }
        }
    }

    // 인벤토리에서 특정 ItemStack의 슬롯 인덱스 찾기
    private int findInvIdx(Player player, ItemStack target) {
        if (target.isEmpty()) return -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (ItemStack.isSameItemSameComponents(player.getInventory().getItem(i), target)) return i;
        return -1;
    }


    private void drawFancySlot(GuiGraphics g, int x, int y, int sz, ItemStack stack, boolean active) {
        int bc = active ? C_GOLD : C_BORDER;
        int d = 3;
        // 모서리 장식
        g.fill(x-2,y-2, x-2+d,y-1, bc); g.fill(x-2,y-2, x-1,y-2+d, bc);
        g.fill(x+sz-d+2,y-2, x+sz+2,y-1, bc); g.fill(x+sz+1,y-2, x+sz+2,y-2+d, bc);
        g.fill(x-2,y+sz+1, x-2+d,y+sz+2, bc); g.fill(x-2,y+sz-d+2, x-1,y+sz+2, bc);
        g.fill(x+sz-d+2,y+sz+1, x+sz+2,y+sz+2, bc); g.fill(x+sz+1,y+sz-d+2, x+sz+2,y+sz+2, bc);
        g.fill(x, y, x + sz, y + sz, C_SLOT);
        g.renderOutline(x, y, sz, sz, active ? C_GDIM : C_BDR2);
        // 비어있을 때 안내 텍스트
        if (stack.isEmpty()) {
            if (sz >= 26) g.drawCenteredString(font, "§8+", x + sz/2, y + sz/2 - 3, 0x444444);
        } else {
            g.renderItem(stack, x + (sz - 16) / 2, y + (sz - 16) / 2);
        }
    }

    private void drawDiamond(GuiGraphics g, int cx, int cy, int r, int color, int bg) {
        for (int dy = -r; dy <= r; dy++) {
            int w = r - Math.abs(dy);
            g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
        }
        for (int dy = -(r-1); dy <= r-1; dy++) {
            int w = r - 1 - Math.abs(dy);
            g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, bg);
        }
    }

    private int[] gearXY()  { return new int[]{ bx() + 10, by() + VIZ_H / 2 - 10 }; }
    public void setResultMessage(String msg, int color, boolean special) {
        this.resultMsg = msg;
        this.resultColor = color;
        this.resultSpecial = special;
        this.resultTick = 0;
        this.enhanced = true;
        // 인벤토리 버튼만 재구성 (슬롯 상태 유지)
        rebuildWidgets();
    }

    private int[] stoneXY() { return new int[]{ bx() + 10, by() + VIZ_H / 2 + 20 }; }

    private void doEnhance() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (gearSlot.isEmpty())  { player.displayClientMessage(Component.literal("§c장비 슬롯이 비어있습니다."),  false); return; }
        if (stoneSlot.isEmpty()) { player.displayClientMessage(Component.literal("§c강화석 슬롯이 비어있습니다."), false); return; }
        // 슬롯 인덱스 유효성 검증
        if (gearInvIdx < 0 || stoneInvIdx < 0) {
            player.displayClientMessage(Component.literal("§c슬롯 정보가 올바르지 않습니다."), false);
            return;
        }
        // 강화석 타입 판별
        int stoneType = -1;
        if (stoneSlot.getItem() == CombatItems.ENHANCE_STONE_BASIC) stoneType = 0;
        else if (stoneSlot.getItem() == CombatItems.ENHANCE_STONE_MID)   stoneType = 1;
        else if (stoneSlot.getItem() == CombatItems.ENHANCE_STONE_HIGH)  stoneType = 2;
        else if (stoneSlot.getItem() instanceof com.example.cosmod.combat.GemItem) stoneType = 3;
        if (stoneType < 0) {
            player.displayClientMessage(Component.literal("§c올바른 강화석이 아닙니다."), false);
            return;
        }
        // 서버에 요청만 전송, 결과는 EnhanceResultPayload로 수신
        resultStack = gearSlot.copy();
        resultMsg = "강화 요청 중...";
        resultColor = 0xAAAAAA;
        resultSpecial = false;
        ClientPlayNetworking.send(new EnhanceRequestPayload(gearInvIdx, stoneInvIdx, stoneType));
        // 슬롯만 비움 - rebuildWidgets() 호출 안 함 (결과 메시지 보존)
        gearSlot = ItemStack.EMPTY;
        stoneSlot = ItemStack.EMPTY;
        gearInvIdx = -1;
        stoneInvIdx = -1;
    }

    private List<Integer> getGearSlots(Player player) {
        List<Integer> s = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (player.getInventory().getItem(i).getItem() instanceof GearItem) s.add(i);
        return s;
    }
    private int countStones(Player player) {
        int c = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() == CombatItems.ENHANCE_STONE_BASIC) c += s.getCount();
        }
        return c;
    }
    private int getFirstStoneSlot(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() == CombatItems.ENHANCE_STONE_BASIC) return i;
        }
        return -1;
    }

    /**
     * MC 기본 인벤토리 툴팁 스타일 렌더
     * 배경: 어두운 반투명, 테두리: 보라 그라디언트
     */
    private void renderMcTooltip(GuiGraphics g, List<Component> lines, int mx, int my) {
        if (lines.isEmpty()) return;

        // 툴팁 크기 계산
        int maxWidth = 0;
        for (Component line : lines)
            maxWidth = Math.max(maxWidth, font.width(line));

        int lineH    = font.lineHeight + 2;
        int totalH   = lines.size() * lineH - 2;
        int padX     = 4, padY = 4;
        int boxW     = maxWidth + padX * 2;
        int boxH     = totalH  + padY * 2;

        // 화면 경계 처리
        int tx = mx + 12;
        int ty = my - 12;
        if (tx + boxW > this.width)  tx = this.width  - boxW - 4;
        if (ty + boxH > this.height) ty = this.height - boxH - 4;
        if (ty < 0) ty = 4;

        // ── 배경 (MC 표준: #F0100010 어두운 보라) ─────────────────
        g.fill(tx,        ty,        tx + boxW,     ty + boxH,     0xF0100010);
        // 안쪽 한 픽셀 더 어둡게
        g.fill(tx + 1,    ty + 1,    tx + boxW - 1, ty + boxH - 1, 0xF0100010);

        // ── 테두리 (보라색 단색) ───────────────────────────────────
        int border = 0xFF5000FF;
        g.fill(tx,            ty,            tx + boxW,     ty + 1,        border); // 위
        g.fill(tx,            ty + boxH - 1, tx + boxW,     ty + boxH,     border); // 아래
        g.fill(tx,            ty,            tx + 1,        ty + boxH,     border); // 왼
        g.fill(tx + boxW - 1, ty,            tx + boxW,     ty + boxH,     border); // 오른

        // ── 텍스트 렌더 ────────────────────────────────────────────
        int ly = ty + padY;
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            g.drawString(font, line, tx + padX, ly, 0xFFFFFFFF, true);
            ly += lineH;
            // 첫 줄(아이템 이름) 아래 구분선
            if (i == 0 && lines.size() > 1) {
                g.fill(tx + padX, ly - 1, tx + boxW - padX, ly, 0xFF505050);
                ly += 1;
            }
        }
    }


    private List<Component> buildTooltip(ItemStack stack, Player player) {
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName().copy());
        // GearItem이면 옵션도 추가
        stack.getItem().appendHoverText(
            stack,
            net.minecraft.world.item.Item.TooltipContext.EMPTY,
            new net.minecraft.world.item.component.TooltipDisplay(true, new java.util.LinkedHashSet<>()),
            lines::add,
            net.minecraft.world.item.TooltipFlag.Default.NORMAL
        );
        return lines;
    }

    @Override
    public void onClose() {
        // 창 닫을 때 슬롯 아이템 인벤토리로 반환
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            if (!gearSlot.isEmpty()) {
                if (!player.getInventory().add(gearSlot.copy()))
                    player.drop(gearSlot.copy(), false);
                gearSlot = ItemStack.EMPTY;
            }
            if (!stoneSlot.isEmpty()) {
                if (!player.getInventory().add(stoneSlot.copy()))
                    player.drop(stoneSlot.copy(), false);
                stoneSlot = ItemStack.EMPTY;
            }
        }
        super.onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
}
