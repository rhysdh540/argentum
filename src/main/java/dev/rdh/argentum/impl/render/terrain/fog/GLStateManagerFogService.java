package dev.rdh.argentum.impl.render.terrain.fog;

import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkFogMode;
import org.lwjgl.opengl.GL11;

public class GLStateManagerFogService implements FogService {
    public static float fogColorRed, fogColorGreen, fogColorBlue;
    public static boolean fogEnabled;
    public static int fogMode = GL11.GL_EXP;
    public static float fogDensity = 1.0F;
    public static float fogStart;
    public static float fogEnd = 1.0F;

    @Override
    public float getFogEnd() {
        return fogEnd;
    }

    @Override
    public float getFogStart() {
        return fogStart;
    }

    @Override
    public float getFogDensity() {
        return fogDensity;
    }

    @Override
    public int getFogShapeIndex() {
        return 0;
    }

    @Override
    public float getFogCutoff() {
        return getFogEnd();
    }

    @Override
    public float[] getFogColor() {
        return new float[]{fogColorRed, fogColorGreen, fogColorBlue, 1.0F};
    }

    @Override
    public ChunkFogMode getFogMode() {
        if (!fogEnabled) {
            return ChunkFogMode.NONE;
        }
        return ChunkFogMode.fromGLMode(fogMode);
    }
}
