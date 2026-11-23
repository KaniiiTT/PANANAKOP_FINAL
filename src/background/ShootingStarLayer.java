package background;

import main.Game;

/**
 * Now and then a shooting star crosses the night sky. Streaks only start when
 * it is fully dark and come at random intervals; there are never more than the
 * pool holds. By day the layer is invisible.
 */
public class ShootingStarLayer extends AmbientLayer {
    private static final int POOL_SIZE = 2;

    private final DayNightCycle cycle;
    private double secondsUntilNext;

    public ShootingStarLayer(DayNightCycle cycle, double updatesPerSecond) {
        super(updatesPerSecond);
        this.cycle = cycle;
        var tail = ShootingStar.createTail();
        int pixel = PixelSprites.pixelUnit();
        for (int i = 0; i < POOL_SIZE; i++)
            add(new ShootingStar(tail, pixel, dt()));
        secondsUntilNext = 4 + random.nextDouble() * 6;
    }

    @Override
    protected double opacity() {
        return cycle.getNightness();
    }

    @Override
    protected void spawn() {
        if (cycle.getNightness() < 0.85)
            return;
        secondsUntilNext -= dt();
        if (secondsUntilNext > 0)
            return;
        secondsUntilNext = 15 + random.nextDouble() * 25;

        ShootingStar star = findInactive(ShootingStar.class);
        if (star == null)
            return;
        int w = Game.GAME_WIDTH;
        int h = Game.GAME_HEIGHT;
        boolean toTheRight = random.nextBoolean();
        double slope = Math.toRadians(18 + random.nextDouble() * 22); // shallow dive
        double angle = toTheRight ? slope : Math.PI - slope;
        star.launch(w * (0.10 + 0.80 * random.nextDouble()), h * (0.03 + 0.20 * random.nextDouble()), angle,
                w * (0.50 + 0.25 * random.nextDouble()), 0.9 + 0.6 * random.nextDouble(), getCameraX());
    }
}
