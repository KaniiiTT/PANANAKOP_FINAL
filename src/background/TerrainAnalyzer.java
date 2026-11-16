
package background;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Detects the actual ground surface from level tile data.
 *
 * Only solid terrain connected to the bottom of the level
 * is considered part of the main ground mass.
 *
 * Empty columns are treated as open air instead of being
 * automatically filled between neighboring terrain sections.
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

        boolean[][] groundMass = findGroundMass(
                solid, border, rows, columns);

        int[] surfaceRows = findSurfaceRows(
                levelData, groundMass, rows, columns);

        return new TerrainProfile(
                levelData,
                resolveColumnsWithoutSurface(surfaceRows, rows)
        );
    }

    private boolean[][] findSolidTiles(
            int[][] levelData, int rows, int columns) {

        boolean[][] solid = new boolean[rows][columns];

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {

                solid[row][column] =
                        classifier.isSolid(levelData, column, row);
            }
        }

        return solid;
    }

    private boolean[][] findBorder(
            boolean[][] solid, int rows, int columns) {

        boolean[][] border = new boolean[rows][columns];

        for (int column = 0; column < columns; column++) {

            for (int row = 0;
                    row < rows && solid[row][column];
                    row++) {

                border[row][column] = true;
            }
        }

        return border;
    }

    private boolean[][] findGroundMass(
            boolean[][] solid,
            boolean[][] border,
            int rows,
            int columns) {

        boolean[][] mass = new boolean[rows][columns];

        ArrayDeque<int[]> queue = new ArrayDeque<>();

        // Start searching from the bottom row.
        for (int column = 0; column < columns; column++) {
            enqueueIfGround(
                    solid, border, mass, queue,
                    rows - 1, column
            );
        }

        int[][] neighbours = {
                { 1, 0 },
                { -1, 0 },
                { 0, 1 },
                { 0, -1 }
        };

        while (!queue.isEmpty()) {

            int[] tile = queue.poll();

            for (int[] step : neighbours) {

                int row = tile[0] + step[0];
                int column = tile[1] + step[1];

                if (row >= 0 && row < rows
                        && column >= 0 && column < columns) {

                    enqueueIfGround(
                            solid, border, mass, queue,
                            row, column
                    );
                }
            }
        }

        return mass;
    }

    private void enqueueIfGround(
            boolean[][] solid,
            boolean[][] border,
            boolean[][] mass,
            ArrayDeque<int[]> queue,
            int row,
            int column) {

        if (solid[row][column]
                && !border[row][column]
                && !mass[row][column]) {

            mass[row][column] = true;
            queue.add(new int[] { row, column });
        }
    }

    private int[] findSurfaceRows(
            int[][] levelData,
            boolean[][] groundMass,
            int rows,
            int columns) {

        int[] raw = new int[columns];

        Arrays.fill(raw, NO_GROUND);

        for (int column = 0; column < columns; column++) {

            for (int row = 0; row < rows; row++) {

                if (groundMass[row][column]) {

                    if (classifier.isWalkableSurface(
                            levelData[row][column])) {

                        raw[column] = row;
                    }

                    break;
                }
            }
        }

        return raw;
    }

    /**
     * Columns without an actual ground surface remain open.
     *
     * Using rows as the boundary means:
     * - Above-ground scenery can extend through the column.
     * - Underground soil is not drawn in the column.
     */
    private int[] resolveColumnsWithoutSurface(
            int[] raw, int rows) {

        int[] resolved = raw.clone();

        for (int column = 0; column < raw.length; column++) {

            if (raw[column] == NO_GROUND) {

                resolved[column] = rows;
            }
        }

        return resolved;
    }
}
