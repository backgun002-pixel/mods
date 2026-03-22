package com.example.cosmod.client.renderer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import com.example.cosmod.job.JobNpcEntity;
import com.example.cosmod.CosmodMod;

public class JobNpcRenderer extends EntityRenderer<JobNpcEntity, JobNpcRenderState> {

    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(CosmodMod.MOD_ID, "job_npc"), "main");

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");

    private final JobNpcModel model;

    public JobNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new JobNpcModel(ctx.bakeLayer(LAYER));
    }

    @Override
    public JobNpcRenderState createRenderState() {
        return new JobNpcRenderState();
    }

    public Identifier getTextureLocation(JobNpcRenderState state) {
        return TEXTURE;
    }
}
