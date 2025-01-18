package com.github.tacowasa059.multiviewtemplete.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 照準位置の変更
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    int scale = 2;
    @Shadow
    private static ResourceLocation GUI_ICONS_LOCATION;
    @Shadow
    private Minecraft minecraft;
    @Shadow abstract protected  boolean canRenderCrosshairForSpectator(HitResult p_93025_);

    @Shadow protected int screenWidth;
    @Shadow protected int screenHeight;
    @Redirect(method = "renderCrosshair",at=@At(value = "INVOKE",target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    public void translate(PoseStack instance, float p_254202_, float p_253782_, float p_254238_){
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        instance.translate((float)(screenWidth / 2), (float)(screenHeight / 4), 0.0F);
    }
    @Inject(method ="renderCrosshair",at=@At("HEAD"),cancellable = true)
    public void rendercrosshaier(GuiGraphics p_282828_, CallbackInfo ci){
        Options options = this.minecraft.options;
        if (options.getCameraType().isFirstPerson()) {
            if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR || this.canRenderCrosshairForSpectator(this.minecraft.hitResult)) {
                if (options.renderDebug && !options.hideGui && !this.minecraft.player.isReducedDebugInfo() && !options.reducedDebugInfo().get()) {
                    Camera camera = this.minecraft.gameRenderer.getMainCamera();
                    PoseStack posestack = RenderSystem.getModelViewStack();
                    posestack.pushPose();
                    posestack.mulPoseMatrix(p_282828_.pose().last().pose());
                    posestack.translate((float)(this.screenWidth / 2), (float)(this.screenHeight / (2*scale)), 0.0F);
                    posestack.mulPose(Axis.XN.rotationDegrees(camera.getXRot()));
                    posestack.mulPose(Axis.YP.rotationDegrees(camera.getYRot()));
                    posestack.scale(-1.0F, -1.0F, -1.0F);
                    RenderSystem.applyModelViewMatrix();
                    RenderSystem.renderCrosshair(10);
                    posestack.popPose();
                    RenderSystem.applyModelViewMatrix();
                } else {
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    int i = 15;
                    p_282828_.blit(GUI_ICONS_LOCATION, (this.screenWidth - 15) / 2, (this.screenHeight - 15) / (2*scale), 0, 0, 15, 15);
                    if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
                        float f = this.minecraft.player.getAttackStrengthScale(0.0F);
                        boolean flag = false;
                        if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && f >= 1.0F) {
                            flag = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F;
                            flag &= this.minecraft.crosshairPickEntity.isAlive();
                        }

                        int j = this.screenHeight / 2 - 7 + 16;
                        int k = this.screenWidth / 2 - 8;
                        if (flag) {
                            p_282828_.blit(GUI_ICONS_LOCATION, k, j/scale, 68, 94, 16, 16);
                        } else if (f < 1.0F) {
                            int l = (int)(f * 17.0F);
                            p_282828_.blit(GUI_ICONS_LOCATION, k, j/scale, 36, 94, 16, 4);
                            p_282828_.blit(GUI_ICONS_LOCATION, k, j/scale, 52, 94, l, 4);
                        }
                    }

                    RenderSystem.defaultBlendFunc();
                }

            }
        }
        ci.cancel();
    }
}
