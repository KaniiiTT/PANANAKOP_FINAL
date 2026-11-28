package utilz;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import audio.AudioEngine;
import audio.Encounter;
import audio.MusicDirector;
import audio.MusicZones;

/**
 * The game's one audio facade. Gameplay code only calls the simple static methods below.
 * <ul>
 * <li>Music (Audio Pack, Task 1) is delegated to the {@code audio} package: {@link AudioEngine} plays it and
 * {@link MusicDirector} decides what should play.</li>
 * <li>The small generated sound effects below are the original ones and are untouched.</li>
 * </ul>
 */
public final class SoundManager {
	private static final int SAMPLE_RATE = 22050;
	private static final Map<Effect, Clip> CLIPS = new EnumMap<>(Effect.class);
	private static volatile boolean muted;
	private static volatile float volume = 0.7f;

	/*
	 * Menu mouse events are delivered on Swing's Event Dispatch Thread. Opening a
	 * Java Sound Clip can block while Windows searches for an audio device, so
	 * legacy generated SFX must never be opened from the UI thread.
	 */
	private static final ExecutorService SFX_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "PANANAKOP-sfx");
		t.setDaemon(true);
		return t;
	});

	private static AudioEngine engine;
	private static MusicDirector music;
	private static boolean musicMuted;
	private static boolean pausedState;
	private static boolean shutdownHookAdded;

	private SoundManager() {}

	// ------------------------------------------------------------------ music (audio pack)

	private static synchronized MusicDirector music() {
		if (music == null) {
			engine = AudioEngine.createDefault();
			engine.setMasterVolume(volume);
			engine.setBusMuted(audio.Bus.MUSIC, musicMuted);
			engine.setBusMuted(audio.Bus.AMBIENCE, muted);
			engine.setBusMuted(audio.Bus.SFX, muted);
			music = new MusicDirector(engine, MusicZones.defaultWorld());
			if (!shutdownHookAdded) {
				shutdownHookAdded = true;
				Runtime.getRuntime().addShutdownHook(new Thread(SoundManager::shutdown, "PANANAKOP-audio-shutdown"));
			}
		}
		return music;
	}

	/** Main menu, character select and options screens. Cheap to call every update. */
	public static void musicMenu() {
		pausedState = false;
		music().enterMenu();
	}

	/** Call every gameplay update with the current map index, the player's tile column and the frame time. */
	public static void musicWorldUpdate(int levelIndex, int playerTileX, float dt) {
		music().updateExploration(levelIndex, playerTileX, dt);
	}

	public static void musicBossEngaged(boolean finalBoss) {
		music().beginEncounter(finalBoss ? Encounter.FINAL_BOSS : Encounter.MINI_BOSS);
	}

	public static void musicBossEnded() {
		music().endEncounter();
	}

	public static void musicGameOver() {
		music().gameOver();
	}

	/** Pause menu open/close; only acts when the state really changes. */
	public static void musicPaused(boolean paused) {
		if (paused == pausedState)
			return;
		pausedState = paused;
		music().setPaused(paused);
	}

	/** Freezes / resumes all audio when the game window loses / regains focus. */
	public static void windowFocusChanged(boolean focused) {
		music();
		engine.setSuspended(!focused);
	}

	public static synchronized void setMusicMuted(boolean value) {
		musicMuted = value;
		if (engine != null)
			engine.setBusMuted(audio.Bus.MUSIC, value);
	}

	public static synchronized boolean isMusicMuted() {
		return musicMuted;
	}

	/** The ambience that is audible right now (null = none). For diagnostics and tests. */
	public static String currentAmbience() {
		return engine == null ? null : engine.currentAmbience();
	}

	/** The track that is audible right now (null = none). For diagnostics and tests. */
	public static String currentMusic() {
		return engine == null ? null : engine.currentMusic();
	}

	/** The underlying engine (null until first use). For diagnostics and tests only. */
	public static synchronized AudioEngine audioEngine() {
		return engine;
	}

	/** Stops the audio thread and releases the sound card and all loaded audio. Safe to call repeatedly. */
	public static synchronized void shutdown() {
		if (engine != null) {
			engine.shutdown();
			engine = null;
			music = null; // a later call (e.g. another test run in the editor) builds a fresh engine
			pausedState = false;
		}
	}

	public enum Effect { JUMP, ATTACK, HIT, PICKUP, BOUNCE, TRAP, MENU }

	public static void play(Effect effect) {
		if (effect == null || muted)
			return;

		// Do not open or start a Clip on the Swing EDT (menu clicks run there).
		// Audio device discovery can block on some Windows drivers.
		try {
			SFX_EXECUTOR.execute(() -> playNow(effect));
		} catch (java.util.concurrent.RejectedExecutionException ignored) {
			// The game is shutting down; dropping a short UI/gameplay sound is safe.
		}
	}

	private static synchronized void playNow(Effect effect) {
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

	/** The sound-effects button: mutes the effects and, as the old ambient sounds did, the environment ambience. */
	public static synchronized void setMuted(boolean value) {
		muted = value;
		if (engine != null) {
			engine.setBusMuted(audio.Bus.AMBIENCE, value);
			engine.setBusMuted(audio.Bus.SFX, value);
		}
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
		if (engine != null)
			engine.setMasterVolume(volume);
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
