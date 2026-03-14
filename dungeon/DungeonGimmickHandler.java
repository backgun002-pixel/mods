package com.example.cosmod.dungeon;

import com.example.cosmod.dungeon.DungeonSpawner;
import com.example.cosmod.CosmodMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class DungeonGimmickHandler {

    public static final List<RoomData> COMBAT_ROOMS = new ArrayList<>();
    public static boolean BOSS_DEFEATED  = false;
    public static boolean BOSS_SPAWNED   = false;
    public static final List<RoomData> MINIBOSS_ROOMS = new ArrayList<>();
    public static List<BlockPos> BOSS_DOORS   = new ArrayList<>();
    public static List<BlockPos> BOSS_CHESTS  = new ArrayList<>();
    public static BlockPos       PORTAL_POS   = null;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            ServerLevel level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (level == null) return;

            if (!BOSS_SPAWNED && PORTAL_POS != null) {
                AABB bossArea = AABB.ofSize(PORTAL_POS.getCenter(), 40, 20, 60);
                boolean playerInBoss = !level.getEntitiesOfClass(ServerPlayer.class, bossArea).isEmpty();
                if (playerInBoss) {
                    BOSS_SPAWNED = true;
                    BlockPos bossCenter = PORTAL_POS.offset(0, 0, -10);
                    DungeonSpawner.spawnBossRoom(level, bossCenter);
                    for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, bossArea)) {
                        p.displayClientMessage(Component.literal("§4§l[던전] 보스가 강림했다!"), false);
                    }
                }
            }

            for (RoomData room : COMBAT_ROOMS) {
                if (room.cleared) continue;
                boolean hasPlayer = !level.getEntitiesOfClass(ServerPlayer.class, room.bounds).isEmpty();
                if (!hasPlayer) continue;
                if (!room.doorsClosed) {
                    closeDoors(level, room);
                    room.doorsClosed = true;
                    for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, room.bounds)) {
                        p.displayClientMessage(Component.literal("§c[던전] 문이 잠겼다! 모든 적을 처치하라!"), false);
                    }
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
                boolean noMonsters = level.getEntitiesOfClass(Monster.class, room.bounds).isEmpty();
                if (noMonsters && room.doorsClosed) {
                    openDoors(level, room);
                    room.cleared = true;
                    for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, room.bounds.inflate(5))) {
                        p.displayClientMessage(Component.literal("§a[던전] 적을 모두 처치했다! 문이 열렸다."), false);
                    }
                }
            }
        });
    }

    private static void closeDoors(ServerLevel level, RoomData room) {
        for (BlockPos pos : room.doorPositions) level.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), 3);
    }

    private static void openDoors(ServerLevel level, RoomData room) {
        for (BlockPos pos : room.doorPositions) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    public static void onBossDefeated(ServerLevel level, BlockPos bossPos) {
        if (BOSS_DEFEATED) return;
        BOSS_DEFEATED = true;
        for (BlockPos door : BOSS_DOORS) level.setBlock(door, Blocks.AIR.defaultBlockState(), 3);
        for (BlockPos chest : BOSS_CHESTS) {
            BlockPos above = chest.above();
            if (level.getBlockState(above).is(Blocks.IRON_BARS)) level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
        }
        if (PORTAL_POS != null) buildReturnPortal(level, PORTAL_POS);
        AABB bossArea = AABB.ofSize(bossPos.getCenter(), 50, 20, 50);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, bossArea)) {
            p.displayClientMessage(Component.literal("§6§l[던전] 보스를 처치했다! 귀환 포탈이 열렸다!"), false);
            p.displayClientMessage(Component.literal("§e보상 상자를 확인하세요."), false);
        }
    }

    private static void buildReturnPortal(ServerLevel level, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos fp = center.offset(dx, 0, dz);
            if (dx == 0 && dz == 0) continue;
            level.setBlock(fp, Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(net.minecraft.world.level.block.EndPortalFrameBlock.HAS_EYE, true), 3);
        }
        level.setBlock(center, Blocks.END_PORTAL.defaultBlockState(), 3);
        level.setBlock(center.north(2), Blocks.OAK_SIGN.defaultBlockState(), 3);
    }

    public static class RoomData {
        public final AABB bounds;
        public final List<BlockPos> doorPositions;
        public final BlockPos spawnCenter;
        public final int roomIndex;
        public boolean doorsClosed = false;
        public boolean cleared     = false;
        public boolean spawned     = false;
        public RoomData(AABB bounds, List<BlockPos> doorPositions, BlockPos spawnCenter, int roomIndex) {
            this.bounds = bounds; this.doorPositions = doorPositions;
            this.spawnCenter = spawnCenter; this.roomIndex = roomIndex;
        }
    }
}
