package audio;

import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * Software mixer: sums every active {@link Voice} into one bus buffer per {@link Bus}, applies bus / master
 * gain, mute, ducking and a soft limiter, and writes 16-bit little-endian stereo. It is driven by an
 * {@link AudioOutput} (real sound card) or directly by tests, so all audio logic is verifiable offline.
 */
public final class AudioMixer {
	public static final int SAMPLE_RATE = 44100;
	public static final int MAX_VOICES = 24;
	private static final int CHUNK = 1024;

	private final Object lock = new Object();
	private final ArrayList<Voice> active = new ArrayList<>(MAX_VOICES);
	private final ArrayDeque<Voice> pool = new ArrayDeque<>(MAX_VOICES);

	private final float[][] busLeft = new float[Bus.VALUES.length][CHUNK];
	private final float[][] busRight = new float[Bus.VALUES.length][CHUNK];
	private final float[] busVolume = new float[Bus.VALUES.length];
	private final boolean[] busMuted = new boolean[Bus.VALUES.length];
	private final float[] busCurrent = new float[Bus.VALUES.length];
	private float master = 1f;
	private boolean masterMuted;

	private float musicDuckTarget = 1f, musicDuckCurrent = 1f;
	private boolean suspended;
	private long framesRendered;
	private float lastPeak;

	public AudioMixer() {
		for (int i = 0; i < MAX_VOICES; i++)
			pool.add(new Voice());
		for (int i = 0; i < busVolume.length; i++) {
			busVolume[i] = 1f;
			busCurrent[i] = 1f;
		}
	}

	Object lock() {
		return lock;
	}

	// ------------------------------------------------------------------ voices

	/** Starts a sound. Returns null (nothing plays) if the voice limit is hit. */
	Voice start(SoundData data, float fadeInSeconds) {
		synchronized (lock) {
			Voice v = pool.pollFirst();
			if (v == null)
				return null;
			v.start(data, fadeInSeconds, SAMPLE_RATE);
			active.add(v);
			return v;
		}
	}

	void fadeOut(Voice v, float seconds) {
		if (v == null)
			return;
		synchronized (lock) {
			v.fadeOut(seconds, SAMPLE_RATE);
		}
	}

	/**
	 * Makes every music voice that is already fading out finish within {@code seconds} (never slower than before).
	 * Keeps rapid track changes from leaving three tracks audible at once.
	 */
	void hurryFadeOuts(Kind kind, float seconds) {
		synchronized (lock) {
			for (int i = 0; i < active.size(); i++) {
				Voice v = active.get(i);
				if (v.data.kind() == kind && v.isFadingOut() && v.fade > 0f)
					v.fadeStep = Math.max(v.fadeStep, v.fade / (Math.max(0.05f, seconds) * SAMPLE_RATE));
			}
		}
	}

	public int activeVoiceCount() {
		synchronized (lock) {
			return active.size();
		}
	}

	/** Number of voices currently audible for the given sound id (including ones fading out). */
	public int voiceCountFor(String id) {
		synchronized (lock) {
			int n = 0;
			for (int i = 0; i < active.size(); i++)
				if (active.get(i).data.id().equals(id))
					n++;
			return n;
		}
	}

	/** Number of voices playing a given kind that are NOT fading out. */
	public int steadyVoiceCount(Kind kind) {
		synchronized (lock) {
			int n = 0;
			for (int i = 0; i < active.size(); i++) {
				Voice v = active.get(i);
				if (v.data.kind() == kind && !v.isFadingOut())
					n++;
			}
			return n;
		}
	}

	// ------------------------------------------------------------------ levels

	public void setMasterVolume(float v) {
		synchronized (lock) {
			master = clamp01(v);
		}
	}

	public float getMasterVolume() {
		return master;
	}

	public void setMasterMuted(boolean m) {
		synchronized (lock) {
			masterMuted = m;
		}
	}

	public boolean isMasterMuted() {
		return masterMuted;
	}

	public void setBusVolume(Bus b, float v) {
		synchronized (lock) {
			busVolume[b.ordinal()] = clamp01(v);
		}
	}

	public float getBusVolume(Bus b) {
		return busVolume[b.ordinal()];
	}

	public void setBusMuted(Bus b, boolean m) {
		synchronized (lock) {
			busMuted[b.ordinal()] = m;
		}
	}

	public boolean isBusMuted(Bus b) {
		return busMuted[b.ordinal()];
	}

	/** Quietens the music bus (1 = normal), e.g. behind the pause menu. Changes are smoothed. */
	public void setMusicDuck(float level) {
		synchronized (lock) {
			musicDuckTarget = clamp01(level);
		}
	}

	/** While suspended (window lost focus) every voice is frozen in place and silent. */
	public void setSuspended(boolean s) {
		synchronized (lock) {
			suspended = s;
		}
	}

	public boolean isSuspended() {
		return suspended;
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : Math.min(1f, v);
	}

	private float targetGain(int bus) {
		if (masterMuted || busMuted[bus])
			return 0f;
		return master * busVolume[bus];
	}

	public long framesRendered() {
		return framesRendered;
	}

	/** Peak absolute sample (0..1) of the most recent render call; handy for tests and meters. */
	public float lastPeak() {
		return lastPeak;
	}

	// ------------------------------------------------------------------ rendering

	/** Renders {@code frames} stereo frames into {@code out} (4 bytes per frame). */
	public void render(byte[] out, int frames) {
		int offset = 0;
		float peak = 0f;
		while (frames > 0) {
			int n = Math.min(frames, CHUNK);
			synchronized (lock) {
				peak = Math.max(peak, renderChunk(out, offset, n));
			}
			offset += n * 4;
			frames -= n;
		}
		lastPeak = peak;
	}

	private float renderChunk(byte[] out, int offset, int n) {
		if (suspended) {
			for (int i = 0; i < n * 4; i++)
				out[offset + i] = 0;
			return 0f;
		}
		for (int b = 0; b < busLeft.length; b++) {
			java.util.Arrays.fill(busLeft[b], 0, n, 0f);
			java.util.Arrays.fill(busRight[b], 0, n, 0f);
		}
		for (int i = 0; i < active.size(); i++) {
			Voice v = active.get(i);
			v.mix(busLeft[v.bus.ordinal()], busRight[v.bus.ordinal()], n);
		}
		// Recycle finished voices (swap-remove keeps this O(active)).
		for (int i = active.size() - 1; i >= 0; i--) {
			Voice v = active.get(i);
			if (!v.active) {
				active.set(i, active.get(active.size() - 1));
				active.remove(active.size() - 1);
				v.release();
				pool.addFirst(v);
			}
		}

		float duckStart = musicDuckCurrent;
		musicDuckCurrent += (musicDuckTarget - musicDuckCurrent) * Math.min(1f, n / (0.25f * SAMPLE_RATE));
		float peak = 0f;
		for (int i = 0; i < n; i++) {
			float t = (i + 1) / (float) n;
			float l = 0f, r = 0f;
			for (int b = 0; b < busLeft.length; b++) {
				float g0 = busCurrent[b];
				float g1 = targetGain(b);
				float g = g0 + (g1 - g0) * t;
				if (b == Bus.MUSIC.ordinal())
					g *= duckStart + (musicDuckCurrent - duckStart) * t;
				l += busLeft[b][i] * g;
				r += busRight[b][i] * g;
			}
			l = limit(l);
			r = limit(r);
			peak = Math.max(peak, Math.max(Math.abs(l), Math.abs(r)));
			int li = (int) (l * 32767f), ri = (int) (r * 32767f);
			int o = offset + i * 4;
			out[o] = (byte) li;
			out[o + 1] = (byte) (li >> 8);
			out[o + 2] = (byte) ri;
			out[o + 3] = (byte) (ri >> 8);
		}
		for (int b = 0; b < busCurrent.length; b++)
			busCurrent[b] = targetGain(b);
		framesRendered += n;
		return peak;
	}

	/** Linear below 0.85, smoothly squashed above so loud overlaps never hard-clip. */
	private static float limit(float x) {
		float a = Math.abs(x);
		if (a <= 0.85f)
			return x;
		float y = 0.85f + 0.15f * (1f - (float) Math.exp(-(a - 0.85f) / 0.15f));
		return x < 0 ? -y : y;
	}

	/** Stops everything and drops all voices. */
	public void clear() {
		synchronized (lock) {
			for (Voice v : active) {
				v.release();
				pool.addFirst(v);
			}
			active.clear();
		}
	}
}
