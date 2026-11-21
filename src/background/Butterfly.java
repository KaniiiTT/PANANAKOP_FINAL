package background;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * A butterfly flapping its wings while it wanders over the bushes. It goes to
 * sleep at dusk: it fades out as the sky darkens and returns with the morning.
 */
public class Butterfly extends WanderingCreature {
    private static final int[] FLAP_CYCLE = { 0, 1, 2, 1 };
    private static final double FLAPS_PER_SECOND = 8.0;

    private final BufferedImage[] frames;
    private final DayNightCycle cycle;
    private final AlphaSteps alpha = AlphaSteps.shared();
    private double flapClock;

    public Butterfly(BufferedImage[] frames, DayNightCycle cycle, double parallaxFactor, double secondsPerUpdate,
            Random random, double speed, double minY, double maxY) {
        super(parallaxFactor, secondsPerUpdate, random, speed, minY, maxY);
        this.frames = frames;
        this.cycle = cycle;
        this.flapClock = random.nextDouble() * 8;
    }

    @Override
    protected void animate() {
        flapClock += FLAPS_PER_SECOND * dt();
    }

    @Override
    protected void render(Graphics2D g, int screenX, int screenY) {
        double daylight = cycle.getDaylight();
        if (alpha.index(daylight) == 0)
            return;
        Composite original = g.getComposite();
        if (daylight < 0.99)
            g.setComposite(alpha.get(daylight));
        g.drawImage(frames[FLAP_CYCLE[(int) flapClock % FLAP_CYCLE.length]], screenX, screenY, null);
        g.setComposite(original);
    }
}
