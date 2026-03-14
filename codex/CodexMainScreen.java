package com.example.cosmod.codex;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Environment(EnvType.CLIENT)
public class CodexMainScreen extends Screen {

    public CodexMainScreen() {
        super(Component.literal("코스모드 도감"));
    }

    @Override
    protected void init() {
        int bw=width-16, bh=height-40, btnW=bw/2-20, btnH=bh-50, ly=20+30;
        addRenderableWidget(Button.builder(Component.empty(),
            b -> minecraft.setScreen(new CodexDetailScreen(CodexData.Tab.FARMER)))
            .bounds(8+10, ly, btnW, btnH).build());
        addRenderableWidget(Button.builder(Component.empty(),
            b -> minecraft.setScreen(new CodexDetailScreen(CodexData.Tab.MINER)))
            .bounds(8+bw/2+10, ly, btnW, btnH).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        int bx=8, by=20, bw=width-16, bh=height-40, cx=bx+bw/2;
        int btnW=bw/2-20, btnH=bh-50, ly=by+30;
        int lx=bx+10, rx=bx+bw/2+10;

        // ── 1단계: 배경 ──────────────────────────────────────────
        this.renderMenuBackground(g);

        // ── 버튼 먼저 렌더 ───────────────────────────────────────
        super.render(g, mx, my, dt);

        // ── 커스텀 패널로 완전히 덮어씌우기 ──────────────────────
        g.fill(bx, by, bx+bw, by+bh, 0xF0080810);
        g.renderOutline(bx, by, bw, bh, 0xFFFFD700);
        g.fill(bx, by, bx+bw, by+22, 0xFF0D0D1A);
        g.fill(bx, by+21, bx+bw, by+22, 0xFFFFD700);
        // 타이틀
        g.drawCenteredString(font, "★ 코스모드 도감 ★", cx, by+7, 0xFFD700);

        // 농부 패널
        boolean hL = mx>=lx && mx<=lx+btnW && my>=ly && my<=ly+btnH;
        g.fill(lx, ly, lx+btnW, ly+btnH, hL?0xCC0D2208:0xAA081408);
        g.renderOutline(lx, ly, btnW, btnH, hL?0xFF88FF44:0xFF44AA22);

        int iconCx = lx+btnW/2, iconTop = ly+16;
        g.renderItem(new ItemStack(Items.CARROT),       iconCx-20, iconTop);
        g.renderItem(new ItemStack(Items.APPLE),        iconCx,    iconTop);
        g.renderItem(new ItemStack(Items.GOLDEN_CARROT),iconCx-20, iconTop+20);
        g.renderItem(new ItemStack(Items.WHEAT),        iconCx,    iconTop+20);

        int fc=CodexClientCache.farmerCount(), ft=CodexRegistry.FARMER.size();
        g.drawCenteredString(font, fc+" / "+ft+" 종", iconCx, ly+62, 0x88BB66);
        g.fill(lx+20, ly+74, lx+btnW-20, ly+74+14, 0xFF1A1A1A);
        if (ft>0) g.fill(lx+20, ly+74, lx+20+(btnW-40)*fc/ft, ly+74+14, 0xFF33AA11);
        g.renderOutline(lx+20, ly+74, btnW-40, 14, 0xFF335522);
        g.drawCenteredString(font, "농부 도감", lx+btnW/2, ly+76, 0xCCFFCC);
        g.drawCenteredString(font, hL?"[ 클릭하여 열기 ]":"[ 클릭 ]", iconCx, ly+btnH-16, hL?0xFFFFFF:0x555555);

        // 광부 패널
        boolean hR = mx>=rx && mx<=rx+btnW && my>=ly && my<=ly+btnH;
        g.fill(rx, ly, rx+btnW, ly+btnH, hR?0xCC08081E:0xAA08080E);
        g.renderOutline(rx, ly, btnW, btnH, hR?0xFF44AAFF:0xFF224488);

        int iconCx2 = rx+btnW/2;
        g.renderItem(new ItemStack(Items.DIAMOND),   iconCx2-20, iconTop);
        g.renderItem(new ItemStack(Items.IRON_INGOT),iconCx2,    iconTop);
        g.renderItem(new ItemStack(Items.GOLD_INGOT),iconCx2-20, iconTop+20);
        g.renderItem(new ItemStack(Items.COAL),      iconCx2,    iconTop+20);

        int mc=CodexClientCache.minerCount(), mt=CodexRegistry.MINER.size();
        g.drawCenteredString(font, mc+" / "+mt+" 종", iconCx2, ly+62, 0x6699BB);
        g.fill(rx+20, ly+74, rx+btnW-20, ly+74+14, 0xFF1A1A1A);
        if (mt>0) g.fill(rx+20, ly+74, rx+20+(btnW-40)*mc/mt, ly+74+14, 0xFF1155CC);
        g.renderOutline(rx+20, ly+74, btnW-40, 14, 0xFF224466);
        g.drawCenteredString(font, "광부 도감", rx+btnW/2, ly+76, 0xCCDDFF);
        g.drawCenteredString(font, hR?"[ 클릭하여 열기 ]":"[ 클릭 ]", iconCx2, ly+btnH-16, hR?0xFFFFFF:0x555555);

        g.drawCenteredString(font, "Y 키로 닫기", cx, by+bh-10, 0x333344);
    }

    @Override public boolean isPauseScreen() { return false; }
}
