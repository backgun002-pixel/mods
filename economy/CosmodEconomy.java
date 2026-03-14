package com.example.cosmod.economy;

import com.example.cosmod.CosmodMod;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import com.example.cosmod.combat.GearItem;
import net.minecraft.world.item.ItemStack;

public class CosmodEconomy {

    // ── 인벤토리 순회 헬퍼 (1.21.11: items 직접 접근 불가) ────────
    // Inventory는 Iterable<ItemStack> 이므로 for-each 사용 가능
    private static Iterable<ItemStack> allItems(Player player) {
        Inventory inv = player.getInventory();
        // Inventory가 Iterable을 구현하므로 직접 반복 가능
        return inv;
    }

    // ── 코인 잔액 ────────────────────────────────────────────────
    public static int getCoins(Player player) {
        int total = 0;
        for (ItemStack stack : allItems(player)) {
            if (!stack.isEmpty() && stack.getItem() instanceof CoinItem) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static void giveCoins(Player player, int amount) {
        player.getInventory().add(new ItemStack(CosmodEconomyItems.COSMO_COIN, amount));
    }

    public static boolean takeCoins(Player player, int amount) {
        if (getCoins(player) < amount) return false;
        int remaining = amount;
        for (ItemStack stack : allItems(player)) {
            if (remaining <= 0) break;
            if (!stack.isEmpty() && stack.getItem() instanceof CoinItem) {
                int take = Math.min(stack.getCount(), remaining);
                stack.shrink(take);
                remaining -= take;
            }
        }
        return true;
    }

    // ── 트랜잭션 ─────────────────────────────────────────────────
    public enum TradeResult { SUCCESS, NOT_FOUND, INSUFFICIENT_ITEMS, INSUFFICIENT_COINS }

    public static TradeResult sell(Player player, net.minecraft.world.item.Item item, int amount) {
        ShopEntry entry = ShopRegistry.findByItem(item);
        if (entry == null) return TradeResult.NOT_FOUND;
        if (entry.getCategory() == ShopEntry.Category.COSME) return TradeResult.NOT_FOUND;
        if (countItem(player, item) < amount) return TradeResult.INSUFFICIENT_ITEMS;

        removeItem(player, item, amount);
        giveCoins(player, entry.getSellPrice() * amount);
        entry.recordSell(amount);
        return TradeResult.SUCCESS;
    }

    public static TradeResult buy(Player player, net.minecraft.world.item.Item item, int amount) {
        ShopEntry entry = ShopRegistry.findByItem(item);
        if (entry == null) return TradeResult.NOT_FOUND;
        int cost = entry.getBuyPrice() * amount;
        if (!takeCoins(player, cost)) return TradeResult.INSUFFICIENT_COINS;

        // GearItem이면 랜덤 옵션 붙여서 지급 (1개씩)
        if (item instanceof GearItem gearItem) {
            for (int i = 0; i < amount; i++) {
                player.getInventory().add(GearItem.createWithOptions(gearItem));
            }
        } else {
            player.getInventory().add(new ItemStack(item, amount));
        }
        entry.recordBuy(amount);
        return TradeResult.SUCCESS;
    }

    // ── 유틸 ─────────────────────────────────────────────────────
    private static int countItem(Player player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack s : allItems(player)) {
            if (!s.isEmpty() && s.getItem() == item) count += s.getCount();
        }
        return count;
    }

    private static void removeItem(Player player, net.minecraft.world.item.Item item, int amount) {
        int remaining = amount;
        for (ItemStack s : allItems(player)) {
            if (remaining <= 0) break;
            if (!s.isEmpty() && s.getItem() == item) {
                int take = Math.min(s.getCount(), remaining);
                s.shrink(take);
                remaining -= take;
            }
        }
    }
}
