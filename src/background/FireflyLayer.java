package background;

import java.awt.image.BufferedImage;

import main.Game;

/**
 * Fireflies drifting over the bushes at night.
 *
 * It is drawn AFTER the mood tint, so the glow is not dimmed by the night
 * colour and really looks like light. During the day the fireflies are
 * invisible (their brightness is zero).
 */
public class FireflyLayer extends AmbientLayer {
    private static final double FACTOR = 0.34;
    private static final int COUNT = 16;

    public FireflyLayer(DayNightCycle cycle, double verticalFactor, double updatesPerSecond) {
        super(updatesPerSecond, verticalFactor);
        BufferedImage glow = createGlow(PixelSprites.pixelUnit());
        int w = Game.GAME_WIDTH;
        int h = Game.GAME_HEIGHT;
        for (int i = 0; i < COUNT; i++) {
            double speed = w * (0.008 + 0.010 * random.nextDouble());
            add(new Firefly(cycle, glow, FACTOR, dt(), random, speed, h * 0.45, h * 0.90));
        }
    }

    /** A bright core inside a soft yellow-green halo. */
    private static BufferedImage createGlow(int unit) {
        int size = 9 * unit;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        double centre = (size - 1) / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double d = Math.hypot(x - centre, y - centre) / centre;
                if (d >= 1.0)
                    continue;
                if (d < 0.20) {
                    image.setRGB(x, y, 0xFFFFFAB4); // core
                } else {
                    int a = (int) (190 * (1 - d) * (1 - d));
                    image.setRGB(x, y, (a << 24) | 0xE8F070);
                }
            }
        }
        return image;
    }

    @Override
    protected void spawn() {
        // The fireflies are permanent; they just stay invisible during the day.
    }
}
