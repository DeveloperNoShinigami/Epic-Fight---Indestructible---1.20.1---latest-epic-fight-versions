package com.nameless.indestructible.world.ai.task;

import com.nameless.indestructible.mixin.BehaviorMixin;
import com.google.common.collect.ImmutableMap;
import com.nameless.indestructible.mixin.BehaviorSeriesMixin;
import com.nameless.indestructible.mixin.CombatBehaviorsMixin;
import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileWeaponItem;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

import java.util.List;

public class AdvancedCombatBehavior<T extends AdvancedCustomHumanoidMobPatch<?>> extends Behavior<Mob> {
	protected final AdvancedCustomHumanoidMobPatch<?> mobpatch;
	protected final CombatBehaviors<HumanoidMobPatch<?>> combatBehaviors;
	
	public AdvancedCombatBehavior(AdvancedCustomHumanoidMobPatch<?> mobpatch, CombatBehaviors<HumanoidMobPatch<?>> combatBehaviors) {
		super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
	    this.mobpatch = mobpatch;
	    this.combatBehaviors = combatBehaviors;
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel levelIn, Mob entityIn) {
		return !this.isHoldingRangeWeapon(entityIn) && this.isValidTarget(this.mobpatch.getTarget());
	}
	
	@Override
	protected boolean canStillUse(ServerLevel levelIn, Mob entityIn, long gameTimeIn) {
		return this.checkExtraStartConditions(levelIn, entityIn) && BehaviorUtils.canSee(entityIn, this.mobpatch.getTarget()) && !this.mobpatch.getEntityState().hurt();
	}
	
	@Override
	protected void tick(ServerLevel worldIn, Mob entityIn, long gameTimeIn) {
		boolean inaction = mobpatch.isBlocking() || mobpatch.getInactionTime() >0;
		if (this.mobpatch.getTarget() != null) {
			EntityState state = this.mobpatch.getEntityState();
			this.combatBehaviors.tick();
			if (this.combatBehaviors.hasActivatedMove()) {
				int count =  ((CombatBehaviorsMixin<?>)combatBehaviors).getCurrentBehaviorPointer();
				CombatBehaviors.BehaviorSeries<?> currentBehaviorSeries = count >= 0
						? ((CombatBehaviorsMixin<?>)combatBehaviors).getBehaviorSeriesList().get(count)
						: null;
				boolean preserveLoop = this.shouldPreserveCurrentTaczLoop(currentBehaviorSeries);
				if (preserveLoop) {
					this.mobpatch.refreshTaczSustainHeartbeat();
				}
				this.mobpatch.updateActiveTaczCombatRange(currentBehaviorSeries);
				if(mobpatch.interrupted){
					this.mobpatch.clearActiveTaczCombatRange();
					this.resetBehaviorSeries(currentBehaviorSeries, true);
					mobpatch.interrupted = false;
					return;
				}
				if (state.canBasicAttack() && !inaction) {
					CombatBehaviors.Behavior<HumanoidMobPatch<?>> result = preserveLoop
						? this.tryProceedCurrentLoopingSeries(currentBehaviorSeries)
						: this.combatBehaviors.tryProceed();

					if (result != null) {
						this.mobpatch.updateActiveTaczCombatRange(currentBehaviorSeries);
						mobpatch.setDamageSourceModifier(null);
						mobpatch.setBlocking(false);
						mobpatch.setAttackSpeed(1.0F);
						mobpatch.setHurtResistLevel(2);
						mobpatch.getEventManager().initAnimationEvent();
						result.execute(this.mobpatch);
					} else {
						this.mobpatch.clearActiveTaczCombatRange();
						if (this.mobpatch.hasActiveTaczCombatTarget()) {
							this.resetBehaviorSeries(currentBehaviorSeries, false);
						}
					}
				}
			} else {
				if (!state.inaction() && !inaction) {
					CombatBehaviors.Behavior<HumanoidMobPatch<?>> result = this.combatBehaviors.selectRandomBehaviorSeries();
					int selectedCount = ((CombatBehaviorsMixin<?>)combatBehaviors).getCurrentBehaviorPointer();
					CombatBehaviors.BehaviorSeries<?> selectedBehaviorSeries = selectedCount >= 0
							? ((CombatBehaviorsMixin<?>)combatBehaviors).getBehaviorSeriesList().get(selectedCount)
							: null;

					if (result != null) {
						this.mobpatch.updateActiveTaczCombatRange(selectedBehaviorSeries);
						mobpatch.setDamageSourceModifier(null);
						mobpatch.setBlocking(false);
						mobpatch.setAttackSpeed(1.0F);
						mobpatch.setHurtResistLevel(2);
						mobpatch.getEventManager().initAnimationEvent();
						result.execute(this.mobpatch);
					} else {
						this.mobpatch.clearActiveTaczCombatRange();
					}
				}
			}
		}
		else {
			this.mobpatch.clearActiveTaczCombatRange();
		}
	}

	private void resetBehaviorSeries(CombatBehaviors.BehaviorSeries<?> currentBehaviorSeries, boolean interrupted) {
		if (currentBehaviorSeries == null) {
			return;
		}

		((BehaviorSeriesMixin)currentBehaviorSeries).setLoopFinished(interrupted);
		((BehaviorSeriesMixin)currentBehaviorSeries).setNextBehaviorPointer(0);
	}

	private boolean shouldPreserveCurrentTaczLoop(CombatBehaviors.BehaviorSeries<?> currentBehaviorSeries) {
		return currentBehaviorSeries != null
			&& this.mobpatch.hasActiveTaczCombatTarget()
			&& ((BehaviorSeriesMixin) currentBehaviorSeries).getLooping();
	}

	@SuppressWarnings("unchecked")
	private CombatBehaviors.Behavior<HumanoidMobPatch<?>> tryProceedCurrentLoopingSeries(CombatBehaviors.BehaviorSeries<?> currentBehaviorSeries) {
		BehaviorSeriesMixin seriesAccessor = (BehaviorSeriesMixin) currentBehaviorSeries;
		List<CombatBehaviors.Behavior<?>> behaviors = seriesAccessor.getBehaviors();
		if (behaviors.isEmpty()) {
			((CombatBehaviorsMixin<?>) this.combatBehaviors).setCurrentBehaviorPointer(-1);
			return null;
		}

		int nextPointer = seriesAccessor.getNextBehaviorPointer();
		CombatBehaviors.Behavior<HumanoidMobPatch<?>> nextBehavior = (CombatBehaviors.Behavior<HumanoidMobPatch<?>>) behaviors.get(nextPointer);
		if (!this.checkBehaviorPredicates(nextBehavior)) {
			seriesAccessor.setNextBehaviorPointer(0);
			seriesAccessor.setLoopFinished(false);
			((CombatBehaviorsMixin<?>) this.combatBehaviors).setCurrentBehaviorPointer(-1);
			return null;
		}

		this.advanceBehaviorPointer(seriesAccessor, behaviors.size());
		return nextBehavior;
	}

	private boolean checkBehaviorPredicates(CombatBehaviors.Behavior<HumanoidMobPatch<?>> behavior) {
		for (Condition<HumanoidMobPatch<?>> condition : ((BehaviorMixin<HumanoidMobPatch<?>>) behavior).getConditions()) {
			if (!condition.predicate(this.mobpatch)) {
				return false;
			}
		}

		return true;
	}

	private void advanceBehaviorPointer(BehaviorSeriesMixin seriesAccessor, int behaviorCount) {
		int nextPointer = seriesAccessor.getNextBehaviorPointer() + 1;
		seriesAccessor.setLoopFinished(false);
		if (nextPointer >= behaviorCount) {
			nextPointer %= behaviorCount;
		}
		seriesAccessor.setNextBehaviorPointer(nextPointer);
	}
	
	private boolean isHoldingRangeWeapon(Mob mob) {
		return mob.isHolding((stack) -> {
			Item item = stack.getItem();
			return item instanceof ProjectileWeaponItem && mob.canFireProjectileWeapon((ProjectileWeaponItem) item);
		});
	}
	
	protected boolean isValidTarget(LivingEntity attackTarget) {
    	return attackTarget != null && attackTarget.isAlive() && !((attackTarget instanceof Player) && (attackTarget.isSpectator() || ((Player)attackTarget).isCreative()));
    }
}