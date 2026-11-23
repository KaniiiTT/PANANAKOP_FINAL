package background;

import java.awt.image.BufferedImage;

import main.Game;

/** A bird: a steady flight line with a gentle bob and a relaxed wing flap. */
public class Bird extends FlyingCreature {
    // Order in which the wing frames are shown (up ... down ... up).
    private static final int[] FLAP_CYCLE = { 0, 1, 2, 3, 2, 1 };
    private static final double FLAPS_PER_SECOND = 9.0;
    private static final double BOB_RATE = 1.6; // radians per second

    private final double bobAmplitude = Game.GAME_HEIGHT * 0.006;

    public Bird(BufferedImage[] frames, double parallaxFactor, double secondsPerUpdate) {
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
        return Math.sin(flightClock * BOB_RATE) * bobAmplitude;
    }
}
