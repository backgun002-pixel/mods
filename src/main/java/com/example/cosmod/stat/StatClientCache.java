package com.example.cosmod.stat;

public class StatClientCache {
    public static float atkFlat    = 1.0f;
    public static float atkPct     = 0f;
    public static float def        = 0f;
    public static float maxHp      = 20f;
    public static float spdPct     = 0f;
    public static float critChance = 0f;
    public static float critDmg    = 0f;
    public static float jump       = 0.42f;
    public static int   coins      = 0;
    public static String jobName     = "없음";
    public static int    jobLevel    = 0;
    public static String lifeJobName = "없음";

    public static void update(StatSyncPayload p) {
        atkFlat    = p.atkFlat();
        atkPct     = p.atkPct();
        def        = p.def();
        maxHp      = p.maxHp();
        spdPct     = p.spdPct();
        critChance = p.critChance();
        critDmg    = p.critDmg();
        jump       = p.jump();
        coins      = p.coins();
        jobName    = p.jobName();
        jobLevel   = p.jobLevel();
        lifeJobName = p.lifeJobName();
    }
}
