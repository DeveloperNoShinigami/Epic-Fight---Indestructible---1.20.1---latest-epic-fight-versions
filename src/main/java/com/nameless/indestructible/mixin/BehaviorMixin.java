package com.nameless.indestructible.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

import java.util.List;

@Mixin(CombatBehaviors.Behavior.class)
public interface BehaviorMixin<T> {
    @Accessor(value = "conditions", remap = false)
    List<Condition<T>> getConditions();
}