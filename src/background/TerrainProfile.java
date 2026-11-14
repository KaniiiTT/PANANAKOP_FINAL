package background;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;

/**
 * The terrain boundary of a level: for every tile column, the first row that
 * is underground. Rows above it are open air (scenery), rows from it down are
 * soil territory.
 *
 * A profile is immutable and is produced by {@link TerrainAnalyzer}. It turns
 * the boundary into screen-space clip shapes for a given camera position, so
 * the above-ground scenery and the underground fill are both clipped by the
 * very same boundary while remaining separate rendering operations.
 */
public final class TerrainProfile {
    private final int[][] sourceLevelData;
    private final int[] surfaceRows;

    TerrainProfile(int[][] sourceLevelData, int[] surfaceRows) {
        this.sourceLevelData = sourceLevelData;
        this.surfaceRows = surfaceRows;
    }

    /** True when this profile was computed from exactly this level array. */
    public boolean isBuiltFrom(int[][] levelData) {
        return sourceLevelData == levelData;
    }

    /** First underground row of a tile column (columns outside the level reuse the nearest edge). */
    public int getSurfaceRow(int column) {
        int clamped = Math.max(0, Math.min(surfaceRows.length - 1, column));
        return surfaceRows[clamped];
    }

    /** Screen region where sky-side scenery (mountains, hills, trees) may appear. */
    public Shape aboveGroundClip(int cameraX, int tileSize, int viewWidth, int viewHeight) {
        return buildClip(cameraX, tileSize, viewWidth, viewHeight, true);
    }

    /** Screen region where the underground soil fill may appear. */
    public Shape undergroundClip(int cameraX, int tileSize, int viewWidth, int viewHeight) {
        return buildClip(cameraX, tileSize, viewWidth, viewHeight, false);
    }

    private Shape buildClip(int cameraX, int tileSize, int viewWidth, int viewHeight, boolean above) {
        Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO);
        int firstColumn = Math.floorDiv(cameraX, tileSize);
        int lastColumn = Math.floorDiv(cameraX + viewWidth - 1, tileSize);

        // Merge neighbouring columns with the same boundary into one rectangle.
        int runStart = firstColumn;
        int runRow = getSurfaceRow(firstColumn);
        for (int column = firstColumn + 1; column <= lastColumn + 1; column++) {
            boolean endOfView = column > lastColumn;
            int row = endOfView ? Integer.MIN_VALUE : getSurfaceRow(column);
            if (row == runRow)
                continue;

            int x0 = Math.max(0, runStart * tileSize - cameraX);
            int x1 = Math.min(viewWidth, column * tileSize - cameraX);
            int boundaryY = Math.max(0, Math.min(viewHeight, runRow * tileSize));
            Rectangle rect = above
                    ? new Rectangle(x0, 0, x1 - x0, boundaryY)
                    : new Rectangle(x0, boundaryY, x1 - x0, viewHeight - boundaryY);
            if (rect.width > 0 && rect.height > 0)
                path.append(rect, false);

            runStart = column;
            runRow = row;
        }
        return path;
    }
}
