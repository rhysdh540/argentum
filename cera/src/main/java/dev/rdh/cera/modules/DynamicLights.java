package dev.rdh.cera.modules;

import dev.rdh.argentum.impl.render.terrain.ArgentumWorldRenderer;
import dev.rdh.cera.Cera;

import net.minecraft.block.material.Material;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.mob.monster.BlazeEntity;
import net.minecraft.entity.living.mob.monster.CreeperEntity;
import net.minecraft.entity.living.mob.monster.MagmaCubeEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Identifier;
import net.minecraft.util.math.BlockPos;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.ornithemc.osl.resource.loader.api.resource.Resource;
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class DynamicLights {
    private static final String CONFIG = "optifine/dynamic_lights.properties";
    private static final double MAX_DISTANCE = 7.5;
    private static final Light[] NO_LIGHTS = new Light[0];
    private static final Int2ObjectMap<Light> tracked = new Int2ObjectOpenHashMap<>();
    private static volatile Rules rules = Rules.EMPTY;
    private static volatile Light[] lights = NO_LIGHTS;
    private static ClientWorld world;
    private static int ticks;

    private DynamicLights() {
    }

    public static void reload(ResourceManager resources) {
        Map<String, Integer> entities = new HashMap<>();
        Map<Item, Integer> items = new HashMap<>();

        resources.findResources("optifine", id -> CONFIG.equals(id.identifier())).forEach((id, resource) ->
                load(resource, id.namespace(), entities, items));

        rules = new Rules(Map.copyOf(entities), Map.copyOf(items));
        Cera.LOGGER.info("Loaded {} dynamic light entity rules and {} item rules", entities.size(), items.size());
    }

    public static void update(ClientWorld world, ArgentumWorldRenderer renderer) {
        Mode mode = Cera.CONFIG.dynamicLights;
        if (mode == Mode.OFF) {
            clear();
            return;
        }
        if (DynamicLights.world != world) {
            clear();
            DynamicLights.world = world;
        }
        if (mode == Mode.FAST && ticks++ % 10 != 0) return;

        Set<Integer> seen = new HashSet<>();
        for (Entity entity : world.entities) {
            int level = getLightLevel(entity);
            if (level <= 0) continue;

            int id = entity.getNetworkId();
            seen.add(id);
            double x = entity.x - 0.5;
            double y = entity.y - 0.5 + entity.getEyeHeight();
            double z = entity.z - 0.5;
            boolean underwater = world.getBlockState(new BlockPos(x, y, z)).getBlock().getMaterial() == Material.WATER;
            Light previous = tracked.get(id);
            if (previous == null || previous.changed(x, y, z, level, underwater)) {
                if (previous != null) dirty(renderer, previous.x, previous.y, previous.z);
                dirty(renderer, x, y, z);
                tracked.put(id, new Light(x, y, z, level, underwater));
            }
        }

        tracked.int2ObjectEntrySet().removeIf(entry -> {
            if (seen.contains(entry.getIntKey())) return false;
            Light light = entry.getValue();
            dirty(renderer, light.x, light.y, light.z);
            return true;
        });
        lights = tracked.values().toArray(Light[]::new);
    }

    public static int combine(int x, int y, int z, int packedLight) {
        if (Cera.CONFIG.dynamicLights == Mode.OFF) return packedLight;

        double maximum = 0.0;
        for (Light light : lights) {
            double dx = x - light.x;
            double dy = y - light.y;
            double dz = z - light.z;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            int level = light.level;
            if (light.underwater) {
                level = Math.max(0, level - 2);
                distanceSquared *= 2.0;
            }
            if (distanceSquared > MAX_DISTANCE * MAX_DISTANCE) continue;
            maximum = Math.max(maximum, (1.0 - Math.sqrt(distanceSquared) / MAX_DISTANCE) * level);
        }

        int dynamic = (int)(maximum * 16.0);
        return dynamic > (packedLight & 0xFF) ? packedLight & ~0xFF | dynamic : packedLight;
    }

    public static int combine(Entity entity, int packedLight) {
        if (Cera.CONFIG.dynamicLights == Mode.OFF) return packedLight;
        int dynamic = getLightLevel(entity) << 4;
        return dynamic > (packedLight & 0xFF) ? packedLight & ~0xFF | dynamic : packedLight;
    }

    private static int getLightLevel(Entity entity) {
        if (entity instanceof PlayerEntity player && player.isSpectator()) return 0;
        if (entity.isOnFire()) return 15;

        int level = rules.entity(entity);
        if (entity instanceof BlazeEntity blaze && blaze.isFireActive()) level = Math.max(level, 15);
        if (entity instanceof MagmaCubeEntity magma && magma.targetStretch > 0.6F) level = Math.max(level, 13);
        if (entity instanceof CreeperEntity creeper && creeper.getFuse(0.0F) > 0.001F) level = Math.max(level, 15);
        if (entity instanceof LivingEntity living) {
            level = Math.max(level, getLightLevel(living.getDisplayItemInHand()));
            level = Math.max(level, getLightLevel(living.getEquipment(4)));
        }
        if (entity instanceof ItemEntity item) level = Math.max(level, getLightLevel(item.getItem()));
        return level;
    }

    private static int getLightLevel(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        if (stack.getItem() instanceof BlockItem blockItem) return blockItem.getBlock().getLight();
        return rules.items.getOrDefault(stack.getItem(), 0);
    }

    private static void load(Resource resource, String namespace, Map<String, Integer> entities, Map<Item, Integer> items) {
        try (resource) {
            Properties properties = new Properties();
            try (var stream = resource.open()) {
                properties.load(stream);
            }
            parse(properties.getProperty("entities"), namespace, (name, level) ->
                    entities.put("minecraft".equals(namespace) ? name : namespace + ":" + name, level));
            parse(properties.getProperty("items"), namespace, (name, level) -> {
                Item item = Item.REGISTRY.get(new Identifier(namespace, name));
                if (item == null) {
                    Cera.LOGGER.warn("Unknown dynamic light item: {}:{}", namespace, name);
                } else {
                    items.put(item, level);
                }
            });
        } catch (IOException e) {
            Cera.LOGGER.warn("Failed to load {} dynamic lights", namespace, e);
        }
    }

    private static void parse(String value, String namespace, RuleConsumer consumer) {
        if (value == null) return;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return;
        for (String entry : trimmed.split("\\s+")) {
            int separator = entry.lastIndexOf(':');
            if (separator <= 0) {
                Cera.LOGGER.warn("Invalid dynamic light rule in {}: {}", namespace, entry);
                continue;
            }
            try {
                int level = Integer.parseInt(entry.substring(separator + 1));
                if (level < 0 || level > 15) throw new NumberFormatException();
                consumer.accept(entry.substring(0, separator), level);
            } catch (NumberFormatException e) {
                Cera.LOGGER.warn("Invalid dynamic light level in {}: {}", namespace, entry);
            }
        }
    }

    private static void dirty(ArgentumWorldRenderer renderer, double x, double y, double z) {
        renderer.scheduleRebuildForBlockArea(
                (int)Math.floor(x - MAX_DISTANCE), (int)Math.floor(y - MAX_DISTANCE), (int)Math.floor(z - MAX_DISTANCE),
                (int)Math.floor(x + MAX_DISTANCE), (int)Math.floor(y + MAX_DISTANCE), (int)Math.floor(z + MAX_DISTANCE),
                false);
    }

    private static void clear() {
        tracked.clear();
        lights = NO_LIGHTS;
        world = null;
        ticks = 0;
    }

    private record Rules(Map<String, Integer> entities, Map<Item, Integer> items) {
        private static final Rules EMPTY = new Rules(Map.of(), Map.of());

        private int entity(Entity entity) {
            String key = Entities.getKey(entity);
            return key == null ? 0 : entities.getOrDefault(key, 0);
        }
    }

    private record Light(double x, double y, double z, int level, boolean underwater) {
        private boolean changed(double x, double y, double z, int level, boolean underwater) {
            return Math.abs(this.x - x) > 0.1 || Math.abs(this.y - y) > 0.1 || Math.abs(this.z - z) > 0.1
                    || this.level != level || this.underwater != underwater;
        }
    }

    @FunctionalInterface
    private interface RuleConsumer {
        void accept(String name, int level);
    }

    public enum Mode {
        OFF,
        FAST,
        FANCY;

        public String key() {
            return "value.dynamic_lights." + name().toLowerCase();
        }
    }
}
