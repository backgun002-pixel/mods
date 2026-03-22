package com.example.cosmod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

public class DungeonEntityRenderer<T extends LivingEntity>
        extends EntityRenderer<T, EntityRenderState> {

    private final Identifier texture;

    public DungeonEntityRenderer(EntityRendererProvider.Context ctx, Identifier texture) {
        super(ctx);
        this.texture = texture;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    // @Override 제거 - 1.21.11에서는 슈퍼클래스에 해당 메서드 없음
    public Identifier getTextureLocation(EntityRenderState state) {
        return texture;
    }

    public static <T extends LivingEntity> DungeonEntityRenderer<T> stoneGuard(
            EntityRendererProvider.Context ctx) {
        return new DungeonEntityRenderer<>(ctx,
            Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png"));
    }

    public static <T extends LivingEntity> DungeonEntityRenderer<T> stoneGolem(
            EntityRendererProvider.Context ctx) {
        return new DungeonEntityRenderer<>(ctx,
            Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png"));
    }

    public static <T extends LivingEntity> DungeonEntityRenderer<T> eliteStoneGuard(
            EntityRendererProvider.Context ctx) {
        return new DungeonEntityRenderer<>(ctx,
            Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png"));
    }

    public static <T extends LivingEntity> DungeonEntityRenderer<T> dungeonGuardian(
            EntityRendererProvider.Context ctx) {
        return new DungeonEntityRenderer<>(ctx,
            Identifier.withDefaultNamespace("textures/entity/guardian_elder.png"));
    }
}
