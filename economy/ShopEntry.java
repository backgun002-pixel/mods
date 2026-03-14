package com.example.cosmod.economy;

import net.minecraft.world.item.Item;

/**
 * 상점에 등록된 아이템 하나의 가격 정보
 *
 * 수요/공급 가격 변동 공식:
 *   현재가 = basePrice * (1 + demandFactor * (totalSold - totalBought) / 100)
 *   - totalSold   : 플레이어들이 NPC에게 판매한 누적 수
 *   - totalBought : 플레이어들이 NPC에게 구매한 누적 수
 *   - demandFactor: 가격 민감도 (0.0 ~ 1.0)
 */
public class ShopEntry {

    public enum Category { CROP, ORE, COSME, WEAPON, ARMOR }

    private final Item item;
    private final Category category;
    private final int basePrice;        // 기준 가격 (코인)
    private final float demandFactor;   // 가격 민감도
    private int totalSold;              // 누적 판매량 (플레이어→NPC)
    private int totalBought;            // 누적 구매량 (NPC→플레이어)

    public ShopEntry(Item item, Category category, int basePrice, float demandFactor) {
        this.item         = item;
        this.category     = category;
        this.basePrice    = basePrice;
        this.demandFactor = demandFactor;
        this.totalSold    = 0;
        this.totalBought  = 0;
    }

    // ── 현재 판매가 (플레이어가 NPC에게 파는 가격) ──────────────
    public int getSellPrice() {
        int price = (int)(basePrice * (1.0f + demandFactor * (totalSold - totalBought) / 100.0f));
        return Math.max(1, price); // 최소 1코인
    }

    // ── 현재 구매가 (플레이어가 NPC에게 사는 가격) = 판매가 * 1.3 ──
    public int getBuyPrice() {
        return (int)(getSellPrice() * 1.3f);
    }

    // ── 거래 처리 ────────────────────────────────────────────────
    public void recordSell(int amount)  { totalSold    += amount; }
    public void recordBuy(int amount)   { totalBought  += amount; }

    // ── Getters ──────────────────────────────────────────────────
    public Item     getItem()        { return item;        }
    public Category getCategory()    { return category;    }
    public int      getBasePrice()   { return basePrice;   }
    public int      getTotalSold()   { return totalSold;   }
    public int      getTotalBought() { return totalBought; }

    // ── 저장/불러오기용 ───────────────────────────────────────────
    public void setTotalSold(int v)   { this.totalSold   = v; }
    public void setTotalBought(int v) { this.totalBought = v; }
}
