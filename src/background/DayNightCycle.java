package background;

import java.awt.Color;

/**
 * The clock of the backdrop: a time of day that loops forever, and the "mood"
 * (tint colour and how dark it is) that belongs to every moment of the day.
 *
 * The day is described by a short list of keyframes (day, golden hour, sunset,
 * dusk, night, pre-dawn, dawn). From them the class builds a smooth curve ONCE:
 * the keyframes are laid out in time, then the whole loop is smoothed so the
 * sky drifts from one mood into the next with no sudden steps and no pauses
 * at the keyframes. While playing, the current mood is simply read from that
 * curve, so nothing heavy happens per update.
 *
 * Layers do not know anything about time; they ask this class how dark it is
 * and react (stars appear, butterflies leave...).
 *
 * The time of day is a fraction: 0.0 = early morning, 0.12 = clear day, 0.4 = the
 * afternoon starts to turn golden, 0.77 = night, 1.0 = the next morning again.
 */
public class DayNightCycle {
    /** Fraction of the day at which the moon starts to rise. */
    private static final double MOON_RISE = 0.44;

    private static final int SAMPLES = 4096;        // resolution of the mood curve
    private static final double SMOOTHING = 0.040;  // half-width of the smoothing, as a share of the day

    // One keyframe per row: time, red, green, blue, tint strength (0 = no tint), night (0 = day, 1 = night).
    // Between about 0.12 and 0.40 the day has no tint at all, so that part of the day looks exactly like
    // the original art. The first and last keyframes are equal: the day is a loop.
    private static final double[][] KEYS = {
            { 0.00, 255, 185, 120, 0.10, 0.00 },  // warm morning glow (the end of the dawn)
            { 0.08, 0, 0, 0, 0.00, 0.00 },        // clear morning
            { 0.40, 0, 0, 0, 0.00, 0.00 },        // end of the long day
            { 0.47, 255, 205, 130, 0.10, 0.00 },  // golden hour
            { 0.54, 255, 140, 80, 0.22, 0.06 },   // sunset orange
            { 0.60, 200, 95, 115, 0.28, 0.22 },   // pink
            { 0.66, 95, 60, 130, 0.36, 0.52 },    // purple dusk
            { 0.72, 28, 40, 100, 0.50, 0.90 },    // blue dusk
            { 0.77, 12, 20, 66, 0.56, 1.00 },     // night
            { 0.84, 12, 20, 66, 0.56, 1.00 },     // deep night
            { 0.89, 30, 40, 100, 0.52, 0.85 },    // first light, still dark blue
            { 0.93, 110, 80, 130, 0.38, 0.50 },   // violet
            { 0.97, 255, 160, 115, 0.24, 0.20 },  // dawn pink
            { 1.00, 255, 185, 120, 0.10, 0.00 } };// warm morning glow again

    // The mood curve, one value per sample. Colours are stored multiplied by their
    // strength, so blending a clear sky into a tint never shifts the hue.
    private final double[] strengthCurve = new double[SAMPLES];
    private final double[] redCurve = new double[SAMPLES];
    private final double[] greenCurve = new double[SAMPLES];
    private final double[] blueCurve = new double[SAMPLES];
    private final double[] nightCurve = new double[SAMPLES];

    private final double fractionPerUpdate;
    private double timeOfDay;

    // Current mood, read from the curve on every update.
    private double night;
    private Color tint = new Color(0, 0, 0, 0);
    private int tintArgb;

    /**
     * @param daySeconds       real seconds one whole day lasts
     * @param startFraction    time of day the game starts at (0 = morning)
     * @param updatesPerSecond how often update() is called by the game loop
     */
    public DayNightCycle(double daySeconds, double startFraction, double updatesPerSecond) {
        this.fractionPerUpdate = 1.0 / (daySeconds * updatesPerSecond);
        buildCurve();
        setTimeOfDay(startFraction);
    }

    /** Lays the keyframes out in time (straight lines between them), then smooths the whole loop. */
    private void buildCurve() {
        double[][] raw = new double[5][SAMPLES];
        int key = 0;
        for (int i = 0; i < SAMPLES; i++) {
            double time = i / (double) SAMPLES;
            while (key < KEYS.length - 2 && time >= KEYS[key + 1][0])
                key++;
            double[] from = KEYS[key];
            double[] to = KEYS[key + 1];
            double t = (time - from[0]) / (to[0] - from[0]);
            double strength = lerp(from[4], to[4], t);
            raw[0][i] = strength;
            raw[1][i] = lerp(from[1] * from[4], to[1] * to[4], t);
            raw[2][i] = lerp(from[2] * from[4], to[2] * to[4], t);
            raw[3][i] = lerp(from[3] * from[4], to[3] * to[4], t);
            raw[4][i] = lerp(from[5], to[5], t);
        }
        // Two passes of a moving average make a smooth, gentle curve (no corners at the
        // keyframes). The loop is treated as a circle so evening and morning join up.
        int radius = (int) Math.round(SAMPLES * SMOOTHING / 2);
        double[][] smooth = { strengthCurve, redCurve, greenCurve, blueCurve, nightCurve };
        for (int c = 0; c < raw.length; c++) {
            double[] once = movingAverage(raw[c], radius);
            double[] twice = movingAverage(once, radius);
            System.arraycopy(twice, 0, smooth[c], 0, SAMPLES);
        }
    }

    private static double[] movingAverage(double[] values, int radius) {
        int n = values.length;
        double[] result = new double[n];
        double sum = 0;
        for (int k = -radius; k <= radius; k++)
            sum += values[Math.floorMod(k, n)];
        for (int i = 0; i < n; i++) {
            result[i] = sum / (2 * radius + 1);
            sum += values[Math.floorMod(i + radius + 1, n)] - values[Math.floorMod(i - radius, n)];
        }
        return result;
    }

    public void update() {
        timeOfDay += fractionPerUpdate;
        if (timeOfDay >= 1.0)
            timeOfDay -= 1.0;
        recompute();
    }

    /** Jumps to a moment of the day (0..1; other values wrap around). */
    public final void setTimeOfDay(double fraction) {
        timeOfDay = fraction - Math.floor(fraction);
        recompute();
    }

    /** Reads the mood for the current time from the curve, blending the two nearest samples. */
    private void recompute() {
        double position = timeOfDay * SAMPLES;
        int first = (int) position;
        double t = position - first;
        int second = (first + 1) % SAMPLES;

        double strength = lerp(strengthCurve[first], strengthCurve[second], t);
        night = Math.max(0.0, Math.min(1.0, lerp(nightCurve[first], nightCurve[second], t)));

        int r = 0;
        int g = 0;
        int b = 0;
        if (strength > 0.001) {
            r = clamp255(lerp(redCurve[first], redCurve[second], t) / strength);
            g = clamp255(lerp(greenCurve[first], greenCurve[second], t) / strength);
            b = clamp255(lerp(blueCurve[first], blueCurve[second], t) / strength);
        }
        int argb = (clamp255(strength * 255) << 24) | (r << 16) | (g << 8) | b;
        // A new Color is only made when the colour really changed, not every update.
        if (argb != tintArgb) {
            tintArgb = argb;
            tint = new Color(argb, true);
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int clamp255(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }

    /** Colour (with its opacity in the alpha channel) to lay over the whole backdrop. */
    public Color getTint() {
        return tint;
    }

    /** 0 = full daylight, 1 = full night. */
    public double getNightness() {
        return night;
    }

    /** 1 = full daylight, 0 = full night. */
    public double getDaylight() {
        return 1.0 - night;
    }

    /** How far the moon has travelled across the sky, from 0 (rising) to 1 (setting). */
    public double getMoonProgress() {
        return Math.max(0.0, Math.min(1.0, (timeOfDay - MOON_RISE) / (1.0 - MOON_RISE)));
    }

    public double getTimeOfDay() {
        return timeOfDay;
    }
}
