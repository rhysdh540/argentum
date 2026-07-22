package org.taumc.celeritas.impl.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.lwjgl.opengl.GL11;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class LegacyDrawContext implements DrawContext {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final TextRenderer font = this.minecraft.textRenderer;

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        GuiElement.fill(x1, y1, x2, y2, color);
    }

    @Override
    public int drawString(TextComponent text, int x, int y, int color, boolean shadow) {
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        int width = this.font.draw(this.extractString(text), x, y, color, shadow);
        GlStateManager.disableBlend();
        return width;
    }

    @Override
    public void blitWholeImage(String texture, int x, int y, int width, int height) {
        this.minecraft.getTextureManager().bind(new Identifier(texture));
        GuiElement.drawTexture(x, y, 0.0F, 0.0F, width, height, width, height);
    }

    @Override
    public void pushMatrix() {
        GlStateManager.pushMatrix();
    }

    @Override
    public void translate(double x, double y, double z) {
        GlStateManager.translated(x, y, z);
    }

    @Override
    public void popMatrix() {
        GlStateManager.popMatrix();
    }

    @Override
    public void enableScissor(int x, int y, int x2, int y2) {
        Window window = new Window(this.minecraft);
        int scale = window.getScale();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, this.minecraft.height - y2 * scale,
                (x2 - x + 1) * scale, (y2 - y + 1) * scale);
    }

    @Override
    public void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public int getStringWidth(TextComponent text) {
        return this.font.getWidth(this.extractString(text));
    }

    @Override
    public String substrByWidth(String text, int width) {
        return this.font.trim(text, width);
    }

    @Override
    public List<TextComponent> split(TextComponent text, int width) {
        return this.font.split(this.extractString(text), width).stream().map(TextComponent::literal).toList();
    }

    @Override
    public String extractString(TextComponent component) {
        if (component instanceof TextComponent.Literal(String text)) {
            return text;
        }
        if (component instanceof TextComponent.Translatable(List<String> keys, List<?> args)) {
            return I18n.translate(
                    keys.getFirst(),
                    args.stream()
                            .map(arg -> arg instanceof TextComponent c ? this.extractString(c) : arg)
                            .toArray()
            );
        }
        if (component instanceof TextComponent.Styled(TextComponent inner, java.util.EnumSet<TextFormattingStyle> styles)) {
            StringBuilder result = new StringBuilder();
            for (TextFormattingStyle style : styles) {
                result.append('§').append(style.isColor() ? Integer.toHexString(style.ordinal())
                        : switch (style) {
                            case STRIKETHROUGH -> "m";
                            case UNDERLINE -> "n";
                            case ITALIC -> "o";
                            default -> "";
                        });
            }
            return result.append(this.extractString(inner)).append("§r").toString();
        }
        throw new IllegalArgumentException("Unsupported text component: " + component.getClass().getName());
    }

    @Override
    public int lineHeight() {
        return this.font.fontHeight;
    }

    @Override
    public TextComponent getFriendlyModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> TextComponent.literal(container.getMetadata().getName()))
                .orElseGet(() -> DrawContext.super.getFriendlyModName(modId));
    }

    @Override
    public @Nullable String getModLogoPath(String modId) {
        // TODO
        return "celeritas".equals(modId) ? "celeritas:textures/gui/icon.png" : null;
    }
}
