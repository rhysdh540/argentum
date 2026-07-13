package org.taumc.celeritas.test;

import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screen.Screen;

public class FontVisualTestScreen extends Screen {
    private final String variant = System.getProperty("celeritas.fontTestVariant", "batched");
    private int ticks;

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        fill(0, 0, this.width, this.height, 0xFF202020);

        int x = 16;
        int y = 16;
        this.textRenderer.draw("Celeritas font visual test", x, y, 0xFFFFFFFF);
        this.textRenderer.draw("Formatting: §aGreen §lBold §oItalic §rReset", x, y + 16, 0xFFFFFFFF);
        this.textRenderer.draw("Unicode: Ελληνικά Русский 日本語 Ğğ", x, y + 32, 0xFFFFFFFF);
        this.textRenderer.drawWithShadow("Shadow: colored text", x, y + 48, 0xFFFFAA55);
        this.textRenderer.draw("Decorations: §nUnderline §mStrike §n§mBoth", x, y + 64, 0xFFFFFFFF);

        boolean bidirectional = this.textRenderer.isBidirectional();
        this.textRenderer.setBidirectional(true);
        this.textRenderer.draw("שלום עולם", x, y + 80, 0xFFFFFFFF);
        this.textRenderer.setBidirectional(bidirectional);
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.ticks == 5) {
            this.takeScreenshot("before-reload");
        } else if (this.ticks == 6) {
            this.minecraft.reloadResources();
        } else if (this.ticks == 12) {
            this.takeScreenshot("after-reload");
        } else if (this.ticks == 13) {
            this.minecraft.stop();
        }
    }

    private void takeScreenshot(String stage) {
        Screenshot.take(this.minecraft.gameDir, "font-" + this.variant + "-" + stage + ".png",
                this.minecraft.width, this.minecraft.height, this.minecraft.getRenderTarget());
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
