package background;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import utilz.SoundManager;

/**
 * The sound of the world: wind in the leaves, birds by day and crickets by night.
 *
 * Like the game's sound effects, the three sounds are synthesized in code, so
 * no audio files are needed. Each one is a short seamless loop that is made once
 * and then plays continuously; the only thing that changes while playing is
 * its volume, which follows the {@link DayNightCycle}:
 * <ul>
 * <li>wind: always, a little stronger at night;</li>
 * <li>birds: loudest in full daylight, fading out through the evening;</li>
 * <li>crickets: fading in at dusk, loudest in the night, fading out at dawn.</li>
 * </ul>
 * Because the volumes are read from the same smooth day-night curve, the sounds
 * change gradually, never suddenly. The overall volume and the mute switch are
 * those of {@link SoundManager}.
 *
 * The loops stop by themselves when {@link #update()} is no longer called (game
 * paused, menu, window in the background) and start again when it is.
 */
public final class AmbientSoundscape {
    static final int SAMPLE_RATE = 22050;

    private static final double WIND_SECONDS = 12;
    private static final double BIRD_SECONDS = 16;
    private static final double CRICKET_SECONDS = 8;

    // Loudness of each sound at full level, relative to the full-scale (and quieter than the effects).
    private static final float WIND_MIX = 0.40f;
    private static final float BIRD_MIX = 0.50f;
    private static final float CRICKET_MIX = 0.30f;

    private static final int GAIN_UPDATE_EVERY = 10; // updates between volume adjustments (20 per second)
    private static final long SILENCE_AFTER_NANOS = 500_000_000L;

    private static ScheduledExecutorService watchdog;

    /** One looping sound and the control that sets its volume. */
    private static final class Loop {
        final Clip clip;
        final FloatControl gain;
        float lastDb = Float.NaN;

        Loop(byte[] samples) throws Exception {
            clip = AudioSystem.getClip();
            clip.open(new AudioFormat(SAMPLE_RATE, 16, 1, true, false), samples, 0, samples.length);
            gain = clip.isControlSupported(FloatControl.Type.MASTER_GAIN)
                    ? (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)
                    : null;
            setLevel(0f);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }

        void setLevel(float level) {
            if (gain == null)
                return;
            float db = level < 0.002f ? gain.getMinimum() : (float) (20 * Math.log10(level));
            db = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
            if (Math.abs(db - lastDb) < 0.4f && !Float.isNaN(lastDb))
                return;
            lastDb = db;
            gain.setValue(db);
        }
    }

    private final DayNightCycle cycle;
    private volatile Loop wind;
    private volatile Loop birds;
    private volatile Loop crickets;
    private volatile boolean opening;
    private boolean running;
    private long lastUpdateNanos;
    private int tick;

    public AmbientSoundscape(DayNightCycle cycle) {
        this.cycle = cycle;
    }

    /** Call once per game update. The first call starts building the sounds on a helper thread. */
    public void update() {
        lastUpdateNanos = System.nanoTime();
        if (wind == null) {
            if (!opening)
                open();
            return;
        }
        synchronized (this) {
            if (!running)
                resume();
            if (++tick >= GAIN_UPDATE_EVERY) {
                tick = 0;
                applyLevels();
            }
        }
    }

    private void open() {
        opening = true;
        Thread builder = new Thread(() -> {
            try {
                Loop w = new Loop(windSamples());
                Loop b = new Loop(birdSamples());
                Loop c = new Loop(cricketSamples());
                synchronized (AmbientSoundscape.this) {
                    wind = w;
                    birds = b;
                    crickets = c;
                    running = true;
                }
                startWatchdog(this);
            } catch (Exception | LinkageError noAudio) {
                // No audio hardware: the game simply stays quiet.
            }
        }, "ambient-sound-builder");
        builder.setDaemon(true);
        builder.start();
    }

    private synchronized void applyLevels() {
        float master = SoundManager.isMuted() ? 0f : SoundManager.getVolume();
        double night = cycle.getNightness();
        wind.setLevel((float) (WIND_MIX * (0.75 + 0.25 * night)) * master);
        // The birds go quiet as the evening comes, the crickets take over at dusk.
        birds.setLevel((float) (BIRD_MIX * (1 - smooth(night, 0.10, 0.55))) * master);
        crickets.setLevel((float) (CRICKET_MIX * smooth(night, 0.35, 0.85)) * master);
    }

    private static double smooth(double value, double from, double to) {
        double t = Math.max(0, Math.min(1, (value - from) / (to - from)));
        return t * t * (3 - 2 * t);
    }

    private void resume() {
        wind.clip.loop(Clip.LOOP_CONTINUOUSLY);
        birds.clip.loop(Clip.LOOP_CONTINUOUSLY);
        crickets.clip.loop(Clip.LOOP_CONTINUOUSLY);
        running = true;
        applyLevels();
    }

    private synchronized void pauseIfIdle() {
        if (!running || System.nanoTime() - lastUpdateNanos < SILENCE_AFTER_NANOS)
            return;
        wind.clip.stop();
        birds.clip.stop();
        crickets.clip.stop();
        running = false;
    }

    private static synchronized void startWatchdog(AmbientSoundscape soundscape) {
        if (watchdog == null)
            watchdog = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread t = new Thread(task, "ambient-sound-watchdog");
                t.setDaemon(true);
                return t;
            });
        watchdog.scheduleWithFixedDelay(soundscape::pauseIfIdle, 300, 300, TimeUnit.MILLISECONDS);
    }

    // ------------------------------------------------------------------ synthesis

    /** Wind: a slow rumble that swells and fades in gusts, and a leaf rustle riding on the gusts. */
    static byte[] windSamples() {
        int n = (int) (WIND_SECONDS * SAMPLE_RATE);
        Random r = new Random(7);
        double[] noise = new double[n];
        for (int i = 0; i < n; i++)
            noise[i] = r.nextDouble() * 2 - 1;
        double[] rumble = circularLowPass(circularLowPass(noise, 0.08), 0.16);
        double[] bass = circularLowPass(rumble, 0.012);
        for (int i = 0; i < n; i++)
            rumble[i] -= bass[i]; // no deep boom: wind in leaves is a soft hush
        double[] soft = circularLowPass(noise, 0.30);
        double[] band = new double[n];
        for (int i = 0; i < n; i++)
            band[i] = noise[i] - soft[i];
        double[] hiss = circularLowPass(circularLowPass(band, 0.45), 0.6); // rustle: 1 to 3 kHz, not a harsh white hiss
        double[] flutter = circularLowPass(circularLowPass(noise, 0.0012), 0.0012); // changes a few times a second
        double ph1 = r.nextDouble() * 6.28;
        double ph2 = r.nextDouble() * 6.28;
        normalise(rumble, 1);
        normalise(flutter, 1);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double gust = 0.42 + 0.34 * Math.sin(2 * Math.PI * t + ph1) + 0.20 * Math.sin(2 * Math.PI * 3 * t + ph2);
            gust = Math.max(0.08, Math.min(1, gust));
            double rustle = hiss[i] * 2.4 * Math.pow(gust, 2) * Math.pow(0.5 + 0.5 * flutter[i], 2);
            out[i] = rumble[i] * gust * 0.75 + rustle * 1.0;
        }
        normalise(out, 0.9);
        return toBytes(out);
    }

    /** Crickets: several insects chirping at slightly different pitches and rhythms. */
    static byte[] cricketSamples() {
        int n = (int) (CRICKET_SECONDS * SAMPLE_RATE);
        double[] out = new double[n];
        // pitch (Hz), seconds between chirps (a whole number of them fits the loop), start offset, volume
        double[][] voices = { { 4300, 0.50, 0.00, 1.0 }, { 4650, 0.40, 0.13, 0.8 }, { 3950, 0.80, 0.31, 0.9 },
                { 5000, 0.32, 0.07, 0.6 } };
        for (double[] v : voices) {
            int period = (int) Math.round(v[1] * SAMPLE_RATE);
            int pulses = 4;
            int pulseLength = (int) (0.020 * SAMPLE_RATE);
            int pulseGap = (int) (0.030 * SAMPLE_RATE);
            for (int start = (int) (v[2] * SAMPLE_RATE); start < n; start += period)
                for (int p = 0; p < pulses; p++) {
                    int from = start + p * pulseGap;
                    for (int i = 0; i < pulseLength; i++) {
                        double env = Math.sin(Math.PI * i / pulseLength);
                        out[(from + i) % n] += env * env * v[3] * Math.sin(2 * Math.PI * v[0] * i / SAMPLE_RATE);
                    }
                }
        }
        normalise(out, 0.85);
        return toBytes(out);
    }

    /** Birds: a few different calls scattered over the loop, with a little echo. */
    static byte[] birdSamples() {
        int n = (int) (BIRD_SECONDS * SAMPLE_RATE);
        Random r = new Random(23);
        double[] out = new double[n];
        double time = 0.6;
        while (time < BIRD_SECONDS - 1.6) {
            addCall(out, (int) (time * SAMPLE_RATE), r.nextInt(4), 0.55 + 0.45 * r.nextDouble());
            time += 1.3 + r.nextDouble() * 1.7;
        }
        // an echo among the trees
        int delay = (int) (0.09 * SAMPLE_RATE);
        double[] echoed = new double[n];
        for (int i = 0; i < n; i++)
            echoed[i] = out[i] + 0.28 * out[(i - delay + n) % n];
        normalise(echoed, 0.85);
        return toBytes(echoed);
    }

    private static void addCall(double[] out, int start, int species, double volume) {
        int n = out.length;
        switch (species) {
        case 0: // "tew-tew-tew": three short rising notes
            for (int k = 0; k < 3; k++)
                addNote(out, start + k * (int) (0.12 * SAMPLE_RATE), 0.085, 3300, 4300, 0, 0, volume);
            break;
        case 1: // a warble with vibrato
            addNote(out, start, 0.55, 2900, 3300, 28, 260, volume * 0.8);
            break;
        case 2: // two falling whistles
            addNote(out, start, 0.26, 4300, 2700, 0, 0, volume);
            addNote(out, start + (int) (0.34 * SAMPLE_RATE), 0.26, 4200, 2600, 0, 0, volume * 0.85);
            break;
        default: // "chi-chi-chi-chirrup": quick high notes
            for (int k = 0; k < 5; k++)
                addNote(out, start + k * (int) (0.06 * SAMPLE_RATE), 0.04, 5200 - k * 120, 5600 - k * 120, 0, 0,
                        volume * 0.7);
            addNote(out, start + (int) (0.34 * SAMPLE_RATE), 0.18, 3400, 4700, 0, 0, volume * 0.8);
            break;
        }
    }

    private static void addNote(double[] out, int start, double seconds, double hzFrom, double hzTo, double vibratoHz,
            double vibratoDepth, double volume) {
        int n = out.length;
        int length = (int) (seconds * SAMPLE_RATE);
        double phase = 0;
        for (int i = 0; i < length; i++) {
            double t = i / (double) length;
            double hz = hzFrom + (hzTo - hzFrom) * t;
            if (vibratoHz > 0)
                hz += vibratoDepth * Math.sin(2 * Math.PI * vibratoHz * i / SAMPLE_RATE);
            phase += 2 * Math.PI * hz / SAMPLE_RATE;
            double env = Math.sin(Math.PI * t);
            env = Math.pow(env, 0.7);
            out[(start + i) % n] += volume * env * Math.sin(phase);
        }
    }

    /** A one-pole low-pass filter run around the loop, so the end of the loop flows into its start. */
    private static double[] circularLowPass(double[] x, double a) {
        int n = x.length;
        double[] y = new double[n];
        double state = 0;
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i < n; i++) {
                state += a * (x[i] - state);
                if (pass == 1)
                    y[i] = state;
            }
        return y;
    }

    private static void normalise(double[] x, double peak) {
        double max = 1e-9;
        for (double v : x)
            max = Math.max(max, Math.abs(v));
        for (int i = 0; i < x.length; i++)
            x[i] = x[i] / max * peak;
    }

    private static byte[] toBytes(double[] x) {
        byte[] data = new byte[x.length * 2];
        for (int i = 0; i < x.length; i++) {
            short s = (short) Math.round(Math.max(-1, Math.min(1, x[i])) * 32000);
            data[i * 2] = (byte) s;
            data[i * 2 + 1] = (byte) (s >> 8);
        }
        return data;
    }
}
