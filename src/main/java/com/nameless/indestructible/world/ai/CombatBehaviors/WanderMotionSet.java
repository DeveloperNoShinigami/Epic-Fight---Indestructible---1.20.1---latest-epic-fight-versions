package com.nameless.indestructible.world.ai.CombatBehaviors;

/**
 * Compatibility shim: TCRCore references this class when calling actStrafing on
 * IAdvancedCapability. The four constructor parameters match the original API:
 *   WanderMotionSet(int time, int distance, float forward, float clockwise)
 *
 * The actual strafing logic lives in AdvancedCustomHumanoidMobPatch.
 * This is only a data-carrier so TCRCore mixins can compile and run.
 */
public class WanderMotionSet {
    public final int time;
    public final int distance;
    public final float forward;
    public final float clockwise;

    public WanderMotionSet(int time, int distance, float forward, float clockwise) {
        this.time = time;
        this.distance = distance;
        this.forward = forward;
        this.clockwise = clockwise;
    }
}
