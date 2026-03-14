package com.example.cosmod.job;

/**
 * 직업 정의
 * LIFE: 생활직업 / COMBAT: 전투직업
 */
public enum JobClass {

    // ── 생활직업 ──────────────────────────────────────────────────
    MINER   ("광부",    JobCategory.LIFE,
        new int[]{5,10,15,20},
        new String[]{
            "광석 추가 드롭 +10%",
            "채굴속도 +15%",
            "희귀 광석 드롭률 +5%",
            "포춘 효과 자동 적용"
        }),
    FARMER  ("농사꾼",  JobCategory.LIFE,
        new int[]{5,10,15,20},
        new String[]{
            "작물 2배 드롭 확률 +15%",
            "코스모드 작물 판매가 +10%",
            "씨앗 심기 즉시 성장 확률 +5%",
            "작물 수확 시 EXP 2배"
        }),
    COOK    ("요리사",  JobCategory.LIFE,
        new int[]{5,10,15,20},
        new String[]{
            "음식 허기 회복 +1",
            "음식 섭취 시 재생 효과",
            "포션 지속시간 +25%",
            "특수 요리 레시피 해금"
        }),

    // ── 전투직업 ──────────────────────────────────────────────────
    WARRIOR ("전사",   JobCategory.COMBAT,
        new int[]{5,10,15,20},
        new String[]{
            "근접 공격력 +5%",
            "피격 데미지 감소 +5%",
            "돌진 스킬 해금",
            "광폭화 스킬 해금"
        }),
    ARCHER  ("궁수",   JobCategory.COMBAT,
        new int[]{5,10,15,20},
        new String[]{
            "화살 데미지 +5%",
            "이동속도 +5%",
            "다중화살 스킬 해금",
            "독화살 스킬 해금"
        });

    public final String displayName;
    public final JobCategory category;
    public final int[] bonusLevels;      // 보너스 발동 레벨
    public final String[] bonusDesc;     // 보너스 설명

    JobClass(String name, JobCategory cat, int[] levels, String[] desc) {
        this.displayName = name;
        this.category    = cat;
        this.bonusLevels = levels;
        this.bonusDesc   = desc;
    }

    public enum JobCategory { LIFE, COMBAT }

    public static JobClass[] lifeJobs() {
        return new JobClass[]{MINER, FARMER, COOK};
    }
    public static JobClass[] combatJobs() {
        return new JobClass[]{WARRIOR, ARCHER};
    }
}
