package com.nameless.indestructible.world.ai.goal;

import com.nameless.indestructible.mixin.BehaviorMixin;
import com.nameless.indestructible.mixin.BehaviorSeriesMixin;
import com.nameless.indestructible.mixin.CombatBehaviorsMixin;
import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

import java.util.List;

public class AdvancedCombatGoal<T extends AdvancedCustomHumanoidMobPatch<?>> extends Goal {
	protected final AdvancedCustomHumanoidMobPatch<?> mobpatch;
	protected final CombatBehaviors<HumanoidMobPatch<?>> combatBehaviors;
	public AdvancedCombatGoal(AdvancedCustomHumanoidMobPatch<?> mobpatch, CombatBehaviors<HumanoidMobPatch<?>> combatBehaviors) {
		this.mobpatch = mobpatch;
		this.combatBehaviors = combatBehaviors;
	}
	@Override
	public boolean canUse() {
		LivingEntity livingentity = this.mobpatch.getTarget();
		if (livingentity == null) {
			return false;
		} else if (!livingentity.isAlive()) {
			return false;
		} else {
			return !(livingentity instanceof Player) || !livingentity.isSpectator() && !((Player)livingentity).isCreative();
		}
	}
	
	@Override
	public void tick() {

		boolean inaction =  (mobpatch.isBlocking() || mobpatch.getInactionTime() >0);
		if (this.mobpatch.getTarget() != null) {
			EntityState state = this.mobpatch.getEntityState();
			this.combatBehaviors.tick();
			if (this.combatBehaviors.hasActivatedMove()) {
				int count =  ((CombatBehaviorsMixin<?>)combatBehaviors).getCurrentBehaviorPointer();
				CombatBehaviors.BehaviorSeries<?> currentBehaviorSeries = count >= 0
						? ((CombatBehaviorsMixin<?>)combatBehaviors).getBehaviorSeriesList().get(count)
						: null;
				this.mobpatch.updateActiveTaczCombatRange(currentBehaviorSeries);
				if(mobpatch.interrupted){
					this.mobpatch.clearActiveTaczCombatRange();
					this.resetBehaviorSeries(currentBehaviorSeries, true);
					mobpatch.interrupted = false;
					return;
				}

				if (state.canBasicAttack() && !inaction) {
					CombatBehaviors.Behavior<HumanoidMobPatch<?>> result = this.shouldPreserveCurrentTaczLoop(currentBehaviorSeries)
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
}