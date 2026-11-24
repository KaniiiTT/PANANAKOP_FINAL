package utilz;

import java.util.EnumMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/** Small generated sound effects, so the game does not need external audio files. */
public final class SoundManager {
	private static final int SAMPLE_RATE = 22050;
	private static final Map<Effect, Clip> CLIPS = new EnumMap<>(Effect.class);
	private static boolean muted;
	private static float volume = 0.7f;

	private SoundManager() {}

	public enum Effect { JUMP, ATTACK, HIT, PICKUP, BOUNCE, TRAP, MENU }

	public static synchronized void play(Effect effect) {
		if (muted)
			return;
		try {
			Clip clip = CLIPS.get(effect);
			if (clip == null || !clip.isOpen()) {
				clip = AudioSystem.getClip();
				byte[] samples = createSamples(effect);
				AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
				clip.open(format, samples, 0, samples.length);
				CLIPS.put(effect, clip);
			}
			if (clip.isRunning())
				clip.stop();
			clip.setFramePosition(0);
			applyVolume(clip);
			clip.start();
		} catch (Exception ignored) {
			// Audio hardware may be unavailable; keep gameplay running silently.
		}
	}

	private static byte[] createSamples(Effect effect) {
		int durationMs = effect == Effect.HIT || effect == Effect.TRAP ? 180 : 120;
		int count = SAMPLE_RATE * durationMs / 1000;
		byte[] data = new byte[count * 2];
		float startHz = switch (effect) {
			case JUMP -> 330f;
			case ATTACK -> 190f;
			case HIT, TRAP -> 120f;
			case PICKUP -> 520f;
			case BOUNCE -> 260f;
			case MENU -> 440f;
		};
		float endHz = switch (effect) {
			case JUMP, BOUNCE, PICKUP -> startHz * 1.8f;
			case HIT, TRAP -> startHz * 0.55f;
			default -> startHz * 0.7f;
		};
		for (int i = 0; i < count; i++) {
			float progress = i / (float) count;
			float frequency = startHz + (endHz - startHz) * progress;
			float envelope = Math.min(1f, i / (SAMPLE_RATE * 0.008f)) * (1f - progress);
			float tone = (float) Math.sin(2 * Math.PI * frequency * i / SAMPLE_RATE);
			short sample = (short) (tone * envelope * 9000);
			data[i * 2] = (byte) sample;
			data[i * 2 + 1] = (byte) (sample >> 8);
		}
		return data;
	}

	public static synchronized void setMuted(boolean value) {
		muted = value;
	}

	/** Master volume (0..1) chosen in the audio options; the ambient sounds follow it too. */
	public static synchronized float getVolume() {
		return volume;
	}

	public static synchronized boolean isMuted() {
		return muted;
	}

	public static synchronized void setVolume(float value) {
		volume = Math.max(0f, Math.min(1f, value));
		for (Clip clip : CLIPS.values())
			applyVolume(clip);
	}

	private static void applyVolume(Clip clip) {
		if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
			FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			float db = volume <= 0.001f ? gain.getMinimum() : (float) (20 * Math.log10(volume));
			gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
		}
	}
}
