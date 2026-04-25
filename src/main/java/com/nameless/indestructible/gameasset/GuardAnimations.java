package com.nameless.indestructible.gameasset;

import com.nameless.indestructible.api.animation.types.CustomGuardAnimation;
import com.nameless.indestructible.main.Indestructible;
import net.minecraftforge.fml.ModList;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

import java.util.Set;

public class GuardAnimations {
    public static StaticAnimation MOB_SWORD_GUARD;
    public static StaticAnimation MOB_LONGSWORD_GUARD;
    public static StaticAnimation MOB_GREATSWORD_GUARD;
    public static StaticAnimation MOB_KATANA_GUARD;
    public static StaticAnimation MOB_KATANA_GUARD1;
    public static StaticAnimation MOB_SPEAR_GUARD;
    public static StaticAnimation MOB_DUAL_SWORD_GUARD;
    public static StaticAnimation MOB_COUNTER_ATTACK;
    public static StaticAnimation MOB_YAMATO_GUARD;
    public static StaticAnimation MOB_AGONY_GUARD;
    public static StaticAnimation MOB_RUINE_GUARD;
    public static StaticAnimation MOB_HERRSCHER_GUARD;
    public static StaticAnimation SHIELD_HIT;
    public static StaticAnimation MOB_SHIELD_GUARD;

    public static void registerAnimations(AnimationRegistryEvent event) {
        event.newBuilder(Indestructible.MOD_ID, GuardAnimations::build);
    }
    private static void build(AnimationBuilder builder) {
        HumanoidArmature biped = Armatures.BIPED.get();

        MOB_SWORD_GUARD = builder.nextAccessor("guard/guard_sword", accessor -> new CustomGuardAnimation(accessor,
                "epicfight:biped/skill/guard_sword_hit", Armatures.BIPED)).get();
        MOB_LONGSWORD_GUARD = builder.nextAccessor("guard/guard_longsword", accessor -> new CustomGuardAnimation(accessor,
                "epicfight:biped/skill/guard_longsword_hit", Armatures.BIPED)).get();
        MOB_GREATSWORD_GUARD = builder.nextAccessor("guard/guard_greatsword", accessor -> new CustomGuardAnimation(accessor,
                "epicfight:biped/skill/guard_greatsword_hit", Armatures.BIPED)).get();
        MOB_KATANA_GUARD = builder.nextAccessor("guard/guard_uchigatana", accessor -> new CustomGuardAnimation(accessor,
                "epicfight:biped/skill/guard_sword_hit", Armatures.BIPED)).get();
        MOB_KATANA_GUARD1 = builder.nextAccessor("guard/guard_katana", accessor -> new CustomGuardAnimation(accessor,
                "epicfight:biped/skill/guard_sword_hit", Armatures.BIPED)).get();
        MOB_SPEAR_GUARD = builder.nextAccessor("guard/guard_spear", accessor -> new CustomGuardAnimation(accessor,
                "epicfight:biped/skill/guard_spear_hit", Armatures.BIPED)).get();
        MOB_DUAL_SWORD_GUARD = builder.nextAccessor("guard/guard_dualsword", accessor -> new CustomGuardAnimation(accessor,
                "epicfight:biped/skill/guard_dualsword_hit", Armatures.BIPED)).get();
        MOB_COUNTER_ATTACK = builder.<AttackAnimation>nextAccessor("guard/counter", accessor -> new AttackAnimation(0.3F, 0.08F, 0.1F, 0.15F, 0.525F, ColliderPreset.FIST, biped.legR, accessor, Armatures.BIPED)
                .addProperty(AnimationProperty.AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH.get())
                .addProperty(AnimationProperty.AttackPhaseProperty.PARTICLE, EpicFightParticles.HIT_BLUNT)
                .addProperty(AnimationProperty.AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLUNT_HIT.get())
                .addProperty(AnimationProperty.AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.COUNTER))
                .addProperty(AnimationProperty.AttackPhaseProperty.STUN_TYPE, StunType.LONG)
                .addProperty(AnimationProperty.AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1.0F))
                .addProperty(AnimationProperty.AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(0.5F))
                .addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.setter(1F))).get();

        if(ModList.get().isLoaded("yamatomoveset")){
            MOB_YAMATO_GUARD = builder.nextAccessor("guard/guard_yamato", accessor -> new CustomGuardAnimation(accessor,
                    "yamatomoveset:biped/yamato/yamato_guard_hit", Armatures.BIPED)).get();
        }

        if(ModList.get().isLoaded("wom")){
            MOB_AGONY_GUARD = builder.nextAccessor("guard/guard_agony", accessor -> new CustomGuardAnimation(accessor,
                    "epicfight:biped/skill/guard_spear_hit", Armatures.BIPED)).get();
            MOB_RUINE_GUARD = builder.nextAccessor("guard/guard_ruine", accessor -> new CustomGuardAnimation(accessor,
                    "epicfight:biped/skill/guard_longsword_hit", Armatures.BIPED)).get();
            MOB_HERRSCHER_GUARD = builder.nextAccessor("guard/guard_herrscher", accessor -> new CustomGuardAnimation(accessor,
                    "indestructible:guard/shield_hit_left", Armatures.BIPED)).get();
        }

        SHIELD_HIT = builder.nextAccessor("guard/shield_hit_left", accessor -> new CustomGuardAnimation(accessor,
                null, Armatures.BIPED, true)).get();
        MOB_SHIELD_GUARD = builder.nextAccessor("guard/guard_shield", accessor -> new CustomGuardAnimation(accessor,
                "indestructible:guard/shield_hit_left", Armatures.BIPED,true)).get();
    }




}
