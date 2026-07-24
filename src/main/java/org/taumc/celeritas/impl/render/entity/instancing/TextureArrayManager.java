package org.taumc.celeritas.impl.render.entity.instancing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.platform.GLX;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.Texture;
import net.minecraft.resource.Identifier;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTTextureArray;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;

import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class TextureArrayManager {
    private static final int MAX_LAYERS = 256;
    private static final int MAX_POOL_BYTES = 8 * 1024 * 1024;

    private final Map<PoolKey, Pool> pools = new LinkedHashMap<>();
    private final Map<Texture, CachedTexture> textures = new IdentityHashMap<>();
    private int framebuffer;
    private int fallbackTexture;
    private int maxLayers;

    boolean initialize() {
        this.maxLayers = Math.min(GL11.glGetInteger(EXTTextureArray.GL_MAX_ARRAY_TEXTURE_LAYERS_EXT), MAX_LAYERS);
        if (this.maxLayers < 2) {
            return false;
        }
        int activeTexture = GL11.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        GlStateManager.activeTexture(GLX.GL_TEXTURE2);
        int previous = GL11.glGetInteger(EXTTextureArray.GL_TEXTURE_BINDING_2D_ARRAY_EXT);
        try {
            this.fallbackTexture = GL11.glGenTextures();
            GL11.glBindTexture(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, this.fallbackTexture);
            GL11.glTexParameteri(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, GL12C.GL_TEXTURE_MAX_LEVEL, 0);
            GL12C.glTexImage3D(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, 0, GL11.GL_RGBA8,
                    1, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
        } finally {
            GL11.glBindTexture(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, previous);
            GlStateManager.activeTexture(activeTexture);
        }
        this.framebuffer = EXTFramebufferObject.glGenFramebuffersEXT();
        return true;
    }

    int bindFallback() {
        GlStateManager.activeTexture(GLX.GL_TEXTURE2);
        int previous = GL11.glGetInteger(EXTTextureArray.GL_TEXTURE_BINDING_2D_ARRAY_EXT);
        GL11.glBindTexture(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, this.fallbackTexture);
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        return previous;
    }

    void restore(int texture) {
        GlStateManager.activeTexture(GLX.GL_TEXTURE2);
        GL11.glBindTexture(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, texture);
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
    }

    Selection select(Identifier location, int frame) {
        Minecraft minecraft = Minecraft.getInstance();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        Texture source = minecraft.getTextureManager().get(location);
        if (source == null) {
            minecraft.getTextureManager().bind(location);
            source = minecraft.getTextureManager().get(location);
        }

        int sourceId = source.getGlId();
        CachedTexture cached = this.textures.get(source);
        if (cached != null && cached.sourceId == sourceId) {
            Layer layer = cached.pool.getLayer(location, source, sourceId, frame);
            return layer != null ? layer.selection : null;
        }

        GlStateManager.bindTexture(sourceId);
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        int minFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
        if (width <= 0 || height <= 0 || minFilter >= GL11.GL_NEAREST_MIPMAP_NEAREST) {
            return null;
        }

        PoolKey key = new PoolKey(width, height, minFilter,
                GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER),
                GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S),
                GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T));
        Pool pool = this.pools.get(key);
        if (pool == null) {
            int capacity = (int)Math.min(this.maxLayers,
                    Math.max(2L, MAX_POOL_BYTES / Math.max(1L, (long)width * height * 4)));
            pool = new Pool(key, capacity);
            this.pools.put(key, pool);
        }
        this.textures.put(source, new CachedTexture(sourceId, pool));
        Layer layer = pool.getLayer(location, source, sourceId, frame);
        return layer != null ? layer.selection : null;
    }

    void invalidate(Texture texture) {
        this.textures.remove(texture);
        for (Pool pool : this.pools.values()) {
            pool.invalidate(texture);
        }
    }

    record Selection(Pool pool, int layer) {
    }

    final class Pool {
        private final Map<Identifier, Layer> layers = new LinkedHashMap<>(16, 0.75F, true);
        private final PoolKey key;
        private final int texture;
        private final int capacity;

        private Pool(PoolKey key, int capacity) {
            this.key = key;
            this.capacity = capacity;
            int activeTexture = GL11.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
            GlStateManager.activeTexture(GLX.GL_TEXTURE2);
            int previous = GL11.glGetInteger(EXTTextureArray.GL_TEXTURE_BINDING_2D_ARRAY_EXT);
            try {
                this.texture = GL11.glGenTextures();
                GL11.glBindTexture(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, this.texture);
                GL11.glTexParameteri(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, GL11.GL_TEXTURE_MIN_FILTER, key.minFilter);
                GL11.glTexParameteri(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, GL11.GL_TEXTURE_MAG_FILTER, key.magFilter);
                GL11.glTexParameteri(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, GL11.GL_TEXTURE_WRAP_S, key.wrapS);
                GL11.glTexParameteri(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, GL11.GL_TEXTURE_WRAP_T, key.wrapT);
                GL11.glTexParameteri(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, GL12C.GL_TEXTURE_MAX_LEVEL, 0);
                GL12C.glTexImage3D(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, 0, GL11.GL_RGBA8,
                        key.width, key.height, capacity, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
            } finally {
                GL11.glBindTexture(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, previous);
                GlStateManager.activeTexture(activeTexture);
            }
        }

        int bind() {
            GlStateManager.activeTexture(GLX.GL_TEXTURE2);
            int previous = GL11.glGetInteger(EXTTextureArray.GL_TEXTURE_BINDING_2D_ARRAY_EXT);
            GL11.glBindTexture(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, this.texture);
            GlStateManager.activeTexture(GLX.GL_TEXTURE0);
            return previous;
        }

        void restore(int texture) {
            GlStateManager.activeTexture(GLX.GL_TEXTURE2);
            GL11.glBindTexture(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, texture);
            GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        }

        private Layer getLayer(Identifier location, Texture source, int sourceId, int frame) {
            Layer layer = this.layers.get(location);
            if (layer != null && layer.source == source && layer.sourceId == sourceId) {
                layer.frame = frame;
                return layer;
            }
            if (layer == null) {
                int index = this.layers.size();
                if (index == this.capacity) {
                    Iterator<Layer> iterator = this.layers.values().iterator();
                    while (iterator.hasNext()) {
                        Layer candidate = iterator.next();
                        if (candidate.frame != frame) {
                            index = candidate.index;
                            iterator.remove();
                            break;
                        }
                    }
                    if (index == this.capacity) {
                        return null;
                    }
                }
                layer = new Layer(index, new Selection(this, index));
                this.layers.put(location, layer);
            }
            if (!copy(sourceId, layer.index)) {
                return null;
            }
            layer.source = source;
            layer.sourceId = sourceId;
            layer.frame = frame;
            return layer;
        }

        private boolean copy(int sourceTexture, int layer) {
            int previousFramebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
            int activeTexture = GL11.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
            GlStateManager.activeTexture(GLX.GL_TEXTURE2);
            int previousArray = GL11.glGetInteger(EXTTextureArray.GL_TEXTURE_BINDING_2D_ARRAY_EXT);
            try {
                EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
                EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                        EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, sourceTexture, 0);
                if (EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT)
                        != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) {
                    return false;
                }
                GL11.glBindTexture(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, this.texture);
                GL12C.glCopyTexSubImage3D(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, 0,
                        0, 0, layer, 0, 0, this.key.width, this.key.height);
                return true;
            } finally {
                GL11.glBindTexture(EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT, previousArray);
                GlStateManager.activeTexture(activeTexture);
                EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, previousFramebuffer);
            }
        }

        private void invalidate(Texture texture) {
            for (Layer layer : this.layers.values()) {
                if (layer.source == texture) layer.sourceId = -1;
            }
        }
    }

    private record PoolKey(int width, int height, int minFilter, int magFilter, int wrapS, int wrapT) {
    }

    private record CachedTexture(int sourceId, Pool pool) {
    }

    private static final class Layer {
        private final int index;
        private final Selection selection;
        private Texture source;
        private int sourceId = -1;
        private int frame;

        private Layer(int index, Selection selection) {
            this.index = index;
            this.selection = selection;
        }
    }
}
