package dev.rdh.cera;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.ornithemc.osl.resource.loader.api.client.ClientResourceLoaderEvents;
import dev.rdh.argentum.impl.config.JsonOptionStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.taumc.celeritas.api.OptionGUIConstructionEvent;

public class Cera implements ClientModInitializer {
    public static CeraConfig CONFIG = new CeraConfig();
    static JsonOptionStorage<CeraConfig> CONFIG_STORAGE;

	public static final Logger LOGGER = LogManager.getLogger("Cera");

    @Override
    public void onInitializeClient() {
        CONFIG_STORAGE = JsonOptionStorage.load(FabricLoader.getInstance().getConfigDir().resolve("cera.json"),
                CeraConfig.class, CeraConfig::new, CeraConfig::validate);
        CONFIG = CONFIG_STORAGE.getData();
        OptionGUIConstructionEvent.BUS.addListener(event -> event.addPage(CeraOptionPage.create()));
        ClientResourceLoaderEvents.END_RESOURCE_RELOAD.register((resources, context) ->
                NaturalTextures.reload(resources));
    }
}
