package com.nameless.indestructible.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

import java.util.List;

@Mixin(CombatBehaviors.BehaviorSeries.class)
public interface BehaviorSeriesMixin {

    @Accessor(value = "behaviors", remap = false)
    List<CombatBehaviors.Behavior<?>> getBehaviors();

    @Accessor(value = "nextBehaviorPointer", remap = false)
    int getNextBehaviorPointer();

    @Accessor(value = "nextBehaviorPointer", remap = false)
    void setNextBehaviorPointer(int nextBehaviorPointer);

    @Accessor(value = "loopFinished", remap = false)
    boolean getLoopFinished();

    @Accessor(value = "loopFinished", remap = false)
    void setLoopFinished(boolean finished);

    @Accessor(value = "looping", remap = false)
    boolean getLooping();
}
