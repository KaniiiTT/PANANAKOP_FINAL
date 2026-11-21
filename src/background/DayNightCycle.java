package background;

import java.awt.Color;
import java.util.ArrayList;

/**
 * The clock of the backdrop: a time of day that loops forever, and the "mood"
 * (tint colour and how dark it is) that belongs to every moment of the day.
 *
 * The day is described by a short list of keyframes (day, sunset, night, dawn).
 * Between two keyframes the mood is blended smoothly, so the sky changes
 * gradually over several minutes. Layers do not know anything about time; they
 * ask this class how dark it is and react (stars appear, butterflies leave...).
 *
 * The time of day is a fraction: 0.0 = morning, 0.46 = end of the afternoon,
 * 0.72 = night, 1.0 = the next morning again.
 */
public class DayNightCycle {
    /** Fraction of the day at which the moon starts to rise. */
    private static final double MOON_RISE = 0.56;

    /** One moment of the day. Colours are stored multiplied by their strength so blending stays natural. */
    private static final class MoodKey {
        private final double time;
        private final double red;
        private final double green;
        private final double blue;
        private final double strength; // opacity of the tint, 0 = no tint at all
        private final double night;    // 0 = full daylight, 1 = full night

        private MoodKey(double time, int r, int g, int b, double strength, double night) {
            this.time = time;
            this.red = r * strength;
            this.green = g * strength;
            this.blue = b * strength;
            this.strength = strength;
            this.night = night;
        }
    }

    private final ArrayList<MoodKey> keys = new ArrayList<>();
    private final double fractionPerUpdate;
    private double timeOfDay;

    // Current mood, recomputed on every update.
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

        // Daytime has no tint at all, so the daytime scene looks exactly like the original art.
        keys.add(new MoodKey(0.00, 0, 0, 0, 0.00, 0.00));       // morning
        keys.add(new MoodKey(0.46, 0, 0, 0, 0.00, 0.00));       // end of the long day
        keys.add(new MoodKey(0.56, 255, 128, 60, 0.30, 0.10));  // sunset orange
        keys.add(new MoodKey(0.64, 110, 60, 120, 0.36, 0.55));  // purple twilight
        keys.add(new MoodKey(0.72, 12, 20, 66, 0.56, 1.00));    // night blue
        keys.add(new MoodKey(0.90, 12, 20, 66, 0.56, 1.00));    // deep night
        keys.add(new MoodKey(0.95, 255, 160, 110, 0.26, 0.35)); // dawn pink
        keys.add(new MoodKey(1.00, 0, 0, 0, 0.00, 0.00));       // morning again
        setTimeOfDay(startFraction);
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

    /** Blends the two keyframes around the current time (a linear scan; the list is tiny). */
    private void recompute() {
        int i = 0;
        while (i < keys.size() - 2 && timeOfDay >= keys.get(i + 1).time)
            i++;
        MoodKey from = keys.get(i);
        MoodKey to = keys.get(i + 1);

        double t = (timeOfDay - from.time) / (to.time - from.time);
        t = t * t * (3 - 2 * t); // ease in and out

        double strength = lerp(from.strength, to.strength, t);
        night = lerp(from.night, to.night, t);

        int r = 0;
        int g = 0;
        int b = 0;
        if (strength > 0.001) {
            r = clamp255(lerp(from.red, to.red, t) / strength);
            g = clamp255(lerp(from.green, to.green, t) / strength);
            b = clamp255(lerp(from.blue, to.blue, t) / strength);
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
