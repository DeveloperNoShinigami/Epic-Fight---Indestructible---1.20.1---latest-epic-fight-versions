package com.nameless.indestructible.main;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.nameless.indestructible.client.UIConfig;
import com.nameless.indestructible.client.gui.BossBarGUi;
import com.nameless.indestructible.client.gui.StatusIndicator;
import com.nameless.indestructible.command.AHPatchPlayAnimationCommand;
import com.nameless.indestructible.command.AHPatchSetLookAtCommand;
import com.nameless.indestructible.command.AHPatchSetPhaseCommand;
import com.nameless.indestructible.data.AdvancedMobpatchReloader;
import com.nameless.indestructible.gameasset.GuardAnimations;
import com.nameless.indestructible.util.IMixinCapabilityDispatcher;
import com.nameless.indestructible.network.SPDatapackSync;
import com.nameless.indestructible.server.CommonConfig;
import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.client.gui.EntityUI;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.provider.EntityPatchProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.lang.reflect.Method;

@Mod(Indestructible.MOD_ID)
public class Indestructible {
    public static final String MOD_ID = "indestructible";
    public static final Logger LOGGER = LogManager.getLogger(Indestructible.MOD_ID);
    public static List<StaticAnimation> NEUTRALIZE_ANIMATION_LIST = new ArrayList<>();
    private static final Map<net.minecraft.world.entity.EntityType<?>, Function<Entity, Supplier<EntityPatch<?>>>> ORIGINAL_PATCH_PROVIDERS = new HashMap<>();
    private static final Map<net.minecraft.world.entity.EntityType<?>, Function<Entity, Supplier<EntityPatch<?>>>> CURRENT_PATCH_PROVIDERS = new HashMap<>();
    private static final Map<net.minecraft.world.entity.EntityType<?>, Function<Entity, Supplier<EntityPatch<?>>>> PREVIOUS_PATCH_PROVIDERS = new HashMap<>();
    private static final Method CAPABILITY_PROVIDER_GET_CAPABILITIES_METHOD;

    static {
        try {
            CAPABILITY_PROVIDER_GET_CAPABILITIES_METHOD = net.minecraftforge.common.capabilities.CapabilityProvider.class.getDeclaredMethod("getCapabilities");
            CAPABILITY_PROVIDER_GET_CAPABILITIES_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to resolve CapabilityProvider#getCapabilities", e);
        }
    }
    public Indestructible(){
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(GuardAnimations::registerAnimations);
        bus.addListener(this::doCommonStuff);
        bus.addListener(this::doClientStuff);
        MinecraftForge.EVENT_BUS.addListener(this::reloadListnerEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onDatapackSync);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::stopTrackingEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onEntityJoinLevel);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingTick);
    }

    private void doCommonStuff(final FMLCommonSetupEvent event) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        EpicFightNetworkManager.INSTANCE.registerMessage(99, SPDatapackSync.class, SPDatapackSync::toBytes, SPDatapackSync::fromBytes, SPDatapackSync::handle);
    }
    private void doClientStuff(final FMLClientSetupEvent event){
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, UIConfig.SPEC);
        new StatusIndicator();
        MinecraftForge.EVENT_BUS.register(new BossBarGUi());
    }

    private void reloadListnerEvent(final AddReloadListenerEvent event) {
        event.addListener(new AdvancedMobpatchReloader());
        NEUTRALIZE_ANIMATION_LIST.clear();
        CommonConfig.neutralizeAnimations().forEach((obj) -> {
            var animation = AnimationManager.byKey(obj);

            if (animation != null) {
                NEUTRALIZE_ANIMATION_LIST.add(animation.get());
            }
        });
    }

    private void onDatapackSync(final OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if(player != null){
            if (!player.getServer().isSingleplayerOwner(player.getGameProfile())) {
                SPDatapackSync mobPatchPacket = new SPDatapackSync(AdvancedMobpatchReloader.getTagCount());
                AdvancedMobpatchReloader.getDataStream().forEach(mobPatchPacket::write);
                EpicFightNetworkManager.sendToPlayer(mobPatchPacket, player);
            }
        } else {
            event.getPlayerList().getPlayers().forEach((serverPlayer -> {
                SPDatapackSync mobPatchPacket = new SPDatapackSync(AdvancedMobpatchReloader.getTagCount());
                AdvancedMobpatchReloader.getDataStream().forEach(mobPatchPacket::write);
                EpicFightNetworkManager.sendToPlayer(mobPatchPacket, serverPlayer);
            }));
        }

}
    private void registerCommands(final RegisterCommandsEvent event){
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal(Indestructible.MOD_ID)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("living_entity", EntityArgument.entity())
                                .then(AHPatchSetPhaseCommand.register())
                                .then(AHPatchSetLookAtCommand.register())
                                .then(AHPatchPlayAnimationCommand.register()))
        );
    }

    private void stopTrackingEvent(PlayerEvent.StopTracking event) {
        Entity trackingTarget = event.getTarget();
        AdvancedCustomHumanoidMobPatch<?> achPatch = EpicFightCapabilities.getEntityPatch(trackingTarget, AdvancedCustomHumanoidMobPatch.class);

        if (achPatch != null) {
            achPatch.onStopTracking((ServerPlayer)event.getEntity());
        }
    }

    private void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!(entity instanceof net.minecraft.world.entity.Mob)) return;
        if (this.isCustomNpcsEntity(entity)) {
            this.logCustomNpcsSkipOnce(entity, "join");
            return;
        }

        this.tryApplyNbtProvider(entity, event, "join");
    }

    private void onLivingTick(LivingEvent.LivingTickEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!(entity instanceof net.minecraft.world.entity.Mob)) return;
        if (this.isCustomNpcsEntity(entity)) {
            this.logCustomNpcsSkipOnce(entity, "tick");
            return;
        }
        if (entity.tickCount % 20 != 0) return;

        boolean matchesDatapackNbtTag = this.findMatchingNbtProvider(entity) != null;
        EntityPatch<?> currentPatch = EpicFightCapabilities.getEntityPatch(entity, EntityPatch.class);
        boolean isAdvanced = currentPatch instanceof AdvancedCustomHumanoidMobPatch<?>;

        if (matchesDatapackNbtTag && !isAdvanced) {
            this.tryApplyNbtProvider(entity, new EntityJoinLevelEvent(entity, entity.level()), "tick");
        } else if (!matchesDatapackNbtTag && isAdvanced && this.hasTrackedProviderOverride(entity)) {
            this.tryRestoreOriginalPatch(entity, new EntityJoinLevelEvent(entity, entity.level()), "tick");
        }
    }

    private boolean hasTrackedProviderOverride(Entity entity) {
        net.minecraft.world.entity.EntityType<?> entityType = entity.getType();
        Function<Entity, Supplier<EntityPatch<?>>> currentProvider = CURRENT_PATCH_PROVIDERS.get(entityType);
        Function<Entity, Supplier<EntityPatch<?>>> originalProvider = ORIGINAL_PATCH_PROVIDERS.get(entityType);

        if (currentProvider == null || originalProvider == null) {
            return false;
        }

        return currentProvider != originalProvider;
    }

    private boolean isCustomNpcsEntity(Entity entity) {
        net.minecraft.resources.ResourceLocation typeKey = net.minecraft.world.entity.EntityType.getKey(entity.getType());
        return typeKey != null && "customnpcs".equals(typeKey.getNamespace());
    }

    private void logCustomNpcsSkipOnce(Entity entity, String source) {
        String logKey = MOD_ID + ":cnpc_skip_logged";
        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.getBoolean(logKey)) {
            return;
        }

        LOGGER.info("[Advanced Mobpatch] Skipping dynamic NBT patch checks for CNPC entity on {}: type={} id={}",
                source,
                entity.getType(),
                entity.getId());
        persistentData.putBoolean(logKey, true);
    }

    private AdvancedMobpatchReloader.AdvancedCustomHumanoidMobPatchProvider findMatchingNbtProvider(Entity entity) {
        CompoundTag entityNbt = entity.serializeNBT();
        for (AdvancedMobpatchReloader.AdvancedCustomHumanoidMobPatchProvider provider : AdvancedMobpatchReloader.getNbtTagProviders()) {
            CompoundTag nbtMatcher = provider.getNbtMatcher();
            if (nbtMatcher != null && NbtUtils.compareNbt(nbtMatcher, entityNbt, true)) {
                return provider;
            }
        }
        return null;
    }

    private void tryApplyNbtProvider(Entity entity, EntityJoinLevelEvent event, String source) {
        EntityPatch<?> beforePatch = EpicFightCapabilities.getEntityPatch(entity, EntityPatch.class);
        String beforePatchName = beforePatch != null ? beforePatch.getClass().getName() : "null";

        AdvancedMobpatchReloader.AdvancedCustomHumanoidMobPatchProvider provider = this.findMatchingNbtProvider(entity);
        if (provider != null) {
            // NBT-tag providers are loaded without a fixed entity type, so bind armature at runtime.
            provider.registerArmature(entity.getType());

            this.cacheOriginalProvider(entity);

            Function<Entity, Supplier<EntityPatch<?>>> advancedProvider = (ent) -> () -> provider.get(ent);
            this.setCurrentProvider(entity.getType(), advancedProvider);
            EntityPatchProvider.putCustomEntityPatch(entity.getType(), this.getCurrentProvider(entity.getType()));

            try {
                swapEntityPatchProvider(entity, event);
            } catch (Throwable throwable) {
                LOGGER.error("[Advanced Mobpatch] Failed to swap provider safely: type={} id={}",
                        entity.getType(), entity.getId(), throwable);
                return;
            }

            EntityPatch<?> refreshedPatch = EpicFightCapabilities.getEntityPatch(entity, EntityPatch.class);
            if (refreshedPatch != null) {
                this.invokeOnJoinWorld(refreshedPatch, entity, event);
            } else {
                LOGGER.warn("[Advanced Mobpatch] Patch {} refresh returned null: type={} id={}", source, entity.getType(), entity.getId());
            }
        }
    }

    private void cacheOriginalProvider(Entity entity) {
        net.minecraft.world.entity.EntityType<?> entityType = entity.getType();
        if (ORIGINAL_PATCH_PROVIDERS.containsKey(entityType)) {
            return;
        }

        String key = net.minecraft.world.entity.EntityType.getKey(entityType).toString();
        Function<Entity, Supplier<EntityPatch<?>>> baseProvider = EntityPatchProvider.get(key);
        if (baseProvider != null) {
            ORIGINAL_PATCH_PROVIDERS.put(entityType, baseProvider);
            CURRENT_PATCH_PROVIDERS.putIfAbsent(entityType, baseProvider);
        }
    }

    private void tryRestoreOriginalPatch(Entity entity, EntityJoinLevelEvent event, String source) {
        net.minecraft.world.entity.EntityType<?> entityType = entity.getType();
        this.cacheOriginalProvider(entity);
        if (!CURRENT_PATCH_PROVIDERS.containsKey(entityType)) {
            return;
        }

        this.swapCurrentAndPreviousProvider(entityType);
        Function<Entity, Supplier<EntityPatch<?>>> currentProvider = this.getCurrentProvider(entityType);
        if (currentProvider == null) {
            return;
        }

        EntityPatchProvider.putCustomEntityPatch(entityType, currentProvider);

        try {
            swapEntityPatchProvider(entity, event);
            EntityPatch<?> restoredPatch = EpicFightCapabilities.getEntityPatch(entity, EntityPatch.class);
            if (restoredPatch == null) {
                LOGGER.warn("[Advanced Mobpatch] Restored patch is null on {}: type={} id={}", source, entityType, entity.getId());
            }
        } catch (Throwable throwable) {
            LOGGER.error("[Advanced Mobpatch] Failed restoring original patch: type={} id={}",
                    entityType,
                    entity.getId(),
                    throwable);
        }
    }

    private void setCurrentProvider(net.minecraft.world.entity.EntityType<?> entityType, Function<Entity, Supplier<EntityPatch<?>>> nextProvider) {
        Function<Entity, Supplier<EntityPatch<?>>> current = CURRENT_PATCH_PROVIDERS.get(entityType);
        if (current != null && current != nextProvider) {
            PREVIOUS_PATCH_PROVIDERS.put(entityType, current);
        }
        CURRENT_PATCH_PROVIDERS.put(entityType, nextProvider);
    }

    private void swapCurrentAndPreviousProvider(net.minecraft.world.entity.EntityType<?> entityType) {
        Function<Entity, Supplier<EntityPatch<?>>> current = CURRENT_PATCH_PROVIDERS.get(entityType);
        Function<Entity, Supplier<EntityPatch<?>>> previous = PREVIOUS_PATCH_PROVIDERS.get(entityType);

        if (previous == null) {
            Function<Entity, Supplier<EntityPatch<?>>> baseProvider = ORIGINAL_PATCH_PROVIDERS.get(entityType);
            if (baseProvider != null) {
                CURRENT_PATCH_PROVIDERS.put(entityType, baseProvider);
            }
            return;
        }

        CURRENT_PATCH_PROVIDERS.put(entityType, previous);
        PREVIOUS_PATCH_PROVIDERS.put(entityType, current);
    }

    private Function<Entity, Supplier<EntityPatch<?>>> getCurrentProvider(net.minecraft.world.entity.EntityType<?> entityType) {
        return CURRENT_PATCH_PROVIDERS.get(entityType);
    }

    private void swapEntityPatchProvider(Entity entity, EntityJoinLevelEvent event) {
        CapabilityDispatcher dispatcher = this.getCapabilityDispatcher(entity);
        if (dispatcher == null) {
            LOGGER.warn("[Advanced Mobpatch] Capability dispatcher missing: type={} id={}", entity.getType(), entity.getId());
            return;
        }

        IMixinCapabilityDispatcher mixinDispatcher = (IMixinCapabilityDispatcher) (Object) dispatcher;
        ICapabilityProvider[] caps = mixinDispatcher.getCaps();
        EntityPatchProvider newProvider = new EntityPatchProvider(entity);
        EntityPatch<?> newPatch = (EntityPatch<?>) newProvider.get();

        if (newPatch == null || !newProvider.hasCapability()) {
            return;
        }

        this.invokeOnConstructed(newPatch, entity);

        boolean replaced = false;
        for (int i = 0; i < caps.length; i++) {
            if (caps[i] instanceof EntityPatchProvider) {
                caps[i] = newProvider;
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            ICapabilityProvider[] expandedCaps = new ICapabilityProvider[caps.length + 1];
            System.arraycopy(caps, 0, expandedCaps, 0, caps.length);
            expandedCaps[caps.length] = newProvider;
            mixinDispatcher.setCaps(expandedCaps);
        }

        this.invokeOnJoinWorld(newPatch, entity, event);
    }

    private CapabilityDispatcher getCapabilityDispatcher(Entity entity) {
        try {
            return (CapabilityDispatcher) CAPABILITY_PROVIDER_GET_CAPABILITIES_METHOD.invoke(entity);
        } catch (Throwable throwable) {
            LOGGER.error("[Advanced Mobpatch] Failed to read capability dispatcher: type={} id={}", entity.getType(), entity.getId(), throwable);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> void invokeOnConstructed(EntityPatch<?> patch, T entity) {
        ((EntityPatch<T>) patch).onConstructed(entity);
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> void invokeOnJoinWorld(EntityPatch<?> patch, T entity, EntityJoinLevelEvent event) {
        ((EntityPatch<T>) patch).onJoinWorld(entity, event);
    }
}
