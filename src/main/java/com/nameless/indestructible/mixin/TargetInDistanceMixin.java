package com.nameless.indestructible.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.data.conditions.entity.TargetInDistance;

@Mixin(TargetInDistance.class)
public interface TargetInDistanceMixin {
    @Accessor(value = "max", remap = false)
    double getMax();
}