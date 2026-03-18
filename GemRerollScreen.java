package com.example.cosmod.client.screen;

import com.example.cosmod.combat.GemItem;
import com.example.cosmod.combat.GemRerollRequestPayload;
import com.example.cosmod.combat.GemTier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class GemRerollScreen extends Screen {

    private ItemStack gem;
    private final int slotIdx;

    private static final int W = 200;
    private static final int H = 200;

    private static final int C_BG    = 0xE0101018;
    private static final int C_BDR   = 0xFF8855CC;
    private static final int C_TEXT  = 0xFFFFDD88;
    private static final int C_GREEN = 0xFF55FF55;
    private static final int C_GRAY  = 0xFF888888;

    public GemRerollScreen(ItemStack gem, int slotIdx) {
        super(Component.literal("보석 감정"));
        this.gem     = gem.copy();
        this.slotIdx = slotIdx;
    }

    public void updateGem(ItemStack newGem) {
        this.gem = newGem.copy();
        rebuildWidgets();
    }

    @Override
    protected void init() {
        super.init();
        int bx = (width - W) / 2;
        int by = (height - H) / 2;

        boolean maxed = GemItem.isMaxRerolled(gem);

        // 감정하기 버튼
        addRenderableWidget(Button.builder(
            Component.literal(maxed ? "§c재설정 불가" : "§a감정하기"),
            btn -> {
                if (!maxed) {
                    ClientPlayNetworking.send(new GemRerollRequestPayload(slotIdx));
                }
            })
            .bounds(bx + W / 2 - 50, by + H - 35, 100, 20)
            .build()
        );

        // 닫기 버튼
        addRenderableWidget(Button.builder(
            Component.literal("§f닫기"),
            btn -> onClose())
            .bounds(bx + W - 28, by + 4, 24, 14)
            .build()
        );
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        int bx = (width - W) / 2;
        int by = (height - H) / 2;

        // 배경
        g.fill(bx, by, bx + W, by + H, C_BG);
        g.fill(bx, by, bx + W, by + 1, C_BDR);
        g.fill(bx, by + H - 1, bx + W, by + H, C_BDR);
        g.fill(bx, by, bx + 1, by + H, C_BDR);
        g.fill(bx + W - 1, by, bx + W, by + H, C_BDR);

        // 제목
        if (gem.getItem() instanceof GemItem gi) {
            String title = gi.getTier().color + "● " + gi.getTier().displayName + " 아이템 보석";
            g.drawCenteredString(font, title, bx + W / 2, by + 8, 0xFFFFFFFF);
        }

        int ly = by + 28;
        CompoundTag tag = GemItem.getGemTag(gem);
        boolean identified = tag.contains("identified") && tag.getBoolean("identified").orElse(false);

        if (!identified) {
            g.drawCenteredString(font, "§8미감정 보석", bx + W / 2, ly, 0xFFFFFFFF);
            ly += 14;
            g.drawCenteredString(font, "§7아래 버튼으로 감정하세요.", bx + W / 2, ly, 0xFFFFFFFF);
        } else {
            g.drawString(font, "§e[옵션 정보]", bx + 12, ly, C_TEXT, false);
            ly += 14;

            // 아이콘 색상 배열
            int[] iconColors = {0xFFFF6666, 0xFF6688FF, 0xFFFFFF44, 0xFF44FF44,
                                 0xFFFF44FF, 0xFFFF4444, 0xFF88AAFF};
            String[] icons = {"⚔", "✦", "✚", "⚡", "◎", "❤", "🛡"};

            int lines = ((GemItem) gem.getItem()).getTier().optionLines;
            for (int i = 0; i < lines; i++) {
                if (!tag.contains("gem_type_" + i)) continue;
                int optType = tag.getInt("gem_type_" + i).orElse(0);
                int val     = tag.getInt("gem_val_"  + i).orElse(0);
                int ic      = iconColors[optType];
                g.drawString(font, GemTier.OPT_NAMES[optType], bx + 28, ly, C_TEXT, false);
                g.drawString(font, "+" + val + GemTier.OPT_UNITS[optType], bx + W - 45, ly, C_GREEN, false);
                ly += 14;
            }
        }

        // 재설정 횟수
        int rerolls = GemItem.getRerollCount(gem);
        int maxR = gem.getItem() instanceof GemItem gi2 ? gi2.getTier().maxRerolls : 3;
        ly = by + H - 50;
        g.fill(bx + 8, ly, bx + W - 8, ly + 1, 0xFF444444);
        ly += 4;
        String rerollText = "재설정: " + rerolls + " / " + maxR;
        int rc = rerolls >= maxR ? 0xFFFF5555 : 0xFFAAAAAA;
        g.drawCenteredString(font, rerollText, bx + W / 2, ly, rc);

        super.render(g, mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
