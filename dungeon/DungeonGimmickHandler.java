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

/**
 * 던전 기믹 핸들러
 * - 전투방: 플레이어 진입 시 철문 닫힘 → 몬스터 전멸 시 열림
 * - 보스방: 보스 처치 시 철창 열림 + 상자 해제 + 귀환 포탈 생성
 */
public class DungeonGimmickHandler {

    // ── 전투방 데이터 ─────────────────────────────────────────────
    // 각 전투방의 [문 위치 목록, 몬스터 감지 영역]
    public static final List<RoomData> COMBAT_ROOMS = new ArrayList<>();
    public static boolean BOSS_DEFEATED  = false;
    public static boolean BOSS_SPAWNED   = false;

    // 미니보스방 (별도 관리용 - COMBAT_ROOMS와 중복 등록 가능)
    public static final List<RoomData> MINIBOSS_ROOMS = new ArrayList<>();

    // 보스방 문/상자/포탈 위치 (DungeonBuilder가 생성 후 설정)
    public static List<BlockPos> BOSS_DOORS   = new ArrayList<>();
    public static List<BlockPos> BOSS_CHESTS  = new ArrayList<>();
    public static BlockPos       PORTAL_POS   = null;

    public static void register() {

        // 20틱(1초)마다 전투방 체크
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            ServerLevel level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (level == null) return;

            for (RoomData room : COMBAT_ROOMS) {
                if (room.cleared) continue;

                // 플레이어가 방 안에 있는지
                boolean hasPlayer = !level.getEntitiesOfClass(
                    ServerPlayer.class, room.bounds).isEmpty();
                if (!hasPlayer) continue;

                // 문 닫기 (플레이어가 처음 들어왔을 때)
                if (!room.doorsClosed) {
                    closeDoors(level, room);
                    room.doorsClosed = true;
                    // 방 안 플레이어에게 메시지
                    for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, room.bounds)) {
                        p.displayClientMessage(
                            Component.literal("§c[던전] 문이 잠겼다! 모든 적을 처치하라!"), false);
                    }
                    // 몬스터 스폰
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

                // 몬스터 전멸 체크
                boolean noMonsters = level.getEntitiesOfClass(
                    Monster.class, room.bounds).isEmpty();
                if (noMonsters && room.doorsClosed) {
                    openDoors(level, room);
                    room.cleared = true;
                    for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, room.bounds.inflate(5))) {
                        p.displayClientMessage(
                            Component.literal("§a[던전] 적을 모두 처치했다! 문이 열렸다."), false);
                    }
                }
            }
        });
    }

    // ── 전투방 문 닫기/열기 ───────────────────────────────────────
    private static void closeDoors(ServerLevel level, RoomData room) {
        for (BlockPos pos : room.doorPositions) {
            level.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), 3);
        }
    }

    private static void openDoors(ServerLevel level, RoomData room) {
        for (BlockPos pos : room.doorPositions) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    // ── 보스 처치 이벤트 ─────────────────────────────────────────
    public static void onBossDefeated(ServerLevel level, BlockPos bossPos) {
        if (BOSS_DEFEATED) return;
        BOSS_DEFEATED = true;

        // 1. 철창 문 열기
        for (BlockPos door : BOSS_DOORS) {
            level.setBlock(door, Blocks.AIR.defaultBlockState(), 3);
        }

        // 2. 상자 잠금 해제 (Chest 위 철창 제거)
        for (BlockPos chest : BOSS_CHESTS) {
            BlockPos above = chest.above();
            if (level.getBlockState(above).is(Blocks.IRON_BARS)) {
                level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        // 3. 귀환 포탈 생성 (엔드 포탈 프레임 + 엔드 포탈 블록)
        if (PORTAL_POS != null) {
            buildReturnPortal(level, PORTAL_POS);
        }

        // 4. 주변 플레이어에게 알림
        AABB bossArea = AABB.ofSize(bossPos.getCenter(), 50, 20, 50);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, bossArea)) {
            p.displayClientMessage(
                Component.literal("§6§l[던전] 보스를 처치했다! 귀환 포탈이 열렸다!"), false);
            p.displayClientMessage(
                Component.literal("§e보상 상자를 확인하세요."), false);
        }
    }

    // ── 귀환 포탈 (엔드 포탈 프레임 3x3) ─────────────────────────
    private static void buildReturnPortal(ServerLevel level, BlockPos center) {
        // 포탈 프레임 (3x3 테두리)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos framePos = center.offset(dx, 0, dz);
                if (dx == 0 && dz == 0) continue;
                level.setBlock(framePos,
                    Blocks.END_PORTAL_FRAME.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.EndPortalFrameBlock.HAS_EYE, true),
                    3);
            }
        }
        // 포탈 블록 (중앙)
        level.setBlock(center, Blocks.END_PORTAL.defaultBlockState(), 3);

        // 포탈 옆 안내판
        BlockPos signPos = center.north(2);
        level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
    }

    // ── 데이터 클래스 ─────────────────────────────────────────────
    public static class RoomData {
        public final AABB bounds;
        public final List<BlockPos> doorPositions;
        public final BlockPos spawnCenter;
        public final int roomIndex;
        public boolean doorsClosed  = false;
        public boolean cleared      = false;
        public boolean spawned      = false;

        public RoomData(AABB bounds, List<BlockPos> doorPositions, BlockPos spawnCenter, int roomIndex) {
            this.bounds        = bounds;
            this.doorPositions = doorPositions;
            this.spawnCenter   = spawnCenter;
            this.roomIndex     = roomIndex;
        }
    }
}
