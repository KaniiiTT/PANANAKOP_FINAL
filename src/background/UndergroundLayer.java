package background;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import main.Game;

/**
 * Repeating dirt-and-stone soil texture used behind everything below the
 * terrain surface (including open pits and gaps).
 *
 * The texture is anchored to world coordinates horizontally (it scrolls with
 * the level tiles) and to the top of the world vertically, so it never
 * depends on where the terrain boundary happens to be.
 */
public class UndergroundLayer implements BackgroundLayer {
    private final BufferedImage soil;

    public UndergroundLayer(BufferedImage soil) {
        this.soil = soil;
    }

    @Override
    public void draw(Graphics2D g, int cameraX) {
        int textureWidth = soil.getWidth();
        int textureHeight = soil.getHeight();
        int startX = -Math.floorMod(cameraX, textureWidth);

        for (int y = 0; y < Game.GAME_HEIGHT; y += textureHeight)
            for (int x = startX; x < Game.GAME_WIDTH; x += textureWidth)
                g.drawImage(soil, x, y, null);
    }
}
