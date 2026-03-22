package com.example.cosmod.client.renderer;

import com.example.cosmod.CosmodMod;
import com.example.cosmod.dungeon.entity.DungeonGuardianEntity;
import net.minecraft.resources.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class DungeonGuardianModel extends GeoModel<DungeonGuardianEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID,
                "entity/dungeon_guardian");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID,
                "textures/entity/dungeon_guardian.png");
    }

    @Override
    public Identifier getAnimationResource(DungeonGuardianEntity entity) {
        return Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID,
                "entity/dungeon_guardian");
    }
}
