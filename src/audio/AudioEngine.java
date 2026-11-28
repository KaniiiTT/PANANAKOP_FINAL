package audio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The single entry point of the audio system. It owns the library (what is loaded), the mixer (what is
 * playing) and the output (the sound card), and offers simple music and ambience operations. Game code never
 * touches files, lines or threads.
 *
 * <p>Music and ambience are two independent {@link Channel}s with the same rules:
 * <ul>
 * <li>at most one steady (non-fading) track per channel;</li>
 * <li>asking for the track that is already playing does nothing, and asking for the one that is just fading
 * out brings that same playback back instead of starting a second copy;</li>
 * <li>changing tracks crossfades; at most the outgoing track is still audible, and anything older is cut off in a
 * fraction of a second, so rapid changes can never pile up;</li>
 * <li>a missing file means silence, not a crash.</li>
 * </ul>
 */
public final class AudioEngine {
	public static final float DEFAULT_CROSSFADE = 2.0f;

	private final Object lock;
	private final AudioMixer mixer = new AudioMixer();
	private final SoundLibrary library;
	private final AudioOutput output;
	private final ExecutorService loader; // null = load on the calling thread (tests)
	private final Channel music = new Channel(Kind.MUSIC, "Music");
	private final Channel ambience = new Channel(Kind.AMBIENCE, "Ambience");

	private boolean paused;
	private float ambienceEncounterLevel = 1f;
	private volatile boolean outputOk;
	private volatile boolean shutDown;

	/** State of one looping background channel. All fields are guarded by {@link #lock}. */
	private static final class Channel {
		final Kind kind;
		final String label;
		String requestedId;   // what the game last asked for (may still be loading)
		String currentId;     // what is audible as the main track (null while loading, stopped or silent)
		Voice current;
		int currentGen;
		String previousId;    // the track on its way out
		Voice previous;
		int previousGen;
		long serial;          // bumped by every request/stop so a slow load can tell it is out of date

		Channel(Kind kind, String label) {
			this.kind = kind;
			this.label = label;
		}
	}

	public AudioEngine(AudioManifest manifest, AudioOutput output, boolean asyncLoading) {
		this.library = new SoundLibrary(manifest);
		this.output = output;
		this.lock = mixer.lock();
		this.loader = asyncLoading ? Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "PANANAKOP-audio-loader");
			t.setDaemon(true);
			return t;
		}) : null;
		mixer.setBusVolume(Bus.MUSIC, MixLevels.MUSIC_BUS);
		mixer.setBusVolume(Bus.AMBIENCE, MixLevels.AMBIENCE_BUS);
		mixer.setBusVolume(Bus.SFX, MixLevels.SFX_BUS);
		// Opening a Java Sound device can block while Windows probes output devices or drivers.
		// Never do that on the game/update thread: start the device from a daemon bootstrap thread.
		Thread starter = new Thread(() -> {
			boolean started = false;
			try {
				if (!shutDown)
					started = output.start(mixer);
			} catch (RuntimeException e) {
				System.err.println("[Audio] Output startup failed: " + e.getMessage()
						+ " - the game will continue without music.");
			} finally {
				outputOk = started;
				if (shutDown)
					output.close();
			}
		}, "PANANAKOP-audio-device-startup");
		starter.setDaemon(true);
		starter.start();
	}

	/** Normal game setup: reads the pack manifest, opens the sound card, loads in the background. */
	public static AudioEngine createDefault() {
		return new AudioEngine(AudioManifest.load(), new JavaSoundOutput(), true);
	}

	// ------------------------------------------------------------------ music

	/** Crossfades to the given track. Does nothing if that track is already the current one. */
	public void playMusic(String id, float fadeSeconds) {
		request(music, id, fadeSeconds, 1f);
	}

	/** Same, with an extra playback level (1 = as mastered); used to mix boss music a little louder. */
	public void playMusic(String id, float fadeSeconds, float level) {
		request(music, id, fadeSeconds, level);
	}

	/** Fades the current music out. A later playMusic of the same track starts it again. */
	public void stopMusic(float fadeSeconds) {
		stop(music, fadeSeconds);
	}

	/** The track the game last asked for (null if stopped). */
	public String requestedMusic() {
		synchronized (lock) {
			return music.requestedId;
		}
	}

	/** The track that is actually playing right now (null while loading, stopped or silent). */
	public String currentMusic() {
		synchronized (lock) {
			return music.currentId;
		}
	}

	// ------------------------------------------------------------------ ambience

	public void playAmbience(String id, float fadeSeconds) {
		request(ambience, id, fadeSeconds, 1f);
	}

	public void stopAmbience(float fadeSeconds) {
		stop(ambience, fadeSeconds);
	}

	public String requestedAmbience() {
		synchronized (lock) {
			return ambience.requestedId;
		}
	}

	public String currentAmbience() {
		synchronized (lock) {
			return ambience.currentId;
		}
	}

	/** Sinks the ambience under an encounter (1 = normal, 0.2 = much quieter) over about {@code seconds}. */
	public void setAmbienceEncounterLevel(float level, float seconds) {
		synchronized (lock) {
			ambienceEncounterLevel = level;
			applyDucks(seconds);
		}
	}

	// ------------------------------------------------------------------ channel logic

	private void request(Channel ch, String id, float fadeSeconds, float level) {
		final long serial;
		synchronized (lock) {
			if (shutDown || id == null || id.equals(ch.requestedId))
				return;
			ch.requestedId = id;
			ch.serial++;
			serial = ch.serial;
			// Already playing it (the change that was loading is simply abandoned).
			if (id.equals(ch.currentId) && mixer.isAlive(ch.current, ch.currentGen) && !ch.current.isFadingOut())
				return;
			// It is the track that is just fading out: bring that same playback back instead of starting a copy.
			if (id.equals(ch.previousId) && mixer.isAlive(ch.previous, ch.previousGen)) {
				Voice back = ch.previous;
				int backGen = ch.previousGen;
				mixer.revive(back, fadeSeconds);
				pushCurrentToPrevious(ch, fadeSeconds, back);
				ch.current = back;
				ch.currentGen = backGen;
				ch.currentId = id;
				System.out.println("[Audio] " + ch.label + " -> " + id + " (resumed, fade " + fadeSeconds + "s)");
				return;
			}
		}
		if (loader == null)
			startLoaded(ch, id, fadeSeconds, level, serial);
		else
			loader.execute(() -> startLoaded(ch, id, fadeSeconds, level, serial));
	}

	private void startLoaded(Channel ch, String id, float fadeSeconds, float level, long serial) {
		SoundData data = library.get(id); // decoded outside the lock: may take a few milliseconds
		synchronized (lock) {
			if (shutDown || serial != ch.serial)
				return; // the game asked for something else while this was loading
			pushCurrentToPrevious(ch, fadeSeconds, null);
			if (data == null)
				return; // missing/unsupported: silence
			Voice v = mixer.start(data, fadeSeconds, level);
			if (v == null) {
				System.out.println("[Audio] " + ch.label + " -> " + id + " (NOT started: no free voice)");
				return;
			}
			ch.current = v;
			ch.currentGen = v.generation;
			ch.currentId = id;
			mixer.retireStrays(ch.kind, v, MixLevels.RETIRE_FADE);
			System.out.println("[Audio] " + ch.label + " -> " + id + " (fade " + fadeSeconds + "s)");
		}
	}

	/**
	 * Moves the main track to the "on its way out" slot. Whatever was already fading out is finished almost at once,
	 * which is what keeps the number of audible tracks bounded. Must be called with the lock held.
	 */
	private void pushCurrentToPrevious(Channel ch, float fadeSeconds, Voice keepAlive) {
		Voice outgoing = mixer.isAlive(ch.current, ch.currentGen) && ch.current != keepAlive ? ch.current : null;
		if (ch.previous != null && ch.previous != keepAlive && mixer.isAlive(ch.previous, ch.previousGen))
			mixer.fadeOut(ch.previous, MixLevels.RETIRE_FADE);
		mixer.hurryFadeOuts(ch.kind, MixLevels.RETIRE_FADE); // any other stragglers
		if (outgoing != null) {
			mixer.fadeOut(outgoing, fadeSeconds);
			ch.previous = outgoing;
			ch.previousGen = ch.currentGen;
			ch.previousId = ch.currentId;
		} else {
			ch.previous = null;
			ch.previousId = null;
		}
		ch.current = null;
		ch.currentId = null;
	}

	private void stop(Channel ch, float fadeSeconds) {
		synchronized (lock) {
			if (shutDown)
				return;
			ch.serial++;
			ch.requestedId = null;
			pushCurrentToPrevious(ch, fadeSeconds, null);
		}
	}

	// ------------------------------------------------------------------ settings & state

	public void setMasterVolume(float v) { mixer.setMasterVolume(v); }
	public float getMasterVolume() { return mixer.getMasterVolume(); }
	public void setMasterMuted(boolean m) { mixer.setMasterMuted(m); }
	public boolean isMasterMuted() { return mixer.isMasterMuted(); }
	public void setBusVolume(Bus b, float v) { mixer.setBusVolume(b, v); }
	public float getBusVolume(Bus b) { return mixer.getBusVolume(b); }
	public void setBusMuted(Bus b, boolean m) { mixer.setBusMuted(b, m); }
	public boolean isBusMuted(Bus b) { return mixer.isBusMuted(b); }

	/** Pause menu: music and ambience keep playing but quieter, so volume changes can still be heard. */
	public void setGamePaused(boolean paused) {
		synchronized (lock) {
			this.paused = paused;
			applyDucks(MixLevels.PAUSE_DUCK_SECONDS);
		}
	}

	private void applyDucks(float seconds) {
		float pause = paused ? MixLevels.PAUSE_DUCK : 1f;
		mixer.setBusDuck(Bus.MUSIC, pause, seconds);
		mixer.setBusDuck(Bus.AMBIENCE, pause * ambienceEncounterLevel, seconds);
	}

	/** Window lost focus: freeze everything (resumes exactly where it stopped). */
	public void setSuspended(boolean suspended) {
		mixer.setSuspended(suspended);
	}

	public boolean hasOutputDevice() {
		return outputOk && output.isRunning();
	}

	public AudioMixer mixer() {
		return mixer;
	}

	public SoundLibrary library() {
		return library;
	}

	/** Stops the output thread, closes the sound line and frees every decoded buffer. Safe to call twice. */
	public void shutdown() {
		synchronized (lock) {
			if (shutDown)
				return;
			shutDown = true;
			music.serial++;
			ambience.serial++;
			music.current = music.previous = null;
			ambience.current = ambience.previous = null;
			mixer.clear();
		}
		if (loader != null)
			loader.shutdownNow();
		output.close();
		library.close();
	}

	public boolean isShutDown() {
		return shutDown;
	}
}
