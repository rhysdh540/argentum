package dev.rdh.argentum.impl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.embeddedt.embeddium.impl.gl.device.GLRenderDevice;
import org.lwjgl.opengl.GL15C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.rdh.argentum.impl.config.ArgentumConfig;
import dev.rdh.argentum.impl.config.JsonOptionStorage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Argentum implements ClientModInitializer {
    public static final String MODID = "argentum";
    public static String VERSION;
    public static ArgentumConfig CONFIG = new ArgentumConfig();
    public static JsonOptionStorage<ArgentumConfig> CONFIG_STORAGE;

    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitializeClient() {
        GLRenderDevice.VANILLA_STATE_RESETTER = () -> {
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
        };
        FabricLoader loader = FabricLoader.getInstance();
        VERSION = loader.getModContainer(MODID).orElseThrow().getMetadata().getVersion().toString();
        CONFIG_STORAGE = JsonOptionStorage.load(getConfigPath(loader.getConfigDir()), ArgentumConfig.class, ArgentumConfig::new, ArgentumConfig::validate);
        CONFIG = CONFIG_STORAGE.getData();
    }

    private Path getConfigPath(Path c) {
        Path celery = c.resolve("celeritas.json");
        Path argentum = c.resolve("argentum.json");
        if (Files.exists(celery)) {
            if (!Files.exists(argentum)) {
                try {
                    Files.move(celery, argentum);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                try {
                    Files.delete(celery);
                } catch (IOException e) {
					throw new UncheckedIOException(e);
				}
            }
        }
        return argentum;
    }
}
