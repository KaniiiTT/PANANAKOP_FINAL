package background;

import java.util.Set;

import utilz.HelpMethods;

/**
 * Knows what the tile values in the level data mean for terrain analysis.
 *
 * Solidity reuses {@link HelpMethods#IsTileSolid}, the same rule collision
 * uses. A walkable surface is a tile whose top edge is a full-width grass
 * cap in res/outside_sprites.png; plain stone, wall and filler tiles can be
 * solid without being a surface.
 */
public class TileClassifier {
    // Atlas indices (12 tiles per row) with a grass cap across the whole top edge.
    // Corner-tuft tiles (6, 8, 9, 10, 44) are deliberately excluded: they only
    // decorate wall tops and are not walkable ground lines.
    private static final Set<Integer> GRASS_TOPPED = Set.of(
            0, 1, 2, 3,
            30, 31, 33, 34, 35,
            36, 37, 38, 39);

    public boolean isSolid(int[][] levelData, int column, int row) {
        return HelpMethods.IsTileSolid(column, row, levelData);
    }

    public boolean isWalkableSurface(int tileValue) {
        return GRASS_TOPPED.contains(tileValue);
    }
}
