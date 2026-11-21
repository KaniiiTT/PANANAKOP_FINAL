package background;

import java.awt.AlphaComposite;

/**
 * A ready-made set of {@link AlphaComposite} objects, from fully transparent to
 * fully opaque. Layers that fade (stars, fireflies, night clouds...) pick one
 * with {@link #get(double)} instead of creating a new composite every frame.
 */
final class AlphaSteps {
    private static final AlphaSteps SHARED = new AlphaSteps(32);

    private final AlphaComposite[] levels;

    private AlphaSteps(int steps) {
        levels = new AlphaComposite[steps + 1];
        for (int i = 0; i <= steps; i++)
            levels[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) i / steps);
    }

    /** The one shared instance (32 opacity steps are invisible to the eye). */
    static AlphaSteps shared() {
        return SHARED;
    }

    /** Composite closest to the given opacity; values outside 0..1 are clamped. */
    AlphaComposite get(double opacity) {
        return levels[index(opacity)];
    }

    /** Index of the step closest to the opacity; 0 means fully transparent. */
    int index(double opacity) {
        double clamped = Math.max(0.0, Math.min(1.0, opacity));
        return (int) Math.round(clamped * (levels.length - 1));
    }
}
