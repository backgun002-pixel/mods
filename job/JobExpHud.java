package com.example.cosmod.job;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class JobExpHud {

    public static void register() {
        HudRenderCallback.EVENT.register((g, dt) -> render(g));
    }

    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (JobExpClientCache.level <= 0) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        var font = mc.font;

        // ── EXP 바 ────────────────────────────────────────────────
        int barW = 182;
        int barH = 3;
        int barX = sw / 2 - barW / 2;
        int barY = sh - 49;

        g.fill(barX, barY, barX + barW, barY + barH, 0xCC000000);

        float ratio = JobExpClientCache.expToNext > 0
            ? (float) JobExpClientCache.exp / JobExpClientCache.expToNext : 0f;
        ratio = Math.min(1f, Math.max(0f, ratio));
        int filled = (int)(barW * ratio);

        boolean flash = JobExpClientCache.levelUpTimer > 0
            && (JobExpClientCache.levelUpTimer % 6) < 3;
        int expColor = flash ? 0xFFFFFFAA : 0xFF55FF55;

        if (filled > 0) g.fill(barX, barY, barX + filled, barY + barH, expColor);

        String lvTxt = "Lv." + JobExpClientCache.level;
        g.drawString(font, lvTxt, barX - font.width(lvTxt) - 2, barY - 1, 0xFFAAAAAA, false);

        // ── 레벨업 연출 (화면 중앙, SkillUnlockHud 스타일) ────────
        int t = JobExpClientCache.levelUpTimer;
        if (t <= 0) return;

        int alpha = t < 20 ? (int)(255f * t / 20f) : 255;
        int bgAlpha = Math.min(alpha, 140);

        // 배경 어둡게
        g.fill(0, sh/2 - 38, sw, sh/2 + 38, (bgAlpha << 24));

        // 반짝이는 가로 라인
        boolean lineFlash = (t % 8) < 4;
        int lineAlpha = lineFlash ? alpha/2 : alpha/3;
        int lineColor = (lineAlpha << 24) | 0xFFDD00;
        g.fill(0, sh/2 - 16, sw, sh/2 - 14, lineColor);
        g.fill(0, sh/2 + 14, sw, sh/2 + 16, lineColor);

        // "LEVEL UP!" 텍스트
        String line1 = "✦  LEVEL UP!  ✦";
        int w1 = font.width(line1);
        int c1 = (alpha << 24) | 0xFFDD00;
        g.drawString(font, line1, sw/2 - w1/2 + 1, sh/2 - 12 + 1, (alpha/3 << 24) | 0x000000, false);
        g.drawString(font, line1, sw/2 - w1/2, sh/2 - 12, c1, false);

        // 레벨 표시
        String line2 = "Lv." + JobExpClientCache.level;
        int w2 = font.width(line2);
        int c2 = (alpha << 24) | 0xFFFFFF;
        g.drawString(font, line2, sw/2 - w2/2 + 1, sh/2 + 2 + 1, (alpha/3 << 24) | 0x000000, false);
        g.drawString(font, line2, sw/2 - w2/2, sh/2 + 2, c2, false);
    }
}
