package com.example.cosmod.client.renderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * 정예 석상병사 - 일반 병사 2배 크기
 * 텍스처: 128x64 (UV 범위 초과 없음)
 */
public class EliteStoneGuardModel extends EntityModel<LivingEntityRenderState> {

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftShoulder;
    private final ModelPart rightShoulder;

    public EliteStoneGuardModel(ModelPart root) {
        super(root);
        this.root          = root;
        this.head          = root.getChild("head");
        this.body          = root.getChild("body");
        this.leftArm       = root.getChild("left_arm");
        this.rightArm      = root.getChild("right_arm");
        this.leftLeg       = root.getChild("left_leg");
        this.rightLeg      = root.getChild("right_leg");
        this.leftShoulder  = root.getChild("left_shoulder");
        this.rightShoulder = root.getChild("right_shoulder");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 128x64 텍스처 기준 - 모든 UV가 범위 안에 들어오도록
        // 머리 (투구 포함) - 16x16x16 큐브 → UV(0~64)
        root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8, -16, -8, 16, 16, 16),
            PartPose.offset(0, -4, 0));

        // 몸통 - UV(0~48)
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 32).addBox(-8, 0, -4, 16, 20, 8),
            PartPose.offset(0, -4, 0));

        // 왼팔
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(64, 0).addBox(-1, -2, -4, 8, 18, 8),
            PartPose.offset(9, -2, 0));

        // 오른팔
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(64, 26).addBox(-7, -2, -4, 8, 18, 8),
            PartPose.offset(-9, -2, 0));

        // 왼어깨
        root.addOrReplaceChild("left_shoulder",
            CubeListBuilder.create()
                .texOffs(96, 0).addBox(-1, -4, -4, 10, 4, 8),
            PartPose.offset(8, -3, 0));

        // 오른어깨
        root.addOrReplaceChild("right_shoulder",
            CubeListBuilder.create()
                .texOffs(96, 12).addBox(-9, -4, -4, 10, 4, 8),
            PartPose.offset(-8, -3, 0));

        // 왼다리
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(0, 52).addBox(-4, 0, -4, 8, 18, 8),
            PartPose.offset(4, 16, 0));

        // 오른다리
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(32, 52).addBox(-4, 0, -4, 8, 18, 8),
            PartPose.offset(-4, 16, 0));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        float t = state.ageInTicks;
        head.yRot          = (float) Math.sin(t * 0.05f) * 0.15f;
        leftArm.xRot       =  (float) Math.sin(t * 0.1f) * 0.4f;
        rightArm.xRot      = -(float) Math.sin(t * 0.1f) * 0.4f;
        leftLeg.xRot       = -(float) Math.sin(t * 0.1f) * 0.4f;
        rightLeg.xRot      =  (float) Math.sin(t * 0.1f) * 0.4f;
        leftShoulder.xRot  = leftArm.xRot;
        rightShoulder.xRot = rightArm.xRot;
    }
}
