package com.nameless.indestructible.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class UIConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final boolean DEFAULT_REPLACE_UI = true;
    public static final List<String> DEFAULT_BOSS_NAMES = List.of();
    public static final ForgeConfigSpec.ConfigValue<Boolean> REPLACE_UI;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOSS_NAME;
    public static final ForgeConfigSpec SPEC;
    static {
        BUILDER.push("replace entityindicator");
        BUILDER.comment("replace original entityindicator");
        REPLACE_UI = BUILDER.define("replace_ui", DEFAULT_REPLACE_UI);
        BUILDER.pop();

        BUILDER.push("cancel original boss bar");
        BOSS_NAME = BUILDER.defineList("name", ArrayList::new, obj -> true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static boolean replaceUi() {
        try {
            return REPLACE_UI.get();
        } catch (IllegalStateException exception) {
            return DEFAULT_REPLACE_UI;
        }
    }

    public static List<? extends String> bossNames() {
        try {
            return BOSS_NAME.get();
        } catch (IllegalStateException exception) {
            return DEFAULT_BOSS_NAMES;
        }
    }

}
