package io.github.fusionflux.portalcubed.mixin.client;

import com.mojang.blaze3d.platform.GlStateManager;

import io.github.fusionflux.portalcubed.content.portal.PortalRenderer;

import io.github.fusionflux.portalcubed.framework.util.RenderingUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlStateManager.class, remap = false)
public class GlStateManagerMixin {
	@Inject(method = "_clearColor", at = @At("HEAD"), cancellable = true)
	private static void captureClearColor(float red, float green, float blue, float alpha, CallbackInfo ci) {
		if (PortalRenderer.isRenderingView()) {
			// alpha is weird for some reason, looks like OpenGL doesn't respect it
			RenderingUtils.CLEAR_COLOR.set(red, green, blue);
			ci.cancel();
		}
	}
}
