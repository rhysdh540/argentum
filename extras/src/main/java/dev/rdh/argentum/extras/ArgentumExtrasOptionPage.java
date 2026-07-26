package dev.rdh.argentum.extras;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpl;
import org.taumc.celeritas.api.options.structure.OptionPage;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class ArgentumExtrasOptionPage {
    private static final String MOD_ID = "argentum-extras";

    private ArgentumExtrasOptionPage() {
    }

    static List<OptionPage> create() {
        OptionGroup particles = OptionGroup.createBuilder()
                .setId(id("particle_controls"))
                .add(toggle("explosion_particles", (c, v) -> c.explosionParticles = v, c -> c.explosionParticles))
                .add(toggle("spell_particles", (c, v) -> c.spellParticles = v, c -> c.spellParticles))
                .add(toggle("portal_particles", (c, v) -> c.portalParticles = v, c -> c.portalParticles))
                .add(toggle("smoke_and_flame_particles", (c, v) -> c.smokeAndFlameParticles = v,
                        c -> c.smokeAndFlameParticles))
                .add(toggle("redstone_particles", (c, v) -> c.redstoneParticles = v, c -> c.redstoneParticles))
                .add(toggle("water_particles", (c, v) -> c.waterParticles = v, c -> c.waterParticles))
                .add(toggle("miscellaneous_particles", (c, v) -> c.miscellaneousParticles = v,
                        c -> c.miscellaneousParticles))
                .build();

        OptionGroup debug = OptionGroup.createBuilder()
                .setId(id("debug_hud"))
                .add(toggle("steady_debug_hud", (c, v) -> c.steadyDebugHud = v, c -> c.steadyDebugHud))
                .add(slider("debug_hud_refresh_interval", 0, 1000, 50,
                        (c, v) -> c.debugHudRefreshIntervalMs = v, c -> c.debugHudRefreshIntervalMs,
                        value -> value == 0 ? text("value.every_frame") : text("value.milliseconds", value))
                        .setEnabledPredicate(() -> ArgentumExtras.CONFIG.steadyDebugHud)
                        .build())
                .add(slider("debug_hud_scale", 0, 4, 1,
                        (c, v) -> c.debugHudScale = v, c -> c.debugHudScale,
                        value -> value == 0 ? text("value.gui_scale") : text("value.scale", value)).build())
                .add(toggle("fps_hud", (c, v) -> c.fpsHud = v, c -> c.fpsHud))
                .build();

        OptionGroup camera = OptionGroup.createBuilder()
                .setId(id("camera_effects"))
                .add(percentSlider("dynamic_fov_strength",
                        (c, v) -> c.dynamicFovStrength = v, c -> c.dynamicFovStrength))
                .add(percentSlider("portal_distortion_strength",
                        (c, v) -> c.portalDistortionStrength = v, c -> c.portalDistortionStrength))
                .add(percentSlider("view_bobbing_strength",
                        (c, v) -> c.viewBobbingStrength = v, c -> c.viewBobbingStrength))
                .add(percentSlider("hurt_camera_strength",
                        (c, v) -> c.hurtCameraStrength = v, c -> c.hurtCameraStrength))
                .build();

        OptionGroup environment = OptionGroup.createBuilder()
                .setId(id("environment"))
                .add(slider("cloud_render_distance", 0, 1536, 96,
                        (c, v) -> c.cloudRenderDistance = v, c -> c.cloudRenderDistance,
                        value -> value == 0 ? text("value.vanilla") : text("value.blocks", value)).build())
                .add(slider("cloud_height_offset", -128, 128, 4,
                        (c, v) -> c.cloudHeightOffset = v, c -> c.cloudHeightOffset,
                        value -> value == 0 ? text("value.vanilla") : text("value.signed_blocks", value)).build())
                .add(slider("cloud_speed", 0, 200, 5,
                        (c, v) -> c.cloudSpeed = v, c -> c.cloudSpeed,
                        ControlValueFormatter.percentage()).build())
                .add(toggle("cloud_fog", (c, v) -> c.cloudFog = v, c -> c.cloudFog))
                .add(percentSlider("terrain_fog_density",
                        (c, v) -> c.terrainFogDensity = v, c -> c.terrainFogDensity))
                .add(percentSlider("fluid_fog_density",
                        (c, v) -> c.fluidFogDensity = v, c -> c.fluidFogDensity))
                .add(slider("weather_render_distance", 0, 15, 1,
                        (c, v) -> c.weatherRenderDistance = v, c -> c.weatherRenderDistance,
                        value -> value == 0 ? text("value.vanilla") : text("value.blocks", value)).build())
                .add(percentSlider("weather_density",
                        (c, v) -> c.weatherDensity = v, c -> c.weatherDensity))
                .build();

        return List.of(
                new OptionPage(id("particles"), text("pages.particles"), List.of(particles)),
                new OptionPage(id("debug_hud"), text("pages.debug_hud"), List.of(debug)),
                new OptionPage(id("camera_effects"), text("pages.camera_effects"), List.of(camera)),
                new OptionPage(id("environment"), text("pages.environment"), List.of(environment)));
    }

    private static OptionImpl<ArgentumExtrasConfig, Boolean> toggle(String name,
            BiConsumer<ArgentumExtrasConfig, Boolean> setter, Function<ArgentumExtrasConfig, Boolean> getter) {
        return OptionImpl.createBuilder(boolean.class, ArgentumExtras.CONFIG_STORAGE)
                .setId(id(name))
                .setControl(TickBoxControl::new)
                .setBinding(setter, getter)
                .build();
    }

    private static OptionImpl<ArgentumExtrasConfig, Integer> percentSlider(String name,
            BiConsumer<ArgentumExtrasConfig, Integer> setter, Function<ArgentumExtrasConfig, Integer> getter) {
        return slider(name, 0, 100, 5, setter, getter, ControlValueFormatter.percentage()).build();
    }

    private static OptionImpl.Builder<ArgentumExtrasConfig, Integer> slider(String name, int min, int max, int step,
            BiConsumer<ArgentumExtrasConfig, Integer> setter, Function<ArgentumExtrasConfig, Integer> getter,
            ControlValueFormatter formatter) {
        return OptionImpl.createBuilder(int.class, ArgentumExtras.CONFIG_STORAGE)
                .setId(id(name))
                .setControl(option -> new SliderControl(option, min, max, step, formatter))
                .setBinding(setter, getter);
    }

    private static <T> OptionIdentifier<T> id(String path) {
        return OptionIdentifier.create(MOD_ID, path).cast();
    }

    private static TextComponent text(String path, Object... args) {
        return TextComponent.translatable(MOD_ID + ".options." + path, args);
    }
}
