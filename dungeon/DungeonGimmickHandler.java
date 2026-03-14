package com.example.cosmod.dungeon;

import com.example.cosmod.dungeon.DungeonSpawner;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class DungeonGimmickHandler {

    public static final List<RoomData> COMBAT_ROOMS   = new ArrayList<>();
    public static final List<RoomData> MINIBOSS_ROOMS = new ArrayList<>();
    public static boolean BOSS_DEFEATED = false;
    public static boolean BOSS_SPAWNED  = false;
    public static List<BlockPos> BOSS_DOORS  = new ArrayList<>();
    public static List<BlockPos> BOSS_CHESTS = new ArrayList<>();
    public static BlockPos PORTAL_POS = null;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            ServerLevel level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (level == null) return;

            if (!BOSS_SPAWNED && PORTAL_POS != null) {
                AABB bossArea = AABB.ofSize(PORTAL_POS.getCenter(), 40, 20, 60);
                if (!level.getEntitiesOfClass(ServerPlayer.class, bossArea).isEmpty()) {
                    BOSS_SPAWNED = true;
                    DungeonSpawner.spawnBossRoom(level, PORTAL_POS.offset(0, 1, -11));
                    for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, bossArea))
                        p.displayClientMessage(Component.literal("§4§l[던전] 수호자가 강림했다!"), false);
                }
            }

            if (BOSS_DEFEATED && PORTAL_POS != null) {
                AABB glowArea = AABB.ofSize(PORTAL_POS.getCenter(), 3, 2, 3);
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, glowArea))
                    DungeonManager.teleportBack(p);
            }

            for (RoomData room : COMBAT_ROOMS) {
                if (room.cleared) continue;
                if (level.getEntitiesOfClass(ServerPlayer.class, room.bounds).isEmpty()) continue;
                if (!room.doorsClosed) {
                    closeDoors(level, room); room.doorsClosed = true;
                    for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, room.bounds))
                        p.displayClientMessage(Component.literal("§c[던전] 문이 잠겼다! 모든 적을 처치하라!"), false);
                    if (!room.spawned && room.spawnCenter != null) {
                        room.spawned = true;
                        switch (room.roomIndex) {
                            case 1 -> DungeonSpawner.spawnCombatRoom1(level, room.spawnCenter);
                            case 2 -> DungeonSpawner.spawnCombatRoom2(level, room.spawnCenter);
                            case 3 -> DungeonSpawner.spawnCombatRoom3(level, room.spawnCenter);
                            case 4 -> DungeonSpawner.spawnMinibossRoom(level, room.spawnCenter);
                        }
                    }
                }
                if (level.getEntitiesOfClass(Monster.class, room.bounds).isEmpty() && room.doorsClosed) {
                    openDoors(level, room); room.cleared = true;
                    for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, room.bounds.inflate(5)))
                        p.displayClientMessage(Component.literal("§a[던전] 적을 모두 처치했다! 문이 열렸다."), false);
                }
            }
        });
    }

    private static void closeDoors(ServerLevel level, RoomData room) {
        for (BlockPos p : room.doorPositions) level.setBlock(p, Blocks.IRON_BARS.defaultBlockState(), 3);
    }
    private static void openDoors(ServerLevel level, RoomData room) {
        for (BlockPos p : room.doorPositions) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
    }

    public static void onBossDefeated(ServerLevel level, BlockPos bossPos) {
        if (BOSS_DEFEATED) return;
        BOSS_DEFEATED = true;
        for (BlockPos d : BOSS_DOORS) level.setBlock(d, Blocks.AIR.defaultBlockState(), 3);
        if (PORTAL_POS != null) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    level.setBlock(PORTAL_POS.offset(dx, 0, dz), Blocks.GLOWSTONE.defaultBlockState(), 3);
        }
        AABB area = AABB.ofSize(bossPos.getCenter(), 50, 20, 50);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            p.displayClientMessage(Component.literal("§6§l[던전] 수호자를 처치했다!"), false);
            p.displayClientMessage(Component.literal("§e끝 벽의 밝나는 발판을 밟지세요."), false);
        }
    }

    public static class RoomData {
        public final AABB bounds;
        public final List<BlockPos> doorPositions;
        public final BlockPos spawnCenter;
        public final int roomIndex;
        public boolean doorsClosed = false, cleared = false, spawned = false;
        public RoomData(AABB b, List<BlockPos> d, BlockPos s, int i) {
            bounds=b; doorPositions=d; spawnCenter=s; roomIndex=i;
        }
    }
}
