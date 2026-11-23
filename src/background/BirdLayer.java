package background;

import java.awt.image.BufferedImage;

import main.Game;

/**
 * Sends small flocks of birds across the sky by day. The sky is often empty
 * between flocks, and no new flock takes off after sunset.
 */
public class BirdLayer extends FlockLayer<Bird> {
    private static final int POOL_SIZE = 5;
    private static final int BODY_COLOR = 0xFF3C4668; // dark slate, reads as a distant silhouette

    // Nine-wide wing frames: raised, half raised, level, lowered.
    private static final String[][] FRAMES = {
            { "a.......a", "aa.....aa", ".aa...aa.", "..aaaaa..", "...aaa..." },
            { ".........", "aa.....aa", ".aa...aa.", "..aaaaa..", "...aaa..." },
            { ".........", ".........", "aaaa.aaaa", "..aaaaa..", "...aaa..." },
            { ".........", "..aaaaa..", ".aaaaaaa.", "aa.aaa.aa", "a...a...a" } };

    private final DayNightCycle cycle;

    public BirdLayer(DayNightCycle cycle, double parallaxFactor, double verticalFactor, double updatesPerSecond) {
        // The first flock takes off soon after the level starts.
        super(Bird.class, updatesPerSecond, verticalFactor, 3 + Math.random() * 3);
        this.cycle = cycle;
        int unit = PixelSprites.pixelUnit();
        BufferedImage[] frames = new BufferedImage[FRAMES.length];
        for (int i = 0; i < frames.length; i++)
            frames[i] = PixelSprites.create(FRAMES[i], unit, BODY_COLOR);
        for (int i = 0; i < POOL_SIZE; i++)
            add(new Bird(frames, parallaxFactor, dt()));
    }

    @Override
    protected double opacity() {
        return cycle.getDaylight(); // birds still in the sky fade away as it gets dark
    }

    @Override
    protected boolean mayLaunch() {
        return cycle.getNightness() <= 0.3;
    }

    @Override
    protected double nextFlockDelay() {
        return 18 + random.nextDouble() * 22;
    }

    @Override
    protected int flockSize() {
        return 1 + random.nextInt(3);
    }

    @Override
    protected double flockY() {
        return Game.GAME_HEIGHT * (0.07 + 0.22 * random.nextDouble());
    }

    @Override
    protected double flockSpeed() {
        return Game.GAME_WIDTH * (0.040 + 0.025 * random.nextDouble());
    }
}
