package com.example.cosmod.client.renderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class JobNpcModel extends EntityModel<JobNpcRenderState> {

    private final ModelPart root;

    public JobNpcModel(ModelPart root) {
        super(root);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 머리
        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -8, -4, 8, 8, 8),
            PartPose.offset(0, 0, 0));

        // 몸통
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4, 0, -2, 8, 12, 4),
            PartPose.offset(0, 0, 0));

        // 왼팔
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(32, 48)
                .addBox(-1, -2, -2, 4, 12, 4),
            PartPose.offset(5, 2, 0));

        // 오른팔
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(40, 16)
                .addBox(-3, -2, -2, 4, 12, 4),
            PartPose.offset(-5, 2, 0));

        // 왼다리
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(16, 48)
                .addBox(-2, 0, -2, 4, 12, 4),
            PartPose.offset(2, 12, 0));

        // 오른다리
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, 0, -2, 4, 12, 4),
            PartPose.offset(-2, 12, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(JobNpcRenderState state) {}
}
