package com.nameless.indestructible.world.capability;

import com.nameless.indestructible.data.AdvancedMobpatchReloader.AmmoSlotConfig;
import com.nameless.indestructible.data.AdvancedMobpatchReloader.AmmoSlotRule;
import com.nameless.indestructible.data.AdvancedMobpatchReloader.AmmoSlotType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class TaczCompat {
    private static final String TACZ_MOD_ID = "tacz";

    private static final Class<?> I_GUN_CLASS;
    private static final Class<?> I_AMMO_BOX_CLASS;
    private static final Class<?> I_GUN_OPERATOR_CLASS;
    private static final Class<?> FIRE_MODE_CLASS;
    private static final Method I_GUN_GET_OR_NULL;
    private static final Method I_AMMO_GET_OR_NULL;
    private static final Method I_GUN_GET_GUN_ID;
    private static final Method I_GUN_GET_CURRENT_AMMO_COUNT;
    private static final Method I_GUN_SET_CURRENT_AMMO_COUNT;
    private static final Method I_GUN_HAS_BULLET_IN_BARREL;
    private static final Method I_GUN_GET_FIRE_MODE;
    private static final Method I_GUN_SET_FIRE_MODE;
    private static final Method I_GUN_USE_INVENTORY_AMMO;
    private static final Method I_AMMO_IS_AMMO_OF_GUN;
    private static final Method I_AMMO_BOX_IS_AMMO_BOX_OF_GUN;
    private static final Method I_AMMO_BOX_GET_AMMO_COUNT;
    private static final Method I_AMMO_BOX_SET_AMMO_ID;
    private static final Method I_AMMO_BOX_SET_AMMO_COUNT;
    private static final Method I_AMMO_BOX_IS_CREATIVE;
    private static final Method I_AMMO_BOX_IS_ALL_TYPE_CREATIVE;
    private static final Method GUN_OPERATOR_FROM_LIVING_ENTITY;
    private static final Method GUN_OPERATOR_INITIAL_DATA;
    private static final Method GUN_OPERATOR_AIM;
    private static final Method GUN_OPERATOR_RELOAD;
    private static final Method GUN_OPERATOR_GET_DATA_HOLDER;
    private static final Method GUN_OPERATOR_SHOOT;
    private static final Method GUN_OPERATOR_NEED_CHECK_AMMO;
    private static final Method GUN_OPERATOR_CONSUMES_AMMO_OR_NOT;
    private static final Method GUN_OPERATOR_GET_SYN_RELOAD_STATE;
    private static final Method GUN_OPERATOR_GET_SYN_IS_AIMING;
    private static final Method TIMELESS_GET_COMMON_GUN_INDEX;
    private static final Method COMMON_GUN_INDEX_GET_GUN_DATA;
    private static final Method GUN_DATA_GET_AMMO_ID;
    private static final Method GUN_DATA_GET_AMMO_AMOUNT;
    private static final Method GUN_DATA_GET_BOLT;
    private static final Method GUN_DATA_GET_BURST_DATA;
    private static final Method GUN_DATA_GET_FIRE_MODE_SET;
    private static final Method GUN_DATA_GET_SHOOT_INTERVAL;
    private static final Method AMMO_ITEM_BUILDER_CREATE;
    private static final Method AMMO_ITEM_BUILDER_SET_COUNT;
    private static final Method AMMO_ITEM_BUILDER_SET_ID;
    private static final Method AMMO_ITEM_BUILDER_BUILD;
    private static final Method BURST_DATA_GET_MIN_INTERVAL;
    private static final Method ATTACHMENT_GET_AMMO_COUNT_WITH_ATTACHMENT;
    private static final Method ABSTRACT_GUN_ITEM_START_RELOAD;
    private static final Method RELOAD_STATE_GET_STATE_TYPE;
    private static final Method RELOAD_STATE_TYPE_IS_RELOADING;
    private static final Field SHOOTER_DATA_HOLDER_RELOAD_TIMESTAMP;
    private static final Field SHOOTER_DATA_HOLDER_RELOAD_STATE_TYPE;
    private static final Object RELOAD_STATE_NOT_RELOADING;
    private static final Object RELOAD_STATE_EMPTY_RELOAD_FEEDING;
    private static final Object RELOAD_STATE_TACTICAL_RELOAD_FEEDING;
    private static final Object BOLT_OPEN_BOLT;
    private static final TaczCompatBackend BACKEND;

    static {
        Class<?> iGunClass = null;
        Class<?> iAmmoClass = null;
        Class<?> iAmmoBoxClass = null;
        Class<?> iGunOperatorClass = null;
        Class<?> fireModeClass = null;
        Method iGunGetOrNull = null;
        Method iAmmoGetOrNull = null;
        Method iGunGetGunId = null;
        Method iGunGetCurrentAmmoCount = null;
        Method iGunSetCurrentAmmoCount = null;
        Method iGunHasBulletInBarrel = null;
        Method iGunGetFireMode = null;
        Method iGunSetFireMode = null;
        Method iGunUseDummyAmmo = null;
        Method iGunGetDummyAmmoAmount = null;
        Method iGunSetDummyAmmoAmount = null;
        Method iGunUseInventoryAmmo = null;
        Method iAmmoIsAmmoOfGun = null;
        Method iAmmoBoxIsAmmoBoxOfGun = null;
        Method iAmmoBoxGetAmmoCount = null;
        Method iAmmoBoxSetAmmoId = null;
        Method iAmmoBoxSetAmmoCount = null;
        Method iAmmoBoxIsCreative = null;
        Method iAmmoBoxIsAllTypeCreative = null;
        Method gunOperatorFromLivingEntity = null;
        Method gunOperatorInitialData = null;
        Method gunOperatorAim = null;
        Method gunOperatorReload = null;
        Method gunOperatorGetDataHolder = null;
        Method gunOperatorShoot = null;
        Method gunOperatorNeedCheckAmmo = null;
        Method gunOperatorConsumesAmmoOrNot = null;
        Method gunOperatorGetSynReloadState = null;
        Method gunOperatorGetSynIsAiming = null;
        Method timelessGetCommonGunIndex = null;
        Method commonGunIndexGetGunData = null;
        Method gunDataGetAmmoId = null;
        Method gunDataGetAmmoAmount = null;
        Method gunDataGetBolt = null;
        Method gunDataGetBurstData = null;
        Method gunDataGetFireModeSet = null;
        Method gunDataGetShootInterval = null;
        Method ammoItemBuilderCreate = null;
        Method ammoItemBuilderSetCount = null;
        Method ammoItemBuilderSetId = null;
        Method ammoItemBuilderBuild = null;
        Method burstDataGetMinInterval = null;
        Method attachmentGetAmmoCountWithAttachment = null;
        Method abstractGunItemStartReload = null;
        Method reloadStateGetStateType = null;
        Method reloadStateTypeIsReloading = null;
        Field shooterDataHolderReloadTimestamp = null;
        Field shooterDataHolderReloadStateType = null;
        Object reloadStateNotReloading = null;
        Object reloadStateEmptyReloadFeeding = null;
        Object reloadStateTacticalReloadFeeding = null;
        Object boltOpenBolt = null;

        try {
            iGunClass = Class.forName("com.tacz.guns.api.item.IGun");
            iAmmoClass = Class.forName("com.tacz.guns.api.item.IAmmo");
            iAmmoBoxClass = Class.forName("com.tacz.guns.api.item.IAmmoBox");
            iGunOperatorClass = Class.forName("com.tacz.guns.api.entity.IGunOperator");
            Class<?> reloadStateClass = Class.forName("com.tacz.guns.api.entity.ReloadState");
            Class<?> reloadStateTypeClass = Class.forName("com.tacz.guns.api.entity.ReloadState$StateType");
            Class<?> shooterDataHolderClass = Class.forName("com.tacz.guns.entity.shooter.ShooterDataHolder");
            Class<?> abstractGunItemClass = Class.forName("com.tacz.guns.api.item.gun.AbstractGunItem");
            fireModeClass = Class.forName("com.tacz.guns.api.item.gun.FireMode");
            Class<?> timelessApiClass = Class.forName("com.tacz.guns.api.TimelessAPI");
            Class<?> commonGunIndexClass = Class.forName("com.tacz.guns.resource.index.CommonGunIndex");
            Class<?> gunDataClass = Class.forName("com.tacz.guns.resource.pojo.data.gun.GunData");
            Class<?> boltClass = Class.forName("com.tacz.guns.resource.pojo.data.gun.Bolt");
            Class<?> burstDataClass = Class.forName("com.tacz.guns.resource.pojo.data.gun.BurstData");
            Class<?> attachmentUtilsClass = Class.forName("com.tacz.guns.util.AttachmentDataUtils");
            Class<?> ammoItemBuilderClass = Class.forName("com.tacz.guns.api.item.builder.AmmoItemBuilder");

            iGunGetOrNull = iGunClass.getMethod("getIGunOrNull", ItemStack.class);
            iAmmoGetOrNull = iAmmoClass.getMethod("getIAmmoOrNull", ItemStack.class);
            iGunGetGunId = iGunClass.getMethod("getGunId", ItemStack.class);
            iGunGetCurrentAmmoCount = iGunClass.getMethod("getCurrentAmmoCount", ItemStack.class);
            iGunSetCurrentAmmoCount = iGunClass.getMethod("setCurrentAmmoCount", ItemStack.class, int.class);
            iGunHasBulletInBarrel = iGunClass.getMethod("hasBulletInBarrel", ItemStack.class);
            iGunGetFireMode = iGunClass.getMethod("getFireMode", ItemStack.class);
            iGunSetFireMode = iGunClass.getMethod("setFireMode", ItemStack.class, fireModeClass);
            iGunUseDummyAmmo = iGunClass.getMethod("useDummyAmmo", ItemStack.class);
            iGunGetDummyAmmoAmount = iGunClass.getMethod("getDummyAmmoAmount", ItemStack.class);
            iGunSetDummyAmmoAmount = iGunClass.getMethod("setDummyAmmoAmount", ItemStack.class, int.class);
            iGunUseInventoryAmmo = iGunClass.getMethod("useInventoryAmmo", ItemStack.class);
            iAmmoIsAmmoOfGun = iAmmoClass.getMethod("isAmmoOfGun", ItemStack.class, ItemStack.class);
            iAmmoBoxIsAmmoBoxOfGun = iAmmoBoxClass.getMethod("isAmmoBoxOfGun", ItemStack.class, ItemStack.class);
            iAmmoBoxGetAmmoCount = iAmmoBoxClass.getMethod("getAmmoCount", ItemStack.class);
            iAmmoBoxSetAmmoId = iAmmoBoxClass.getMethod("setAmmoId", ItemStack.class, ResourceLocation.class);
            iAmmoBoxSetAmmoCount = iAmmoBoxClass.getMethod("setAmmoCount", ItemStack.class, int.class);
            iAmmoBoxIsCreative = iAmmoBoxClass.getMethod("isCreative", ItemStack.class);
            iAmmoBoxIsAllTypeCreative = iAmmoBoxClass.getMethod("isAllTypeCreative", ItemStack.class);
            gunOperatorFromLivingEntity = iGunOperatorClass.getMethod("fromLivingEntity", LivingEntity.class);
            gunOperatorInitialData = iGunOperatorClass.getMethod("initialData");
            gunOperatorAim = iGunOperatorClass.getMethod("aim", boolean.class);
            gunOperatorReload = iGunOperatorClass.getMethod("reload");
            gunOperatorGetDataHolder = iGunOperatorClass.getMethod("getDataHolder");
            gunOperatorShoot = iGunOperatorClass.getMethod("shoot", Supplier.class, Supplier.class);
            gunOperatorNeedCheckAmmo = iGunOperatorClass.getMethod("needCheckAmmo");
            gunOperatorConsumesAmmoOrNot = iGunOperatorClass.getMethod("consumesAmmoOrNot");
            gunOperatorGetSynReloadState = iGunOperatorClass.getMethod("getSynReloadState");
            gunOperatorGetSynIsAiming = iGunOperatorClass.getMethod("getSynIsAiming");
            timelessGetCommonGunIndex = timelessApiClass.getMethod("getCommonGunIndex", ResourceLocation.class);
            commonGunIndexGetGunData = commonGunIndexClass.getMethod("getGunData");
            gunDataGetAmmoId = gunDataClass.getMethod("getAmmoId");
            gunDataGetAmmoAmount = gunDataClass.getMethod("getAmmoAmount");
            gunDataGetBolt = gunDataClass.getMethod("getBolt");
            gunDataGetBurstData = gunDataClass.getMethod("getBurstData");
            gunDataGetFireModeSet = gunDataClass.getMethod("getFireModeSet");
            gunDataGetShootInterval = gunDataClass.getMethod("getShootInterval", LivingEntity.class, fireModeClass, ItemStack.class);
            ammoItemBuilderCreate = ammoItemBuilderClass.getMethod("create");
            ammoItemBuilderSetCount = ammoItemBuilderClass.getMethod("setCount", int.class);
            ammoItemBuilderSetId = ammoItemBuilderClass.getMethod("setId", ResourceLocation.class);
            ammoItemBuilderBuild = ammoItemBuilderClass.getMethod("build");
            burstDataGetMinInterval = burstDataClass.getMethod("getMinInterval");
            attachmentGetAmmoCountWithAttachment = attachmentUtilsClass.getMethod("getAmmoCountWithAttachment", ItemStack.class, gunDataClass);
            abstractGunItemStartReload = abstractGunItemClass.getMethod("startReload", shooterDataHolderClass, ItemStack.class, LivingEntity.class);
            reloadStateGetStateType = reloadStateClass.getMethod("getStateType");
            reloadStateTypeIsReloading = reloadStateTypeClass.getMethod("isReloading");
            shooterDataHolderReloadTimestamp = shooterDataHolderClass.getField("reloadTimestamp");
            shooterDataHolderReloadStateType = shooterDataHolderClass.getField("reloadStateType");

            reloadStateNotReloading = reloadStateTypeClass.getField("NOT_RELOADING").get(null);
            reloadStateEmptyReloadFeeding = reloadStateTypeClass.getField("EMPTY_RELOAD_FEEDING").get(null);
            reloadStateTacticalReloadFeeding = reloadStateTypeClass.getField("TACTICAL_RELOAD_FEEDING").get(null);

            boltOpenBolt = boltClass.getField("OPEN_BOLT").get(null);
        } catch (ReflectiveOperationException ignored) {
        }

        I_GUN_CLASS = iGunClass;
        I_AMMO_BOX_CLASS = iAmmoBoxClass;
        I_GUN_OPERATOR_CLASS = iGunOperatorClass;
        FIRE_MODE_CLASS = fireModeClass;
        I_GUN_GET_OR_NULL = iGunGetOrNull;
        I_AMMO_GET_OR_NULL = iAmmoGetOrNull;
        I_GUN_GET_GUN_ID = iGunGetGunId;
        I_GUN_GET_CURRENT_AMMO_COUNT = iGunGetCurrentAmmoCount;
        I_GUN_SET_CURRENT_AMMO_COUNT = iGunSetCurrentAmmoCount;
        I_GUN_HAS_BULLET_IN_BARREL = iGunHasBulletInBarrel;
        I_GUN_GET_FIRE_MODE = iGunGetFireMode;
        I_GUN_SET_FIRE_MODE = iGunSetFireMode;
        I_GUN_USE_INVENTORY_AMMO = iGunUseInventoryAmmo;
        I_AMMO_IS_AMMO_OF_GUN = iAmmoIsAmmoOfGun;
        I_AMMO_BOX_IS_AMMO_BOX_OF_GUN = iAmmoBoxIsAmmoBoxOfGun;
        I_AMMO_BOX_GET_AMMO_COUNT = iAmmoBoxGetAmmoCount;
        I_AMMO_BOX_SET_AMMO_ID = iAmmoBoxSetAmmoId;
        I_AMMO_BOX_SET_AMMO_COUNT = iAmmoBoxSetAmmoCount;
        I_AMMO_BOX_IS_CREATIVE = iAmmoBoxIsCreative;
        I_AMMO_BOX_IS_ALL_TYPE_CREATIVE = iAmmoBoxIsAllTypeCreative;
        GUN_OPERATOR_FROM_LIVING_ENTITY = gunOperatorFromLivingEntity;
        GUN_OPERATOR_INITIAL_DATA = gunOperatorInitialData;
        GUN_OPERATOR_AIM = gunOperatorAim;
        GUN_OPERATOR_RELOAD = gunOperatorReload;
        GUN_OPERATOR_GET_DATA_HOLDER = gunOperatorGetDataHolder;
        GUN_OPERATOR_SHOOT = gunOperatorShoot;
        GUN_OPERATOR_NEED_CHECK_AMMO = gunOperatorNeedCheckAmmo;
        GUN_OPERATOR_CONSUMES_AMMO_OR_NOT = gunOperatorConsumesAmmoOrNot;
        GUN_OPERATOR_GET_SYN_RELOAD_STATE = gunOperatorGetSynReloadState;
        GUN_OPERATOR_GET_SYN_IS_AIMING = gunOperatorGetSynIsAiming;
        TIMELESS_GET_COMMON_GUN_INDEX = timelessGetCommonGunIndex;
        COMMON_GUN_INDEX_GET_GUN_DATA = commonGunIndexGetGunData;
        GUN_DATA_GET_AMMO_ID = gunDataGetAmmoId;
        GUN_DATA_GET_AMMO_AMOUNT = gunDataGetAmmoAmount;
        GUN_DATA_GET_BOLT = gunDataGetBolt;
        GUN_DATA_GET_BURST_DATA = gunDataGetBurstData;
        GUN_DATA_GET_FIRE_MODE_SET = gunDataGetFireModeSet;
        GUN_DATA_GET_SHOOT_INTERVAL = gunDataGetShootInterval;
        AMMO_ITEM_BUILDER_CREATE = ammoItemBuilderCreate;
        AMMO_ITEM_BUILDER_SET_COUNT = ammoItemBuilderSetCount;
        AMMO_ITEM_BUILDER_SET_ID = ammoItemBuilderSetId;
        AMMO_ITEM_BUILDER_BUILD = ammoItemBuilderBuild;
        BURST_DATA_GET_MIN_INTERVAL = burstDataGetMinInterval;
        ATTACHMENT_GET_AMMO_COUNT_WITH_ATTACHMENT = attachmentGetAmmoCountWithAttachment;
        ABSTRACT_GUN_ITEM_START_RELOAD = abstractGunItemStartReload;
        RELOAD_STATE_GET_STATE_TYPE = reloadStateGetStateType;
        RELOAD_STATE_TYPE_IS_RELOADING = reloadStateTypeIsReloading;
        SHOOTER_DATA_HOLDER_RELOAD_TIMESTAMP = shooterDataHolderReloadTimestamp;
        SHOOTER_DATA_HOLDER_RELOAD_STATE_TYPE = shooterDataHolderReloadStateType;
        RELOAD_STATE_NOT_RELOADING = reloadStateNotReloading;
        RELOAD_STATE_EMPTY_RELOAD_FEEDING = reloadStateEmptyReloadFeeding;
        RELOAD_STATE_TACTICAL_RELOAD_FEEDING = reloadStateTacticalReloadFeeding;
        BOLT_OPEN_BOLT = boltOpenBolt;
        BACKEND = createBackend();
    }

    private TaczCompat() {
    }

    static boolean isAvailable() {
        return BACKEND.isAvailable();
    }

    private static boolean hasReflectiveTaczBridge() {
        return ModList.get().isLoaded(TACZ_MOD_ID)
                && I_GUN_CLASS != null
                && I_GUN_OPERATOR_CLASS != null
                && FIRE_MODE_CLASS != null;
    }

    static boolean isTaczGun(ItemStack stack) {
        return BACKEND.isTaczGun(stack);
    }

    private static boolean reflectiveIsTaczGun(ItemStack stack) {
        return getIGun(stack) != null;
    }

    @Nullable
    static InteractionHand findGunHand(LivingEntity entity, @Nullable InteractionHand preferredHand) {
        return BACKEND.findGunHand(entity, preferredHand);
    }

    @Nullable
    private static InteractionHand reflectiveFindGunHand(LivingEntity entity, @Nullable InteractionHand preferredHand) {
        if (preferredHand != null && isTaczGun(entity.getItemInHand(preferredHand))) {
            return preferredHand;
        }

        if (isTaczGun(entity.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }

        if (isTaczGun(entity.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    static boolean hasUsableAmmo(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
        return BACKEND.hasUsableAmmo(shooter, gunStack, ammoSlots);
    }

    private static boolean reflectiveHasUsableAmmo(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
        if (!requiresAmmo(shooter, gunStack)) {
            return true;
        }

        if (usesInventoryAmmo(gunStack)) {
            try {
                return hasConfiguredOrLegacyAmmo(shooter, gunStack, ammoSlots);
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        }

        return getEffectiveAmmoCount(gunStack) > 0;
    }

    static boolean hasReserveAmmo(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
        return BACKEND.hasReserveAmmo(shooter, gunStack, ammoSlots);
    }

    private static boolean reflectiveHasReserveAmmo(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
        if (!requiresAmmo(shooter, gunStack)) {
            return true;
        }

        try {
            return hasConfiguredOrLegacyAmmo(shooter, gunStack, ammoSlots);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    static boolean isAmmoEmpty(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
        return BACKEND.isAmmoEmpty(shooter, gunStack, ammoSlots);
    }

    private static boolean reflectiveIsAmmoEmpty(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
        boolean invAmmo = usesInventoryAmmo(gunStack);
        boolean result;
        if (invAmmo) {
            try {
                result = !hasConfiguredOrLegacyAmmo(shooter, gunStack, ammoSlots);
            } catch (ReflectiveOperationException ignored) {
                result = true;
            }
        } else {
            result = getEffectiveAmmoCount(gunStack) <= 0;
        }
        return result;
    }

    static boolean isAmmoNotFull(LivingEntity shooter, ItemStack gunStack) {
        return BACKEND.isAmmoNotFull(shooter, gunStack);
    }

    private static boolean reflectiveIsAmmoNotFull(LivingEntity shooter, ItemStack gunStack) {
        if (usesInventoryAmmo(gunStack)) {
            return false;
        }

        int maxAmmo = getMaxAmmoCount(gunStack);
        int currentAmmo = getCurrentAmmoCount(gunStack);
        if (maxAmmo <= 0) {
            return currentAmmo <= 0;
        }
        return currentAmmo < maxAmmo;
    }

    static boolean canReload(LivingEntity shooter, ItemStack gunStack) {
        return BACKEND.canReload(shooter, gunStack);
    }

    private static boolean reflectiveCanReload(LivingEntity shooter, ItemStack gunStack) {
        return isTaczGun(gunStack) && !usesInventoryAmmo(gunStack) && isAmmoNotFull(shooter, gunStack);
    }

    static boolean tryReload(LivingEntity shooter, InteractionHand hand, boolean requireAmmo, List<AmmoSlotConfig> ammoSlots) {
        return BACKEND.tryReload(shooter, hand, requireAmmo, ammoSlots);
    }

    private static boolean reflectiveTryReload(LivingEntity shooter, InteractionHand hand, boolean requireAmmo, List<AmmoSlotConfig> ammoSlots) {
        ItemStack gunStack = shooter.getItemInHand(hand);
        Object operator = getOperator(shooter);
        if (operator == null || !isTaczGun(gunStack) || usesInventoryAmmo(gunStack)) {
            return false;
        }

        // When requireAmmo=false, skip all slot/inventory machinery and directly fill the gun.
        // This is the same mechanism the synthetic fallback uses — TACZ just needs the ammo count
        // set in the gun's NBT and the reload state triggered; no real item is needed.
        if (!requireAmmo && I_GUN_SET_CURRENT_AMMO_COUNT != null) {
            Object iGun = getIGun(gunStack);
            if (iGun != null) {
                int currentAmmo = getCurrentAmmoCount(gunStack);
                int maxAmmo = getMaxAmmoCount(gunStack);
                if (maxAmmo <= 0) maxAmmo = Math.max(1, currentAmmo + 1);
                int neededAmmo = maxAmmo - currentAmmo;
                if (neededAmmo > 0) {
                    Object reloadStateType = currentAmmo <= 0 ? RELOAD_STATE_EMPTY_RELOAD_FEEDING : RELOAD_STATE_TACTICAL_RELOAD_FEEDING;
                    try {
                        prepareSlotReload(shooter, hand, gunStack, operator, reloadStateType);
                    } catch (ReflectiveOperationException ignored) {
                    }
                    try {
                        I_GUN_SET_CURRENT_AMMO_COUNT.invoke(iGun, gunStack, currentAmmo + neededAmmo);
                        shooter.startUsingItem(hand);
                        return true;
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
        }

        if (ammoSlots != null && !ammoSlots.isEmpty()) {
            if (tryReloadFromConfiguredAmmo(shooter, hand, gunStack, operator, ammoSlots, requireAmmo)) {
                return true;
            }
        }

        if (tryReloadFromCustomNpcProjectileAmmo(shooter, hand, gunStack, operator)) {
            return true;
        }

        return startNativeReload(shooter, hand, operator);
    }

    private static boolean startNativeReload(LivingEntity shooter, InteractionHand hand, Object operator) {
        try {
            GUN_OPERATOR_INITIAL_DATA.invoke(operator);
            GUN_OPERATOR_AIM.invoke(operator, false);
            shooter.startUsingItem(hand);
            GUN_OPERATOR_RELOAD.invoke(operator);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    static boolean tryShoot(LivingEntity shooter, LivingEntity target, InteractionHand hand, @Nullable String fireModeName) {
        return BACKEND.tryShoot(shooter, target, hand, fireModeName);
    }

    private static boolean reflectiveTryShoot(LivingEntity shooter, LivingEntity target, InteractionHand hand, @Nullable String fireModeName) {
        ItemStack gunStack = shooter.getItemInHand(hand);
        Object operator = getOperator(shooter);
        if (operator == null || !isTaczGun(gunStack)) {
            return false;
        }

        if (requiresAmmo(shooter, gunStack) && getEffectiveAmmoCount(gunStack) <= 0) {
            return false;
        }

        try {
            GUN_OPERATOR_INITIAL_DATA.invoke(operator);
            setFireMode(gunStack, fireModeName);
            GUN_OPERATOR_AIM.invoke(operator, true);
            shooter.startUsingItem(hand);
            GUN_OPERATOR_SHOOT.invoke(operator, pitchSupplier(shooter, target), yawSupplier(shooter, target));
            releaseTriggerForSemiMode(shooter, operator, gunStack);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    static boolean tryAim(LivingEntity shooter, InteractionHand hand) {
        return BACKEND.tryAim(shooter, hand);
    }

    private static boolean reflectiveTryAim(LivingEntity shooter, InteractionHand hand) {
        ItemStack gunStack = shooter.getItemInHand(hand);
        Object operator = getOperator(shooter);
        if (operator == null || !isTaczGun(gunStack)) {
            return false;
        }

        try {
            GUN_OPERATOR_INITIAL_DATA.invoke(operator);
            GUN_OPERATOR_AIM.invoke(operator, true);
            shooter.startUsingItem(hand);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    static void stopAiming(LivingEntity shooter) {
        BACKEND.stopAiming(shooter);
    }

    private static void reflectiveStopAiming(LivingEntity shooter) {
        Object operator = getOperator(shooter);
        if (operator == null) {
            return;
        }

        try {
            GUN_OPERATOR_AIM.invoke(operator, false);
            shooter.stopUsingItem();
        } catch (ReflectiveOperationException ignored) {
        }
    }

    static boolean isReloading(LivingEntity shooter) {
        return BACKEND.isReloading(shooter);
    }

    private static boolean reflectiveIsReloading(LivingEntity shooter) {
        Object operator = getOperator(shooter);
        if (operator == null || GUN_OPERATOR_GET_SYN_RELOAD_STATE == null || RELOAD_STATE_GET_STATE_TYPE == null || RELOAD_STATE_TYPE_IS_RELOADING == null) {
            return false;
        }

        try {
            Object reloadState = GUN_OPERATOR_GET_SYN_RELOAD_STATE.invoke(operator);
            if (reloadState == null) {
                return false;
            }

            Object stateType = RELOAD_STATE_GET_STATE_TYPE.invoke(reloadState);
            return stateType != null && (boolean) RELOAD_STATE_TYPE_IS_RELOADING.invoke(stateType);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean isAiming(LivingEntity shooter) {
        return BACKEND.isAiming(shooter);
    }

    private static boolean reflectiveIsAiming(LivingEntity shooter) {
        Object operator = getOperator(shooter);
        if (operator == null || GUN_OPERATOR_GET_SYN_IS_AIMING == null) {
            return false;
        }

        try {
            return (boolean) GUN_OPERATOR_GET_SYN_IS_AIMING.invoke(operator);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    static int getShootIntervalTicks(LivingEntity shooter, ItemStack gunStack) {
        // Fire-rate interval calculation is intentionally disabled for validation.
        return 0;
    }

    private static int reflectiveGetShootIntervalTicks(LivingEntity shooter, ItemStack gunStack) {
        // Fire-rate interval calculation is intentionally disabled for validation.
        return 0;
    }

    private static TaczCompatBackend createBackend() {
        if (!hasReflectiveTaczBridge()) {
            return NoopTaczCompatBackend.INSTANCE;
        }
        return new ReflectiveTaczCompatBackend();
    }

    private static final class ReflectiveTaczCompatBackend implements TaczCompatBackend {
        @Override
        public boolean isAvailable() {
            return hasReflectiveTaczBridge();
        }

        @Override
        public boolean isTaczGun(ItemStack stack) {
            return reflectiveIsTaczGun(stack);
        }

        @Override
        public InteractionHand findGunHand(LivingEntity entity, @Nullable InteractionHand preferredHand) {
            return reflectiveFindGunHand(entity, preferredHand);
        }

        @Override
        public boolean hasUsableAmmo(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
            return reflectiveHasUsableAmmo(shooter, gunStack, ammoSlots);
        }

        @Override
        public boolean hasReserveAmmo(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
            return reflectiveHasReserveAmmo(shooter, gunStack, ammoSlots);
        }

        @Override
        public boolean isAmmoEmpty(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
            return reflectiveIsAmmoEmpty(shooter, gunStack, ammoSlots);
        }

        @Override
        public boolean isAmmoNotFull(LivingEntity shooter, ItemStack gunStack) {
            return reflectiveIsAmmoNotFull(shooter, gunStack);
        }

        @Override
        public boolean canReload(LivingEntity shooter, ItemStack gunStack) {
            return reflectiveCanReload(shooter, gunStack);
        }

        @Override
        public boolean tryReload(LivingEntity shooter, InteractionHand hand, boolean requireAmmo, List<AmmoSlotConfig> ammoSlots) {
            return reflectiveTryReload(shooter, hand, requireAmmo, ammoSlots);
        }

        @Override
        public boolean tryShoot(LivingEntity shooter, LivingEntity target, InteractionHand hand, @Nullable String fireModeName) {
            return reflectiveTryShoot(shooter, target, hand, fireModeName);
        }

        @Override
        public boolean tryAim(LivingEntity shooter, InteractionHand hand) {
            return reflectiveTryAim(shooter, hand);
        }

        @Override
        public void stopAiming(LivingEntity shooter) {
            reflectiveStopAiming(shooter);
        }

        @Override
        public boolean isReloading(LivingEntity shooter) {
            return reflectiveIsReloading(shooter);
        }

        @Override
        public boolean isAiming(LivingEntity shooter) {
            return reflectiveIsAiming(shooter);
        }

        @Override
        public int getShootIntervalTicks(LivingEntity shooter, ItemStack gunStack) {
            // Fire-rate interval calculation is intentionally disabled for validation.
            return 0;
        }
    }

    @Nullable
    private static Object getIGun(ItemStack stack) {
        if (!isAvailable() || stack.isEmpty()) {
            return null;
        }

        try {
            return I_GUN_GET_OR_NULL.invoke(null, stack);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Nullable
    private static Object getOperator(LivingEntity shooter) {
        if (!isAvailable()) {
            return null;
        }

        try {
            return GUN_OPERATOR_FROM_LIVING_ENTITY.invoke(null, shooter);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean requiresAmmo(LivingEntity shooter, ItemStack gunStack) {
        Object operator = getOperator(shooter);
        if (operator == null) {
            return true;
        }

        try {
            boolean needCheckAmmo = (boolean) GUN_OPERATOR_NEED_CHECK_AMMO.invoke(operator);
            boolean consumesAmmo = (boolean) GUN_OPERATOR_CONSUMES_AMMO_OR_NOT.invoke(operator);
            return needCheckAmmo && consumesAmmo;
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private static boolean usesInventoryAmmo(ItemStack gunStack) {
        Object iGun = getIGun(gunStack);
        if (iGun == null) {
            return false;
        }

        try {
            return (boolean) I_GUN_USE_INVENTORY_AMMO.invoke(iGun, gunStack);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static int getCurrentAmmoCount(ItemStack gunStack) {
        Object iGun = getIGun(gunStack);
        if (iGun == null) {
            return 0;
        }

        try {
            return ((Number) I_GUN_GET_CURRENT_AMMO_COUNT.invoke(iGun, gunStack)).intValue();
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }

    private static int getEffectiveAmmoCount(ItemStack gunStack) {
        return getCurrentAmmoCount(gunStack) + (hasShootableBarrelRound(gunStack) ? 1 : 0);
    }

    private static boolean hasShootableBarrelRound(ItemStack gunStack) {
        Object iGun = getIGun(gunStack);
        Object gunData = getGunData(gunStack);
        if (iGun == null || gunData == null || I_GUN_HAS_BULLET_IN_BARREL == null || GUN_DATA_GET_BOLT == null || BOLT_OPEN_BOLT == null) {
            return false;
        }

        try {
            Object boltType = GUN_DATA_GET_BOLT.invoke(gunData);
            return !BOLT_OPEN_BOLT.equals(boltType) && (boolean) I_GUN_HAS_BULLET_IN_BARREL.invoke(iGun, gunStack);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static int getMaxAmmoCount(ItemStack gunStack) {
        Object gunData = getGunData(gunStack);
        if (gunData == null) {
            return 0;
        }

        try {
            return ((Number) ATTACHMENT_GET_AMMO_COUNT_WITH_ATTACHMENT.invoke(null, gunStack, gunData)).intValue();
        } catch (ReflectiveOperationException ignored) {
            try {
                return ((Number) GUN_DATA_GET_AMMO_AMOUNT.invoke(gunData)).intValue();
            } catch (ReflectiveOperationException ignoredAgain) {
                return 0;
            }
        }
    }

    private static boolean isAmmoCandidateForGun(ItemStack gunStack, ItemStack candidate) throws ReflectiveOperationException {
        if (candidate.isEmpty()) {
            return false;
        }

        Object iAmmo = I_AMMO_GET_OR_NULL.invoke(null, candidate);
        if (iAmmo != null && (boolean) I_AMMO_IS_AMMO_OF_GUN.invoke(iAmmo, gunStack, candidate)) {
            return true;
        }

        if (I_AMMO_BOX_CLASS.isInstance(candidate.getItem())) {
            boolean creative = (boolean) I_AMMO_BOX_IS_CREATIVE.invoke(candidate.getItem(), candidate)
                    || (boolean) I_AMMO_BOX_IS_ALL_TYPE_CREATIVE.invoke(candidate.getItem(), candidate);
            if (creative) {
                return true;
            }

            int ammoCount = ((Number) I_AMMO_BOX_GET_AMMO_COUNT.invoke(candidate.getItem(), candidate)).intValue();
            return ammoCount > 0 && (boolean) I_AMMO_BOX_IS_AMMO_BOX_OF_GUN.invoke(candidate.getItem(), gunStack, candidate);
        }

        return false;
    }

    private static boolean hasConfiguredOrLegacyAmmo(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots)
            throws ReflectiveOperationException {
        List<AmmoSlotConfig> effectiveAmmoSlots = getEffectiveAmmoSlots(shooter, ammoSlots);
        if (!effectiveAmmoSlots.isEmpty()) {
            for (AmmoSlotConfig ammoSlot : effectiveAmmoSlots) {
                if (ammoSlot.getRule() != AmmoSlotRule.SUPPLY_OR_RELOAD_FROM) {
                    continue;
                }

                if (isAmmoCandidateForGun(gunStack, getAmmoSlotStack(shooter, ammoSlot))) {
                    return true;
                }
            }

            return false;
        }

        if (shooter instanceof InventoryCarrier inventoryCarrier) {
            SimpleContainer inventory = inventoryCarrier.getInventory();
            for (int index = 0; index < inventory.getContainerSize(); index++) {
                if (isAmmoCandidateForGun(gunStack, inventory.getItem(index))) {
                    return true;
                }
            }
        }

        for (AmmoSlotConfig ammoSlot : getCustomNpcReserveAmmoSlots(shooter)) {
            if (isAmmoCandidateForGun(gunStack, getAmmoSlotStack(shooter, ammoSlot))) {
                return true;
            }
        }

        return false;
    }

    private static boolean tryReloadFromConfiguredAmmo(LivingEntity shooter, InteractionHand hand, ItemStack gunStack, Object operator,
                                                       List<AmmoSlotConfig> ammoSlots, boolean requireAmmo) {
        Object iGun = getIGun(gunStack);
        if (iGun == null) {
            return false;
        }

        int currentAmmo = getCurrentAmmoCount(gunStack);
        int maxAmmo = getMaxAmmoCount(gunStack);
        if (maxAmmo <= 0) {
            maxAmmo = Math.max(1, currentAmmo + 1);
        }
        int neededAmmo = maxAmmo - currentAmmo;
        if (neededAmmo <= 0) {
            return false;
        }

        Object reloadStateType = currentAmmo <= 0 ? RELOAD_STATE_EMPTY_RELOAD_FEEDING : RELOAD_STATE_TACTICAL_RELOAD_FEEDING;

        for (AmmoSlotConfig ammoSlot : getEffectiveAmmoSlots(shooter, ammoSlots)) {
            if (ammoSlot.getRule() != AmmoSlotRule.SUPPLY_OR_RELOAD_FROM) {
                continue;
            }

            if (tryReloadFromAmmoSlot(shooter, hand, gunStack, operator, iGun, ammoSlot, currentAmmo, neededAmmo, reloadStateType)) {
                return true;
            }
        }

        return false;
    }

    private static List<AmmoSlotConfig> getEffectiveAmmoSlots(LivingEntity shooter, @Nullable List<AmmoSlotConfig> ammoSlots) {
        ArrayList<AmmoSlotConfig> effectiveSlots = new ArrayList<>();
        Set<String> seenSlots = new HashSet<>();

        if (ammoSlots != null) {
            for (AmmoSlotConfig ammoSlot : ammoSlots) {
                if (seenSlots.add(ammoSlot.getSlot())) {
                    effectiveSlots.add(ammoSlot);
                }
            }
        }

        for (AmmoSlotConfig ammoSlot : getCustomNpcReserveAmmoSlots(shooter)) {
            if (seenSlots.add(ammoSlot.getSlot())) {
                effectiveSlots.add(ammoSlot);
            }
        }

        return effectiveSlots;
    }

    private static List<AmmoSlotConfig> getCustomNpcReserveAmmoSlots(LivingEntity shooter) {
        if (!isCustomNpcAvailable() || CnpcSlotAccess.getInventory(shooter) == null) {
            return List.of();
        }

        ArrayList<AmmoSlotConfig> ammoSlots = new ArrayList<>();
        ammoSlots.add(new AmmoSlotConfig("customnpcs:projectile", AmmoSlotRule.SUPPLY_OR_RELOAD_FROM, AmmoSlotType.CUSTOMNPC_PROJECTILE, null, -1));
        ammoSlots.add(new AmmoSlotConfig("offhand", AmmoSlotRule.SUPPLY_OR_RELOAD_FROM, AmmoSlotType.EQUIPMENT, EquipmentSlot.OFFHAND, -1));

        for (int dropSlot = 0; dropSlot <= 20; dropSlot++) {
            ammoSlots.add(new AmmoSlotConfig("customnpcs:drop:" + dropSlot, AmmoSlotRule.SUPPLY_OR_RELOAD_FROM, AmmoSlotType.CUSTOMNPC_DROP, null, dropSlot));
        }

        return ammoSlots;
    }

    private static boolean tryReloadFromAmmoSlot(LivingEntity shooter, InteractionHand hand, ItemStack gunStack, Object operator,
                                                 Object iGun, AmmoSlotConfig ammoSlot, int currentAmmo, int neededAmmo,
                                                 @Nullable Object reloadStateType) {
        ItemStack ammoStack = getAmmoSlotStack(shooter, ammoSlot);
        if (ammoStack.isEmpty()) {
            return false;
        }

        try {
            Object iAmmo = I_AMMO_GET_OR_NULL.invoke(null, ammoStack);
            if (iAmmo != null && (boolean) I_AMMO_IS_AMMO_OF_GUN.invoke(iAmmo, gunStack, ammoStack)) {
                int loadAmount = Math.min(neededAmmo, ammoStack.getCount());
                if (loadAmount <= 0 || !prepareSlotReload(shooter, hand, gunStack, operator, reloadStateType)) {
                    return false;
                }

                I_GUN_SET_CURRENT_AMMO_COUNT.invoke(iGun, gunStack, currentAmmo + loadAmount);
                ItemStack updatedStack = ammoStack.copy();
                updatedStack.shrink(loadAmount);
                setAmmoSlotStack(shooter, ammoSlot, updatedStack.isEmpty() ? ItemStack.EMPTY : updatedStack);
                shooter.startUsingItem(hand);
                return true;
            }

            if (I_AMMO_BOX_CLASS.isInstance(ammoStack.getItem())
                    && (boolean) I_AMMO_BOX_IS_AMMO_BOX_OF_GUN.invoke(ammoStack.getItem(), gunStack, ammoStack)) {
                boolean creative = (boolean) I_AMMO_BOX_IS_CREATIVE.invoke(ammoStack.getItem(), ammoStack)
                        || (boolean) I_AMMO_BOX_IS_ALL_TYPE_CREATIVE.invoke(ammoStack.getItem(), ammoStack);
                int ammoCount = creative ? neededAmmo : ((Number) I_AMMO_BOX_GET_AMMO_COUNT.invoke(ammoStack.getItem(), ammoStack)).intValue();
                int loadAmount = Math.min(neededAmmo, ammoCount);
                if (loadAmount <= 0 || !prepareSlotReload(shooter, hand, gunStack, operator, reloadStateType)) {
                    return false;
                }

                I_GUN_SET_CURRENT_AMMO_COUNT.invoke(iGun, gunStack, currentAmmo + loadAmount);
                if (!creative) {
                    ItemStack updatedStack = ammoStack.copy();
                    I_AMMO_BOX_SET_AMMO_COUNT.invoke(updatedStack.getItem(), updatedStack, ammoCount - loadAmount);
                    setAmmoSlotStack(shooter, ammoSlot, updatedStack);
                }
                shooter.startUsingItem(hand);
                return true;
            }
        } catch (ReflectiveOperationException ignored) {
            return false;
        }

        return false;
    }

    private static boolean prepareSlotReload(LivingEntity shooter, InteractionHand hand, ItemStack gunStack, Object operator,
                                             @Nullable Object reloadStateType) throws ReflectiveOperationException {
        GUN_OPERATOR_INITIAL_DATA.invoke(operator);
        GUN_OPERATOR_AIM.invoke(operator, false);
        return startCustomAmmoReloadState(shooter, gunStack, operator, reloadStateType);
    }

    private static boolean canAutoSupplyAmmoSlot(ItemStack gunStack, AmmoSlotConfig ammoSlot, ItemStack ammoStack) {
        if (ammoStack.isEmpty()) {
            return true;
        }

        try {
            if (isAmmoCandidateForGun(gunStack, ammoStack)) {
                return false;
            }
        } catch (ReflectiveOperationException ignored) {
            return false;
        }

        if (ammoSlot.getType() == AmmoSlotType.CUSTOMNPC_PROJECTILE) {
            return true;
        }

        if (!isAmmoBoxStack(ammoStack)) {
            return false;
        }

        try {
            boolean creative = (boolean) I_AMMO_BOX_IS_CREATIVE.invoke(ammoStack.getItem(), ammoStack)
                    || (boolean) I_AMMO_BOX_IS_ALL_TYPE_CREATIVE.invoke(ammoStack.getItem(), ammoStack);
            if (creative) {
                return false;
            }

            int ammoCount = ((Number) I_AMMO_BOX_GET_AMMO_COUNT.invoke(ammoStack.getItem(), ammoStack)).intValue();
            return ammoCount <= 0;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isAmmoBoxStack(ItemStack ammoStack) {
        return !ammoStack.isEmpty() && I_AMMO_BOX_CLASS != null && I_AMMO_BOX_CLASS.isInstance(ammoStack.getItem());
    }

    private static ItemStack createSuppliedAmmoForSlot(ItemStack gunStack, ItemStack existingAmmo, int count) {
        if (isAmmoBoxStack(existingAmmo)) {
            return createAmmoBoxSupplyForGun(gunStack, existingAmmo, count);
        }

        return createAmmoStackForGun(gunStack, count);
    }

    private static ItemStack createAmmoBoxSupplyForGun(ItemStack gunStack, ItemStack ammoBoxStack, int count) {
        if (count <= 0 || ammoBoxStack.isEmpty() || I_AMMO_BOX_SET_AMMO_COUNT == null || I_AMMO_BOX_SET_AMMO_ID == null) {
            return ItemStack.EMPTY;
        }

        ResourceLocation ammoId = getGunAmmoId(gunStack);
        if (ammoId == null) {
            return ItemStack.EMPTY;
        }

        try {
            ItemStack suppliedBox = ammoBoxStack.copy();
            I_AMMO_BOX_SET_AMMO_ID.invoke(suppliedBox.getItem(), suppliedBox, ammoId);
            I_AMMO_BOX_SET_AMMO_COUNT.invoke(suppliedBox.getItem(), suppliedBox, count);
            return suppliedBox;
        } catch (ReflectiveOperationException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static boolean tryReloadFromCustomNpcProjectileAmmo(LivingEntity shooter, InteractionHand hand, ItemStack gunStack, Object operator) {
        ItemStack projectileAmmo = getCustomNpcProjectileStack(shooter);
        if (projectileAmmo.isEmpty()) {
            return false;
        }

        Object iGun = getIGun(gunStack);
        if (iGun == null) {
            return false;
        }

        int currentAmmo = getCurrentAmmoCount(gunStack);
        int maxAmmo = getMaxAmmoCount(gunStack);
        int neededAmmo = maxAmmo - currentAmmo;
        if (neededAmmo <= 0) {
            return false;
        }

        Object reloadStateType = currentAmmo <= 0 ? RELOAD_STATE_EMPTY_RELOAD_FEEDING : RELOAD_STATE_TACTICAL_RELOAD_FEEDING;

        try {
            GUN_OPERATOR_INITIAL_DATA.invoke(operator);
            GUN_OPERATOR_AIM.invoke(operator, false);
            if (!startCustomAmmoReloadState(shooter, gunStack, operator, reloadStateType)) {
                return false;
            }

            Object iAmmo = I_AMMO_GET_OR_NULL.invoke(null, projectileAmmo);
            if (iAmmo != null && (boolean) I_AMMO_IS_AMMO_OF_GUN.invoke(iAmmo, gunStack, projectileAmmo)) {
                int loadAmount = Math.min(neededAmmo, projectileAmmo.getCount());
                if (loadAmount <= 0) {
                    return false;
                }

                I_GUN_SET_CURRENT_AMMO_COUNT.invoke(iGun, gunStack, currentAmmo + loadAmount);
                ItemStack updatedProjectileAmmo = projectileAmmo.copy();
                updatedProjectileAmmo.shrink(loadAmount);
                setCustomNpcProjectileStack(shooter, updatedProjectileAmmo.isEmpty() ? ItemStack.EMPTY : updatedProjectileAmmo);
                shooter.startUsingItem(hand);
                return true;
            }

            if (I_AMMO_BOX_CLASS.isInstance(projectileAmmo.getItem())
                    && (boolean) I_AMMO_BOX_IS_AMMO_BOX_OF_GUN.invoke(projectileAmmo.getItem(), gunStack, projectileAmmo)) {
                boolean creative = (boolean) I_AMMO_BOX_IS_CREATIVE.invoke(projectileAmmo.getItem(), projectileAmmo)
                        || (boolean) I_AMMO_BOX_IS_ALL_TYPE_CREATIVE.invoke(projectileAmmo.getItem(), projectileAmmo);
                int ammoCount = creative ? neededAmmo : ((Number) I_AMMO_BOX_GET_AMMO_COUNT.invoke(projectileAmmo.getItem(), projectileAmmo)).intValue();
                int loadAmount = Math.min(neededAmmo, ammoCount);
                if (loadAmount <= 0) {
                    return false;
                }

                I_GUN_SET_CURRENT_AMMO_COUNT.invoke(iGun, gunStack, currentAmmo + loadAmount);
                if (!creative) {
                    I_AMMO_BOX_SET_AMMO_COUNT.invoke(projectileAmmo.getItem(), projectileAmmo, ammoCount - loadAmount);
                }
                shooter.startUsingItem(hand);
                return true;
            }
        } catch (ReflectiveOperationException ignored) {
            return false;
        }

        return false;
    }

    private static boolean startCustomAmmoReloadState(LivingEntity shooter, ItemStack gunStack, Object operator, @Nullable Object reloadStateType) {
        if (reloadStateType == null
                || GUN_OPERATOR_GET_DATA_HOLDER == null
                || SHOOTER_DATA_HOLDER_RELOAD_TIMESTAMP == null
                || SHOOTER_DATA_HOLDER_RELOAD_STATE_TYPE == null
                || ABSTRACT_GUN_ITEM_START_RELOAD == null) {
            return false;
        }

        try {
            Object dataHolder = GUN_OPERATOR_GET_DATA_HOLDER.invoke(operator);
            if (dataHolder == null) {
                return false;
            }

            SHOOTER_DATA_HOLDER_RELOAD_STATE_TYPE.set(dataHolder, reloadStateType);
            SHOOTER_DATA_HOLDER_RELOAD_TIMESTAMP.setLong(dataHolder, System.currentTimeMillis());

            if (!(boolean) ABSTRACT_GUN_ITEM_START_RELOAD.invoke(gunStack.getItem(), dataHolder, gunStack, shooter)) {
                SHOOTER_DATA_HOLDER_RELOAD_STATE_TYPE.set(dataHolder, RELOAD_STATE_NOT_RELOADING);
                SHOOTER_DATA_HOLDER_RELOAD_TIMESTAMP.setLong(dataHolder, -1L);
                return false;
            }

            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static ItemStack getAmmoSlotStack(LivingEntity shooter, AmmoSlotConfig ammoSlot) {
        return switch (ammoSlot.getType()) {
            case EQUIPMENT -> getEquipmentAmmoSlotStack(shooter, ammoSlot);
            case INVENTORY -> getInventoryAmmoSlotStack(shooter, ammoSlot.getIndex());
            case CUSTOMNPC_PROJECTILE -> getCustomNpcProjectileStack(shooter);
            case CUSTOMNPC_DROP -> getCustomNpcDropStack(shooter, ammoSlot.getIndex());
        };
    }

    private static boolean setAmmoSlotStack(LivingEntity shooter, AmmoSlotConfig ammoSlot, ItemStack stack) {
        return switch (ammoSlot.getType()) {
            case EQUIPMENT -> setEquipmentAmmoSlotStack(shooter, ammoSlot, stack);
            case INVENTORY -> setInventoryAmmoSlotStack(shooter, ammoSlot.getIndex(), stack);
            case CUSTOMNPC_PROJECTILE -> setCustomNpcProjectileStack(shooter, stack);
            case CUSTOMNPC_DROP -> setCustomNpcDropStack(shooter, ammoSlot.getIndex(), stack);
        };
    }

    private static ItemStack getInventoryAmmoSlotStack(LivingEntity shooter, int index) {
        if (!(shooter instanceof InventoryCarrier inventoryCarrier)) {
            return ItemStack.EMPTY;
        }

        SimpleContainer inventory = inventoryCarrier.getInventory();
        if (index < 0 || index >= inventory.getContainerSize()) {
            return ItemStack.EMPTY;
        }

        return inventory.getItem(index);
    }

    private static ItemStack getEquipmentAmmoSlotStack(LivingEntity shooter, AmmoSlotConfig ammoSlot) {
        if (ammoSlot.getEquipmentSlot() == null) {
            return ItemStack.EMPTY;
        }

        if (isCustomNpcAvailable() && CnpcSlotAccess.getInventory(shooter) != null) {
            return switch (ammoSlot.getEquipmentSlot()) {
                case MAINHAND -> getCustomNpcRightHandStack(shooter);
                case OFFHAND -> getCustomNpcLeftHandStack(shooter);
                default -> shooter.getItemBySlot(ammoSlot.getEquipmentSlot());
            };
        }

        return shooter.getItemBySlot(ammoSlot.getEquipmentSlot());
    }

    private static boolean setInventoryAmmoSlotStack(LivingEntity shooter, int index, ItemStack stack) {
        if (!(shooter instanceof InventoryCarrier inventoryCarrier)) {
            return false;
        }

        SimpleContainer inventory = inventoryCarrier.getInventory();
        if (index < 0 || index >= inventory.getContainerSize()) {
            return false;
        }

        inventory.setItem(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        return true;
    }

    private static boolean setEquipmentAmmoSlotStack(LivingEntity shooter, AmmoSlotConfig ammoSlot, ItemStack stack) {
        if (ammoSlot.getEquipmentSlot() == null) {
            return false;
        }

        if (isCustomNpcAvailable() && CnpcSlotAccess.getInventory(shooter) != null) {
            return switch (ammoSlot.getEquipmentSlot()) {
                case MAINHAND -> setCustomNpcRightHandStack(shooter, stack);
                case OFFHAND -> setCustomNpcLeftHandStack(shooter, stack);
                default -> setGenericEquipmentAmmoSlotStack(shooter, ammoSlot, stack);
            };
        }

        return setGenericEquipmentAmmoSlotStack(shooter, ammoSlot, stack);
    }

    private static boolean setGenericEquipmentAmmoSlotStack(LivingEntity shooter, AmmoSlotConfig ammoSlot, ItemStack stack) {
        if (ammoSlot.getEquipmentSlot() == null) {
            return false;
        }

        shooter.setItemSlot(ammoSlot.getEquipmentSlot(), stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        return true;
    }

    private static ItemStack getCustomNpcRightHandStack(LivingEntity shooter) {
        if (!isCustomNpcAvailable()) {
            return ItemStack.EMPTY;
        }
        return CnpcSlotAccess.getRightHandStack(shooter);
    }

    private static ItemStack getCustomNpcLeftHandStack(LivingEntity shooter) {
        if (!isCustomNpcAvailable()) {
            return ItemStack.EMPTY;
        }
        return CnpcSlotAccess.getLeftHandStack(shooter);
    }

    private static boolean isCustomNpcAvailable() {
        return ModList.get().isLoaded("customnpcs");
    }

    private static ItemStack getCustomNpcProjectileStack(LivingEntity shooter) {
        if (!isCustomNpcAvailable()) {
            return ItemStack.EMPTY;
        }
        return CnpcSlotAccess.getProjectileStack(shooter);
    }

    private static ItemStack getCustomNpcDropStack(LivingEntity shooter, int slot) {
        if (!isCustomNpcAvailable()) {
            return ItemStack.EMPTY;
        }
        return CnpcSlotAccess.getDropStack(shooter, slot);
    }

    private static boolean setCustomNpcProjectileStack(LivingEntity shooter, ItemStack stack) {
        if (!isCustomNpcAvailable()) {
            return false;
        }

        if (!CnpcSlotAccess.setProjectileStack(shooter, stack)) {
            return false;
        }

        ItemStack observedStack = getCustomNpcProjectileStack(shooter);
        if (!matchesExpectedAmmoSlotStack(stack, observedStack)) {
            return false;
        }

        return true;
    }

    private static boolean setCustomNpcLeftHandStack(LivingEntity shooter, ItemStack stack) {
        if (!isCustomNpcAvailable()) {
            return false;
        }

        if (!CnpcSlotAccess.setLeftHandStack(shooter, stack)) {
            return false;
        }

        ItemStack observedStack = getCustomNpcLeftHandStack(shooter);
        if (!matchesExpectedAmmoSlotStack(stack, observedStack)) {
            return false;
        }

        return true;
    }

    private static boolean setCustomNpcRightHandStack(LivingEntity shooter, ItemStack stack) {
        if (!isCustomNpcAvailable()) {
            return false;
        }

        if (!CnpcSlotAccess.setRightHandStack(shooter, stack)) {
            return false;
        }

        ItemStack observedStack = getCustomNpcRightHandStack(shooter);
        if (!matchesExpectedAmmoSlotStack(stack, observedStack)) {
            return false;
        }

        return true;
    }

    private static boolean setCustomNpcDropStack(LivingEntity shooter, int slot, ItemStack stack) {
        if (!isCustomNpcAvailable()) {
            return false;
        }

        if (!CnpcSlotAccess.setDropStack(shooter, slot, stack)) {
            return false;
        }

        ItemStack observedStack = getCustomNpcDropStack(shooter, slot);
        if (!matchesExpectedAmmoSlotStack(stack, observedStack)) {
            return false;
        }

        return true;
    }

    private static boolean matchesExpectedAmmoSlotStack(ItemStack expected, ItemStack actual) {
        if (expected.isEmpty()) {
            return actual.isEmpty();
        }

        return !actual.isEmpty()
                    && ItemStack.isSameItem(expected, actual);
    }

    private static ItemStack createAmmoStackForGun(ItemStack gunStack, int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        if (AMMO_ITEM_BUILDER_CREATE == null || GUN_DATA_GET_AMMO_ID == null) {
            return ItemStack.EMPTY;
        }

        ResourceLocation ammoId = getGunAmmoId(gunStack);
        if (ammoId == null) {
            return ItemStack.EMPTY;
        }

        try {
            Object builder = AMMO_ITEM_BUILDER_CREATE.invoke(null);
            AMMO_ITEM_BUILDER_SET_ID.invoke(builder, ammoId);
            AMMO_ITEM_BUILDER_SET_COUNT.invoke(builder, count);
            Object builtStack = AMMO_ITEM_BUILDER_BUILD.invoke(builder);
            return builtStack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException e) {
            return ItemStack.EMPTY;
        }
    }

    @Nullable
    private static ResourceLocation getGunAmmoId(ItemStack gunStack) {
        if (GUN_DATA_GET_AMMO_ID == null) {
            return null;
        }

        Object gunData = getGunData(gunStack);
        if (gunData == null) {
            return null;
        }

        try {
            return (ResourceLocation) GUN_DATA_GET_AMMO_ID.invoke(gunData);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Nullable
    private static Object getGunData(ItemStack gunStack) {
        Object iGun = getIGun(gunStack);
        if (iGun == null) {
            return null;
        }

        try {
            ResourceLocation gunId = (ResourceLocation) I_GUN_GET_GUN_ID.invoke(iGun, gunStack);
            Object optional = TIMELESS_GET_COMMON_GUN_INDEX.invoke(null, gunId);
            Object commonGunIndex = optional.getClass().getMethod("orElse", Object.class).invoke(optional, new Object[] {null});
            if (commonGunIndex == null) {
                return null;
            }
            return COMMON_GUN_INDEX_GET_GUN_DATA.invoke(commonGunIndex);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void setFireMode(ItemStack gunStack, @Nullable String fireModeName) throws ReflectiveOperationException {
        Object iGun = getIGun(gunStack);
        Object gunData = getGunData(gunStack);
        if (iGun == null || gunData == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<Object> supportedModes = (List<Object>) GUN_DATA_GET_FIRE_MODE_SET.invoke(gunData);
        Object requestedMode = resolveRequestedFireMode(fireModeName, supportedModes);
        if (requestedMode == null) {
            return;
        }

        Object currentMode = I_GUN_GET_FIRE_MODE.invoke(iGun, gunStack);
        if (!requestedMode.equals(currentMode)) {
            I_GUN_SET_FIRE_MODE.invoke(iGun, gunStack, requestedMode);
        }
    }

    @Nullable
    private static Object resolveRequestedFireMode(@Nullable String fireModeName, List<Object> supportedModes) {
        if (supportedModes == null || supportedModes.isEmpty()) {
            return null;
        }

        if (fireModeName == null || fireModeName.isBlank()) {
            return null;
        }

        return findSupportedFireMode(supportedModes, fireModeName);
    }

    @Nullable
    private static Object getCurrentFireMode(Object iGun, ItemStack gunStack) throws ReflectiveOperationException {
        return I_GUN_GET_FIRE_MODE.invoke(iGun, gunStack);
    }

    private static boolean isFireMode(Object fireMode, String expectedMode) {
        return fireMode instanceof Enum<?> enumMode && enumMode.name().equalsIgnoreCase(expectedMode);
    }

    @Nullable
    private static Number getFireModeIntervalMs(LivingEntity shooter, ItemStack gunStack, Object gunData, @Nullable Object fireMode)
            throws ReflectiveOperationException {
        if (isFireMode(fireMode, "BURST") && GUN_DATA_GET_BURST_DATA != null && BURST_DATA_GET_MIN_INTERVAL != null) {
            Object burstData = GUN_DATA_GET_BURST_DATA.invoke(gunData);
            if (burstData != null) {
                Number minIntervalSeconds = (Number) BURST_DATA_GET_MIN_INTERVAL.invoke(burstData);
                if (minIntervalSeconds != null) {
                    return minIntervalSeconds.doubleValue() * 1000.0D;
                }
            }
        }

        return (Number) GUN_DATA_GET_SHOOT_INTERVAL.invoke(gunData, shooter, fireMode, gunStack);
    }

    private static void releaseTriggerForSemiMode(LivingEntity shooter, Object operator, ItemStack gunStack)
            throws ReflectiveOperationException {
        Object iGun = getIGun(gunStack);
        if (iGun == null) {
            return;
        }

        Object fireMode = getCurrentFireMode(iGun, gunStack);
        if (!isFireMode(fireMode, "SEMI")) {
            return;
        }

        shooter.stopUsingItem();
    }

    @Nullable
    private static Object findSupportedFireMode(List<Object> supportedModes, String requestedName) {
        for (Object supportedMode : supportedModes) {
            if (supportedMode instanceof Enum<?> enumMode && enumMode.name().equalsIgnoreCase(requestedName)) {
                return supportedMode;
            }
        }

        return null;
    }

    private static Supplier<Float> pitchSupplier(LivingEntity shooter, LivingEntity target) {
        return () -> {
            double dx = target.getX() - shooter.getX();
            double dy = target.getEyeY() - shooter.getEyeY();
            double dz = target.getZ() - shooter.getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            return (float) (-Math.toDegrees(Math.atan2(dy, horizontalDistance)));
        };
    }

    private static Supplier<Float> yawSupplier(LivingEntity shooter, LivingEntity target) {
        return () -> {
            double dx = target.getX() - shooter.getX();
            double dz = target.getZ() - shooter.getZ();
            return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        };
    }
}
