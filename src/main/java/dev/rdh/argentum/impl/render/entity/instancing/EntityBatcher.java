package dev.rdh.argentum.impl.render.entity.instancing;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.model.Model;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.TextureAtlas;
import net.minecraft.resource.Identifier;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import dev.rdh.argentum.impl.debug.RenderMetrics;

import java.util.IdentityHashMap;
import java.util.EnumMap;
import java.util.Map;

final class EntityBatcher {
    private final Map<Model, ModelBatch> models = new IdentityHashMap<>();
    private final EnumMap<EntityRenderPass, Object2ObjectLinkedOpenHashMap<Identifier, TextureBatch>> textures =
            new EnumMap<>(EntityRenderPass.class);
    private final EnumMap<EntityRenderPass, Map<TextureArrayManager.Pool, TextureBatch>> arrayTextures =
            new EnumMap<>(EntityRenderPass.class);

    EntityBatcher() {
        for (EntityRenderPass pass : EntityRenderPass.values()) {
            this.textures.put(pass, new Object2ObjectLinkedOpenHashMap<>());
            this.arrayTextures.put(pass, new IdentityHashMap<>());
        }
    }

    void clear() {
        this.textures.values().forEach(map -> map.values().forEach(TextureBatch::clear));
        this.arrayTextures.values().forEach(map -> map.values().forEach(TextureBatch::clear));
    }

    ModelBatch model(Model model) {
        return this.models.computeIfAbsent(model, ignored -> new ModelBatch());
    }

    void delete(CommandList commandList) {
        this.models.values().forEach(model -> model.delete(commandList));
        this.models.clear();
        this.textures.values().forEach(Map::clear);
        this.arrayTextures.values().forEach(Map::clear);
    }

    TextureBatch texture(Identifier texture, EntityRenderPass pass) {
        return this.textures.get(pass).computeIfAbsent(texture, TextureBatch::new);
    }

    TextureBatch texture(TextureArrayManager.Pool pool, EntityRenderPass pass) {
        return this.arrayTextures.get(pass).computeIfAbsent(pool, ignored -> new TextureBatch(null));
    }

    Stats render(CommandList commandList, GlProgram<EntityShader> program) {
        int draws = 0;
        int textureCount = 0;
        Stats normal = this.renderPass(commandList, program, EntityRenderPass.NORMAL);
        draws += normal.draws;
        textureCount += normal.textures;
        if (this.has(EntityRenderPass.CULL_FRONT)) {
            GlStateManager.enableCull();
            GlStateManager.cullFace(GL11.GL_FRONT);
            Stats culled = this.renderPass(commandList, program, EntityRenderPass.CULL_FRONT);
            draws += culled.draws;
            textureCount += culled.textures;
            GlStateManager.cullFace(GL11.GL_BACK);
            GlStateManager.disableCull();
        }
        if (this.has(EntityRenderPass.CULL_BACK)) {
            GlStateManager.enableCull();
            GlStateManager.cullFace(GL11.GL_BACK);
            Stats culled = this.renderPass(commandList, program, EntityRenderPass.CULL_BACK);
            draws += culled.draws;
            textureCount += culled.textures;
            GlStateManager.disableCull();
        }
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (this.has(EntityRenderPass.ITEM)) {
            var textureManager = Minecraft.getInstance().getTextureManager();
            textureManager.bind(TextureAtlas.BLOCKS_LOCATION);
            var blockAtlas = textureManager.get(TextureAtlas.BLOCKS_LOCATION);
            blockAtlas.pushFilter(false, false);
            try {
                Stats items = this.renderPass(commandList, program, EntityRenderPass.ITEM);
                draws += items.draws;
                textureCount += items.textures;
            } finally {
                textureManager.bind(TextureAtlas.BLOCKS_LOCATION);
                blockAtlas.popFilter();
            }
        }
        Stats translucent = this.renderPass(commandList, program, EntityRenderPass.TRANSLUCENT);
        draws += translucent.draws;
        textureCount += translucent.textures;
        GlStateManager.blendFunc(1, 1);
        Stats emissive = this.renderPass(commandList, program, EntityRenderPass.EMISSIVE);
        draws += emissive.draws;
        textureCount += emissive.textures;
        Stats charge = this.renderPass(commandList, program, EntityRenderPass.CREEPER_CHARGE);
        draws += charge.draws;
        textureCount += charge.textures;
        charge = this.renderPass(commandList, program, EntityRenderPass.WITHER_CHARGE);
        draws += charge.draws;
        textureCount += charge.textures;
        GlStateManager.disableBlend();
        GlStateManager.blendFunc(770, 771);
        if (this.has(EntityRenderPass.GLINT)
                || this.has(EntityRenderPass.ITEM_GLINT_0)
                || this.has(EntityRenderPass.ITEM_GLINT_1)) {
            GlStateManager.enableBlend();
            GlStateManager.depthFunc(GL11.GL_EQUAL);
            GlStateManager.depthMask(false);
            GlStateManager.blendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
            program.getInterface().setGlintPass(0);
            Stats glint = this.renderPass(commandList, program, EntityRenderPass.GLINT);
            draws += glint.draws;
            textureCount += glint.textures;
            program.getInterface().setGlintPass(1);
            glint = this.renderPass(commandList, program, EntityRenderPass.GLINT);
            draws += glint.draws;
            textureCount += glint.textures;
            program.getInterface().setGlintPass(-1);
            program.getInterface().setItemGlintPass(0);
            glint = this.renderPass(commandList, program, EntityRenderPass.ITEM_GLINT_0);
            draws += glint.draws;
            textureCount += glint.textures;
            program.getInterface().setItemGlintPass(1);
            glint = this.renderPass(commandList, program, EntityRenderPass.ITEM_GLINT_1);
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

    private boolean has(EntityRenderPass pass) {
        return this.textures.get(pass).values().stream().anyMatch(batch -> batch.count != 0)
                || this.arrayTextures.get(pass).values().stream().anyMatch(batch -> batch.count != 0);
    }

    private Stats renderPass(CommandList commandList, GlProgram<EntityShader> program, EntityRenderPass pass) {
        int draws = 0;
        int textureCount = 0;
        program.getInterface().setEmissive(pass != EntityRenderPass.NORMAL
                && pass != EntityRenderPass.CULL_FRONT
                && pass != EntityRenderPass.CULL_BACK
                && pass != EntityRenderPass.ITEM
                && pass != EntityRenderPass.TRANSLUCENT);
        program.getInterface().setChargePass(pass.chargePass);
        for (TextureBatch texture : this.textures.get(pass).values()) {
            if (texture.count == 0) continue;
            Minecraft.getInstance().getTextureManager().bind(texture.texture);
            program.getInterface().setTextureArray(false);
            draws += texture.render(commandList, pass == EntityRenderPass.TRANSLUCENT);
            textureCount++;
        }
        for (Map.Entry<TextureArrayManager.Pool, TextureBatch> entry : this.arrayTextures.get(pass).entrySet()) {
            if (entry.getValue().count == 0) continue;
            int previous = entry.getKey().bind();
            program.getInterface().setTextureArray(true);
            draws += entry.getValue().render(commandList, pass == EntityRenderPass.TRANSLUCENT);
            entry.getKey().restore(previous);
            textureCount++;
        }
        return new Stats(draws, textureCount);
    }

    record Stats(int draws, int textures) {
    }

    static final class TextureBatch {
        private final Identifier texture;
        private final Reference2ObjectLinkedOpenHashMap<EntityGeometry, Instances> parts = new Reference2ObjectLinkedOpenHashMap<>();
        private int count;

        private TextureBatch(Identifier texture) {
            this.texture = texture;
        }

        private void clear() {
            this.parts.values().forEach(Instances::clear);
            this.count = 0;
        }

        void add(EntityGeometry geometry, Matrix4f matrix, float u, float v, int layer,
                float red, float green, float blue, float alpha, float effectTime,
                float overlayRed, float overlayGreen, float overlayBlue, float overlayAlpha) {
            this.parts.computeIfAbsent(geometry, ignored -> new Instances())
                    .add(matrix, u, v, layer, red, green, blue, alpha, effectTime,
                            overlayRed, overlayGreen, overlayBlue, overlayAlpha);
            this.count++;
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

enum EntityRenderPass {
    NORMAL(0),
    CULL_FRONT(0),
    CULL_BACK(0),
    ITEM(0),
    TRANSLUCENT(0),
    EMISSIVE(0),
    GLINT(0),
    ITEM_GLINT_0(0),
    ITEM_GLINT_1(0),
    CREEPER_CHARGE(1),
    WITHER_CHARGE(2);

    final int chargePass;

    EntityRenderPass(int chargePass) {
        this.chargePass = chargePass;
    }
}
