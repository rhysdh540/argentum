package dev.rdh.argentum.extras;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.taumc.celeritas.api.OptionGUIConstructionEvent;
import org.taumc.celeritas.impl.config.JsonOptionStorage;

public class ArgentumExtras implements ClientModInitializer {
    public static ArgentumExtrasConfig CONFIG = new ArgentumExtrasConfig();
    static JsonOptionStorage<ArgentumExtrasConfig> CONFIG_STORAGE;

	@Override
	public void onInitializeClient() {
		CONFIG_STORAGE = JsonOptionStorage.load(FabricLoader.getInstance().getConfigDir().resolve("argentum-extras.json"),
                ArgentumExtrasConfig.class, ArgentumExtrasConfig::new, ArgentumExtrasConfig::validate);
        CONFIG = CONFIG_STORAGE.getData();
        OptionGUIConstructionEvent.BUS.addListener(event -> event.addPage(ArgentumExtrasOptionPage.create()));
	}
}
