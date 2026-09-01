package dev.rdh.cera;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.taumc.celeritas.api.options.structure.OptionFlag;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpl;
import org.taumc.celeritas.api.options.structure.OptionPage;

import dev.rdh.cera.modules.BetterGrass;
import dev.rdh.cera.modules.ctm.ConnectedTextures;
import dev.rdh.cera.modules.DynamicLights;
import net.minecraft.client.Minecraft;

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
                .add(OptionImpl.createBuilder(boolean.class, Cera.CONFIG_STORAGE)
                        .setId(id("animated_textures"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> {
                            if (config.animatedTextures != value) {
                                config.animatedTextures = value;
                                Minecraft.getInstance().getTextureManager().cera$getAnimatedTextures().setEnabled(value);
                            }
                        }, config -> config.animatedTextures)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, Cera.CONFIG_STORAGE)
                        .setId(id("custom_colors"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> {
                            config.customColors = value;
                            Minecraft.getInstance().cera$getCustomColors().reapplyTextColors();
                        }, config -> config.customColors)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, Cera.CONFIG_STORAGE)
                        .setId(id("custom_lightmaps"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.customLightmaps = value, config -> config.customColors)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, Cera.CONFIG_STORAGE)
                        .setId(id("random_entities"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.randomEntities = value, config -> config.randomEntities)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, Cera.CONFIG_STORAGE)
                        .setId(id("custom_sky"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.customSky = value, config -> config.customSky)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, Cera.CONFIG_STORAGE)
                        .setId(id("custom_guis"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.customGuis = value, config -> config.customGuis)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, Cera.CONFIG_STORAGE)
                        .setId(id("custom_items"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.customItems = value, config -> config.customItems)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, Cera.CONFIG_STORAGE)
                        .setId(id("optifine_cosmetics"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.optifineCosmetics = value, config -> config.optifineCosmetics)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, Cera.CONFIG_STORAGE)
                        .setId(id("emissive_textures"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.emissiveTextures = value, config -> config.emissiveTextures)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, Cera.CONFIG_STORAGE)
                        .setId(id("custom_block_layers"))
                        .setControl(TickBoxControl::new)
                        .setBinding((config, value) -> config.customBlockLayers = value, config -> config.customBlockLayers)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD, OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, Cera.CONFIG_STORAGE)
                        .setId(id("connected_textures"))
                        .setControl(option -> new SliderControl(option, 0, ConnectedTextures.Mode.values().length - 1, 1,
                                value -> text(ConnectedTextures.Mode.values()[value].key())))
                        .setBinding((config, value) -> config.connectedTextures = ConnectedTextures.Mode.values()[value],
                                config -> config.connectedTextures.ordinal())
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
