package background;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import main.Game;

/**
 * Abstract base of the creatures that cross the sky from one side to the other
 * (birds by day, bats by night).
 *
 * The base class does everything they have in common: it flies in a straight
 * line at a speed, flaps through its wing frames, and becomes inactive once it
 * has left the view. A subclass only describes its own style of flying:
 * how fast it flaps, which frames it shows and how it wobbles up and down.
 */
public abstract class FlyingCreature extends AmbientCreature {
    private final BufferedImage[] frames;
    private int direction; // 1 = flying right, -1 = flying left
    private double speed;  // pixels per second
    private double baseY;
    private double flightClock; // seconds since take-off (plus a random start)
    private double flapClock;

    protected FlyingCreature(BufferedImage[] frames, double parallaxFactor, double secondsPerUpdate) {
        super(parallaxFactor, false, secondsPerUpdate);
        this.frames = frames;
    }

    /** Order in which the wing frames are shown. */
    protected abstract int[] flapCycle();

    /** Wing frames shown per second. */
    protected abstract double flapsPerSecond();

    /** Vertical wobble in pixels, relative to the flight line, at the given moment of the flight. */
    protected abstract double verticalOffset(double flightClock);

    /** Multiplier of the flight speed at the given moment; 1 = steady. */
    protected double speedFactor(double flightClock) {
        return 1.0;
    }

    public final int getSpriteWidth() {
        return frames[0].getWidth();
    }

    /**
     * @param screenX    where the creature appears (just outside the view)
     * @param flapOffset random starting point of the wing cycle
     * @param startClock random starting moment of the wobble
     */
    public final void launch(int direction, double screenX, double y, double speed, double flapOffset,
            double startClock, int cameraX) {
        this.direction = direction;
        this.speed = speed;
        this.baseY = y;
        this.flightClock = startClock;
        this.flapClock = flapOffset;
        placeOnScreen(screenX, y, cameraX);
        setActive(true);
    }

    @Override
    public final void update() {
        flightClock += dt();
        flapClock += flapsPerSecond() * dt();
        moveBy(direction * speed * speedFactor(flightClock) * dt(), 0);
        setY(baseY + verticalOffset(flightClock));

        double sx = screenX();
        boolean leftTheView = (direction > 0 && sx > Game.GAME_WIDTH + WRAP_MARGIN)
                || (direction < 0 && sx < -getSpriteWidth() - WRAP_MARGIN);
        // The camera can also carry a creature far behind its flight direction.
        boolean leftBehind = sx < -3.0 * Game.GAME_WIDTH || sx > 4.0 * Game.GAME_WIDTH;
        if (leftTheView || leftBehind)
            setActive(false);
    }

    @Override
    protected final void render(Graphics2D g, int screenX, int screenY) {
        int[] cycle = flapCycle();
        g.drawImage(frames[cycle[(int) flapClock % cycle.length]], screenX, screenY, null);
    }
}
