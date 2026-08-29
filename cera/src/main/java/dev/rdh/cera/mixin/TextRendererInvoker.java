package dev.rdh.cera.mixin;

import net.minecraft.client.render.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TextRenderer.class)
public interface TextRendererInvoker {
    @Invoker("init")
    void cera$init();
}
