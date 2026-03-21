package com.example.cosmod.client.screen;

import com.example.cosmod.combat.GemItem;
import com.example.cosmod.combat.GemTier;
import com.example.cosmod.combat.GemRerollRequestPayload;
import com.example.cosmod.combat.GearOption;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class GemRerollScreen extends Screen {

    private static final int W = 220, H = 180;
    private int left, top;
    private final int slotIdx;
    private ItemStack gem;
    private String resultMsg = null;

    public GemRerollScreen(ItemStack gem, int slotIdx) {
        super(Component.literal("보석 감정"));
        this.gem = gem;
        this.slotIdx = slotIdx;
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top  = (this.height - H) / 2;
        buildButtons();
    }

    private void buildButtons() {
        this.clearWidgets();

        CompoundTag tag = GemItem.getGemTag(gem);
        int rerolls = tag.getInt("rerolls").orElse(0);
        GemItem gi = (GemItem) gem.getItem();
        int maxRerolls = gi.getTier().maxRerolls;
        boolean canReroll = rerolls < maxRerolls;

        this.addRenderableWidget(
            Button.builder(Component.literal(canReroll ? "§a✦ 감정하기 (재설정)" : "§8재설정 불가"),
                btn -> {
                    if (canReroll) {
                        ClientPlayNetworking.send(new GemRerollRequestPayload(slotIdx));
                        resultMsg = "§b감정 요청 전송...";
                    }
                })
            .bounds(left + W/2 - 70, top + H - 36, 140, 22)
            .build());

        this.addRenderableWidget(
            Button.builder(Component.literal("닫기"), btn -> this.onClose())
            .bounds(left + W - 50, top + 4, 44, 18)
            .build());
    }

    public void updateGem(ItemStack newGem) {
        this.gem = newGem;
        resultMsg = "§a✔ 감정 완료!";
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        this.renderMenuBackground(g);
        g.fill(left, top, left+W, top+H, 0xDD1A1A2E);
        g.renderOutline(left, top, W, H, 0xFF4466AA);

        int cx = left + W/2;
        g.drawCenteredString(font, "§b§l✦ 보석 감정 ✦", cx, top+8, 0xFFFFFF);

        // 아이템 아이콘
        g.renderItem(gem, left+W/2-8, top+22);

        // 등급 표시
        GemItem gi = (GemItem) gem.getItem();
        GemTier tier = gi.getTier();
        g.drawCenteredString(font, "§e" + tier.displayName + " 보석", cx, top+42, 0xFFFFFF);

        // 옵션 표시
        CompoundTag tag = GemItem.getGemTag(gem);
        int lines = tag.getInt("lines").orElse(0);
        boolean identified = tag.getBoolean("identified").orElse(false);

        if (!identified) {
            g.drawCenteredString(font, "§7미감정 상태", cx, top+60, 0xAAAAAA);
        } else {
            for (int i = 0; i < lines; i++) {
                int optType = tag.getInt("gem_type_" + i).orElse(0);
                int val     = tag.getInt("gem_val_"  + i).orElse(0);
                String optName = GearOption.GEM_OPT_NAMES[optType % GearOption.GEM_OPT_NAMES.length];
                g.drawCenteredString(font, "§f" + optName + " +§e" + val, cx, top+58 + i*12, 0xFFFFFF);
            }
        }

        // 재설정 횟수
        int rerolls = tag.getInt("rerolls").orElse(0);
        g.drawCenteredString(font, "§7재설정: " + rerolls + "/" + tier.maxRerolls,
            cx, top + H - 52, 0xAAAAAA);

        // 결과 메시지
        if (resultMsg != null) {
            g.drawCenteredString(font, resultMsg, cx, top + H - 62, 0xFFFFFF);
        }

        super.render(g, mx, my, delta);
    }

    @Override public boolean isPauseScreen() { return false; }
}
