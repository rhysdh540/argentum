package dev.rdh.cera.ext;

import dev.rdh.cera.modules.colors.CustomColors;
import dev.rdh.cera.modules.DynamicLights;
import dev.rdh.cera.modules.OptifineCosmetics;
import dev.rdh.cera.modules.cit.CustomItems;

public interface CeraMinecraftExtension {
    default DynamicLights.Rules cera$getDynamicLightRules() {
        throw new UnsupportedOperationException();
    }

    default CustomColors cera$getCustomColors() {
        throw new UnsupportedOperationException();
    }

    default CustomItems cera$getCustomItems() {
        throw new UnsupportedOperationException();
    }

    default OptifineCosmetics cera$getOptifineCosmetics() {
        throw new UnsupportedOperationException();
    }
}
