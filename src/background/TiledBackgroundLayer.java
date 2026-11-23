package background;

import java.awt.Composite;
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
    private double verticalFactor;
    private int verticalOffset;

    protected TiledBackgroundLayer(BufferedImage source, double parallaxFactor, int verticalOffset) {
        this(source, parallaxFactor, verticalOffset, 0.0);
    }

    /**
     * @param verticalFactor how much the layer follows vertical movement (see
     *                       {@link BackgroundLayer#getVerticalFactor()}). A layer that can
     *                       move and reaches the bottom of the screen gets extra rows of
     *                       artwork below the screen, mirrored from its last rows.
     */
    protected TiledBackgroundLayer(BufferedImage source, double parallaxFactor, int verticalOffset,
            double verticalFactor) {
        this.tileWidth = Game.GAME_WIDTH;
        BufferedImage scaled = scaleToView(source);
        int[] visible = visibleRows(scaled);
        if (visible == null) {
            // Fully opaque (the sky): a plain RGB image blits faster than one with alpha.
            this.image = copyOf(scaled, 0, scaled.getHeight(), 0, BufferedImage.TYPE_INT_RGB);
            this.cropTop = 0;
        } else {
            // Keep only the rows that contain something, so empty rows are never blitted.
            boolean reachesBottom = visible[1] == scaled.getHeight() - 1;
            int padding = verticalFactor > 0 && reachesBottom ? MAX_VERTICAL_SHIFT : 0;
            this.image = copyOf(scaled, visible[0], visible[1] - visible[0] + 1, padding,
                    BufferedImage.TYPE_INT_ARGB);
            this.cropTop = visible[0];
        }
        this.parallaxFactor = parallaxFactor;
        this.verticalFactor = Math.max(0.0, verticalFactor);
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

    /** Copies a band of rows; {@code padding} extra rows are a mirror image of the last copied rows. */
    private static BufferedImage copyOf(BufferedImage img, int firstRow, int rows, int padding, int type) {
        int width = img.getWidth();
        BufferedImage copy = new BufferedImage(width, rows + padding, type);
        Graphics2D g = copy.createGraphics();
        g.drawImage(img, 0, -firstRow, null);
        if (padding > 0) {
            // Source rectangle with its edges swapped = flipped upside down, so the artwork
            // continues smoothly below the screen instead of smearing a single row.
            int lastRow = firstRow + rows - 1;
            g.drawImage(img, 0, rows, width, rows + padding, 0, lastRow + 1, width, lastRow + 1 - padding, null);
        }
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
        double opacity = opacity();
        Composite original = null;
        if (opacity < 0.99) {
            original = g.getComposite();
            g.setComposite(AlphaSteps.shared().get(opacity));
        }
        paint(g, x, y);
        if (original != null)
            g.setComposite(original);
    }

    /**
     * Paints one wrapped copy of the artwork at (x, y) and, when needed, the
     * next copy beside it. A layer with a special look (swaying bushes) overrides this.
     */
    protected void paint(Graphics2D g, int x, int y) {
        g.drawImage(image, x, y, null);
        if (x + tileWidth < Game.GAME_WIDTH)
            g.drawImage(image, x + tileWidth, y, null);
    }

    /**
     * Screen row of the first visible pixel in the given column of the artwork
     * (before any vertical parallax shift), or -1 if the column is empty.
     * Used to find, for example, where the tree crowns of a forest are.
     */
    public final int topVisibleRow(int column) {
        if (column < 0 || column >= image.getWidth())
            return -1;
        for (int row = 0; row < image.getHeight(); row++)
            if ((image.getRGB(column, row) >>> 24) != 0)
                return verticalOffset + cropTop + row;
        return -1;
    }

    /** True if the artwork has a visible pixel at the given column and screen row (before any vertical shift). */
    public final boolean isVisibleAt(int column, int screenRow) {
        int row = screenRow - verticalOffset - cropTop;
        if (column < 0 || column >= image.getWidth() || row < 0 || row >= image.getHeight())
            return false;
        return (image.getRGB(column, row) >>> 24) != 0;
    }

    /** Screen x at which the given column of the artwork is drawn for this camera offset. */
    public final int screenXOfColumn(int column, int cameraX) {
        double position = horizontalPosition(cameraX);
        double wrapped = position - Math.floor(position / tileWidth) * tileWidth;
        int x = column - (int) wrapped;
        return x < 0 ? x + tileWidth : x;
    }

    /** The stored artwork (the visible rows only, plus any bottom padding). */
    protected final BufferedImage getImage() {
        return image;
    }

    protected final int getTileWidth() {
        return tileWidth;
    }

    @Override
    public double getVerticalFactor() {
        return verticalFactor;
    }

    /** How visible the layer is, 0..1. A layer that fades (clouds at night) overrides this. */
    protected double opacity() {
        return 1.0;
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
