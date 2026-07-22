package org.taumc.celeritas.impl.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.Window;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkBuilder;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.lwjgl.opengl.Display;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.CyclingControl;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.taumc.celeritas.api.options.structure.OptionFlag;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpact;
import org.taumc.celeritas.api.options.structure.OptionImpl;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.taumc.celeritas.api.options.structure.StandardOptions;
import org.taumc.celeritas.impl.Celeritas;
import org.taumc.celeritas.impl.config.CeleritasConfig;
import org.taumc.celeritas.impl.config.JsonOptionStorage;

import java.util.List;

final class CeleritasOptionPages {
    private static final GameOptionsStorage VANILLA = new GameOptionsStorage();
    private static final CeleritasConfig CONFIG = Celeritas.CONFIG;
    private static final JsonOptionStorage<CeleritasConfig> CONFIG_STORAGE = Celeritas.CONFIG_STORAGE;

    private CeleritasOptionPages() {
    }

    static OptionPage general() {
        OptionGroup rendering = OptionGroup.createBuilder()
                .setId(StandardOptions.Group.RENDERING)
                .add(OptionImpl.createBuilder(int.class, VANILLA)
                        .setId(StandardOptions.Option.RENDER_DISTANCE.cast())
                        .setName(vanilla("options.renderDistance"))
                        .setTooltip(text("render_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 2, 32, 1,
                                value -> vanilla("options.chunks", value)))
                        .setBinding((options, value) -> options.viewDistance = value, options -> options.viewDistance)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, VANILLA)
                        .setId(StandardOptions.Option.BRIGHTNESS.cast())
                        .setName(vanilla("options.gamma"))
                        .setTooltip(text("brightness.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 100, 1, ControlValueFormatter.brightness()))
                        .setBinding((options, value) -> options.gamma = value / 100.0F,
                                options -> Math.round(options.gamma * 100.0F))
                        .build())
                .build();

        OptionGroup window = OptionGroup.createBuilder()
                .setId(StandardOptions.Group.WINDOW)
                .add(OptionImpl.createBuilder(int.class, VANILLA)
                        .setId(StandardOptions.Option.GUI_SCALE.cast())
                        .setName(vanilla("options.guiScale"))
                        .setTooltip(text("gui_scale.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.guiScale()))
                        .setBinding((options, value) -> {
                            options.guiScale = value;
                            Minecraft minecraft = Minecraft.getInstance();
                            Window scaledWindow = new Window(minecraft);
                            minecraft.screen.resize(minecraft, scaledWindow.getWidth(), scaledWindow.getHeight());
                        }, options -> options.guiScale)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, VANILLA)
                        .setId(StandardOptions.Option.FULLSCREEN.cast())
                        .setName(vanilla("options.fullscreen"))
                        .setTooltip(text("fullscreen.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((options, value) -> {
                            options.fullscreen = value;
                            Minecraft minecraft = Minecraft.getInstance();
                            if (minecraft.isFullscreen() != value) {
                                minecraft.toggleFullscreen();
                                options.fullscreen = minecraft.isFullscreen();
                            }
                        }, options -> options.fullscreen)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, VANILLA)
                        .setId(StandardOptions.Option.VSYNC.cast())
                        .setName(vanilla("options.vsync"))
                        .setTooltip(text("vsync.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((options, value) -> {
                            options.vsync = value;
                            Display.setVSyncEnabled(value);
                        }, options -> options.vsync)
                        .setImpact(OptionImpact.VARIES)
                        .build())
                .add(OptionImpl.createBuilder(int.class, VANILLA)
                        .setId(StandardOptions.Option.MAX_FRAMERATE.cast())
                        .setName(vanilla("options.framerateLimit"))
                        .setTooltip(text("framerate_limit.tooltip"))
                        .setControl(option -> new SliderControl(option, 10, 260, 10, ControlValueFormatter.fpsLimit()))
                        .setBinding((options, value) -> options.fpsLimit = value, options -> options.fpsLimit)
                        .build())
                .build();

        OptionGroup indicators = OptionGroup.createBuilder()
                .setId(StandardOptions.Group.INDICATORS)
                .add(OptionImpl.createBuilder(boolean.class, VANILLA)
                        .setId(StandardOptions.Option.VIEW_BOBBING.cast())
                        .setName(vanilla("options.viewBobbing"))
                        .setTooltip(text("view_bobbing.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((options, value) -> options.viewBobbing = value, options -> options.viewBobbing)
                        .build())
                .build();

        return new OptionPage(StandardOptions.Pages.GENERAL, text("pages.general"),
                List.of(rendering, window, indicators));
    }

    static OptionPage quality() {
        OptionGroup graphics = OptionGroup.createBuilder()
                .setId(StandardOptions.Group.GRAPHICS)
                .add(OptionImpl.createBuilder(boolean.class, VANILLA)
                        .setId(StandardOptions.Option.GRAPHICS_MODE.cast())
                        .setName(vanilla("options.graphics"))
                        .setTooltip(text("graphics.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((options, value) -> options.fancyGraphics = value, options -> options.fancyGraphics)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, VANILLA)
                        .setId(OptionIdentifier.create("minecraft", "anaglyph", boolean.class))
                        .setName(vanilla("options.anaglyph"))
                        .setTooltip(text("anaglyph.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((options, value) -> options.anaglyph = value, options -> options.anaglyph)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD, OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build();

        OptionGroup details = OptionGroup.createBuilder()
                .setId(StandardOptions.Group.DETAILS)
                .add(OptionImpl.createBuilder(int.class, VANILLA)
                        .setId(StandardOptions.Option.CLOUDS.cast())
                        .setName(vanilla("options.renderClouds"))
                        .setTooltip(text("clouds.tooltip"))
                        .setControl(option -> new CyclingControl<>(option,
                                new Integer[]{0, 1, 2}, vanillaValues("options.off", "options.clouds.fast", "options.clouds.fancy")))
                        .setBinding((options, value) -> options.cloudRenderMode = value,
                                options -> options.cloudRenderMode)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, VANILLA)
                        .setId(StandardOptions.Option.PARTICLES.cast())
                        .setName(vanilla("options.particles"))
                        .setTooltip(text("particles.tooltip"))
                        .setControl(option -> new CyclingControl<>(option,
                                new Integer[]{0, 1, 2}, vanillaValues("options.particles.all", "options.particles.decreased", "options.particles.minimal")))
                        .setBinding((options, value) -> options.particles = value, options -> options.particles)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(int.class, VANILLA)
                        .setId(StandardOptions.Option.SMOOTH_LIGHT.cast())
                        .setName(vanilla("options.ao"))
                        .setTooltip(text("smooth_lighting.tooltip"))
                        .setControl(option -> new CyclingControl<>(option,
                                new Integer[]{0, 1, 2}, vanillaValues("options.ao.off", "options.ao.min", "options.ao.max")))
                        .setBinding((options, value) -> options.ambientOcclusion = value,
                                options -> options.ambientOcclusion)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, VANILLA)
                        .setId(StandardOptions.Option.MIPMAP_LEVEL.cast())
                        .setName(vanilla("options.mipmapLevels"))
                        .setTooltip(text("mipmap_levels.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.number()))
                        .setBinding((options, value) -> options.mipmapLevels = value, options -> options.mipmapLevels)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .build();

        return new OptionPage(StandardOptions.Pages.QUALITY, text("pages.quality"),
                List.of(graphics, details));
    }

    static OptionPage performance() {
        OptionGroup chunkUpdates = OptionGroup.createBuilder()
                .setId(StandardOptions.Group.CHUNK_UPDATES)
                .add(OptionImpl.createBuilder(int.class, CONFIG_STORAGE)
                        .setId(StandardOptions.Option.CHUNK_UPDATE_THREADS.cast())
                        .setName(text("chunk_builder_threads.name"))
                        .setTooltip(text("chunk_builder_threads.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, ChunkBuilder.getMaxThreadCount(), 1,
                                value -> value == 0 ? text("value.auto") : text("value.threads", value)))
                        .setBinding((config, value) -> config.chunkBuilderThreads = value,
                                config -> config.chunkBuilderThreads)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(toggle("defer_chunk_updates",
                        (config, value) -> config.deferChunkUpdates = value, config -> config.deferChunkUpdates))
                .add(OptionImpl.createBuilder(AsyncOcclusionMode.class, CONFIG_STORAGE)
                        .setId(StandardOptions.Option.ASYNC_GRAPH_SEARCH.cast())
                        .setName(text("async_occlusion.name"))
                        .setTooltip(text("async_occlusion.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, AsyncOcclusionMode.class,
                                values("off", "shadows_only", "everything")))
                        .setBinding((config, value) -> config.asyncOcclusion = value, config -> config.asyncOcclusion)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build();

        OptionGroup culling = OptionGroup.createBuilder()
                .setId(StandardOptions.Group.RENDERING_CULLING)
                .add(toggle("fog_culling",
                        (config, value) -> config.fogCulling = value, config -> config.fogCulling))
                .add(toggle("entity_culling",
                        (config, value) -> config.entityCulling = value, config -> config.entityCulling))
                .add(OptionImpl.createBuilder(int.class, CONFIG_STORAGE)
                        .setId(id("entity_occlusion_interval"))
                        .setName(text("entity_occlusion_interval.name"))
                        .setTooltip(text("entity_occlusion_interval.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 500, 10,
                                value -> text("value.milliseconds", value)))
                        .setBinding((config, value) -> config.entityOcclusionIntervalMs = value,
                                config -> config.entityOcclusionIntervalMs)
                        .setEnabledPredicate(() -> CONFIG.entityCulling)
                        .build())
                .add(toggle("particle_culling",
                        (config, value) -> config.particleCulling = value, config -> config.particleCulling))
                .build();

        OptionGroup rendering = OptionGroup.createBuilder()
                .setId(OptionIdentifier.create("celeritas", "rendering"))
                .add(toggle("entity_instancing",
                        (config, value) -> config.entityInstancing = value, config -> config.entityInstancing))
                .add(toggle("animate_visible_textures",
                        (config, value) -> config.animateOnlyVisibleTextures = value,
                        config -> config.animateOnlyVisibleTextures))
                .add(toggle("translucency_sorting",
                        (config, value) -> config.translucencySorting = value,
                        config -> config.translucencySorting, OptionFlag.REQUIRES_RENDERER_RELOAD))
                .add(toggle("safe_chunk_edges",
                        (config, value) -> config.safeChunkEdges = value, config -> config.safeChunkEdges,
                        OptionFlag.REQUIRES_RENDERER_RELOAD))
                .add(toggle("compact_vertex_format",
                        (config, value) -> config.compactVertexFormat = value, config -> config.compactVertexFormat,
                        OptionFlag.REQUIRES_RENDERER_RELOAD))
                .build();

        return new OptionPage(StandardOptions.Pages.PERFORMANCE, text("pages.performance"),
                List.of(chunkUpdates, culling, rendering));
    }

    static OptionPage advanced() {
        OptionGroup diagnostics = OptionGroup.createBuilder()
                .setId(OptionIdentifier.create("celeritas", "diagnostics"))
                .add(toggle("check_gl_errors",
                        (config, value) -> config.checkGlErrors = value, config -> config.checkGlErrors,
                        OptionFlag.REQUIRES_GAME_RESTART))
                .build();
        return new OptionPage(StandardOptions.Pages.ADVANCED, text("pages.advanced"),
                List.of(diagnostics));
    }

    private static OptionImpl<CeleritasConfig, Boolean> toggle(String id,
            java.util.function.BiConsumer<CeleritasConfig, Boolean> setter,
            java.util.function.Function<CeleritasConfig, Boolean> getter, OptionFlag... flags) {
        return OptionImpl.createBuilder(boolean.class, CONFIG_STORAGE)
                .setId(id(id))
                .setName(text(id + ".name"))
                .setTooltip(text(id + ".tooltip"))
                .setControl(TickBoxControl::new)
                .setBinding(setter, getter)
                .setFlags(flags)
                .build();
    }

    private static <T> OptionIdentifier<T> id(String path) {
        return OptionIdentifier.create("celeritas", path).cast();
    }

    private static TextComponent text(String path, Object... args) {
        return TextComponent.translatable("celeritas.options." + path, args);
    }

    private static TextComponent vanilla(String key, Object... args) {
        return TextComponent.translatable(key, args);
    }

    private static TextComponent[] values(String... names) {
        TextComponent[] components = new TextComponent[names.length];
        for (int i = 0; i < names.length; i++) {
            components[i] = text("value." + names[i]);
        }
        return components;
    }

    private static TextComponent[] vanillaValues(String... keys) {
        TextComponent[] components = new TextComponent[keys.length];
        for (int i = 0; i < keys.length; i++) {
            components[i] = vanilla(keys[i]);
        }
        return components;
    }
}
