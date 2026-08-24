package com.nameless.indestructible.mixin;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * GoalSelector exposes mutable collections to goals. EFI and compatibility
 * patches can legitimately need to replace a combat goal while a goal is
 * running, but doing so during tickRunningGoals invalidates LinkedHashMap's
 * iterator and crashes the server. Defer those mutations until the iterator
 * has completed.
 */
@Mixin(GoalSelector.class)
public abstract class GoalSelectorMutationGuardMixin {
    @Unique
    private boolean indestructible$iteratingRunningGoals;

    @Unique
    private boolean indestructible$drainingMutations;

    @Unique
    private final List<Runnable> indestructible$deferredMutations = new ArrayList<>();

    @Inject(method = "tickRunningGoals", at = @At("HEAD"))
    private void indestructible$beginGoalIteration(boolean tickAll, CallbackInfo ci) {
        this.indestructible$iteratingRunningGoals = true;
    }

    @Inject(method = "tickRunningGoals", at = @At("RETURN"))
    private void indestructible$finishGoalIteration(boolean tickAll, CallbackInfo ci) {
        this.indestructible$iteratingRunningGoals = false;
        if (this.indestructible$deferredMutations.isEmpty()) {
            return;
        }

        List<Runnable> pending = new ArrayList<>(this.indestructible$deferredMutations);
        this.indestructible$deferredMutations.clear();
        this.indestructible$drainingMutations = true;
        try {
            pending.forEach(Runnable::run);
        } finally {
            this.indestructible$drainingMutations = false;
        }
    }

    @Inject(method = "addGoal", at = @At("HEAD"), cancellable = true)
    private void indestructible$deferAddGoal(int priority, Goal goal, CallbackInfo ci) {
        if (this.indestructible$iteratingRunningGoals && !this.indestructible$drainingMutations) {
            this.indestructible$deferredMutations.add(
                    () -> ((GoalSelector) (Object) this).addGoal(priority, goal));
            ci.cancel();
        }
    }

    @Inject(method = "removeGoal", at = @At("HEAD"), cancellable = true)
    private void indestructible$deferRemoveGoal(Goal goal, CallbackInfo ci) {
        if (this.indestructible$iteratingRunningGoals && !this.indestructible$drainingMutations) {
            this.indestructible$deferredMutations.add(
                    () -> ((GoalSelector) (Object) this).removeGoal(goal));
            ci.cancel();
        }
    }

    @Inject(method = "removeAllGoals", at = @At("HEAD"), cancellable = true)
    private void indestructible$deferRemoveAllGoals(java.util.function.Predicate<Goal> predicate, CallbackInfo ci) {
        if (this.indestructible$iteratingRunningGoals && !this.indestructible$drainingMutations) {
            this.indestructible$deferredMutations.add(
                    () -> ((GoalSelector) (Object) this).removeAllGoals(predicate));
            ci.cancel();
        }
    }
}
