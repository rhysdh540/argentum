package dev.rdh.argentum.impl.render.hud.item;

import dev.rdh.argentum.impl.Argentum;
import dev.rdh.argentum.impl.render.hud.HudRecorder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.TextureAtlas;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public final class GuiItemIcons {
    private static final int ICON_SIZE = 16;

    private static final GuiItemAtlas ATLAS = new GuiItemAtlas();
    private static final HudRecorder RECORDER = new HudRecorder();

    private static boolean initialized;
    private static int readyTick;
    private static boolean baking;

    private GuiItemIcons() {
    }

    public static boolean enabled() {
        if (!Argentum.CONFIG.guiItemAtlas) return false;
        if (!initialized) {
            initialized = true;
            ATLAS.initialize();
            readyTick = currentTick() + 1;
        }
        // the atlas is created mid-frame, so leave that frame on the vanilla path
        return ATLAS.isSupported() && currentTick() >= readyTick;
    }

    // glint masks itself with GL_EQUAL against the depth the model wrote, which a flat icon has not
    public static boolean canBake(ItemStack item) {
        return item != null && item.getItem() != null && !item.hasEnchantmentGlint();
    }

    public static void invalidate() {
        ATLAS.invalidate();
    }

    public static int acquire(Object model, ItemStack item, Runnable bake) {
        return ATLAS.acquire(GuiItemAtlas.keyFor(model, item), currentTick(), iconPixels(), () -> {
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0, ICON_SIZE, ICON_SIZE, 0.0, 1000.0, 3000.0);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            GlStateManager.translatef(0.0F, 0.0F, -2000.0F);

            baking = true;
            try {
                bake.run();
            } finally {
                baking = false;
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
            }
        });
    }

    public static boolean baking() {
        return baking;
    }

    private static int iconPixels() {
        int scale = new Window(Minecraft.getInstance()).getScale();
        return Math.min(ICON_SIZE * Math.max(scale, 1), GuiItemAtlas.maxPixels());
    }

    private static int currentTick() {
        var world = Minecraft.getInstance().world;
        return world != null ? (int) world.getTime() : (int) (System.nanoTime() / 50_000_000L);
    }

    public static void draw(int slot, int x, int y, float zOffset) {
        float u0 = GuiItemAtlas.u0(slot);
        float v0 = GuiItemAtlas.v0(slot);
        float extent = ATLAS.uvExtent();

        // the framebuffer's origin is bottom left, so v runs the opposite way to GUI space
        RECORDER.quad(HudRecorder.LAYER_CONTENT, HudRecorder.MATERIAL_PREMULTIPLIED, ATLAS.getTexture(),
                x, y, x + ICON_SIZE, y + ICON_SIZE,
                u0, v0 + extent, u0 + extent, v0,
                100.0F + zOffset, 0xFFFFFFFF, 0xFFFFFFFF);
    }

    /**
     * Anything drawing over an icon either turns depth testing off first, in which case it depends on
     * paint order and the pending icons have to go down now, or it uses a greater z and depth sorts it.
     */
    public static void flush() {
        if (baking || RECORDER.isEmpty()) return;

        GlStateManager.disableLighting();
        RECORDER.flush();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        Minecraft.getInstance().getTextureManager().bind(TextureAtlas.BLOCKS_LOCATION);
    }
}
