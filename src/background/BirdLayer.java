package background;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import main.Game;

/**
 * Sends small flocks of birds across the sky now and then.
 *
 * The layer keeps a fixed pool of birds. A countdown timer decides when the
 * next flock takes off; a flock wakes one to three inactive birds, so there is
 * never more than the pool size in the sky.
 */
public class BirdLayer extends AmbientLayer {
    private static final int POOL_SIZE = 5;
    private static final int BODY_COLOR = 0xFF3C4668; // dark slate, reads as a distant silhouette

    // Nine-wide wing frames: raised, half raised, level, lowered.
    private static final String[][] FRAMES = {
            { "a.......a", "aa.....aa", ".aa...aa.", "..aaaaa..", "...aaa..." },
            { ".........", "aa.....aa", ".aa...aa.", "..aaaaa..", "...aaa..." },
            { ".........", ".........", "aaaa.aaaa", "..aaaaa..", "...aaa..." },
            { ".........", "..aaaaa..", ".aaaaaaa.", "aa.aaa.aa", "a...a...a" } };

    private final ArrayList<Bird> birds = new ArrayList<>();
    private final DayNightCycle cycle;
    private double secondsUntilNextFlock;

    public BirdLayer(DayNightCycle cycle, double parallaxFactor, double updatesPerSecond) {
        super(updatesPerSecond);
        this.cycle = cycle;
        int unit = PixelSprites.pixelUnit();
        BufferedImage[] frames = new BufferedImage[FRAMES.length];
        for (int i = 0; i < frames.length; i++)
            frames[i] = PixelSprites.create(FRAMES[i], unit, BODY_COLOR);
        for (int i = 0; i < POOL_SIZE; i++) {
            Bird bird = new Bird(frames, parallaxFactor, dt());
            birds.add(bird);
            add(bird);
        }
        secondsUntilNextFlock = 3 + random.nextDouble() * 3; // first flock soon after the level starts
    }

    @Override
    protected double opacity() {
        return cycle.getDaylight(); // birds still in the sky fade away as it gets dark
    }

    @Override
    protected void spawn() {
        if (cycle.getNightness() > 0.3)
            return; // no new flocks after sunset; the countdown waits for the morning
        secondsUntilNextFlock -= dt();
        if (secondsUntilNextFlock > 0)
            return;
        launchFlock();
        secondsUntilNextFlock = 18 + random.nextDouble() * 22; // the sky is often empty between flocks
    }

    private void launchFlock() {
        int count = 1 + random.nextInt(3);
        int direction = random.nextBoolean() ? 1 : -1;
        double speed = Game.GAME_WIDTH * (0.040 + 0.025 * random.nextDouble());
        double flockY = Game.GAME_HEIGHT * (0.07 + 0.22 * random.nextDouble());

        double gap = 0;
        for (int i = 0; i < count; i++) {
            Bird bird = findInactive(Bird.class);
            if (bird == null)
                return;
            double startX = direction > 0
                    ? -bird.getSpriteWidth() - 8 - gap
                    : Game.GAME_WIDTH + 8 + gap;
            double y = flockY + (random.nextDouble() - 0.5) * Game.GAME_HEIGHT * 0.05;
            bird.launch(direction, startX, y, speed * (0.95 + 0.1 * random.nextDouble()),
                    random.nextDouble() * 6, random.nextDouble() * Math.PI * 2, getCameraX());
            gap += 60 + random.nextDouble() * 50;
        }
    }
}
