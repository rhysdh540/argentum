package dev.rdh.argentum.impl.debug;

import it.unimi.dsi.fastutil.Pair;
import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.common.util.NativeBuffer;
import dev.rdh.argentum.impl.Argentum;
import dev.rdh.argentum.impl.render.entity.instancing.EntityInstancingRenderer;
import dev.rdh.argentum.impl.render.terrain.ArgentumWorldRenderer;

import net.minecraft.text.Formatting;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class DebugStrings {
    public static List<Pair<String, Formatting>> getStringsToRender() {
        List<Pair<String, Formatting>> stringsToRender = new ArrayList<>();

        stringsToRender.add(Pair.of(getNativeMemoryString(), Formatting.RESET));
        stringsToRender.add(Pair.of("", Formatting.RESET));

        stringsToRender.add(Pair.of("%s Renderer (%s)".formatted("Argentum", Argentum.VERSION), Formatting.GREEN));
        stringsToRender.add(Pair.of("", Formatting.RESET));

        var renderer = ArgentumWorldRenderer.instanceNullable();

        if (renderer != null) {
            RenderMetrics.getDebugStrings().forEach(str -> stringsToRender.add(Pair.of(str, Formatting.RESET)));
            stringsToRender.add(Pair.of(EntityInstancingRenderer.getDebugString(), Formatting.RESET));
            stringsToRender.add(Pair.of("", Formatting.RESET));
            renderer.getDebugStrings().forEach(str -> stringsToRender.add(Pair.of(str, Formatting.RESET)));
        }

        return stringsToRender;
    }

    private static String getNativeMemoryString() {
        return "Off-Heap: +" + MathUtil.toMib(getNativeMemoryUsage()) + "MB";
    }

    private static long getNativeMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() + NativeBuffer.getTotalAllocated();
    }
}
