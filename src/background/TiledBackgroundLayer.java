package background;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import main.Game;

/**
 * Abstract base of the horizontally repeating background layers.
 *
 * The base class owns everything that is the same for every layer: the image
 * (scaled to the game view once, at construction, and then reused every
 * frame), the parallax factor, the vertical placement and the wraparound
 * drawing that keeps the layer gap-free. Subclasses only decide HOW the layer
 * moves by implementing {@link #horizontalPosition(int)} and, when they
 * animate on their own, by overriding {@link #update()}.
 */
public abstract class TiledBackgroundLayer implements BackgroundLayer {
    private final BufferedImage image;
    private final int cropTop; // screen row of the first stored row (transparent rows above are not stored)
    private final int tileWidth;
    private double parallaxFactor;
    private int verticalOffset;

    protected TiledBackgroundLayer(BufferedImage source, double parallaxFactor, int verticalOffset) {
        this.tileWidth = Game.GAME_WIDTH;
        BufferedImage scaled = scaleToView(source);
        int[] visible = visibleRows(scaled);
        if (visible == null) {
            // Fully opaque (the sky): a plain RGB image blits faster than one with alpha.
            this.image = copyOf(scaled, 0, scaled.getHeight(), BufferedImage.TYPE_INT_RGB);
            this.cropTop = 0;
        } else {
            // Keep only the rows that contain something, so empty rows are never blitted.
            this.image = copyOf(scaled, visible[0], visible[1] - visible[0] + 1, BufferedImage.TYPE_INT_ARGB);
            this.cropTop = visible[0];
        }
        this.parallaxFactor = parallaxFactor;
        this.verticalOffset = verticalOffset;
    }

    /**
     * Scales once to the game view, exactly as the previous single background
     * image was stretched on every frame, so the look does not change.
     */
    private static BufferedImage scaleToView(BufferedImage source) {
        BufferedImage scaled = new BufferedImage(Game.GAME_WIDTH, Game.GAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.drawImage(source, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        g.dispose();
        return scaled;
    }

    /** First and last row containing any visible pixel, or null if every pixel is fully opaque. */
    private static int[] visibleRows(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] row = new int[w];
        int first = h;
        int last = -1;
        boolean hasTransparency = false;
        for (int y = 0; y < h; y++) {
            img.getRGB(0, y, w, 1, row, 0, w);
            boolean any = false;
            for (int x = 0; x < w; x++) {
                int a = row[x] >>> 24;
                if (a != 0)
                    any = true;
                if (a != 255)
                    hasTransparency = true;
            }
            if (any) {
                first = Math.min(first, y);
                last = y;
            }
        }
        if (!hasTransparency || last < 0)
            return null;
        return new int[] { first, last };
    }

    private static BufferedImage copyOf(BufferedImage img, int firstRow, int rows, int type) {
        BufferedImage copy = new BufferedImage(img.getWidth(), rows, type);
        Graphics2D g = copy.createGraphics();
        g.drawImage(img, 0, -firstRow, null);
        g.dispose();
        return copy;
    }

    /**
     * Total horizontal scroll of this layer in pixels. Positive values move
     * the artwork to the left.
     *
     * @param cameraX horizontal camera offset of the level in pixels
     */
    protected abstract double horizontalPosition(int cameraX);

    /** Parallax algorithm shared by the subclasses: layerOffset = cameraOffset * parallaxFactor. */
    protected final double cameraOffset(int cameraX) {
        return cameraX * parallaxFactor;
    }

    @Override
    public final void draw(Graphics2D g, int cameraX) {
        double position = horizontalPosition(cameraX);
        double wrapped = position - Math.floor(position / tileWidth) * tileWidth;
        int x = -(int) wrapped;
        int y = verticalOffset + cropTop;
        g.drawImage(image, x, y, null);
        if (x + tileWidth < Game.GAME_WIDTH)
            g.drawImage(image, x + tileWidth, y, null);
    }

    public double getParallaxFactor() {
        return parallaxFactor;
    }

    public void setParallaxFactor(double parallaxFactor) {
        this.parallaxFactor = Math.max(0.0, parallaxFactor);
    }

    public int getVerticalOffset() {
        return verticalOffset;
    }

    public void setVerticalOffset(int verticalOffset) {
        this.verticalOffset = verticalOffset;
    }
}
