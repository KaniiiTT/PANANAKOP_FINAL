package background;

import java.awt.image.BufferedImage;

import main.Game;

/**
 * Sends small groups of bats flitting across the night sky. They take off only
 * once it is properly dark, and fly lower than the birds so they pass in front
 * of the far trees. By day the layer is invisible.
 */
public class BatLayer extends FlockLayer<Bat> {
    private static final int POOL_SIZE = 6;
    // Much darker than the hazy far trees they fly in front of, so they read as clear silhouettes.
    private static final int WING_COLOR = 0xFF140F24;

    // Nine-wide wing frames: raised, half raised, level, lowered. The pointed ears are on the top row.
    private static final String[][] FRAMES = {
            { "a.......a", "aa.a.a.aa", ".aaaaaaa.", "..aaaaa..", "...a.a..." },
            { ".........", "aa.a.a.aa", ".aaaaaaa.", "..aaaaa..", "...a.a..." },
            { ".........", "...a.a...", "aaaaaaaaa", ".aaaaaaa.", "...a.a..." },
            { "...a.a...", "..aaaaa..", ".aaaaaaa.", "aa.aaa.aa", "a...a...a" } };

    private final DayNightCycle cycle;

    public BatLayer(DayNightCycle cycle, double parallaxFactor, double verticalFactor, double updatesPerSecond) {
        super(Bat.class, updatesPerSecond, verticalFactor, 4);
        this.cycle = cycle;
        int unit = PixelSprites.pixelUnit() + 1; // a little bigger than the birds: bats are close
        BufferedImage[] frames = new BufferedImage[FRAMES.length];
        for (int i = 0; i < frames.length; i++)
            frames[i] = PixelSprites.create(FRAMES[i], unit, WING_COLOR);
        for (int i = 0; i < POOL_SIZE; i++)
            add(new Bat(frames, parallaxFactor, dt()));
    }

    @Override
    protected double opacity() {
        return cycle.getNightness();
    }

    @Override
    protected boolean mayLaunch() {
        return cycle.getNightness() > 0.7;
    }

    @Override
    protected double nextFlockDelay() {
        return 7 + random.nextDouble() * 12;
    }

    @Override
    protected int flockSize() {
        return 2 + random.nextInt(3);
    }

    @Override
    protected double flockY() {
        return Game.GAME_HEIGHT * (0.28 + 0.24 * random.nextDouble());
    }

    @Override
    protected double flockSpeed() {
        return Game.GAME_WIDTH * (0.060 + 0.030 * random.nextDouble());
    }
}
