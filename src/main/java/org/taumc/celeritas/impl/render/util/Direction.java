package org.taumc.celeritas.impl.render.util;

public enum Direction {
    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    private final int stepX, stepY, stepZ;

    public int getStepX() { return stepX; }
    public int getStepY() { return stepY; }
    public int getStepZ() { return stepZ; }

    Direction(int stepX, int stepY, int stepZ) {
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
    }
}
