package com.github.tacowasa059.multiviewtemplete.mixin;

import com.github.tacowasa059.multiviewtemplete.MultiViewTemplete;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * フレームバッファの固定
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Redirect(method = "renderLevel", at=@At(value = "INVOKE",target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V"))
    public void onBind(RenderTarget instance, boolean p_83948_){
        MultiViewTemplete.target.bindWrite(true);
    }
}
