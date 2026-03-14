package com.example.cosmod.dungeon;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 던전에서 사용하는 블록 팔레트
 * 지하 석굴 테마: Deepslate + Soul Lantern
 */
public class DungeonBlocks {

    // ── 메인 벽/바닥 ──────────────────────────────────────────────
    public static final BlockState WALL        = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    public static final BlockState WALL_CRACKED= Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    public static final BlockState WALL_CHISELED=Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    public static final BlockState FLOOR       = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    public static final BlockState FLOOR_TILE  = Blocks.DEEPSLATE_TILES.defaultBlockState();
    public static final BlockState CEILING     = Blocks.DEEPSLATE_BRICKS.defaultBlockState();

    // ── 기둥/장식 ─────────────────────────────────────────────────
    public static final BlockState PILLAR      = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    public static final BlockState PILLAR_TOP  = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    public static final BlockState ACCENT      = Blocks.BLACKSTONE.defaultBlockState();
    public static final BlockState ACCENT_POLY = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    public static final BlockState GILDED      = Blocks.GILDED_BLACKSTONE.defaultBlockState();

    // ── 조명 ──────────────────────────────────────────────────────
    public static final BlockState LANTERN     = Blocks.SOUL_LANTERN.defaultBlockState();
    public static final BlockState TORCH       = Blocks.SOUL_TORCH.defaultBlockState();
    public static final BlockState SHROOMLIGHT = Blocks.SHROOMLIGHT.defaultBlockState();

    // ── 보스방 특수 ───────────────────────────────────────────────
    public static final BlockState BOSS_FLOOR  = Blocks.DEEPSLATE_TILES.defaultBlockState();
    public static final BlockState BOSS_WALL   = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    public static final BlockState BOSS_PILLAR = Blocks.OBSIDIAN.defaultBlockState();
    public static final BlockState BOSS_ACCENT = Blocks.CRYING_OBSIDIAN.defaultBlockState();
    public static final BlockState LAVA        = Blocks.LAVA.defaultBlockState();

    // ── 공기/특수 ─────────────────────────────────────────────────
    public static final BlockState AIR         = Blocks.AIR.defaultBlockState();
    public static final BlockState SPAWNER     = Blocks.SPAWNER.defaultBlockState();
    public static final BlockState CHEST       = Blocks.CHEST.defaultBlockState();
    public static final BlockState IRON_BARS   = Blocks.IRON_BARS.defaultBlockState();
    public static final BlockState COBWEB      = Blocks.COBWEB.defaultBlockState();
}
