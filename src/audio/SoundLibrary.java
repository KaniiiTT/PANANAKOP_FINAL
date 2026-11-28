package audio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Loads and caches decoded sounds. Lookup by id is a HashMap hit, a file is decoded at most once,
 * music (large) is kept in a small LRU cache, and a file that fails to load is remembered so it is
 * neither retried nor logged again.
 */
public final class SoundLibrary {
	private static final int MUSIC_CACHE_SIZE = 3;

	private final AudioManifest manifest;
	private final Map<String, SoundData> cache = new HashMap<>();
	private final Map<String, SoundData> musicCache = new LinkedHashMap<>(8, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, SoundData> eldest) {
			return size() > MUSIC_CACHE_SIZE;
		}
	};
	private final Set<String> failed = new HashSet<>();
	private int decodeCount;

	public SoundLibrary(AudioManifest manifest) {
		this.manifest = manifest;
	}

	public AudioManifest manifest() {
		return manifest;
	}

	/** Returns the decoded sound, loading it on first use; null (and silence) if it is missing or unsupported. */
	public synchronized SoundData get(String id) {
		AudioManifest.Entry entry = manifest.get(id);
		if (entry == null) {
			warnOnce(id, "not listed in the audio manifest");
			return null;
		}
		boolean isMusic = entry.kind == Kind.MUSIC;
		SoundData cached = isMusic ? musicCache.get(id) : cache.get(id);
		if (cached != null)
			return cached;
		if (failed.contains(id))
			return null;
		SoundData loaded = decode(entry);
		if (loaded == null)
			return null;
		if (isMusic)
			musicCache.put(id, loaded);
		else
			cache.put(id, loaded);
		return loaded;
	}

	private SoundData decode(AudioManifest.Entry entry) {
		String path = entry.resourcePath();
		try (InputStream raw = ResourceLocator.open(path)) {
			if (raw == null) {
				warnOnce(entry.id, "file not found: " + path);
				return null;
			}
			try (AudioInputStream source = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
				AudioFormat in = source.getFormat();
				if (in.getChannels() < 1 || in.getChannels() > 2) {
					warnOnce(entry.id, "unsupported channel count " + in.getChannels());
					return null;
				}
				AudioFormat pcm16 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, in.getSampleRate(), 16,
						in.getChannels(), in.getChannels() * 2, in.getSampleRate(), false);
				try (AudioInputStream converted = AudioSystem.getAudioInputStream(pcm16, source)) {
					byte[] bytes = converted.readAllBytes();
					short[] samples = new short[bytes.length / 2];
					for (int i = 0; i < samples.length; i++)
						samples[i] = (short) ((bytes[2 * i] & 0xFF) | (bytes[2 * i + 1] << 8));
					decodeCount++;
					return new SoundData(entry.id, entry.kind, samples, in.getChannels(), (int) in.getSampleRate(),
							entry.loop, entry.loopEndSec, gainDbFor(entry, samples));
				}
			}
		} catch (Exception e) { // unsupported format, truncated file, IO problem...
			warnOnce(entry.id, "cannot be decoded (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
			return null;
		}
	}

	/**
	 * How loud a sound plays relative to its file.
	 * <ul>
	 * <li>Music plays at the level it was mastered (about -16 dBFS RMS). The pack's "suggested_volume_db" of -8
	 * exists to balance music against effects; stacked on the volume slider it made the music hard to hear.</li>
	 * <li>Ambience files differ by about 10 dB in loudness (a night shrine is far quieter than a beach), so one bus
	 * level cannot balance them. Each is brought to a common loudness, without letting its peaks reach full scale.</li>
	 * <li>Everything else keeps the pack's suggested level.</li>
	 * </ul>
	 */
	private static float gainDbFor(AudioManifest.Entry entry, short[] samples) {
		if (entry.kind == Kind.MUSIC)
			return 0f;
		if (entry.kind == Kind.AMBIENCE)
			return ambienceTrimDb(samples);
		return entry.volumeDb;
	}

	static float ambienceTrimDb(short[] samples) {
		double sum = 0;
		int peak = 1;
		for (short v : samples) {
			sum += (double) v * v;
			peak = Math.max(peak, Math.abs(v));
		}
		if (samples.length == 0)
			return 0f;
		double rmsDb = 20 * Math.log10(Math.max(1e-9, Math.sqrt(sum / samples.length) / 32768.0));
		double peakDb = 20 * Math.log10(peak / 32768.0);
		double trim = MixLevels.AMBIENCE_TARGET_RMS_DB - rmsDb;
		trim = Math.min(trim, MixLevels.AMBIENCE_PEAK_CEILING_DB - peakDb);
		return (float) Math.max(-12.0, Math.min(12.0, trim));
	}

	private void warnOnce(String id, String why) {
		if (failed.add(id))
			System.err.println("[Audio] '" + id + "' " + why + " - playing silence instead.");
	}

	public synchronized boolean isCached(String id) {
		return cache.containsKey(id) || musicCache.containsKey(id);
	}

	/** How many files were actually decoded since start-up (used to prove nothing is re-loaded). */
	public synchronized int decodeCount() {
		return decodeCount;
	}

	/** Releases every decoded buffer. */
	public synchronized void close() {
		cache.clear();
		musicCache.clear();
	}
}
