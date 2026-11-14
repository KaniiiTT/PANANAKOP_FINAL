package background;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Works out where the real ground surface is from the level's tile data.
 *
 * Nothing here assumes a fixed row. The rules are:
 *
 *  1. Border: in each column, the run of solid tiles that starts at the very
 *     top row (ceiling band, edge walls) is decorative framing, not ground.
 *  2. Ground mass: solid, non-border tiles that are connected (4-neighbour)
 *     to the bottom row. Floating platforms never touch it, so they cannot
 *     be mistaken for the ground line.
 *  3. Surface: in each column, the topmost ground-mass tile, provided it is a
 *     walkable (grass-topped) tile. Columns whose only ground is a bare
 *     stone floor, such as the bottom of a spike pit, have no surface.
 *  4. Columns without a surface: an open end continues the neighbouring
 *     ground line; a gap between two ground sections is a pit and takes the
 *     lower of its two rims, so the pit counts as underground.
 *
 * Columns that do have a surface keep their own row, so a walkable low
 * corridor between raised blocks is never mistaken for a pit.
 */
public class TerrainAnalyzer {
    private static final int NO_GROUND = -1;

    private final TileClassifier classifier;

    public TerrainAnalyzer() {
        this(new TileClassifier());
    }

    public TerrainAnalyzer(TileClassifier classifier) {
        this.classifier = classifier;
    }

    public TerrainProfile analyze(int[][] levelData) {
        int rows = levelData.length;
        int columns = levelData[0].length;

        boolean[][] solid = findSolidTiles(levelData, rows, columns);
        boolean[][] border = findBorder(solid, rows, columns);
        boolean[][] groundMass = findGroundMass(solid, border, rows, columns);

        int[] raw = findSurfaceRows(levelData, groundMass, rows, columns);
        return new TerrainProfile(levelData, resolveColumnsWithoutSurface(raw, rows));
    }

    private boolean[][] findSolidTiles(int[][] levelData, int rows, int columns) {
        boolean[][] solid = new boolean[rows][columns];
        for (int row = 0; row < rows; row++)
            for (int column = 0; column < columns; column++)
                solid[row][column] = classifier.isSolid(levelData, column, row);
        return solid;
    }

    private boolean[][] findBorder(boolean[][] solid, int rows, int columns) {
        boolean[][] border = new boolean[rows][columns];
        for (int column = 0; column < columns; column++)
            for (int row = 0; row < rows && solid[row][column]; row++)
                border[row][column] = true;
        return border;
    }

    private boolean[][] findGroundMass(boolean[][] solid, boolean[][] border, int rows, int columns) {
        boolean[][] mass = new boolean[rows][columns];
        ArrayDeque<int[]> queue = new ArrayDeque<>();

        for (int column = 0; column < columns; column++)
            enqueueIfGround(solid, border, mass, queue, rows - 1, column);

        int[][] neighbours = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!queue.isEmpty()) {
            int[] tile = queue.poll();
            for (int[] step : neighbours) {
                int row = tile[0] + step[0];
                int column = tile[1] + step[1];
                if (row >= 0 && row < rows && column >= 0 && column < columns)
                    enqueueIfGround(solid, border, mass, queue, row, column);
            }
        }
        return mass;
    }

    private void enqueueIfGround(boolean[][] solid, boolean[][] border, boolean[][] mass,
            ArrayDeque<int[]> queue, int row, int column) {
        if (solid[row][column] && !border[row][column] && !mass[row][column]) {
            mass[row][column] = true;
            queue.add(new int[] { row, column });
        }
    }

    private int[] findSurfaceRows(int[][] levelData, boolean[][] groundMass, int rows, int columns) {
        int[] raw = new int[columns];
        Arrays.fill(raw, NO_GROUND);
        for (int column = 0; column < columns; column++)
            for (int row = 0; row < rows; row++)
                if (groundMass[row][column]) {
                    if (classifier.isWalkableSurface(levelData[row][column]))
                        raw[column] = row;
                    break;   // the topmost ground tile decides; nothing deeper is a surface
                }
        return raw;
    }

    private int[] resolveColumnsWithoutSurface(int[] raw, int rows) {
        int columns = raw.length;
        int[] resolved = raw.clone();

        int[] leftGround = new int[columns];
        int last = NO_GROUND;
        for (int column = 0; column < columns; column++) {
            if (raw[column] != NO_GROUND)
                last = raw[column];
            leftGround[column] = last;
        }
        int[] rightGround = new int[columns];
        last = NO_GROUND;
        for (int column = columns - 1; column >= 0; column--) {
            if (raw[column] != NO_GROUND)
                last = raw[column];
            rightGround[column] = last;
        }

        for (int column = 0; column < columns; column++) {
            if (raw[column] != NO_GROUND)
                continue;
            int left = leftGround[column];
            int right = rightGround[column];
            if (left != NO_GROUND && right != NO_GROUND)
                resolved[column] = Math.max(left, right);   // pit: rise to the lower rim
            else if (left != NO_GROUND)
                resolved[column] = left;     // open end: keep the neighbouring ground line
            else if (right != NO_GROUND)
                resolved[column] = right;
            else
                resolved[column] = rows;     // no surface anywhere: nothing is underground
        }
        return resolved;
    }
}
