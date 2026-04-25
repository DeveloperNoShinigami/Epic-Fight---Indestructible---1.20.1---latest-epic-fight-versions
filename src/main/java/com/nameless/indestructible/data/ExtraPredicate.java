package com.nameless.indestructible.data;

import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.types.LongHitAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.entity.HealthPoint;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import javax.annotation.Nullable;
import java.util.List;

import static com.nameless.indestructible.main.Indestructible.NEUTRALIZE_ANIMATION_LIST;

public class ExtraPredicate {
    private abstract static class BaseCondition<T extends MobPatch<?>> implements Condition<T> {
        @Override
        public Condition<T> read(CompoundTag tag) {
            return this;
        }

        @Override
        public CompoundTag serializePredicate() {
            return new CompoundTag();
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public List<ParameterEditor> getAcceptingParameters(Screen screen) {
            return List.of();
        }
    }

    public static class TargetIsGuardBreak<T extends MobPatch<?>> extends BaseCondition<T> {
        private final boolean invert;
        public TargetIsGuardBreak(boolean invert){
            this.invert = invert;
        }
        public boolean predicate(T mobpatch) {

            LivingEntityPatch<?> tartgetpatch = EpicFightCapabilities.getEntityPatch(mobpatch.getTarget(), LivingEntityPatch.class);
            if(tartgetpatch == null) return !this.invert;
            boolean targetisguardbreak = tartgetpatch.getEntityState().hurtLevel() > 1 && tartgetpatch.getAnimator().getPlayerFor(null).getAnimation() instanceof LongHitAnimation animation && NEUTRALIZE_ANIMATION_LIST.contains((StaticAnimation) animation);
            if (!this.invert) {
                return targetisguardbreak;
            } else {
                return !targetisguardbreak;
            }
        }
    }

    public static class TargetIsKnockDown<T extends MobPatch<?>> extends BaseCondition<T> {
        private final boolean invert;
        public TargetIsKnockDown(boolean invert){
            this.invert = invert;
        }
        public boolean predicate(T mobpatch) {
            LivingEntityPatch<?> tartgetpatch = EpicFightCapabilities.getEntityPatch(mobpatch.getTarget(), LivingEntityPatch.class);
            if(tartgetpatch == null) return !this.invert;
            boolean targetisknockdown = tartgetpatch.getEntityState().knockDown();
            if (!this.invert) {
                return targetisknockdown;
            } else {
                return !targetisknockdown;
            }
        }
    }

    public static class TargetWithinState<T extends MobPatch<?>> extends BaseCondition<T> {
        private final int minLevel;
        private final int maxLevel;

        public TargetWithinState(int minLevel, int maxLevel) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        public boolean predicate(T mobpatch) {
            LivingEntityPatch<?> tartgetpatch = EpicFightCapabilities.getEntityPatch(mobpatch.getTarget(), LivingEntityPatch.class);
            if(tartgetpatch == null) return false;
            int level = tartgetpatch.getEntityState().getLevel();
            return this.minLevel <= level && level <= this.maxLevel;
        }
    }

    public static class SelfStamina<T extends MobPatch<?>> extends BaseCondition<T> {
        private final float value;
        private final HealthPoint.Comparator comparator;

        public SelfStamina(float value, HealthPoint.Comparator comparator) {
            this.value = value;
            this.comparator = comparator;
        }

        public boolean predicate(T mobpatch) {
            if(!(mobpatch instanceof AdvancedCustomHumanoidMobPatch)) return false;
            float stamina = ((AdvancedCustomHumanoidMobPatch<?>) mobpatch).getStamina();
            float maxstamina = ((AdvancedCustomHumanoidMobPatch<?>) mobpatch).getMaxStamina();
            switch (this.comparator) {
                case LESS_ABSOLUTE:
                    return this.value > stamina;
                case GREATER_ABSOLUTE:
                    return this.value < stamina;
                case LESS_RATIO:
                    return this.value > stamina / maxstamina;
                case GREATER_RATIO:
                    return this.value < stamina / maxstamina;
            }

            return true;
        }
    }

    public static class TargetIsUsingItem<T extends MobPatch<?>> extends BaseCondition<T> {
        private final boolean isEdible;
        public TargetIsUsingItem(boolean isEdible){
            this.isEdible = isEdible;
        }
        public boolean predicate(T mobpatch) {
            LivingEntity target = mobpatch.getTarget();
            if (target.isUsingItem()) {
                ItemStack item = target.getUseItem();
                if (isEdible) {
                    return item.getItem() instanceof PotionItem || item.getItem().isEdible();
                } else {
                    return !(item.getItem() instanceof PotionItem || item.getItem().isEdible());
                }
            }
            return false;
        }
    }

    public static class Phase<T extends MobPatch<?>> extends BaseCondition<T> {
        private final int minLevel;
        private final int maxLevel;

        public Phase(int minLevel, int maxLevel) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        public boolean predicate(T mobpatch) {
            if(mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch){
                int phase = advancedCustomHumanoidMobPatch.getPhase();
                return this.minLevel <= phase && phase <= this.maxLevel;
            }
            return false;
        }
    }

    public static class MainhandWeaponCategory<T extends MobPatch<?>> extends BaseCondition<T> {
        private final WeaponCategory category;

        public MainhandWeaponCategory(String categoryName) {
            this.category = WeaponCategory.ENUM_MANAGER.get(categoryName);
        }

        public boolean predicate(T mobpatch) {
            if (this.category == null || !(mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch)) {
                return false;
            }

            return advancedCustomHumanoidMobPatch.getResolvedWeaponCategory(InteractionHand.MAIN_HAND) == this.category;
        }
    }

    public static class OffhandWeaponCategory<T extends MobPatch<?>> extends BaseCondition<T> {
        private final WeaponCategory category;

        public OffhandWeaponCategory(String categoryName) {
            this.category = WeaponCategory.ENUM_MANAGER.get(categoryName);
        }

        public boolean predicate(T mobpatch) {
            if (this.category == null || !(mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch)) {
                return false;
            }

            return advancedCustomHumanoidMobPatch.getResolvedWeaponCategory(InteractionHand.OFF_HAND) == this.category;
        }
    }

    public static class HasGearInInventory<T extends MobPatch<?>> extends BaseCondition<T> {
        private final ResourceLocation itemId;
        @Nullable
        private final EquipmentSlot slot;
        private final boolean includeEquipped;

        public HasGearInInventory(ResourceLocation itemId, @Nullable EquipmentSlot slot, boolean includeEquipped) {
            this.itemId = itemId;
            this.slot = slot;
            this.includeEquipped = includeEquipped;
        }

        public boolean predicate(T mobpatch) {
            if (mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch) {
                return advancedCustomHumanoidMobPatch.hasGearInInventory(this.itemId, this.slot, this.includeEquipped);
            }

            return false;
        }
    }

    public static class HasAmmo<T extends MobPatch<?>> extends BaseCondition<T> {
        private final InteractionHand hand;
        private final boolean invert;

        public HasAmmo(InteractionHand hand, boolean invert) {
            this.hand = hand;
            this.invert = invert;
        }

        public boolean predicate(T mobpatch) {
            if (!(mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch)) {
                return false;
            }

            boolean hasAmmo = advancedCustomHumanoidMobPatch.hasAmmo(this.hand);
            return this.invert ? !hasAmmo : hasAmmo;
        }
    }

    public static class AmmoIsEmpty<T extends MobPatch<?>> extends BaseCondition<T> {
        @Nullable
        private final InteractionHand hand;
        private final boolean invert;

        public AmmoIsEmpty(@Nullable InteractionHand hand, boolean invert) {
            this.hand = hand;
            this.invert = invert;
        }

        public boolean predicate(T mobpatch) {
            if (!(mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch)) {
                return false;
            }

            boolean ammoEmpty = advancedCustomHumanoidMobPatch.isAmmoEmpty(this.hand);
            return this.invert ? !ammoEmpty : ammoEmpty;
        }
    }

    public static class AmmoNotFull<T extends MobPatch<?>> extends BaseCondition<T> {
        @Nullable
        private final InteractionHand hand;
        private final boolean invert;

        public AmmoNotFull(@Nullable InteractionHand hand, boolean invert) {
            this.hand = hand;
            this.invert = invert;
        }

        public boolean predicate(T mobpatch) {
            if (!(mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch)) {
                return false;
            }

            boolean ammoNotFull = advancedCustomHumanoidMobPatch.isAmmoNotFull(this.hand);
            return this.invert ? !ammoNotFull : ammoNotFull;
        }
    }

    public static class AmmoHasReserve<T extends MobPatch<?>> extends BaseCondition<T> {
        @Nullable
        private final InteractionHand hand;
        private final boolean invert;

        public AmmoHasReserve(@Nullable InteractionHand hand, boolean invert) {
            this.hand = hand;
            this.invert = invert;
        }

        public boolean predicate(T mobpatch) {
            if (!(mobpatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedCustomHumanoidMobPatch)) {
                return false;
            }

            boolean hasReserveAmmo = advancedCustomHumanoidMobPatch.hasReserveAmmo(this.hand);
            return this.invert ? !hasReserveAmmo : hasReserveAmmo;
        }
    }

    public static class TargetBlocking<T extends MobPatch<?>> extends BaseCondition<T> {
        private final boolean invert;

        public TargetBlocking(boolean invert) {
            this.invert = invert;
        }

        public boolean predicate(T mobpatch) {
            LivingEntity target = mobpatch.getTarget();
            if (target == null) {
                return this.invert;
            }

            LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
            boolean blocking = target.isBlocking()
                    || (target.isUsingItem() && target.getUseItem().getUseAnimation() == UseAnim.BLOCK)
                    || (targetPatch instanceof AdvancedCustomHumanoidMobPatch<?> advancedTargetPatch && advancedTargetPatch.isBlocking());

            return this.invert ? !blocking : blocking;
        }
    }

    public static class TargetUsingShield<T extends MobPatch<?>> extends BaseCondition<T> {
        private final boolean invert;

        public TargetUsingShield(boolean invert) {
            this.invert = invert;
        }

        public boolean predicate(T mobpatch) {
            LivingEntity target = mobpatch.getTarget();
            if (target == null) {
                return this.invert;
            }

            ItemStack useItem = target.getUseItem();
            boolean usingShield = (target.isUsingItem() && useItem.getItem() instanceof ShieldItem)
                    || target.getMainHandItem().getItem() instanceof ShieldItem
                    || target.getOffhandItem().getItem() instanceof ShieldItem;

            return this.invert ? !usingShield : usingShield;
        }
    }

    public static class NoTarget<T extends MobPatch<?>> extends BaseCondition<T> {
        public boolean predicate(T mobpatch) {
            LivingEntity target = mobpatch.getTarget();
            return target == null || !target.isAlive();
        }
    }

    public static class HasTarget<T extends MobPatch<?>> extends BaseCondition<T> {
        public boolean predicate(T mobpatch) {
            LivingEntity target = mobpatch.getTarget();
            return target != null && target.isAlive();
        }
    }

}
