package background;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import main.Game;

/**
 * Forest artwork that sways gently in the wind.
 *
 * The picture is painted in thin horizontal strips and every strip is shifted
 * sideways by a whole number of pixels. The shift is strongest at the tree
 * tops and shrinks to nothing near the ground, so the trunks stay planted
 * while the crowns lean. A slow wave runs down the strips (a gust passing
 * through) and a second, even slower wave makes the wind rise and fall.
 *
 * Nothing is created while playing: the strips are source rectangles of the
 * one stored image, and their sway strengths are worked out once.
 */
public class SwayingForestLayer extends ForestLayer {
    private static final int STRIP_HEIGHT = 4;          // pixels per strip
    private static final double SWAY_ZONE = 0.70;       // share of the image height that sways
    private static final double SWAY_SECONDS = 4.6;     // one lean to the left and back
    private static final double GUST_SECONDS = 23.0;    // wind rises and falls this slowly
    private static final double WAVE_PER_ROW = 0.010;   // phase lag between rows (the gust travelling down)

    private final double[] stripWeight; // 1 at the tree tops, 0 at and below the base of the sway zone
    private final int swayingStrips;    // strips from the top that move at all
    private final double secondsPerUpdate;
    private int maxSway;                // pixels the tree tops move at full wind
    private double clock;

    /**
     * @param maxSway          pixels the tree tops lean at full wind (0 = no sway at all)
     * @param updatesPerSecond how often update() is called by the game loop
     */
    public SwayingForestLayer(BufferedImage image, double parallaxFactor, double verticalFactor, int maxSway,
            double updatesPerSecond) {
        super(image, parallaxFactor, verticalFactor);
        this.maxSway = Math.max(0, maxSway);
        this.secondsPerUpdate = 1.0 / updatesPerSecond;

        int rows = getImage().getHeight();
        int strips = (rows + STRIP_HEIGHT - 1) / STRIP_HEIGHT;
        stripWeight = new double[strips];
        int moving = 0;
        for (int s = 0; s < strips; s++) {
            double t = (s * STRIP_HEIGHT) / (SWAY_ZONE * rows);
            double weight = t >= 1.0 ? 0.0 : (1.0 - t) * (1.0 - t);
            stripWeight[s] = weight;
            if (weight > 0.0)
                moving = s + 1;
        }
        swayingStrips = moving;
    }

    public int getMaxSway() {
        return maxSway;
    }

    public void setMaxSway(int maxSway) {
        this.maxSway = Math.max(0, maxSway);
    }

    @Override
    public void update() {
        clock += secondsPerUpdate;
        if (clock > 1_000_000)
            clock -= 1_000_000;
    }

    @Override
    protected void paint(Graphics2D g, int x, int y) {
        if (maxSway == 0) {
            super.paint(g, x, y);
            return;
        }
        BufferedImage image = getImage();
        int width = getTileWidth();
        int rows = image.getHeight();

        double lean = clock * 2 * Math.PI / SWAY_SECONDS;
        // Wind strength between 0.15 and 1.0.
        double wind = 0.575 + 0.425 * Math.sin(clock * 2 * Math.PI / GUST_SECONDS + 1.0);

        for (int s = 0; s < swayingStrips; s++) {
            int top = s * STRIP_HEIGHT;
            int bottom = Math.min(rows, top + STRIP_HEIGHT);
            double wave = Math.sin(lean - top * WAVE_PER_ROW);
            int shift = (int) Math.round(maxSway * wind * stripWeight[s] * wave);
            blitStrip(g, image, width, x + shift, y, top, bottom);
        }
        // Everything below the swaying zone is one still block.
        int still = swayingStrips * STRIP_HEIGHT;
        if (still < rows)
            blitStrip(g, image, width, x, y, still, rows);
    }

    /** Draws rows [top, bottom) of the image at x, repeated so the wraparound never shows a gap. */
    private static void blitStrip(Graphics2D g, BufferedImage image, int width, int x, int y, int top, int bottom) {
        for (int copy = x > 0 ? -1 : 0; copy <= 1; copy++) {
            int dx = x + copy * width;
            if (dx >= Game.GAME_WIDTH)
                break;
            if (dx + width <= 0)
                continue;
            g.drawImage(image, dx, y + top, dx + width, y + bottom, 0, top, width, bottom, null);
        }
    }
}
