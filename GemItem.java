package com.example.cosmod.combat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Random;
import java.util.function.Consumer;

public class GemItem extends Item {

    private final GemTier tier;

    public GemItem(Properties props, GemTier tier) {
        super(props);
        this.tier = tier;
    }

    public GemTier getTier() { return tier; }

    // ── NBT 헬퍼 ─────────────────────────────────────────────────
    public static CompoundTag getGemTag(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd != null ? cd.copyTag() : new CompoundTag();
    }
    public static void setGemTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** 재설정 횟수 조회 */
    public static int getRerollCount(ItemStack stack) {
        CompoundTag tag = getGemTag(stack);
        return tag.contains("rerolls") ? tag.getInt("rerolls").orElse(0) : 0;
    }

    /** 최대 재설정 도달 여부 */
    public static boolean isMaxRerolled(ItemStack stack) {
        if (!(stack.getItem() instanceof GemItem gi)) return false;
        return getRerollCount(stack) >= gi.tier.maxRerolls;
    }

    /** 옵션 감정 (랜덤 재설정) */
    public static void reroll(ItemStack stack) {
        if (!(stack.getItem() instanceof GemItem gi)) return;
        CompoundTag tag = getGemTag(stack);
        int rerolls = tag.contains("rerolls") ? tag.getInt("rerolls").orElse(0) : 0;
        if (rerolls >= gi.tier.maxRerolls) return;

        Random rng = new Random();
        int lines = gi.tier.optionLines;
        // 중복 없이 옵션 타입 선택
        int[] picked = pickDistinct(GemTier.OPT_NAMES.length, lines, rng);
        for (int i = 0; i < lines; i++) {
            int optType = picked[i];
            int[] range = gi.tier.getRange(optType);
            int val = range[0] + rng.nextInt(range[1] - range[0] + 1);
            tag.putInt("gem_type_" + i, optType);
            tag.putInt("gem_val_" + i,  val);
        }
        tag.putInt("rerolls", rerolls + 1);
        tag.putBoolean("identified", true);
        setGemTag(stack, tag);
    }

    /** 보석이 감정됐는지 확인 */
    public static boolean isIdentified(ItemStack stack) {
        CompoundTag tag = getGemTag(stack);
        return tag.contains("identified") && tag.getBoolean("identified").orElse(false);
    }

    // ── 우클릭 → 서버에서 감정 UI 열기 패킷 전송 ────────────────
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        int slotIdx = findSlot(player, stack);
        if (slotIdx < 0) return InteractionResult.PASS;
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp,
                new OpenGemRerollPayload(slotIdx));
        }
        return InteractionResult.SUCCESS;
    }

    private static int findSlot(Player player, ItemStack stack) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i) == stack) return i;
        }
        return -1;
    }

    private static int[] pickDistinct(int pool, int count, Random rng) {
        int[] result = new int[count];
        boolean[] used = new boolean[pool];
        for (int i = 0; i < count; i++) {
            int r;
            do { r = rng.nextInt(pool); } while (used[r]);
            result[i] = r;
            used[r] = true;
        }
        return result;
    }

    // ── 툴팁 ─────────────────────────────────────────────────────
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                TooltipDisplay display,
                                Consumer<Component> consumer,
                                TooltipFlag flag) {
        CompoundTag tag = getGemTag(stack);
        int rerolls = tag.contains("rerolls") ? tag.getInt("rerolls").orElse(0) : 0;
        boolean identified = tag.contains("identified") && tag.getBoolean("identified").orElse(false);

        consumer.accept(Component.literal("§7━━━━━━━━━━━━━━"));
        if (identified) {
            consumer.accept(Component.literal("§e[옵션]"));
            int lines = ((GemItem) stack.getItem()).tier.optionLines;
            for (int i = 0; i < lines; i++) {
                if (!tag.contains("gem_type_" + i)) continue;
                int optType = tag.getInt("gem_type_" + i).orElse(0);
                int val     = tag.getInt("gem_val_"  + i).orElse(0);
                consumer.accept(Component.literal("§f  " + GemTier.OPT_NAMES[optType]
                    + " +" + val + GemTier.OPT_UNITS[optType]));
            }
        } else {
            consumer.accept(Component.literal("§8미감정 보석"));
            consumer.accept(Component.literal("§7우클릭으로 감정하세요."));
            GemTier t = ((GemItem) stack.getItem()).tier;
            consumer.accept(Component.literal("§7옵션 " + t.optionLines + "줄"));
            for (int i = 0; i < GemTier.OPT_NAMES.length; i++) {
                int[] range = t.getRange(i);
                consumer.accept(Component.literal("  §8" + GemTier.OPT_NAMES[i]
                    + " +" + range[0] + " ~ " + range[1] + GemTier.OPT_UNITS[i]));
            }
        }
        consumer.accept(Component.literal("§7━━━━━━━━━━━━━━"));
        consumer.accept(Component.literal("§7재설정: §e" + rerolls + "§7/§e" + tier.maxRerolls));
    }
}
