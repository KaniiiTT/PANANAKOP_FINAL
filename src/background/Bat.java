package background;

import java.awt.image.BufferedImage;

import main.Game;

/**
 * A bat: it flaps much faster than a bird, swoops up and down along two
 * overlapping waves and keeps speeding up and slowing down, so its flight
 * looks jerky and erratic.
 */
public class Bat extends FlyingCreature {
    private static final int[] FLAP_CYCLE = { 0, 1, 2, 3, 2, 1 };
    private static final double FLAPS_PER_SECOND = 14.0;

    private final double bigSwoop = Game.GAME_HEIGHT * 0.030;
    private final double smallSwoop = Game.GAME_HEIGHT * 0.010;

    public Bat(BufferedImage[] frames, double parallaxFactor, double secondsPerUpdate) {
        super(frames, parallaxFactor, secondsPerUpdate);
    }

    @Override
    protected int[] flapCycle() {
        return FLAP_CYCLE;
    }

    @Override
    protected double flapsPerSecond() {
        return FLAPS_PER_SECOND;
    }

    @Override
    protected double verticalOffset(double flightClock) {
        return Math.sin(flightClock * 1.1) * bigSwoop + Math.sin(flightClock * 3.4 + 1.3) * smallSwoop;
    }

    @Override
    protected double speedFactor(double flightClock) {
        return 1.0 + 0.35 * Math.sin(flightClock * 0.9);
    }
}
