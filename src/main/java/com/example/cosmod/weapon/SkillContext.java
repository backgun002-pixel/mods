package com.example.cosmod.weapon;

import net.minecraft.world.entity.LivingEntity;

public class SkillContext {
    private final LivingEntity target;

    public SkillContext(LivingEntity target) { this.target = target; }

    public LivingEntity getTarget() { return target; }
    public boolean hasTarget() { return target != null; }

    public static final SkillContext EMPTY = new SkillContext(null);
}
