package dev.rdh.argentum.impl.render.entity.instancing;

import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat3v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import dev.rdh.argentum.impl.render.terrain.fog.GLStateManagerFogService;

final class EntityShader {
    private static final float[] FOG_COLOR = new float[4];

    private final GlUniformInt texture;
    private final GlUniformInt lightmap;
    private final GlUniformInt textureArray;
    private final GlUniformInt textureArrayEnabled;
    private final GlUniformInt fogMode;
    private final GlUniformInt emissive;
    private final GlUniformInt glintPass;
    private final GlUniformInt chargePass;
    private final GlUniformInt itemGlintPass;
    private final GlUniformFloat itemGlintOffset;
    private final GlUniformFloat4v fogColor;
    private final GlUniformFloat3v fogParameters;

    EntityShader(ShaderBindingContext context, boolean textureArraysSupported) {
        this.texture = context.bindUniform("uTexture", GlUniformInt::new);
        this.lightmap = context.bindUniform("uLightmap", GlUniformInt::new);
        this.textureArray = textureArraysSupported
                ? context.bindUniform("uTextureArray", GlUniformInt::new)
                : null;
        this.textureArrayEnabled = textureArraysSupported
                ? context.bindUniform("uTextureArrayEnabled", GlUniformInt::new)
                : null;
        this.fogMode = context.bindUniform("uFogMode", GlUniformInt::new);
        this.emissive = context.bindUniform("uEmissive", GlUniformInt::new);
        this.glintPass = context.bindUniform("uGlintPass", GlUniformInt::new);
        this.chargePass = context.bindUniform("uChargePass", GlUniformInt::new);
        this.itemGlintPass = context.bindUniform("uItemGlintPass", GlUniformInt::new);
        this.itemGlintOffset = context.bindUniform("uItemGlintOffset", GlUniformFloat::new);
        this.fogColor = context.bindUniform("uFogColor", GlUniformFloat4v::new);
        this.fogParameters = context.bindUniform("uFogParameters", GlUniformFloat3v::new);
    }

    void initialize() {
        this.texture.setInt(0);
        this.lightmap.setInt(1);
        if (this.textureArray != null) {
            this.textureArray.setInt(2);
        }
        this.glintPass.setInt(-1);
        this.chargePass.setInt(0);
        this.itemGlintPass.setInt(-1);
    }

    void setUniforms() {
        this.fogMode.setInt(GLStateManagerFogService.fogEnabled ? GLStateManagerFogService.fogMode : 0);
        FOG_COLOR[0] = GLStateManagerFogService.fogColorRed;
        FOG_COLOR[1] = GLStateManagerFogService.fogColorGreen;
        FOG_COLOR[2] = GLStateManagerFogService.fogColorBlue;
        FOG_COLOR[3] = 1.0F;
        this.fogColor.set(FOG_COLOR);
        this.fogParameters.set(GLStateManagerFogService.fogStart, GLStateManagerFogService.fogEnd,
                GLStateManagerFogService.fogDensity);
    }

    void setTextureArray(boolean enabled) {
        if (this.textureArrayEnabled != null) {
            this.textureArrayEnabled.setInt(enabled ? 1 : 0);
        }
    }

    void setEmissive(boolean emissive) {
        this.emissive.setInt(emissive ? 1 : 0);
    }

    void setGlintPass(int pass) {
        this.glintPass.setInt(pass);
    }

    void setChargePass(int pass) {
        this.chargePass.setInt(pass);
    }

    void setItemGlintPass(int pass) {
        this.itemGlintPass.setInt(pass);
        if (pass >= 0) {
            long period = pass == 0 ? 3000L : 4873L;
            float direction = pass == 0 ? 1.0F : -1.0F;
            this.itemGlintOffset.setFloat(direction * (Minecraft.getTime() % period) / period / 8.0F);
        }
    }
}
