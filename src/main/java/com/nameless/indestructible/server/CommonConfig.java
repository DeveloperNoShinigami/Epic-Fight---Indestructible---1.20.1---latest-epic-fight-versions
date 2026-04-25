package com.nameless.indestructible.server;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class CommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final List<String> DEFAULT_NEUTRALIZE_ANIMATIONS = List.of("epicfight:biped/skill/guard_break1", "epicfight:biped/skill/guard_break2");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> NEUTRALIZE_ANIMATION;
    public static final ForgeConfigSpec SPEC;
    static {
        BUILDER.push("animation considered as neutralize animation");
        BUILDER.comment("mostly for predicate");
        NEUTRALIZE_ANIMATION = BUILDER.defineList("neutralize animation", DEFAULT_NEUTRALIZE_ANIMATIONS, (obj) -> true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static List<? extends String> neutralizeAnimations() {
        try {
            return NEUTRALIZE_ANIMATION.get();
        } catch (IllegalStateException exception) {
            return DEFAULT_NEUTRALIZE_ANIMATIONS;
        }
    }

}
