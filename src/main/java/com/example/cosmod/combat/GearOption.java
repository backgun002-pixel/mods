package com.example.cosmod.combat;

public enum GearOption {
    // 공격
    BONUS_ATK("공격력 +", true),
    ATK_SPEED("공격속도 +", true),

    // 방어/생존
    DEFENSE("방어력 +", false),
    BONUS_DEF("방어력 +", false),
    MAX_HP("최대 체력 +", false),
    BONUS_HP("최대 체력 +", false),

    // 이동
    MOVE_SPEED("이동속도 +", true),
    JUMP_BOOST("점프력 +", true),

    // 치명타
    CRIT_CHANCE("치명타 확률 +", true),
    CRIT_DAMAGE("치명타 피해 +", true),
    CRIT_CHANCE_DEF("치명타 방어 +", false),

    // 보석 전용
    GEM_ATK("공격력 +", true),
    GEM_DEF("방어력 +", false),
    GEM_HP("최대 체력 +", false),
    GEM_SPD("이동속도 +", true),
    GEM_CRIT("치명타 확률 +", true),
    GEM_MAGIC("마법 피해 +", true),
    GEM_HOLY("신성 피해 +", true),
    ;

    public final String displayName;
    public final boolean isOffensive;

    GearOption(String name, boolean offensive) {
        this.displayName = name;
        this.isOffensive = offensive;
    }

    /** 보석 전용 옵션 이름 목록 (GemTier에서 사용) */
    public static final String[] GEM_OPT_NAMES = {
        "공격력", "마법력", "신성력", "이동속도", "치명타확률", "체력", "방어력"
    };

    public static GearOption gem(int idx) {
        return switch (idx % 7) {
            case 0 -> GEM_ATK;
            case 1 -> GEM_MAGIC;
            case 2 -> GEM_HOLY;
            case 3 -> GEM_SPD;
            case 4 -> GEM_CRIT;
            case 5 -> GEM_HP;
            case 6 -> GEM_DEF;
            default -> GEM_ATK;
        };
    }
}
