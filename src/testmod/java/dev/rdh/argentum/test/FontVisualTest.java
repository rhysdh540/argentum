package dev.rdh.argentum.test;

import net.fabricmc.api.ClientModInitializer;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;

public class FontVisualTest implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MinecraftClientEvents.READY.register(minecraft -> minecraft.openScreen(new FontVisualTestScreen()));
    }
}
