package com.example.cosmod.combat;

public enum GearOption {
    // ── 메인 옵션 (무기용) ────────────────────────────────
    ATK_PERCENT("공격력", "%", true, false, 5, 25),
    ATK_SPEED("공격속도", "%", true, false, 3, 15),
    CRIT_CHANCE("치명타 확률", "%", true, false, 3, 15),

    // ── 메인 옵션 (방어구용) ─────────────────────────────
    DEFENSE("방어력", "", false, true, 2, 10),
    MAX_HP("생명력", "", false, true, 2, 20),
    CRIT_CHANCE_DEF("치명타 확률", "%", false, true, 2, 10),

    // ── 부옵션 (공용) ────────────────────────────────────
    MOVE_SPEED("이동속도", "%", true, true, 2, 8),
    CRIT_DAMAGE("치명타 데미지", "%", true, true, 5, 20),
    BONUS_ATK("공격력", "", true, true, 1, 8),
    BONUS_DEF("방어력", "", true, true, 1, 6),
    BONUS_HP("생명력", "", true, true, 2, 10),
    JUMP_BOOST("점프력", "", true, true, 1, 3);

    public final String displayName;
    public final String unit;
    public final boolean forWeapon;
    public final boolean forArmor;
    public final int minVal;
    public final int maxVal;

    GearOption(String name, String unit, boolean forWeapon, boolean forArmor, int min, int max) {
        this.displayName = name;
        this.unit        = unit;
        this.forWeapon   = forWeapon;
        this.forArmor    = forArmor;
        this.minVal      = min;
        this.maxVal      = max;
    }

    // 메인옵션 무기 목록
    public static GearOption[] mainWeapon() {
        return new GearOption[]{ATK_PERCENT, ATK_SPEED, CRIT_CHANCE};
    }

    // 메인옵션 방어구 목록
    public static GearOption[] mainArmor() {
        return new GearOption[]{DEFENSE, MAX_HP, CRIT_CHANCE_DEF};
    }

    // 부옵션 목록
    public static GearOption[] sub() {
        return new GearOption[]{MOVE_SPEED, CRIT_DAMAGE, BONUS_ATK, BONUS_DEF, BONUS_HP, JUMP_BOOST};
    }
}
