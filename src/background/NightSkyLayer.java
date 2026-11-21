package background;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import main.Game;

/**
 * Everything that only belongs to the night sky: a dark sky wash, twinkling
 * stars and a moon that crosses the sky. It sits right behind the clouds and
 * the forests, so they correctly pass in front of the stars and the moon, and
 * it draws nothing during the day.
 */
public class NightSkyLayer implements BackgroundLayer {
    private static final int STAR_COUNT = 70;
    private static final int STAR_STEPS = 8;   // brightness steps of the stars
    private static final int MOON_RADIUS = 6;  // in sprite pixels
    private static final double WASH_STRENGTH = 0.80;
    // The bushes are fully opaque below about 72% of the picture height, so the wash only
    // has to cover the part above that; the rest would be painted over anyway.
    private static final double WASH_COVERAGE = 0.73;

    private final DayNightCycle cycle;
    private final AlphaSteps alpha = AlphaSteps.shared();
    private final double secondsPerUpdate;
    private final Color[] starColors = new Color[STAR_STEPS + 1];
    private final BufferedImage wash;
    private final BufferedImage moon;
    private final BufferedImage halo;

    private final int[] starX = new int[STAR_COUNT];
    private final int[] starY = new int[STAR_COUNT];
    private final int[] starSize = new int[STAR_COUNT];
    private final double[] starPhase = new double[STAR_COUNT];
    private final double[] starRate = new double[STAR_COUNT];
    private double clock;

    public NightSkyLayer(DayNightCycle cycle, double updatesPerSecond) {
        this.cycle = cycle;
        this.secondsPerUpdate = 1.0 / updatesPerSecond;
        for (int i = 0; i <= STAR_STEPS; i++)
            starColors[i] = new Color(235, 240, 255, i * 255 / STAR_STEPS);

        int unit = PixelSprites.pixelUnit();
        wash = createWash();
        moon = createMoon(unit);
        halo = createHalo(moon.getWidth() * 3);

        // A fixed seed gives the same sky every run; more stars high up than near the horizon.
        Random random = new Random(7);
        for (int i = 0; i < STAR_COUNT; i++) {
            double r = random.nextDouble();
            starX[i] = random.nextInt(Game.GAME_WIDTH);
            starY[i] = (int) (Game.GAME_HEIGHT * 0.50 * r * r);
            starSize[i] = random.nextInt(5) == 0 ? unit : Math.max(1, unit - 1);
            starPhase[i] = random.nextDouble() * Math.PI * 2;
            starRate[i] = 0.7 + random.nextDouble() * 1.6;
        }
    }

    /** Deep blue at the top, a little lighter towards the horizon. */
    private static BufferedImage createWash() {
        int w = Game.GAME_WIDTH;
        int h = Game.GAME_HEIGHT;
        int covered = (int) Math.ceil(h * WASH_COVERAGE);
        BufferedImage image = new BufferedImage(w, covered, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < covered; y++) {
            double t = Math.min(1.0, y / (h * 0.60));
            int r = (int) (10 + 34 * t);
            int g = (int) (14 + 42 * t);
            int b = (int) (44 + 66 * t);
            int rgb = (r << 16) | (g << 8) | b;
            for (int x = 0; x < w; x++)
                image.setRGB(x, y, rgb);
        }
        return image;
    }

    private static BufferedImage createMoon(int unit) {
        int size = (2 * MOON_RADIUS + 1) * unit;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        // Craters, as offsets from the centre in sprite pixels.
        int[][] craters = { { -2, -2 }, { 2, 0 }, { -1, 3 }, { 3, -3 }, { 0, 0 } };
        for (int py = 0; py <= 2 * MOON_RADIUS; py++) {
            for (int px = 0; px <= 2 * MOON_RADIUS; px++) {
                int dx = px - MOON_RADIUS;
                int dy = py - MOON_RADIUS;
                if (dx * dx + dy * dy > (MOON_RADIUS + 0.4) * (MOON_RADIUS + 0.4))
                    continue;
                int color = 0xFFF6F2D6;                   // pale yellow
                if (dx + dy > MOON_RADIUS * 0.7)
                    color = 0xFFDDD8B8;                   // shaded lower right
                for (int[] c : craters)
                    if (dx == c[0] && dy == c[1])
                        color = 0xFFD2CDAA;
                for (int oy = 0; oy < unit; oy++)
                    for (int ox = 0; ox < unit; ox++)
                        image.setRGB(px * unit + ox, py * unit + oy, color);
            }
        }
        return image;
    }

    /** A soft round glow around the moon. */
    private static BufferedImage createHalo(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        double centre = (size - 1) / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double d = Math.hypot(x - centre, y - centre) / centre;
                if (d >= 1.0)
                    continue;
                int a = (int) (95 * (1 - d) * (1 - d));
                image.setRGB(x, y, (a << 24) | 0xDCE6FF);
            }
        }
        return image;
    }

    @Override
    public void update() {
        clock += secondsPerUpdate;
    }

    @Override
    public void draw(Graphics2D g, int cameraX) {
        double night = cycle.getNightness();
        if (night <= 0.01)
            return;

        Composite originalComposite = g.getComposite();
        Color originalColor = g.getColor();
        try {
            g.setComposite(alpha.get(night * WASH_STRENGTH));
            g.drawImage(wash, 0, 0, null);
            g.setComposite(originalComposite);

            for (int i = 0; i < STAR_COUNT; i++) {
                double twinkle = 0.55 + 0.45 * Math.sin(clock * starRate[i] + starPhase[i]);
                int level = (int) Math.round(night * twinkle * STAR_STEPS);
                if (level == 0)
                    continue;
                g.setColor(starColors[level]);
                g.fillRect(starX[i], starY[i], starSize[i], starSize[i]);
            }

            double progress = cycle.getMoonProgress();
            int moonX = (int) (Game.GAME_WIDTH * (0.12 + 0.76 * progress));
            int moonY = (int) (Game.GAME_HEIGHT * (0.27 - 0.20 * Math.sin(Math.PI * progress)));
            g.setComposite(alpha.get(night));
            g.drawImage(halo, moonX - halo.getWidth() / 2, moonY - halo.getHeight() / 2, null);
            g.drawImage(moon, moonX - moon.getWidth() / 2, moonY - moon.getHeight() / 2, null);
        } finally {
            g.setComposite(originalComposite);
            g.setColor(originalColor);
        }
    }
}
