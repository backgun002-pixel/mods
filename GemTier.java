package com.example.cosmod.combat;

/**
 * 보석 등급 - 하급/중급/상급
 * 등급마다 옵션 수치 범위와 줄 수가 다릅니다.
 */
public enum GemTier {
    //         이름    색상   줄수  최대재설정
    BASIC  ("하급", "§7",  2,   3),
    MID    ("중급", "§a",  2,   3),
    HIGH   ("상급", "§d",  3,   3),
    ;

    public final String displayName;
    public final String color;
    public final int optionLines;   // 옵션 줄 수 (하급/중급=2, 상급=3)
    public final int maxRerolls;    // 최대 재설정 횟수

    GemTier(String name, String color, int lines, int maxRerolls) {
        this.displayName = name;
        this.color       = color;
        this.optionLines = lines;
        this.maxRerolls  = maxRerolls;
    }

    /**
     * 옵션 종류별 수치 범위 [min, max]
     * 0=공격력, 1=주문력, 2=신성력, 3=이동속도, 4=치명타확률, 5=체력, 6=방어력
     */
    public int[] getRange(int optType) {
        return switch (this) {
            case BASIC -> new int[][]{{1,3},{1,3},{1,3},{1,3},{1,2},{1,5},{1,3}}[optType];
            case MID   -> new int[][]{{3,7},{3,7},{3,7},{3,7},{2,5},{5,12},{3,7}}[optType];
            case HIGH  -> new int[][]{{6,14},{6,14},{6,14},{6,14},{4,8},{10,22},{6,12}}[optType];
        };
    }

    public static final String[] OPT_NAMES = {
        "공격력", "주문력", "신성력", "이동속도", "치명타 확률", "체력", "방어력"
    };
    public static final String[] OPT_UNITS = {
        "", "", "", "%", "%", "", ""
    };
}
