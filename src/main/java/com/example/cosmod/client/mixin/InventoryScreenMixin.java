package com.example.cosmod.client.mixin;

import com.example.cosmod.client.CosmeticSlotHandler;
import com.example.cosmod.codex.*;
import com.example.cosmod.inventory.CosmeticInventory;
import com.example.cosmod.inventory.CosmeticInventoryHolder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen {

    private static final int COSM_SLOT = CosmeticSlotHandler.SLOT_SIZE;
    private static final int COSM_PAD  = CosmeticSlotHandler.SLOT_PADDING;

    // 도감 패널: 인벤토리 상단(스티브+제작) 영역 덮기
    // leftPos+8, topPos+8, 160x72
    private static final int OX = 8, OY = 8;   // 인벤토리 기준 오프셋
    private static final int PW = 160, PH = 72; // 패널 크기
    private static final int CELL = 20, COLS = 8, ROWS = 3; // 그리드

    // 탭 (패널 오른쪽에 붙음)
    private static final int TW = 52, TH = 22, TG = 3;

    // 색상
    private static final int BG   = 0xFFD4A84B;
    private static final int BD   = 0xFF6B3D0A;
    private static final int HDR  = 0xFFB8832A;
    private static final int CELL_N = 0xFFC49535;
    private static final int CELL_R = 0xFFEED880;

    // 탭 정의
    private static final CodexData.Tab[] T_TAB   = { CodexData.Tab.FARMER, CodexData.Tab.MINER };
    private static final String[]        T_NAME  = { "농부", "광부" };
    private static final int[]           T_COLOR = { 0xFF3A8A28, 0xFF5A7A8A };

    private int     scroll  = 0;
    private boolean clicked = false;
    private final ArrayList<Button> tabBtns = new ArrayList<>();

    private final ArrayList<AbstractWidget> cosmodBtns = new ArrayList<>();

    public InventoryScreenMixin() { super(null, null, null); }

    @Inject(method = "init", at = @At("TAIL"))
    private void cosmod$init(CallbackInfo ci) {
        cosmodBtns.clear();
        int px = this.leftPos + this.imageWidth + 8;
        int py = this.topPos + 8;
        String[] lbl = {"모","상","하","신"};
        for (int i = 0; i < CosmeticInventory.SIZE; i++) {
            final int idx = i;
            var btn = Button.builder(Component.literal(lbl[i]),
                b -> CosmeticSlotHandler.handleClick(idx))
                .bounds(px, py + 10 + i*(COSM_SLOT+COSM_PAD), COSM_SLOT, COSM_SLOT).build();
            this.addRenderableWidget(btn);
            cosmodBtns.add(btn);
        }
        // 탭 버튼 등록 (커스텀 렌더러로 색상+텍스트 직접 그림)
        tabBtns.clear();
        int tpx = this.leftPos + OX + PW + 4;
        int tpy = this.topPos  + OY;
        for (int t = 0; t < T_TAB.length; t++) {
            final int ti = t;
            int ty = tpy + t*(TH+TG);
            final int tt = t;
            var btn = Button.builder(Component.empty(), b -> {
                CodexClientHandler.activeTab = T_TAB[ti];
                scroll = 0;
            }).bounds(tpx, ty, TW, TH).build();
            // MC 기본 렌더 완전히 막고 blitSprite로 교체
            btn.setTooltip(null);
            tabBtns.add(btn);
            this.addRenderableWidget(btn);
        }
        // 레시피 북 버튼 숨기기
        hideRecipeBook();
    }


    @Inject(method = "render", at = @At("TAIL"))
    private void cosmod$render(GuiGraphics g, int mx, int my, float dt, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        Font font = Minecraft.getInstance().font;

        // 코디 버튼 visible 토글
        for (var btn : cosmodBtns) btn.visible = !CodexClientHandler.isOpen;
        // 탭 버튼 visible 토글
        for (var btn : tabBtns) btn.visible = CodexClientHandler.isOpen;

        if (CodexClientHandler.isOpen) {
            renderCodex(g, font, mx, my);
        } else {
            renderCosm(g, font, player, mx, my);
        }
    }

    private void renderCodex(GuiGraphics g, Font font, int mx, int my) {
        int px = this.leftPos + OX;
        int py = this.topPos  + OY;

        CodexData.Tab tab = CodexClientHandler.activeTab;
        boolean farm = tab == CodexData.Tab.FARMER;
        List<CodexRegistry.Entry> list = farm ? CodexRegistry.FARMER : CodexRegistry.MINER;
        int reg = farm ? CodexClientCache.farmerCount() : CodexClientCache.minerCount();
        int pct = list.isEmpty() ? 0 : reg * 100 / list.size();

        // ── 패널 배경 ──────────────────────────────────────────
        g.fill(px-1, py-1, px+PW+1, py+PH+1, BD);
        g.fill(px, py, px+PW, py+PH, BG);

        // ── 헤더 (높이 12) ─────────────────────────────────────
        g.fill(px, py, px+PW, py+12, HDR);
        g.fill(px, py+11, px+PW, py+12, BD);

        // 좌: "농부 도감"
        String left = (farm ? "농부" : "광부") + " 도감";
        g.drawString(font, left, px+3, py+2, 0xFF3B1F00, true);

        // 우: "0/22  0%"  (겹치지 않게 우측 정렬)
        String right = reg + "/" + list.size() + "  " + pct + "%";
        int rw = font.width(right);
        g.drawString(font, right, px + PW - rw - 3, py+2, 0xFF3B1F00, true);

        // ── 그리드 ─────────────────────────────────────────────
        int gx = px, gy = py + 12;
        g.fill(gx, gy, gx+PW, gy+ROWS*CELL, CELL_N);

        int maxS = Math.max(0, (int)Math.ceil(list.size()/(float)COLS) - ROWS);
        scroll = Math.max(0, Math.min(scroll, maxS));
        int start = scroll * COLS, end = Math.min(start + ROWS*COLS, list.size());

        // 1패스: 배경+아이콘
        for (int i = start; i < end; i++) {
            int c = (i-start)%COLS, r = (i-start)/COLS;
            int cx = gx+c*CELL, cy = gy+r*CELL;
            boolean isReg = isReg(list.get(i).id(), tab);
            boolean hov = mx>=cx && mx<cx+CELL && my>=cy && my<cy+CELL;
            g.fill(cx+1, cy+1, cx+CELL-1, cy+CELL-1, isReg?CELL_R:(hov?0xFFDDBB55:CELL_N));
            g.fill(cx, cy, cx+CELL, cy+1, BD);
            g.fill(cx, cy, cx+1, cy+CELL, BD);
            Item item = CodexRegistry.getItem(list.get(i));
            if (item != null) {
                g.renderItem(new ItemStack(item), cx+2, cy+2);
                if (!isReg) g.fill(cx+2, cy+2, cx+18, cy+18, 0x99000000);
            }
        }
        // 빈 셀 격자
        for (int i = end-start; i < ROWS*COLS; i++) {
            int c=i%COLS, r=i/COLS;
            g.fill(gx+c*CELL, gy+r*CELL, gx+c*CELL+CELL, gy+r*CELL+1, BD);
            g.fill(gx+c*CELL, gy+r*CELL, gx+c*CELL+1, gy+r*CELL+CELL, BD);
        }
        g.fill(gx+PW, gy, gx+PW+1, gy+ROWS*CELL, BD);
        g.fill(gx, gy+ROWS*CELL, gx+PW+1, gy+ROWS*CELL+1, BD);

        // 2패스: ✔
        for (int i = start; i < end; i++) {
            int c=(i-start)%COLS, r=(i-start)/COLS;
            if (isReg(list.get(i).id(), tab))
                g.drawString(font, "✔", gx+c*CELL+CELL-9, gy+r*CELL+1, 0xFF007700, false);
        }

        // ── 탭 버튼 (blitSprite) ──────────────────────────────
        int tx = px + PW + 4;
        for (int t = 0; t < T_TAB.length; t++) {
            boolean sel = tab == T_TAB[t];
            int ty = py + t*(TH+TG);
            boolean hov = mx>=tx && mx<tx+TW && my>=ty && my<ty+TH;

            // 스프라이트 ID: cosmod:widget/tab_farmer, cosmod:widget/tab_miner
            // 선택/비선택 2가지
            String spriteName = (T_TAB[t]==CodexData.Tab.FARMER ? "tab_farmer" : "tab_miner")
                + (sel ? "_selected" : "");
            net.minecraft.resources.Identifier sprite =
                net.minecraft.resources.Identifier.fromNamespaceAndPath("cosmod", "widget/" + spriteName);

            // 테두리
            g.fill(tx-1, ty-1, tx+TW+1, ty+TH+1, BD);
            // 스프라이트 렌더 (blitSprite는 스프라이트 아틀라스 사용 - 좌표 깨짐 없음)
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, TW, TH);
            // hover 오버레이
            if (hov && !sel) g.fill(tx, ty, tx+TW, ty+TH, 0x33FFFFFF);
        }

        // ── 호버 슬롯 하이라이트 ───────────────────────────────
        if (this.hoveredSlot != null) {
            ItemStack st = this.hoveredSlot.getItem();
            if (!st.isEmpty()) {
                boolean canR = canReg(st, list, tab);
                boolean alR  = alReg(st, list, tab);
                if (canR || alR) {
                    int sx = this.leftPos + this.hoveredSlot.x;
                    int sy = this.topPos  + this.hoveredSlot.y;
                    g.fill(sx, sy, sx+16, sy+16, alR ? 0x5500BB00 : 0x55FFAA00);
                    g.renderOutline(sx-1, sy-1, 18, 18, alR ? 0xFF00EE44 : 0xFFFFCC00);
                    String hint = alR ? "등록됨 ✔" : "클릭→등록!";
                    int hw = font.width(hint)+8;
                    int hx = Math.min(mx+12, this.width-hw-2);
                    int hy = Math.max(my-16, 2);
                    g.fill(hx, hy, hx+hw, hy+font.lineHeight+4, 0xEE2A1400);
                    g.renderOutline(hx, hy, hw, font.lineHeight+4, alR?0xFF00EE44:0xFFFFCC00);
                    g.drawString(font, hint, hx+4, hy+2, alR?0xFF00FF44:0xFFFFDD00, true);
                }
            }
        }


        handleCodexClick(px, py, mx, my, tab, list);
    }

    private void handleCodexClick(int px, int py, int mx, int my,
            CodexData.Tab tab, java.util.List<CodexRegistry.Entry> list) {
        boolean press = Minecraft.getInstance().mouseHandler.isLeftPressed();
        if (press) {
            if (!clicked) {
                clicked = true;
                // 슬롯 등록
                if (this.hoveredSlot != null) {
                    ItemStack st = this.hoveredSlot.getItem();
                    if (!st.isEmpty()) {
                        for (var e : list) {
                            if (match(st, e) && !isReg(e.id(), tab)) {
                                ClientPlayNetworking.send(new CodexRegisterPayload(tab.name(), e.id()));
                                break;
                            }
                        }
                    }
                }
                // 탭 클릭
                int tx = px + PW + 4;
                for (int t = 0; t < T_TAB.length; t++) {
                    int ty = py + t*(TH+TG);
                    if (mx>=tx && mx<tx+TW && my>=ty && my<ty+TH) {
                        CodexClientHandler.activeTab = T_TAB[t];
                        scroll = 0;
                        break;
                    }
                }
            }
        } else {
            clicked = false;
        }
    }

    private void renderCosm(GuiGraphics g, Font font, Player player, int mx, int my) {
        CosmeticInventory inv = ((CosmeticInventoryHolder)player).cosmod$getCosmeticInventory();
        int px = this.leftPos+this.imageWidth+8, py = this.topPos+8;
        int pw=26, ph=CosmeticInventory.SIZE*(COSM_SLOT+COSM_PAD)+16;
        g.fill(px-4,py-4,px+pw,py+ph,0xCC111122);
        g.renderOutline(px-4,py-4,pw+4,ph,0xFFFFD700);
        g.drawString(font,"코디",px,py-2,0xFFD700,true);
        String[] lbl={"모","상","하","신"};
        for (int i=0;i<CosmeticInventory.SIZE;i++) {
            int sx=px, sy=py+10+i*(COSM_SLOT+COSM_PAD);
            g.fill(sx-1,sy-1,sx+COSM_SLOT+1,sy+COSM_SLOT+1,0xFF555566);
            g.fill(sx,sy,sx+COSM_SLOT,sy+COSM_SLOT,inv.get(i).isEmpty()?0xFF8B8B9B:0xFF556677);
            ItemStack st=inv.get(i);
            if (!st.isEmpty()){g.renderItem(st,sx+1,sy+1);g.renderItemDecorations(font,st,sx+1,sy+1);}
            else g.drawString(font,lbl[i],sx+6,sy+5,0xAAAAAA,false);
            if (mx>=sx&&mx<sx+COSM_SLOT&&my>=sy&&my<sy+COSM_SLOT)
                g.fill(sx,sy,sx+COSM_SLOT,sy+COSM_SLOT,0x80FFFFFF);
        }
    }

    private void hideRecipeBook() {
        int px = this.leftPos+OX, py = this.topPos+OY;
        for (var c : this.children()) {
            if (c instanceof AbstractWidget aw && !cosmodBtns.contains(aw)) {
                if (aw.getX()>=px && aw.getX()<px+PW && aw.getY()>=py && aw.getY()<py+PH)
                    aw.visible = false;
            }
        }
    }

    private int darken(int c, float f) {
        return 0xFF000000|(((int)(((c>>16)&0xFF)*f))<<16)|(((int)(((c>>8)&0xFF)*f))<<8)|((int)((c&0xFF)*f));
    }
    private boolean isReg(String id, CodexData.Tab t) {
        return t==CodexData.Tab.FARMER?CodexClientCache.isFarmerRegistered(id):CodexClientCache.isMinerRegistered(id);
    }
    private boolean canReg(ItemStack s, List<CodexRegistry.Entry> l, CodexData.Tab t) {
        for (var e:l) if (match(s,e)&&!isReg(e.id(),t)) return true; return false;
    }
    private boolean alReg(ItemStack s, List<CodexRegistry.Entry> l, CodexData.Tab t) {
        for (var e:l) if (match(s,e)&&isReg(e.id(),t)) return true; return false;
    }
    private boolean match(ItemStack s, CodexRegistry.Entry e) {
        Item item=CodexRegistry.getItem(e);
        if (item!=null) return s.getItem()==item;
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().equals(e.id());
    }
}
