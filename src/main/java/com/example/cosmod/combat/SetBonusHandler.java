package com.example.cosmod.combat;

import com.example.cosmod.job.JobClass;
import com.example.cosmod.stat.StatSyncHandler;
import com.example.cosmod.job.PlayerJobData;
import com.example.cosmod.job.PlayerJobManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class SetBonusHandler {

    // ── Identifier 헬퍼 ───────────────────────────────────────────
    private static Identifier id(String key) {
        return Identifier.fromNamespaceAndPath("cosmod", key);
    }

    // 세트 보너스용
    private static Identifier atkId(String key)  { return id("set_atk_"  + key.toLowerCase()); }
    private static Identifier defId(String key)  { return id("set_def_"  + key.toLowerCase()); }
    private static Identifier spdId(String key)  { return id("set_spd_"  + key.toLowerCase()); }

    // 직업 보너스용
    private static final Identifier JOB_ATK  = id("job_atk");
    private static final Identifier JOB_DEF  = id("job_def");
    private static final Identifier JOB_SPD  = id("job_spd");

    // 기어 옵션용
    private static final Identifier GEAR_ATK        = id("gear_atk");
    private static final Identifier GEAR_ATK_PCT    = id("gear_atk_pct");
    private static final Identifier GEAR_DEF        = id("gear_def");
    private static final Identifier GEAR_HP         = id("gear_hp");
    private static final Identifier GEAR_SPD        = id("gear_spd");
    private static final Identifier GEAR_CRIT       = id("gear_crit");   // 치명타는 별도 Map으로 관리
    private static final Identifier GEAR_JUMP       = id("gear_jump");
    private static final Identifier GEAR_CRITDMG    = id("gear_critdmg");

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 5 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                applySetBonuses(player);
                applyJobBonuses(player);
                applyGearOptions(player);
                StatSyncHandler.sync(player);  // 스탯 변경 후 즉각 동기화
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // 1. 세트 보너스 (기존 코드 유지)
    // ══════════════════════════════════════════════════════════════
    private static void applySetBonuses(ServerPlayer player) {
        for (SetBonus set : SetBonus.values()) {
            SetBonus.Effect effect = SetBonus.getActiveBonus(player, set);
            String key = set.name();

            AttributeInstance atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) {
                atk.removeModifier(atkId(key));
                if (effect != null && effect.atkBonus() > 0)
                    atk.addTransientModifier(new AttributeModifier(atkId(key), effect.atkBonus(), AttributeModifier.Operation.ADD_VALUE));
            }
            AttributeInstance def = player.getAttribute(Attributes.ARMOR);
            if (def != null) {
                def.removeModifier(defId(key));
                if (effect != null && effect.defBonus() > 0)
                    def.addTransientModifier(new AttributeModifier(defId(key), effect.defBonus(), AttributeModifier.Operation.ADD_VALUE));
            }
            AttributeInstance spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spd != null) {
                spd.removeModifier(spdId(key));
                if (effect != null && effect.speedPercent() > 0)
                    spd.addTransientModifier(new AttributeModifier(spdId(key), effect.speedPercent() / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            if (effect != null && effect.strengthLevel() > 0) {
                if (!player.hasEffect(MobEffects.STRENGTH))
                    player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, effect.strengthLevel() - 1, false, false, true));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 2. 직업 보너스
    // ══════════════════════════════════════════════════════════════
    private static void applyJobBonuses(ServerPlayer player) {
        PlayerJobData data = PlayerJobManager.get(player);
        JobClass combat = data.getCombatJob();

        // 항상 기존 수정자 제거 후 재적용
        removeModifier(player, Attributes.ATTACK_DAMAGE,    JOB_ATK);
        removeModifier(player, Attributes.ARMOR,            JOB_DEF);
        removeModifier(player, Attributes.MOVEMENT_SPEED,   JOB_SPD);

        if (combat == null) return;
        int lv = data.getLevel(combat);

        switch (combat) {
            case WARRIOR -> {
                // Lv5: 근접 공격력 +5%
                if (lv >= 5)
                    addMultiplier(player, Attributes.ATTACK_DAMAGE, JOB_ATK, 0.05);
                // Lv10: 방어력 +3 (피격 감소)
                if (lv >= 10)
                    addFlat(player, Attributes.ARMOR, JOB_DEF, 3.0);
            }
            case ARCHER -> {
                // Lv5: 화살 데미지는 getArrowDamageMultiplier()로 별도 처리 (코드에 이미 존재)
                // Lv10: 이동속도 +5%
                if (lv >= 10)
                    addMultiplier(player, Attributes.MOVEMENT_SPEED, JOB_SPD, 0.05);
            }
            case MAGE -> {
                // Lv5: 마법 데미지는 getInfinityMultiplier()로 별도 처리
                // Lv10: 최대 체력 +4
                if (lv >= 10)
                    addFlat(player, Attributes.MAX_HEALTH, JOB_DEF, 4.0);
            }
            case MONK -> {
                // Lv5: 근접 공격력 +5%
                if (lv >= 5)
                    addMultiplier(player, Attributes.ATTACK_DAMAGE, JOB_ATK, 0.05);
                // Lv10: 이동속도 +5%
                if (lv >= 10)
                    addMultiplier(player, Attributes.MOVEMENT_SPEED, JOB_SPD, 0.05);
            }
            default -> {}
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 3. 기어 옵션
    // ══════════════════════════════════════════════════════════════
    private static void applyGearOptions(ServerPlayer player) {
        // 기존 기어 수정자 모두 제거
        removeModifier(player, Attributes.ATTACK_DAMAGE,  GEAR_ATK);
        removeModifier(player, Attributes.ATTACK_DAMAGE,  GEAR_ATK_PCT);
        removeModifier(player, Attributes.ARMOR,          GEAR_DEF);
        removeModifier(player, Attributes.MAX_HEALTH,     GEAR_HP);
        removeModifier(player, Attributes.MOVEMENT_SPEED, GEAR_SPD);
        removeModifier(player, Attributes.JUMP_STRENGTH,  GEAR_JUMP);

        // 기어 옵션 합산
        double totalAtkFlat = 0, totalAtkPct = 0, totalDef = 0;
        double totalHp = 0, totalSpd = 0, totalJump = 0;

        // 메인핸드 (무기)
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof GearItem) {
            GearStats s = readGearStats(mainHand);
            totalAtkFlat += s.atkFlat;
            totalAtkPct  += s.atkPct;
            totalDef     += s.def;
            totalHp      += s.hp;
            totalSpd     += s.spd;
            totalJump    += s.jump;
        }

        // 방어구 슬롯 (HEAD, CHEST, LEGS, FEET)
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof GearItem) {
                GearStats s = readGearStats(armor);
                totalAtkFlat += s.atkFlat;
                totalAtkPct  += s.atkPct;
                totalDef     += s.def;
                totalHp      += s.hp;
                totalSpd     += s.spd;
                totalJump    += s.jump;
            }
        }

        // 합산된 스탯 적용
        if (totalAtkFlat != 0)
            addFlat(player, Attributes.ATTACK_DAMAGE, GEAR_ATK, totalAtkFlat);
        if (totalAtkPct != 0)
            addMultiplier(player, Attributes.ATTACK_DAMAGE, GEAR_ATK_PCT, totalAtkPct / 100.0);
        if (totalDef != 0)
            addFlat(player, Attributes.ARMOR, GEAR_DEF, totalDef);
        if (totalHp != 0)
            addFlat(player, Attributes.MAX_HEALTH, GEAR_HP, totalHp);
        if (totalSpd != 0)
            addMultiplier(player, Attributes.MOVEMENT_SPEED, GEAR_SPD, totalSpd / 100.0);
        if (totalJump != 0)
            addFlat(player, Attributes.JUMP_STRENGTH, GEAR_JUMP, totalJump * 0.05);
    }

    /** GearItem의 NBT에서 옵션 수치를 읽어 합산 */
    private static GearStats readGearStats(ItemStack stack) {
        GearStats stats = new GearStats();
        CompoundTag tag = GearItem.getGearTag(stack);
        if (!tag.contains("isGear")) return stats;

        // 메인옵션 2개
        for (int i = 0; i < 2; i++) {
            String optName = tag.getString("main_opt_" + i).orElse("");
            int    optVal  = tag.getInt("main_val_"    + i).orElse(0);
            if (optName.isEmpty()) continue;
            try {
                GearOption opt = GearOption.valueOf(optName);
                applyToStats(stats, opt, optVal);
            } catch (IllegalArgumentException ignored) {}
        }

        // 부옵션 2개 (강화 완료 시)
        boolean enhanced = tag.getBoolean("enhanced").orElse(false);
        if (enhanced) {
            for (int i = 0; i < 2; i++) {
                String optName = tag.getString("sub_opt_" + i).orElse("");
                int    optVal  = tag.getInt("sub_val_"   + i).orElse(0);
                if (optName.isEmpty()) continue;
                try {
                    GearOption opt = GearOption.valueOf(optName);
                    applyToStats(stats, opt, optVal);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return stats;
    }

    private static void applyToStats(GearStats stats, GearOption opt, int val) {
        switch (opt) {
            case ATK_PERCENT    -> stats.atkPct  += val;
            case BONUS_ATK      -> stats.atkFlat += val;
            case DEFENSE        -> stats.def     += val;
            case BONUS_DEF      -> stats.def     += val;
            case MAX_HP         -> stats.hp      += val;
            case BONUS_HP       -> stats.hp      += val;
            case MOVE_SPEED     -> stats.spd     += val;
            case JUMP_BOOST     -> stats.jump    += val;
            // ATK_SPEED, CRIT_CHANCE, CRIT_DAMAGE, CRIT_CHANCE_DEF → 향후 확장
            default -> {}
        }
    }

    /** 기어 스탯 합산용 내부 클래스 */
    private static class GearStats {
        double atkFlat = 0, atkPct = 0, def = 0, hp = 0, spd = 0, jump = 0;
    }

    // ══════════════════════════════════════════════════════════════
    // 공통 헬퍼
    // ══════════════════════════════════════════════════════════════
    private static void removeModifier(ServerPlayer player,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
            Identifier id) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null) inst.removeModifier(id);
    }

    private static void addFlat(ServerPlayer player,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
            Identifier id, double value) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null)
            inst.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void addMultiplier(ServerPlayer player,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
            Identifier id, double value) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null)
            inst.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
}
