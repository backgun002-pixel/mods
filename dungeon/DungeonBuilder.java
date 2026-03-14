package com.example.cosmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonBuilder {

    private static final int CORRIDOR_W = 7,  CORRIDOR_H = 6,  CORRIDOR_L = 12;
    private static final int ROOM_W     = 16, ROOM_H     = 8,  ROOM_L     = 16;
    private static final int MINIBOSS_W = 20, MINIBOSS_H = 9,  MINIBOSS_L = 20;
    private static final int BOSS_W     = 28, BOSS_H     = 14, BOSS_L     = 28;

    private final ServerLevel level;
    private final BlockPos origin;
    private final Random rng = new Random(12345); // 고정 시드

    public DungeonBuilder(ServerLevel level, BlockPos origin) {
        this.level  = level;
        this.origin = origin;
    }

    public void build() {
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();

        buildEntrance(x, y, z);
        z += 4;

        // 복도1
        buildCorridor(x, y, z, CORRIDOR_L);
        z += CORRIDOR_L;

        // 전투방1
        int r1x = x - ROOM_W/2 + CORRIDOR_W/2;
        buildCombatRoom(r1x, y, z, ROOM_W, ROOM_H, ROOM_L, 1);
        z += ROOM_L;

        // 복도2
        buildCorridor(x, y, z, CORRIDOR_L);
        z += CORRIDOR_L;

        // 전투방2
        int r2x = x - ROOM_W/2 + CORRIDOR_W/2;
        buildCombatRoom(r2x, y, z, ROOM_W, ROOM_H, ROOM_L, 2);
        z += ROOM_L;

        // 복도3
        buildCorridor(x, y, z, CORRIDOR_L);
        z += CORRIDOR_L;

        // 조형물방 (옛 함정방)
        int r3x = x - ROOM_W/2 + CORRIDOR_W/2;
        buildSculptureRoom(r3x, y, z, ROOM_W, ROOM_H, ROOM_L);
        z += ROOM_L;

        // 복도4
        buildCorridor(x, y, z, CORRIDOR_L);
        z += CORRIDOR_L;

        // 미니보스방
        int mbx = x - MINIBOSS_W/2 + CORRIDOR_W/2;
        buildMinibossRoom(mbx, y, z);
        z += MINIBOSS_L;

        // 복도5
        buildCorridor(x, y, z, CORRIDOR_L);
        z += CORRIDOR_L;

        // 보스방
        int bx = x - BOSS_W/2 + CORRIDOR_W/2;
        buildBossRoom(bx, y, z);
    }

    // ── 입구 ──────────────────────────────────────────────────────
    private void buildEntrance(int x, int y, int z) {
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++)
                set(x+dx, y, z+dz, DungeonBlocks.FLOOR_TILE);

        // 기둥 2개
        for (int dy = 1; dy <= 6; dy++) {
            set(x-2, y+dy, z, dy % 2 == 0 ? DungeonBlocks.ACCENT_POLY : DungeonBlocks.PILLAR);
            set(x+2, y+dy, z, dy % 2 == 0 ? DungeonBlocks.ACCENT_POLY : DungeonBlocks.PILLAR);
        }
        // 아치 상단
        for (int dx = -2; dx <= 2; dx++) set(x+dx, y+6, z, DungeonBlocks.WALL);
        set(x, y+7, z, DungeonBlocks.WALL_CHISELED);
        // 아치 내부 개방
        for (int dy = 1; dy <= 5; dy++)
            for (int dx = -1; dx <= 1; dx++)
                set(x+dx, y+dy, z, DungeonBlocks.AIR);
        // 랜턴
        set(x-2, y+5, z+1, DungeonBlocks.LANTERN);
        set(x+2, y+5, z+1, DungeonBlocks.LANTERN);
    }

    // ── 복도 ──────────────────────────────────────────────────────
    private void buildCorridor(int x, int y, int z, int length) {
        int hw = CORRIDOR_W / 2;
        for (int dz = 0; dz < length; dz++) {
            for (int dx = -hw; dx <= hw; dx++) {
                set(x+dx, y, z+dz, DungeonBlocks.FLOOR);
                set(x+dx, y+CORRIDOR_H, z+dz, DungeonBlocks.CEILING);
            }
            for (int dy = 1; dy < CORRIDOR_H; dy++) {
                set(x-hw-1, y+dy, z+dz, DungeonBlocks.WALL);
                set(x+hw+1, y+dy, z+dz, DungeonBlocks.WALL);
                for (int dx = -hw; dx <= hw; dx++)
                    set(x+dx, y+dy, z+dz, DungeonBlocks.AIR);
            }
            if (rng.nextInt(10) == 0)
                set(x + rng.nextInt(CORRIDOR_W) - hw, y+CORRIDOR_H-1, z+dz, DungeonBlocks.COBWEB);
        }
        // 랜턴
        for (int dz = 2; dz < length; dz += 4)
            set(x, y+CORRIDOR_H-1, z+dz, DungeonBlocks.LANTERN);
        // 벽 철창 장식
        set(x-hw-1, y+2, z+length/2, DungeonBlocks.IRON_BARS);
        set(x+hw+1, y+2, z+length/2, DungeonBlocks.IRON_BARS);
    }

    // ── 전투방 ────────────────────────────────────────────────────
    private void buildCombatRoom(int x, int y, int z, int w, int h, int l, int roomIdx) {
        buildRoomShell(x, y, z, w, h, l, DungeonBlocks.FLOOR, DungeonBlocks.WALL, DungeonBlocks.CEILING);

        // 4 코너 기둥
        buildPillar(x+1, y, z+1, h-1);
        buildPillar(x+w-2, y, z+1, h-1);
        buildPillar(x+1, y, z+l-2, h-1);
        buildPillar(x+w-2, y, z+l-2, h-1);

        // 천장 랜턴
        set(x+w/2, y+h-1, z+l/2, DungeonBlocks.SHROOMLIGHT);
        set(x+w/2, y+h-1, z+2,   DungeonBlocks.LANTERN);
        set(x+w/2, y+h-1, z+l-3, DungeonBlocks.LANTERN);
        set(x+2,   y+h-1, z+l/2, DungeonBlocks.LANTERN);
        set(x+w-3, y+h-1, z+l/2, DungeonBlocks.LANTERN);

        // 벽 균열 장식
        for (int i = 0; i < 5; i++)
            set(x + 1 + rng.nextInt(w-2), y + 1 + rng.nextInt(h-2), z, DungeonBlocks.WALL_CRACKED);

        // 스포너
        set(x+w/2, y+1, z+l/2, DungeonBlocks.SPAWNER);

        // ── 기믹: 입구/출구 문 위치 등록 ─────────────────────────
        List<BlockPos> doors = new ArrayList<>();
        // 북쪽 문 (입구)
        for (int dy = 1; dy <= 4; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                doors.add(new BlockPos(x + w/2 + dx, y+dy, z));
                // 남쪽 출구
                doors.add(new BlockPos(x + w/2 + dx, y+dy, z+l-1));
            }

        // 방 내부 AABB
        AABB roomBounds = new AABB(x+1, y, z+1, x+w-1, y+h, z+l-1);
        BlockPos spawnCenter = new BlockPos(x+w/2, y+1, z+l/2);
        DungeonGimmickHandler.COMBAT_ROOMS.add(
            new DungeonGimmickHandler.RoomData(roomBounds, doors, spawnCenter, roomIdx));

        // 처음엔 문 열어두기 (입장 전까지)
        openDoorway(x + w/2, y, z,     4);
        openDoorway(x + w/2, y, z+l-1, 4);

        // 보상상자
        set(x+2, y+1, z+l-3, DungeonBlocks.CHEST);
    }

    private void openDoorway(int cx, int y, int z, int height) {
        for (int dy = 1; dy <= height; dy++)
            for (int dx = -1; dx <= 1; dx++)
                set(cx+dx, y+dy, z, DungeonBlocks.AIR);
    }

    // ── 조형물방 (구 함정방) ──────────────────────────────────────
    private void buildSculptureRoom(int x, int y, int z, int w, int h, int l) {
        buildRoomShell(x, y, z, w, h, l, DungeonBlocks.FLOOR_TILE, DungeonBlocks.WALL, DungeonBlocks.CEILING);

        int cx = x + w/2, cz = z + l/2;

        // 중앙 해골 조형물 (네더라이트 블록 대신 흑요석 + 뼈 블록)
        // 받침대
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                set(cx+dx, y+1, cz+dz, DungeonBlocks.ACCENT_POLY);
        set(cx, y+2, cz, Blocks.BONE_BLOCK.defaultBlockState());
        set(cx, y+3, cz, Blocks.BONE_BLOCK.defaultBlockState());
        set(cx, y+4, cz, Blocks.BONE_BLOCK.defaultBlockState());
        // 머리
        set(cx, y+5, cz, Blocks.CARVED_PUMPKIN.defaultBlockState());
        // 양팔
        set(cx-1, y+3, cz, Blocks.BONE_BLOCK.defaultBlockState()
            .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                net.minecraft.core.Direction.Axis.X));
        set(cx+1, y+3, cz, Blocks.BONE_BLOCK.defaultBlockState()
            .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                net.minecraft.core.Direction.Axis.X));

        // 4 코너 관 모양 기둥 (Soul Lantern 위에 뼈 기둥)
        int[][] corners = {{x+2, z+2},{x+w-3, z+2},{x+2, z+l-3},{x+w-3, z+l-3}};
        for (int[] c : corners) {
            for (int dy = 1; dy <= 4; dy++)
                set(c[0], y+dy, c[1], dy % 2 == 0 ? DungeonBlocks.ACCENT : DungeonBlocks.PILLAR);
            set(c[0], y+5, c[1], DungeonBlocks.LANTERN);
        }

        // 벽면 그림 (Chiseled 블록)
        for (int dx = 2; dx < w-2; dx += 2) {
            set(x+dx, y+3, z,     DungeonBlocks.WALL_CHISELED);
            set(x+dx, y+3, z+l-1, DungeonBlocks.WALL_CHISELED);
        }
        for (int dz = 2; dz < l-2; dz += 2) {
            set(x,     y+3, z+dz, DungeonBlocks.WALL_CHISELED);
            set(x+w-1, y+3, z+dz, DungeonBlocks.WALL_CHISELED);
        }

        // 벽 랜턴
        set(x+w/2, y+h-1, z+l/2, DungeonBlocks.SHROOMLIGHT);

        openDoorway(x + w/2, y, z,     4);
        openDoorway(x + w/2, y, z+l-1, 4);
    }

    // ── 미니보스방 ────────────────────────────────────────────────
    private void buildMinibossRoom(int x, int y, int z) {
        int w = MINIBOSS_W, h = MINIBOSS_H, l = MINIBOSS_L;
        buildRoomShell(x, y, z, w, h, l, DungeonBlocks.FLOOR_TILE, DungeonBlocks.WALL, DungeonBlocks.CEILING);

        int cx = x+w/2, cz = z+l/2;

        // 웅장한 4 코너 기둥
        buildGrandPillar(x+1,   y, z+1,   h);
        buildGrandPillar(x+w-3, y, z+1,   h);
        buildGrandPillar(x+1,   y, z+l-3, h);
        buildGrandPillar(x+w-3, y, z+l-3, h);

        // 벽 상단 길드 블랙스톤 장식
        for (int dx = 2; dx < w-2; dx += 3) {
            set(x+dx, y+h-1, z,     DungeonBlocks.GILDED);
            set(x+dx, y+h-1, z+l-1, DungeonBlocks.GILDED);
        }

        // 천장 조명
        for (int dx = 3; dx < w-3; dx += 4)
            for (int dz = 3; dz < l-3; dz += 4)
                set(x+dx, y+h-1, z+dz, DungeonBlocks.SHROOMLIGHT);

        // 미니보스 스포너 (철창 우리 안)
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2)
                    for (int dy = 1; dy <= 3; dy++)
                        set(cx+dx, y+dy, cz+dz, DungeonBlocks.IRON_BARS);
        set(cx, y+1, cz, DungeonBlocks.SPAWNER);

        // 보상 상자
        set(x+2, y+1, z+l-3, DungeonBlocks.CHEST);
        set(x+3, y+1, z+l-3, DungeonBlocks.CHEST);

        // 기믹 등록 (roomIndex=4 → spawnMinibossRoom)
        List<BlockPos> mbDoors = new ArrayList<>();
        for (int dy = 1; dy <= 4; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                mbDoors.add(new BlockPos(x + w/2 + dx, y+dy, z));
                mbDoors.add(new BlockPos(x + w/2 + dx, y+dy, z+l-1));
            }
        AABB mbBounds = new AABB(x+1, y, z+1, x+w-1, y+h, z+l-1);
        BlockPos mbCenter = new BlockPos(x+w/2, y+1, z+l/2);
        DungeonGimmickHandler.COMBAT_ROOMS.add(
            new DungeonGimmickHandler.RoomData(mbBounds, mbDoors, mbCenter, 4));
        DungeonGimmickHandler.MINIBOSS_ROOMS.add(
            new DungeonGimmickHandler.RoomData(mbBounds, mbDoors, mbCenter, 4));

        openDoorway(x + w/2, y, z,     4);
        openDoorway(x + w/2, y, z+l-1, 4);
    }

    // ── 보스방 ────────────────────────────────────────────────────
    private void buildBossRoom(int x, int y, int z) {
        int w = BOSS_W, h = BOSS_H, l = BOSS_L;

        // 바닥 (패턴)
        for (int dx = 0; dx < w; dx++)
            for (int dz = 0; dz < l; dz++) {
                boolean edge = dx < 2 || dx >= w-2 || dz < 2 || dz >= l-2;
                boolean diag = (dx + dz) % 4 == 0;
                set(x+dx, y, z+dz, edge ? DungeonBlocks.BOSS_PILLAR
                    : diag ? DungeonBlocks.BOSS_ACCENT : DungeonBlocks.BOSS_FLOOR);
            }

        // 천장 (돔형 - 가운데로 갈수록 높아지는 느낌)
        for (int dx = 0; dx < w; dx++)
            for (int dz = 0; dz < l; dz++) {
                boolean center = Math.abs(dx - w/2) < 5 && Math.abs(dz - l/2) < 5;
                set(x+dx, y+h, z+dz, center ? DungeonBlocks.SHROOMLIGHT : DungeonBlocks.BOSS_WALL);
            }

        // 벽 (가로줄에 울음 흑요석 포인트)
        for (int dy = 1; dy < h; dy++) {
            BlockState wall = dy % 4 == 0 ? DungeonBlocks.BOSS_ACCENT : DungeonBlocks.BOSS_WALL;
            for (int dx = 0; dx < w; dx++) {
                set(x+dx, y+dy, z,     wall);
                set(x+dx, y+dy, z+l-1, wall);
            }
            for (int dz = 0; dz < l; dz++) {
                set(x,     y+dy, z+dz, wall);
                set(x+w-1, y+dy, z+dz, wall);
            }
        }

        // 내부 공기
        for (int dx = 1; dx < w-1; dx++)
            for (int dz = 1; dz < l-1; dz++)
                for (int dy = 1; dy < h; dy++)
                    set(x+dx, y+dy, z+dz, DungeonBlocks.AIR);

        // 웅장한 6개 기둥 (좌우 3쌍)
        int[] pillarZ = {z+2, z+l/2-1, z+l-4};
        for (int pz : pillarZ) {
            buildBossPillar(x+1,   y, pz, h);
            buildBossPillar(x+w-2, y, pz, h);
        }

        // 기둥 사이 아치 (상단 연결 빔)
        for (int dx = 2; dx < w-2; dx++) {
            set(x+dx, y+h-1, z+2,   DungeonBlocks.BOSS_WALL);
            set(x+dx, y+h-1, z+l-4, DungeonBlocks.BOSS_WALL);
        }

        // 벽면 감실 (alcove) - 좌우 벽에 오목하게 파기
        for (int dz = l/4; dz <= 3*l/4; dz += l/4) {
            for (int dy = 1; dy <= 4; dy++) {
                set(x+1,   y+dy, z+dz, DungeonBlocks.AIR);
                set(x+w-2, y+dy, z+dz, DungeonBlocks.AIR);
            }
            set(x+1,   y+5, z+dz, DungeonBlocks.LANTERN);
            set(x+w-2, y+5, z+dz, DungeonBlocks.LANTERN);
        }

        // 보스 스폰 단상 (중앙, 2단)
        int cx = x+w/2, cz = z+l/2;
        for (int dx = -4; dx <= 4; dx++)
            for (int dz = -4; dz <= 4; dz++)
                set(cx+dx, y+1, cz+dz, DungeonBlocks.BOSS_FLOOR);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                set(cx+dx, y+2, cz+dz, DungeonBlocks.BOSS_FLOOR);
        // 단상 테두리
        for (int dx = -4; dx <= 4; dx++) {
            set(cx+dx, y+2, cz-4, DungeonBlocks.BOSS_ACCENT);
            set(cx+dx, y+2, cz+4, DungeonBlocks.BOSS_ACCENT);
        }
        for (int dz = -4; dz <= 4; dz++) {
            set(cx-4, y+2, cz+dz, DungeonBlocks.BOSS_ACCENT);
            set(cx+4, y+2, cz+dz, DungeonBlocks.BOSS_ACCENT);
        }

        // 단상 주변 용암 해자
        for (int dx = -6; dx <= 6; dx++)
            for (int dz = -6; dz <= 6; dz++)
                if ((Math.abs(dx) >= 5 || Math.abs(dz) >= 5) && Math.abs(dx) <= 6 && Math.abs(dz) <= 6)
                    set(cx+dx, y, cz+dz, DungeonBlocks.LAVA);

        // 천장 대형 샤워룸라이트
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                set(cx+dx, y+h, cz+dz, DungeonBlocks.SHROOMLIGHT);

        // ── 기믹 데이터 등록 ──────────────────────────────────────
        // 입구 철창 문 (처음엔 열려있다가 보스 처치 후 상자쪽 문 열림)
        List<BlockPos> bossDoors = new ArrayList<>();
        // 보상 상자 뒤쪽 문
        for (int dy = 1; dy <= 5; dy++)
            for (int dx = -2; dx <= 2; dx++)
                bossDoors.add(new BlockPos(x + w/2 + dx, y+dy, z+l-2));
        DungeonGimmickHandler.BOSS_DOORS = bossDoors;

        // 보상 상자 (철창으로 막혀있음)
        List<BlockPos> chests = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            BlockPos chestPos = new BlockPos(x+4+i*3, y+2, z+l-5);
            set(chestPos.getX(), chestPos.getY(), chestPos.getZ(), DungeonBlocks.CHEST);
            // 철창으로 막기
            set(chestPos.getX(), chestPos.getY()+1, chestPos.getZ(), DungeonBlocks.IRON_BARS);
            chests.add(chestPos);
        }
        DungeonGimmickHandler.BOSS_CHESTS = chests;

        // 귀환 포탈 위치 (보상 상자 옆)
        DungeonGimmickHandler.PORTAL_POS = new BlockPos(x+w/2, y+2, z+l-8);

        // 입구 (복도에서 들어오는 쪽) 개방
        openDoorway(x + w/2, y, z, 5);

        // 보상 구역 기본 철창으로 막기
        for (int dy = 1; dy <= 5; dy++)
            for (int dx = -2; dx <= 2; dx++)
                set(x + w/2 + dx, y+dy, z+l-2, DungeonBlocks.IRON_BARS);
    }

    // ── 공통 방 껍데기 ────────────────────────────────────────────
    private void buildRoomShell(int x, int y, int z, int w, int h, int l,
                                 BlockState floor, BlockState wall, BlockState ceil) {
        for (int dx = 0; dx < w; dx++)
            for (int dz = 0; dz < l; dz++) {
                set(x+dx, y, z+dz, floor);
                set(x+dx, y+h, z+dz, ceil);
            }
        for (int dy = 1; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                set(x+dx, y+dy, z,     wall);
                set(x+dx, y+dy, z+l-1, wall);
            }
            for (int dz = 0; dz < l; dz++) {
                set(x,     y+dy, z+dz, wall);
                set(x+w-1, y+dy, z+dz, wall);
            }
        }
        // 내부 공기
        for (int dx = 1; dx < w-1; dx++)
            for (int dz = 1; dz < l-1; dz++)
                for (int dy = 1; dy < h; dy++)
                    set(x+dx, y+dy, z+dz, DungeonBlocks.AIR);
    }

    // ── 기둥 헬퍼 ─────────────────────────────────────────────────
    private void buildPillar(int x, int y, int z, int height) {
        for (int dy = 0; dy <= height; dy++)
            set(x, y+dy, z, dy == 0 || dy == height ? DungeonBlocks.PILLAR_TOP : DungeonBlocks.PILLAR);
    }

    private void buildGrandPillar(int x, int y, int z, int height) {
        for (int dy = 0; dy <= height; dy++) {
            BlockState s = (dy == 0 || dy == height) ? DungeonBlocks.WALL_CHISELED
                : (dy % 3 == 0) ? DungeonBlocks.ACCENT_POLY : DungeonBlocks.PILLAR;
            set(x,   y+dy, z,   s);
            set(x+1, y+dy, z,   DungeonBlocks.PILLAR);
            set(x,   y+dy, z+1, DungeonBlocks.PILLAR);
            set(x+1, y+dy, z+1, DungeonBlocks.PILLAR);
        }
    }

    private void buildBossPillar(int x, int y, int z, int height) {
        for (int dy = 0; dy <= height; dy++) {
            BlockState s = (dy % 3 == 0) ? DungeonBlocks.BOSS_ACCENT : DungeonBlocks.BOSS_PILLAR;
            set(x, y+dy, z, s);
        }
        // 기둥 밑 울음 흑요석 강조
        set(x, y+1, z, DungeonBlocks.BOSS_ACCENT);
        set(x, y+height-1, z, DungeonBlocks.BOSS_ACCENT);
        // 랜턴
        set(x, y+height-2, z+1, DungeonBlocks.LANTERN);
    }

    private void set(int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 3);
    }
}
