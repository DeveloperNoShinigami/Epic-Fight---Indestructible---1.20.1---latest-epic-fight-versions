package com.nameless.indestructible.world.capability;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.nameless.indestructible.api.animation.types.CommandEvent;
import com.nameless.indestructible.data.AdvancedMobpatchReloader.AmmoSlotConfig;
import com.nameless.indestructible.api.animation.types.CustomGuardAnimation;
import com.nameless.indestructible.client.gui.BossBarGUi;
import com.nameless.indestructible.data.AdvancedMobpatchReloader.AdvancedCustomHumanoidMobPatchProvider;
import com.nameless.indestructible.gameasset.GuardAnimations;
import com.nameless.indestructible.main.Indestructible;
import com.nameless.indestructible.mixin.BehaviorMixin;
import com.nameless.indestructible.mixin.BehaviorSeriesMixin;
import com.nameless.indestructible.mixin.TargetInDistanceMixin;
import com.nameless.indestructible.server.AdvancedBossInfo;
import com.nameless.indestructible.world.ai.goal.AdvancedChasingGoal;
import com.nameless.indestructible.world.ai.goal.AdvancedCombatGoal;
import com.nameless.indestructible.world.ai.goal.GuardGoal;
import com.nameless.indestructible.world.ai.task.AdvancedChasingBehavior;
import com.nameless.indestructible.world.ai.task.AdvancedCombatBehavior;
import com.nameless.indestructible.world.ai.task.GuardBehavior;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.entity.TargetInDistance;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeLivingMotion;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.Faction;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.effect.EpicFightMobEffects;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;
import yesman.epicfight.world.entity.ai.behavior.BackUpIfTooCloseStopInaction;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.nameless.indestructible.world.ai.CombatBehaviors.WanderMotionSet;
import com.nameless.indestructible.world.capability.Utils.IAdvancedCapability;

public class AdvancedCustomHumanoidMobPatch<T extends PathfinderMob> extends HumanoidMobPatch<T> implements IAdvancedCapability {

    private final AdvancedCustomHumanoidMobPatchProvider provider;
    private static final Set<AdvancedCustomHumanoidMobPatch<?>> PENDING_AI_REBUILDS = new java.util.HashSet<>();
    private final List<AmmoSlotConfig> ammoSlots;
    private final Map<WeaponCategory, Map<Style,GuardMotion>> weaponGuardMotions;
    private GuardMotion currentGuardMotion;
    private static final EntityDataAccessor<Float> STAMINA = new EntityDataAccessor<Float>(253, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ATTACK_SPEED = new EntityDataAccessor<Float>(177, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_BLOCKING = new EntityDataAccessor<Boolean>(178, EntityDataSerializers.BOOLEAN);
    public static final ResourceLocation BOSS_BAR = ResourceLocation.fromNamespaceAndPath(Indestructible.MOD_ID, "textures/gui/boss_bar.png");
    //stamina
    private final int regenStaminaStandbyTime;
    //stun shield
    private final boolean hasStunReduction;
    private final float maxStunShield;
    private final int reganShieldStandbyTime;
    private final float reganShieldMultiply;
    //block
    private final float staminaLoseMultiply;
    private int block_tick;
    private boolean cancel_block;
    private int maxParryTimes;
    private int tickSinceLastAction;
    private int tickSinceBreakShield;
    private CounterMotion counterMotion;
    //parry
    private boolean isParry;
    private int parryCounter = 0;
    private int parryTimes = 0;
    private int stun_immunity_time;
    private final float guardRadius;
    private final float attackRadius;
    //event
    private final AdvancedCustomPatchEventManger eventManger;
    private DamageSourceModifier damageSourceModifier = null;
    private int phase;
    private int hurtResistLevel = 2;
    private boolean neutralized;
    //wandering
    private float strafingForward;
    private float strafingClockwise;
    private int strafingTime;
    private int inactionTime;
    private int convertTick = 0;
    private boolean isRunning = false;
    private Entity lastAttacker;
    private float lastGetImpact;
    private GuardMotion specificGuardMotion;
    private AdvancedBossInfo bossInfo;
    public  boolean hasBossBar;
    private Component customName;
    private ResourceLocation bossBar;
    public boolean interrupted;
    private boolean isParried = false;
    private WeaponCategory lastMainHandCategory;
    private Style lastMainHandStyle;
    private WeaponCategory lastOffHandCategory;
    private Style lastOffHandStyle;
    private boolean lastBlockingState;
    private boolean pendingWeaponMotionResync;
    private boolean infantryCombatAiRefreshQueued;
    private double cNPC_EpicFight_Addon$activeTaczCombatRange = -1.0D;
    private static final float BASELINE_STEP_HEIGHT = 1.5F;
    private static final int STEP_ASSIST_COOLDOWN_TICKS = 8;
    private static final double STEP_ASSIST_MIN_HORIZONTAL_SPEED_SQR = 0.0009D;
    private static final int TACZ_SUSTAIN_KEEP_ALIVE_TICKS = 6;
    private int stepAssistCooldownTicks;
    private boolean taczSustainRequested;
    private boolean taczSustainActive;
    private int taczSustainKeepAliveTicks;
    private float taczSustainStaminaCost;
    @Nullable
    private yesman.epicfight.api.asset.AssetAccessor<? extends StaticAnimation> taczAimAnimation;
    @Nullable
    private String taczSustainFireMode;
    /** Prevents a provider/equipment refresh from mutating GoalSelector while it is being ticked. */
    private boolean aiInitializationQueued;

    public AdvancedCustomHumanoidMobPatch(Faction faction, AdvancedCustomHumanoidMobPatchProvider provider) {
        super(faction);
        this.provider = provider;
        this.ammoSlots = this.provider.getAmmoSlots();
        this.regenStaminaStandbyTime = this.provider.getRegenStaminaStandbyTime();
        this.hasStunReduction = this.provider.hasStunReduction();
        this.maxStunShield = this.provider.getMaxStunShield();
        this.reganShieldStandbyTime = this.provider.getReganShieldStandbyTime();
        this.reganShieldMultiply = this.provider.getReganShieldMultiply();
        this.staminaLoseMultiply = this.provider.getStaminaLoseMultiply();
        this.weaponLivingMotions = this.provider.getHumanoidWeaponMotions();
        this.weaponAttackMotions = this.provider.getHumanoidCombatBehaviors();
        this.weaponGuardMotions = this.provider.getGuardMotions();
        this.guardRadius = this.provider.getGuardRadius();
        this.attackRadius = this.provider.getAttackRadius();
        this.hasBossBar = this.provider.hasBossBar();
        this.eventManger = new AdvancedCustomPatchEventManger();
    }

    @Override
    public void onConstructed(T entityIn) {
        super.onConstructed(entityIn);
        // Re-applying this patch on the same entity can call onConstructed again.
        // Ignore duplicate define attempts so provider swap can proceed safely.
        try {
            entityIn.getEntityData().define(STAMINA, 0.0F);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            entityIn.getEntityData().define(ATTACK_SPEED, 1.0F);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            entityIn.getEntityData().define(IS_BLOCKING, false);
        } catch (IllegalArgumentException ignored) {
        }
        if(this.hasBossBar) {
            this.bossInfo = new AdvancedBossInfo(this);
        }
    }

    @Override
    public void onJoinWorld(T entityIn, EntityJoinLevelEvent event) {
        this.initialized = true;
        this.original.getAttributes().supplier = new AttributeSupplier(putEpicFightAttributes(this.original.getAttributes().supplier.instances));
        this.initAttributes();
        // Keep CNPC movement compatible with vanilla one-block navigation and
        // Epic Fight dodge/backstep movement from the first server tick.
        this.original.setMaxUpStep(BASELINE_STEP_HEIGHT);
        if (!entityIn.level().isClientSide() && !this.original.isNoAi()) {
            this.initAI();
        }
        this.tickSinceLastAction = 0;
        this.tickSinceBreakShield = 0;
        this.block_tick = 30;
        this.setStamina(this.getMaxStamina());
        this.setAttackSpeed(1F);
        this.setPhase(0);
        if(this.maxStunShield > 0) {
            this.setMaxStunShield(this.maxStunShield);
            this.setStunShield(this.maxStunShield);
        }
        if(!this.isLogicalClient()){
            this.getEventManager().initPassiveEvent(this.provider);
            this.resetMotion();
        this.forceWeaponMotionResync();
        }
        this.bossBar = this.provider.getBossBar() == null ? BOSS_BAR : this.provider.getBossBar();
        this.customName = this.provider.getName() == null ? this.getOriginal().getType().getDescription() : Component.translatable(this.provider.getName());
    }

    /**
     * Provider replacement and held-item updates can happen from a LivingTick
     * callback while Mob.serverAiStep is iterating GoalSelector. Epic Fight's
     * base implementation removes/adds goals immediately, which causes a
     * ConcurrentModificationException. Rebuild goals between entity ticks.
     */
    @Override
    protected void initAI() {
        if (this.original.level().isClientSide()) {
            super.initAI();
            return;
        }

        MinecraftServer server = this.original.level().getServer();
        if (server == null) {
            super.initAI();
            return;
        }

        this.queueAiRebuild();
    }

    private void queueAiRebuild() {
        if (!this.aiInitializationQueued) {
            this.aiInitializationQueued = true;
            PENDING_AI_REBUILDS.add(this);
        }
    }

    /**
     * Compatibility entry point for integrations that used to rebuild goals
     * directly from an entity callback.  The actual mutation is deferred to
     * the next server-tick START phase.
     */
    public void requestAiRebuild() {
        if (!this.original.level().isClientSide()) {
            this.queueAiRebuild();
        }
    }

    /** Called from EFI's server-tick END hook, outside Mob.serverAiStep. */
    public static void processQueuedAiRebuilds() {
        if (PENDING_AI_REBUILDS.isEmpty()) {
            return;
        }

        List<AdvancedCustomHumanoidMobPatch<?>> pending = new ArrayList<>(PENDING_AI_REBUILDS);
        PENDING_AI_REBUILDS.clear();
        for (AdvancedCustomHumanoidMobPatch<?> patch : pending) {
            patch.aiInitializationQueued = false;
            if (patch.original == null || patch.original.isRemoved() || !patch.original.isAlive()
                    || patch.original.level().isClientSide() || patch.original.isNoAi()) {
                continue;
            }

            // This is the only place the queued advanced patch is allowed to
            // mutate GoalSelector. The server is no longer iterating goals.
            patch.rebuildAiNow();
        }
    }

    private void rebuildAiNow() {
        super.initAI();
    }

    private Map<Attribute, AttributeInstance> putEpicFightAttributes(Map<Attribute, AttributeInstance> originalMap) {
        Map<Attribute, AttributeInstance> newMap = Maps.newHashMap();
        AttributeSupplier supplier = AttributeSupplier.builder()
                .add(Attributes.ATTACK_DAMAGE)
                .add(EpicFightAttributes.WEIGHT.get())
                .add(EpicFightAttributes.IMPACT.get())
                .add(EpicFightAttributes.ARMOR_NEGATION.get())
                .add(EpicFightAttributes.MAX_STRIKES.get())
                .add(EpicFightAttributes.STUN_ARMOR.get())
                .add(EpicFightAttributes.EXECUTION_RESISTANCE.get())
                .add(EpicFightAttributes.OFFHAND_ARMOR_NEGATION.get())
                .add(EpicFightAttributes.OFFHAND_IMPACT.get())
                .add(EpicFightAttributes.OFFHAND_MAX_STRIKES.get())
                .add(EpicFightAttributes.OFFHAND_ATTACK_SPEED.get())
                .add(EpicFightAttributes.MAX_STAMINA.get())
                .add(EpicFightAttributes.STAMINA_REGEN.get())
                .build();
        newMap.putAll(supplier.instances);
        newMap.putAll(originalMap);
        return ImmutableMap.copyOf(newMap);
    }

    @Override
    public void serverTick(LivingEvent.LivingTickEvent event) {
        if (this.hasBossBar && this.getOriginal().tickCount % 4 == 0) bossInfo.update();
        if(this.hasStunReduction) {
            super.serverTick(event);
        }

        if (this.original.maxUpStep() < BASELINE_STEP_HEIGHT) {
            this.original.setMaxUpStep(BASELINE_STEP_HEIGHT);
        }
        this.tickUniversalStepAssist();
        this.tickTaczAimTracking();

        if (this.pendingWeaponMotionResync) {
            this.pendingWeaponMotionResync = false;
            this.forceWeaponMotionResync();
        }

        this.syncWeaponLivingMotionsIfNeeded();

        if(this.inactionTime > 0){
           inactionTime--;
        }

          this.tickTaczSustainedFire();

        if (!this.state.inaction() && this.isBlocking()) {
            this.tickSinceLastAction++;
        }

        float stamina = this.getStamina();
        float maxStamina = this.getMaxStamina();
        float staminaRegen = (float)this.original.getAttributeValue(EpicFightAttributes.STAMINA_REGEN.get());

        if (stamina < maxStamina) {
            if(this.tickSinceLastAction > this.regenStaminaStandbyTime) {
                float staminaFactor = 1.0F + (float) Math.pow((stamina / (maxStamina - stamina * 0.5F)), 2);
                this.setStamina(stamina + maxStamina * (0.01F * staminaRegen) * staminaFactor);
            } else {
                this.setStamina(stamina + 0.0015F * staminaRegen * maxStamina);
            }
        }

        if (maxStamina < stamina) {
            this.setStamina(maxStamina);
        }

        if(this.maxStunShield > 0){
            float stunShield = this.getStunShield();
            float maxStunShield = this.getMaxStunShield();
            if(stunShield > 0){
                if(stunShield < maxStunShield && !this.getEntityState().hurt() && !this.getEntityState().knockDown()){
                    this.setStunShield(stunShield + 0.0015F * this.reganShieldMultiply * maxStunShield);
                }
                if(this.tickSinceBreakShield > 0) this.tickSinceBreakShield = 0;
            }

            if(stunShield == 0){
                this.tickSinceBreakShield++;
                if(tickSinceBreakShield > this.reganShieldStandbyTime){
                    this.setStunShield(this.getMaxStunShield());
                }
            }

            if(stunShield > maxStunShield){
                this.setStunShield(this.getMaxStunShield());
            }
        }

        if(neutralized && this.getEntityState().hurtLevel() < 2){
                neutralized = false;
        }

        if(this.getEntityState().hurt() && this.getEntityState().hurtLevel() >= this.getHurtResistLevel()){
            interrupted = true;
        }

        // Idle reload: top off the gun while the NPC has no target so it enters the
        // next fight with a full magazine. require_ammo=false → direct fill in
        // reflectiveTryReload; no real ammo item is consumed.
        if (this.original.tickCount % 40 == 0) {
            LivingEntity idleTarget = this.getTarget();
            if ((idleTarget == null || !idleTarget.isAlive())
                    && !TaczCompat.isReloading(this.original)
                    && this.isAmmoNotFull(null)) {
                this.tryStartReload(null, false, 0, 0.0F);
            }
        }
    }

    private void tickUniversalStepAssist() {
        if (this.stepAssistCooldownTicks > 0) {
            this.stepAssistCooldownTicks--;
        }

        if (!this.shouldTriggerUniversalStepAssist()) {
            return;
        }

        this.original.getJumpControl().jump();
        this.stepAssistCooldownTicks = STEP_ASSIST_COOLDOWN_TICKS;
    }

    private boolean shouldTriggerUniversalStepAssist() {
        if (this.stepAssistCooldownTicks > 0 || this.original.isNoAi() || !this.original.isAlive()) {
            return false;
        }

        if (!this.original.onGround() || !this.original.horizontalCollision) {
            return false;
        }

        if (this.original.isPassenger() || this.original.isInWaterOrBubble() || this.original.onClimbable()) {
            return false;
        }

        if (this.getEntityState().hurt() || this.getEntityState().knockDown()) {
            return false;
        }

        return this.hasHorizontalMovementIntent();
    }

    private boolean hasHorizontalMovementIntent() {
        Vec3 delta = this.original.getDeltaMovement();
        double horizontalSpeedSqr = delta.x * delta.x + delta.z * delta.z;
        if (horizontalSpeedSqr >= STEP_ASSIST_MIN_HORIZONTAL_SPEED_SQR) {
            return true;
        }

        if (!this.original.getNavigation().isDone()) {
            return true;
        }

        return this.original.getMoveControl().hasWanted();
    }

    /**
     * TACZ aim is an operator state, while Entity.lookAt is only updated when
     * the datapack behavior is entered. Refresh both every server tick so a
     * moving target cannot leave the NPC visually aiming at its old position.
     * This also keeps aiming independent from the shoot behavior series.
     */
    private void tickTaczAimTracking() {
        boolean sustainingFire = this.taczSustainRequested || this.taczSustainActive;
        if (!TaczCompat.isAiming(this.original) && !sustainingFire) {
            this.syncTaczAimAnimation(false);
            return;
        }

        LivingEntity target = this.getTarget();
        InteractionHand gunHand = TaczCompat.findGunHand(this.original, null);
        if (target == null || !target.isAlive() || gunHand == null
                || TaczCompat.isReloading(this.original)
                || !this.isWithinTaczEngagementRange()) {
            TaczCompat.stopAiming(this.original);
            this.syncTaczAimAnimation(false);
            this.clearTaczSustainedFire();
            return;
        }

        this.original.lookAt(target, 30.0F, 30.0F);
        // Reassert the operator state in case TACZ cleared it during a shot
        // or an animation transition. Keep the aim state alive for the whole
        // sustained-fire window, not only while the aim behavior is selected.
        if (!TaczCompat.isAiming(this.original)) {
            if (!TaczCompat.tryAim(this.original, gunHand)) {
                this.clearTaczSustainedFire();
                this.syncTaczAimAnimation(false);
                return;
            }
        }
        this.syncTaczAimAnimation(true);
    }

    @Override
    protected void clientTick(LivingEvent.LivingTickEvent event) {
        boolean shouldRunning = this.original.walkAnimation.speed() >= 0.7F;
        if(shouldRunning != isRunning){
            this.convertTick++;
            if(convertTick > 4){
                isRunning = shouldRunning;
            }

        } else {
            this.convertTick = 0;
        }
    }

    public void resetMotion(){
        this.getEventManager().initAnimationEvent();
        if (this.damageSourceModifier != null) this.damageSourceModifier = null;
    }
    public void setBlockTick(int value){this.block_tick = value;}
    public int getBlockTick(){return this.block_tick;}
    public void setBlocking(boolean blocking){this.original.getEntityData().set(IS_BLOCKING, blocking);}
    public boolean isBlocking(){return this.original.getEntityData().get(IS_BLOCKING);}
    public void cancelBlock(boolean setCancel){
        this.cancel_block = setCancel;
    }
    public void setParry(boolean isParry){this.isParry = isParry;}
    public float getMaxStamina() {
        AttributeInstance maxStamina = this.original.getAttribute(EpicFightAttributes.MAX_STAMINA.get());
        return (float)(maxStamina == null ? 0 : maxStamina.getValue());
    }

    public float getStamina() {
        return this.getMaxStamina() == 0 ? 0 : this.original.getEntityData().get(STAMINA);
    }
    public void setStamina(float value) {
        float f1 = Math.max(Math.min(value, this.getMaxStamina()), 0);
        this.original.getEntityData().set(STAMINA, f1);
    }
    public float getAttackSpeed(){return this.original.getEntityData().get(ATTACK_SPEED);}
    public void setAttackSpeed(float value){
        this.original.getEntityData().set(ATTACK_SPEED, Math.abs(value));
    }

    public void setParryCounter(int counter){
        this.parryCounter = counter;
    }
    public void setStunImmunityTime(int time){
        this.stun_immunity_time = time;
    }

    public void setMaxParryTimes(int times){
        this.maxParryTimes = times;
    }

    public void setCounterMotion(CounterMotion counter_motion){
        this.counterMotion = counter_motion;
    }
    public AnimationAccessor<? extends StaticAnimation> getCounter(){
        return this.counterMotion.counter;
    }

    public float getCounterChance(){
        return this.counterMotion.chance;
    }

    public float getCounterStamina(){
        return this.counterMotion.cost;
    }

    public float getCounterSpeed(){
        return this.counterMotion.speed;
    }
    public void specificGuardMotion(@Nullable GuardMotion specific_guard_motion){
        this.specificGuardMotion = specific_guard_motion;
    }
    public void resetActionTick() {
        this.tickSinceLastAction = 0;
    }
    public int getTickSinceLastAction() {
        return this.tickSinceLastAction;
    }

    public AdvancedCustomPatchEventManger getEventManager(){
        return this.eventManger;
    }
    public void setDamageSourceModifier(DamageSourceModifier damageSourceModifier) {
        this.damageSourceModifier = damageSourceModifier;
    }

    public int getPhase(){return this.phase;}
    public void setPhase(int phase){
        this.phase = Math.min(Math.max(0, phase), 20);
    }
    public void setHurtResistLevel(int hurtResistLevel){
        this.hurtResistLevel = Math.max(hurtResistLevel, 1);
    }
    public int getHurtResistLevel(){
      return this.hurtResistLevel;
    }

    public int getStrafingTime(){return this.strafingTime;}
    public void setStrafingTime(int time){this.strafingTime = time;}
    public float getStrafingForward(){return this.strafingForward;}
    public float getStrafingClockwise(){return this.strafingClockwise;}
    public int getInactionTime(){return this.inactionTime;}
    public void setInactionTime(int time){this.inactionTime = time;}
    public void clearActiveTaczCombatRange(){this.cNPC_EpicFight_Addon$activeTaczCombatRange = -1.0D;}
    public void refreshTaczSustainHeartbeat() {
        if (this.taczSustainActive) {
            this.taczSustainRequested = true;
        }
    }
    public void setStrafingDirection(float forward, float clockwise){
        this.strafingForward = forward;
        this.strafingClockwise = clockwise;
    }

    @Override
    public void actStrafing(WanderMotionSet motionSet) {
        this.setStrafingTime(motionSet.time);
        this.setStrafingDirection(motionSet.forward, motionSet.clockwise);
    }

    public void updateActiveTaczCombatRange(@Nullable CombatBehaviors.BehaviorSeries<?> currentBehaviorSeries) {
        if (currentBehaviorSeries == null) {
            this.clearActiveTaczCombatRange();
            return;
        }

        double resolvedRange = -1.0D;
        for (CombatBehaviors.Behavior<?> behavior : ((BehaviorSeriesMixin) currentBehaviorSeries).getBehaviors()) {
            for (Condition<HumanoidMobPatch<?>> condition : ((BehaviorMixin<HumanoidMobPatch<?>>) behavior).getConditions()) {
                Object rawCondition = condition;
                if (rawCondition instanceof TargetInDistance targetInDistance) {
                    resolvedRange = Math.max(resolvedRange, ((TargetInDistanceMixin) (Object) targetInDistance).getMax());
                }
            }
        }

        this.cNPC_EpicFight_Addon$activeTaczCombatRange = resolvedRange;
    }

    public void setParried(boolean isParried){
        this.isParried = isParried;
    }

    public Component getCustomName() {
        return customName;
    }

    public ResourceLocation getBossBar(){
        return this.bossBar;
    }

    public void refreshCombatStateAfterGearChange() {
        this.clearTaczSustainedFire();
        // Delay AI/goal refresh to serverTick to avoid mutating GoalSelector during iteration.
        this.pendingWeaponMotionResync = true;
    }

    public boolean hasGearInInventory(ResourceLocation itemId, @Nullable EquipmentSlot targetSlot, boolean includeEquipped) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return false;
        }

        if (includeEquipped && this.hasMatchingEquippedGear(item, targetSlot)) {
            return true;
        }

        if (this.original instanceof InventoryCarrier inventoryCarrier) {
            SimpleContainer inventory = inventoryCarrier.getInventory();

            for (int index = 0; index < inventory.getContainerSize(); index++) {
                if (this.isMatchingGear(inventory.getItem(index), item, targetSlot, null)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasAmmo(@Nullable InteractionHand hand) {
        if (hand == null) {
            return this.hasAmmo(InteractionHand.MAIN_HAND) || this.hasAmmo(InteractionHand.OFF_HAND);
        }

        ItemStack heldItem = this.original.getItemInHand(hand);
        if (heldItem.isEmpty()) {
            return false;
        }

        if (TaczCompat.isTaczGun(heldItem)) {
            return TaczCompat.hasUsableAmmo(this.original, heldItem, this.ammoSlots);
        }

        if (heldItem.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(heldItem)) {
            return true;
        }

        if (heldItem.getItem() instanceof ProjectileWeaponItem) {
            return !this.original.getProjectile(heldItem).isEmpty();
        }

        return false;
    }

    public boolean isAmmoEmpty(@Nullable InteractionHand hand) {
        InteractionHand resolvedHand = hand != null ? hand : TaczCompat.findGunHand(this.original, null);
        if (resolvedHand == null) {
            return !this.hasAmmo(hand);
        }

        ItemStack heldItem = this.original.getItemInHand(resolvedHand);
        if (heldItem.isEmpty()) {
            return true;
        }

        if (TaczCompat.isTaczGun(heldItem)) {
            return TaczCompat.isAmmoEmpty(this.original, heldItem, this.ammoSlots);
        }

        return !this.hasAmmo(resolvedHand);
    }

    public boolean isAmmoNotFull(@Nullable InteractionHand hand) {
        InteractionHand resolvedHand = hand != null ? hand : TaczCompat.findGunHand(this.original, null);
        if (resolvedHand == null) {
            return false;
        }

        ItemStack heldItem = this.original.getItemInHand(resolvedHand);
        if (heldItem.isEmpty()) {
            return false;
        }

        return TaczCompat.isTaczGun(heldItem) && TaczCompat.isAmmoNotFull(this.original, heldItem);
    }

    public boolean hasReserveAmmo(@Nullable InteractionHand hand) {
        InteractionHand resolvedHand = hand != null ? hand : TaczCompat.findGunHand(this.original, null);
        if (resolvedHand == null) {
            return false;
        }

        ItemStack heldItem = this.original.getItemInHand(resolvedHand);
        if (heldItem.isEmpty()) {
            return false;
        }

        if (TaczCompat.isTaczGun(heldItem)) {
            return TaczCompat.hasReserveAmmo(this.original, heldItem, this.ammoSlots);
        }

        return this.hasAmmo(resolvedHand);
    }

    public boolean tryStartReload(@Nullable InteractionHand hand, boolean requireAmmo, int inactionTime, float staminaCost) {
        InteractionHand reloadHand = this.resolveReloadHand(hand);
        if (reloadHand == null) {
            return false;
        }

        ItemStack heldItem = this.original.getItemInHand(reloadHand);

        boolean isTaczGun = TaczCompat.isTaczGun(heldItem);

        UseAnim useAnim = heldItem.getUseAnimation();
        if (!isTaczGun && !(heldItem.getItem() instanceof ProjectileWeaponItem) && useAnim == UseAnim.NONE) {
            return false;
        }

        if (staminaCost > 0.0F && this.getStamina() < staminaCost) {
            return false;
        }

        if (isTaczGun) {
            if (!TaczCompat.tryReload(this.original, reloadHand, requireAmmo, this.ammoSlots)) {
                return false;
            }
            this.clearTaczSustainedFire();
        } else {
            this.original.startUsingItem(reloadHand);
        }

        if (staminaCost > 0.0F) {
            this.setStamina(this.getStamina() - staminaCost);
        }

        this.setBlocking(false);
        this.setAttackSpeed(1.0F);
        this.resetActionTick();
        this.resetMotion();
        this.setInactionTime(Math.max(this.getInactionTime(), inactionTime));

        return true;
    }

    public boolean tryTaczShoot(float staminaCost, @Nullable String fireMode) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.clearActiveTaczCombatRange();
            this.clearTaczSustainedFire();
            TaczCompat.stopAiming(this.original);
            return false;
        }

        InteractionHand gunHand = TaczCompat.findGunHand(this.original, null);
        if (gunHand == null) {
            this.clearTaczSustainedFire();
            return false;
        }

        if (staminaCost > 0.0F && this.getStamina() < staminaCost) {
            return false;
        }

        // TACZ may clear its operator state during recoil or between shots.
        // Reassert aim immediately before firing so every shot has a visible
        // aim phase and the client receives the corresponding AIM motion.
        this.original.lookAt(target, 30.0F, 30.0F);
        if (!TaczCompat.isAiming(this.original) && !TaczCompat.tryAim(this.original, gunHand)) {
            return false;
        }
        this.syncTaczAimAnimation(true);

        this.taczSustainStaminaCost = staminaCost;
        this.taczSustainFireMode = fireMode;
        this.taczSustainRequested = true;

        if (!TaczCompat.tryShoot(this.original, target, gunHand, fireMode)) {
            // If blocked by empty ammo, trigger reload immediately.
            if (!TaczCompat.isReloading(this.original)
                    && TaczCompat.isAmmoEmpty(this.original, this.original.getItemInHand(gunHand), this.ammoSlots)) {
                this.clearTaczSustainedFire();
                this.tryStartReload(gunHand, false, 0, 0.0F);
            }
            return false;
        }

        this.taczSustainActive = true;
        this.applyTaczShotPostEffects(staminaCost);
        return true;
    }

    public boolean tryTaczAim(@Nullable InteractionHand preferredHand, int inactionTime, float staminaCost) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.clearActiveTaczCombatRange();
            this.clearTaczSustainedFire();
            TaczCompat.stopAiming(this.original);
            return false;
        }

        InteractionHand gunHand = TaczCompat.findGunHand(this.original, preferredHand);
        if (gunHand == null) {
            return false;
        }

        boolean alreadyAiming = TaczCompat.isAiming(this.original);

        if (!alreadyAiming && staminaCost > 0.0F && this.getStamina() < staminaCost) {
            return false;
        }

        if (TaczCompat.isReloading(this.original)) {
            this.clearTaczSustainedFire();
            return false;
        }

        this.original.lookAt(target, 30.0F, 30.0F);

        if (!TaczCompat.tryAim(this.original, gunHand)) {
            return false;
        }

        this.syncTaczAimAnimation(true);

        if (!alreadyAiming && staminaCost > 0.0F) {
            this.setStamina(this.getStamina() - staminaCost);
        }

        int effectiveInactionTime = alreadyAiming ? 0 : inactionTime;
        this.setBlocking(false);
        this.setAttackSpeed(1.0F);
        this.resetActionTick();
        this.resetMotion();
        this.setInactionTime(Math.max(this.getInactionTime(), effectiveInactionTime));
        return true;
    }

    /**
     * TACZ synchronizes its operator state, but NPC clients do not always
     * receive the same operator transition as players. Explicitly synchronize
     * the resolved Epic Fight AIM living animation so the client shows the
     * aiming pose before the shot packet arrives.
     */
    private void syncTaczAimAnimation(boolean aiming) {
        if (this.isLogicalClient()) {
            return;
        }

        if (aiming && this.taczAimAnimation == null) {
            yesman.epicfight.api.asset.AssetAccessor<? extends StaticAnimation> animation =
                    this.getAnimator().getLivingAnimation(LivingMotions.AIM, null);
            if (animation != null && !AnimationManager.checkNull(animation)) {
                this.taczAimAnimation = animation;
                this.playAnimationSynchronized(animation, 0.1F);
            }
        } else if (!aiming && this.taczAimAnimation != null) {
            this.stopPlaying(this.taczAimAnimation);
            this.taczAimAnimation = null;
        }
    }

    public boolean hasActiveTaczCombatTarget() {
        LivingEntity target = this.getTarget();
        return target != null && target.isAlive() && TaczCompat.findGunHand(this.original, null) != null;
    }

    public boolean isWithinTaczEngagementRange() {
        LivingEntity target = this.getTarget();
        InteractionHand gunHand = TaczCompat.findGunHand(this.original, null);
        if (target == null || !target.isAlive() || gunHand == null) {
            return false;
        }

        double maxRange = this.cNPC_EpicFight_Addon$activeTaczCombatRange > 0.0D
                ? this.cNPC_EpicFight_Addon$activeTaczCombatRange
                : this.getTaczEngagementMaxRange(gunHand);
        if (maxRange <= 0.0D) {
            return false;
        }

        return this.original.distanceToSqr(target.getX(), target.getY(), target.getZ()) <= maxRange * maxRange;
    }

    private double getTaczEngagementMaxRange(InteractionHand hand) {
        CapabilityItem itemCap = hand == InteractionHand.OFF_HAND
                ? this.getAdvancedHoldingItemCapability(hand)
                : this.getHoldingItemCapability(hand);
        Style style = itemCap.getStyle(this);
        String styleName = style == null ? "" : style.toString().toLowerCase(Locale.ROOT);

        return switch (styleName) {
            case "pistol", "pistol_alt", "dual_pistol", "dual_pistol_alt" -> 18.0D;
            case "shotgun", "shotgun_alt" -> 12.0D;
            case "rifle" -> 32.0D;
            case "dual_rifle" -> 24.0D;
            case "bolt_action" -> 36.0D;
            case "launcher" -> 32.0D;
            case "minigun" -> 26.0D;
            default -> isRangedWeaponCategory(this.getResolvedWeaponCategory(hand)) ? 20.0D : 0.0D;
        };
    }

    private void tickTaczSustainedFire() {
        if (!this.taczSustainRequested && !this.taczSustainActive) {
            return;
        }

        if (this.taczSustainRequested) {
            this.taczSustainKeepAliveTicks = TACZ_SUSTAIN_KEEP_ALIVE_TICKS;
        } else if (this.taczSustainKeepAliveTicks > 0) {
            this.taczSustainKeepAliveTicks--;
        }
        this.taczSustainRequested = false;

        if (this.taczSustainKeepAliveTicks <= 0 || !this.shouldMaintainTaczSustainedFire()) {
            this.clearTaczSustainedFire();
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null) {
            this.clearTaczSustainedFire();
            return;
        }

        InteractionHand gunHand = TaczCompat.findGunHand(this.original, null);
        if (gunHand == null) {
            this.clearTaczSustainedFire();
            return;
        }

        this.original.lookAt(target, 30.0F, 30.0F);
        if (!TaczCompat.isAiming(this.original) && !TaczCompat.tryAim(this.original, gunHand)) {
            this.clearTaczSustainedFire();
            return;
        }
        this.syncTaczAimAnimation(true);

        if (!TaczCompat.tryShoot(this.original, target, gunHand, this.taczSustainFireMode)) {
            ItemStack heldGun = this.original.getItemInHand(gunHand);
            if (!TaczCompat.isReloading(this.original)
                    && TaczCompat.isAmmoEmpty(this.original, heldGun, this.ammoSlots)) {
                this.clearTaczSustainedFire();
                this.tryStartReload(gunHand, false, 0, 0.0F);
            }
            return;
        }

        this.taczSustainActive = true;
        this.applyTaczShotPostEffects(this.taczSustainStaminaCost);
    }

    private void applyTaczShotPostEffects(float staminaCost) {
        if (staminaCost > 0.0F) {
            this.setStamina(this.getStamina() - staminaCost);
        }

        this.playShootingAnimation();
        this.setBlocking(false);
        this.setAttackSpeed(1.0F);
        this.resetActionTick();
        this.resetMotion();
        this.setInactionTime(0);
    }

    private boolean shouldMaintainTaczSustainedFire() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (this.interrupted || this.getEntityState().hurt() || this.isBlocking()) {
            return false;
        }

        if (TaczCompat.isReloading(this.original)) {
            return false;
        }

        return this.isWithinTaczEngagementRange();
    }

    private void clearTaczSustainedFire() {
        this.taczSustainRequested = false;
        this.taczSustainActive = false;
        this.taczSustainKeepAliveTicks = 0;
        this.taczSustainStaminaCost = 0.0F;
        this.taczSustainFireMode = null;
    }

    public boolean tryGearSwap(ResourceLocation itemId, @Nullable EquipmentSlot targetSlot, boolean allowEquipped, boolean storeOldGear,
                               boolean requiredInInventory, @Nullable ResourceLocation requiredGunId,
                               @Nullable CompoundTag generatedGearTag) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return false;
        }

        ItemStack generatedTemplate = new ItemStack(item);
        if (generatedGearTag != null) {
            generatedTemplate.setTag(generatedGearTag.copy());
        }

        for (EquipmentSlot candidateSlot : this.resolveGearSwapTargets(item, targetSlot)) {
            ItemStack currentTargetGear = this.original.getItemBySlot(candidateSlot);
            if (this.isMatchingGear(currentTargetGear, item, candidateSlot, requiredGunId)) {
                continue;
            }

            if (allowEquipped && this.trySwapEquippedGear(item, candidateSlot, storeOldGear, requiredGunId)) {
                return true;
            }

            if (this.trySwapInventoryGear(item, candidateSlot, storeOldGear, requiredGunId)) {
                return true;
            }

            if (!requiredInInventory && this.tryPlaceGeneratedGear(generatedTemplate, candidateSlot, storeOldGear)) {
                return true;
            }
        }

        return false;
    }

    /** Attempts every swap in the list. Returns true if at least one succeeded.
     *  A single {@link #refreshCombatStateAfterGearChange()} call happens for the whole batch,
     *  but only when at least one swap actually changed the equipment. */
    public boolean tryGearSwapMulti(List<GearSwapEntry> entries) {
        boolean anySwapped = false;
        // Track slots filled during this batch so earlier placements are not stolen by later entries.
        java.util.Set<EquipmentSlot> filledThisPass = new java.util.HashSet<>();
        for (GearSwapEntry e : entries) {
            Item item = ForgeRegistries.ITEMS.getValue(e.itemId());
            if (item == null) continue;
            ItemStack template = new ItemStack(item);
            if (e.nbt() != null) template.setTag(e.nbt().copy());
            boolean entrySwapped = false;
            for (EquipmentSlot candidateSlot : this.resolveGearSwapTargets(item, e.slot())) {
                // If this slot was already filled in this batch, it counts as done.
                if (filledThisPass.contains(candidateSlot)) { entrySwapped = true; break; }
                if (this.isMatchingGear(this.original.getItemBySlot(candidateSlot), item, candidateSlot, e.gunId())) {
                    filledThisPass.add(candidateSlot);
                    entrySwapped = true;
                    break;
                }
                boolean swapped;
                if (!e.requiredInInventory()) {
                    // When generating gear, place directly — never steal from other slots (that would
                    // undo a previous entry's placement within the same batch).
                    swapped = this.tryPlaceGeneratedGear(template, candidateSlot, e.storeGear())
                            || this.trySwapInventoryGear(item, candidateSlot, e.storeGear(), e.gunId());
                } else {
                    // Inventory-required path: search equipped slots, but skip any already filled this pass.
                    swapped = this.trySwapEquippedGearExcluding(item, candidateSlot, e.storeGear(), e.gunId(), filledThisPass)
                            || this.trySwapInventoryGear(item, candidateSlot, e.storeGear(), e.gunId());
                }
                if (swapped) { anySwapped = true; entrySwapped = true; filledThisPass.add(candidateSlot); break; }
            }
            if (entrySwapped) anySwapped = true;
        }
        if (anySwapped) this.refreshCombatStateAfterGearChange();
        return anySwapped;
    }

    @Nullable
    private InteractionHand resolveReloadHand(@Nullable InteractionHand preferredHand) {
        if (preferredHand != null) {
            return this.canReloadFromHand(preferredHand) ? preferredHand : null;
        }

        InteractionHand taczHand = TaczCompat.findGunHand(this.original, null);
        if (taczHand != null && this.canReloadFromHand(taczHand)) {
            return taczHand;
        }

        if (this.canReloadFromHand(InteractionHand.MAIN_HAND)) {
            return InteractionHand.MAIN_HAND;
        }

        if (this.canReloadFromHand(InteractionHand.OFF_HAND)) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    private boolean canReloadFromHand(InteractionHand hand) {
        ItemStack heldItem = this.original.getItemInHand(hand);
        if (heldItem.isEmpty()) {
            return false;
        }

        if (TaczCompat.isTaczGun(heldItem)) {
            return TaczCompat.canReload(this.original, heldItem) && !TaczCompat.isReloading(this.original);
        }

        UseAnim useAnim = heldItem.getUseAnimation();
        if (heldItem.getItem() instanceof CrossbowItem) {
            return !CrossbowItem.isCharged(heldItem) && !this.original.getProjectile(heldItem).isEmpty();
        }

        if (heldItem.getItem() instanceof ProjectileWeaponItem) {
            return !this.original.getProjectile(heldItem).isEmpty();
        }

        if (useAnim == UseAnim.NONE || useAnim == UseAnim.BOW) {
            return false;
        }

        return true;
    }

    private List<EquipmentSlot> resolveGearSwapTargets(Item item, @Nullable EquipmentSlot preferredSlot) {
        if (preferredSlot != null) {
            return List.of(preferredSlot);
        }

        ItemStack stack = new ItemStack(item);
        List<EquipmentSlot> candidates = new ArrayList<>();
        candidates.add(EquipmentSlot.MAINHAND);
        candidates.add(EquipmentSlot.OFFHAND);

        EquipmentSlot naturalSlot = Mob.getEquipmentSlotForItem(stack);
        if (naturalSlot != EquipmentSlot.MAINHAND && naturalSlot != EquipmentSlot.OFFHAND) {
            candidates.add(naturalSlot);
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!candidates.contains(slot) && this.isGearCompatibleWithSlot(stack, slot)) {
                candidates.add(slot);
            }
        }

        return candidates;
    }

    private boolean hasMatchingEquippedGear(Item item, @Nullable EquipmentSlot targetSlot) {
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            if (this.isMatchingGear(this.original.getItemBySlot(equipmentSlot), item, targetSlot, null)) {
                return true;
            }
        }

        return false;
    }

    private boolean trySwapEquippedGearExcluding(Item item, EquipmentSlot targetSlot, boolean storeOldGear,
                                                  @Nullable ResourceLocation requiredGunId,
                                                  java.util.Set<EquipmentSlot> excludedSources) {
        for (EquipmentSlot sourceSlot : EquipmentSlot.values()) {
            if (sourceSlot == targetSlot || excludedSources.contains(sourceSlot)) continue;
            ItemStack sourceStack = this.original.getItemBySlot(sourceSlot);
            if (!this.isMatchingGear(sourceStack, item, targetSlot, requiredGunId)) continue;
            ItemStack targetStack = this.original.getItemBySlot(targetSlot).copy();
            ItemStack movedStack = sourceStack.copy();
            this.original.setItemSlot(targetSlot, movedStack);
            if (storeOldGear && !targetStack.isEmpty()) {
                this.original.setItemSlot(sourceSlot, targetStack);
            } else {
                this.original.setItemSlot(sourceSlot, ItemStack.EMPTY);
            }
            this.refreshCombatStateAfterGearChange();
            return true;
        }
        return false;
    }

    private boolean trySwapEquippedGear(Item item, EquipmentSlot targetSlot, boolean storeOldGear, @Nullable ResourceLocation requiredGunId) {
        for (EquipmentSlot sourceSlot : EquipmentSlot.values()) {
            if (sourceSlot == targetSlot) {
                continue;
            }

            ItemStack sourceStack = this.original.getItemBySlot(sourceSlot);
            if (!this.isMatchingGear(sourceStack, item, targetSlot, requiredGunId)) {
                continue;
            }

            ItemStack targetStack = this.original.getItemBySlot(targetSlot).copy();
            ItemStack movedStack = sourceStack.copy();

            this.original.setItemSlot(targetSlot, movedStack);
            if (storeOldGear && !targetStack.isEmpty()) {
                this.original.setItemSlot(sourceSlot, targetStack);
            } else {
                this.original.setItemSlot(sourceSlot, ItemStack.EMPTY);
            }

            this.refreshCombatStateAfterGearChange();
            return true;
        }

        return false;
    }

    private boolean trySwapInventoryGear(Item item, EquipmentSlot targetSlot, boolean storeOldGear, @Nullable ResourceLocation requiredGunId) {
        if (!(this.original instanceof InventoryCarrier inventoryCarrier)) {
            return false;
        }

        SimpleContainer inventory = inventoryCarrier.getInventory();
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            ItemStack sourceStack = inventory.getItem(index);
            if (!this.isMatchingGear(sourceStack, item, targetSlot, requiredGunId)) {
                continue;
            }

            ItemStack targetStack = this.original.getItemBySlot(targetSlot).copy();
            ItemStack movedStack = sourceStack.copy();
            movedStack.setCount(1);
            sourceStack.shrink(1);
            inventory.setItem(index, sourceStack);
            this.original.setItemSlot(targetSlot, movedStack);

            if (storeOldGear && !targetStack.isEmpty()) {
                this.placeGearInInventoryOrDrop(targetStack);
            }

            this.refreshCombatStateAfterGearChange();
            return true;
        }

        return false;
    }

    private boolean tryPlaceGeneratedGear(ItemStack template, EquipmentSlot targetSlot, boolean storeOldGear) {
        if (template.isEmpty() || !this.isGearCompatibleWithSlot(template, targetSlot)) {
            return false;
        }

        ItemStack targetStack = this.original.getItemBySlot(targetSlot).copy();
        ItemStack movedStack = template.copy();
        movedStack.setCount(1);
        this.original.setItemSlot(targetSlot, movedStack);

        if (storeOldGear && !targetStack.isEmpty()) {
            this.placeGearInInventoryOrDrop(targetStack);
        }

        this.refreshCombatStateAfterGearChange();
        return true;
    }

    private void placeGearInInventoryOrDrop(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (this.original instanceof InventoryCarrier inventoryCarrier) {
            ItemStack leftover = inventoryCarrier.getInventory().addItem(stack.copy());
            if (leftover.isEmpty()) {
                return;
            }

            this.original.spawnAtLocation(leftover);
            return;
        }

        this.original.spawnAtLocation(stack.copy());
    }

    private boolean isMatchingGear(ItemStack stack, Item item, @Nullable EquipmentSlot targetSlot, @Nullable ResourceLocation requiredGunId) {
        if (stack.isEmpty() || !stack.is(item) || !this.isGearCompatibleWithSlot(stack, targetSlot)) {
            return false;
        }

        if (requiredGunId == null) {
            return true;
        }

        CompoundTag tag = stack.getTag();
        return tag != null && requiredGunId.toString().equals(tag.getString("GunId"));
    }

    private boolean isGearCompatibleWithSlot(ItemStack stack, @Nullable EquipmentSlot targetSlot) {
        if (targetSlot == null) {
            return true;
        }

        if (targetSlot == EquipmentSlot.MAINHAND || targetSlot == EquipmentSlot.OFFHAND) {
            return true;
        }

        return Mob.getEquipmentSlotForItem(stack) == targetSlot;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAIAsInfantry(boolean holdingRangedWeapon) {
        boolean isUsingBrain = !this.getOriginal().getBrain().availableBehaviorsByPriority.isEmpty();

        if (isUsingBrain) {
            this.removeBehaviors((Brain<T>)this.original.getBrain(), Activity.CORE, AdvancedCombatBehavior.class);
            this.removeBehaviors((Brain<T>)this.original.getBrain(), Activity.CORE, AdvancedChasingBehavior.class);
            this.removeBehaviors((Brain<T>)this.original.getBrain(), Activity.CORE, GuardBehavior.class);

            CombatBehaviors.Builder<HumanoidMobPatch<?>> builder = this.getHoldingItemWeaponMotionBuilder();

            if (builder != null) {
                this.removeBehaviors((Brain<T>)this.original.getBrain(), Activity.FIGHT, MeleeAttack.class);
                this.addBehaviors((Brain<T>)this.original.getBrain(), Activity.CORE, new AdvancedCombatBehavior<>(this, builder.build(this)));
                this.addBehaviors((Brain<T>)this.original.getBrain(), Activity.CORE, new AdvancedChasingBehavior<>(this, this.provider.getChasingSpeed(), attackRadius));
                if (!holdingRangedWeapon) {
                    this.addBehaviors((Brain<T>)this.original.getBrain(), Activity.CORE, new GuardBehavior<>(this, guardRadius));
                }
            }

            this.replaceBehaviors((Brain<T>)this.original.getBrain(), Activity.FIGHT,
                    BehaviorBuilder.triggerIf((entity) -> entity.isHolding(is -> is.getItem() instanceof CrossbowItem), BackUpIfTooCloseStopInaction.create(5, 0.75F)),
                    MeleeAttack.class);
        } else {
            this.clearAdvancedInfantryGoals();

            CombatBehaviors.Builder<HumanoidMobPatch<?>> builder = this.getHoldingItemWeaponMotionBuilder();

            if (builder != null) {
                this.original.goalSelector.addGoal(0, new AdvancedCombatGoal<>(this, builder.build(this)));
                if (!holdingRangedWeapon) {
                    this.original.goalSelector.addGoal(0, new GuardGoal(this, this.guardRadius));
                }
                this.original.goalSelector.addGoal(1, new AdvancedChasingGoal<>(this, this.getOriginal(), this.provider.getChasingSpeed(), true, this.attackRadius));
            }
        }
    }

    public <E extends LivingEntity> void addBehaviors(Brain<E> brain, Activity activity, BehaviorControl<? super E> newBehavior) {
        for (Map<Activity, Set<BehaviorControl<? super E>>> map : brain.availableBehaviorsByPriority.values()) {
            Set<BehaviorControl<? super E>> set = map.get(activity);

            if (set != null) {
                set.add(newBehavior);
            }
        }
    }

    public <E extends LivingEntity> void removeBehaviors(Brain<E> brain, Activity activity, Class<?> targetBehaviorClass) {
        for (Map<Activity, Set<BehaviorControl<? super E>>> map : brain.availableBehaviorsByPriority.values()) {
            Set<BehaviorControl<? super E>> set = map.get(activity);

            if (set != null) {
                set.removeIf(targetBehaviorClass::isInstance);
            }
        }
    }

    public <E extends LivingEntity> void replaceBehaviors(Brain<E> brain, Activity activity, BehaviorControl<? super E> newBehavior, Class<?> targetBehaviorClass) {
        for (Map<Activity, Set<BehaviorControl<? super E>>> map : brain.availableBehaviorsByPriority.values()) {
            Set<BehaviorControl<? super E>> set = map.get(activity);

            if (set != null) {
                boolean removed = set.removeIf(targetBehaviorClass::isInstance);

                if (removed) {
                    set.add(newBehavior);
                }
            }
        }
    }

    private void clearAdvancedInfantryGoals() {
        List<Goal> goalsToRemove = new ArrayList<>();

        for (WrappedGoal wrappedGoal : this.original.goalSelector.getAvailableGoals()) {
            Goal goal = wrappedGoal.getGoal();

            if (goal instanceof AdvancedCombatGoal<?> || goal instanceof GuardGoal || goal instanceof AdvancedChasingGoal<?>) {
                goalsToRemove.add(goal);
            }
        }

        for (Goal goal : goalsToRemove) {
            this.original.goalSelector.removeGoal(goal);
        }
    }

    private void refreshInfantryCombatAi() {
        if (this.original == null || this.original.level().isClientSide() || this.original.isNoAi() || this.original.getVehicle() != null) {
            return;
        }

        // Weapon motion synchronization runs from the entity living tick.
        // GoalSelector mutations must use the same safe END-of-server-tick
        // queue as provider/initAI changes.
        this.infantryCombatAiRefreshQueued = true;
        this.queueAiRebuild();
    }

    public static boolean isNativeRangedWeaponForAdvancedCombat(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem;
    }

    public WeaponCategory getResolvedWeaponCategory(InteractionHand hand) {
        CapabilityItem itemCap = hand == InteractionHand.OFF_HAND
                ? this.getAdvancedHoldingItemCapability(hand)
                : this.getHoldingItemCapability(hand);
        ItemStack heldItem = this.original.getItemInHand(hand);
        return this.resolveWeaponCategory(itemCap, heldItem);
    }

    private WeaponCategory resolveWeaponCategory(CapabilityItem itemCap, ItemStack heldItem) {
        // TACZ guns do not expose themselves as Epic Fight BOW/CROSSBOW
        // capabilities. Route them through BOW so datapack combat_behavior
        // can select tacz_aim/tacz_shoot/reload consistently.
        if (TaczCompat.isTaczGun(heldItem)) {
            return CapabilityItem.WeaponCategories.BOW;
        }
        WeaponCategory category = itemCap.getWeaponCategory();
        return category;
    }

    private static boolean isRangedWeaponCategory(WeaponCategory category) {
        return category == CapabilityItem.WeaponCategories.BOW
                || category == CapabilityItem.WeaponCategories.CROSSBOW;
    }

    protected CombatBehaviors.Builder<HumanoidMobPatch<?>> getHoldingItemWeaponMotionBuilder() {
        CapabilityItem itemCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        WeaponCategory weaponCategory = this.resolveWeaponCategory(itemCap, this.original.getMainHandItem());
        Style style = itemCap.getStyle(this);

        if (this.weaponAttackMotions != null && this.weaponAttackMotions.containsKey(weaponCategory)) {
            Map<Style, CombatBehaviors.Builder<HumanoidMobPatch<?>>> motionByStyle = this.weaponAttackMotions.get(weaponCategory);

            if (motionByStyle.containsKey(style) || motionByStyle.containsKey(CapabilityItem.Styles.COMMON)) {
                return motionByStyle.getOrDefault(style, motionByStyle.get(CapabilityItem.Styles.COMMON));
            }

            return null;
        }

        // A ranged behavior series can contain the gear swap that equips the
        // actual gun. At patch construction time the NPC may still hold a
        // placeholder item, so selecting only by the current item would leave
        // the entity with no AnimatedAttackGoal and therefore idle forever.
        // Prefer the configured BOW series for that transition, then use the
        // first configured series as a general fallback.
        if (this.weaponAttackMotions != null && !this.weaponAttackMotions.isEmpty()) {
            Map<Style, CombatBehaviors.Builder<HumanoidMobPatch<?>>> ranged =
                    this.weaponAttackMotions.get(CapabilityItem.WeaponCategories.BOW);
            if (ranged != null && !ranged.isEmpty()) {
                return ranged.getOrDefault(CapabilityItem.Styles.COMMON, ranged.values().iterator().next());
            }

            Map<Style, CombatBehaviors.Builder<HumanoidMobPatch<?>>> first =
                    this.weaponAttackMotions.values().iterator().next();
            if (first != null && !first.isEmpty()) {
                return first.getOrDefault(CapabilityItem.Styles.COMMON, first.values().iterator().next());
            }
        }

        return null;
    }

    @Override
    protected void setWeaponMotions() {
        if (this.weaponAttackMotions == null) {
            super.setWeaponMotions();
        }
    }

    protected void initAttributes() {
        this.original.getAttribute(EpicFightAttributes.WEIGHT.get()).setBaseValue(this.provider.getAttributeValues().get(EpicFightAttributes.WEIGHT.get()));
        this.original.getAttribute(EpicFightAttributes.MAX_STRIKES.get()).setBaseValue(this.provider.getAttributeValues().get(EpicFightAttributes.MAX_STRIKES.get()));
        this.original.getAttribute(EpicFightAttributes.ARMOR_NEGATION.get()).setBaseValue(this.provider.getAttributeValues().get(EpicFightAttributes.ARMOR_NEGATION.get()));
        this.original.getAttribute(EpicFightAttributes.IMPACT.get()).setBaseValue(this.provider.getAttributeValues().get(EpicFightAttributes.IMPACT.get()));
        this.original.getAttribute(EpicFightAttributes.MAX_STAMINA.get()).setBaseValue(this.provider.getAttributeValues().get(EpicFightAttributes.MAX_STAMINA.get()));
        this.original.getAttribute(EpicFightAttributes.STAMINA_REGEN.get()).setBaseValue(this.provider.getAttributeValues().get(EpicFightAttributes.STAMINA_REGEN.get()));
        this.original.getAttribute(EpicFightAttributes.OFFHAND_IMPACT.get()).setBaseValue(0.5F);
        this.original.getAttribute(EpicFightAttributes.OFFHAND_ARMOR_NEGATION.get()).setBaseValue(0F);
        this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED.get()).setBaseValue(1.2F);
        this.original.getAttribute(EpicFightAttributes.OFFHAND_MAX_STRIKES.get()).setBaseValue(1);

        if (this.provider.getAttributeValues().containsKey(Attributes.ATTACK_DAMAGE)) {
            this.original.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(this.provider.getAttributeValues().get(Attributes.ATTACK_DAMAGE));
        }
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);

        for (Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>> pair : this.provider.getDefaultAnimations()) {
            animator.addLivingAnimation(pair.getFirst(), pair.getSecond());
        }
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        super.commonAggressiveMobUpdateMotion(considerInaction);

        if (this.currentLivingMotion == LivingMotions.WALK || this.currentLivingMotion == LivingMotions.CHASE) {
            this.currentLivingMotion = this.isRunning ? LivingMotions.CHASE : LivingMotions.WALK;
            this.currentCompositeMotion = this.currentLivingMotion;
        }

        InteractionHand taczHand = TaczCompat.findGunHand(this.original, this.original.getUsedItemHand());
        boolean hasTaczGun = taczHand != null;
        boolean taczReloading = hasTaczGun && TaczCompat.isReloading(this.original);
        boolean taczAiming = hasTaczGun && TaczCompat.isAiming(this.original);

        if (this.original.isUsingItem()) {
            CapabilityItem activeItem = this.getHoldingItemCapability(this.original.getUsedItemHand());
            ItemStack activeStack = this.original.getItemInHand(this.original.getUsedItemHand());
            UseAnim useAnim = this.original.getItemInHand(this.original.getUsedItemHand()).getUseAnimation();
            UseAnim secondUseAnim = activeItem.getUseAnimation(this);

            if (useAnim == UseAnim.BLOCK || secondUseAnim == UseAnim.BLOCK)
                if (activeItem.getWeaponCategory() == CapabilityItem.WeaponCategories.SHIELD)
                    currentCompositeMotion = LivingMotions.BLOCK_SHIELD;
                else
                    currentCompositeMotion = LivingMotions.BLOCK;
            else if (TaczCompat.isTaczGun(activeStack))
                currentCompositeMotion = taczReloading
                        ? LivingMotions.RELOAD
                        : (taczAiming ? LivingMotions.AIM : currentLivingMotion);
            else if (useAnim == UseAnim.BOW || useAnim == UseAnim.SPEAR)
                currentCompositeMotion = LivingMotions.AIM;
            else if (useAnim == UseAnim.CROSSBOW)
                currentCompositeMotion = LivingMotions.RELOAD;
            else
                currentCompositeMotion = currentLivingMotion;
        } else {
            if(this.isBlocking()) currentCompositeMotion = LivingMotions.BLOCK;
            else if (taczReloading)
                currentCompositeMotion = LivingMotions.RELOAD;
            else if (taczAiming)
                currentCompositeMotion = LivingMotions.AIM;
            else if (CrossbowItem.isCharged(this.original.getMainHandItem()))
                currentCompositeMotion = LivingMotions.AIM;
            else if (this.getClientAnimator().getCompositeLayer(Layer.Priority.MIDDLE).animationPlayer.getAnimation().get().isReboundAnimation())
                currentCompositeMotion = LivingMotions.NONE;
            else if (this.original.swinging && this.original.getSleepingPos().isEmpty())
                currentCompositeMotion = LivingMotions.DIGGING;
            else
                currentCompositeMotion = currentLivingMotion;
        }
    }

    @Override
    public AttackResult attack(EpicFightDamageSource damageSource, Entity target, InteractionHand hand) {
        AttackResult result = super.attack(damageSource, target, hand);
        if(result.resultType.dealtDamage() && this.getEventManager().hasHitEvent()){
            for(CommandEvent.BiEvent event: this.getEventManager().getHitEventList()) {
                event.testAndExecute(this, target);
                if(!this.getOriginal().isAlive() || !this.getEventManager().hasHitEvent()){break;}
            }
        }
        return result;
    }

    @Override
    public OpenMatrix4f getModelMatrix(float partialTicks) {
        float scale = this.provider.getScale();
        return super.getModelMatrix(partialTicks).scale(scale, scale, scale);
    }
    @Override
    public void setStunReductionOnHit(StunType stunType) {
        if(this.hasStunReduction){
            super.setStunReductionOnHit(stunType);
        }
    }
    @Override
    public float getStunReduction() {
        if(this.hasStunReduction){
            return super.getStunReduction();
        }
        return 0;
    }
    @Override
    public void modifyLivingMotionByCurrentItem(boolean onStartTracking) {
        Map<LivingMotion, yesman.epicfight.api.asset.AssetAccessor<? extends StaticAnimation>> oldLivingAnimations = this.getAnimator().getLivingAnimations();
        Map<LivingMotion, yesman.epicfight.api.asset.AssetAccessor<? extends StaticAnimation>> newLivingAnimations = Maps.newHashMap();

        CapabilityItem mainhandCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        CapabilityItem offhandCap = this.getAdvancedHoldingItemCapability(InteractionHand.OFF_HAND);
        WeaponCategory mainhandCategory = this.resolveWeaponCategory(mainhandCap, this.original.getMainHandItem());

        // Native held-item motions form the base. Datapack defaults fill only
        // missing entries; humanoid_weapon_motions remains the explicit override.
        newLivingAnimations.putAll(offhandCap.getLivingMotionModifier(this, InteractionHand.OFF_HAND));
        newLivingAnimations.putAll(mainhandCap.getLivingMotionModifier(this, InteractionHand.MAIN_HAND));

        // A datapack default is a fallback, not a replacement for the held
        // item's native capability motions. This is essential for TACZ and
        // Epic Arsenal: their player animations are supplied by the held item
        // capability and must remain available to mob animators as well.
        for (Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>> pair : this.provider.getDefaultAnimations()) {
            newLivingAnimations.putIfAbsent(pair.getFirst(), pair.getSecond());
        }

        if (this.weaponLivingMotions != null && this.weaponLivingMotions.containsKey(mainhandCategory)) {
            Map<Style, Set<Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>>>> mapByStyle = this.weaponLivingMotions.get(mainhandCategory);
            Style style = mainhandCap.getStyle(this);
            this.applyLivingMotionOverrides(newLivingAnimations, mapByStyle, CapabilityItem.Styles.COMMON);
            this.applyLivingMotionOverrides(newLivingAnimations, mapByStyle, style);
        }

        currentGuardMotion = this.getGuardMotion(mainhandCap, mainhandCategory);
        newLivingAnimations.put(LivingMotions.BLOCK, currentGuardMotion.guardAnimation);

        boolean hasChange = oldLivingAnimations.size() != newLivingAnimations.size();

        if (!hasChange) {
            for (Map.Entry<LivingMotion, yesman.epicfight.api.asset.AssetAccessor<? extends StaticAnimation>> entry : newLivingAnimations.entrySet()) {
                if (oldLivingAnimations.get(entry.getKey()) != entry.getValue()) {
                    hasChange = true;
                    break;
                }
            }
        }

        if (hasChange || onStartTracking) {
            this.getAnimator().resetLivingAnimations();
            newLivingAnimations.forEach(this.getAnimator()::addLivingAnimation);

            SPChangeLivingMotion msg = new SPChangeLivingMotion(this.original.getId());
            msg.putEntries(newLivingAnimations.entrySet());
            EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(msg, this.original);
        }
    }

    private void applyLivingMotionOverrides(
            Map<LivingMotion, yesman.epicfight.api.asset.AssetAccessor<? extends StaticAnimation>> target,
            Map<Style, Set<Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>>>> mapByStyle,
            @Nullable Style style) {
        if (style == null) {
            return;
        }

        Set<Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>>> overrides = mapByStyle.get(style);
        if (overrides == null) {
            return;
        }

        for (Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>> pair : overrides) {
            target.put(pair.getFirst(), pair.getSecond());
        }
    }

    private GuardMotion getGuardMotion(CapabilityItem itemCap, WeaponCategory weaponCategory){
        if(this.specificGuardMotion != null){
            return this.specificGuardMotion;
        }

        if(this.weaponGuardMotions != null && itemCap != null){
            Style style = itemCap.getStyle(this);
            Map<Style, GuardMotion> mapByStyle = this.weaponGuardMotions.get(weaponCategory);
            if (mapByStyle != null && (mapByStyle.containsKey(style) || mapByStyle.containsKey(CapabilityItem.Styles.COMMON))) {
                return mapByStyle.getOrDefault(style, mapByStyle.get(CapabilityItem.Styles.COMMON));
            }
        }
        return new GuardMotion(this.getDefaultGuardAnimationAccessor(), false,1);
    }

    public float getGuardCostMultiply(){ return currentGuardMotion.cost;}

    public CustomGuardAnimation getGuardAnimation(){
        if(currentGuardMotion.guardAnimation.get() instanceof CustomGuardAnimation guardAnimation){
            return guardAnimation;
        }
        if (GuardAnimations.MOB_LONGSWORD_GUARD instanceof CustomGuardAnimation guardAnimation) {
            return guardAnimation;
        }

        return new CustomGuardAnimation("epicfight:biped/skill/guard_longsword", "epicfight:biped/skill/guard_longsword_hit", Armatures.BIPED);
    }

    private AnimationAccessor<? extends StaticAnimation> getDefaultGuardAnimationAccessor() {
        if (GuardAnimations.MOB_LONGSWORD_GUARD != null) {
            return AnimationManager.byKey(GuardAnimations.MOB_LONGSWORD_GUARD.getRegistryName());
        }

        return Animations.LONGSWORD_GUARD;
    }
    public boolean canBlockProjectile(){
        return currentGuardMotion.canBlockProjectile;
    }
    public float getParryCostMultiply(){return currentGuardMotion.parry_cost;}
    public AnimationAccessor<? extends StaticAnimation> getParryAnimation(){
        AnimationAccessor<? extends StaticAnimation>[] parry_animation = currentGuardMotion.parry_animation != null ? currentGuardMotion.parry_animation : defaultParryAnimations();
        return parry_animation[this.parryTimes % parry_animation.length];
    }

    @SuppressWarnings("unchecked")
    private static AnimationAccessor<? extends StaticAnimation>[] defaultParryAnimations() {
        return new AnimationAccessor[]{Animations.LONGSWORD_GUARD_ACTIVE_HIT1, Animations.LONGSWORD_GUARD_ACTIVE_HIT2};
    }

    public SoundEvent getHitSound(InteractionHand hand) {
        CapabilityItem itemCap = this.getAdvancedHoldingItemCapability(hand);
        return itemCap.isEmpty() ? this.provider.getHitSound() : itemCap.getHitSound();
    }

    public SoundEvent getSwingSound(InteractionHand hand) {
        CapabilityItem itemCap = this.getAdvancedHoldingItemCapability(hand);
        return itemCap.isEmpty() ? this.provider.getSwingSound() : itemCap.getSmashingSound();
    }

    public HitParticleType getWeaponHitParticle(InteractionHand hand) {
        CapabilityItem itemCap = this.getAdvancedHoldingItemCapability(hand);
        return itemCap.isEmpty() ? this.provider.getHitParticle() : itemCap.getHitParticle();
    }

    @Override
    public AttackResult tryHurt(DamageSource damageSource, float amount) {
        AttackResult result = AttackResult.of(this.getEntityState().attackResult(damageSource), amount);
        if(result.resultType.dealtDamage()){
           result = damageSource.getDirectEntity() != this.getOriginal() ? this.tryProcess(damageSource, amount) : result;
           if(result.resultType.dealtDamage()){
               this.lastAttacker = damageSource.getDirectEntity();
               if(damageSource instanceof EpicFightDamageSource efDamageSource) {
                   this.lastGetImpact = efDamageSource.calculateImpact();
               } else {
                   this.lastGetImpact = amount/3;
               }
           }
        }
        return result;
    }

    private boolean canBlockSource(DamageSource damageSource){
        return  !damageSource.is(DamageTypeTags.IS_EXPLOSION)
                && !damageSource.is(DamageTypes.MAGIC)
                && !damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && (!damageSource.is(DamageTypeTags.IS_PROJECTILE) || this.canBlockProjectile());
    }

    private AttackResult tryProcess(DamageSource damageSource, float amount){
        //TRY BLOCK
        if (this.isBlocking()) {
            CustomGuardAnimation animation = this.getGuardAnimation();
            AnimationAccessor<? extends StaticAnimation> success = animation.successAnimation != null ? AnimationManager.byKey(animation.successAnimation) : Animations.SWORD_GUARD_HIT;
            boolean isFront = false;
            Vec3 sourceLocation = damageSource.getSourcePosition();

            if (sourceLocation != null) {
                Vec3 viewVector = this.getOriginal().getViewVector(1.0F);
                Vec3 toSourceLocation = sourceLocation.subtract(this.getOriginal().position()).normalize();

                if (toSourceLocation.dot(viewVector) > 0.0D) {
                    isFront = true;
                }
            }
            if (this.canBlockSource(damageSource) && isFront) {
                float impact;
                float knockback;
                if (damageSource instanceof EpicFightDamageSource efDamageSource) {
                    impact = efDamageSource.calculateImpact();
                    if(efDamageSource.is(EpicFightDamageTypeTags.GUARD_PUNCTURE)){
                        return new AttackResult(AttackResult.ResultType.SUCCESS, amount);
                    }
                } else {
                    impact = amount / 3;
                }
                knockback = 0.25F + Math.min(impact * 0.1F, 1.0F);
                if (damageSource.getDirectEntity() instanceof LivingEntity targetEntity) {
                    knockback += EnchantmentHelper.getKnockbackBonus(targetEntity) * 0.1F;
                }
                float cost = this.isParry ? this.getParryCostMultiply() : this.getGuardCostMultiply();
                float stamina = this.getStamina() - impact * cost;
                this.setStamina(stamina);
                EpicFightParticles.HIT_BLUNT.get().spawnParticleWithArgument(((ServerLevel) this.getOriginal().level()), HitParticleType.FRONT_OF_EYES, HitParticleType.ZERO, this.getOriginal(), damageSource.getDirectEntity());
               //success
                if (stamina >= 0F) {
                    float counter_cost = this.getCounterStamina();
                    var random = this.getOriginal().getRandom();
                    this.rotateTo(damageSource.getDirectEntity(),30F,true);
                    if (random.nextFloat() < this.getCounterChance() && stamina >= counter_cost) {
                        if(this.stun_immunity_time > 0){
                            this.getOriginal().addEffect(new MobEffectInstance(EpicFightMobEffects.STUN_IMMUNITY.get(), this.stun_immunity_time));
                        }
                        this.setAttackSpeed(this.getCounterSpeed());
                        this.playAnimationSynchronized(this.getCounter(),0);
                        this.playSound(EpicFightSounds.CLASH.get(), -0.05F, 0.1F);
                        //this.knockBackEntity(damageSource.getDirectEntity().position(), 0.1F);
                        if(this.cancel_block){this.setBlocking(false);}
                        this.setStamina(this.getStamina() - counter_cost);
                        //counter
                    } else if (this.isParry){
                        if(this.parryCounter + 1 >= this.maxParryTimes) {
                            this.setBlocking(false);
                            if(this.stun_immunity_time > 0){
                                this.getOriginal().addEffect(new MobEffectInstance(EpicFightMobEffects.STUN_IMMUNITY.get(), this.stun_immunity_time));
                            }
                        }
                        this.playAnimationSynchronized(this.getParryAnimation(), 0F);
                        this.parryCounter += 1;
                        this.parryTimes += 1;
                        this.playSound(EpicFightSounds.CLASH.get(), -0.05F, 0.1F);
                        this.knockBackEntity(damageSource.getDirectEntity().position(), 0.4F * knockback);
                    } else {
                        this.playAnimationSynchronized(success, 0.1F);
                        this.playSound(animation.isShield ? SoundEvents.SHIELD_BLOCK : EpicFightSounds.CLASH.get(), -0.05F, 0.1F);
                        this.knockBackEntity(damageSource.getDirectEntity().position(), knockback);
                    }
                    if(damageSource.getDirectEntity() instanceof LivingEntity living) {
                        AdvancedCustomHumanoidMobPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(living, AdvancedCustomHumanoidMobPatch.class);
                        if(targetPatch != null){
                            targetPatch.setParried(this.isParry);
                            targetPatch.onAttackBlocked(damageSource, this);}
                        }
                    return new AttackResult(AttackResult.ResultType.BLOCKED, amount);
                    //break
                } else {
                    this.setBlocking(false);
                    this.applyStun(StunType.NEUTRALIZE,2.0F);
                    this.playSound(EpicFightSounds.NEUTRALIZE_MOBS.get(), -0.05F, 0.1F);
                    EpicFightParticles.AIR_BURST.get().spawnParticleWithArgument((ServerLevel) this.getOriginal().level(),this.getOriginal(), damageSource.getDirectEntity());
                    this.setStamina(this.getMaxStamina());
                    return new AttackResult(AttackResult.ResultType.SUCCESS, amount/2);
                }
            }
        }
        return new AttackResult(AttackResult.ResultType.SUCCESS, amount);
    }

    @Override
    public EpicFightDamageSource getDamageSource(AnimationAccessor<? extends StaticAnimation> animation, InteractionHand hand) {
        EpicFightDamageSource damagesource = EpicFightDamageSources.mobAttack(this.original)
                .setAnimation(animation)
                .setBaseImpact(this.getImpact(hand))
                .setBaseArmorNegation(this.getArmorNegation(hand))
                .setUsedItem(this.original.getItemInHand(hand));

        if(this.damageSourceModifier != null){
            damagesource.setBaseImpact(this.getImpact(hand) * damageSourceModifier.impact());
            damagesource.setBaseArmorNegation(Math.min(100, this.getArmorNegation(hand) * damageSourceModifier.armor_negation()));
            if(damageSourceModifier.stunType != null){damagesource.setStunType(this.damageSourceModifier.stunType);}
        }
        return damagesource;
    }
    @Override
    public Collider getColliderMatching(InteractionHand hand) {
        if(this.damageSourceModifier != null && this.damageSourceModifier.collider !=null) {
            return this.damageSourceModifier.collider;
        }
        return this.getAdvancedHoldingItemCapability(hand).getWeaponCollider();
    }


    @Override
    public void onDeath(LivingDeathEvent event) {
        this.resetMotion();
        this.setBlocking(false);
        this.getAnimator().playDeathAnimation();
        this.currentLivingMotion = LivingMotions.DEATH;
        if (this.hasBossBar && !this.getOriginal().isRemoved()) {
            bossInfo.update();
        }
    }
    @Override
    public float getModifiedBaseDamage(float baseDamage) {
        if(this.damageSourceModifier != null) {
            baseDamage *= damageSourceModifier.damage();
        }
        return baseDamage;
    }

    @Override
    public boolean applyStun(StunType stunType, float time){
        if(this.neutralized) {
            stunType = stunType == StunType.KNOCKDOWN ? stunType : StunType.NONE;
        } else if (this.staminaLoseMultiply > 0 && this.lastGetImpact > 0 && this.getStunShield() <= 0){
            this.setStamina(this.getStamina() - this.lastGetImpact * this.staminaLoseMultiply);
            if (this.getStamina() <  this.lastGetImpact * this.staminaLoseMultiply) {
                stunType = StunType.NEUTRALIZE;
                this.playSound(EpicFightSounds.NEUTRALIZE_MOBS.get(), -0.05F, 0.1F);
                if(this.lastAttacker != null)EpicFightParticles.AIR_BURST.get().spawnParticleWithArgument((ServerLevel) this.getOriginal().level(),this.getOriginal(), lastAttacker);
                this.setStamina(this.getMaxStamina());
            }
        }

        if(this.getEventManager().hasStunEvent()){
            if(this.getHitAnimation(stunType) != null){
                for(CommandEvent.StunEvent event: this.getEventManager().getStunEvents()) {
                    event.testAndExecute(this, lastAttacker, stunType.ordinal());
                    if(!this.getOriginal().isAlive() || !this.getEventManager().hasStunEvent()){break;}
                 }
            }
        }
        if(stunType != StunType.NONE) {
            this.setAttackSpeed(1F);
            this.resetActionTick();
            this.resetMotion();
        }
        boolean isStunned = super.applyStun(stunType, time);
        if(stunType == StunType.NEUTRALIZE){
            this.neutralized = true;
        }

        return isStunned;
    }

    @Override
    public void onFall(LivingFallEvent event) {
        if (!this.getOriginal().level().isClientSide() && (this.isAirborneState() || (EpicFightGameRules.HAS_FALL_ANIMATION.getRuleValue(this.getOriginal().level())
                && event.getDamageMultiplier() > 0.0F) && !this.getEntityState().inaction())) {

            if (this.isAirborneState() || event.getDistance() > 5.0F) {
                var fallAnimation = this.getAnimator().getLivingAnimation(LivingMotions.LANDING_RECOVERY, this.getHitAnimation(StunType.FALL));

                if (fallAnimation != null) {
                    this.playAnimationSynchronized(fallAnimation, 0);
                    this.setAttackSpeed(1F);
                    this.resetActionTick();
                    this.resetMotion();
                    if(this.getEventManager().hasStunEvent()){
                        for(CommandEvent.StunEvent stunEvent: this.getEventManager().getStunEvents()) {
                            stunEvent.testAndExecute(this, lastAttacker, StunType.FALL.ordinal());
                            if(!this.getOriginal().isAlive() || !this.getEventManager().hasStunEvent()){break;}
                        }
                    }
                }
            }
        }

        this.setAirborneState(false);
    }
    @Override
    public void knockBackEntity(Vec3 sourceLocation, float power) {
        if(this.neutralized) {
            return;
        }
        super.knockBackEntity(sourceLocation,power);
    }
    @Override
    public AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
        return this.provider.getStunAnimations().get(stunType);
    }

    @Override
    public void onStartTracking(ServerPlayer trackingPlayer) {
        super.onStartTracking(trackingPlayer);
        this.modifyLivingMotionByCurrentItem(true);
        if(this.hasBossBar) {
            this.bossInfo.addPlayer(trackingPlayer);
        }
    }

    public void forceWeaponMotionResync() {
        this.lastMainHandCategory = null;
        this.lastMainHandStyle = null;
        this.lastOffHandCategory = null;
        this.lastOffHandStyle = null;
        this.lastBlockingState = !this.isBlocking();
        this.modifyLivingMotionByCurrentItem(false);
        this.refreshInfantryCombatAi();
        this.cacheCurrentWeaponMotionState();
    }

    private void syncWeaponLivingMotionsIfNeeded() {
        CapabilityItem mainhandCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        CapabilityItem offhandCap = this.getAdvancedHoldingItemCapability(InteractionHand.OFF_HAND);
        WeaponCategory currentMainHandCategory = this.resolveWeaponCategory(mainhandCap, this.original.getMainHandItem());
        Style currentMainHandStyle = mainhandCap.getStyle(this);
        WeaponCategory currentOffHandCategory = this.resolveWeaponCategory(offhandCap, this.original.getOffhandItem());
        Style currentOffHandStyle = offhandCap.getStyle(this);
        boolean blocking = this.isBlocking();

        if (currentMainHandCategory != this.lastMainHandCategory
                || currentMainHandStyle != this.lastMainHandStyle
                || currentOffHandCategory != this.lastOffHandCategory
                || currentOffHandStyle != this.lastOffHandStyle
                || blocking != this.lastBlockingState) {
            this.modifyLivingMotionByCurrentItem(false);
            this.refreshInfantryCombatAi();
            this.cacheCurrentWeaponMotionState();
        }
    }

    private void cacheCurrentWeaponMotionState() {
        CapabilityItem mainhandCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        CapabilityItem offhandCap = this.getAdvancedHoldingItemCapability(InteractionHand.OFF_HAND);
        this.lastMainHandCategory = this.resolveWeaponCategory(mainhandCap, this.original.getMainHandItem());
        this.lastMainHandStyle = mainhandCap.getStyle(this);
        this.lastOffHandCategory = this.resolveWeaponCategory(offhandCap, this.original.getOffhandItem());
        this.lastOffHandStyle = offhandCap.getStyle(this);
        this.lastBlockingState = this.isBlocking();
    }

    public void onStopTracking(ServerPlayer trackingPlayer) {
        if(this.hasBossBar) this.bossInfo.removePlayer(trackingPlayer);
        if(this.isLogicalClient()){BossBarGUi.BossBarEntities.remove(this.bossInfo.getId());}
    }

    public void processSpawnData(ByteBuf buf) {
            long mostSignificant = buf.readLong();
            long leastSignificant = buf.readLong();
            UUID uuid = new UUID(mostSignificant, leastSignificant);
            BossBarGUi.BossBarEntities.put(uuid, this);
    }

    @Override
    public void onAttackBlocked(DamageSource damageSource, LivingEntityPatch<?> livingEntityPatch) {
        if(this.getEventManager().hasBlockEvents()){
            for(CommandEvent.BlockedEvent event: this.getEventManager().getBlockedEvents()) {
                event.testAndExecute(this, livingEntityPatch.getOriginal(), this.isParried);
                if(!this.getOriginal().isAlive() || !this.getEventManager().hasBlockEvents()){break;}
            }
        }
        this.isParried = false;
    }

    public record CustomAnimationMotion(AnimationAccessor<? extends StaticAnimation> animation, float convertTime, float speed, float stamina) { }
    public record CounterMotion(AnimationAccessor<? extends StaticAnimation> counter, float cost, float chance, float speed) {}
    public record DamageSourceModifier(float damage, float impact, float armor_negation, @Nullable StunType stunType, @Nullable
                                       Collider collider){ }
    /** One entry in a list-style {@code gear_swap} action. */
    public record GearSwapEntry(
            ResourceLocation itemId,
            @Nullable EquipmentSlot slot,
            boolean storeGear,
            boolean requiredInInventory,
            @Nullable ResourceLocation gunId,
            @Nullable CompoundTag nbt) { }

    public static class GuardMotion{
        private final AnimationAccessor<? extends StaticAnimation> guardAnimation;
        private final boolean canBlockProjectile;
        private final float cost;
        private final float parry_cost;
        private final AnimationAccessor<? extends StaticAnimation>[] parry_animation;
        public GuardMotion(AnimationAccessor<? extends StaticAnimation> guard_animation, boolean canBlockProjectile, float cost, float parry_cost, AnimationAccessor<? extends StaticAnimation>[] parry_animation){
            this.guardAnimation = guard_animation;
            this.canBlockProjectile = canBlockProjectile;
            this.cost = cost;
            this.parry_cost = parry_cost;
            this.parry_animation = parry_animation;
        }
        public GuardMotion(AnimationAccessor<? extends StaticAnimation> guard_animation, boolean canBlockProjectile, float cost){
            this.guardAnimation = guard_animation;
            this.canBlockProjectile = canBlockProjectile;
            this.cost = cost;
            this.parry_cost = 0.5F;
            this.parry_animation = defaultParryAnimations();
        }
    }
}
