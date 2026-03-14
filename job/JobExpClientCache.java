package com.example.cosmod.job;

public class JobExpClientCache {
    public static int level     = 0;
    public static int exp       = 0;
    public static int expToNext = 100;
    public static int levelUpTimer = 0;
    private static final int DURATION = 80; // 4초

    public static void set(int lv, int e, int etn, boolean levelUp) {
        level     = lv;
        exp       = e;
        expToNext = etn;
        if (levelUp) levelUpTimer = DURATION;
    }

    public static void tick() {
        if (levelUpTimer > 0) levelUpTimer--;
    }
}
