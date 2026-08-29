package dev.rdh.cera.mixin;

import net.minecraft.entity.living.mob.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VillagerEntity.class)
public interface VillagerEntityAccessor {
    @Accessor("career")
    int cera$getCareer();

    @Accessor("careerLevel")
    int cera$getCareerLevel();
}
