package com.example.cosmod.combat;

import java.util.Random;

public enum WeaponGrade {
    //          이름     색상   공%  공+  체력  방어  이속%  가중치
    NORMAL  ("일반", "§7",    0,   0,   0,   0,   0,   35),
    RARE    ("희귀", "§a",    5,   3,   5,   2,   2,   30),
    HERO    ("영웅", "§9",   12,   7,  15,   5,   4,   20),
    LEGEND  ("전설", "§e§l", 22,  14,  30,  10,   7,   12),  // 노란색
    ANCIENT ("고대", "§b§l", 38,  25,  50,  18,  12,    3),  // 청록색
    ;

    public final String displayName;
    public final String color;
    public final int atkPercent;
    public final int atkFlat;
    public final int hpBonus;
    public final int defBonus;
    public final int speedPercent;
    public final int weight;

    WeaponGrade(String name, String color, int atkPct, int atkFlat,
                int hp, int def, int spd, int weight) {
        this.displayName  = name;
        this.color        = color;
        this.atkPercent   = atkPct;
        this.atkFlat      = atkFlat;
        this.hpBonus      = hp;
        this.defBonus     = def;
        this.speedPercent = spd;
        this.weight       = weight;
    }

    public static WeaponGrade roll(Random rng) {
        int total = 0;
        for (WeaponGrade g : values()) total += g.weight;
        int r = rng.nextInt(total);
        int acc = 0;
        for (WeaponGrade g : values()) {
            acc += g.weight;
            if (r < acc) return g;
        }
        return NORMAL;
    }

    public String formatName(String weaponName, int enhLevel) {
        String base = color + "[" + displayName + "] §r§f" + weaponName;
        if (enhLevel > 0) base += " §e+" + enhLevel;
        return base;
    }
}
