package com.nameless.indestructible.world.capability;

import com.nameless.indestructible.data.AdvancedMobpatchReloader.AmmoSlotConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

interface TaczCompatBackend {
    boolean isAvailable();

    boolean isTaczGun(ItemStack stack);

    @Nullable
    InteractionHand findGunHand(LivingEntity entity, @Nullable InteractionHand preferredHand);

    boolean hasUsableAmmo(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots);

    boolean hasReserveAmmo(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots);

    boolean isAmmoEmpty(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots);

    boolean isAmmoNotFull(LivingEntity shooter, ItemStack gunStack);

    boolean canReload(LivingEntity shooter, ItemStack gunStack);

    boolean tryReload(LivingEntity shooter, InteractionHand hand, boolean requireAmmo, List<AmmoSlotConfig> ammoSlots);

    boolean tryShoot(LivingEntity shooter, LivingEntity target, InteractionHand hand, @Nullable String fireModeName);

    boolean tryAim(LivingEntity shooter, InteractionHand hand);

    void stopAiming(LivingEntity shooter);

    boolean isReloading(LivingEntity shooter);

    boolean isAiming(LivingEntity shooter);

    int getShootIntervalTicks(LivingEntity shooter, ItemStack gunStack);
}