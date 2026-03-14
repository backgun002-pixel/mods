package com.example.cosmod.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public enum SetBonus {

    JSH("장세현", new Effect(0, 5, 3, 0),  new Effect(0, 15, 0, 2)),
    PJH("박준혁", new Effect(3, 0, 5, 0),  new Effect(10, 5, 10, 0)),
    YYJ("유영진", new Effect(5, 0, 0, 1),  new Effect(15, 0, 5, 2));

    public final String displayName;
    public final Effect bonus2;
    public final Effect bonus4;

    SetBonus(String name, Effect b2, Effect b4) {
        this.displayName = name;
        this.bonus2 = b2;
        this.bonus4 = b4;
    }

    /** 플레이어가 착용 중인 특정 세트 개수 반환 */
    public static int countEquipped(Player player, SetBonus set) {
        int count = 0;
        // 인벤토리 슬롯 36~39 = 방어구 (boots/leggings/chestplate/helmet)
        for (int i = 36; i <= 39; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof GearItem gi && gi.getSetBonus() == set) count++;
        }
        return count;
    }

    /** 착용 수에 따른 활성 보너스 반환 */
    public static Effect getActiveBonus(Player player, SetBonus set) {
        int n = countEquipped(player, set);
        if (n >= 4) return set.bonus4;
        if (n >= 2) return set.bonus2;
        return null;
    }

    public record Effect(
        int atkBonus,
        int defBonus,
        int speedPercent,
        int strengthLevel
    ) {}
}
