package com.example.cosmod.job;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * 플레이어 직업 데이터
 * - 생활직업 1개 + 전투직업 1개 동시 보유
 * - 각 직업마다 독립적인 EXP/레벨
 * - NBT로 저장/불러오기
 */
public class PlayerJobData {

    private static final int MAX_LEVEL = 20;

    // 직업별 EXP 저장 (직업 없으면 0)
    private final Map<JobClass, Integer> expMap   = new HashMap<>();
    private final Map<JobClass, Integer> levelMap = new HashMap<>();

    // 현재 선택된 직업
    private JobClass lifeJob   = null;
    private JobClass combatJob = null;

    // ── 직업 선택 ─────────────────────────────────────────────────
    public boolean setJob(JobClass job) {
        if (job.category == JobClass.JobCategory.LIFE) {
            lifeJob = job;
            expMap.putIfAbsent(job, 0);
            levelMap.putIfAbsent(job, 1);
        } else {
            combatJob = job;
            expMap.putIfAbsent(job, 0);
            levelMap.putIfAbsent(job, 1);
        }
        return true;
    }

    public JobClass getLifeJob()   { return lifeJob; }
    public JobClass getCombatJob() { return combatJob; }
    public void setLifeJob(JobClass j)   { lifeJob   = j; expMap.putIfAbsent(j, 0); levelMap.putIfAbsent(j, 1); }
    public void setCombatJob(JobClass j) { combatJob = j; expMap.putIfAbsent(j, 0); levelMap.putIfAbsent(j, 1); }

    /** 직업 변경 시 기존 직업 레벨/EXP 초기화 */
    public void resetJob(JobClass.JobCategory category) {
        JobClass existing = category == JobClass.JobCategory.LIFE ? lifeJob : combatJob;
        if (existing == null) return;
        expMap.put(existing, 0);
        levelMap.put(existing, 1);
        if (category == JobClass.JobCategory.LIFE) lifeJob = null;
        else combatJob = null;
    }

    public boolean hasJob(JobClass job) {
        return job == lifeJob || job == combatJob;
    }

    // ── EXP/레벨 ──────────────────────────────────────────────────
    /** 저장 파일 불러올 때 직업 보유 여부 관계없이 강제 설정 */
    public void forceSetLevel(JobClass job, int level) {
        levelMap.put(job, Math.max(1, level));
    }
    public void forceSetExp(JobClass job, int exp) {
        expMap.put(job, Math.max(0, exp));
    }

    public void setLevel(JobClass job, int level) {
        if (!hasJob(job)) return;
        levelMap.put(job, Math.max(1, level));
        expMap.put(job, 0);
    }

    public int getLevel(JobClass job) {
        return levelMap.getOrDefault(job, 0);
    }

    public int getExp(JobClass job) {
        return expMap.getOrDefault(job, 0);
    }

    private static final int[] EXP_TABLE = {
        100,   // Lv1→2
        120,   // Lv2→3
        200,   // Lv3→4
        320,   // Lv4→5
        500,   // Lv5→6
        800,   // Lv6→7
        1200,  // Lv7→8
        1800,  // Lv8→9
        2500,  // Lv9→10
        3500,  // Lv10→11
        5000,  // Lv11→12
        7000,  // Lv12→13
        9500,  // Lv13→14
        12500, // Lv14→15
        16000, // Lv15→16
        20000, // Lv16→17
        25000, // Lv17→18
        31000, // Lv18→19
        38000, // Lv19→20
        0,     // Lv20 (MAX)
    };

    public int getExpToNext(int level) {
        if (level <= 0) return EXP_TABLE[0];
        if (level >= EXP_TABLE.length) return 0;
        return EXP_TABLE[level - 1];
    }

    /**
     * EXP 추가 → 레벨업 처리
     * @return 레벨업 횟수 (0 = 레벨업 없음)
     */
    public int addExp(JobClass job, int amount) {
        if (!hasJob(job)) return 0;
        int currentLevel = getLevel(job);
        if (currentLevel >= MAX_LEVEL) return 0;

        int currentExp = getExp(job) + amount;
        int levelsGained = 0;

        while (currentLevel < MAX_LEVEL) {
            int needed = getExpToNext(currentLevel);
            if (currentExp >= needed) {
                currentExp -= needed;
                currentLevel++;
                levelsGained++;
            } else break;
        }

        expMap.put(job, currentExp);
        levelMap.put(job, currentLevel);
        return levelsGained;
    }

    // ── NBT 저장/불러오기 ─────────────────────────────────────────
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        if (lifeJob   != null) tag.putString("lifeJob",   lifeJob.name());
        if (combatJob != null) tag.putString("combatJob", combatJob.name());

        CompoundTag expTag   = new CompoundTag();
        CompoundTag levelTag = new CompoundTag();
        for (JobClass j : JobClass.values()) {
            expTag.putInt(j.name(),   expMap.getOrDefault(j, 0));
            levelTag.putInt(j.name(), levelMap.getOrDefault(j, 1));
        }
        tag.put("exp",   expTag);
        tag.put("level", levelTag);
        return tag;
    }

    public void fromNbt(CompoundTag tag) {
        if (tag.contains("lifeJob"))   lifeJob   = parseJob(tag.getString("lifeJob").orElse(""));
        if (tag.contains("combatJob")) combatJob = parseJob(tag.getString("combatJob").orElse(""));

        if (tag.contains("exp")) {
            CompoundTag expTag = tag.getCompound("exp").orElse(new CompoundTag());
            for (JobClass j : JobClass.values())
                expMap.put(j, expTag.getInt(j.name()).orElse(0));
        }
        if (tag.contains("level")) {
            CompoundTag levelTag = tag.getCompound("level").orElse(new CompoundTag());
            for (JobClass j : JobClass.values())
                levelMap.put(j, levelTag.getInt(j.name()).orElse(1));
        }
    }

    private JobClass parseJob(String name) {
        try { return JobClass.valueOf(name); } catch (Exception e) { return null; }
    }

    public static final String NBT_KEY = "cosmod_jobs";
}
