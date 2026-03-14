package com.example.cosmod.skill;

/** 클라이언트에서 현재 직업 스킬 정보 캐시 */
public class SkillClientCache {
    public static String  currentJob   = null;
    public static String  skill1Name   = null;
    public static String  skill2Name   = null;
    public static String  skill3Name   = null;
    public static String  skill1Icon   = "§f✦";
    public static String  skill2Icon   = "§f✦";
    public static String  skill3Icon   = "§f✦";
    public static boolean skill2Locked = true;
    public static boolean skill3Locked = true;

    public static void setWarrior(int jobLevel) {
        currentJob   = "WARRIOR";
        skill1Name   = "돌진";
        skill2Name   = "칼날폭풍";
        skill3Name   = "광폭화";
        skill1Icon   = "§b⚡";
        skill2Icon   = "§6🌀";
        skill3Icon   = "§c🔥";
        skill2Locked = jobLevel < 15;
        skill3Locked = jobLevel < 20;
    }

    public static void setArcher(int jobLevel) {
        currentJob   = "ARCHER";
        skill1Name   = "더블샷";
        skill2Name   = "애로우레인";
        skill3Name   = "샤프아이즈";
        skill1Icon   = "§a🏹";
        skill2Icon   = "§b☔";
        skill3Icon   = "§2👁";
        skill2Locked = jobLevel < 15;
        skill3Locked = jobLevel < 20;
    }

    public static void clear() {
        currentJob   = null;
        skill1Name   = null;
        skill2Name   = null;
        skill3Name   = null;
    }

    // 화면 이펙트 상태
    public static int activeScreenEffect = SkillScreenEffectPayload.EFFECT_NONE;
}
