package background;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import main.Game;

/**
 * Full-screen parallax image with an optional vertical placement offset.
 * Positive parallaxFactor controls horizontal camera movement; verticalOffset
 * moves the artwork itself upward/downward without changing its scale.
 */
public class ParallaxLayer implements BackgroundLayer {
    private final BufferedImage image;
    private final double parallaxFactor;
    private final int verticalOffset;

    public ParallaxLayer(BufferedImage image, double parallaxFactor) {
        this(image, parallaxFactor, 0);
    }

    public ParallaxLayer(BufferedImage image, double parallaxFactor, int verticalOffset) {
        this.image = image;
        this.parallaxFactor = parallaxFactor;
        this.verticalOffset = verticalOffset;
    }

    @Override
    public void draw(Graphics2D g, int cameraX) {
        int width = Game.GAME_WIDTH;
        int height = Game.GAME_HEIGHT;
        int offset = Math.floorMod((int) (cameraX * parallaxFactor), width);
        int x = -offset;
        int y = verticalOffset;
        g.drawImage(image, x, y, width, height, null);
        g.drawImage(image, x + width, y, width, height, null);
    }
}
