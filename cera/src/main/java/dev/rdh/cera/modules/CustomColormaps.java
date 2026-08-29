package dev.rdh.cera.modules;

import dev.rdh.cera.Cera;
import dev.rdh.cera.props.BlockMatcher;
import dev.rdh.cera.props.Props;
import dev.rdh.cera.modules.CustomColormaps.Colormap.Format;
import dev.rdh.argentum.impl.world.cloned.ChunkRenderContext;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PlanksBlock;
import net.minecraft.block.state.BlockState;
import net.minecraft.resource.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.ornithemc.osl.resource.loader.api.resource.Resource;
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;
import net.ornithemc.osl.resource.loader.api.resource.reload.ResourceReloadListener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CustomColormaps implements ResourceReloadListener {
    private static final String[] DIRECTORIES = {"optifine/colormap/", "mcpatcher/colormap/"};

    private volatile State state = State.EMPTY;

    @Override
    public void resourcesReloaded(ResourceManager resources) {
        this.state = load(resources);
    }

    public int getColor(BlockState state, ChunkRenderContext world, BlockPos pos) {
        if (!Cera.CONFIG.customColors) return -1;
        State s = this.state;
        Block block = state.getBlock();

        Colormap[] matched = s.blockColormaps.get(block);
        if (matched != null) {
            for (Colormap colormap : matched) {
                if (colormap.matches(state)) return colormap.getColor(world.getBiome(pos), pos);
            }
        }

        if (block == Blocks.REDSTONE_WIRE) {
            return s.redstone == null ? -1 : s.redstone.getColorByIndex(block.getMetadataFromState(state));
        }
        if (block == Blocks.PUMPKIN_STEM || block == Blocks.MELON_STEM) {
            Colormap stem;
            if (s.pumpkinStem != null && block == Blocks.PUMPKIN_STEM) {
                stem = s.pumpkinStem;
            } else if (s.melonStem != null && block == Blocks.MELON_STEM) {
                stem = s.melonStem;
            } else if (s.stem != null) {
                stem = s.stem;
            } else {
                return -1;
            }
            return stem.getColorByIndex(block.getMetadataFromState(state));
        }
        if (block == Blocks.LEAVES) {
            PlanksBlock.Variant variant = state.get(LeavesBlock.VARIANT);
            if (variant == PlanksBlock.Variant.SPRUCE && s.pine != null) return s.pine.getColor(world.getBiome(pos), pos);
            if (variant == PlanksBlock.Variant.BIRCH && s.birch != null) return s.birch.getColor(world.getBiome(pos), pos);
        }
        if ((block == Blocks.WATER || block == Blocks.FLOWING_WATER) && s.water != null) {
            return s.water.getColor(world.getBiome(pos), pos);
        }
        if (s.swampGrass != null || s.swampFoliage != null) {
            Biome biome = world.getBiome(pos);
            if (biome == Biome.SWAMPLAND) {
                if (s.swampGrass != null && isGrass(block)) return s.swampGrass.getColor(biome, pos);
                if (s.swampFoliage != null && isFoliage(block)) return s.swampFoliage.getColor(biome, pos);
            }
        }
        return -1;
    }

    public boolean appliesBiomeTint(BlockState state) {
        if (!Cera.CONFIG.customColors) return false;
        State s = this.state;
        Block block = state.getBlock();

        Colormap[] matched = s.blockColormaps.get(block);
        if (matched != null) {
            for (Colormap colormap : matched) {
                if (colormap.matches(state)) return colormap.format() != Colormap.Format.FIXED;
            }
        }
        if (block == Blocks.LEAVES) {
            PlanksBlock.Variant variant = state.get(LeavesBlock.VARIANT);
            if (variant == PlanksBlock.Variant.SPRUCE) return s.pine != null;
            if (variant == PlanksBlock.Variant.BIRCH) return s.birch != null;
        }
        return (block == Blocks.WATER || block == Blocks.FLOWING_WATER) && s.water != null;
    }

    private static boolean isGrass(Block block) {
        return block == Blocks.GRASS || block == Blocks.TALLGRASS || block == Blocks.REEDS;
    }

    private static boolean isFoliage(Block block) {
        return block == Blocks.LEAVES || block == Blocks.LEAVES2 || block == Blocks.VINE;
    }

    private static State load(ResourceManager resources) {
        Map<Block, List<Colormap>> byBlock = new Object2ObjectOpenHashMap<>();
        for (String directory : DIRECTORIES) {
            resources.findResources("minecraft", directory, id -> id.identifier().endsWith(".properties"))
                    .values().forEach(resource -> loadCustom(resource, resources, byBlock));
        }
        Map<Block, Colormap[]> blockColormaps = new Object2ObjectOpenHashMap<>();
        byBlock.forEach((block, list) -> blockColormaps.put(block, list.toArray(Colormap[]::new)));

        State s = new State(
                Map.copyOf(blockColormaps),
                loadNamed(resources, Format.VANILLA, "water.png", "watercolorX.png"),
                loadNamed(resources, Format.VANILLA, "pine.png", "pinecolor.png"),
                loadNamed(resources, Format.VANILLA, "birch.png", "birchcolor.png"),
                loadNamed(resources, Format.VANILLA, "swampgrass.png", "swampgrasscolor.png"),
                loadNamed(resources, Format.VANILLA, "swampfoliage.png", "swampfoliagecolor.png"),
                loadNamed(resources, Format.VANILLA, "redstone.png", "redstonecolor.png"),
                loadNamed(resources, Format.VANILLA, "stem.png", "stemcolor.png"),
                loadNamed(resources, Format.VANILLA, "pumpkinstem.png"),
                loadNamed(resources, Format.VANILLA, "melonstem.png")
        );

        StringBuilder sb = new StringBuilder("[CustomColormaps] loaded {} block maps");
        if (s.water != null) sb.append("/water");
        if (s.pine != null) sb.append("/pine");
        if (s.birch != null) sb.append("/birch");
        if (s.swampGrass != null) sb.append("/swampGrass");
        if (s.swampFoliage != null) sb.append("/swampFoliage");
        if (s.redstone != null) sb.append("/redstone");
        if (s.stem != null) sb.append("/stem");
        if (s.pumpkinStem != null) sb.append("/pumpkinStem");
        if (s.melonStem != null) sb.append("/melonStem");
        Cera.LOGGER.info(sb.toString(), s.blockColormaps.size());

        return s;
    }

    private static void loadCustom(Resource resource, ResourceManager resources, Map<Block, List<Colormap>> byBlock) {
        try {
            Props props = new Props(resource);
            Object2IntMap<Block> blocks = BlockMatcher.parseBlocks(props.get("blocks"));
            if (blocks.isEmpty()) return;

            Format format = parseFormat(props.get("format"));
            Colormap colormap;
            if (format == Format.FIXED) {
                colormap = new Colormap(format, null, 0, 0, 0, 0, props.getColor("color").orElse(0xFFFFFF), blocks);
            } else {
                Identifier source = props.get("source") != null
                        ? withPng(props.parseId(stripPng(props.get("source"))))
                        : withPng(new Identifier(props.id().namespace(), stripPng(props.id().identifier())));
                Image image = readImage(resources, source);
                if (image == null) {
                    Cera.LOGGER.warn("[CustomColors] Source not found for {}", props.id());
                    return;
                }
                colormap = new Colormap(format, image.pixels(), image.width(), image.height(),
                        props.getInt("yVariance", 0).orElse(0), props.getInt("yOffset", 0).orElse(0), 0, blocks);
            }
            for (Block block : blocks.keySet()) {
                byBlock.computeIfAbsent(block, _ -> new ArrayList<>()).add(colormap);
            }
        } catch (IOException e) {
            Cera.LOGGER.warn("[CustomColors] Failed to read {}", resource.location(), e);
        }
    }

    private static Colormap loadNamed(ResourceManager resources, Format format, String... names) {
        for (String directory : DIRECTORIES) {
            for (String name : names) {
                Image image = readImage(resources, new Identifier(directory + name));
                if (image != null) {
                    return new Colormap(format, image.pixels(), image.width(), image.height(), 0, 0, 0, null);
                }
            }
        }
        return null;
    }

    private static Format parseFormat(String value) {
        if (value == null) return Format.VANILLA;
        return switch (value.trim()) {
            case "grid" -> Format.GRID;
            case "fixed" -> Format.FIXED;
            default -> Format.VANILLA;
        };
    }

    private static String stripPng(String path) {
        return path.endsWith(".png") ? path.substring(0, path.length() - 4) : path;
    }

    private static Identifier withPng(Identifier id) {
        return new Identifier(id.getNamespace(), id.getPath() + ".png");
    }

    private static Image readImage(ResourceManager resources, Identifier id) {
        Resource resource = resources.getResource(id).orElse(null);
        if (resource == null) return null;
        try (resource; InputStream in = resource.open()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) return null;
            int width = image.getWidth();
            int height = image.getHeight();
            return new Image(image.getRGB(0, 0, width, height, null, 0, width), width, height);
        } catch (IOException e) {
            return null;
        }
    }

    private record Image(int[] pixels, int width, int height) {
    }

    private record State(
            Map<Block, Colormap[]> blockColormaps,
            Colormap water,
            Colormap pine, Colormap birch,
            Colormap swampGrass, Colormap swampFoliage,
            Colormap redstone,
            Colormap stem, Colormap pumpkinStem, Colormap melonStem
    ) {
        static final State EMPTY = new State(Map.of(), null, null, null, null, null, null, null, null, null);
    }

    record Colormap(
            Format format, int[] colors,
            int width, int height,
            int yVariance, int yOffset,
            int fixedColor,
            Object2IntMap<Block> blocks
    ) {
        enum Format { VANILLA, GRID, FIXED }

        boolean matches(BlockState state) {
            return BlockMatcher.matches(this.blocks, state);
        }

        int getColor(Biome biome, BlockPos pos) {
            return switch (this.format) {
                case FIXED -> this.fixedColor;
                case VANILLA -> {
                    float temperature = Math.clamp(biome.getTemperature(pos), 0.0F, 1.0F);
                    float rainfall = Math.clamp(biome.getDownfall(), 0.0F, 1.0F) * temperature;
                    int cx = (int) ((1.0F - temperature) * (this.width - 1));
                    int cy = (int) ((1.0F - rainfall) * (this.height - 1));
                    yield sample(cx, cy);
                }
                case GRID -> {
                    int cy = pos.getY() - this.yOffset;
                    if (this.yVariance > 0) {
                        int hash = Cera.intHash(pos.getX() * 31 + pos.getZ()) & 0xFF;
                        cy += hash % (this.yVariance * 2 + 1) - this.yVariance;
                    }
                    yield sample(biome.id, cy);
                }
            };
        }

        int getColorByIndex(int index) {
            return this.colors[Math.clamp(index, 0, this.colors.length - 1)] & 0xFFFFFF;
        }

        private int sample(int cx, int cy) {
            cx = Math.clamp(cx, 0, this.width - 1);
            cy = Math.clamp(cy, 0, this.height - 1);
            return this.colors[cy * this.width + cx] & 0xFFFFFF;
        }

    }
}
