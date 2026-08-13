package dev.rdh.argentum.impl.render.entity;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.platform.GLX;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.mob.MobEntity;
import net.minecraft.resource.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttribute;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeBinding;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderComponent;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.shader.ShaderLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import dev.rdh.argentum.impl.debug.RenderMetrics;
import dev.rdh.argentum.impl.render.instancing.InstancedGeometryBuffer;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EntityShadowBatch {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Identifier SHADOW_TEXTURE = new Identifier("textures/misc/shadow.png");
    private static final int INSTANCE_FLOATS = 10;
    private static final GlVertexAttributeBinding[] VERTEX_FORMAT = {
            binding(0, 2, 2 * Float.BYTES, 0, 0)
    };
    private static final GlVertexAttributeBinding[] INSTANCE_FORMAT = {
            binding(1, 4, INSTANCE_FLOATS * Float.BYTES, 0, 1),
            binding(2, 4, INSTANCE_FLOATS * Float.BYTES, 4 * Float.BYTES, 1),
            binding(3, 2, INSTANCE_FLOATS * Float.BYTES, 8 * Float.BYTES, 1)
    };
    private final Long2ReferenceOpenHashMap<Block> blocks = new Long2ReferenceOpenHashMap<>();
    private final Long2LongOpenHashMap light = new Long2LongOpenHashMap();
    private final BlockPos.Mutable pos = new BlockPos.Mutable();
    private final BlockPos.Mutable below = new BlockPos.Mutable();
    private final Map<ChunkShaderComponent.Factory<?>, GlProgram<ShadowShader>> programs = new HashMap<>();

    private float[] instances = new float[4096 * INSTANCE_FLOATS];
    private FloatBuffer upload = BufferUtils.createFloatBuffer(instances.length);
    private boolean initialized;
    private boolean supported;
    private boolean active;
    private InstancedGeometryBuffer geometry;
    private int size;
    private int quads;
    private GlProgram<ShadowShader> program;

    public EntityShadowBatch() {
        light.defaultReturnValue(Long.MIN_VALUE);
    }

    public void beginFrame() {
        blocks.clear();
        light.clear();
        size = 0;
        quads = 0;
        active = initialize();
    }

    public boolean record(World world, Entity entity, double dx, double dy, double dz, float opacity, float tickDelta, float shadowSize) {
        if (!active) {
            return false;
        }

        float scaledSize = shadowSize;
        if (entity instanceof MobEntity mob) {
            scaledSize *= mob.getShadowScale();
            if (mob.isBaby()) {
                scaledSize *= 0.5F;
            }
        }

        double entityX = entity.prevX + (entity.x - entity.prevX) * tickDelta;
        double entityY = entity.prevY + (entity.y - entity.prevY) * tickDelta;
        double entityZ = entity.prevZ + (entity.z - entity.prevZ) * tickDelta;
        int minX = MathHelper.floor(entityX - scaledSize);
        int maxX = MathHelper.floor(entityX + scaledSize);
        int minY = MathHelper.floor(entityY - scaledSize);
        int maxY = MathHelper.floor(entityY);
        int minZ = MathHelper.floor(entityZ - scaledSize);
        int maxZ = MathHelper.floor(entityZ + scaledSize);
        double offsetX = dx - entityX;
        double offsetY = dy - entityY;
        double offsetZ = dz - entityZ;

        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    recordBlock(world, x, y, z, dx, dy, dz, opacity, scaledSize, offsetX, offsetY, offsetZ);
                }
            }
        }
        return true;
    }

    public void flush(CommandList commandList) {
        if (!active) {
            return;
        }
        active = false;
        if (quads == 0) {
            return;
        }
        if (!initializeGeometry(commandList)) {
            return;
        }

        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        Minecraft.getInstance().getTextureManager().bind(SHADOW_TEXTURE);
        GlStateManager.depthMask(false);
        program.bind();
        program.getInterface().fog().setup();

        if (upload.capacity() < size) {
            upload = BufferUtils.createFloatBuffer(instances.length);
        }
        upload.clear().put(instances, 0, size).flip();
        RenderMetrics.Category previous = RenderMetrics.setCategory(RenderMetrics.Category.ENTITY);
        try {
            geometry.draw(commandList, upload, 4, quads);
            RenderMetrics.recordDraw();
        } catch (RuntimeException exception) {
            supported = false;
            LOGGER.error("Instanced entity shadows disabled after a draw failure", exception);
        } finally {
            RenderMetrics.setCategory(previous);
            program.unbind();
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
        }
    }

    private void recordBlock(World world, int x, int y, int z, double dx, double dy, double dz, float opacity,
            float shadowSize, double offsetX, double offsetY, double offsetZ) {
        pos.set(x, y, z);
        long key = pos.toLong();
        Block block = blocks.get(key);
        if (block == null) {
            below.set(x, y - 1, z);
            block = world.getBlockState(below).getBlock();
            blocks.put(key, block);
        }

        long light = this.light.get(key);
        if (light == Long.MIN_VALUE) {
            int raw = world.getRawBrightness(pos);
            float brightness = world.dimension.getBrightnessTable()[raw];
            light = (long)raw << 32 | Float.floatToRawIntBits(brightness) & 0xffffffffL;
            this.light.put(key, light);
        }
        if (block.getRenderType() == -1 || (int)(light >>> 32) <= 3 || !block.isCube()) {
            return;
        }

        double alpha = (opacity - (dy - (y + offsetY)) / 2.0) * 0.5
                * Float.intBitsToFloat((int)light);
        if (alpha < 0.0) {
            return;
        }
        alpha = Math.min(alpha, 1.0);

        double minX = x + block.getMinX() + offsetX;
        double maxX = x + block.getMaxX() + offsetX;
        double surfaceY = y + block.getMinY() + offsetY + 0.015625;
        double minZ = z + block.getMinZ() + offsetZ;
        double maxZ = z + block.getMaxZ() + offsetZ;
        float minU = (float)((dx - minX) / 2.0 / shadowSize + 0.5);
        float maxU = (float)((dx - maxX) / 2.0 / shadowSize + 0.5);
        float minV = (float)((dz - minZ) / 2.0 / shadowSize + 0.5);
        float maxV = (float)((dz - maxZ) / 2.0 / shadowSize + 0.5);

        if (size + INSTANCE_FLOATS > instances.length) {
            instances = Arrays.copyOf(instances, instances.length * 2);
        }
        instances[size++] = (float)minX;
        instances[size++] = (float)surfaceY;
        instances[size++] = (float)minZ;
        instances[size++] = (float)maxX;
        instances[size++] = (float)maxZ;
        instances[size++] = minU;
        instances[size++] = minV;
        instances[size++] = maxU;
        instances[size++] = maxV;
        instances[size++] = (float)alpha;
        quads++;
    }

    private boolean initialize() {
        if (!initialized) {
            initialized = true;
            var capabilities = GL.getCapabilities();
            supported = capabilities.GL_ARB_draw_instanced
                    && capabilities.GL_ARB_instanced_arrays
                    && capabilities.OpenGL20;
            if (!supported) {
                LOGGER.warn("Instanced entity shadows disabled: required OpenGL extensions are missing");
                return false;
            }

            FloatBuffer corners = BufferUtils.createFloatBuffer(8);
            corners.put(new float[]{0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F}).flip();
            geometry = new InstancedGeometryBuffer(corners, VERTEX_FORMAT, INSTANCE_FORMAT);
        }

        if (!supported) {
            return false;
        }

        try {
            ChunkShaderComponent.Factory<?> fogFactory = ChunkShaderFogComponent.FOG_SERVICE.getFogMode();
            program = programs.get(fogFactory);
            if (program == null) {
                boolean firstProgram = programs.isEmpty();
                program = createProgram(fogFactory);
                programs.put(fogFactory, program);
                program.bind();
                try {
                    program.getInterface().texture().setInt(0);
                } finally {
                    program.unbind();
                }
                if (firstProgram) {
                    LOGGER.info("Instanced entity shadows enabled");
                }
            }
        } catch (RuntimeException exception) {
            supported = false;
            LOGGER.error("Instanced entity shadows failed to initialize", exception);
        }
        return supported;
    }

    private boolean initializeGeometry(CommandList commandList) {
        try {
            geometry.initialize(commandList);
            return true;
        } catch (RuntimeException exception) {
            geometry.delete(commandList);
            supported = false;
            LOGGER.error("Instanced entity shadow geometry failed to initialize", exception);
            return false;
        }
    }

    private static GlVertexAttributeBinding binding(int index, int count, int stride, int pointer, int divisor) {
        return new GlVertexAttributeBinding(index,
                new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, "", count, false, pointer, stride, false), divisor);
    }

    public void close(CommandList commandList) {
        active = false;
        if (geometry != null) {
            geometry.delete(commandList);
            geometry = null;
        }
        programs.values().forEach(GlProgram::delete);
        programs.clear();
        program = null;
        initialized = false;
        supported = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private static GlProgram<ShadowShader> createProgram(ChunkShaderComponent.Factory<?> fogFactory) {
        ShaderConstants constants = ShaderConstants.builder().addAll(fogFactory.getDefines()).build();
        List<GlShader> shaders = List.of(
                ShaderLoader.loadShader(ShaderType.VERTEX, "argentum:entity_shadow.vert", constants),
                ShaderLoader.loadShader(ShaderType.FRAGMENT, "argentum:entity_shadow.frag", constants)
        );
        try {
            GlProgram.Builder builder = GlProgram.builder("argentum:entity_shadow");
            shaders.forEach(builder::attachShader);
            return builder
                    .bindAttribute("aCorner", 0)
                    .bindAttribute("aBounds0", 1)
                    .bindAttribute("aBounds1", 2)
                    .bindAttribute("aShadow", 3)
                    .link(ctx -> new ShadowShader(ctx, fogFactory));
        } finally {
            shaders.forEach(GlShader::delete);
        }
    }

    private record ShadowShader(GlUniformInt texture, ChunkShaderComponent fog) {
        ShadowShader(ShaderBindingContext ctx, ChunkShaderComponent.Factory<?> fogFactory) {
            this(ctx.bindUniform("uTexture", GlUniformInt::new), fogFactory.create(ctx));
        }
    }
}
