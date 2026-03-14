package com.example.cosmod.dungeon;

import com.example.cosmod.CosmodMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DungeonManager {

    public static final int DUNGEON_X = 100000;
    public static final int DUNGEON_Y = 50;
    public static final int DUNGEON_Z = 100000;

    private static boolean generated = false;

    // 플레이어별 귀환 위치 (입장권 사용 위치 저장)
    private static final Map<UUID, ReturnPos> RETURN_POSITIONS = new HashMap<>();

    public static class ReturnPos {
        public final double x, y, z;
        public final float  yRot, xRot;
        public ReturnPos(double x, double y, double z, float yRot, float xRot) {
            this.x = x; this.y = y; this.z = z;
            this.yRot = yRot; this.xRot = xRot;
        }
    }

    public static void ensureGenerated(ServerLevel level) {
        if (generated) return;
        generated = true;
        BlockPos origin = new BlockPos(DUNGEON_X, DUNGEON_Y, DUNGEON_Z);
        CosmodMod.LOGGER.info("[Cosmod] 던전 생성 시작: " + origin);
        new DungeonBuilder(level, origin).build();
        CosmodMod.LOGGER.info("[Cosmod] 던전 생성 완료");
    }

    /** 입장권 사용 시 호출 - 현재 위치 저장 후 던전으로 텔레포트 */
    public static void teleportToDungeon(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        ServerLevel overworld  = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        ensureGenerated(overworld);

        // 현재 위치 저장 (차원 정보 포함해서 저장하려면 더 필요하지만
        // 우선 오버월드 내 던전이므로 좌표만 저장)
        RETURN_POSITIONS.put(player.getUUID(),
            new ReturnPos(
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()));

        player.teleportTo(overworld,
            DUNGEON_X + 0.5, DUNGEON_Y + 2, DUNGEON_Z + 2.5,
            Set.of(), player.getYRot(), player.getXRot(), false);

        player.displayClientMessage(
            Component.literal("§8[던전] §f지하 석굴 던전에 입장했습니다."), false);
    }

    /** 귀환 포탈 밟을 때 호출 - 입장권 사용 위치로 귀환 */
    public static void teleportBack(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        ServerLevel overworld  = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        // 연속 귀환 방지: 던전 밖에 이미 있으면 무시
        double dx = player.getX() - DUNGEON_X;
        double dz = player.getZ() - DUNGEON_Z;
        if (dx * dx + dz * dz > 300 * 300) return;

        ReturnPos pos = RETURN_POSITIONS.remove(player.getUUID());

        if (pos != null) {
            player.teleportTo(overworld,
                pos.x, pos.y, pos.z, Set.of(), pos.yRot, pos.xRot, false);
            player.displayClientMessage(
                Component.literal("§a[던전] 입장 전 위치로 귀환했습니다."), false);
        } else {
            // 저장 위치 없으면 0,65,0 스폰으로
            player.teleportTo(overworld,
                0.5, 65, 0.5,
                Set.of(), player.getYRot(), player.getXRot(), false);
            player.displayClientMessage(
                Component.literal("§a[던전] 스폰으로 귀환했습니다."), false);
        }
    }

    public static void resetDungeon() {
        DungeonGimmickHandler.BOSS_DEFEATED = false;
        DungeonGimmickHandler.BOSS_SPAWNED = false;

        // 방 상태만 초기화 (리스트는 던전 생성 시 1회 등록되므로 그대로 유지)
        for (DungeonGimmickHandler.RoomData r : DungeonGimmickHandler.COMBAT_ROOMS) {
            r.cleared = false;
            r.doorsClosed = false;
        }

        for (DungeonGimmickHandler.RoomData r : DungeonGimmickHandler.MINIBOSS_ROOMS) {
            r.cleared = false;
            r.doorsClosed = false;
        }
    }

    /** 서버에서 포탈 블록 제거 (리셋 시 호출) */
    public static void clearPortal(ServerLevel level) {
        BlockPos center = DungeonGimmickHandler.PORTAL_POS;
        if (center == null) return;
        // 흑요석 프레임 + 네더라이트 발판 전체 제거
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = 0; dy <= 4; dy++)
                level.setBlock(center.offset(dx, dy, 0), Blocks.AIR.defaultBlockState(), 3);
    }
}
