package com.example.cosmod.dungeon;

import com.example.cosmod.dungeon.entity.DungeonGuardianEntity;
import com.example.cosmod.dungeon.entity.EliteStoneGuardEntity;
import com.example.cosmod.dungeon.entity.StoneGolemEntity;
import com.example.cosmod.dungeon.entity.StoneGuardEntity;
import com.example.cosmod.item.CosmodItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 던전 드롭 테이블
 * - 일반 몬스터: 해당 위치에 아이템 드롭
 * - 보스: 처치 플레이어 인벤토리에 직접 지급 + 채팅 표시
 */
public class DungeonDropTable {

    private static final Random rng = new Random();

    // ── 드롭 엔트리 ───────────────────────────────────────────────
    public record DropEntry(Item item, int minAmt, int maxAmt, int weight) {}

    // ── 석상병사 드롭 ─────────────────────────────────────────────
    private static final List<DropEntry> STONE_GUARD_DROPS = List.of(
        new DropEntry(Items.STONE,       1, 3,  40),
        new DropEntry(Items.COBBLESTONE, 1, 4,  35),
        new DropEntry(CosmodItems.RUBY,  1, 1,  15),
        new DropEntry(Items.IRON_INGOT,  1, 2,  10)
    );

    // ── 석상골렘 드롭 ─────────────────────────────────────────────
    private static final List<DropEntry> STONE_GOLEM_DROPS = List.of(
        new DropEntry(Items.COBBLESTONE, 2, 5,  35),
        new DropEntry(Items.STONE,       2, 4,  30),
        new DropEntry(CosmodItems.RUBY,  1, 2,  20),
        new DropEntry(Items.IRON_INGOT,  1, 3,  15)
    );

    // ── 정예 석상병사 드롭 ────────────────────────────────────────
    private static final List<DropEntry> ELITE_GUARD_DROPS = List.of(
        new DropEntry(CosmodItems.RUBY,      2, 4,  40),
        new DropEntry(CosmodItems.SAPPHIRE,  1, 2,  30),
        new DropEntry(Items.IRON_INGOT,      2, 5,  20),
        new DropEntry(Items.GOLD_INGOT,      1, 2,  10)
    );

    // ── 보스 드롭 (직접 지급) ─────────────────────────────────────
    private static final List<DropEntry> BOSS_DROPS = List.of(
        new DropEntry(CosmodItems.RED_DIAMOND, 1, 3, 100),  // 확정
        new DropEntry(CosmodItems.SAPPHIRE,    3, 6, 100),  // 확정
        new DropEntry(CosmodItems.RUBY,        5, 10, 100), // 확정
        new DropEntry(Items.DIAMOND,           2, 4,  60),  // 60%
        new DropEntry(Items.NETHERITE_SCRAP,   1, 2,  25)   // 25%
    );

    // ── 일반 몬스터 드롭 처리 ─────────────────────────────────────
    public static void onMonsterDrop(Monster entity, ServerLevel level) {
        List<DropEntry> table;
        if      (entity instanceof StoneGuardEntity)     table = STONE_GUARD_DROPS;
        else if (entity instanceof StoneGolemEntity)     table = STONE_GOLEM_DROPS;
        else if (entity instanceof EliteStoneGuardEntity) table = ELITE_GUARD_DROPS;
        else return;

        List<ItemStack> drops = rollDrops(table);
        for (ItemStack stack : drops) {
            ItemEntity ie = new ItemEntity(level,
                entity.getX(), entity.getY() + 0.5, entity.getZ(), stack);
            ie.setDefaultPickUpDelay();
            level.addFreshEntity(ie);
        }
    }

    // ── 보스 드롭: 처치 플레이어 인벤토리 직접 지급 ─────────────
    public static void onBossDrop(ServerPlayer killer, ServerLevel level) {
        List<ItemStack> drops = new ArrayList<>();

        for (DropEntry entry : BOSS_DROPS) {
            // weight가 100 미만이면 확률 체크
            if (entry.weight() < 100 && rng.nextInt(100) >= entry.weight()) continue;
            int amt = entry.minAmt() + rng.nextInt(entry.maxAmt() - entry.minAmt() + 1);
            drops.add(new ItemStack(entry.item(), amt));
        }

        // 인벤토리에 직접 지급
        List<String> acquired = new ArrayList<>();
        for (ItemStack stack : drops) {
            boolean added = killer.getInventory().add(stack);
            if (!added) {
                // 인벤토리 가득 차면 발 아래 드롭
                ItemEntity ie = new ItemEntity(level,
                    killer.getX(), killer.getY(), killer.getZ(), stack);
                level.addFreshEntity(ie);
            }
            String name = stack.getHoverName().getString();
            acquired.add("§e" + name + " §fx" + stack.getCount());
        }

        // 채팅창에 획득 아이템 표시
        killer.displayClientMessage(
            Component.literal("§6§l[보스 보상] §f획득한 아이템:"), false);
        for (String line : acquired) {
            killer.displayClientMessage(Component.literal("  §7▶ " + line), false);
        }
    }

    // ── 드롭 롤 ──────────────────────────────────────────────────
    private static List<ItemStack> rollDrops(List<DropEntry> table) {
        List<ItemStack> result = new ArrayList<>();
        int totalWeight = table.stream().mapToInt(DropEntry::weight).sum();
        // 1~2개 드롭
        int rolls = 1 + rng.nextInt(2);
        for (int i = 0; i < rolls; i++) {
            int r = rng.nextInt(totalWeight);
            int cum = 0;
            for (DropEntry entry : table) {
                cum += entry.weight();
                if (r < cum) {
                    int amt = entry.minAmt() + rng.nextInt(entry.maxAmt() - entry.minAmt() + 1);
                    result.add(new ItemStack(entry.item(), amt));
                    break;
                }
            }
        }
        return result;
    }
}
