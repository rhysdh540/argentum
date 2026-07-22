package dev.rdh.argentum.extras;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.OptionIdentifier;
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

    static OptionPage create() {
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
                .add(OptionImpl.createBuilder(int.class, ArgentumExtras.CONFIG_STORAGE)
                        .setId(id("debug_hud_refresh_interval"))
                        .setControl(option -> new SliderControl(option, 0, 1000, 50,
                                value -> value == 0 ? text("value.every_frame") : text("value.milliseconds", value)))
                        .setBinding((config, value) -> config.debugHudRefreshIntervalMs = value,
                                config -> config.debugHudRefreshIntervalMs)
                        .setEnabledPredicate(() -> ArgentumExtras.CONFIG.steadyDebugHud)
                        .build())
                .build();

        return new OptionPage(id("options"), text("pages.extras"), List.of(particles, debug));
    }

    private static OptionImpl<ArgentumExtrasConfig, Boolean> toggle(String name,
            BiConsumer<ArgentumExtrasConfig, Boolean> setter, Function<ArgentumExtrasConfig, Boolean> getter) {
        return OptionImpl.createBuilder(boolean.class, ArgentumExtras.CONFIG_STORAGE)
                .setId(id(name))
                .setControl(TickBoxControl::new)
                .setBinding(setter, getter)
                .build();
    }

    private static <T> OptionIdentifier<T> id(String path) {
        return OptionIdentifier.create(MOD_ID, path).cast();
    }

    private static TextComponent text(String path, Object... args) {
        return TextComponent.translatable(MOD_ID + ".options." + path, args);
    }
}
