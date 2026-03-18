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

        public boolean isArmor() { return this != WEAPON; }

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

    public GearItem(Properties props, GearType type, SetBonus set) {
        super(addEquippable(props, type));
        this.gearType = type;
        this.setBonus = set;
    }

    public GearItem(Properties props, GearType type, String name) {
        super(type.toSlot() != null ? addEquippable(props, type) : props);
        this.gearType = type;
        this.setBonus = null;
    }

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

    // ── 아이템 생성 ───────────────────────────────────────────────
    public static ItemStack createWithOptions(GearItem item) {
        ItemStack stack = new ItemStack((Item) item);
        Random rng = new Random();
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isGear", true);
        tag.putInt("enhance_level", 0);

        if (item.gearType.isArmor()) {
            // 방어구: 메인옵션 2개 부여
            GearOption[] pool = GearOption.mainArmor();
            int[] picked = pickTwo(pool.length, rng);
            for (int i = 0; i < 2; i++) {
                GearOption opt = pool[picked[i]];
                int val = opt.minVal + rng.nextInt(opt.maxVal - opt.minVal + 1);
                tag.putString("main_opt_" + i, opt.name());
                tag.putInt("main_val_" + i, val);
            }
        } else {
            // 무기: 등급 랜덤 부여
            WeaponGrade grade = WeaponGrade.roll(rng);
            tag.putString("grade", grade.name());
            // base_name을 NBT에 저장 (이름 중복 방지의 핵심)
            String baseName = stack.getHoverName().getString();
            tag.putString("base_name", baseName);
            applyGradeName(stack, grade, 0);
        }

        setGearTag(stack, tag);
        return stack;
    }

    /** 무기 이름에 등급 적용 - NBT의 base_name 사용으로 중복 방지 */
    public static void applyGradeName(ItemStack stack, WeaponGrade grade, int enhLevel) {
        // base_name NBT에서 직접 꺼냄 (regex 파싱 없음)
        CompoundTag t = getGearTag(stack);
        String baseName;
        if (t.contains("base_name")) {
            baseName = t.getString("base_name").orElse("");
        } else {
            // 구버전 호환: description에서 추출
            // base_name 없는 구버전 아이템: 현재 표시 이름 그대로 사용
            baseName = stack.getItem().getName(stack).getString();
        }
        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(grade.formatName(baseName, enhLevel)));
    }

    // 등급 조회
    public static WeaponGrade getGrade(ItemStack stack) {
        CompoundTag tag = getGearTag(stack);
        if (!tag.contains("grade")) return null;
        try {
            return WeaponGrade.valueOf(tag.getString("grade").orElse("NORMAL"));
        } catch (Exception e) {
            return null;
        }
    }

    // 강화 단계 조회
    public static int getEnhanceLevel(ItemStack stack) {
        CompoundTag tag = getGearTag(stack);
        return tag.contains("enhance_level") ? tag.getInt("enhance_level").orElse(0) : 0;
    }

    // ── 강화 (악놀2 방식) ─────────────────────────────────────────
    public enum EnhanceResult { SUCCESS, FAIL }

    /**
     * @param stoneType 0=일반강화석, 1=중급강화석, 2=상급강화석
     */
    public static EnhanceResult enhance(ItemStack stack, int stoneType) {
        if (!(stack.getItem() instanceof GearItem)) return EnhanceResult.FAIL;
        CompoundTag tag = getGearTag(stack);
        int level = tag.contains("enhance_level") ? tag.getInt("enhance_level").orElse(0) : 0;

        Random rng = new Random();
        int roll = rng.nextInt(100);

        // 성공 확률: 일반=40%, 중급=60%, 상급=80%
        int successRate = switch (stoneType) {
            case 1  -> 60;
            case 2  -> 80;
            default -> 40;
        };
        EnhanceResult result;
        if (roll < successRate) {
            // 성공
            result = EnhanceResult.SUCCESS;
            level = Math.min(level + 1, 20);
        } else {
            // 실패 - 5강 미만이면 하락 없음, 5강 이상이면 -1
            // 파괴 없음
            result = EnhanceResult.FAIL;
            if (level >= 5) {
                level = Math.max(0, level - 1);
            }
        }

        tag.putInt("enhance_level", level);
        setGearTag(stack, tag);

        // 이름 업데이트 - NBT base_name 사용 (regex 없음)
        WeaponGrade grade = getGrade(stack);
        if (grade != null) {
            applyGradeName(stack, grade, level);
        }

        return result;
    }

    // ── 보석 감정 ─────────────────────────────────────────────────
    public static ItemStack applyGem(ItemStack stack) {
        if (!(stack.getItem() instanceof GearItem)) return stack;
        CompoundTag tag = getGearTag(stack);
        Random rng = new Random();
        GearOption[] pool = GearOption.gem();
        int[] picked = pickTwo(pool.length, rng);
        for (int i = 0; i < 2; i++) {
            GearOption opt = pool[picked[i]];
            int val = opt.minVal + rng.nextInt(opt.maxVal - opt.minVal + 1);
            tag.putString("gem_opt_" + i, opt.name());
            tag.putInt("gem_val_" + i, val);
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

        int enhLevel = tag.contains("enhance_level") ? tag.getInt("enhance_level").orElse(0) : 0;
        consumer.accept(Component.literal("§7━━━━━━━━━━━━━━"));

        if (!gearType.isArmor() && tag.contains("grade")) {
            // 무기: 등급 효과 표시
            try {
                WeaponGrade g = WeaponGrade.valueOf(tag.getString("grade").orElse("NORMAL"));
                consumer.accept(Component.literal(g.color + "■ " + g.displayName + " 등급"));
                if (g.atkPercent > 0)
                    consumer.accept(Component.literal("§f  공격력 +" + g.atkPercent + "%"));
                if (g.atkFlat > 0)
                    consumer.accept(Component.literal("§f  공격력 +" + g.atkFlat));
            } catch (Exception ignored) {}
        } else if (gearType.isArmor()) {
            // 방어구: 메인옵션 표시
            consumer.accept(Component.literal("§e[메인 옵션]"));
            for (int i = 0; i < 2; i++) {
                if (!tag.contains("main_opt_" + i)) continue;
                try {
                    GearOption opt = GearOption.valueOf(tag.getString("main_opt_" + i).orElse(""));
                    int val = tag.getInt("main_val_" + i).orElse(0);
                    consumer.accept(Component.literal("§f  " + opt.displayName + " +" + val + opt.unit));
                } catch (Exception ignored) {}
            }
        }

        // 강화 단계
        consumer.accept(Component.literal("§e강화: §f+" + enhLevel));

        // 보석 옵션
        if (tag.contains("enhanced") && tag.getBoolean("enhanced").orElse(false)) {
            consumer.accept(Component.literal("§b[보석 옵션]"));
            int lines = tag.contains("gem_lines") ? tag.getInt("gem_lines").orElse(2) : 2;
            for (int i = 0; i < lines; i++) {
                if (!tag.contains("gem_opt_type_" + i)) continue;
                int optType = tag.getInt("gem_opt_type_" + i).orElse(0);
                int val     = tag.getInt("gem_opt_val_"  + i).orElse(0);
                String name = (optType >= 0 && optType < GemTier.OPT_NAMES.length)
                    ? GemTier.OPT_NAMES[optType] : "?";
                String unit = (optType >= 0 && optType < GemTier.OPT_UNITS.length)
                    ? GemTier.OPT_UNITS[optType] : "";
                consumer.accept(Component.literal("§b  " + name + " +" + val + unit));
            }
        } else {
            consumer.accept(Component.literal("§8[보석 슬롯] §7강화 테이블에서 보석 장착"));
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
        if (e.atkBonus()     > 0) sb.append("공격+").append(e.atkBonus()).append(" ");
        if (e.defBonus()     > 0) sb.append("방어+").append(e.defBonus()).append(" ");
        if (e.speedPercent() > 0) sb.append("이속+").append(e.speedPercent()).append("% ");
        if (e.strengthLevel()> 0) sb.append("힘").append(e.strengthLevel()).append(" ");
        return sb.length() > 0 ? sb.toString().trim() : "없음";
    }


    /** +수치별 강화 기본 성공 확률 */
    public static int getSuccessRateForLevel(int level) {
        return switch (level) {
            case 0  -> 90;
            case 1  -> 80;
            case 2  -> 70;
            case 3  -> 60;
            case 4  -> 50;
            case 5  -> 40;
            case 6  -> 35;
            case 7  -> 28;
            case 8  -> 22;
            case 9  -> 18;
            case 10 -> 15;
            case 11 -> 12;
            case 12 -> 10;
            default ->  8;
        };
    }

    private static int[] pickTwo(int size, Random rng) {
        if (size <= 2) return new int[]{0, size > 1 ? 1 : 0};
        int a = rng.nextInt(size);
        int b;
        do { b = rng.nextInt(size); } while (b == a);
        return new int[]{a, b};
    }
}
