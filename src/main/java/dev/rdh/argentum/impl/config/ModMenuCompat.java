package dev.rdh.argentum.impl.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import dev.rdh.argentum.impl.gui.VideoOptionsScreen;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.embeddedt.embeddium.impl.gui.CeleritasVideoOptionsController;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.OptionGUIConstructionEvent;
import org.taumc.celeritas.api.options.structure.OptionPage;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ModMenuCompat implements ModMenuApi {
	private static final AtomicReference<TextComponent> SELECTED_TAB = resolveSelectedTabRef();

	@SuppressWarnings("unchecked")
	private static AtomicReference<TextComponent> resolveSelectedTabRef() {
		try {
			Field field = CeleritasVideoOptionsController.class.getDeclaredField("tabFrameSelectedTab");
			field.setAccessible(true);
			return (AtomicReference<TextComponent>) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return create(TextComponent.literal("argentum.options.pages.general"));
	}

	@Override
	public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
		List<OptionPage> pages = new ObjectArrayList<>();
		OptionGUIConstructionEvent.BUS.post(new OptionGUIConstructionEvent(pages));
		Map<String, ConfigScreenFactory<?>> ids = new Object2ObjectOpenHashMap<>();
		for (OptionPage page : pages) {
			ids.computeIfAbsent(page.getId().getModId(), _ -> create(page.getName()));
		}
		return Map.copyOf(ids);
	}

	private ConfigScreenFactory<VideoOptionsScreen> create(TextComponent t) {
		return parent -> {
			SELECTED_TAB.set(t);
			return new VideoOptionsScreen(parent);
		};
	}
}
