package com.example.cosmod.client.screen;

import com.example.cosmod.job.JobExpClientCache;
import com.example.cosmod.stat.StatClientCache;
import com.example.cosmod.stat.StatKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StatScreen extends Screen {

    private static final int W = 210;
    private static final int H = 225;
    private int left, top;

    private static final int C_BG      = 0xEE0A1628;
    private static final int C_PANEL   = 0xCC102040;
    private static final int C_BORDER  = 0xFF2A5A8A;
    private static final int C_BORDER2 = 0xFF1A3A5A;
    private static final int C_TITLE_BG= 0xDD0D1F35;
    private static final int C_GOLD    = 0xFFFFD700;
    private static final int C_WHITE   = 0xFFFFFFFF;
    private static final int C_LBLUE   = 0xFF88CCFF;
    private static final int C_GRAY    = 0xFFAAAAAA;
    private static final int C_YELLOW  = 0xFFFFFF55;
    private static final int C_HP_BAR  = 0xFFE03030;
    private static final int C_HP_BG   = 0xFF401010;
    private static final int C_EXP_BAR = 0xFF40C0FF;
    private static final int C_EXP_BG  = 0xFF102040;
    private static final int C_DIVIDER = 0xFF1E4060;

    public StatScreen() {
        super(Component.literal("스탯"));
    }

    @Override
    protected void init() {
        left = (this.width  - W) / 2;
        top  = (this.height - H) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // ── 전체 배경 ────────────────────────────────────────────
        g.fill(left, top, left+W, top+H, C_BG);

        // 외곽 테두리
        g.fill(left,     top,     left+W,   top+2,   C_BORDER);
        g.fill(left,     top+H-2, left+W,   top+H,   C_BORDER);
        g.fill(left,     top,     left+2,   top+H,   C_BORDER);
        g.fill(left+W-2, top,     left+W,   top+H,   C_BORDER);
        g.fill(left+2,   top+2,   left+W-2, top+4,   C_BORDER2);
        g.fill(left+2,   top+H-4, left+W-2, top+H-2, C_BORDER2);
        g.fill(left+2,   top+2,   left+4,   top+H-2, C_BORDER2);
        g.fill(left+W-4, top+2,   left+W-2, top+H-2, C_BORDER2);

        // ── 타이틀 ────────────────────────────────────────────────
        int titleH = 22;
        g.fill(left+4, top+4, left+W-4, top+4+titleH, C_TITLE_BG);
        g.drawString(font, "§b" + mc.player.getName().getString(),
            left+10, top+10, C_WHITE, false);
        String coinStr = "§6✦ " + StatClientCache.coins + " 코인";
        g.drawString(font, coinStr,
            left+W - font.width("✦ " + StatClientCache.coins + " 코인") - 14, top+10, C_GOLD, false);
        g.fill(left+4, top+4+titleH, left+W-4, top+5+titleH, C_DIVIDER);

        // ── 중단 캐릭터 영역 ──────────────────────────────────────
        int midY = top + 24;

        // 플레이어 얼굴
        int faceX = left+10, faceY = midY+4;
        renderPlayerFace(g, faceX, faceY, 24, mc);
        g.renderOutline(faceX-2, faceY-2, 28, 28, C_BORDER2);

        // 직업/레벨 정보
        int infoX = faceX+32;
        g.drawString(font, "§7전투직업: §e" + StatClientCache.jobName,    infoX, midY+4,  C_WHITE, false);
        g.drawString(font, "§7생활직업: §a" + StatClientCache.lifeJobName, infoX, midY+14, C_WHITE, false);
        g.drawString(font, "§fLv §b" + StatClientCache.jobLevel,           infoX, midY+26, C_WHITE, false);

        // HP 바
        int barX = infoX, barW = W-10-32-16, barH = 6;
        int hpY  = midY+42;
        float currHp = mc.player.getHealth(), maxHp = StatClientCache.maxHp;
        float hpRatio = maxHp > 0 ? Math.min(1f, currHp/maxHp) : 0;
        g.fill(barX, hpY, barX+barW, hpY+barH, C_HP_BG);
        g.fill(barX, hpY, barX+(int)(barW*hpRatio), hpY+barH, C_HP_BAR);
        g.renderOutline(barX, hpY, barW, barH, C_BORDER2);
        String hpText = "HP " + (int)currHp + " / " + (int)maxHp;
        g.drawString(font, "§c" + hpText,
            barX+barW/2 - font.width(hpText)/2, hpY+1, C_WHITE, true);

        // EXP 바
        int expY = hpY+12;
        int exp = JobExpClientCache.exp, expToNext = JobExpClientCache.expToNext;
        float expRatio = expToNext > 0 ? Math.min(1f, (float)exp/expToNext) : 0;
        g.fill(barX, expY, barX+barW, expY+barH, C_EXP_BG);
        g.fill(barX, expY, barX+(int)(barW*expRatio), expY+barH, C_EXP_BAR);
        g.renderOutline(barX, expY, barW, barH, C_BORDER2);
        String expText = "EXP " + exp + " / " + expToNext;
        g.drawString(font, "§b" + expText,
            barX+barW/2 - font.width(expText)/2, expY+1, C_WHITE, true);

        // ── 구분선 ────────────────────────────────────────────────
        int divY = midY+62;
        g.fill(left+6, divY, left+W-6, divY+1, C_DIVIDER);
        g.fill(left+4,   divY-2, left+8,   divY+3, C_BORDER);
        g.fill(left+W-8, divY-2, left+W-4, divY+3, C_BORDER);

        // ── 스탯 목록 ────────────────────────────────────────────
        int statY  = divY+11;
        int col1X  = left+12;
        int col2X  = left+W/2+2;
        int lineH  = 13;

        // 공격력: MC 내부값 (검=7.0, 맨손=1.0 기준 데미지)
        // 실제 체력 피해 = 표시값 / 2 하트
        Object[][] stats = {
            {"§f공격력",        String.format("%.1f",  StatClientCache.atkFlat)},
            {"§f공격력 증가%",  StatClientCache.atkPct > 0
                ? String.format("+%.0f%%", StatClientCache.atkPct)
                : "§70%"},
            {"§f방어력",        String.format("%.0f",  StatClientCache.def)},
            {"§f최대 체력",     String.format("%.0f♥", StatClientCache.maxHp / 2)},
            {"§f이동속도 증가", StatClientCache.spdPct > 0.1f
                ? String.format("+%.1f%%", StatClientCache.spdPct)
                : "§7기본"},
            {"§f치명타 확률",   String.format("%.0f%%", StatClientCache.critChance)},
            {"§f치명타 데미지", String.format("+%.0f%%",StatClientCache.critDmg)},
            {"§f점프력",        String.format("%.2f",  StatClientCache.jump)},
        };

        // ── 스탯 목록 ────────────────────────────────────────
        for (int i = 0; i < stats.length; i++) {
            int x = (i%2==0) ? col1X : col2X;
            int y = statY + (i/2)*lineH;
            String label = (String)stats[i][0];
            String value = (String)stats[i][1];
            g.drawString(font, label, x, y, C_LBLUE, false);
            int valX = (i%2==0)
                ? col2X-6 - font.width(value)
                : left+W-12 - font.width(value);
            g.drawString(font, "§e"+value, valX, y, C_YELLOW, false);
        }

        // 행 구분선
        for (int i=1; i<=stats.length/2; i++) {
            int ly = statY + i*lineH - 3;
            g.fill(left+8, ly, left+W-8, ly+1, C_DIVIDER);
        }

        // ── 직업 보너스 테이블 ────────────────────────────────
        int bonusY = statY + (stats.length/2) * lineH + 2;
        g.fill(left+6, bonusY, left+W-6, bonusY+1, C_DIVIDER);
        g.fill(left+4, bonusY-2, left+8,   bonusY+3, C_BORDER);
        g.fill(left+W-8, bonusY-2, left+W-4, bonusY+3, C_BORDER);
        bonusY += 3;

        String jobForBonus = StatClientCache.jobName;
        int curLv = StatClientCache.jobLevel;
        String[][] bonusTable = getBonusTable(jobForBonus);

        g.drawCenteredString(font, "§b§l직업 보너스", left+W/2, bonusY, C_LBLUE);
        bonusY += 7;

        for (String[] row : bonusTable) {
            int reqLv = Integer.parseInt(row[0]);
            String desc = row[1];
            boolean unlocked = curLv >= reqLv;
            int badgeColor = unlocked ? 0xFF44AA44 : 0xFF444444;
            int textColor  = unlocked ? 0xFFFFFFFF : 0xFF888888;
            g.fill(left+10, bonusY-1, left+30, bonusY+8, badgeColor);
            g.drawCenteredString(font, "§fLv"+reqLv, left+20, bonusY, 0xFFFFFFFF);
            String prefix = unlocked ? "§a✔ " : "§7✘ ";
            g.drawString(font, prefix+(unlocked?"§f":"§8")+desc, left+34, bonusY, textColor, false);
            bonusY += 8;
        }

        // 하단 힌트
        g.drawCenteredString(font, "§7[ESC] 닫기   [O] 토글", left+W/2, top+H-14, C_GRAY);

        // 코너 장식
        int cs=4;
        g.fill(left+2,     top+2,     left+2+cs, top+2+cs, C_BORDER);
        g.fill(left+W-2-cs,top+2,     left+W-2,  top+2+cs, C_BORDER);
        g.fill(left+2,     top+H-2-cs,left+2+cs, top+H-2,  C_BORDER);
        g.fill(left+W-2-cs,top+H-2-cs,left+W-2,  top+H-2,  C_BORDER);

        super.render(g, mx, my, delta);
    }

    /** 플레이어 아바타 (이름 이니셜 기반 아이콘) */
    private void renderPlayerFace(GuiGraphics g, int x, int y, int size, Minecraft mc) {
        // 배경 원형 느낌 (사각형)
        g.fill(x, y, x+size, y+size, 0xFF1A3A5A);
        g.renderOutline(x, y, size, size, 0xFF4488CC);
        // 플레이어 머리 아이템 렌더링 (스킨 대신 플레이어 머리 아이템으로 대체)
        try {
            net.minecraft.world.item.ItemStack headStack =
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
            // SkullMeta: 플레이어 UUID 지정
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            net.minecraft.nbt.CompoundTag profile = new net.minecraft.nbt.CompoundTag();
            profile.putString("name", mc.player.getName().getString());
            tag.put("SkullOwner", profile);
            headStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
            g.renderItem(headStack, x + (size-16)/2, y + (size-16)/2);
        } catch (Exception e) {
            // 폴백: 이니셜
            String initial = mc.player != null
                ? String.valueOf(mc.player.getName().getString().charAt(0)).toUpperCase() : "?";
            g.drawCenteredString(font, "§b§l" + initial, x+size/2, y+size/2-4, 0xFFFFFFFF);
        }
    }

    private String[][] getBonusTable(String jobName) {
        return switch (jobName) {
            case "전사"   -> new String[][]{{"5","근접 공격력 +5%"},{"10","방어력 +3"},{"15","칼날폭풍 해금"},{"20","광폭화 해금"}};
            case "궁수"   -> new String[][]{{"5","화살 데미지 +5%"},{"10","이동속도 +5%"},{"15","애로우레인 해금"},{"20","샤프아이즈 해금"}};
            case "마법사" -> new String[][]{{"5","매직미사일 해금"},{"10","최대 체력 +4"},{"15","힐 해금"},{"20","인피니티 + 미사일 3발"}};
            case "무도가" -> new String[][]{{"5","근접 공격력 +5%"},{"10","이동속도 +5%"},{"15","파쇄장 해금"},{"20","정신수양 해금"}};
            case "광부"   -> new String[][]{{"5","광석 추가 드롭 +10%"},{"10","채굴속도 +15%"},{"15","희귀 광석 드롭 +5%"},{"20","포춘 자동 적용"}};
            case "농사꾼" -> new String[][]{{"5","작물 2배 드롭 +15%"},{"10","판매가 +10%"},{"15","즉시 성장 확률 +5%"},{"20","수확 EXP 2배"}};
            case "요리사" -> new String[][]{{"5","허기 회복 +1"},{"10","식사 시 재생 효과"},{"15","포션 지속 +25%"},{"20","특수 레시피 해금"}};
            default       -> new String[][]{{"5","-"},{"10","-"},{"15","-"},{"20","-"}};
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
