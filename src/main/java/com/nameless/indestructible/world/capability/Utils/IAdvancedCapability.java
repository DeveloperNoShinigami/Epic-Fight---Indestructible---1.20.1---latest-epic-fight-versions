package com.nameless.indestructible.world.capability.Utils;

import com.nameless.indestructible.world.ai.CombatBehaviors.WanderMotionSet;

/**
 * Compatibility shim: TCRCore mixins (EFXAnimationUtilsMixin, AdvancedCombatGoalMixin)
 * do instanceof checks against this interface to call getAttackSpeed() and actStrafing().
 *
 * Implemented by AdvancedCustomHumanoidMobPatch, which holds all real logic.
 * This interface is only a thin contract — nothing is duplicated here.
 */
public interface IAdvancedCapability {
    /**
     * Returns the entity's current attack speed multiplier.
     */
    float getAttackSpeed();

    /**
     * Triggers a strafe motion using the parameters inside motionSet.
     * Delegates to setStrafingTime / setStrafingDirection on the patch.
     */
    void actStrafing(WanderMotionSet motionSet);
}
