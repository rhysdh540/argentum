package dev.rdh.cera;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.taumc.celeritas.api.options.structure.OptionFlag;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpl;
import org.taumc.celeritas.api.options.structure.OptionPage;

import java.util.List;

final class CeraOptionPage {
    private CeraOptionPage() {
    }

    static OptionPage create() {
        OptionGroup quality = OptionGroup.createBuilder()
                .setId(id("quality"))
                .add(OptionImpl.createBuilder(int.class, Cera.CONFIG_STORAGE)
                        .setId(id("better_grass"))
                        .setControl(option -> new SliderControl(option, 0, BetterGrass.Mode.values().length - 1, 1,
                                value -> text(BetterGrass.Mode.values()[value].key())))
                        .setBinding((config, value) -> config.betterGrass = BetterGrass.Mode.values()[value],
                                config -> config.betterGrass.ordinal())
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, Cera.CONFIG_STORAGE)
                        .setId(id("natural_textures"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.naturalTextures = value, config -> config.naturalTextures)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, Cera.CONFIG_STORAGE)
                        .setId(id("dynamic_lights"))
                        .setControl(option -> new SliderControl(option, 0, DynamicLights.Mode.values().length - 1, 1,
                                value -> text(DynamicLights.Mode.values()[value].key())))
                        .setBinding((config, value) -> config.dynamicLights = DynamicLights.Mode.values()[value],
                                config -> config.dynamicLights.ordinal())
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build();
        return new OptionPage(id("cera"), text("pages.cera"), List.of(quality));
    }

    private static <T> OptionIdentifier<T> id(String path) {
        return OptionIdentifier.create("cera", path).cast();
    }

    private static TextComponent text(String path, Object... args) {
        return TextComponent.translatable("cera.options." + path, args);
    }
}
