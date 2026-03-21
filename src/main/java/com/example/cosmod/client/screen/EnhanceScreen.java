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

import java.util.ArrayList;
import java.util.List;

public class EnhanceScreen extends Screen {

    private static final int INV_COLS=9,INV_ROWS=4,S=18,PAD=7;
    private static final int W=PAD*2+INV_COLS*S,VIZ_H=96,MSG_H=14,BTN_H=18;
    private static final int INV_H=INV_ROWS*S+4,H=VIZ_H+MSG_H+BTN_H+6+INV_H+6;
    private static final int C_BG=0xF0_1C1408,C_BORDER=0xFF_9B7820,C_BDR2=0xFF_5A4010;
    private static final int C_GOLD=0xFFD4A847,C_GDIM=0xFF7A5C18,C_TEXT=0xFFCCBB99;
    private static final int C_SLOT=0xFF_0C0804,C_GREEN=0xFF55CC77;

    private ItemStack gearSlot=ItemStack.EMPTY,stoneSlot=ItemStack.EMPTY;
    private int gearInvIdx=-1,stoneInvIdx=-1;
    private ItemStack resultStack=ItemStack.EMPTY;
    private boolean enhanced=false;
    private String resultMsg=null;
    private int resultColor=0x55FF55;
    private boolean resultSpecial=false;
    private ItemStack hoveredStack=ItemStack.EMPTY;

    public EnhanceScreen() { super(Component.literal("장비 강화")); }
    private int bx() { return this.width/2-W/2; }
    private int by() { return this.height/2-H/2; }
    @Override protected void init() { rebuildWidgets(); }

    @Override protected void rebuildWidgets() {
        this.clearWidgets();
        Player player = Minecraft.getInstance().player;
        if (player==null) return;
        int bx=bx(),by=by();
        int[] gxy=gearXY();
        this.addRenderableWidget(Button.builder(Component.empty(),btn->handleGearSlotClick()).bounds(gxy[0],gxy[1],26,26).build());
        int[] sxy=stoneXY();
        this.addRenderableWidget(Button.builder(Component.empty(),btn->handleStoneSlotClick()).bounds(sxy[0],sxy[1],16,16).build());
        int btnY=by+VIZ_H+MSG_H+3;
        this.addRenderableWidget(Button.builder(Component.literal("강화"),btn->doEnhance()).bounds(bx+PAD,btnY,W-PAD*2,BTN_H).build());
        int invY0=by+VIZ_H+MSG_H+BTN_H+9;
        int[] displayOrder=new int[36];
        for(int i=0;i<27;i++) displayOrder[i]=i+9;
        for(int i=0;i<9;i++) displayOrder[27+i]=i;
        for(int i=0;i<36;i++) {
            final int invIdx=displayOrder[i];
            int col=i%INV_COLS,row=i/INV_COLS;
            int ix=bx+PAD+col*S,iy=invY0+row*S+(row==3?4:0);
            this.addRenderableWidget(Button.builder(Component.empty(),btn->handleInvClick(player,invIdx)).bounds(ix,iy,S-1,S-1).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("×"),btn->this.onClose()).bounds(bx+W-13,by+3,10,10).build());
    }

    private void handleGearSlotClick() {
        Player player=Minecraft.getInstance().player; if(player==null) return;
        if(!gearSlot.isEmpty()) {
            if(!player.getInventory().add(gearSlot.copy())) player.drop(gearSlot.copy(),false);
            gearSlot=ItemStack.EMPTY; gearInvIdx=-1; resultStack=ItemStack.EMPTY; enhanced=false; rebuildWidgets();
        }
    }
    private void handleStoneSlotClick() {
        Player player=Minecraft.getInstance().player; if(player==null) return;
        if(!stoneSlot.isEmpty()) {
            if(!player.getInventory().add(stoneSlot.copy())) player.drop(stoneSlot.copy(),false);
            stoneSlot=ItemStack.EMPTY; stoneInvIdx=-1; rebuildWidgets();
        }
    }
    private void handleInvClick(Player player, int invIdx) {
        ItemStack s=player.getInventory().getItem(invIdx);
        if(s.isEmpty()) return;
        if(s.getItem() instanceof GearItem) {
            if(!gearSlot.isEmpty()) { player.getInventory().setItem(invIdx,gearSlot.copy()); gearSlot=s.copy(); gearInvIdx=invIdx; }
            else { gearSlot=s.copy(); gearInvIdx=invIdx; player.getInventory().setItem(invIdx,ItemStack.EMPTY); }
            resultStack=ItemStack.EMPTY; enhanced=false;
        } else if(s.getItem()==CombatItems.ENHANCE_STONE_BASIC||s.getItem()==CombatItems.ENHANCE_STONE_MID
                ||s.getItem()==CombatItems.ENHANCE_STONE_HIGH||s.getItem() instanceof com.example.cosmod.combat.GemItem) {
            if(!stoneSlot.isEmpty()) { ItemStack ret=stoneSlot.copy(); if(!player.getInventory().add(ret)) player.drop(ret,false); }
            ItemStack one=s.copy(); one.setCount(1); stoneSlot=one; stoneInvIdx=invIdx; s.shrink(1);
        }
        rebuildWidgets();
    }

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        int bx=bx(),by=by();
        hoveredStack=ItemStack.EMPTY;
        Player player=Minecraft.getInstance().player;
        g.fill(bx-1,by-1,bx+W+1,by+H+1,C_BORDER);
        g.fill(bx,by,bx+W,by+H,C_BG);
        g.renderOutline(bx+1,by+1,W-2,H-2,C_BDR2);
        if(player==null){super.render(g,mx,my,delta);return;}
        super.render(g,mx,my,delta);
        renderViz(g,bx,by,mx,my);
        g.fill(bx+4,by+VIZ_H,bx+W-4,by+VIZ_H+1,C_BDR2);
        int msgY=by+VIZ_H+3;
        String displayMsg; int displayColor;
        if(resultMsg!=null&&enhanced){displayMsg=resultMsg;displayColor=resultColor;}
        else if(!gearSlot.isEmpty()){displayMsg=gearSlot.getHoverName().getString();displayColor=0xFFCCBB99;}
        else{displayMsg="강화 장비를 선택해주세요.";displayColor=0xFFCCBB99;}
        if(resultSpecial&&enhanced){long tick=System.currentTimeMillis()/200;int[]cols={0xFFD700,0xFF8C00,0xFFFF00,0xFFA500};displayColor=cols[(int)(tick%cols.length)];}
        g.drawCenteredString(font,displayMsg,bx+W/2,msgY,displayColor);
        int btnY=by+VIZ_H+MSG_H+3;
        g.renderOutline(bx+PAD-1,btnY-1,W-PAD*2+2,BTN_H+2,C_BORDER);
        int divY=btnY+BTN_H+3;
        g.fill(bx+4,divY,bx+W-4,divY+1,C_BDR2);
        renderInventory(g,bx,divY+5,mx,my,player);
        g.drawString(font,"×",bx+W-12,by+4,0xFF887766,false);
        if(!hoveredStack.isEmpty()) renderMcTooltip(g,buildTooltip(hoveredStack,player),mx,my);
    }

    private void renderViz(GuiGraphics g, int bx, int by, int mx, int my) {
        int cy=by+VIZ_H/2,lineY=cy+2;
        int[] gxy=gearXY();
        drawFancySlot(g,gxy[0],gxy[1],26,gearSlot,!gearSlot.isEmpty());
        if(!gearSlot.isEmpty()&&mx>=gxy[0]&&mx<gxy[0]+26&&my>=gxy[1]&&my<gxy[1]+26) hoveredStack=gearSlot;
        int[] sxy=stoneXY();
        drawFancySlot(g,sxy[0],sxy[1],16,stoneSlot,!stoneSlot.isEmpty());
        int lx1=gxy[0]+28,dmx=bx+W/2+10;
        g.fill(lx1,lineY,bx+W-52,lineY+1,C_GDIM);
        drawDiamond(g,dmx,lineY,13,C_GDIM,C_BG); drawDiamond(g,dmx,lineY,10,C_BDR2,C_BG);
        drawDiamond(g,dmx,lineY,6,C_GDIM,C_BG); drawDiamond(g,dmx,lineY,2,C_GOLD,C_BG);
        int rsx=bx+W-48,rsy=cy-22,rsz=42,rcx=rsx+rsz/2,rcy=rsy+rsz/2;
        drawDiamond(g,rcx,rcy,30,C_BDR2,C_BG); drawDiamond(g,rcx,rcy,26,C_BG,C_BG);
        drawDiamond(g,rcx,rcy,23,C_BDR2,C_BG); drawDiamond(g,rcx,rcy,19,C_BG,C_BG);
        drawDiamond(g,rcx,rcy,17,C_BORDER,C_BG);
        boolean glow=enhanced&&!resultStack.isEmpty();
        if(glow) g.fill(rsx-1,rsy-1,rsx+rsz+1,rsy+rsz+1,0x55_FFD700);
        g.fill(rsx-1,rsy-1,rsx+rsz+1,rsy+rsz+1,glow?C_GOLD:C_BORDER);
        g.fill(rsx,rsy,rsx+rsz,rsy+rsz,C_SLOT);
        g.renderOutline(rsx+2,rsy+2,rsz-4,rsz-4,glow?C_GDIM:C_BDR2);
        if(!resultStack.isEmpty()){g.renderItem(resultStack,rsx+(rsz-16)/2,rsy+(rsz-16)/2);if(mx>=rsx&&mx<rsx+rsz&&my>=rsy&&my<rsy+rsz)hoveredStack=resultStack;}
        else{drawDiamond(g,rcx,rcy,6,C_BDR2,C_SLOT);drawDiamond(g,rcx,rcy,2,C_GDIM,C_SLOT);}
        g.fill(dmx+14,lineY,rsx,lineY+1,C_GDIM);
        if(enhanced&&!resultStack.isEmpty()){CompoundTag tag=GearItem.getGearTag(resultStack);int enhLv=tag.contains("enhance_level")?tag.getInt("enhance_level").orElse(0):0;int ly=by+VIZ_H-28;g.fill(bx+4,ly-2,bx+W-4,ly-1,C_BDR2);g.drawString(font,"강화: §e+"+enhLv,bx+PAD+2,ly,0xFFCCBB99,false);}
    }

    private void renderInventory(GuiGraphics g, int bx, int invY0, int mx, int my, Player player) {
        int[] displayOrder=new int[36];
        for(int i=0;i<27;i++) displayOrder[i]=i+9;
        for(int i=0;i<9;i++) displayOrder[27+i]=i;
        for(int i=0;i<36;i++) {
            int col=i%INV_COLS,row=i/INV_COLS;
            int ix=bx+PAD+col*S,iy=invY0+row*S+(row==3?4:0);
            if(row==3&&col==0) g.fill(bx+PAD,iy-2,bx+W-PAD,iy-1,C_BDR2);
            int invIdx=displayOrder[i];
            ItemStack s=player.getInventory().getItem(invIdx);
            g.fill(ix-1,iy-1,ix+S,iy+S,0xFF_373737);
            g.fill(ix,iy,ix+S-1,iy+S-1,0xFF_8B8B8B);
            g.fill(ix+1,iy+1,ix+S-1,iy+S-1,0xFF_575757);
            if(!s.isEmpty()&&(s.getItem() instanceof GearItem||s.getItem()==CombatItems.ENHANCE_STONE_BASIC||s.getItem()==CombatItems.ENHANCE_STONE_MID||s.getItem()==CombatItems.ENHANCE_STONE_HIGH||s.getItem() instanceof com.example.cosmod.combat.GemItem)) {
                if(mx>=ix&&mx<ix+S-1&&my>=iy&&my<iy+S-1) g.fill(ix+1,iy+1,ix+S-1,iy+S-1,0x44_FFFFFF);
            }
            if(!s.isEmpty()){g.renderItem(s,ix+1,iy+1);g.renderItemDecorations(font,s,ix+1,iy+1);if(mx>=ix&&mx<ix+S-1&&my>=iy&&my<iy+S-1)hoveredStack=s;}
        }
    }

    private void drawFancySlot(GuiGraphics g, int x, int y, int sz, ItemStack stack, boolean active) {
        int bc=active?C_GOLD:C_BORDER,d=3;
        g.fill(x-2,y-2,x-2+d,y-1,bc);g.fill(x-2,y-2,x-1,y-2+d,bc);
        g.fill(x+sz-d+2,y-2,x+sz+2,y-1,bc);g.fill(x+sz+1,y-2,x+sz+2,y-2+d,bc);
        g.fill(x-2,y+sz+1,x-2+d,y+sz+2,bc);g.fill(x-2,y+sz-d+2,x-1,y+sz+2,bc);
        g.fill(x+sz-d+2,y+sz+1,x+sz+2,y+sz+2,bc);g.fill(x+sz+1,y+sz-d+2,x+sz+2,y+sz+2,bc);
        g.fill(x,y,x+sz,y+sz,C_SLOT);
        g.renderOutline(x,y,sz,sz,active?C_GDIM:C_BDR2);
        if(stack.isEmpty()){if(sz>=26)g.drawCenteredString(font,"§8+",x+sz/2,y+sz/2-3,0x444444);}
        else g.renderItem(stack,x+(sz-16)/2,y+(sz-16)/2);
    }
    private void drawDiamond(GuiGraphics g, int cx, int cy, int r, int color, int bg) {
        for(int dy=-r;dy<=r;dy++){int w=r-Math.abs(dy);g.fill(cx-w,cy+dy,cx+w+1,cy+dy+1,color);}
        for(int dy=-(r-1);dy<=r-1;dy++){int w=r-1-Math.abs(dy);g.fill(cx-w,cy+dy,cx+w+1,cy+dy+1,bg);}
    }
    private int[] gearXY() { return new int[]{bx()+10,by()+VIZ_H/2-10}; }
    private int[] stoneXY(){ return new int[]{bx()+10,by()+VIZ_H/2+20}; }

    public void setResultMessage(String msg, int color, boolean special) {
        this.resultMsg=msg; this.resultColor=color; this.resultSpecial=special;
        this.enhanced=true; rebuildWidgets();
    }

    private void doEnhance() {
        Player player=Minecraft.getInstance().player; if(player==null) return;
        if(gearSlot.isEmpty()){player.displayClientMessage(Component.literal("§c장비 슬롯이 비어있습니다."),false);return;}
        if(stoneSlot.isEmpty()){player.displayClientMessage(Component.literal("§c강화석 슬롯이 비어있습니다."),false);return;}
        if(gearInvIdx<0||stoneInvIdx<0){player.displayClientMessage(Component.literal("§c슬롯 정보가 올바르지 않습니다."),false);return;}
        int stoneType=-1;
        if(stoneSlot.getItem()==CombatItems.ENHANCE_STONE_BASIC) stoneType=0;
        else if(stoneSlot.getItem()==CombatItems.ENHANCE_STONE_MID)  stoneType=1;
        else if(stoneSlot.getItem()==CombatItems.ENHANCE_STONE_HIGH) stoneType=2;
        else if(stoneSlot.getItem() instanceof com.example.cosmod.combat.GemItem) stoneType=3;
        if(stoneType<0){player.displayClientMessage(Component.literal("§c올바른 강화석이 아닙니다."),false);return;}
        resultStack=gearSlot.copy(); resultMsg="강화 요청 중..."; resultColor=0xAAAAAA; resultSpecial=false;
        ClientPlayNetworking.send(new EnhanceRequestPayload(gearInvIdx,stoneInvIdx,stoneType));
        gearSlot=ItemStack.EMPTY; stoneSlot=ItemStack.EMPTY; gearInvIdx=-1; stoneInvIdx=-1;
    }

    private void renderMcTooltip(GuiGraphics g, List<Component> lines, int mx, int my) {
        if(lines.isEmpty()) return;
        int maxWidth=0;
        for(Component line:lines) maxWidth=Math.max(maxWidth,font.width(line));
        int lineH=font.lineHeight+2,totalH=lines.size()*lineH-2,padX=4,padY=4;
        int boxW=maxWidth+padX*2,boxH=totalH+padY*2;
        int tx=Math.min(mx+12,this.width-boxW-4),ty=Math.max(Math.min(my-12,this.height-boxH-4),4);
        g.fill(tx,ty,tx+boxW,ty+boxH,0xF0100010);
        g.fill(tx,ty,tx+boxW,ty+1,0xFF5000FF);g.fill(tx,ty+boxH-1,tx+boxW,ty+boxH,0xFF5000FF);
        g.fill(tx,ty,tx+1,ty+boxH,0xFF5000FF);g.fill(tx+boxW-1,ty,tx+boxW,ty+boxH,0xFF5000FF);
        int ly=ty+padY;
        for(int i=0;i<lines.size();i++){
            g.drawString(font,lines.get(i),tx+padX,ly,0xFFFFFFFF,true);
            ly+=lineH;
            if(i==0&&lines.size()>1){g.fill(tx+padX,ly-1,tx+boxW-padX,ly,0xFF505050);ly+=1;}
        }
    }

    private List<Component> buildTooltip(ItemStack stack, Player player) {
        List<Component> lines=new ArrayList<>();
        lines.add(stack.getHoverName().copy());
        stack.getItem().appendHoverText(stack,net.minecraft.world.item.Item.TooltipContext.EMPTY,
            new net.minecraft.world.item.component.TooltipDisplay(true,new java.util.LinkedHashSet<>()),
            lines::add,net.minecraft.world.item.TooltipFlag.Default.NORMAL);
        return lines;
    }

    @Override public void onClose() {
        Player player=Minecraft.getInstance().player;
        if(player!=null){
            if(!gearSlot.isEmpty()){if(!player.getInventory().add(gearSlot.copy()))player.drop(gearSlot.copy(),false);gearSlot=ItemStack.EMPTY;}
            if(!stoneSlot.isEmpty()){if(!player.getInventory().add(stoneSlot.copy()))player.drop(stoneSlot.copy(),false);stoneSlot=ItemStack.EMPTY;}
        }
        super.onClose();
    }
    @Override public boolean isPauseScreen() { return false; }
}
