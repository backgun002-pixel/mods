package com.example.cosmod.combat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.Random;
import java.util.function.Consumer;

public class GearItem extends Item {

    public enum GearType {
        WEAPON, HELMET, CHESTPLATE, LEGGINGS, BOOTS;

        public boolean isArmor() {
            return this != WEAPON;
        }

        public EquipmentSlot toSlot() {
            return switch (this) {
                case HELMET     -> EquipmentSlot.HEAD;
                case CHESTPLATE -> EquipmentSlot.CHEST;
                case LEGGINGS   -> EquipmentSlot.LEGS;
                case BOOTS      -> EquipmentSlot.FEET;
                default         -> null;
            };
        }
    }

    private final GearType gearType;
    private final SetBonus setBonus;

    // 방어구용 생성자 (세트 있음)
    public GearItem(Properties props, GearType type, SetBonus set) {
        super(addEquippable(props, type));
        this.gearType = type;
        this.setBonus = set;
    }

    // 기존 호환 생성자 (무기/세트 없음)
    public GearItem(Properties props, GearType type, String name) {
        super(type.toSlot() != null ? addEquippable(props, type) : props);
        this.gearType = type;
        this.setBonus = null;
    }

    // 착용 슬롯 설정 헬퍼 (1.21.11: Properties.equippable(slot))
    private static Properties addEquippable(Properties props, GearType type) {
        EquipmentSlot slot = type.toSlot();
        if (slot == null) return props;
        return props.equippable(slot);
    }

    public GearType getGearType() { return gearType; }
    public SetBonus getSetBonus() { return setBonus; }

    // ── NBT 헬퍼 ─────────────────────────────────────────────────
    public static CompoundTag getGearTag(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd != null ? cd.copyTag() : new CompoundTag();
    }
    public static void setGearTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // ── 랜덤 메인옵션 2개 부여 ───────────────────────────────────
    public static ItemStack createWithOptions(GearItem item) {
        ItemStack stack = new ItemStack((Item) item);
        Random rng = new Random();
        GearOption[] pool = item.gearType.isArmor()
            ? GearOption.mainArmor() : GearOption.mainWeapon();
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isGear", true);
        tag.putBoolean("enhanced", false);
        int[] picked = pickTwo(pool.length, rng);
        for (int i = 0; i < 2; i++) {
            GearOption opt = pool[picked[i]];
            int val = opt.minVal + rng.nextInt(opt.maxVal - opt.minVal + 1);
            tag.putString("main_opt_" + i, opt.name());
            tag.putInt("main_val_" + i, val);
        }
        setGearTag(stack, tag);
        return stack;
    }

    // ── 강화 ─────────────────────────────────────────────────────
    public static ItemStack enhance(ItemStack stack) {
        if (!(stack.getItem() instanceof GearItem)) return stack;
        CompoundTag tag = getGearTag(stack);
        if (tag.contains("enhanced") && tag.getBoolean("enhanced").orElse(false)) return stack;
        Random rng = new Random();
        GearOption[] pool = GearOption.sub();
        int[] picked = pickTwo(pool.length, rng);
        for (int i = 0; i < 2; i++) {
            GearOption opt = pool[picked[i]];
            int val = opt.minVal + rng.nextInt(opt.maxVal - opt.minVal + 1);
            tag.putString("sub_opt_" + i, opt.name());
            tag.putInt("sub_val_" + i, val);
        }
        tag.putBoolean("enhanced", true);
        setGearTag(stack, tag);
        return stack;
    }

    // ── 툴팁 ─────────────────────────────────────────────────────
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                TooltipDisplay display,
                                Consumer<Component> consumer,
                                TooltipFlag flag) {
        CompoundTag tag = getGearTag(stack);
        if (setBonus != null)
            consumer.accept(Component.literal("§6[" + setBonus.displayName + " 세트]"));

        if (!tag.contains("isGear")) return;

        consumer.accept(Component.literal("§7━━━━━━━━━━━━━━"));
        consumer.accept(Component.literal("§e[메인 옵션]"));
        for (int i = 0; i < 2; i++) {
            if (!tag.contains("main_opt_" + i)) continue;
            try {
                GearOption opt = GearOption.valueOf(tag.getString("main_opt_" + i).orElse(""));
                int val = tag.getInt("main_val_" + i).orElse(0);
                consumer.accept(Component.literal("§f  " + opt.displayName + " +" + val + opt.unit));
            } catch (Exception ignored) {}
        }

        if (tag.contains("enhanced") && tag.getBoolean("enhanced").orElse(false)) {
            consumer.accept(Component.literal("§b[부 옵션] §7(강화됨)"));
            for (int i = 0; i < 2; i++) {
                if (!tag.contains("sub_opt_" + i)) continue;
                try {
                    GearOption opt = GearOption.valueOf(tag.getString("sub_opt_" + i).orElse(""));
                    int val = tag.getInt("sub_val_" + i).orElse(0);
                    consumer.accept(Component.literal("§b  " + opt.displayName + " +" + val + opt.unit));
                } catch (Exception ignored) {}
            }
        } else {
            consumer.accept(Component.literal("§8[부 옵션] §7강화 시 해금"));
        }

        if (setBonus != null) {
            consumer.accept(Component.literal("§7━━━━━━━━━━━━━━"));
            consumer.accept(Component.literal("§6◆ 세트 효과"));
            consumer.accept(Component.literal("§72세트: " + formatEffect(setBonus.bonus2)));
            consumer.accept(Component.literal("§64세트: " + formatEffect(setBonus.bonus4)));
        }
        consumer.accept(Component.literal("§7━━━━━━━━━━━━━━"));
    }

    private String formatEffect(SetBonus.Effect e) {
        StringBuilder sb = new StringBuilder();
        if (e.atkBonus()      > 0) sb.append("공격+").append(e.atkBonus()).append(" ");
        if (e.defBonus()      > 0) sb.append("방어+").append(e.defBonus()).append(" ");
        if (e.speedPercent()  > 0) sb.append("이속+").append(e.speedPercent()).append("% ");
        if (e.strengthLevel() > 0) sb.append("힘 Lv").append(e.strengthLevel());
        return sb.toString().trim();
    }

    private static int[] pickTwo(int poolSize, Random rng) {
        if (poolSize < 2) return new int[]{0, 0};
        int a = rng.nextInt(poolSize);
        int b; do { b = rng.nextInt(poolSize); } while (b == a);
        return new int[]{a, b};
    }
}
