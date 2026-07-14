package org.taumc.celeritas.impl.debug;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class RenderMetrics {
    private static final long SAMPLE_NANOS = 1_000_000_000L;
    private static final long[] frameDraws = new long[Category.values().length];
    private static final LongAdder chunkBuildCount = new LongAdder();
    private static final LongAdder chunkBuildNanos = new LongAdder();
    private static final AtomicLong longestChunkBuild = new AtomicLong();

    private static Category category = Category.OTHER;
    private static long sampleStarted = System.nanoTime();
    private static int sampledFrames;
    private static long renderedEntities;
    private static long culledEntities;
    private static long renderedBlockEntities;
    private static long renderedParticles;
    private static long culledParticles;
    private static long fontBatches;
    private static boolean frameStarted;
    private static List<String> debugStrings = format(new double[Category.values().length],
            0, 0, 0, 0, 0, 0, 0, 0, 0);

    private RenderMetrics() {
    }

    public static void beginFrame() {
        category = Category.OTHER;
        if (!frameStarted) {
            frameStarted = true;
            return;
        }

        sampledFrames++;
        long now = System.nanoTime();
        if (now - sampleStarted >= SAMPLE_NANOS) {
            publish(now);
        }
    }

    public static Category setCategory(Category next) {
        Category previous = category;
        category = next;
        return previous;
    }

    public static void recordDraw() {
        frameDraws[category.ordinal()]++;
    }

    public static void recordTerrainDraws(int count) {
        frameDraws[Category.TERRAIN.ordinal()] += count;
    }

    public static void recordRenderedEntity() {
        renderedEntities++;
    }

    public static void recordCulledEntity() {
        culledEntities++;
    }

    public static void recordRenderedBlockEntity() {
        renderedBlockEntities++;
    }

    public static void recordRenderedParticle() {
        renderedParticles++;
    }

    public static void recordCulledParticle() {
        culledParticles++;
    }

    public static void recordFontBatch() {
        fontBatches++;
    }

    public static void recordChunkBuild(long nanos) {
        chunkBuildCount.increment();
        chunkBuildNanos.add(nanos);

        long longest = longestChunkBuild.get();
        while (nanos > longest && !longestChunkBuild.compareAndSet(longest, nanos)) {
            longest = longestChunkBuild.get();
        }
    }

    public static List<String> getDebugStrings() {
        return debugStrings;
    }

    private static void publish(long now) {
        int frames = Math.max(1, sampledFrames);
        double[] drawAverages = new double[frameDraws.length];
        for (int i = 0; i < frameDraws.length; i++) {
            drawAverages[i] = (double)frameDraws[i] / frames;
            frameDraws[i] = 0;
        }

        long builds = chunkBuildCount.sumThenReset();
        long buildNanos = chunkBuildNanos.sumThenReset();
        long elapsed = now - sampleStarted;
        debugStrings = format(drawAverages,
                (double)renderedEntities / frames, (double)culledEntities / frames,
                (double)renderedBlockEntities / frames, (double)renderedParticles / frames,
                (double)culledParticles / frames, (double)fontBatches / frames,
                builds * 1_000_000_000.0D / elapsed,
                builds == 0 ? 0.0D : buildNanos / (builds * 1_000_000.0D),
                longestChunkBuild.getAndSet(0) / 1_000_000.0D);

        sampledFrames = 0;
        renderedEntities = 0;
        culledEntities = 0;
        renderedBlockEntities = 0;
        renderedParticles = 0;
        culledParticles = 0;
        fontBatches = 0;
        sampleStarted = now;
    }

    private static List<String> format(double[] draws, double renderedEntities, double culledEntities,
            double renderedBlockEntities, double renderedParticles, double culledParticles, double fontBatches,
            double chunkBuildsPerSecond, double averageChunkBuildMillis, double longestChunkBuildMillis) {
        return List.of(
                "Tracked draws/frame: T %.1f | E %.1f | BE %.1f | P %.1f | Txt %.1f | HUD %.1f | O %.1f".formatted(
                        draws[Category.TERRAIN.ordinal()], draws[Category.ENTITY.ordinal()],
                        draws[Category.BLOCK_ENTITY.ordinal()], draws[Category.PARTICLE.ordinal()],
                        draws[Category.TEXT.ordinal()], draws[Category.HUD.ordinal()], draws[Category.OTHER.ordinal()]),
                "Objects/frame: E %.1f rendered / %.1f culled | BE %.1f | P %.1f / %.1f | Font %.1f batches".formatted(
                        renderedEntities, culledEntities, renderedBlockEntities, renderedParticles, culledParticles,
                        fontBatches),
                "Chunk builds: %.1f/s | %.2f ms avg | %.2f ms max".formatted(
                        chunkBuildsPerSecond, averageChunkBuildMillis, longestChunkBuildMillis)
        );
    }

    public enum Category {
        TERRAIN,
        ENTITY,
        BLOCK_ENTITY,
        PARTICLE,
        TEXT,
        HUD,
        OTHER
    }
}
