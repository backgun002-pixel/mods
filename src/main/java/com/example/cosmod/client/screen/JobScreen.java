package com.example.cosmod.client.screen;

import com.example.cosmod.job.JobClass;
import com.example.cosmod.job.SelectJobPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class JobScreen extends Screen {

    private static final int W = 320, H = 240;
    private static final int JOB_COST = 200;
    private int left, top;
    private JobClass pendingJob = null;

    public JobScreen() { super(Component.literal("직업 선택")); }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top  = (this.height - H) / 2;
        buildMainButtons();
    }

    private void buildMainButtons() {
        this.clearWidgets();
        int btnW = 85, btnH = 24, gap = 8;

        // 생활직업 (3개 1행)
        JobClass[] life = JobClass.lifeJobs();
        int lx = left + (W - (life.length * (btnW + gap) - gap)) / 2;
        for (int i = 0; i < life.length; i++) {
            final JobClass job = life[i];
            this.addRenderableWidget(
                Button.builder(Component.literal("§a" + job.displayName), btn -> onJobClick(job))
                    .bounds(lx + i * (btnW + gap), top + 68, btnW, btnH).build());
        }

        // 전투직업 (4개 2행×2열)
        JobClass[] combat = JobClass.combatJobs();
        int cx2 = left + (W - (2 * (btnW + gap) - gap)) / 2;
        for (int i = 0; i < combat.length; i++) {
            final JobClass job = combat[i];
            int row = i / 2, col = i % 2;
            this.addRenderableWidget(
                Button.builder(Component.literal("§c" + job.displayName), btn -> onJobClick(job))
                    .bounds(cx2 + col * (btnW + gap), top + 135 + row * (btnH + 6), btnW, btnH).build());
        }

        this.addRenderableWidget(
            Button.builder(Component.literal("닫기"), btn -> this.onClose())
                .bounds(left + W - 58, top + H - 26, 52, 20).build());
    }

    private void buildConfirmButtons() {
        this.clearWidgets();
        int cx = left + W / 2, cy = top + H / 2;
        this.addRenderableWidget(
            Button.builder(Component.literal("§a✔ 변경하기 (-" + JOB_COST + "코인)"), btn -> confirmJob())
                .bounds(cx - 105, cy - 12, 100, 24).build());
        this.addRenderableWidget(
            Button.builder(Component.literal("§c✘ 취소"), btn -> { pendingJob = null; buildMainButtons(); })
                .bounds(cx + 5, cy - 12, 100, 24).build());
    }

    private void onJobClick(JobClass job) { pendingJob = job; buildConfirmButtons(); }

    private void confirmJob() {
        ClientPlayNetworking.send(new SelectJobPayload(pendingJob.name()));
        pendingJob = null; this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        int cx = left + W / 2;
        this.renderMenuBackground(g);
        g.fill(left, top, left + W, top + H, 0xDD334455);
        g.renderOutline(left, top, W, H, 0xFF4499FF);
        if (pendingJob == null) renderMainText(g, cx);
        super.render(g, mx, my, delta);
    }

    private void renderMainText(GuiGraphics g, int cx) {
        g.drawCenteredString(font, "§b§l✦ 직업 선택 ✦", cx, top + 10, 0xFFFFFF);
        g.drawCenteredString(font, "§f생활직업 1개 + 전투직업 1개를 선택할 수 있습니다", cx, top + 23, 0xDDDDDD);
        g.drawCenteredString(font, "§e선택/변경 비용: §6§l" + JOB_COST + " 코인", cx, top + 36, 0xFFFFFF);

        g.fill(left + 10, top + 50, left + W - 10, top + 51, 0xFF33AA33);
        g.drawCenteredString(font, "§a§l── 생활직업 ──", cx, top + 55, 0x55FF55);
        g.drawCenteredString(font, "§f광부 / 농사꾼 / 요리사", cx, top + 97, 0xCCCCCC);

        g.fill(left + 10, top + 115, left + W - 10, top + 116, 0xFFAA3333);
        g.drawCenteredString(font, "§c§l── 전투직업 ──", cx, top + 120, 0xFF5555);
        g.drawCenteredString(font, "§f전사 / 궁수 / 마법사 / 무도가", cx, top + 205, 0xCCCCCC);
    }

    @Override public boolean isPauseScreen() { return false; }
}
