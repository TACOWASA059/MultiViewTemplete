package com.github.tacowasa059.multiviewtemplete.mixin;

import com.github.tacowasa059.multiviewtemplete.MultiViewTemplete;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(GameRenderer.class)
abstract public class GameRendererMixin {
    @Shadow
    LightTexture lightTexture;
    @Shadow

    Minecraft minecraft;
    @Shadow
    abstract public void pick(float p_109088_);

    @Shadow
    abstract boolean shouldRenderBlockOutline();
    @Shadow
    Camera mainCamera;
    @Shadow
    float renderDistance;
    @Shadow
    boolean renderHand;
    @Shadow
    abstract double getFov(Camera p_109142_, float p_109143_, boolean p_109144_);

    @Shadow
    abstract void bobHurt(PoseStack p_109118_, float p_109119_);
    @Shadow
    abstract void bobView(PoseStack p_109139_, float p_109140_);
    @Shadow
    int tick;

    @Shadow
    abstract void renderItemInHand(PoseStack p_109121_, Camera p_109122_, float p_109123_);

    private MainTarget frontFramebuffer;
    private MainTarget backFramebuffer;

    @Inject(method = "renderLevel",at = @At("HEAD"),cancellable = true)
    public void renderLevel(float p_109090_, long p_109091_, PoseStack p_109092_, CallbackInfo ci) {
        reset();
        renderToFramebuffer(frontFramebuffer, p_109090_, p_109091_, p_109092_,false);
        renderToFramebuffer(backFramebuffer, p_109090_, p_109091_, p_109092_,true);
        ci.cancel();
    }
    private void renderLevelPart(float p_109090_, long p_109091_, PoseStack p_109092_, boolean isBack) {
        GameRenderer gameRenderer = (GameRenderer)(Object)this;
        this.lightTexture.updateLightTexture(p_109090_);
        if (this.minecraft.getCameraEntity() == null) {
            this.minecraft.setCameraEntity(this.minecraft.player);
        }

        this.pick(p_109090_);
        this.minecraft.getProfiler().push("center");
        boolean flag = this.shouldRenderBlockOutline();
        this.minecraft.getProfiler().popPush("camera");
        Camera camera = this.mainCamera;

        this.renderDistance = (float)(this.minecraft.options.getEffectiveRenderDistance() * 16);
        PoseStack posestack = new PoseStack();
        double d0 = this.getFov(camera, p_109090_, true);
        posestack.mulPoseMatrix(gameRenderer.getProjectionMatrix(d0));
        this.bobHurt(posestack, p_109090_);
        if (this.minecraft.options.bobView().get()) {
            this.bobView(posestack, p_109090_);
        }

        float f = this.minecraft.options.screenEffectScale().get().floatValue();
        float f1 = Mth.lerp(p_109090_, this.minecraft.player.oSpinningEffectIntensity, this.minecraft.player.spinningEffectIntensity) * f * f;
        if (f1 > 0.0F) {
            int i = this.minecraft.player.hasEffect(MobEffects.CONFUSION) ? 7 : 20;
            float f2 = 5.0F / (f1 * f1 + 5.0F) - f1 * 0.04F;
            f2 *= f2;
            Axis axis = Axis.of(new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F));
            posestack.mulPose(axis.rotationDegrees(((float)this.tick + p_109090_) * (float)i));
            posestack.scale(1.0F / f2, 1.0F, 1.0F);
            float f3 = -((float)this.tick + p_109090_) * (float)i;
            posestack.mulPose(axis.rotationDegrees(f3));
        }

        Matrix4f matrix4f = posestack.last().pose();
        gameRenderer.resetProjectionMatrix(matrix4f);

        // modified
        Entity entity = this.minecraft.getCameraEntity() == null ? this.minecraft.player : this.minecraft.getCameraEntity();
        if(isBack) entity.setYRot(entity.getYRot() -180F);
        camera.setup(this.minecraft.level, entity, !this.minecraft.options.getCameraType().isFirstPerson(), this.minecraft.options.getCameraType().isMirrored(), p_109090_);
        if(isBack) entity.setYRot(entity.getYRot() +180F);
        //
        net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles cameraSetup = net.minecraftforge.client.ForgeHooksClient.onCameraSetup(gameRenderer, camera, p_109090_);


        p_109092_.mulPose(Axis.ZP.rotationDegrees(cameraSetup.getRoll()));
        p_109092_.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        p_109092_.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));

        Matrix3f matrix3f = (new Matrix3f(p_109092_.last().normal())).invert();
        RenderSystem.setInverseViewRotationMatrix(matrix3f);

        this.minecraft.levelRenderer.prepareCullFrustum(p_109092_, camera.getPosition(), gameRenderer.getProjectionMatrix(Math.max(d0, (double)this.minecraft.options.fov().get().intValue())));

        this.minecraft.levelRenderer.renderLevel(p_109092_, p_109090_, p_109091_, flag, camera, gameRenderer, this.lightTexture, matrix4f);

        this.minecraft.getProfiler().popPush("forge_render_last");
        net.minecraftforge.client.ForgeHooksClient.dispatchRenderStage(net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL, this.minecraft.levelRenderer, posestack, matrix4f, this.minecraft.levelRenderer.getTicks(), camera, this.minecraft.levelRenderer.getFrustum());
        this.minecraft.getProfiler().popPush("hand");
        if (this.renderHand && !isBack) {
            RenderSystem.clear(256, Minecraft.ON_OSX);
            this.renderItemInHand(p_109092_, camera, p_109090_);
        }

        this.minecraft.getProfiler().pop();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        this.frontFramebuffer = new MainTarget(width, (int)(height/2f));
        this.backFramebuffer = new MainTarget(width, (int)(height/2f));
    }

    private void reset(){
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();

        this.frontFramebuffer.resize(width, (int)(height/2f),true);
        this.frontFramebuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        this.frontFramebuffer.clear(Minecraft.ON_OSX);

        this.backFramebuffer.resize(width, (int)(height/2f),true);
        this.backFramebuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        this.backFramebuffer.clear(Minecraft.ON_OSX);
    }

    private void renderToFramebuffer(MainTarget framebuffer, float partialTicks, long finishTimeNano, PoseStack poseStack, boolean isBack) {
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();


        framebuffer.clear(Minecraft.ON_OSX);
        framebuffer.bindWrite(true);


        MultiViewTemplete.target = framebuffer;

        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.backupProjectionMatrix();


        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        renderLevelPart(partialTicks, finishTimeNano, poseStack, isBack);


        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);

        if(!isBack) {
            blitToScreen(framebuffer, 0, height/2, width, height/2, true);
        }{
            blitToScreen(framebuffer, 0, 0, width, height/2, true);
        }

        framebuffer.unbindRead();
    }

    public void blitToScreen(MainTarget framebuffer, int x, int y, int width, int height, boolean flag){
        RenderSystem.assertOnGameThreadOrInit();
        if (!RenderSystem.isInInitPhase()) {
            RenderSystem.recordRenderCall(() -> {
                _blitToScreen(framebuffer, x, y, width, height, flag);
            });
        } else {
            _blitToScreen(framebuffer, x, y, width, height, flag);
        }
    }
    public void _blitToScreen(MainTarget framebuffer, int x, int y, int width, int height, boolean flag){
        RenderSystem.assertOnRenderThread();
        GlStateManager._colorMask(true, true, true, false);

        GlStateManager._viewport(x, y, width, height);
        if (flag) {
            GlStateManager._disableBlend();
        }

        Minecraft minecraft = Minecraft.getInstance();
        ShaderInstance shaderinstance = minecraft.gameRenderer.blitShader;
        shaderinstance.setSampler("DiffuseSampler", framebuffer.getColorTextureId());
        Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)width, (float)height, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
        if (shaderinstance.MODEL_VIEW_MATRIX != null) {
            shaderinstance.MODEL_VIEW_MATRIX.set((new Matrix4f()).translation(0.0F, 0.0F, -2000.0F));
        }

        if (shaderinstance.PROJECTION_MATRIX != null) {
            shaderinstance.PROJECTION_MATRIX.set(matrix4f);
        }

        shaderinstance.apply();
        float f = (float)width;
        float f1 = (float)height;
        float f2 = (float)framebuffer.viewWidth / (float)framebuffer.width;
        float f3 = (float)framebuffer.viewHeight / (float)framebuffer.height;
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.vertex(0, (double)f1, 0.0D).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferbuilder.vertex((double)f, (double)f1, 0.0D).uv(f2, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferbuilder.vertex((double)f, 0, 0.0D).uv(f2, f3).color(255, 255, 255, 255).endVertex();
        bufferbuilder.vertex(0,0, 0.0D).uv(0.0F, f3).color(255, 255, 255, 255).endVertex();
        BufferUploader.draw(bufferbuilder.end());
        shaderinstance.clear();
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
    }
}
