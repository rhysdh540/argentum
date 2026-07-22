package org.taumc.celeritas.impl.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.options.GameOptions;
import org.taumc.celeritas.api.options.structure.OptionStorage;

final class GameOptionsStorage implements OptionStorage<GameOptions> {
    @Override
    public GameOptions getData() {
        return Minecraft.getInstance().options;
    }

    @Override
    public void save() {
        this.getData().save();
    }
}
