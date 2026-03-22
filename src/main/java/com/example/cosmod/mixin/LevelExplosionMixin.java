package com.example.cosmod.mixin;

import com.example.cosmod.dungeon.DungeonManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelExplosionMixin {

    /**
     * Level.explode() 호출 시 던전 영역(±200) 내부이면
     * interaction을 NONE으로 바꿔 재호출합니다.
     * → 블록 파괴 없음, 불 생성 없음, 엔티티 피해는 그대로.
     */
    @Inject(
        method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cosmod$blockDungeonExplosion(
            Entity source,
            DamageSource damageSource,
            ExplosionDamageCalculator calculator,
            double x, double y, double z,
            float radius,
            boolean fire,
            Level.ExplosionInteraction interaction,
            CallbackInfo ci) {

        double dx = Math.abs(x - DungeonManager.DUNGEON_X);
        double dz = Math.abs(z - DungeonManager.DUNGEON_Z);

        if (dx < 200 && dz < 200
                && interaction != Level.ExplosionInteraction.NONE) {
            ci.cancel();
            Level self = (Level)(Object)this;
            // NONE으로 재호출 (블록 파괴·불 없이 폭발 효과만)
            self.explode(source, damageSource, calculator,
                x, y, z, radius, false,
                Level.ExplosionInteraction.NONE);
        }
    }
}
