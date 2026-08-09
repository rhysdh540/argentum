package dev.rdh.argentum.impl.render.entity.instancing;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.layer.AbstractArmorLayer;
import net.minecraft.client.render.entity.layer.EntityRenderLayer;
import net.minecraft.client.render.model.Model;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.client.resource.model.BakedModel;
import net.minecraft.client.render.platform.GLX;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.Texture;
import net.minecraft.client.render.texture.TextureAtlas;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.resource.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.embeddedt.embeddium.impl.render.shader.ShaderLoader;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import dev.rdh.argentum.impl.debug.RenderMetrics;
import dev.rdh.argentum.impl.Argentum;
import java.util.List;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class EntityInstancingRenderer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Identifier ENCHANTMENT_GLINT_TEXTURE =
            new Identifier("textures/misc/enchanted_item_glint.png");
    private static final int MATRIX_STACK_SIZE = 64;
    private static final Matrix4f[] MATRICES = new Matrix4f[MATRIX_STACK_SIZE];
    private static final Matrix4f ARROW_MATRIX = new Matrix4f();
    private static final EntityBatcher BATCHER = new EntityBatcher();
    private static final BakedItemGeometryCache ITEM_GEOMETRY = new BakedItemGeometryCache();
    private static final TextureArrayManager TEXTURE_ARRAYS = new TextureArrayManager();
    private static final DeferredNameTags NAME_TAGS = new DeferredNameTags();
    private static final Set<ModelPart> GLINT_PARTS = Collections.newSetFromMap(new IdentityHashMap<>());

    private static boolean initialized;
    private static boolean supported;
    private static boolean textureArraysSupported;
    private static boolean frameActive;
    private static Capture activeCapture;
    private static boolean modelActive;
    private static boolean entityRecorded;
    private static boolean currentPlayer;
    private static boolean suppressFixedFunction;
    private static boolean selectingTexture;
    private static boolean armorLayer;
    private static int matrixDepth;
    private static int matrixMode = GL11.GL_MODELVIEW;
    private static int frame;
    private static int selectedFrame = -1;
    private static int currentTextureLayer;
    private static int selectedTextureLayer;
    private static int itemGlintPass;
    private static Identifier currentEntityTexture;
    private static Identifier currentBoundTexture;
    private static Identifier selectedTexture;
    private static GlProgram<EntityShader> program;
    private static ModelBatch currentModel;
    private static EntityBatcher.TextureBatch currentTexture;
    private static EntityBatcher.TextureBatch selectedTextureBatch;
    private static EntityRenderPass currentPass = EntityRenderPass.NORMAL;
    private static EntityRenderPass selectedTexturePass;
    private static int entityCount;
    private static int instanceCount;
    private static int drawCount;
    private static int textureCount;
    private static int playerCount;
    private static int arrayPlayerCount;
    private static int fallbackPlayerCount;
    private static float lightU;
    private static float lightV;
    private static float red = 1.0F;
    private static float green = 1.0F;
    private static float blue = 1.0F;
    private static float alpha = 1.0F;
    private static float effectTime;
    private static float overlayRed;
    private static float overlayGreen;
    private static float overlayBlue;
    private static float overlayAlpha;
    private static float currentOverlayAlpha;
    private static ArrowGeometry arrowGeometry;
    private static String debugString = "Entity instancing: waiting";

    static {
        for (int i = 0; i < MATRICES.length; i++) {
            MATRICES[i] = new Matrix4f();
        }
    }

    private EntityInstancingRenderer() {
    }

    public static void beginFrame() {
        if (!Argentum.CONFIG.entityInstancing) {
            debugString = "Entity instancing: disabled by config";
            return;
        }
        if (!initialize()) {
            debugString = "Entity instancing: unsupported";
            return;
        }

        BATCHER.clear();
        entityCount = 0;
        instanceCount = 0;
        drawCount = 0;
        textureCount = 0;
        playerCount = 0;
        arrayPlayerCount = 0;
        fallbackPlayerCount = 0;
        NAME_TAGS.clear();
        frame++;
        frameActive = true;
    }

    public static Capture beginEntity(Model model, Identifier texture, boolean player, boolean preserveFixedFunction,
            float effectTime, float overlayRed, float overlayGreen, float overlayBlue, float overlayAlpha) {
        if (!frameActive || activeCapture != null || model == null || texture == null) {
            return null;
        }

        currentModel = BATCHER.model(model);
        currentEntityTexture = texture;
        currentPass = EntityRenderPass.NORMAL;
        selectTexture(texture);
        if (player && currentTextureLayer >= 0) {
            arrayPlayerCount++;
        } else if (player) {
            fallbackPlayerCount++;
        }

        MATRICES[0].identity();
        matrixDepth = 0;
        matrixMode = GL11.GL_MODELVIEW;
        entityRecorded = false;
        currentPlayer = player;
        red = green = blue = alpha = 1.0F;
        EntityInstancingRenderer.effectTime = effectTime;
        EntityInstancingRenderer.overlayRed = overlayRed;
        EntityInstancingRenderer.overlayGreen = overlayGreen;
        EntityInstancingRenderer.overlayBlue = overlayBlue;
        EntityInstancingRenderer.overlayAlpha = overlayAlpha;
        currentOverlayAlpha = overlayAlpha;
        suppressFixedFunction = !preserveFixedFunction;
        return activeCapture = new Capture(false);
    }

    public static Capture beginItemEntity(ItemEntity entity, BakedModel model) {
        ItemStack item = entity.getItem();
        if (!frameActive || activeCapture != null || item == null || !ITEM_GEOMETRY.supportsItem(model, item)) {
            return null;
        }
        currentPass = EntityRenderPass.ITEM;
        currentModel = null;
        currentEntityTexture = null;
        selectTexture(TextureAtlas.BLOCKS_LOCATION);
        MATRICES[0].identity();
        matrixDepth = 0;
        matrixMode = GL11.GL_MODELVIEW;
        entityRecorded = false;
        currentPlayer = false;
        red = green = blue = alpha = 1.0F;
        overlayRed = overlayGreen = overlayBlue = overlayAlpha = currentOverlayAlpha = 0.0F;
        effectTime = entity.getAge();
        itemGlintPass = 0;
        suppressFixedFunction = true;
        modelActive = true;
        return activeCapture = new Capture(true);
    }

    public static boolean record(ModelPart part, float scale) {
        if (!modelActive) {
            return false;
        }

        recordPart(part, scale);
        return true;
    }

    public static boolean recordItem(BakedModel model, ItemStack item, int color) {
        if (!modelActive) {
            return false;
        }
        EntityGeometry geometry;
        EntityRenderPass previousPass = currentPass;
        if (item != null && color == -1) {
            currentPass = EntityRenderPass.ITEM;
            selectTexture(currentBoundTexture);
            geometry = ITEM_GEOMETRY.getItem(model, item);
        } else if (item == null && color == -8372020 && itemGlintPass < 2) {
            currentPass = itemGlintPass++ == 0 ? EntityRenderPass.ITEM_GLINT_0 : EntityRenderPass.ITEM_GLINT_1;
            selectTexture(currentBoundTexture);
            geometry = ITEM_GEOMETRY.getFixed(model, color);
        } else {
            return false;
        }
        if (geometry == null) {
            currentPass = previousPass;
            selectTexture(currentBoundTexture);
            return false;
        }
        currentTexture.add(geometry, MATRICES[matrixDepth], lightU, lightV, currentTextureLayer,
                red, green, blue, alpha, effectTime,
                overlayRed, overlayGreen, overlayBlue, currentOverlayAlpha);
        if (item != null) {
            currentPass = previousPass;
            selectTexture(currentBoundTexture);
        }
        entityRecorded = true;
        instanceCount++;
        return true;
    }

    public static boolean recordBlock(BakedModel model, float brightness, float red, float green, float blue) {
        if (!modelActive) {
            return false;
        }
        EntityGeometry geometry = ITEM_GEOMETRY.getBlock(model, brightness, red, green, blue);
        if (geometry == null) {
            return false;
        }
        currentTexture.add(geometry, MATRICES[matrixDepth], lightU, lightV, currentTextureLayer,
                EntityInstancingRenderer.red, EntityInstancingRenderer.green, EntityInstancingRenderer.blue,
                alpha, effectTime, overlayRed, overlayGreen, overlayBlue, currentOverlayAlpha);
        entityRecorded = true;
        instanceCount++;
        return true;
    }

    public static boolean recordArrow(ArrowEntity arrow, double x, double y, double z, float tickDelta,
            Identifier texture) {
        boolean attached = modelActive;
        if (!attached && !frameActive) {
            return false;
        }
        if (!attached) {
            currentPass = EntityRenderPass.CULL_BACK;
        }
        selectTexture(texture);
        Matrix4f matrix = attached ? ARROW_MATRIX.set(MATRICES[matrixDepth]) : ARROW_MATRIX.identity();
        matrix
                .translate((float)x, (float)y, (float)z)
                .rotateY((float)Math.toRadians(arrow.lastYaw + (arrow.yaw - arrow.lastYaw) * tickDelta - 90.0F))
                .rotateZ((float)Math.toRadians(arrow.lastPitch + (arrow.pitch - arrow.lastPitch) * tickDelta));
        float shake = arrow.shake - tickDelta;
        if (shake > 0.0F) {
            matrix.rotateZ((float)Math.toRadians(-(float)Math.sin(shake * 3.0F) * shake));
        }
        matrix.rotateX((float)Math.toRadians(45.0F)).scale(0.05625F).translate(-4.0F, 0.0F, 0.0F);
        if (arrowGeometry == null) {
            arrowGeometry = new ArrowGeometry();
        }
        currentTexture.add(arrowGeometry, matrix, lightU, lightV, currentTextureLayer,
                1.0F, 1.0F, 1.0F, 1.0F, arrow.ticks + tickDelta,
                0.0F, 0.0F, 0.0F, 0.0F);
        if (attached) {
            entityRecorded = true;
        } else {
            entityCount++;
        }
        instanceCount++;
        return true;
    }

    public static boolean pushMatrix() {
        if (!tracksModelView()) {
            return false;
        }
        MATRICES[++matrixDepth].set(MATRICES[matrixDepth - 1]);
        return suppressFixedFunction;
    }

    public static boolean popMatrix() {
        if (!tracksModelView()) {
            return false;
        }
        if (matrixDepth > 0) {
            matrixDepth--;
        }
        return suppressFixedFunction;
    }

    public static boolean translate(float x, float y, float z) {
        if (!tracksModelView()) {
            return false;
        }
        MATRICES[matrixDepth].translate(x, y, z);
        return suppressFixedFunction;
    }

    public static boolean rotate(float angle, float x, float y, float z) {
        if (!tracksModelView()) {
            return false;
        }

        float length = (float)Math.sqrt(x * x + y * y + z * z);
        if (length != 0.0F) {
            MATRICES[matrixDepth].rotate((float)Math.toRadians(angle), x / length, y / length, z / length);
        }
        return suppressFixedFunction;
    }

    public static boolean scale(float x, float y, float z) {
        if (!tracksModelView()) {
            return false;
        }
        MATRICES[matrixDepth].scale(x, y, z);
        return suppressFixedFunction;
    }

    public static void setLight(float u, float v) {
        lightU = u;
        lightV = v;
    }

    public static void setMatrixMode(int mode) {
        matrixMode = mode;
    }

    private static boolean tracksModelView() {
        return activeCapture != null && matrixMode == GL11.GL_MODELVIEW;
    }

    public static void setColor(float red, float green, float blue, float alpha) {
        if (activeCapture != null) {
            EntityInstancingRenderer.red = red;
            EntityInstancingRenderer.green = green;
            EntityInstancingRenderer.blue = blue;
            EntityInstancingRenderer.alpha = alpha;
        }
    }

    public static void setTexture(Identifier texture) {
        if (activeCapture != null && texture != null) {
            if (modelActive && armorLayer) {
                currentPass = ENCHANTMENT_GLINT_TEXTURE.equals(texture)
                        ? EntityRenderPass.GLINT
                        : EntityRenderPass.NORMAL;
            }
            selectTexture(texture);
        }
    }

    public static void invalidateTexture(Texture texture) {
        selectedFrame = -1;
        TEXTURE_ARRAYS.invalidate(texture);
    }

    public static void flush(CommandList commandList) {
        if (!frameActive) {
            return;
        }

        frameActive = false;
        if (instanceCount == 0) {
            debugString = "Entity instancing: 0 entities";
            NAME_TAGS.render();
            return;
        }

        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        GlStateManager.disableCull();
        program.bind();
        program.getInterface().setUniforms();
        int previousArray = textureArraysSupported ? TEXTURE_ARRAYS.bindFallback() : 0;

        boolean failed = false;
        RenderMetrics.Category previous = RenderMetrics.setCategory(RenderMetrics.Category.ENTITY);
        try {
            EntityBatcher.Stats stats = BATCHER.render(commandList, program);
            drawCount = stats.draws();
            textureCount = stats.textures();
        } catch (RuntimeException exception) {
            failed = true;
            supported = false;
            LOGGER.error("Entity instancing disabled after a geometry failure", exception);
        } finally {
            RenderMetrics.setCategory(previous);
            if (textureArraysSupported) {
                TEXTURE_ARRAYS.restore(previousArray);
            }
            if (failed) {
                GlStateManager.depthMask(true);
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                GlStateManager.disableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            program.unbind();
            GlStateManager.enableCull();
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        }

        debugString = "Entity instancing: %d entities (%d players, %d array/%d fallback) | %d parts | %d draws | %d textures".formatted(
                entityCount, playerCount, arrayPlayerCount, fallbackPlayerCount, instanceCount, drawCount, textureCount);
        NAME_TAGS.render();
    }

    public static void deleteGeometry(CommandList commandList) {
        BATCHER.delete(commandList);
        ITEM_GEOMETRY.delete(commandList);
        if (arrowGeometry != null) {
            arrowGeometry.delete(commandList);
            arrowGeometry = null;
        }
    }

    public static String getDebugString() {
        return debugString;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static final class Capture implements AutoCloseable {
        private final boolean item;

        private Capture(boolean item) {
            this.item = item;
        }

        public void beginModel() {
            if (activeCapture != this) return;
            selectTexture(currentEntityTexture);
            currentModel.begin();
            modelActive = true;
            currentOverlayAlpha = overlayAlpha;
        }

        public boolean beginLayer(Object layer, LivingEntity entity) {
            EntityRenderPass pass = EntityLayerSupport.pass(layer, entity);
            if (activeCapture != this || pass == null) return false;
            currentPass = pass;
            selectTexture(currentBoundTexture);
            armorLayer = layer instanceof AbstractArmorLayer;
            currentOverlayAlpha = layer instanceof EntityRenderLayer<?> renderLayer
                    && renderLayer.colorsWhenDamaged() ? overlayAlpha : 0.0F;
            GLINT_PARTS.clear();
            itemGlintPass = 0;
            currentModel.begin();
            modelActive = true;
            red = green = blue = alpha = 1.0F;
            return true;
        }

        public void endLayer() {
            if (activeCapture != this) return;
            modelActive = false;
            currentPass = EntityRenderPass.NORMAL;
            armorLayer = false;
            red = green = blue = alpha = 1.0F;
        }

        public void endModel() {
            if (activeCapture == this) modelActive = false;
        }

        public boolean deferNameTag(LivingEntityRenderer<?> renderer, LivingEntity entity,
                double x, double y, double z) {
            if (activeCapture != this || !entityRecorded) return false;
            NAME_TAGS.add(renderer, entity, x, y, z);
            return true;
        }

        @Override
        public void close() {
            if (activeCapture != this) return;
            if (entityRecorded) {
                entityCount++;
                if (!this.item && currentPlayer) playerCount++;
            }
            modelActive = false;
            currentPlayer = false;
            suppressFixedFunction = false;
            itemGlintPass = 0;
            currentModel = null;
            currentTexture = null;
            currentEntityTexture = null;
            currentBoundTexture = null;
            currentPass = EntityRenderPass.NORMAL;
            armorLayer = false;
            activeCapture = null;
        }
    }

    private static void recordPart(ModelPart part, float scale) {
        if (part.invisible || !part.visible) {
            return;
        }
        if (currentPass == EntityRenderPass.GLINT && !GLINT_PARTS.add(part)) {
            return;
        }
        PartGeometry geometry = currentModel.getGeometry(part, scale);

        pushMatrix();
        translate(part.translateX, part.translateY, part.translateZ);
        translate(part.x * scale, part.y * scale, part.z * scale);
        MATRICES[matrixDepth].rotateZ(part.rotationZ).rotateY(part.rotationY).rotateX(part.rotationX);

        if (!part.boxes.isEmpty()) {
            currentTexture.add(geometry, MATRICES[matrixDepth], lightU, lightV, currentTextureLayer,
                    red, green, blue, alpha, effectTime,
                    overlayRed, overlayGreen, overlayBlue, currentOverlayAlpha);
            entityRecorded = true;
            instanceCount++;
        }

        if (part.children != null) {
            for (int i = 0; i < part.children.size(); i++) {
                recordPart(part.children.get(i), scale);
            }
        }
        popMatrix();
    }

    private static TextureArrayManager.Selection getTextureArray(Identifier texture) {
        if (!textureArraysSupported) {
            return null;
        }
        try {
            return TEXTURE_ARRAYS.select(texture, frame);
        } catch (RuntimeException exception) {
            textureArraysSupported = false;
            LOGGER.warn("Entity texture arrays disabled after a copy failure", exception);
            return null;
        }
    }

    private static void selectTexture(Identifier texture) {
        if (selectingTexture || texture == null) {
            return;
        }
        currentBoundTexture = texture;
        if (selectedFrame == frame && selectedTexturePass == currentPass && texture.equals(selectedTexture)) {
            currentTexture = selectedTextureBatch;
            currentTextureLayer = selectedTextureLayer;
            return;
        }
        TextureArrayManager.Selection selection = null;
        selectingTexture = true;
        try {
            if (!TextureAtlas.BLOCKS_LOCATION.equals(texture)) {
                selection = getTextureArray(texture);
            }
        } finally {
            selectingTexture = false;
        }
        if (selection != null) {
            currentTextureLayer = selection.layer();
            currentTexture = BATCHER.texture(selection.pool(), currentPass);
        } else {
            currentTextureLayer = 0;
            currentTexture = BATCHER.texture(texture, currentPass);
        }
        selectedFrame = frame;
        selectedTexture = texture;
        selectedTexturePass = currentPass;
        selectedTextureBatch = currentTexture;
        selectedTextureLayer = currentTextureLayer;
    }

    private static boolean initialize() {
        if (initialized) {
            return supported;
        }

        initialized = true;
        var capabilities = GL.getCapabilities();
        supported = capabilities.GL_ARB_draw_instanced
                && capabilities.GL_ARB_instanced_arrays
                && capabilities.OpenGL20;
        if (!supported) {
            LOGGER.warn("Entity instancing disabled: required OpenGL extensions are missing");
            return false;
        }

        try {
            if (capabilities.GL_EXT_texture_array
                    && capabilities.GL_EXT_framebuffer_object
                    && capabilities.GL_EXT_gpu_shader4) {
                try {
                    textureArraysSupported = TEXTURE_ARRAYS.initialize();
                } catch (RuntimeException exception) {
                    LOGGER.warn("Player skin texture arrays unavailable", exception);
                }
            }
            try {
                program = createProgram();
            } catch (RuntimeException exception) {
                if (!textureArraysSupported) {
                    throw exception;
                }
                LOGGER.warn("Player skin texture-array shader unavailable", exception);
                textureArraysSupported = false;
                program = createProgram();
            }
            program.bind();
            program.getInterface().initialize();
            program.unbind();
            LOGGER.info("Entity model-part instancing enabled");
        } catch (RuntimeException exception) {
            supported = false;
            LOGGER.error("Entity instancing failed to initialize", exception);
        }
        return supported;
    }

    private static GlProgram<EntityShader> createProgram() {
        ShaderConstants constants = textureArraysSupported
                ? ShaderConstants.builder().add("TEXTURE_ARRAY").build()
                : ShaderConstants.EMPTY;
        List<GlShader> shaders = List.of(
                ShaderLoader.loadShader(ShaderType.VERTEX, "argentum:entity_instancing.vert", constants),
                ShaderLoader.loadShader(ShaderType.FRAGMENT, "argentum:entity_instancing.frag", constants));
        try {
            GlProgram.Builder builder = GlProgram.builder("argentum:entity_instancing");
            shaders.forEach(builder::attachShader);
            return builder
                    .bindAttribute("aPosition", 0)
                    .bindAttribute("aTexCoord", 1)
                    .bindAttribute("aNormal", 2)
                    .bindAttribute("aModel0", 3)
                    .bindAttribute("aModel1", 4)
                    .bindAttribute("aModel2", 5)
                    .bindAttribute("aModel3", 6)
                    .bindAttribute("aLightCoord", 7)
                    .bindAttribute("aColor", 8)
                    .bindAttribute("aVertexColor", 9)
                    .bindAttribute("aEffectTime", 10)
                    .bindAttribute("aOverlay", 11)
                    .link(context -> new EntityShader(context, textureArraysSupported));
        } finally {
            shaders.forEach(GlShader::delete);
        }
    }

}
