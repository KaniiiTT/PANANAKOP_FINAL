package background;

import java.awt.Graphics2D;

import main.Game;

/**
 * One drawable layer of the Stage 1 backdrop.
 *
 * A layer only knows how to paint itself across the whole view. Where it is
 * allowed to appear (above or below the terrain) is decided by the caller,
 * which applies a clip before invoking {@link #draw(Graphics2D, int)}.
 */
public interface BackgroundLayer {

    /**
     * Largest vertical shift, in pixels, any layer may be moved by the vertical
     * parallax. Layers that reach the bottom of the screen keep this many extra
     * rows of artwork below the screen so a shift never opens a gap.
     */
    int MAX_VERTICAL_SHIFT = Math.max(8, Math.round(Game.GAME_HEIGHT * 0.03f));

    /**
     * How far this layer follows the viewer's vertical movement: the layer is
     * shifted by (viewer shift in pixels * this factor). 0 means the layer never
     * moves vertically (the sky, the full-screen tint). Closer layers use larger
     * values, exactly like the horizontal parallax factor.
     */
    default double getVerticalFactor() {
        return 0.0;
    }

    /**
     * Advances any self-driven animation (for example drifting clouds) by one
     * game update. Layers that only react to the camera keep this no-op default.
     */
    default void update() {
    }

    /**
     * @param g       graphics context, already clipped by the caller if needed
     * @param cameraX horizontal level scroll offset in pixels
     */
    void draw(Graphics2D g, int cameraX);
}
