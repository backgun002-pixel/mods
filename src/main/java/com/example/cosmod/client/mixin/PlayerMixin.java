package com.example.cosmod.client.mixin;

import com.example.cosmod.inventory.CosmeticInventory;
import com.example.cosmod.inventory.CosmeticInventoryHolder;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * PlayerEntity 에 CosmeticInventory 를 주입합니다.
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements CosmeticInventoryHolder {

    @Unique
    private final CosmeticInventory cosmod$cosmeticInventory = new CosmeticInventory();

    @Override
    public CosmeticInventory cosmod$getCosmeticInventory() {
        return cosmod$cosmeticInventory;
    }
}
