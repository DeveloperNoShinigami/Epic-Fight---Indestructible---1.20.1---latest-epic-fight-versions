package com.nameless.indestructible.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nameless.indestructible.client.UIConfig;
import com.nameless.indestructible.main.Indestructible;
import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import yesman.epicfight.client.gui.EntityUI;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.config.ClientConfig.HealthBarVisibility;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.effect.VisibleMobEffect;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;


@OnlyIn(Dist.CLIENT)
public class StatusIndicator extends EntityUI {
    public static final ResourceLocation STATUS_BAR = new ResourceLocation(Indestructible.MOD_ID, "textures/gui/bar.png");

    @Override
    public boolean shouldDraw(LivingEntity entityIn, @Nullable LivingEntityPatch<?> entitypatch, LocalPlayerPatch playerpatch, float partialTicks) {

        HealthBarVisibility option = ClientConfig.healthBarVisibility;
        Minecraft mc = Minecraft.getInstance();
        if (!UIConfig.replaceUi() || option == HealthBarVisibility.NONE) {
            return false;
        } else if (entityIn.isInvisibleTo(playerpatch.getOriginal()) || entityIn == playerpatch.getOriginal().getVehicle()) {
            return false;
        } else if (entityIn.distanceToSqr(mc.getCameraEntity()) >= 400) {
            return false;
        } else if (entityIn instanceof Player playerIn) {
            if (playerIn == playerpatch.getOriginal() && playerpatch.getMaxStunShield() <= 0.0F) {
                return false;
            } else if (playerIn.isCreative() || playerIn.isSpectator()) {
                return false;
            }
        } else if (entitypatch instanceof AdvancedCustomHumanoidMobPatch<?> AHPatch && AHPatch.hasBossBar){
            return false;
        }

        if (option == HealthBarVisibility.TARGET) {
            return playerpatch.getTarget() == entityIn;
        }

        return (!entityIn.getActiveEffects().isEmpty() || entityIn.getHealth() < entityIn.getMaxHealth() || (entitypatch instanceof AdvancedCustomHumanoidMobPatch<?> AHPatch && AHPatch.getStamina() < AHPatch.getMaxStamina())) && !entityIn.isRemoved();
    }

    @Override
    public void draw(LivingEntity entityIn, @Nullable LivingEntityPatch<?> entitypatch, LocalPlayerPatch playerpatch, PoseStack matStackIn, MultiBufferSource bufferIn, float partialTicks) {
        boolean adjustUI = entitypatch instanceof AdvancedCustomHumanoidMobPatch<?> && UIConfig.replaceUi();
        float height = adjustUI ? 0.45F : 0.25F;

        Matrix4f mvMatrix = super.getModelViewMatrixAlignedToCamera(matStackIn, entityIn, 0.0F, entityIn.getBbHeight() + height, 0.0F, true, partialTicks);
        Collection<MobEffectInstance> activeEffects = entityIn.getActiveEffects();

        if (!activeEffects.isEmpty() && !entityIn.is(playerpatch.getOriginal())) {
            Iterator<MobEffectInstance> iter = activeEffects.iterator();
            int acives = activeEffects.size();
            int row = acives > 1 ? 1 : 0;
            int column = ((acives-1) / 2);
            float startX = -0.8F + -0.3F * row;
            float startY = -0.15F + 0.15F * column;

            for (int i = 0; i <= column; i++) {
                for (int j = 0; j <= row; j++) {
                    MobEffectInstance effectInstance = iter.next();
                    MobEffect effect = effectInstance.getEffect();
                    ResourceLocation rl;

                    if (effect instanceof VisibleMobEffect visibleMobEffect) {
                        rl = visibleMobEffect.getIcon(effectInstance);
                    } else {
                        rl = ResourceLocation.fromNamespaceAndPath(ForgeRegistries.MOB_EFFECTS.getKey(effect).getNamespace(), "textures/mob_effect/" + ForgeRegistries.MOB_EFFECTS.getKey(effect).getPath() + ".png");
                    }

                    float x = startX + 0.3F * j;
                    float y = startY + -0.3F * i;

                    drawUIAsLevelModel(mvMatrix, rl, bufferIn, x, y, x + 0.3F, y + 0.3F, 0, 0, 256, 256, 256);
                    if (!iter.hasNext()) {
                        break;
                    }
                }
            }
        }

        float ratio = Mth.clamp(entityIn.getHealth() / entityIn.getMaxHealth(), 0.0F, 1.0F);
        float healthRatio = -0.5F + ratio;
        int textureRatio = (int) (65 * ratio);
        drawUIAsLevelModel(mvMatrix, STATUS_BAR, bufferIn, -0.5F, -0.05F, healthRatio, 0.08F, 0, 26, textureRatio, 35, 256);
        drawUIAsLevelModel(mvMatrix, STATUS_BAR, bufferIn, healthRatio, -0.05F, 0.5F, 0.08F, textureRatio, 16, 65, 25, 256);

        if(entitypatch instanceof AdvancedCustomHumanoidMobPatch<?> achpatch){
            this.renderStamina(achpatch, mvMatrix, bufferIn);
        }
    }


    private void renderStamina(AdvancedCustomHumanoidMobPatch<?> achpatch, Matrix4f mvMatrix, MultiBufferSource bufferIn) {
        float ratio = Mth.clamp(achpatch.getStamina() / achpatch.getMaxStamina(), 0.0F, 1.0F);
        float barRatio = -0.5F + ratio;
        int textureRatio = (int) (63 * ratio);

        drawUIAsLevelModel(mvMatrix, STATUS_BAR, bufferIn, -0.5F, -0.15F, barRatio, -0.05F, 0, 8, textureRatio, 15, 256);
        drawUIAsLevelModel(mvMatrix, STATUS_BAR, bufferIn, barRatio, -0.15F, 0.5F, -0.05F, textureRatio, 0, 63, 7, 256);
    }
}
