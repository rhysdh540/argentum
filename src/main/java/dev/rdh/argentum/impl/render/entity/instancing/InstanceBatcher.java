package dev.rdh.argentum.impl.render.entity.instancing;

import dev.rdh.argentum.impl.render.instancing.TextureArrayManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.model.Model;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.TextureAtlas;
import net.minecraft.resource.Identifier;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.joml.Matrix4f;
import org.joml.Vector4fc;
import org.lwjgl.opengl.GL11;
import dev.rdh.argentum.impl.debug.RenderMetrics;

import java.util.EnumMap;

final class InstanceBatcher {
    private final Reference2ReferenceOpenHashMap<Model, ModelGeometry> models = new Reference2ReferenceOpenHashMap<>();
    private final EnumMap<InstanceRenderPass, Object2ObjectLinkedOpenHashMap<Identifier, TextureBatch>> textures =
            new EnumMap<>(InstanceRenderPass.class);
    private final EnumMap<InstanceRenderPass, Reference2ReferenceOpenHashMap<TextureArrayManager.Pool, TextureBatch>> arrayTextures =
            new EnumMap<>(InstanceRenderPass.class);

    InstanceBatcher() {
        for (InstanceRenderPass pass : InstanceRenderPass.values()) {
            this.textures.put(pass, new Object2ObjectLinkedOpenHashMap<>());
            this.arrayTextures.put(pass, new Reference2ReferenceOpenHashMap<>());
        }
    }

    void clear() {
        this.textures.values().forEach(map -> map.values().forEach(TextureBatch::clear));
        this.arrayTextures.values().forEach(map -> map.values().forEach(TextureBatch::clear));
    }

    ModelGeometry model(Model model) {
        return this.models.computeIfAbsent(model, ignored -> new ModelGeometry());
    }

    void delete(CommandList commandList) {
        this.models.values().forEach(model -> model.delete(commandList));
        this.models.clear();
        this.textures.values().forEach(Object2ObjectLinkedOpenHashMap::clear);
        this.arrayTextures.values().forEach(Reference2ReferenceOpenHashMap::clear);
    }

    TextureBatch texture(Identifier texture, InstanceRenderPass pass) {
        return this.textures.get(pass).computeIfAbsent(texture, TextureBatch::new);
    }

    TextureBatch texture(TextureArrayManager.Pool pool, InstanceRenderPass pass) {
        return this.arrayTextures.get(pass).computeIfAbsent(pool, ignored -> new TextureBatch(null));
    }

    Stats render(CommandList commandList, GlProgram<InstanceShader> program) {
        int draws = 0;
        int textureCount = 0;
        Stats normal = this.renderPass(commandList, program, InstanceRenderPass.NORMAL);
        draws += normal.draws;
        textureCount += normal.textures;
        if (this.has(InstanceRenderPass.CULL_FRONT)) {
            GlStateManager.enableCull();
            GlStateManager.cullFace(GL11.GL_FRONT);
            Stats culled = this.renderPass(commandList, program, InstanceRenderPass.CULL_FRONT);
            draws += culled.draws;
            textureCount += culled.textures;
            GlStateManager.cullFace(GL11.GL_BACK);
            GlStateManager.disableCull();
        }
        if (this.has(InstanceRenderPass.CULL_BACK)) {
            GlStateManager.enableCull();
            GlStateManager.cullFace(GL11.GL_BACK);
            Stats culled = this.renderPass(commandList, program, InstanceRenderPass.CULL_BACK);
            draws += culled.draws;
            textureCount += culled.textures;
            GlStateManager.disableCull();
        }
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (this.has(InstanceRenderPass.ITEM)) {
            var textureManager = Minecraft.getInstance().getTextureManager();
            textureManager.bind(TextureAtlas.BLOCKS_LOCATION);
            var blockAtlas = textureManager.get(TextureAtlas.BLOCKS_LOCATION);
            blockAtlas.pushFilter(false, false);
            try {
                Stats items = this.renderPass(commandList, program, InstanceRenderPass.ITEM);
                draws += items.draws;
                textureCount += items.textures;
            } finally {
                textureManager.bind(TextureAtlas.BLOCKS_LOCATION);
                blockAtlas.popFilter();
            }
        }
        Stats translucent = this.renderPass(commandList, program, InstanceRenderPass.TRANSLUCENT);
        draws += translucent.draws;
        textureCount += translucent.textures;
        GlStateManager.blendFunc(1, 1);
        Stats emissive = this.renderPass(commandList, program, InstanceRenderPass.EMISSIVE);
        draws += emissive.draws;
        textureCount += emissive.textures;
        Stats charge = this.renderPass(commandList, program, InstanceRenderPass.CREEPER_CHARGE);
        draws += charge.draws;
        textureCount += charge.textures;
        charge = this.renderPass(commandList, program, InstanceRenderPass.WITHER_CHARGE);
        draws += charge.draws;
        textureCount += charge.textures;
        GlStateManager.disableBlend();
        GlStateManager.blendFunc(770, 771);
        if (this.has(InstanceRenderPass.GLINT)
                || this.has(InstanceRenderPass.ITEM_GLINT_0)
                || this.has(InstanceRenderPass.ITEM_GLINT_1)) {
            GlStateManager.enableBlend();
            GlStateManager.depthFunc(GL11.GL_EQUAL);
            GlStateManager.depthMask(false);
            GlStateManager.blendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
            program.getInterface().setGlintPass(0);
            Stats glint = this.renderPass(commandList, program, InstanceRenderPass.GLINT);
            draws += glint.draws;
            textureCount += glint.textures;
            program.getInterface().setGlintPass(1);
            glint = this.renderPass(commandList, program, InstanceRenderPass.GLINT);
            draws += glint.draws;
            textureCount += glint.textures;
            program.getInterface().setGlintPass(-1);
            program.getInterface().setItemGlintPass(0);
            glint = this.renderPass(commandList, program, InstanceRenderPass.ITEM_GLINT_0);
            draws += glint.draws;
            textureCount += glint.textures;
            program.getInterface().setItemGlintPass(1);
            glint = this.renderPass(commandList, program, InstanceRenderPass.ITEM_GLINT_1);
            draws += glint.draws;
            textureCount += glint.textures;
            program.getInterface().setItemGlintPass(-1);
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.disableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        return new Stats(draws, textureCount);
    }

    private boolean has(InstanceRenderPass pass) {
        return this.textures.get(pass).values().stream().anyMatch(batch -> batch.count != 0)
                || this.arrayTextures.get(pass).values().stream().anyMatch(batch -> batch.count != 0);
    }

    private Stats renderPass(CommandList commandList, GlProgram<InstanceShader> program, InstanceRenderPass pass) {
        int draws = 0;
        int textureCount = 0;
        program.getInterface().setEmissive(pass != InstanceRenderPass.NORMAL
                && pass != InstanceRenderPass.CULL_FRONT
                && pass != InstanceRenderPass.CULL_BACK
                && pass != InstanceRenderPass.ITEM
                && pass != InstanceRenderPass.TRANSLUCENT
        );
        program.getInterface().setChargePass(pass.chargePass);
        for (TextureBatch texture : this.textures.get(pass).values()) {
            if (texture.count == 0) continue;
            Minecraft.getInstance().getTextureManager().bind(texture.texture);
            program.getInterface().setTextureArray(false);
            draws += texture.render(commandList, pass == InstanceRenderPass.TRANSLUCENT);
            textureCount++;
        }
        for (var entry : this.arrayTextures.get(pass).reference2ReferenceEntrySet()) {
            if (entry.getValue().count == 0) continue;
            int previous = entry.getKey().bind();
            program.getInterface().setTextureArray(true);
            draws += entry.getValue().render(commandList, pass == InstanceRenderPass.TRANSLUCENT);
            entry.getKey().restore(previous);
            textureCount++;
        }
        return new Stats(draws, textureCount);
    }

    record Stats(int draws, int textures) {
    }

    static final class TextureBatch {
        private final Identifier texture;
        private final Reference2ObjectLinkedOpenHashMap<InstanceGeometry, Instances> parts = new Reference2ObjectLinkedOpenHashMap<>();
        private int count;

        private TextureBatch(Identifier texture) {
            this.texture = texture;
        }

        private void clear() {
            this.parts.values().forEach(Instances::clear);
            this.count = 0;
        }

        void add(InstanceGeometry geometry, Matrix4f matrix, float u, float v, int layer,
                Vector4fc color, float effectTime, Vector4fc overlayColor) {
            geometry.instances(this).add(matrix, u, v, layer, color, effectTime, overlayColor);
            this.count++;
        }

        Instances instances(InstanceGeometry geometry) {
            Instances instances = this.parts.get(geometry);
            if (instances == null) {
                instances = new Instances();
                this.parts.put(geometry, instances);
            }
            return instances;
        }

        private int render(CommandList commandList, boolean sort) {
            int draws = 0;
            for (var entry : this.parts.reference2ObjectEntrySet()) {
                if (entry.getValue().count() != 0) {
                    if (sort) {
                        entry.getValue().sortBackToFront();
                    }
                    entry.getKey().render(commandList, entry.getValue());
                    RenderMetrics.recordDraw();
                    draws++;
                }
            }
            return draws;
        }
    }
}
