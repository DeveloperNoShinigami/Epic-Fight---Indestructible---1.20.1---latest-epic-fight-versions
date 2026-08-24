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
import com.nameless.indestructible.network.SPNbtPatchSelection;
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
import net.minecraftforge.event.TickEvent;
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
    private static final String NBT_OVERRIDE_ACTIVE = MOD_ID + ":nbt_override_active";
    private static final String NBT_SELECTED_PROVIDER = MOD_ID + ":nbt_selected_provider";
    public static final Logger LOGGER = LogManager.getLogger(Indestructible.MOD_ID);
    private static Indestructible INSTANCE;
    public static List<StaticAnimation> NEUTRALIZE_ANIMATION_LIST = new ArrayList<>();
    private static final Map<net.minecraft.world.entity.EntityType<?>, Function<Entity, Supplier<EntityPatch<?>>>> ORIGINAL_PATCH_PROVIDERS = new HashMap<>();
    private static final Map<net.minecraft.world.entity.EntityType<?>, Function<Entity, Supplier<EntityPatch<?>>>> NBT_DISPATCHERS = new HashMap<>();
    private static final Map<Integer, String> CLIENT_NBT_SELECTIONS = new HashMap<>();
    private static final Map<Integer, String> CLIENT_NBT_SELECTIONS_PENDING = new HashMap<>();
    /**
     * Provider replacement invokes Epic Fight patch lifecycle methods, some of
     * which rebuild a mob's GoalSelector.  LivingTickEvent is fired while the
     * level is iterating entities, so doing that replacement there can mutate
     * GoalSelector while it is being iterated.  Queue server-side changes and
     * apply them at the end of the server tick instead.
     */
    private static final Set<Entity> SERVER_NBT_REFRESH_PENDING = new HashSet<>();
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
        INSTANCE = this;
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(GuardAnimations::registerAnimations);
        bus.addListener(this::doCommonStuff);
        bus.addListener(this::doClientStuff);
        MinecraftForge.EVENT_BUS.addListener(this::reloadListnerEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onDatapackSync);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::stopTrackingEvent);
        MinecraftForge.EVENT_BUS.addListener(this::startTrackingEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onEntityJoinLevel);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
    }

    private void doCommonStuff(final FMLCommonSetupEvent event) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        EpicFightNetworkManager.INSTANCE.registerMessage(99, SPDatapackSync.class, SPDatapackSync::toBytes, SPDatapackSync::fromBytes, SPDatapackSync::handle);
        EpicFightNetworkManager.INSTANCE.registerMessage(100, SPNbtPatchSelection.class, SPNbtPatchSelection::toBytes, SPNbtPatchSelection::fromBytes, SPNbtPatchSelection::handle);
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
        if (!(entity instanceof net.minecraft.world.entity.Mob)) return;
        // NBT providers are universal. CustomNPCs entities are mobs too, so
        // they use the same EFI matcher and per-entity dispatcher.
        this.tryApplyNbtProvider(entity, event, "join");
    }

    private void onLivingTick(LivingEvent.LivingTickEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof net.minecraft.world.entity.Mob)) return;
        if (entity.level().isClientSide() && CLIENT_NBT_SELECTIONS_PENDING.containsKey(entity.getId())) {
            String providerId = CLIENT_NBT_SELECTIONS_PENDING.remove(entity.getId());
            applyClientNbtSelection(entity.getId(), providerId, true);
        }
        // Keep NBT routing universal; this includes CustomNPCs mobs.
        String selectedProviderId = this.getSelectedProviderId(entity);
        String previousProviderId = entity.getPersistentData().getString(NBT_SELECTED_PROVIDER);
        if (!selectedProviderId.equals(previousProviderId)) {
            if (entity.level().isClientSide()) {
                if (selectedProviderId.isEmpty()) {
                    this.tryRestoreOriginalPatch(entity, new EntityJoinLevelEvent(entity, entity.level()), "tick");
                } else {
                    this.tryApplyNbtProvider(entity, new EntityJoinLevelEvent(entity, entity.level()), "tick");
                }
            } else {
                SERVER_NBT_REFRESH_PENDING.add(entity);
            }
        }
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        // Rebuilds queued during the previous entity tick are drained before
        // the next level/entity tick begins.  END is not a safe boundary in
        // the Forge dev server because some level work can still be nested in
        // the server-tick dispatch sequence.
        if (event.phase == TickEvent.Phase.START) {
            AdvancedCustomHumanoidMobPatch.processQueuedAiRebuilds();
            return;
        }

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!SERVER_NBT_REFRESH_PENDING.isEmpty()) {
            // Drain first so a lifecycle callback cannot modify the collection we
            // are iterating.  A one-tick delay is intentional: it places provider
            // replacement outside the entity/goal-selector iteration.
            List<Entity> pending = new ArrayList<>(SERVER_NBT_REFRESH_PENDING);
            SERVER_NBT_REFRESH_PENDING.clear();
            for (Entity entity : pending) {
                if (entity.isRemoved() || entity.level().isClientSide() || !(entity instanceof net.minecraft.world.entity.Mob)) {
                    continue;
                }

                String selectedProviderId = this.getSelectedProviderId(entity);
                String previousProviderId = entity.getPersistentData().getString(NBT_SELECTED_PROVIDER);
                if (selectedProviderId.equals(previousProviderId)) {
                    continue;
                }

                if (selectedProviderId.isEmpty()) {
                    this.tryRestoreOriginalPatch(entity, new EntityJoinLevelEvent(entity, entity.level()), "server-tick");
                } else {
                    this.tryApplyNbtProvider(entity, new EntityJoinLevelEvent(entity, entity.level()), "server-tick");
                }
            }
        }

    }

    private String getSelectedProviderId(Entity entity) {
        AdvancedMobpatchReloader.AdvancedCustomHumanoidMobPatchProvider provider = this.findMatchingNbtProvider(entity);
        if (provider == null) {
            return "";
        }
        String id = AdvancedMobpatchReloader.getNbtProviderId(provider);
        return id == null ? "" : id;
    }

    private boolean isCustomNpcsEntity(Entity entity) {
        net.minecraft.resources.ResourceLocation typeKey = net.minecraft.world.entity.EntityType.getKey(entity.getType());
        return typeKey != null && "customnpcs".equals(typeKey.getNamespace());
    }

    private AdvancedMobpatchReloader.AdvancedCustomHumanoidMobPatchProvider findMatchingNbtProvider(Entity entity) {
        if (entity.level().isClientSide()) {
            if (CLIENT_NBT_SELECTIONS.containsKey(entity.getId())) {
                String selectedId = CLIENT_NBT_SELECTIONS.get(entity.getId());
                return selectedId == null || selectedId.isEmpty()
                        ? null
                        : AdvancedMobpatchReloader.getNbtProvider(selectedId);
            }
        }

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

            // Keep the provider selection per entity. The EntityPatchProvider registry is
            // keyed by EntityType, so installing the first matching provider directly
            // made every later entity of that type inherit the first entity's patch.
            // The dispatcher re-evaluates NBT for each entity being constructed.
            Function<Entity, Supplier<EntityPatch<?>>> advancedProvider = this.getOrCreateNbtDispatcher(entity.getType());
            EntityPatchProvider.putCustomEntityPatch(entity.getType(), advancedProvider);

            try {
                swapEntityPatchProvider(entity, event);
            } catch (Throwable throwable) {
                LOGGER.error("[Advanced Mobpatch] Failed to swap provider safely: type={} id={}",
                        entity.getType(), entity.getId(), throwable);
                return;
            }

            EntityPatch<?> refreshedPatch = EpicFightCapabilities.getEntityPatch(entity, EntityPatch.class);
            if (refreshedPatch != null) {
                String providerId = AdvancedMobpatchReloader.getNbtProviderId(provider);
                entity.getPersistentData().putBoolean(NBT_OVERRIDE_ACTIVE, true);
                entity.getPersistentData().putString(NBT_SELECTED_PROVIDER, providerId == null ? "" : providerId);
                if (!entity.level().isClientSide()) {
                    if (providerId != null) {
                        EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(
                                new SPNbtPatchSelection(entity.getId(), providerId, true), entity);
                    }
                }
            } else {
                LOGGER.warn("[Advanced Mobpatch] Patch {} refresh returned null: type={} id={}", source, entity.getType(), entity.getId());
            }
        }
    }

    private void startTrackingEvent(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (!(event.getEntity() instanceof ServerPlayer player) || target.level().isClientSide()) {
            return;
        }

        AdvancedMobpatchReloader.AdvancedCustomHumanoidMobPatchProvider provider =
                this.findMatchingNbtProvider(target);
        if (provider == null) {
            EpicFightNetworkManager.sendToPlayer(
                    new SPNbtPatchSelection(target.getId(), "", false), player);
            return;
        }

        String providerId = AdvancedMobpatchReloader.getNbtProviderId(provider);
        if (providerId != null) {
            EpicFightNetworkManager.sendToPlayer(
                    new SPNbtPatchSelection(target.getId(), providerId, true), player);
        }
    }

    /** Applies the server-selected NBT provider on the client entity instance. */
    public static void applyClientNbtSelection(int entityId, String providerId, boolean active) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(entityId);
        if (entity == null) {
            if (active) {
                CLIENT_NBT_SELECTIONS_PENDING.put(entityId, providerId);
            } else {
                CLIENT_NBT_SELECTIONS_PENDING.remove(entityId);
            }
            return;
        }

        if (active && AdvancedMobpatchReloader.getNbtProvider(providerId) != null) {
            CLIENT_NBT_SELECTIONS.put(entityId, providerId);
            if (INSTANCE != null) {
                INSTANCE.tryApplyNbtProvider(entity,
                        new EntityJoinLevelEvent(entity, entity.level()), "client-sync");
            }
        } else {
            CLIENT_NBT_SELECTIONS.put(entityId, "");
            if (INSTANCE != null) {
                INSTANCE.tryRestoreOriginalPatch(entity,
                        new EntityJoinLevelEvent(entity, entity.level()), "client-sync");
            }
        }
    }

    private Function<Entity, Supplier<EntityPatch<?>>> getOrCreateNbtDispatcher(
            net.minecraft.world.entity.EntityType<?> entityType) {
        Function<Entity, Supplier<EntityPatch<?>>> existing = NBT_DISPATCHERS.get(entityType);
        if (existing != null) {
            return existing;
        }

        Function<Entity, Supplier<EntityPatch<?>>> dispatcher = entity -> () -> {
            AdvancedMobpatchReloader.AdvancedCustomHumanoidMobPatchProvider matching =
                    this.findMatchingNbtProvider(entity);
            if (matching != null) {
                matching.registerArmature(entity.getType());
                return matching.get(entity);
            }

            Function<Entity, Supplier<EntityPatch<?>>> base = ORIGINAL_PATCH_PROVIDERS.get(entity.getType());
            return base == null ? null : base.apply(entity).get();
        };
        NBT_DISPATCHERS.put(entityType, dispatcher);
        return dispatcher;
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
        }
    }

    private void tryRestoreOriginalPatch(Entity entity, EntityJoinLevelEvent event, String source) {
        net.minecraft.world.entity.EntityType<?> entityType = entity.getType();
        this.cacheOriginalProvider(entity);
        Function<Entity, Supplier<EntityPatch<?>>> baseProvider = ORIGINAL_PATCH_PROVIDERS.get(entityType);
        if (baseProvider == null) {
            return;
        }

        // Keep the per-entity dispatcher installed globally. It resolves the
        // NBT override for the entity being constructed and falls back to the
        // cached original provider. Restoring the raw base provider here would
        // make one entity's reset affect every entity of the same type.
        EntityPatchProvider.putCustomEntityPatch(entityType, this.getOrCreateNbtDispatcher(entityType));

        try {
            swapEntityPatchProvider(entity, event);
            EntityPatch<?> restoredPatch = EpicFightCapabilities.getEntityPatch(entity, EntityPatch.class);
            if (restoredPatch == null) {
                LOGGER.warn("[Advanced Mobpatch] Restored patch is null on {}: type={} id={}", source, entityType, entity.getId());
            } else {
                entity.getPersistentData().remove(NBT_OVERRIDE_ACTIVE);
                entity.getPersistentData().remove(NBT_SELECTED_PROVIDER);
                if (!entity.level().isClientSide()) {
                    EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(
                            new SPNbtPatchSelection(entity.getId(), "", false), entity);
                }
            }
        } catch (Throwable throwable) {
            LOGGER.error("[Advanced Mobpatch] Failed restoring original patch: type={} id={}",
                    entityType,
                    entity.getId(),
                    throwable);
        }
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
        EntityPatch<?> installedPatch = EpicFightCapabilities.getEntityPatch(entity, EntityPatch.class);
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
