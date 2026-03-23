package com.example.cosmod.combat;

public enum GemTier {
    // ranges index: 0=ATK, 1=SPD, 2=CRIT, 3=HP, 4=DEF
    BASIC("하급", 2, 2, new int[][]{{1,3},{1,2},{1,2},{2,5},{1,3}}),
    MID  ("중급", 2, 3, new int[][]{{2,5},{2,4},{2,4},{4,8},{2,5}}),
    HIGH ("상급", 3, 3, new int[][]{{4,8},{3,6},{3,6},{6,12},{2,5}}),
    ;

    public final String displayName;
    public final int optionLines;
    public final int maxRerolls;
    public final int[][] ranges;

    GemTier(String name, int lines, int maxRerolls, int[][] ranges) {
        this.displayName = name;
        this.optionLines = lines;
        this.maxRerolls  = maxRerolls;
        this.ranges      = ranges;
    }
}
