package com.example.cosmod.skill;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class SkillUnlockHud {

    private static String unlockSkillName = null;
    private static int timer = 0;
    private static final int DURATION = 100; // 5초

    public static void trigger(String skillName) {
        unlockSkillName = skillName;
        timer = DURATION;
    }

    public static void tick() {
        if (timer > 0) timer--;
    }

    public static void register() {
        HudRenderCallback.EVENT.register((g, dt) -> render(g));
    }

    private static void render(GuiGraphics g) {
        if (timer <= 0 || unlockSkillName == null) return;
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        var font = mc.font;

        // 알파값 (마지막 20틱에서 페이드아웃)
        int alpha = timer < 20 ? (int)(255f * timer / 20f) : 255;

        // ── 배경 어둡게 ──────────────────────────────────────────
        int bgAlpha = Math.min(alpha, 160);
        int bgColor = (bgAlpha << 24);
        g.fill(0, sh/2 - 40, sw, sh/2 + 40, bgColor);

        // ── 반짝임 효과: 가로 라인 ───────────────────────────────
        boolean flash = (timer % 8) < 4;
        int lineColor = flash
            ? ((alpha/2) << 24) | 0xFFDD00
            : ((alpha/3) << 24) | 0xFFAA00;
        g.fill(0, sh/2 - 18, sw, sh/2 - 16, lineColor);
        g.fill(0, sh/2 + 16, sw, sh/2 + 18, lineColor);

        // ── 메인 텍스트: "스킬 해금!" ────────────────────────────
        String line1 = "✦  스킬 해금!  ✦";
        int w1 = font.width(line1);
        int color1 = (alpha << 24) | 0xFFDD00;
        // 그림자
        g.drawString(font, line1, sw/2 - w1/2 + 1, sh/2 - 14 + 1, (alpha/3 << 24) | 0x000000, false);
        g.drawString(font, line1, sw/2 - w1/2, sh/2 - 14, color1, false);

        // ── 스킬 이름 ─────────────────────────────────────────────
        String line2 = "[ " + unlockSkillName + " ]";
        int w2 = font.width(line2);
        int color2 = (alpha << 24) | 0xFFFFFF;
        g.drawString(font, line2, sw/2 - w2/2 + 1, sh/2 + 1, (alpha/3 << 24) | 0x000000, false);
        g.drawString(font, line2, sw/2 - w2/2, sh/2, color2, false);

        // ── 서브 텍스트 ───────────────────────────────────────────
        String line3 = "F 키로 사용 가능";
        int w3 = font.width(line3);
        int color3 = (alpha << 24) | 0xAAAAAA;
        g.drawString(font, line3, sw/2 - w3/2, sh/2 + 16, color3, false);
    }
}
