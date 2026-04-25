package com.nameless.indestructible.world.capability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

final class CnpcSlotAccess {
    private CnpcSlotAccess() {
    }

    // ItemStackWrapper(ItemStack) is protected — reflect it once so we can construct
    // wrappers for any ItemStack without requiring the CNPC capability on the stack.
    @Nullable
    private static final Constructor<ItemStackWrapper> ITEM_STACK_WRAPPER_CTOR;

    static {
        Constructor<ItemStackWrapper> ctor = null;
        try {
            ctor = ItemStackWrapper.class.getDeclaredConstructor(ItemStack.class);
            ctor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // Will fall back to null; wrap() will return null if this fails.
        }
        ITEM_STACK_WRAPPER_CTOR = ctor;
    }

    @Nullable
    static DataInventory getInventory(LivingEntity shooter) {
        if (shooter instanceof EntityNPCInterface npc) {
            return npc.inventory;
        }
        return null;
    }

    static ItemStack getRightHandStack(LivingEntity shooter) {
        DataInventory inventory = getInventory(shooter);
        return inventory == null ? ItemStack.EMPTY : unwrap(inventory.getRightHand());
    }

    static ItemStack getLeftHandStack(LivingEntity shooter) {
        DataInventory inventory = getInventory(shooter);
        return inventory == null ? ItemStack.EMPTY : unwrap(inventory.getLeftHand());
    }

    static ItemStack getProjectileStack(LivingEntity shooter) {
        DataInventory inventory = getInventory(shooter);
        return inventory == null ? ItemStack.EMPTY : unwrap(inventory.getProjectile());
    }

    static ItemStack getDropStack(LivingEntity shooter, int slot) {
        DataInventory inventory = getInventory(shooter);
        return inventory == null ? ItemStack.EMPTY : unwrap(inventory.getDropItem(slot));
    }

    static boolean setProjectileStack(LivingEntity shooter, ItemStack stack) {
        DataInventory inventory = getInventory(shooter);
        if (inventory == null) {
            return false;
        }

        IItemStack wrappedStack = wrap(stack);
        if (!stack.isEmpty() && wrappedStack == null) {
            return false;
        }

        inventory.setProjectile(wrappedStack);
        return true;
    }

    static boolean setRightHandStack(LivingEntity shooter, ItemStack stack) {
        DataInventory inventory = getInventory(shooter);
        if (inventory == null) {
            return false;
        }

        IItemStack wrappedStack = wrap(stack);
        if (!stack.isEmpty() && wrappedStack == null) {
            return false;
        }

        inventory.setRightHand(wrappedStack);
        return true;
    }

    static boolean setLeftHandStack(LivingEntity shooter, ItemStack stack) {
        DataInventory inventory = getInventory(shooter);
        if (inventory == null) {
            return false;
        }

        IItemStack wrappedStack = wrap(stack);
        if (!stack.isEmpty() && wrappedStack == null) {
            return false;
        }

        inventory.setLeftHand(wrappedStack);
        return true;
    }

    static boolean setDropStack(LivingEntity shooter, int slot, ItemStack stack) {
        DataInventory inventory = getInventory(shooter);
        if (inventory == null) {
            return false;
        }

        IItemStack wrappedStack = wrap(stack);
        if (!stack.isEmpty() && wrappedStack == null) {
            return false;
        }

        inventory.setDropItem(slot, wrappedStack, getDropChance(shooter, slot));
        return true;
    }

    static float getDropChance(LivingEntity shooter, int slot) {
        DataInventory inventory = getInventory(shooter);
        if (inventory == null) {
            return 100.0F;
        }

        return inventory.dropchance.getOrDefault(slot, 100.0F);
    }

    private static ItemStack unwrap(@Nullable IItemStack wrapper) {
        return wrapper == null ? ItemStack.EMPTY : ItemStackWrapper.MCItem(wrapper);
    }

    @Nullable
    private static IItemStack wrap(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (ITEM_STACK_WRAPPER_CTOR == null) {
            return null;
        }
        try {
            return ITEM_STACK_WRAPPER_CTOR.newInstance(stack.copy());
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}