package audio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The single entry point of the audio system. It owns the library (what is loaded), the mixer (what is
 * playing) and the output (the sound card), and offers simple music operations. Game code never touches
 * files, lines or threads.
 *
 * <p>Music rules enforced here: at most one non-fading music voice exists; asking for the track that is
 * already playing does nothing; changing tracks crossfades; a missing file means silence, not a crash.
 */
public final class AudioEngine {
	public static final float DEFAULT_CROSSFADE = 2.0f;

	private final Object lock;
	private final AudioMixer mixer = new AudioMixer();
	private final SoundLibrary library;
	private final AudioOutput output;
	private final ExecutorService loader; // null = load on the calling thread (tests)

	private String requestedMusicId;   // what the game last asked for (may still be loading)
	private String playingMusicId;     // what is actually audible as the current track
	private Voice musicVoice;
	private long requestSerial;
	private boolean shutDown;
	private boolean outputOk;

	public AudioEngine(AudioManifest manifest, AudioOutput output, boolean asyncLoading) {
		this.library = new SoundLibrary(manifest);
		this.output = output;
		this.lock = mixer.lock();
		this.loader = asyncLoading ? Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "PANANAKOP-audio-loader");
			t.setDaemon(true);
			return t;
		}) : null;
		this.outputOk = output.start(mixer);
	}

	/** Normal game setup: reads the pack manifest, opens the sound card, loads in the background. */
	public static AudioEngine createDefault() {
		return new AudioEngine(AudioManifest.load(), new JavaSoundOutput(), true);
	}

	// ------------------------------------------------------------------ music

	/** Crossfades to the given track. Does nothing if that track is already the current one. */
	public void playMusic(String id, float fadeSeconds) {
		final long serial;
		synchronized (lock) {
			if (shutDown || id == null || id.equals(requestedMusicId))
				return;
			requestedMusicId = id;
			serial = ++requestSerial;
		}
		if (loader == null)
			startMusic(id, fadeSeconds, serial);
		else
			loader.execute(() -> startMusic(id, fadeSeconds, serial));
	}

	private void startMusic(String id, float fadeSeconds, long serial) {
		SoundData data = library.get(id); // decoded outside the lock: may take a few milliseconds
		synchronized (lock) {
			if (shutDown || serial != requestSerial)
				return; // the game asked for something else while this was loading
			Voice old = musicVoice;
			musicVoice = null;
			playingMusicId = null;
			mixer.hurryFadeOuts(Kind.MUSIC, fadeSeconds);
			mixer.fadeOut(old, fadeSeconds);
			if (data == null)
				return; // missing/unsupported: silence
			musicVoice = mixer.start(data, fadeSeconds);
			playingMusicId = musicVoice == null ? null : id;
			System.out.println("[Audio] Music -> " + id + (musicVoice == null ? " (NOT started: no free voice)" : "")
					+ " (fade " + fadeSeconds + "s)");
		}
	}

	/** Fades the current music out. A later playMusic of the same track starts it again. */
	public void stopMusic(float fadeSeconds) {
		synchronized (lock) {
			if (shutDown)
				return;
			requestSerial++;
			requestedMusicId = null;
			playingMusicId = null;
			Voice old = musicVoice;
			musicVoice = null;
			mixer.fadeOut(old, fadeSeconds);
		}
	}

	/** The track the game last asked for (null if stopped). */
	public String requestedMusic() {
		synchronized (lock) {
			return requestedMusicId;
		}
	}

	/** The track that is actually playing right now (null while loading, stopped or silent). */
	public String currentMusic() {
		synchronized (lock) {
			return playingMusicId;
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

	/** Pause menu: the music keeps playing but quieter so volume changes can still be heard. */
	public void setGamePaused(boolean paused) {
		mixer.setMusicDuck(paused ? 0.35f : 1f);
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
			requestSerial++;
			musicVoice = null;
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
