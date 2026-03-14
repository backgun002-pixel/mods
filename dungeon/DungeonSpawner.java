package com.example.cosmod.dungeon;

import com.example.cosmod.dungeon.entity.DungeonGuardianEntity;
import com.example.cosmod.dungeon.entity.EliteStoneGuardEntity;
import com.example.cosmod.dungeon.entity.StoneGolemEntity;
import com.example.cosmod.dungeon.entity.StoneGuardEntity;
import com.example.cosmod.entity.CosmodEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

public class DungeonSpawner {

    public static void spawnCombatRoom1(ServerLevel level, BlockPos center) {
        spawnGuard(level, center.offset(-3, 1,  0));
        spawnGuard(level, center.offset( 3, 1,  0));
        spawnGuard(level, center.offset( 0, 1, -3));
        spawnGolem(level, center.offset( 0, 1,  3));
    }

    public static void spawnCombatRoom2(ServerLevel level, BlockPos center) {
        spawnGuard(level, center.offset(-4, 1,  0));
        spawnGuard(level, center.offset( 4, 1,  0));
        spawnGolem(level, center.offset(-2, 1, -2));
        spawnGolem(level, center.offset( 2, 1,  2));
    }

    public static void spawnCombatRoom3(ServerLevel level, BlockPos center) {
        spawnGuard(level, center.offset(-3, 1, -3));
        spawnGuard(level, center.offset( 3, 1,  3));
        spawnGolem(level, center.offset( 3, 1, -3));
        spawnGolem(level, center.offset(-3, 1,  3));
    }

    public static void spawnMinibossRoom(ServerLevel level, BlockPos center) {
        spawnElite(level, center);
        spawnGuard(level, center.offset(-5, 1, 0));
        spawnGuard(level, center.offset( 5, 1, 0));
    }

    public static void spawnBossRoom(ServerLevel level, BlockPos center) {
        spawnBoss(level, center);
    }

    private static void spawnGuard(ServerLevel level, BlockPos pos) {
        StoneGuardEntity e = new StoneGuardEntity(CosmodEntities.STONE_GUARD, level);
        e.setPos(pos.getX()+.5, pos.getY(), pos.getZ()+.5);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
        level.addFreshEntity(e);
    }

    private static void spawnGolem(ServerLevel level, BlockPos pos) {
        StoneGolemEntity e = new StoneGolemEntity(CosmodEntities.STONE_GOLEM, level);
        e.setPos(pos.getX()+.5, pos.getY(), pos.getZ()+.5);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
        level.addFreshEntity(e);
    }

    private static void spawnElite(ServerLevel level, BlockPos pos) {
        EliteStoneGuardEntity e = new EliteStoneGuardEntity(CosmodEntities.ELITE_STONE_GUARD, level);
        e.setPos(pos.getX()+.5, pos.getY(), pos.getZ()+.5);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
        level.addFreshEntity(e);
    }

    private static void spawnBoss(ServerLevel level, BlockPos pos) {
        DungeonGuardianEntity e = new DungeonGuardianEntity(CosmodEntities.DUNGEON_GUARDIAN, level);
        e.setPos(pos.getX()+.5, pos.getY()+1.0, pos.getZ()+.5);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
        level.addFreshEntity(e);
    }
}
