package com.nameless.indestructible.api.animation.types;

import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;

public class CustomGuardAnimation extends StaticAnimation {

	public final String successAnimation;
	public final Boolean isShield;

	public CustomGuardAnimation(String path, String successanimation, AssetAccessor<? extends Armature> armature, boolean isShield) {
		super(0.05F,true, path, armature);
		this.successAnimation = successanimation;
		this.isShield = isShield;
	}
	public CustomGuardAnimation(AnimationAccessor<? extends StaticAnimation> accessor, String successanimation, AssetAccessor<? extends Armature> armature, boolean isShield) {
		super(0.05F, true, accessor, armature);
		this.successAnimation = successanimation;
		this.isShield = isShield;
	}
	public CustomGuardAnimation(String path, String successanimation, AssetAccessor<? extends Armature> armature){
		this(path,successanimation,armature,false);
	}
	public CustomGuardAnimation(AnimationAccessor<? extends StaticAnimation> accessor, String successanimation, AssetAccessor<? extends Armature> armature){
		this(accessor, successanimation, armature, false);
	}
}