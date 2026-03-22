package com.example.cosmod.inventory;

/**
 * PlayerEntity 에 코디 인벤토리를 주입하기 위한 인터페이스
 * Mixin에서 implements 로 구현합니다.
 */
public interface CosmeticInventoryHolder {
    CosmeticInventory cosmod$getCosmeticInventory();
}
