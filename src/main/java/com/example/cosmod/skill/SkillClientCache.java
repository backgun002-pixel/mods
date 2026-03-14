package com.example.cosmod.skill;

public class SkillClientCache {
    public static String currentJob   = null;
    public static String skill1Name   = null;
    public static String skill2Name   = null;
    public static String skill3Name   = null;
    public static String skill1Icon   = "§f✦";
    public static String skill2Icon   = "§f✦";
    public static String skill3Icon   = "§f✦";
    public static boolean skill2Locked = true;
    public static boolean skill3Locked = true;

    public static void setWarrior(int lv) {
        currentJob="WARRIOR"; skill1Name="돌진"; skill2Name="칼날폭풍"; skill3Name="광폭화";
        skill1Icon="§b⚡"; skill2Icon="§6🌀"; skill3Icon="§c🔥";
        skill2Locked=lv<15; skill3Locked=lv<20;
    }
    public static void setArcher(int lv) {
        currentJob="ARCHER"; skill1Name="더블샷"; skill2Name="애로우레인"; skill3Name="샤프아이즈";
        skill1Icon="§a🏹"; skill2Icon="§b☔"; skill3Icon="§2👁";
        skill2Locked=lv<15; skill3Locked=lv<20;
    }
    public static void setMage(int lv) {
        currentJob="MAGE"; skill1Name="매직미사일"; skill2Name="힐"; skill3Name="인피니티";
        skill1Icon="§5✦"; skill2Icon="§a✚"; skill3Icon="§d∞";
        skill2Locked=lv<15; skill3Locked=lv<20;
    }
    public static void setMonk(int lv) {
        currentJob="MONK"; skill1Name="정권지르기"; skill2Name="파쇄장"; skill3Name="정신수양";
        skill1Icon="§e👊"; skill2Icon="§6掌"; skill3Icon="§b禪";
        skill2Locked=lv<15; skill3Locked=lv<20;
    }
    public static void clear() { currentJob=null; skill1Name=null; skill2Name=null; skill3Name=null; }

    public static int activeScreenEffect = SkillScreenEffectPayload.EFFECT_NONE;
}
