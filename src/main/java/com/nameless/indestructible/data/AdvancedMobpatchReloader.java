package com.nameless.indestructible.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.nameless.indestructible.api.animation.types.CommandEvent;
import com.nameless.indestructible.gameasset.GuardAnimations;
import com.nameless.indestructible.main.Indestructible;
import com.nameless.indestructible.network.SPDatapackSync;
import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch;
import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch.CounterMotion;
import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch.CustomAnimationMotion;
import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch.DamageSourceModifier;
import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch.GuardMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.entity.HealthPoint;
import yesman.epicfight.data.conditions.entity.RandomChance;
import yesman.epicfight.data.conditions.entity.TargetInDistance;
import yesman.epicfight.data.conditions.entity.TargetInEyeHeight;
import yesman.epicfight.data.conditions.entity.TargetInPov;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.Faction;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.capabilities.provider.EntityPatchProvider;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static yesman.epicfight.api.data.reloader.MobPatchReloadListener.*;

public class AdvancedMobpatchReloader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).create();
    private static final Map<EntityType<?>, CompoundTag> TAGMAP = Maps.newHashMap();
    private static final Map<EntityType<?>, MobPatchReloadListener.AbstractMobPatchProvider> ADVANCED_MOB_PATCH_PROVIDERS = Maps.newHashMap();
    private static final List<AdvancedCustomHumanoidMobPatchProvider> NBT_TAG_PROVIDERS = Lists.newArrayList();

    public AdvancedMobpatchReloader() {
        super(GSON, "advanced_mobpatch");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        // Clear both provider maps on reload
        ADVANCED_MOB_PATCH_PROVIDERS.clear();
        NBT_TAG_PROVIDERS.clear();
        TAGMAP.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
            ResourceLocation rl = entry.getKey();
            CompoundTag tag = null;

            try {
                tag = TagParser.parseTag(entry.getValue().toString());
            } catch (CommandSyntaxException e) {
                Indestructible.LOGGER.error("[Advanced Mobpatch] Failed to parse JSON for " + rl, e);
                continue;
            }

            // ===== NBT_TAG PATH: Check for nbt_tag field =====
            if (tag.contains("nbt_tag")) {
                try {
                    String nbtTagString = tag.getString("nbt_tag");
                    CompoundTag nbtMatcher = TagParser.parseTag(nbtTagString);
                    AdvancedCustomHumanoidMobPatchProvider provider = deserializeMobPatchProvider(null, tag, false);
                    provider.nbtMatcher = nbtMatcher;
                    NBT_TAG_PROVIDERS.add(provider);
                    Indestructible.LOGGER.info("[Advanced Mobpatch] Loaded NBT-tag provider from " + rl + " with matcher: " + nbtTagString);
                } catch (CommandSyntaxException e) {
                    Indestructible.LOGGER.error("[Advanced Mobpatch] Invalid SNBT in nbt_tag field for " + rl, e);
                    continue;
                }
            } else {
                // ===== ENTITY_TYPE PATH: Filename must match entity registry key =====
                String pathString = rl.getPath();
                ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), pathString);

                if (!ForgeRegistries.ENTITY_TYPES.containsKey(registryName)) {
                    Indestructible.LOGGER.warn("[Advanced Mobpatch] Entity named " + registryName + " does not exist");
                    continue;
                }

                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(registryName);
                ADVANCED_MOB_PATCH_PROVIDERS.put(entityType, deserializeMobPatchProvider(entityType, tag, false));
                EntityPatchProvider.putCustomEntityPatch(entityType, (entity) -> () -> ADVANCED_MOB_PATCH_PROVIDERS.get(entity.getType()).get(entity));
                TAGMAP.put(entityType, filterClientData(tag));

                if (EpicFightSharedConstants.isPhysicalClient()) {
                    ClientEngine.getInstance().renderEngine.registerCustomEntityRenderer(entityType, tag.contains("preset") ? tag.getString("preset") : tag.getString("renderer"), tag);
                }
            }
        }
    }

    public static AdvancedCustomHumanoidMobPatchProvider deserializeMobPatchProvider(EntityType<?> entityType, CompoundTag tag, boolean clientSide) {
            AdvancedCustomHumanoidMobPatchProvider provider = new AdvancedCustomHumanoidMobPatchProvider();
            provider.attributeValues = deserializeAdvancedAttributes(tag.getCompound("attributes"));
            ResourceLocation modelLocation = ResourceLocation.parse(tag.getString("model"));
            ResourceLocation armatureLocation = ResourceLocation.parse(tag.getString("armature"));

            if (EpicFightSharedConstants.isPhysicalClient()) {
                Meshes.getOrCreate(modelLocation, (jsonAssetLoader) -> jsonAssetLoader.loadSkinnedMesh(HumanoidMesh::new));
            }

            provider.armature = Armatures.getOrCreate(armatureLocation, Armature::new);
            if (entityType != null) {
                Armatures.registerEntityTypeArmature(entityType, provider.armature);
            }

            provider.hasBossBar = tag.contains("boss_bar") && tag.getBoolean("boss_bar");
            provider.name = tag.contains("boss_bar") && tag.contains("custom_name") ? tag.getString("custom_name") : null;
            provider.bossBar = tag.contains("boss_bar") && tag.contains("custom_texture") ? ResourceLocation.tryParse(tag.getString("custom_texture")) : null;
                provider.swingSound = tag.contains("swing_sound")
                    ? ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse(tag.getString("swing_sound")))
                    : null;
                provider.hitSound = tag.contains("hit_sound")
                    ? ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse(tag.getString("hit_sound")))
                    : null;
                provider.hitParticle = tag.contains("hit_particle")
                    ? (HitParticleType) ForgeRegistries.PARTICLE_TYPES.getValue(ResourceLocation.parse(tag.getString("hit_particle")))
                    : null;


            provider.defaultAnimations = deserializeDefaultAnimations(tag.getCompound("default_livingmotions"));
            provider.faction = Faction.ENUM_MANAGER.getOrThrow(tag.getString("faction"));
            provider.scale = tag.getCompound("attributes").contains("scale") ? (float)tag.getCompound("attributes").getDouble("scale") : 1.0F;
            provider.maxStunShield = tag.getCompound("attributes").contains("max_stun_shield") ? (float)tag.getCompound("attributes").getDouble("max_stun_shield") : 0F;
            if (!clientSide) {
                provider.ammoSlots = tag.contains("ammo_slots", 9) ? deserializeAmmoSlots(tag.getList("ammo_slots", 10)) : List.of();
                provider.stunAnimations = deserializeStunAnimations(tag.getCompound("stun_animations"));
                provider.chasingSpeed = tag.getCompound("attributes").getDouble("chasing_speed");
                provider.AHCombatBehaviors = deserializeAdvancedCombatBehaviors(tag.getList("combat_behavior", 10));
                provider.AHWeaponMotions = deserializeHumanoidWeaponMotions(tag.getList("humanoid_weapon_motions", 10));
                provider.guardMotions = deserializeGuardMotions(tag.getList("custom_guard_motion",10));
                provider.regenStaminaStandbyTime = tag.getCompound("attributes").contains("stamina_regan_delay") ? tag.getCompound("attributes").getInt("stamina_regan_delay") : 30;
                provider.hasStunReduction = !tag.getCompound("attributes").contains("has_stun_reduction") || tag.getCompound("attributes").getBoolean("has_stun_reduction");
                provider.reganShieldStandbyTime = tag.getCompound("attributes").contains("stun_shield_regan_delay") ? tag.getCompound("attributes").getInt("stun_shield_regan_delay") : 30;
                provider.reganShieldMultiply = tag.getCompound("attributes").contains("stun_shield_regan_multiply")
                    ? (float)tag.getCompound("attributes").getDouble("stun_shield_regan_multiply")
                    : (tag.getCompound("attributes").contains("stun_shield_multiply")
                        ? (float)tag.getCompound("attributes").getDouble("stun_shield_multiply")
                        : 1F);
                provider.staminaLoseMultiply = tag.getCompound("attributes").contains("stamina_lose_multiply") ? (float)tag.getCompound("attributes").getDouble("stamina_lose_multiply") : 0F;
                provider.attackRadius = tag.getCompound("attributes").contains("attack_radius") ? (float)tag.getCompound("attributes").getDouble("attack_radius") : 1.5F;
                provider.guardRadius = tag.getCompound("attributes").contains("guard_radius") ? (float)tag.getCompound("attributes").getDouble("guard_radius") : 3F;
                provider.stunEvent = deserializeStunCommandList(tag.getList("stun_command_list", 10));
            }
            return provider;
    }

    public static CompoundTag filterClientData(CompoundTag tag) {
        CompoundTag clientTag = new CompoundTag();
        extractBranch(clientTag, tag);
        return clientTag;
    }

    public static CompoundTag extractBranch(CompoundTag extract, CompoundTag original) {
        extract.put("model", original.get("model"));
        extract.put("armature", original.get("armature"));
        extract.putBoolean("isHumanoid", original.contains("isHumanoid") ? original.getBoolean("isHumanoid") : false);
        extract.put("renderer", original.get("renderer"));
        extract.put("faction", original.get("faction"));
        extract.put("default_livingmotions", original.get("default_livingmotions"));
        extract.put("attributes", original.get("attributes"));
        if (original.contains("nbt_tag")) {
            extract.put("nbt_tag", original.get("nbt_tag"));
        }
        if(original.contains("boss_bar")){
            extract.put("boss_bar", original.get("boss_bar"));
            if(original.contains("custom_name"))extract.put("custom_name", original.get("custom_name"));
            if(original.contains("custom_texture"))extract.put("custom_texture", original.get("custom_texture"));
        }
        if (original.contains("swing_sound")) {
            extract.put("swing_sound", original.get("swing_sound"));
        }
        if (original.contains("hit_sound")) {
            extract.put("hit_sound", original.get("hit_sound"));
        }
        if (original.contains("hit_particle")) {
            extract.put("hit_particle", original.get("hit_particle"));
        }
        return extract;
    }

    public static Stream<CompoundTag> getDataStream() {
        Stream<CompoundTag> tagStream = TAGMAP.entrySet().stream().map((entry) -> {
            entry.getValue().putString("id", ForgeRegistries.ENTITY_TYPES.getKey(entry.getKey()).toString());
            return entry.getValue();
        });

        return tagStream;
    }

    public static int getTagCount() {
        return TAGMAP.size();
    }

    public static List<AdvancedCustomHumanoidMobPatchProvider> getNbtTagProviders() {
        return NBT_TAG_PROVIDERS;
    }

    @OnlyIn(Dist.CLIENT)
    public static void processServerPacket(SPDatapackSync packet) {
        ADVANCED_MOB_PATCH_PROVIDERS.clear();
        NBT_TAG_PROVIDERS.clear();

        for (CompoundTag tag : packet.getTags()) {
            // Handle NBT_TAG entries
            if (tag.contains("nbt_tag")) {
                try {
                    String nbtTagString = tag.getString("nbt_tag");
                    CompoundTag nbtMatcher = TagParser.parseTag(nbtTagString);
                    AdvancedCustomHumanoidMobPatchProvider provider = deserializeMobPatchProvider(null, tag, true);
                    provider.nbtMatcher = nbtMatcher;
                    NBT_TAG_PROVIDERS.add(provider);
                } catch (CommandSyntaxException e) {
                    Indestructible.LOGGER.error("[Advanced Mobpatch] Invalid SNBT in nbt_tag field on client", e);
                }
            } else {
                // Handle entity-type entries
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(tag.getString("id")));
                ADVANCED_MOB_PATCH_PROVIDERS.put(entityType, deserializeMobPatchProvider(entityType, tag, true));
                EntityPatchProvider.putCustomEntityPatch(entityType, (entity) -> () -> ADVANCED_MOB_PATCH_PROVIDERS.get(entity.getType()).get(entity));
                ResourceLocation armatureLocation = ResourceLocation.parse(tag.getString("armature"));
                boolean humanoid = tag.getBoolean("isHumanoid");
                AssetAccessor<? extends Armature> armature = Armatures.getOrCreate(armatureLocation, humanoid ? Armature::new : HumanoidArmature::new);
                Armatures.registerEntityTypeArmature(entityType, armature);
                ClientEngine.getInstance().renderEngine.registerCustomEntityRenderer(entityType, tag.contains("preset") ? tag.getString("preset") : tag.getString("renderer"), tag);
            }
        }
    }

    public enum AmmoSlotRule {
        SUPPLY_OR_RELOAD_FROM;
    }

    public enum AmmoSlotType {
        EQUIPMENT,
        INVENTORY,
        CUSTOMNPC_PROJECTILE,
        CUSTOMNPC_DROP
    }

    public static class AmmoSlotConfig {
        private final String slot;
        private final AmmoSlotRule rule;
        private final AmmoSlotType type;
        @Nullable
        private final EquipmentSlot equipmentSlot;
        private final int index;

        public AmmoSlotConfig(String slot, AmmoSlotRule rule, AmmoSlotType type, @Nullable EquipmentSlot equipmentSlot, int index) {
            this.slot = slot;
            this.rule = rule;
            this.type = type;
            this.equipmentSlot = equipmentSlot;
            this.index = index;
        }

        public String getSlot() {
            return this.slot;
        }

        public AmmoSlotRule getRule() {
            return this.rule;
        }

        public AmmoSlotType getType() {
            return this.type;
        }

        @Nullable
        public EquipmentSlot getEquipmentSlot() {
            return this.equipmentSlot;
        }

        public int getIndex() {
            return this.index;
        }
    }


    public static class AdvancedCustomHumanoidMobPatchProvider extends MobPatchReloadListener.AbstractMobPatchProvider {
        protected Map<WeaponCategory, Map<Style, CombatBehaviors.Builder<HumanoidMobPatch<?>>>> AHCombatBehaviors;
        protected Map<WeaponCategory, Map<Style, Set<Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>>>>> AHWeaponMotions;
        protected Map<WeaponCategory, Map<Style, GuardMotion>> guardMotions;
        protected int regenStaminaStandbyTime;
        protected boolean hasStunReduction;
        protected float maxStunShield;
        protected int reganShieldStandbyTime;
        protected float reganShieldMultiply;
        protected float staminaLoseMultiply;
        protected float guardRadius;
        protected float attackRadius;
        protected List<Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>>> defaultAnimations;
        protected Map<StunType, AnimationAccessor<? extends StaticAnimation>> stunAnimations;
        protected Map<Attribute, Double> attributeValues;
        protected Faction faction;
        protected double chasingSpeed;
        protected float scale;
        protected boolean hasBossBar;
        protected ResourceLocation bossBar;
        protected String name;
        protected SoundEvent swingSound;
        protected SoundEvent hitSound;
        protected HitParticleType hitParticle;
        protected List<CommandEvent.StunEvent> stunEvent;
        protected List<AmmoSlotConfig> ammoSlots = List.of();
        protected AssetAccessor<? extends Armature> armature;
        @Nullable
        protected CompoundTag nbtMatcher;
        public AdvancedCustomHumanoidMobPatchProvider() {
        }

        @SuppressWarnings("rawtypes")
        public EntityPatch<?> get(Entity entity) {
            return new AdvancedCustomHumanoidMobPatch(this.faction, this);
        }

        public Map<WeaponCategory, Map<Style, Set<Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>>>>> getHumanoidWeaponMotions() {
            return this.AHWeaponMotions;
        }

        public Map<WeaponCategory, Map<Style, CombatBehaviors.Builder<HumanoidMobPatch<?>>>> getHumanoidCombatBehaviors() {
            return this.AHCombatBehaviors;
        }

        public Map<WeaponCategory, Map<Style, GuardMotion>> getGuardMotions(){
            return this.guardMotions;
        }
        public List<Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>>> getDefaultAnimations() {
            return this.defaultAnimations;
        }

        public Map<StunType, AnimationAccessor<? extends StaticAnimation>> getStunAnimations() {
            return this.stunAnimations;
        }

        public Map<Attribute, Double> getAttributeValues() {
            return this.attributeValues;
        }

        public double getChasingSpeed() {
            return this.chasingSpeed;
        }
        public float getScale() {
            return this.scale;
        }
        //stamina
        public int getRegenStaminaStandbyTime(){return this.regenStaminaStandbyTime;}
        //stun
        public boolean hasStunReduction(){return this.hasStunReduction;}
        public float getMaxStunShield(){return this.maxStunShield;}
        public int getReganShieldStandbyTime(){return this.reganShieldStandbyTime;}
        public float getReganShieldMultiply() {return this.reganShieldMultiply;}
        public float getStaminaLoseMultiply(){return this.staminaLoseMultiply;}
        public float getGuardRadius(){return this.guardRadius;}
        public float getAttackRadius(){return this.attackRadius;}
        public SoundEvent getSwingSound() {return this.swingSound;}
        public SoundEvent getHitSound() {return this.hitSound;}
        public HitParticleType getHitParticle() {return this.hitParticle;}
        public List<CommandEvent.StunEvent> getStunEvent(){
            return this.stunEvent;
        }
        public List<AmmoSlotConfig> getAmmoSlots() {
            return this.ammoSlots;
        }
        public boolean hasBossBar(){return this.hasBossBar;}
        public String getName(){return this.name;}
        public ResourceLocation getBossBar(){return this.bossBar;}
        public void registerArmature(EntityType<?> entityType) {
            if (entityType != null && this.armature != null) {
                Armatures.registerEntityTypeArmature(entityType, this.armature);
            }
        }
        @Nullable
        public CompoundTag getNbtMatcher() {return this.nbtMatcher;}
    }
    public static Map<Attribute, Double> deserializeAdvancedAttributes(CompoundTag tag) {
        Map<Attribute, Double> attributes = Maps.newHashMap();
        attributes.put(EpicFightAttributes.WEIGHT.get(), tag.contains("weight") ? tag.getDouble("weight") : 40);
        attributes.put(EpicFightAttributes.IMPACT.get(), tag.contains("impact") ? tag.getDouble("impact") : 0.5);
        attributes.put(EpicFightAttributes.ARMOR_NEGATION.get(), tag.contains("armor_negation") ? tag.getDouble("armor_negation") : 0.0);
        attributes.put(EpicFightAttributes.MAX_STAMINA.get(), tag.contains("max_stamina") ? tag.getDouble("max_stamina") : 15.0);
        attributes.put(EpicFightAttributes.STAMINA_REGEN.get(), tag.contains("stamina_regan_multiply") ? tag.getDouble("stamina_regan_multiply") : 1.0F);
        attributes.put(EpicFightAttributes.MAX_STRIKES.get(), (double)(tag.contains("max_strikes", 3) ? tag.getInt("max_strikes") : 1));
        if (tag.contains("attack_damage", 6)) {
            attributes.put(Attributes.ATTACK_DAMAGE, tag.getDouble("attack_damage"));
        }

        return attributes;
    }

    public static Map<WeaponCategory, Map<Style, CombatBehaviors.Builder<HumanoidMobPatch<?>>>> deserializeAdvancedCombatBehaviors(ListTag tag) {
        Map<WeaponCategory, Map<Style, CombatBehaviors.Builder<HumanoidMobPatch<?>>>> combatBehaviorsMapBuilder = Maps.newHashMap();

        for (int i = 0; i < tag.size(); i++) {
            CompoundTag combatBehavior = tag.getCompound(i);
            ListTag categories = combatBehavior.getList("weapon_categories", 8);
            Style style = Style.ENUM_MANAGER.get(combatBehavior.getString("style"));
            CombatBehaviors.Builder<HumanoidMobPatch<?>> builder = deserializeAdvancedBehaviorsBuilder(combatBehavior.getList("behavior_series", 10));

            for (int j = 0; j < categories.size(); j++) {
                WeaponCategory category = WeaponCategory.ENUM_MANAGER.get(categories.getString(j));
                combatBehaviorsMapBuilder.computeIfAbsent(category, (key) -> Maps.newHashMap());
                combatBehaviorsMapBuilder.get(category).put(style, builder);
            }
        }

        return combatBehaviorsMapBuilder;
    }

    public static Map<WeaponCategory, Map<Style, GuardMotion>> deserializeGuardMotions(ListTag tag){
        Map<WeaponCategory, Map<Style,GuardMotion>> map = Maps.newHashMap();

        for (int i = 0; i < tag.size(); i++) {
            CompoundTag list = tag.getCompound(i);
            Style style = Style.ENUM_MANAGER.get(list.getString("style"));
            AnimationAccessor<? extends StaticAnimation> guard = list.contains("guard") ? AnimationManager.byKey(ResourceLocation.parse(list.getString("guard"))) : getDefaultGuardAnimationAccessor();
            float guard_cost = list.contains("stamina_cost_multiply") ? (float)list.getDouble("stamina_cost_multiply") : 1F;
            boolean canBlockProjectile = list.contains("can_block_projectile") && list.getBoolean("can_block_projectile");
            float parry_cost = list.contains("parry_cost_multiply") ? (float)list.getDouble("parry_cost_multiply") : 0.5F;
            AnimationAccessor<? extends StaticAnimation>[] parry_animations = null;
            if(list.contains("parry_animation")){
                ListTag animationId = list.getList("parry_animation", 8);
                parry_animations = new AnimationAccessor[animationId.size()];
                for (int j = 0; j < animationId.size(); j++) {
                    AnimationAccessor<? extends StaticAnimation> parry_animation = AnimationManager.byKey(ResourceLocation.parse(animationId.getString(j)));
                    parry_animations[j] = parry_animation;
                }
            }

            Tag weponTypeTag = list.get("weapon_categories");

            if (weponTypeTag instanceof StringTag) {
                WeaponCategory weaponCategory = WeaponCategory.ENUM_MANAGER.get(weponTypeTag.getAsString());
                if (!map.containsKey(weaponCategory)) {
                    map.put(weaponCategory, Maps.newHashMap());
                }
                map.get(weaponCategory).put(style, new GuardMotion(guard, canBlockProjectile, guard_cost, parry_cost, parry_animations));

            } else if (weponTypeTag instanceof ListTag weponTypesTag) {

                for (int j = 0; j < weponTypesTag.size(); j++) {
                    WeaponCategory weaponCategory = WeaponCategory.ENUM_MANAGER.get(weponTypesTag.getString(j));
                    if (!map.containsKey(weaponCategory)) {
                        map.put(weaponCategory, Maps.newHashMap());
                    }
                    map.get(weaponCategory).put(style, new GuardMotion(guard, canBlockProjectile, guard_cost, parry_cost, parry_animations));
                }
            }
        }
        return map;
    }

    private static AnimationAccessor<? extends StaticAnimation> getDefaultGuardAnimationAccessor() {
        if (GuardAnimations.MOB_LONGSWORD_GUARD != null) {
            return AnimationManager.byKey(GuardAnimations.MOB_LONGSWORD_GUARD.getRegistryName());
        }

        return Animations.LONGSWORD_GUARD;
    }

    private static <T extends MobPatch<?>> CombatBehaviors.Builder<T> deserializeAdvancedBehaviorsBuilder(ListTag tag) {
        CombatBehaviors.Builder<T> builder = CombatBehaviors.builder();

        for (int i = 0; i < tag.size(); i++) {
            CompoundTag behaviorSeries = tag.getCompound(i);
            float weight = (float)behaviorSeries.getDouble("weight");
            int cooldown = behaviorSeries.contains("cooldown") ? behaviorSeries.getInt("cooldown") : 0;
            boolean canBeInterrupted = behaviorSeries.contains("canBeInterrupted") && behaviorSeries.getBoolean("canBeInterrupted");
            boolean looping = behaviorSeries.contains("looping") && behaviorSeries.getBoolean("looping");
            ListTag behaviorList = behaviorSeries.getList("behaviors", 10);
            CombatBehaviors.BehaviorSeries.Builder<T> behaviorSeriesBuilder = CombatBehaviors.BehaviorSeries.builder();
            behaviorSeriesBuilder.weight(weight).cooldown(cooldown).canBeInterrupted(canBeInterrupted).looping(looping);

            for (int j = 0; j < behaviorList.size(); j++) {
                CombatBehaviors.Behavior.Builder<T> behaviorBuilder = CombatBehaviors.Behavior.builder();
                CompoundTag behavior = behaviorList.getCompound(j);
                ListTag conditionList = behavior.getList("conditions", 10);
                int phase = behavior.contains("set_phase") ? behavior.getInt("set_phase") : -1;
                int hurt_level = behavior.contains("end_by_hurt_level") ? behavior.getInt("end_by_hurt_level") : 2;
                if(behavior.contains("animation")) {
                    AnimationAccessor<? extends StaticAnimation> animation = AnimationManager.byKey(ResourceLocation.parse(behavior.getString("animation")));
                    float speed = behavior.contains("play_speed") ? (float) behavior.getDouble("play_speed") : 1F;
                    float stamina = behavior.contains("stamina") ? (float) behavior.getDouble("stamina") : 0F;
                    float convertTime = behavior.contains("convert_time") ? (float)behavior.getDouble("convert_time") : 0F;
                    CustomAnimationMotion motion = new CustomAnimationMotion(animation,convertTime,speed,stamina);
                    List<CommandEvent.TimeStampedEvent> timeCommandList = behavior.contains("command_list") ? deserializeTimeCommandList(behavior.getList("command_list", 10)) : null;
                    List<CommandEvent.BiEvent> hitCommandList = behavior.contains("hit_command_list") ? deserializeHitCommandList(behavior.getList("hit_command_list", 10)) : null;
                    DamageSourceModifier modifier = behavior.contains("damage_modifier") ? deserializeDamageModifier(behavior.getCompound("damage_modifier")) : null;
                    List<CommandEvent.BlockedEvent> blockedEvents = behavior.contains("blocked_command_list") ? deserializeBlockedCommandList(behavior.getList("blocked_command_list",10)) : null;
                    behaviorBuilder.behavior(customAttackAnimation(motion, modifier, timeCommandList, hitCommandList, blockedEvents, phase, hurt_level));
                } else if (behavior.contains("guard")){
                    int guardTime = behavior.getInt("guard");
                    AnimationAccessor<? extends StaticAnimation> counter = behavior.contains("counter") ? AnimationManager.byKey(ResourceLocation.parse(behavior.getString("counter"))) : AnimationManager.byKey(GuardAnimations.MOB_COUNTER_ATTACK.getRegistryName());
                    boolean isParry = behavior.contains("parry") && behavior.getBoolean("parry");
                    int parry_times = behavior.contains("parry_times")
                            ? behavior.getInt("parry_times")
                            : (behavior.contains("parry_time") ? behavior.getInt("parry_time") : Integer.MAX_VALUE);
                    int stun_immunity_time = behavior.contains("stun_immunity_time") ? behavior.getInt("stun_immunity_time") : 0;
                    float cost = behavior.contains("counter_cost") ? (float) behavior.getDouble("counter_cost") : 3.0F;
                    float chance = behavior.contains("counter_chance") ? (float)behavior.getDouble("counter_chance") : 0.3F;
                    float speed = behavior.contains("counter_speed") ? (float)behavior.getDouble("counter_speed") : 1F;
                    CounterMotion counterMotion = new CounterMotion(counter, cost, chance, speed);
                    boolean cancel = !behavior.contains("cancel_after_counter") || behavior.getBoolean("cancel_after_counter");
                    GuardMotion guardMotion = behavior.contains("specific_guard_motion") ? deserializeSpecificGuardMotion(behavior.getCompound("specific_guard_motion")) : null;
                    behaviorBuilder.behavior(setGuardMotion(guardTime, isParry, parry_times, stun_immunity_time, counterMotion, cancel, guardMotion, phase, hurt_level));
                } else if (behavior.contains("wander")){
                    int strafingTime = behavior.getInt("wander");
                    int inactionTime = behavior.contains("inaction_time") ?  behavior.getInt("inaction_time") : behavior.getInt("wander");
                    float forward = behavior.contains("z_axis") ? (float) behavior.getDouble("z_axis") : 0F;
                    float clockwise = behavior.contains("x_axis") ? (float) behavior.getDouble("x_axis") : 0F;
                    behaviorBuilder.behavior(setStrafing(strafingTime, inactionTime, forward, clockwise, phase, hurt_level));
                } else if (behavior.contains("gear_swap", 10)) {
                    CompoundTag gearSwap = behavior.getCompound("gear_swap");
                    String itemKey = null;
                    ResourceLocation requiredGunId = null;
                    if (gearSwap.contains("item", 8)) {
                        itemKey = gearSwap.getString("item");
                    }

                    if (gearSwap.contains("gun_id", 8)) {
                        requiredGunId = ResourceLocation.tryParse(gearSwap.getString("gun_id"));
                        if (requiredGunId == null) {
                            loggerNote(Indestructible.LOGGER, "gear_swap", "gun_id", "resource_location", "tacz:ak47");
                            continue;
                        }

                        // TACZ gun variant id (GunId) is metadata on a TACZ gun item stack.
                        // If no explicit item is provided, default to the TACZ base gun item.
                        if (itemKey == null || itemKey.isBlank()) {
                            itemKey = "tacz:modern_kinetic_gun";
                        }
                    }

                    if (itemKey == null || itemKey.isBlank()) {
                        loggerNote(Indestructible.LOGGER, "gear_swap", "item|gun_id", "string", "");
                    } else {
                        ResourceLocation itemId = ResourceLocation.parse(itemKey);
                        EquipmentSlot slot = gearSwap.contains("slot", 8) ? parseEquipmentSlot(gearSwap.getString("slot")) : null;
                        boolean allowEquipped = !gearSwap.contains("allow_equipped") || gearSwap.getBoolean("allow_equipped");
                        boolean storeOldGear = !gearSwap.contains("store_gear") || gearSwap.getBoolean("store_gear");
                        boolean requiredInInventory = !gearSwap.contains("required_in_inv") || gearSwap.getBoolean("required_in_inv");
                        CompoundTag generatedGearTag = null;
                        if (gearSwap.contains("nbt", 10)) {
                            generatedGearTag = gearSwap.getCompound("nbt").copy();
                        } else if (gearSwap.contains("nbt", 8)) {
                            try {
                                generatedGearTag = TagParser.parseTag(gearSwap.getString("nbt"));
                            } catch (CommandSyntaxException e) {
                                Indestructible.LOGGER.warn("Invalid gear_swap nbt payload: {}", gearSwap.getString("nbt"), e);
                            }
                        }

                        // Ensure TACZ GunId is present for generated stacks when gun_id is declared.
                        if (requiredGunId != null) {
                            if (generatedGearTag == null) {
                                generatedGearTag = new CompoundTag();
                            }
                            if (!generatedGearTag.contains("GunId", 8)) {
                                generatedGearTag.putString("GunId", requiredGunId.toString());
                            }
                        }

                        float stamina = gearSwap.contains("stamina") ? (float) gearSwap.getDouble("stamina") : 0F;
                        int inactionTime = gearSwap.contains("inaction_time") ? gearSwap.getInt("inaction_time") : 0;
                        SoundEvent swapSound = gearSwap.contains("sound", 8)
                                ? ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse(gearSwap.getString("sound")))
                                : null;
                        behaviorBuilder.behavior(setGearSwap(itemId, slot, allowEquipped, storeOldGear, requiredInInventory, requiredGunId, generatedGearTag, stamina, inactionTime, swapSound, phase, hurt_level));
                    }
                } else if (behavior.contains("gear_swap", 9)) {
                    ListTag swapsTag = behavior.getList("gear_swap", 10);
                    if (swapsTag.isEmpty()) {
                        Indestructible.LOGGER.warn("[Advanced Mobpatch] gear_swap list is empty, skipping");
                    } else {
                        List<AdvancedCustomHumanoidMobPatch.GearSwapEntry> entries = new java.util.ArrayList<>();
                        for (int s = 0; s < swapsTag.size(); s++) {
                            CompoundTag se = swapsTag.getCompound(s);
                            String seItemKey = null;
                            ResourceLocation seGunId = null;
                            if (se.contains("item", 8)) seItemKey = se.getString("item");
                            if (se.contains("gun_id", 8)) {
                                seGunId = ResourceLocation.tryParse(se.getString("gun_id"));
                                if (seGunId != null && (seItemKey == null || seItemKey.isBlank())) seItemKey = "tacz:modern_kinetic_gun";
                            }
                            if (seItemKey == null || seItemKey.isBlank()) { continue; }
                            ResourceLocation seItemId = ResourceLocation.parse(seItemKey);
                            EquipmentSlot seSlot = se.contains("slot", 8) ? parseEquipmentSlot(se.getString("slot")) : null;
                            boolean seStore = !se.contains("store_gear") || se.getBoolean("store_gear");
                            boolean seReqInv = !se.contains("required_in_inv") || se.getBoolean("required_in_inv");
                            CompoundTag seNbt = null;
                            if (se.contains("nbt", 10)) {
                                seNbt = se.getCompound("nbt").copy();
                            } else if (se.contains("nbt", 8)) {
                                try { seNbt = TagParser.parseTag(se.getString("nbt")); } catch (CommandSyntaxException e) { Indestructible.LOGGER.warn("Invalid nbt in gear_swap list entry", e); }
                            }
                            if (seGunId != null) {
                                if (seNbt == null) seNbt = new CompoundTag();
                                if (!seNbt.contains("GunId", 8)) seNbt.putString("GunId", seGunId.toString());
                            }
                            entries.add(new AdvancedCustomHumanoidMobPatch.GearSwapEntry(seItemId, seSlot, seStore, seReqInv, seGunId, seNbt));
                        }
                        if (!entries.isEmpty()) {
                            float stamina = behavior.contains("stamina") ? (float) behavior.getDouble("stamina") : 0F;
                            int inactionTime = behavior.contains("inaction_time") ? behavior.getInt("inaction_time") : 0;
                            SoundEvent swapSound = behavior.contains("sound", 8)
                                    ? ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse(behavior.getString("sound")))
                                    : null;
                            behaviorBuilder.behavior(setGearSwapMulti(entries, stamina, inactionTime, swapSound, phase, hurt_level));
                        }
                    }
                } else if (behavior.contains("reload")) {
                    int reloadTime = behavior.getInt("reload");
                    InteractionHand hand = behavior.contains("hand", 8) ? parseInteractionHand(behavior.getString("hand")) : null;
                    boolean requireAmmo = !behavior.contains("require_ammo") || behavior.getBoolean("require_ammo");
                    float stamina = behavior.contains("stamina") ? (float) behavior.getDouble("stamina") : 0F;
                    List<CommandEvent.TimeStampedEvent> timeCommandList = behavior.contains("command_list") ? deserializeTimeCommandList(behavior.getList("command_list", 10)) : null;
                    behaviorBuilder.behavior(setReload(reloadTime, hand, requireAmmo, stamina, timeCommandList, phase, hurt_level));
                } else if (behavior.contains("tacz_aim", 10)) {
                    CompoundTag taczAim = behavior.getCompound("tacz_aim");
                    InteractionHand hand = taczAim.contains("hand", 8) ? parseInteractionHand(taczAim.getString("hand")) : null;
                    int inactionTime = taczAim.contains("inaction_time") ? taczAim.getInt("inaction_time") : 4;
                    float stamina = taczAim.contains("stamina") ? (float) taczAim.getDouble("stamina") : 0F;
                    List<CommandEvent.TimeStampedEvent> timeCommandList = taczAim.contains("command_list") ? deserializeTimeCommandList(taczAim.getList("command_list", 10)) : null;
                    behaviorBuilder.behavior(setTaczAim(hand, inactionTime, stamina, timeCommandList, phase, hurt_level));
                } else if (behavior.contains("tacz_aim", 3)) {
                    int inactionTime = behavior.getInt("tacz_aim");
                    InteractionHand hand = behavior.contains("hand", 8) ? parseInteractionHand(behavior.getString("hand")) : null;
                    float stamina = behavior.contains("stamina") ? (float) behavior.getDouble("stamina") : 0F;
                    List<CommandEvent.TimeStampedEvent> timeCommandList = behavior.contains("command_list") ? deserializeTimeCommandList(behavior.getList("command_list", 10)) : null;
                    behaviorBuilder.behavior(setTaczAim(hand, inactionTime, stamina, timeCommandList, phase, hurt_level));
                } else if (behavior.contains("tacz_shoot", 10)) {
                    CompoundTag taczShoot = behavior.getCompound("tacz_shoot");
                    String fireMode = taczShoot.contains("fire_mode", 8) ? taczShoot.getString("fire_mode") : null;
                    float stamina = taczShoot.contains("stamina") ? (float) taczShoot.getDouble("stamina") : 0F;
                    List<CommandEvent.TimeStampedEvent> timeCommandList = taczShoot.contains("command_list") ? deserializeTimeCommandList(taczShoot.getList("command_list", 10)) : null;
                    behaviorBuilder.behavior(setTaczShoot(fireMode, stamina, timeCommandList, phase, hurt_level));
                } else if (behavior.contains("tacz_shoot", 3)) {
                    String fireMode = behavior.contains("fire_mode", 8) ? behavior.getString("fire_mode") : null;
                    float stamina = behavior.contains("stamina") ? (float) behavior.getDouble("stamina") : 0F;
                    List<CommandEvent.TimeStampedEvent> timeCommandList = behavior.contains("command_list") ? deserializeTimeCommandList(behavior.getList("command_list", 10)) : null;
                    behaviorBuilder.behavior(setTaczShoot(fireMode, stamina, timeCommandList, phase, hurt_level));
                }

                for (int k = 0; k < conditionList.size(); k++) {
                    CompoundTag condition = conditionList.getCompound(k);
                    Condition<T> predicate = deserializeAdvancedBehaviorPredicate(condition.getString("predicate"), condition);
                    behaviorBuilder.predicate(predicate);
                }

                behaviorSeriesBuilder.nextBehavior(behaviorBuilder);
            }

            builder.newBehaviorSeries(behaviorSeriesBuilder);
        }

        return builder;
    }



    public static <T extends MobPatch<?>> Consumer<T> customAttackAnimation(CustomAnimationMotion motion, @Nullable DamageSourceModifier damageSourceModifier,
                                                                             @Nullable List<CommandEvent.TimeStampedEvent> timeEvents, @Nullable List<CommandEvent.BiEvent> hitEvents, @Nullable List<CommandEvent.BlockedEvent> blockedEvents, int phase, int hurtResist){
        return (mobpatch) -> {
            if(mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch){
                advancedCustomHumanoidMobPatch.setAttackSpeed(motion.speed());
                advancedCustomHumanoidMobPatch.setBlocking(false);
                if(motion.stamina() != 0F) advancedCustomHumanoidMobPatch.setStamina(advancedCustomHumanoidMobPatch.getStamina() - motion.stamina());
                if(timeEvents != null){
                    for(CommandEvent.TimeStampedEvent event : timeEvents){
                        advancedCustomHumanoidMobPatch.getEventManager().addTimeStampedEvent(event);
                    }
                }
                if(hitEvents != null){
                    for(CommandEvent.BiEvent event : hitEvents){
                        advancedCustomHumanoidMobPatch.getEventManager().addHitEvent(event);
                    }
                }
                if(blockedEvents != null){
                    for(CommandEvent.BlockedEvent event : blockedEvents){
                        advancedCustomHumanoidMobPatch.getEventManager().addBlockedEvents(event);
                    }
                }
                advancedCustomHumanoidMobPatch.setDamageSourceModifier(damageSourceModifier);
                if(phase >= 0)advancedCustomHumanoidMobPatch.setPhase(phase);
                advancedCustomHumanoidMobPatch.setHurtResistLevel(hurtResist);
            }
            if(!mobpatch.getEntityState().turningLocked()){mobpatch.getOriginal().lookAt(mobpatch.getTarget(),30F,30F); }
            mobpatch.playAnimationSynchronized(motion.animation(), motion.convertTime());
        };
    }

    public static GuardMotion deserializeSpecificGuardMotion(CompoundTag args){
        GuardMotion guardMotion = null;
        if(args.contains("guard") && args.contains("stamina_cost_multiply") && args.contains("can_block_projectile") && args.contains("parry_cost_multiply") && args.contains("parry_animation")) {
            AnimationAccessor<? extends StaticAnimation> guard = AnimationManager.byKey(ResourceLocation.parse(args.getString("guard")));
            float guard_cost = (float) args.getDouble("stamina_cost_multiply");
            boolean canBlockProjectile = args.getBoolean("can_block_projectile");
            float parry_cost = (float) args.getDouble("parry_cost_multiply");
            AnimationAccessor<? extends StaticAnimation>[] parry_animations = null;
            if (args.contains("parry_animation")) {
                ListTag animationId = args.getList("parry_animation", 8);
                parry_animations = new AnimationAccessor[animationId.size()];
                for (int j = 0; j < animationId.size(); j++) {
                    AnimationAccessor<? extends StaticAnimation> parry_animation = AnimationManager.byKey(ResourceLocation.parse(animationId.getString(j)));
                    parry_animations[j] = parry_animation;
                }
            }
            guardMotion = new GuardMotion(guard, canBlockProjectile, guard_cost, parry_cost, parry_animations);
        }
        return guardMotion;
    }

    public static <T extends MobPatch<?>> Consumer<T> setGuardMotion(int guardTime, boolean parry, int parry_time, int stun_immunity_time, CounterMotion counter_motion,
                                                                     boolean cancel, @Nullable GuardMotion guard_motion, int phase, int hurtResist) {
        return (mobpatch) -> {
            if(mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch){
                if(guard_motion != null) advancedCustomHumanoidMobPatch.specificGuardMotion(guard_motion);
                advancedCustomHumanoidMobPatch.modifyLivingMotionByCurrentItem(false);
                advancedCustomHumanoidMobPatch.setBlocking(true);
                advancedCustomHumanoidMobPatch.setBlockTick(guardTime);
                advancedCustomHumanoidMobPatch.setParry(parry);
                advancedCustomHumanoidMobPatch.setMaxParryTimes(parry_time);
                advancedCustomHumanoidMobPatch.setStunImmunityTime(stun_immunity_time);
                advancedCustomHumanoidMobPatch.setCounterMotion(counter_motion);
                advancedCustomHumanoidMobPatch.cancelBlock(cancel);
                advancedCustomHumanoidMobPatch.setHurtResistLevel(hurtResist);
                if(phase >= 0)advancedCustomHumanoidMobPatch.setPhase(phase);
            }
        };
    }

    public static <T extends MobPatch<?>> Consumer<T> setStrafing(int strafingTime, int inactionTime, float forward, float clockwise, int phase, int hurtResist){
        return (mobpatch) -> {
            if(mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch){
                advancedCustomHumanoidMobPatch.setStrafingTime(strafingTime);
                advancedCustomHumanoidMobPatch.setInactionTime(inactionTime);
                advancedCustomHumanoidMobPatch.setStrafingDirection(forward, clockwise);
                advancedCustomHumanoidMobPatch.setHurtResistLevel(hurtResist);
                if(phase >= 0)advancedCustomHumanoidMobPatch.setPhase(phase);
            }
        };
    }

    public static <T extends MobPatch<?>> Consumer<T> setGearSwap(ResourceLocation itemId, @Nullable EquipmentSlot slot,
                                                                   boolean allowEquipped, boolean storeOldGear, boolean requiredInInventory,
                                                                   @Nullable ResourceLocation requiredGunId,
                                                                   @Nullable CompoundTag generatedGearTag, float stamina,
                                                                   int inactionTime, @Nullable SoundEvent swapSound, int phase, int hurtResist) {
        return (mobpatch) -> {
            if (mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch) {
                if (stamina > 0.0F && advancedCustomHumanoidMobPatch.getStamina() < stamina) {
                    return;
                }

                boolean swapped = advancedCustomHumanoidMobPatch.tryGearSwap(itemId, slot, allowEquipped, storeOldGear, requiredInInventory, requiredGunId, generatedGearTag);
                if (!swapped) {
                    return;
                }

                if (stamina > 0.0F) {
                    advancedCustomHumanoidMobPatch.setStamina(advancedCustomHumanoidMobPatch.getStamina() - stamina);
                }

                advancedCustomHumanoidMobPatch.setBlocking(false);
                advancedCustomHumanoidMobPatch.setAttackSpeed(1.0F);
                advancedCustomHumanoidMobPatch.resetActionTick();
                advancedCustomHumanoidMobPatch.resetMotion();
                advancedCustomHumanoidMobPatch.setInactionTime(Math.max(advancedCustomHumanoidMobPatch.getInactionTime(), inactionTime));
                advancedCustomHumanoidMobPatch.setHurtResistLevel(hurtResist);
                if (phase >= 0) advancedCustomHumanoidMobPatch.setPhase(phase);
                if (swapSound != null) {
                    advancedCustomHumanoidMobPatch.playSound(swapSound, 0.0F, 0.0F);
                }
            }
        };
    }

    public static <T extends MobPatch<?>> Consumer<T> setGearSwapMulti(List<AdvancedCustomHumanoidMobPatch.GearSwapEntry> entries, float stamina, int inactionTime,
                                                                        @Nullable SoundEvent swapSound, int phase, int hurtResist) {
        return (mobpatch) -> {
            if (mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> patch) {
                if (stamina > 0.0F && patch.getStamina() < stamina) return;
                boolean anySwapped = patch.tryGearSwapMulti(entries);
                if (!anySwapped) return;
                if (stamina > 0.0F) patch.setStamina(patch.getStamina() - stamina);
                patch.setBlocking(false);
                patch.setAttackSpeed(1.0F);
                patch.resetActionTick();
                patch.resetMotion();
                patch.setInactionTime(Math.max(patch.getInactionTime(), inactionTime));
                patch.setHurtResistLevel(hurtResist);
                if (phase >= 0) patch.setPhase(phase);
                if (swapSound != null) patch.playSound(swapSound, 0.0F, 0.0F);
            }
        };
    }

    public static <T extends MobPatch<?>> Consumer<T> setReload(int reloadTime, @Nullable InteractionHand hand, boolean requireAmmo, float stamina,
                                                                 @Nullable List<CommandEvent.TimeStampedEvent> timeEvents,
                                                                 int phase, int hurtResist) {
        return (mobpatch) -> {
            if (mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch) {
                if (timeEvents != null) {
                    for (CommandEvent.TimeStampedEvent event : timeEvents) {
                        advancedCustomHumanoidMobPatch.getEventManager().addTimeStampedEvent(event);
                    }
                }

                boolean started = advancedCustomHumanoidMobPatch.tryStartReload(hand, requireAmmo, reloadTime, stamina);
                if (!started) {
                    return;
                }

                advancedCustomHumanoidMobPatch.setHurtResistLevel(hurtResist);
                if (phase >= 0) advancedCustomHumanoidMobPatch.setPhase(phase);
            }
        };
    }

    public static <T extends MobPatch<?>> Consumer<T> setTaczShoot(@Nullable String fireMode, float stamina,
                                                                    @Nullable List<CommandEvent.TimeStampedEvent> timeEvents,
                                                                    int phase, int hurtResist) {
        return (mobpatch) -> {
            if (mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch) {
                if (timeEvents != null) {
                    for (CommandEvent.TimeStampedEvent event : timeEvents) {
                        advancedCustomHumanoidMobPatch.getEventManager().addTimeStampedEvent(event);
                    }
                }

                boolean started = advancedCustomHumanoidMobPatch.tryTaczShoot(stamina, fireMode);
                if (!started) {
                    return;
                }

                advancedCustomHumanoidMobPatch.setHurtResistLevel(hurtResist);
                if (phase >= 0) advancedCustomHumanoidMobPatch.setPhase(phase);
            }
        };
    }

    public static <T extends MobPatch<?>> Consumer<T> setTaczAim(@Nullable InteractionHand hand, int inactionTime, float stamina,
                                                                  @Nullable List<CommandEvent.TimeStampedEvent> timeEvents,
                                                                  int phase, int hurtResist) {
        return (mobpatch) -> {
            if (mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch) {
                if (timeEvents != null) {
                    for (CommandEvent.TimeStampedEvent event : timeEvents) {
                        advancedCustomHumanoidMobPatch.getEventManager().addTimeStampedEvent(event);
                    }
                }

                boolean started = advancedCustomHumanoidMobPatch.tryTaczAim(hand, inactionTime, stamina);
                if (!started) {
                    return;
                }

                advancedCustomHumanoidMobPatch.setHurtResistLevel(hurtResist);
                if (phase >= 0) advancedCustomHumanoidMobPatch.setPhase(phase);
            }
        };
    }



    public static List<CommandEvent.TimeStampedEvent> deserializeTimeCommandList(ListTag args){
        List<CommandEvent.TimeStampedEvent> list = Lists.newArrayList();
        for(int k = 0; k < args.size(); k++){
            CompoundTag command = args.getCompound(k);
            boolean execute_at_target = command.contains("execute_at_target") && command.getBoolean("execute_at_target");
            CommandEvent.TimeStampedEvent event = CommandEvent.TimeStampedEvent.CreateTimeCommandEvent(command.getFloat("time"), command.getString("command"), execute_at_target);
            list.add(event);
        }
        return list;
    }

    public static List<CommandEvent.BiEvent> deserializeHitCommandList(ListTag args){
        List<CommandEvent.BiEvent> list = Lists.newArrayList();
        for(int k = 0; k < args.size(); k++){
            CompoundTag command = args.getCompound(k);
            boolean execute_at_target = command.contains("execute_at_target") && command.getBoolean("execute_at_target");
            CommandEvent.BiEvent event = CommandEvent.BiEvent.CreateBiCommandEvent(command.getString("command"), execute_at_target);
            list.add(event);
        }
        return list;
    }

    public static List<CommandEvent.StunEvent> deserializeStunCommandList(ListTag args){
        List<CommandEvent.StunEvent> list = Lists.newArrayList();
        for(int k = 0; k < args.size(); k++){
            CompoundTag command = args.getCompound(k);
            boolean execute_at_target = command.contains("execute_at_target") && command.getBoolean("execute_at_target");
            CommandEvent.StunEvent event = CommandEvent.StunEvent.CreateStunCommandEvent(command.getString("command"), execute_at_target, StunType.valueOf(command.getString("stun_type").toUpperCase(Locale.ROOT)));
            list.add(event);
        }
        return list;
    }

    public static List<CommandEvent.BlockedEvent> deserializeBlockedCommandList(ListTag args){
        List<CommandEvent.BlockedEvent> list = Lists.newArrayList();
        for(int k = 0; k < args.size(); k++){
            CompoundTag command = args.getCompound(k);
            boolean execute_at_target = command.contains("execute_at_target") && command.getBoolean("execute_at_target");
            CommandEvent.BlockedEvent event = CommandEvent.BlockedEvent.CreateBlockCommandEvent(command.getString("command"), execute_at_target, command.getBoolean("is_parry"));
            list.add(event);
        }
        return list;
    }

    public static DamageSourceModifier deserializeDamageModifier(CompoundTag args){
        float damage = args.contains("damage") ? args.getFloat("damage") : 1F;
        float impact = args.contains("impact") ? args.getFloat("impact") : 1F;
        float armor_negation = args.contains("armor_negation") ? args.getFloat("armor_negation") : 1F;
        StunType stunType = args.contains("stun_type") ? StunType.valueOf(args.getString("stun_type").toUpperCase(Locale.ROOT)) : null;
        Collider collider = args.contains("collider") ?  deserializeCollider(args.getCompound("collider")) : null;
        return new DamageSourceModifier(damage, impact, armor_negation, stunType, collider);
    }

    public static Collider deserializeCollider(CompoundTag tag) {
        int number = tag.getInt("number");

        if (number < 1) {
            EpicFightMod.LOGGER.warn("Datapack deserialization error: the number of colliders must bigger than 0! ");
            return null;
        }

        ListTag sizeVector = tag.getList("size", 6);
        ListTag centerVector = tag.getList("center", 6);

        double sizeX = sizeVector.getDouble(0);
        double sizeY = sizeVector.getDouble(1);
        double sizeZ = sizeVector.getDouble(2);

        double centerX = centerVector.getDouble(0);
        double centerY = centerVector.getDouble(1);
        double centerZ = centerVector.getDouble(2);

        if (sizeX < 0 || sizeY < 0 || sizeZ < 0) {
            EpicFightMod.LOGGER.warn("Datapack deserialization error: the size of the collider must be non-negative! ");
            return null;
        }

        if (number == 1) {
            return new OBBCollider(sizeX, sizeY, sizeZ, centerX, centerY, centerZ);
        } else {
            return new MultiOBBCollider(number, sizeX, sizeY, sizeZ, centerX, centerY, centerZ);
        }
    }


    public static <T extends MobPatch<?>> Condition<T> deserializeAdvancedBehaviorPredicate(String type, CompoundTag args) {
        String predicateType = type;
        int namespaceSeparator = type.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < type.length()) {
            predicateType = type.substring(namespaceSeparator + 1);
        }

        Condition<T> predicate = null;
        List<String[]> loggerNote = Lists.newArrayList();

        switch (predicateType) {
            case "random_chance":
                if (!args.contains("chance", 6)) {
                    loggerNote.add(new String[] {"random_chance", "chance", "double", "0.0"});
                }

                predicate = castCondition(new RandomChance((float) args.getDouble("chance")));
                break;
            case "within_eye_height":
                predicate = castCondition(new TargetInEyeHeight());
                break;
            case "within_distance":
                if (!args.contains("min", 6)) {
                    loggerNote.add(new String[] {"within_distance", "min", "double", "0.0"});
                }

                if (!args.contains("max", 6)) {
                    loggerNote.add(new String[] {"within_distance", "max", "double", "0.0"});
                }

                predicate = castCondition(new TargetInDistance(args.getDouble("min"), args.getDouble("max")));
                break;
            case "within_angle":
                if (!args.contains("min", 6)) {
                    loggerNote.add(new String[] {"within_angle", "within_distance", "min", "double", "0.0F"});
                }

                if (!args.contains("max", 6)) {
                    loggerNote.add(new String[] {"within_angle", "max", "double", "0.0F"});
                }

                predicate = castCondition(new TargetInPov(args.getDouble("min"), args.getDouble("max")));
                break;
            case "within_angle_horizontal":
                if (!args.contains("min", 6)) {
                    loggerNote.add(new String[] {"within_angle_horizontal", "min", "double", "0.0F"});
                }

                if (!args.contains("max", 6)) {
                    loggerNote.add(new String[] {"within_angle_horizontal", "max", "double", "0.0F"});
                }

                predicate = castCondition(new TargetInPov.TargetInPovHorizontal(args.getDouble("min"), args.getDouble("max")));
                break;
            case "health":
                if (!args.contains("health", 6)) {
                    loggerNote.add(new String[] {"health", "health", "double", "0.0F"});
                }

                if (!args.contains("comparator", 8)) {
                    loggerNote.add(new String[] {"health", "comparator", "string", ""});
                }

                predicate = castCondition(new HealthPoint((float) args.getDouble("health"), HealthPoint.Comparator.valueOf(args.getString("comparator").toUpperCase(Locale.ROOT))));
                break;

            case "guard_break":
                if(!args.contains("invert")){
                    loggerNote.add(new String[] {"guard_break", "invert", "boolean", ""});
                }
                predicate = new ExtraPredicate.TargetIsGuardBreak<>(args.getBoolean("invert"));
                break;

            case "knock_down":
                if(!args.contains("invert")){
                    loggerNote.add(new String[] {"knock_down", "invert", "boolean", ""});
                }
                predicate = new ExtraPredicate.TargetIsKnockDown<>(args.getBoolean("invert"));
                break;

            case "attack_level":
                if(!args.contains("min",3)){
                    loggerNote.add(new String[] {"level","min","int",""});
                }
                if(!args.contains("max",3)){
                    loggerNote.add(new String[] {"level","max","int",""});
                }
                predicate = new ExtraPredicate.TargetWithinState<>(args.getInt("min"),args.getInt("max"));
                break;

            case "stamina":
                if (!args.contains("stamina", 6)) {
                    loggerNote.add(new String[] {"stamina", "stamina", "double", "0.0F"});
                }

                if (!args.contains("comparator", 8)) {
                    loggerNote.add(new String[] {"stamina", "comparator", "string", ""});
                }
                predicate = new ExtraPredicate.SelfStamina<>((float) args.getDouble("stamina"), HealthPoint.Comparator.valueOf(args.getString("comparator").toUpperCase(Locale.ROOT)));
                break;

            case "using_item":
                if(!args.contains("edible")){
                    loggerNote.add(new String[] {"using_item", "edible", "boolean", ""});
                }
                predicate = new ExtraPredicate.TargetIsUsingItem<>(args.getBoolean("edible"));
                break;

            case "mainhand_weapon_category":
                if (!args.contains("category", 8)) {
                    loggerNote.add(new String[] {"mainhand_weapon_category", "category", "string", ""});
                }
                predicate = new ExtraPredicate.MainhandWeaponCategory<>(args.getString("category"));
                break;

            case "offhand_weapon_category":
                if (!args.contains("category", 8)) {
                    loggerNote.add(new String[] {"offhand_weapon_category", "category", "string", ""});
                }
                predicate = new ExtraPredicate.OffhandWeaponCategory<>(args.getString("category"));
                break;

            case "has_gear_in_inventory":
                if (!args.contains("item", 8)) {
                    loggerNote.add(new String[] {"has_gear_in_inventory", "item", "string", ""});
                }
                predicate = new ExtraPredicate.HasGearInInventory<>(
                        ResourceLocation.parse(args.getString("item")),
                        args.contains("slot", 8) ? parseEquipmentSlot(args.getString("slot")) : null,
                        args.contains("include_equipped") && args.getBoolean("include_equipped")
                );
                break;

            case "has_ammo":
                predicate = new ExtraPredicate.HasAmmo<>(
                        args.contains("hand", 8) ? parseInteractionHand(args.getString("hand")) : InteractionHand.MAIN_HAND,
                        args.contains("invert") && args.getBoolean("invert")
                );
                break;

            case "ammo_is_empty":
                predicate = new ExtraPredicate.AmmoIsEmpty<>(
                        args.contains("hand", 8) ? parseInteractionHand(args.getString("hand")) : null,
                        args.contains("invert") && args.getBoolean("invert")
                );
                break;

            case "ammo_not_full":
                predicate = new ExtraPredicate.AmmoNotFull<>(
                        args.contains("hand", 8) ? parseInteractionHand(args.getString("hand")) : null,
                        args.contains("invert") && args.getBoolean("invert")
                );
                break;

            case "ammo_has_reserve":
                predicate = new ExtraPredicate.AmmoHasReserve<>(
                        args.contains("hand", 8) ? parseInteractionHand(args.getString("hand")) : null,
                        args.contains("invert") && args.getBoolean("invert")
                );
                break;

            case "target_blocking":
                predicate = new ExtraPredicate.TargetBlocking<>(args.contains("invert") && args.getBoolean("invert"));
                break;

            case "target_using_shield":
                predicate = new ExtraPredicate.TargetUsingShield<>(args.contains("invert") && args.getBoolean("invert"));
                break;

            case "no_target":
                predicate = new ExtraPredicate.NoTarget<>();
                break;

            case "has_target":
                predicate = new ExtraPredicate.HasTarget<>();
                break;

            case "phase":
                if(!args.contains("min",3)){
                    loggerNote.add(new String[] {"phase","min","int",""});
                }
                if(!args.contains("max",3)){
                    loggerNote.add(new String[] {"phase","max","int",""});
                }
                predicate = new ExtraPredicate.Phase<>(args.getInt("min"),args.getInt("max"));
                break;
        }

        for (String[] formatArgs : loggerNote) {
            Indestructible.LOGGER.info(String.format("[Custom Entity Error] can't find a proper argument for %s. [name: %s, type: %s, default: %s]", (Object[])formatArgs));
        }

        if (predicate == null) {
            throw new IllegalArgumentException("[Custom Entity Error] No predicate type: " + type);
        }

        return predicate;
    }

    @SuppressWarnings("unchecked")
    private static <T extends MobPatch<?>> Condition<T> castCondition(Condition<?> condition) {
        return (Condition<T>) condition;
    }

    private static EquipmentSlot parseEquipmentSlot(String slotName) {
        return switch (slotName.toLowerCase(Locale.ROOT)) {
            case "mainhand", "main_hand" -> EquipmentSlot.MAINHAND;
            case "offhand", "off_hand" -> EquipmentSlot.OFFHAND;
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "feet", "boots" -> EquipmentSlot.FEET;
            default -> throw new IllegalArgumentException("[Custom Entity Error] No equipment slot named: " + slotName);
        };
    }

    private static InteractionHand parseInteractionHand(String handName) {
        return switch (handName.toLowerCase(Locale.ROOT)) {
            case "mainhand", "main_hand" -> InteractionHand.MAIN_HAND;
            case "offhand", "off_hand" -> InteractionHand.OFF_HAND;
            default -> throw new IllegalArgumentException("[Custom Entity Error] No hand named: " + handName);
        };
    }

    private static List<AmmoSlotConfig> deserializeAmmoSlots(ListTag tag) {
        List<AmmoSlotConfig> ammoSlots = Lists.newArrayList();

        for (int index = 0; index < tag.size(); index++) {
            CompoundTag ammoSlotTag = tag.getCompound(index);
            if (!ammoSlotTag.contains("slot", 8)) {
                loggerNote(Indestructible.LOGGER, "ammo_slots", "slot", "string", "");
                continue;
            }
            if (!ammoSlotTag.contains("rule", 8)) {
                loggerNote(Indestructible.LOGGER, "ammo_slots", "rule", "string", "supply_or_reload_from");
                continue;
            }

            ammoSlots.add(parseAmmoSlotConfig(ammoSlotTag.getString("slot"), ammoSlotTag.getString("rule")));
        }

        return ammoSlots;
    }

    private static AmmoSlotConfig parseAmmoSlotConfig(String slotSelector, String ruleName) {
        AmmoSlotRule rule = parseAmmoSlotRule(ruleName);
        String normalizedSelector = slotSelector.toLowerCase(Locale.ROOT);

        if ("customnpcs:projectile".equals(normalizedSelector)) {
            return new AmmoSlotConfig(slotSelector, rule, AmmoSlotType.CUSTOMNPC_PROJECTILE, null, -1);
        }

        if (normalizedSelector.startsWith("inventory:")) {
            return new AmmoSlotConfig(slotSelector, rule, AmmoSlotType.INVENTORY, null,
                    parseAmmoSlotIndex(slotSelector, normalizedSelector, "inventory:"));
        }

        if (normalizedSelector.startsWith("customnpcs:drop:")) {
            return new AmmoSlotConfig(slotSelector, rule, AmmoSlotType.CUSTOMNPC_DROP, null,
                    parseAmmoSlotIndex(slotSelector, normalizedSelector, "customnpcs:drop:"));
        }

        return new AmmoSlotConfig(slotSelector, rule, AmmoSlotType.EQUIPMENT, parseEquipmentSlot(normalizedSelector), -1);
    }

    private static AmmoSlotRule parseAmmoSlotRule(String ruleName) {
        return switch (ruleName.toLowerCase(Locale.ROOT)) {
            case "supply_or_reload_from" -> AmmoSlotRule.SUPPLY_OR_RELOAD_FROM;
            default -> throw new IllegalArgumentException("[Custom Entity Error] No ammo slot rule named: " + ruleName);
        };
    }

    private static int parseAmmoSlotIndex(String originalSelector, String normalizedSelector, String prefix) {
        String indexText = normalizedSelector.substring(prefix.length());

        try {
            int index = Integer.parseInt(indexText);
            if (index < 0) {
                throw new IllegalArgumentException("[Custom Entity Error] Ammo slot index must be >= 0: " + originalSelector);
            }
            return index;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("[Custom Entity Error] Invalid ammo slot selector: " + originalSelector, exception);
        }
    }

    private static void loggerNote(org.apache.logging.log4j.Logger logger, String type, String name, String valueType, String defaultValue) {
        logger.info(String.format("[Custom Entity Error] can't find a proper argument for %s. [name: %s, type: %s, default: %s]", type, name, valueType, defaultValue));
    }
}
