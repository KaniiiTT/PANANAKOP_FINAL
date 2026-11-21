package background;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import main.Game;

/**
 * A butterfly that wanders over the bushes: it keeps picking a new direction
 * every second or two, turns smoothly towards it and stays inside a band of
 * the screen. It never becomes inactive; it only wraps around horizontally.
 */
public class Butterfly extends AmbientCreature {
    private static final int[] FLAP_CYCLE = { 0, 1, 2, 1 };
    private static final double FLAPS_PER_SECOND = 8.0;
    private static final double TURN_RATE = 2.5; // how quickly the heading eases to its target

    private final BufferedImage[] frames;
    private final Random random;
    private final double minY;
    private final double maxY;
    private final double speed; // pixels per second
    private double heading;     // radians, 0 = right
    private double targetHeading;
    private double secondsUntilTurn;
    private double flapClock;

    public Butterfly(BufferedImage[] frames, double parallaxFactor, double secondsPerUpdate, Random random,
            double speed, double minY, double maxY) {
        super(parallaxFactor, true, secondsPerUpdate);
        this.frames = frames;
        this.random = random;
        this.speed = speed;
        this.minY = minY;
        this.maxY = maxY;
        this.heading = random.nextDouble() * Math.PI * 2;
        this.targetHeading = heading;
        this.flapClock = random.nextDouble() * 8;
        placeOnScreen(random.nextDouble() * Game.GAME_WIDTH, minY + random.nextDouble() * (maxY - minY), 0);
        setActive(true);
    }

    @Override
    public void update() {
        secondsUntilTurn -= dt();
        if (secondsUntilTurn <= 0) {
            chooseNewTarget();
            secondsUntilTurn = 0.8 + random.nextDouble() * 1.7;
        }
        double difference = Math.atan2(Math.sin(targetHeading - heading), Math.cos(targetHeading - heading));
        heading += difference * Math.min(1.0, TURN_RATE * dt());
        flapClock += FLAPS_PER_SECOND * dt();
        // Butterflies drift sideways more than they climb.
        moveBy(Math.cos(heading) * speed * dt(), Math.sin(heading) * speed * 0.55 * dt());
    }

    /** Random direction, but pushed back down or up when the butterfly nears the edge of its band. */
    private void chooseNewTarget() {
        if (getY() < minY)
            targetHeading = random.nextDouble() * Math.PI;           // downwards
        else if (getY() > maxY)
            targetHeading = -random.nextDouble() * Math.PI;          // upwards
        else
            targetHeading = random.nextDouble() * Math.PI * 2;
    }

    @Override
    protected void render(Graphics2D g, int screenX, int screenY) {
        g.drawImage(frames[FLAP_CYCLE[(int) flapClock % FLAP_CYCLE.length]], screenX, screenY, null);
    }
}
