package dev.rdh.cera.mixin;

import dev.rdh.cera.entity.SpawnSnapshot;
import dev.rdh.cera.ext.CeraClientWorldExtension;
import dev.rdh.cera.modules.DynamicLights;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.Difficulty;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;

@Mixin(ClientWorld.class)
public class ClientWorldMixin implements CeraClientWorldExtension {
    @Unique
    private final DynamicLights cera$dynamicLights = new DynamicLights(Minecraft.getInstance().cera$getDynamicLightRules());
    @Unique
    private final Map<UUID, SpawnSnapshot> cera$spawnSnapshots = new Object2ObjectOpenHashMap<>();

    @Override
    public DynamicLights cera$getDynamicLights() {
        return this.cera$dynamicLights;
    }

    @Override
    public Map<UUID, SpawnSnapshot> cera$getSpawnSnapshots() {
        return this.cera$spawnSnapshots;
    }

    /** The loading screen draws before there is a renderable world, so remember what we are loading into. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void cera$recordDimension(ClientPlayNetworkHandler networkHandler, WorldSettings settings, int dimension,
            Difficulty difficulty, Profiler profiler, CallbackInfo ci) {
        Minecraft.getInstance().cera$getCustomLoadingScreens().setDimension(dimension);
    }

    @Inject(method = "addEntity", at = @At("TAIL"))
    private void cera$snapshotSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        BlockPos pos = new BlockPos(entity.x, entity.y, entity.z);
        this.cera$spawnSnapshots.put(entity.getUuid(), new SpawnSnapshot(pos, ((ClientWorld) (Object) this).getBiome(pos)));
    }

    @Inject(method = "removeEntity(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
    private void cera$dropSnapshot(Entity entity, CallbackInfo ci) {
        this.cera$spawnSnapshots.remove(entity.getUuid());
    }
}
