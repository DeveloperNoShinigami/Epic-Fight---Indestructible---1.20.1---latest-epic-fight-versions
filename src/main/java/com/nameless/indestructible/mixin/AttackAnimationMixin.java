package com.nameless.indestructible.mixin;

import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.function.BiFunction;

@Mixin(AttackAnimation.class)
public class AttackAnimationMixin {

    /**
     * TEMP FIX: Guard against a null particle receiver in AttackAnimation.spawnHitParticle.
     * Some animations (e.g. AirSlashAnimation used by CustomNPCs) are registered without a
     * hit particle, causing a NullPointerException when spawnParticleWithArgument is called.
     * This silently skips the particle spawn instead of crashing.
     * Remove once Epic Fight properly guards this internally.
     */
    @SuppressWarnings("rawtypes")
    @Redirect(
        method = "spawnHitParticle(Lnet/minecraft/server/level/ServerLevel;Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/Entity;Lyesman/epicfight/api/animation/types/AttackAnimation$Phase;)V",
        at = @At(value = "INVOKE", target = "Lyesman/epicfight/particle/HitParticleType;spawnParticleWithArgument(Lnet/minecraft/server/level/ServerLevel;Ljava/util/function/BiFunction;Ljava/util/function/BiFunction;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V"),
        remap = false
    )
    private void guardNullParticleSpawn(HitParticleType particle, ServerLevel level, BiFunction particleFactory, BiFunction velocityFactory, Entity attacker, Entity target) {
        if (particle != null) {
            particle.spawnParticleWithArgument(level, particleFactory, velocityFactory, attacker, target);
        }
    }

    @Inject(method = "getPlaySpeed(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lyesman/epicfight/api/animation/types/DynamicAnimation;)F", at = @At("RETURN"), cancellable = true, remap = false)
    private void onGetPlaySpeed(LivingEntityPatch<?> entitypatch, DynamicAnimation animation, CallbackInfoReturnable<Float> cir) {
        if(entitypatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch){
            cir.setReturnValue(advancedCustomHumanoidMobPatch.getAttackSpeed());
        }
    }
}
