package com.example.cosmod.client.renderer;

import com.example.cosmod.dungeon.entity.DungeonGuardianEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class DungeonGuardianRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<DungeonGuardianEntity, R> {

    public DungeonGuardianRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DungeonGuardianModel());
    }

    @Override
    protected AABB getBoundingBoxForCulling(DungeonGuardianEntity entity) {
        return AABB.ofSize(entity.position(), 10, 25, 10).move(0, 5, 0);
    }
}