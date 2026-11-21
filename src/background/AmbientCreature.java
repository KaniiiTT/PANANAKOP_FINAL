package background;

import java.awt.Graphics2D;

import main.Game;

/**
 * Abstract base of every small living thing in the backdrop (birds,
 * butterflies, leaves, pollen).
 *
 * The base class keeps the shared state (position, depth, active flag) and the
 * shared parallax rule: screenX = x - cameraX * parallaxFactor. A subclass only
 * describes how it moves ({@link #update()}) and how it looks
 * ({@link #render(Graphics2D, int, int)}).
 *
 * Creatures are created once and then recycled: instead of being created and
 * thrown away, an inactive creature waits until its layer launches it again.
 */
public abstract class AmbientCreature {
    /** Extra room outside the view so wrapped creatures leave and re-enter smoothly. */
    public static final int WRAP_MARGIN = 64;

    private final double parallaxFactor;
    private final boolean wrapsHorizontally;
    private final double secondsPerUpdate;
    private double x;
    private double y;
    private boolean active;
    private int cameraX; // camera seen by the last draw, used to reason about what is on screen

    protected AmbientCreature(double parallaxFactor, boolean wrapsHorizontally, double secondsPerUpdate) {
        this.parallaxFactor = parallaxFactor;
        this.wrapsHorizontally = wrapsHorizontally;
        this.secondsPerUpdate = secondsPerUpdate;
    }

    /** Moves the creature by one game update. Called only while it is active. */
    public abstract void update();

    /** Paints the creature at the given screen position. */
    protected abstract void render(Graphics2D g, int screenX, int screenY);

    public final void draw(Graphics2D g, int cameraX) {
        this.cameraX = cameraX;
        if (!active)
            return;
        double sx = wrapsHorizontally ? wrappedScreenX() : screenX();
        render(g, (int) Math.round(sx), (int) Math.round(y));
    }

    /** Position on screen with the parallax shift applied. */
    protected final double screenX() {
        return x - cameraX * parallaxFactor;
    }

    /** Like {@link #screenX()}, but looped so the creature never leaves the wrap field. */
    private double wrappedScreenX() {
        double field = Game.GAME_WIDTH + 2.0 * WRAP_MARGIN;
        double v = screenX() + WRAP_MARGIN;
        return v - Math.floor(v / field) * field - WRAP_MARGIN;
    }

    /** Puts the creature at a screen position as seen with the given camera. */
    protected final void placeOnScreen(double screenX, double screenY, int cameraX) {
        this.cameraX = cameraX;
        this.x = screenX + cameraX * parallaxFactor;
        this.y = screenY;
    }

    protected final double getX() {
        return x;
    }

    protected final double getY() {
        return y;
    }

    protected final void setY(double y) {
        this.y = y;
    }

    protected final void moveBy(double dx, double dy) {
        x += dx;
        y += dy;
    }

    /** Length of one game update in seconds, so speeds can be written in pixels per second. */
    protected final double dt() {
        return secondsPerUpdate;
    }

    public final boolean isActive() {
        return active;
    }

    protected final void setActive(boolean active) {
        this.active = active;
    }
}
