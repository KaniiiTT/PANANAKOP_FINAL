package background;

/**
 * Abstract base of the layers that send small flocks of {@link FlyingCreature}s
 * across the sky now and then (birds by day, bats by night).
 *
 * The layer keeps a fixed pool of creatures. A countdown timer decides when
 * the next flock takes off; a flock wakes some inactive creatures from the
 * pool, so there are never more in the sky than the pool holds. A subclass
 * fills the pool and says when flocks may launch, how often, how big they are
 * and where and how fast they fly.
 *
 * @param <T> the kind of creature this layer flocks
 */
public abstract class FlockLayer<T extends FlyingCreature> extends AmbientLayer {
    private final Class<T> type;
    private double secondsUntilNextFlock;

    protected FlockLayer(Class<T> type, double updatesPerSecond, double verticalFactor, double firstDelaySeconds) {
        super(updatesPerSecond, verticalFactor);
        this.type = type;
        this.secondsUntilNextFlock = firstDelaySeconds;
    }

    /** True while flocks may take off (the countdown waits when this is false). */
    protected abstract boolean mayLaunch();

    /** Seconds until the next flock after one has taken off. */
    protected abstract double nextFlockDelay();

    protected abstract int flockSize();

    /** Height in pixels of the flight line of a flock. */
    protected abstract double flockY();

    /** Speed in pixels per second of a flock. */
    protected abstract double flockSpeed();

    @Override
    protected final void spawn() {
        if (!mayLaunch())
            return;
        secondsUntilNextFlock -= dt();
        if (secondsUntilNextFlock > 0)
            return;
        launchFlock();
        secondsUntilNextFlock = nextFlockDelay();
    }

    private void launchFlock() {
        int count = flockSize();
        int direction = random.nextBoolean() ? 1 : -1;
        double speed = flockSpeed();
        double flockY = flockY();
        int viewWidth = main.Game.GAME_WIDTH;
        int viewHeight = main.Game.GAME_HEIGHT;

        double gap = 0;
        for (int i = 0; i < count; i++) {
            T creature = findInactive(type);
            if (creature == null)
                return;
            double startX = direction > 0
                    ? -creature.getSpriteWidth() - 8 - gap
                    : viewWidth + 8 + gap;
            double y = flockY + (random.nextDouble() - 0.5) * viewHeight * 0.05;
            creature.launch(direction, startX, y, speed * (0.95 + 0.1 * random.nextDouble()),
                    random.nextDouble() * 6, random.nextDouble() * Math.PI * 2, getCameraX());
            gap += 60 + random.nextDouble() * 50;
        }
    }
}
