package background;

import java.awt.Graphics2D;

/**
 * One drawable layer of the Stage 1 backdrop.
 *
 * A layer only knows how to paint itself across the whole view. Where it is
 * allowed to appear (above or below the terrain) is decided by the caller,
 * which applies a clip before invoking {@link #draw(Graphics2D, int)}.
 */
public interface BackgroundLayer {

    /**
     * @param g       graphics context, already clipped by the caller if needed
     * @param cameraX horizontal level scroll offset in pixels
     */
    void draw(Graphics2D g, int cameraX);
}
