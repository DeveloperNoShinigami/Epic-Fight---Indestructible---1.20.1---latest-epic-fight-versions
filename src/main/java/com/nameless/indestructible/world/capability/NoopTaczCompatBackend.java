package com.nameless.indestructible.world.capability;

import com.nameless.indestructible.data.AdvancedMobpatchReloader.AmmoSlotConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

final class NoopTaczCompatBackend implements TaczCompatBackend {
    static final NoopTaczCompatBackend INSTANCE = new NoopTaczCompatBackend();

    private NoopTaczCompatBackend() {
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean isTaczGun(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionHand findGunHand(LivingEntity entity, @Nullable InteractionHand preferredHand) {
        return null;
    }

    @Override
    public boolean hasUsableAmmo(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
        return false;
    }

    @Override
    public boolean hasReserveAmmo(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
        return false;
    }

    @Override
    public boolean isAmmoEmpty(LivingEntity shooter, ItemStack gunStack, List<AmmoSlotConfig> ammoSlots) {
        return true;
    }

    @Override
    public boolean isAmmoNotFull(LivingEntity shooter, ItemStack gunStack) {
        return false;
    }

    @Override
    public boolean canReload(LivingEntity shooter, ItemStack gunStack) {
        return false;
    }

    @Override
    public boolean tryReload(LivingEntity shooter, InteractionHand hand, boolean requireAmmo, List<AmmoSlotConfig> ammoSlots) {
        return false;
    }

    @Override
    public boolean tryShoot(LivingEntity shooter, LivingEntity target, InteractionHand hand, @Nullable String fireModeName) {
        return false;
    }

    @Override
    public boolean tryAim(LivingEntity shooter, InteractionHand hand) {
        return false;
    }

    @Override
    public void stopAiming(LivingEntity shooter) {
    }

    @Override
    public boolean isReloading(LivingEntity shooter) {
        return false;
    }

    @Override
    public boolean isAiming(LivingEntity shooter) {
        return false;
    }

    @Override
    public int getShootIntervalTicks(LivingEntity shooter, ItemStack gunStack) {
        return 0;
    }
}