package background;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;

/**
 * A background layer made of {@link AmbientCreature}s.
 *
 * It owns a pool: an {@code ArrayList} filled once with every creature the
 * layer will ever use. Each update only the active ones move, and
 * {@link #spawn()} may wake inactive ones, so nothing is allocated or removed
 * while playing. Creatures are drawn in list order, so the order they are
 * added in is their front-to-back order.
 */
public abstract class AmbientLayer implements BackgroundLayer {
    private final ArrayList<AmbientCreature> creatures = new ArrayList<>();
    private final double secondsPerUpdate;
    private int cameraX;
    protected final Random random = new Random();

    protected AmbientLayer(double updatesPerSecond) {
        this.secondsPerUpdate = 1.0 / updatesPerSecond;
    }

    protected final void add(AmbientCreature creature) {
        creatures.add(creature);
    }

    /** Called once per update, after the creatures moved: wake creatures up here. */
    protected abstract void spawn();

    @Override
    public final void update() {
        for (AmbientCreature creature : creatures)
            if (creature.isActive())
                creature.update();
        spawn();
    }

    @Override
    public final void draw(Graphics2D g, int cameraX) {
        this.cameraX = cameraX;
        for (AmbientCreature creature : creatures)
            creature.draw(g, cameraX);
    }

    /** Camera offset seen by the most recent draw. */
    protected final int getCameraX() {
        return cameraX;
    }

    protected final double dt() {
        return secondsPerUpdate;
    }

    /** First inactive creature of the given type in the pool, or null if all are busy. */
    protected final <T extends AmbientCreature> T findInactive(Class<T> type) {
        for (AmbientCreature creature : creatures)
            if (!creature.isActive() && type.isInstance(creature))
                return type.cast(creature);
        return null;
    }
}
