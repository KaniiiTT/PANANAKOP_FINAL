package background;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * A firefly: a small warm glow that slowly blinks while it wanders. It is only
 * visible at night; how strongly it shines follows the darkness of the day.
 */
public class Firefly extends WanderingCreature {
    private final DayNightCycle cycle;
    private final BufferedImage glow;
    private final AlphaSteps alpha = AlphaSteps.shared();
    private final double blinkRate;
    private double blinkPhase;

    public Firefly(DayNightCycle cycle, BufferedImage glow, double parallaxFactor, double secondsPerUpdate,
            Random random, double speed, double minY, double maxY) {
        super(parallaxFactor, secondsPerUpdate, random, speed, minY, maxY);
        this.cycle = cycle;
        this.glow = glow;
        this.blinkRate = 0.9 + random.nextDouble() * 1.2;
        this.blinkPhase = random.nextDouble() * Math.PI * 2;
    }

    @Override
    protected void animate() {
        blinkPhase += blinkRate * dt();
    }

    @Override
    protected void render(Graphics2D g, int screenX, int screenY) {
        double blink = 0.5 + 0.5 * Math.sin(blinkPhase);
        double brightness = (0.25 + 0.75 * blink * blink) * cycle.getNightness();
        if (alpha.index(brightness) == 0)
            return;
        Composite original = g.getComposite();
        g.setComposite(alpha.get(brightness));
        g.drawImage(glow, screenX - glow.getWidth() / 2, screenY - glow.getHeight() / 2, null);
        g.setComposite(original);
    }
}
