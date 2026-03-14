package com.example.cosmod.codex;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@Environment(EnvType.CLIENT)
public class CodexDetailScreen extends Screen {

    private static final int SIDEBAR_W = 36;
    private static final int REWARD_W  = 110;
    private static final int PAD       = 6;
    private static final int COLS      = 2;
    private static final int ENTRY_H   = 26;
    private static final int[] MILESTONES = {25, 50, 75, 100};

    private final CodexData.Tab tab;
    private int scrollOffset = 0;

    public CodexDetailScreen(CodexData.Tab tab) {
        super(Component.literal(tab == CodexData.Tab.FARMER ? "농부 도감" : "광부 도감"));
        this.tab = tab;
    }

    private List<CodexRegistry.Entry> entries() {
        return tab == CodexData.Tab.FARMER ? CodexRegistry.FARMER : CodexRegistry.MINER;
    }
    private boolean isReg(String id) {
        return tab == CodexData.Tab.FARMER
            ? CodexClientCache.isFarmerRegistered(id)
            : CodexClientCache.isMinerRegistered(id);
    }

    private int bx()     { return 4; }
    private int by()     { return 4; }
    private int bw()     { return width - 8; }
    private int bh()     { return height - 8; }
    private int gx()     { return bx() + SIDEBAR_W + PAD; }
    private int gy()     { return by() + 18; }
    private int gridW()  { return bw() - SIDEBAR_W - REWARD_W - PAD * 2 - 6; }
    private int entryW() { return gridW() / COLS; }
    private int visRows(){ return Math.max(1, (bh() - 18 - 24) / ENTRY_H); }
    private int rpX()    { return bx() + bw() - REWARD_W; }

    @Override
    protected void init() {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        boolean farmer = tab == CodexData.Tab.FARMER;
        int accent = farmer ? 0xFF44BB22 : 0xFF2288CC;
        int bx=bx(), by=by(), bw=bw(), bh=bh();
        List<CodexRegistry.Entry> list = entries();
        int reg = farmer ? CodexClientCache.farmerCount() : CodexClientCache.minerCount();

        this.renderMenuBackground(g);
        g.fill(bx, by, bx+bw, by+bh, 0xF0080810);
        g.renderOutline(bx, by, bw, bh, accent);

        // 사이드바
        g.fill(bx, by, bx+SIDEBAR_W, by+bh, farmer ? 0xFF081808 : 0xFF08080E);
        g.renderOutline(bx, by, SIDEBAR_W, bh, accent);
        String tabName = farmer ? "농부도감" : "광부도감";
        int charY = by + bh/2 - (tabName.length()*9)/2;
        for (int i=0; i<tabName.length(); i++)
            g.drawCenteredString(font, String.valueOf(tabName.charAt(i)),
                bx+SIDEBAR_W/2, charY+i*9, accent);

        // 타이틀바
        g.fill(bx+SIDEBAR_W, by, bx+bw, by+16, 0xFF0A0A14);
        g.fill(bx+SIDEBAR_W, by+15, bx+bw, by+16, accent);
        g.drawString(font, farmer?"농부 도감":"광부 도감", bx+SIDEBAR_W+PAD, by+4, accent, false);
        g.drawString(font, reg+"/"+list.size()+" 종", bx+SIDEBAR_W+PAD+70, by+4, 0x555566, false);

        // 안내문 (인벤토리 열어서 등록)
        g.drawString(font, "§7인벤토리(I키)에서 아이템 클릭 → 등록",
            bx+SIDEBAR_W+PAD, by+4+10, 0x445544, false);

        // 뒤로가기
        int backX=bx+SIDEBAR_W+PAD, backY=by+bh-16;
        boolean backHov = mx>=backX && mx<=backX+52 && my>=backY && my<=backY+13;
        g.fill(backX, backY, backX+52, backY+13, backHov?0xFF2A2A3A:0xFF1A1A28);
        g.renderOutline(backX, backY, 52, 13, backHov?0xFF8888AA:0xFF444466);
        g.drawCenteredString(font, "< 목록", backX+26, backY+2, backHov?0xFFFFFF:0xAAAAAA);

        // ── 도감 그리드 ────────────────────────────────────────────
        int gx=gx(), gy=gy(), ew=entryW(), vis=visRows();
        int start=scrollOffset*COLS, end=Math.min(start+vis*COLS, list.size());

        // 1패스: 배경 + 아이콘
        for (int i=start; i<end; i++) {
            int rel=i-start, col=rel%COLS, row=rel/COLS;
            int ex=gx+col*ew, ey=gy+row*ENTRY_H;
            CodexRegistry.Entry entry=list.get(i);
            boolean registered=isReg(entry.id());
            boolean hov=mx>=ex&&mx<ex+ew-2&&my>=ey&&my<ey+ENTRY_H-2;
            int bg=registered?(farmer?0xAA0D2208:0xAA080D22):(hov?0x44222233:0x33111118);
            int bd=registered?accent:(hov?0xFF444466:0xFF202030);
            g.fill(ex, ey, ex+ew-2, ey+ENTRY_H-2, bg);
            g.renderOutline(ex, ey, ew-2, ENTRY_H-2, bd);
            Item item=CodexRegistry.getItem(entry);
            int iconY=ey+(ENTRY_H-2-16)/2;
            if (item!=null) {
                g.renderItem(new ItemStack(item), ex+3, iconY);
                if (!registered) g.fill(ex+3, iconY, ex+3+16, iconY+16, 0xAA000000);
            }
        }
        for (int i=end-start; i<vis*COLS; i++) {
            int col=i%COLS, row=i/COLS;
            g.fill(gx+col*ew, gy+row*ENTRY_H, gx+col*ew+ew-2, gy+row*ENTRY_H+ENTRY_H-2, 0x22111118);
            g.renderOutline(gx+col*ew, gy+row*ENTRY_H, ew-2, ENTRY_H-2, 0xFF181820);
        }

        // 2패스: 텍스트
        for (int i=start; i<end; i++) {
            int rel=i-start, col=rel%COLS, row=rel/COLS;
            int ex=gx+col*ew, ey=gy+row*ENTRY_H;
            CodexRegistry.Entry entry=list.get(i);
            boolean registered=isReg(entry.id());
            int nameMaxW=ew-2-3-16-6;
            String name=entry.name();
            if (font.width(name)>nameMaxW)
                name=font.substrByWidth(Component.literal(name),nameMaxW).getString()+"..";
            g.drawString(font, name, ex+3+16+4, ey+(ENTRY_H-2-font.lineHeight)/2,
                registered?0xEEEEEE:0x666677, false);
            if (registered) g.drawString(font, "✔", ex+ew-12, ey+2, 0x33FF11, false);
        }

        // 스크롤바
        int totalRows=(int)Math.ceil(list.size()/(float)COLS);
        int maxScroll=Math.max(0, totalRows-vis);
        int sbX=gx+gridW()+2, sbY=gy, sbH=vis*ENTRY_H;
        g.fill(sbX, sbY, sbX+4, sbY+sbH, 0xFF141420);
        if (maxScroll>0) {
            int thumbH=Math.max(10, sbH*vis/totalRows);
            int thumbY=sbY+(sbH-thumbH)*scrollOffset/maxScroll;
            g.fill(sbX, thumbY, sbX+4, thumbY+thumbH, accent);
        }

        // 보상 패널
        renderRewardPanel(g, farmer, accent, reg, list.size());
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        int backX=bx()+SIDEBAR_W+PAD, backY=by()+bh()-16;
        if (mx>=backX && mx<=backX+52 && my>=backY && my<=backY+13) {
            minecraft.setScreen(new CodexMainScreen());
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int totalRows=(int)Math.ceil(entries().size()/(float)COLS);
        int maxScroll=Math.max(0, totalRows-visRows());
        scrollOffset=(int)Math.max(0, Math.min(scrollOffset+(sy<0?1:-1), maxScroll));
        return true;
    }

    private void renderRewardPanel(GuiGraphics g, boolean farmer, int accent, int reg, int total) {
        int rx=rpX(), ry=by()+16, rw=REWARD_W, rh=bh()-16-20;
        g.fill(rx, ry, rx+rw, ry+rh, 0xFF0A0A16);
        g.renderOutline(rx, ry, rw, rh, accent);
        g.fill(rx, ry, rx+rw, ry+11, 0xFF111122);
        g.fill(rx, ry+10, rx+rw, ry+11, 0xFF333355);
        g.drawCenteredString(font, "달성 보상", rx+rw/2, ry+2, 0x8888AA);
        int pct=total==0?0:reg*100/total;
        int barY=ry+14;
        g.fill(rx+4, barY, rx+rw-4, barY+5, 0xFF141420);
        g.fill(rx+4, barY, rx+4+(rw-8)*pct/100, barY+5, farmer?0xFF33AA11:0xFF1155CC);
        g.renderOutline(rx+4, barY, rw-8, 5, farmer?0xFF335522:0xFF224466);
        g.drawCenteredString(font, pct+"% "+reg+"/"+total, rx+rw/2, barY+6, 0x555566);
        int listTop=barY+15, listBot=ry+rh-4, slotH=(listBot-listTop)/4;
        for (int mi=0; mi<MILESTONES.length; mi++) {
            int ms=MILESTONES[mi], sy=listTop+mi*slotH;
            boolean done=pct>=ms, next=!done&&ms==getNextMilestone(pct);
            if (mi>0) g.fill(rx+4, sy, rx+rw-4, sy+1, 0xFF1A1A28);
            if (next) g.fill(rx+4, sy+1, rx+rw-4, sy+slotH-1, 0x33665500);
            g.fill(rx+5, sy+4, rx+8, sy+7, done?0xFF33FF11:(next?0xFFFFD700:0xFF2A2A3A));
            g.drawString(font, (done?"✔ ":(next?"▶ ":"  "))+ms+"%", rx+9, sy+2,
                done?0x338822:(next?0xFFCC00:0x333355), false);
            String[] rl=getRewardLines(ms, farmer);
            if (rl.length>0) g.drawString(font, rl[0], rx+9, sy+10, done?0x334422:(next?0xDDCC66:0x2A2A3A), false);
            if (rl.length>1) g.drawString(font, rl[1], rx+9, sy+18, done?0x334422:(next?0xDDCC66:0x2A2A3A), false);
            if (next) {
                int need=(int)Math.ceil(ms*total/100.0)-reg;
                g.drawString(font, Math.max(0,need)+"종 남음", rx+9, sy+26, 0xFFAA44, false);
            }
        }
    }

    private int getNextMilestone(int pct) {
        for (int ms:MILESTONES) if (pct<ms) return ms; return -1;
    }
    private String[] getRewardLines(int ms, boolean farmer) {
        return switch(ms) {
            case 25  -> new String[]{"코인 500","강화석 ×5"};
            case 50  -> new String[]{"코인 1500","강화석 ×5"};
            case 75  -> new String[]{"코인 3000",farmer?"성장속도+10%":"채굴속도+10%"};
            case 100 -> new String[]{"코인 5000",farmer?"농부 칭호":"광부 칭호"};
            default  -> new String[]{};
        };
    }

    @Override public boolean isPauseScreen() { return false; }
}
