package org.taumc.celeritas.impl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.embeddedt.embeddium.impl.gl.device.GLRenderDevice;
import org.lwjgl.opengl.GL15C;
import org.taumc.celeritas.impl.config.CeleritasConfig;
import org.taumc.celeritas.impl.config.JsonOptionStorage;

public class Celeritas implements ClientModInitializer {
    public static final String MODID = "celeritas";
    public static String VERSION;
    public static CeleritasConfig CONFIG = new CeleritasConfig();
    public static JsonOptionStorage<CeleritasConfig> CONFIG_STORAGE;

    @Override
    public void onInitializeClient() {
        GLRenderDevice.VANILLA_STATE_RESETTER = () -> {
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
        };
        FabricLoader loader = FabricLoader.getInstance();
        VERSION = loader.getModContainer(MODID).orElseThrow().getMetadata().getVersion().toString();
        CONFIG_STORAGE = JsonOptionStorage.load(loader.getConfigDir().resolve("celeritas.json"),
                CeleritasConfig.class, CeleritasConfig::new, CeleritasConfig::validate);
        CONFIG = CONFIG_STORAGE.getData();
    }
}
