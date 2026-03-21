package com.example.cosmod.combat;

public enum GemTier {
    BASIC("하급", 2, 2, new int[][]{{1,3},{1,2},{1,2},{1,2},{1,3},{2,5},{1,3}}),
    MID  ("중급", 2, 3, new int[][]{{2,5},{2,4},{2,4},{2,4},{2,5},{4,8},{2,5}}),
    HIGH ("상급", 3, 3, new int[][]{{4,8},{3,6},{3,6},{2,5},{3,7},{6,12},{2,5}}),
    ;

    public final String displayName;
    public final int optionLines;
    public final int maxRerolls;
    /** [optIdx][0=min,1=max] - GearOption.GEM_OPT_NAMES 순서와 동일 */
    public final int[][] ranges;

    GemTier(String name, int lines, int maxRerolls, int[][] ranges) {
        this.displayName = name;
        this.optionLines = lines;
        this.maxRerolls  = maxRerolls;
        this.ranges      = ranges;
    }
}
