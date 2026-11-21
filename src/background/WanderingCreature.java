package background;

import java.util.Random;

import main.Game;

/**
 * A creature that wanders aimlessly inside a horizontal band of the screen
 * (butterflies by day, fireflies by night).
 *
 * It keeps picking a new direction every second or two, turns smoothly towards
 * it, and is pushed back when it nears the top or bottom of its band. It never
 * becomes inactive; it only wraps around horizontally. Subclasses add how it
 * looks and what its own animation does ({@link #animate()}).
 */
public abstract class WanderingCreature extends AmbientCreature {
    private static final double TURN_RATE = 2.5; // how quickly the heading eases to its target

    private final Random random;
    private final double minY;
    private final double maxY;
    private final double speed; // pixels per second
    private double heading;     // radians, 0 = right
    private double targetHeading;
    private double secondsUntilTurn;

    protected WanderingCreature(double parallaxFactor, double secondsPerUpdate, Random random, double speed,
            double minY, double maxY) {
        super(parallaxFactor, true, secondsPerUpdate);
        this.random = random;
        this.speed = speed;
        this.minY = minY;
        this.maxY = maxY;
        this.heading = random.nextDouble() * Math.PI * 2;
        this.targetHeading = heading;
        placeOnScreen(random.nextDouble() * Game.GAME_WIDTH, minY + random.nextDouble() * (maxY - minY), 0);
        setActive(true);
    }

    /** Advances the creature's own animation (wing flap, glow blink...). */
    protected abstract void animate();

    @Override
    public final void update() {
        secondsUntilTurn -= dt();
        if (secondsUntilTurn <= 0) {
            chooseNewTarget();
            secondsUntilTurn = 0.8 + random.nextDouble() * 1.7;
        }
        double difference = Math.atan2(Math.sin(targetHeading - heading), Math.cos(targetHeading - heading));
        heading += difference * Math.min(1.0, TURN_RATE * dt());
        // They drift sideways more than they climb.
        moveBy(Math.cos(heading) * speed * dt(), Math.sin(heading) * speed * 0.55 * dt());
        animate();
    }

    /** Random direction, but pushed back down or up when the creature nears the edge of its band. */
    private void chooseNewTarget() {
        if (getY() < minY)
            targetHeading = random.nextDouble() * Math.PI;           // downwards
        else if (getY() > maxY)
            targetHeading = -random.nextDouble() * Math.PI;          // upwards
        else
            targetHeading = random.nextDouble() * Math.PI * 2;
    }
}
