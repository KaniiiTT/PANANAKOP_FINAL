package audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads {@code /audio/audio_manifest.json} (shipped with the audio pack) into a HashMap keyed by sound id,
 * so lookups are O(1) and nothing about the sounds is hard-coded in Java.
 */
public final class AudioManifest {
	public static final String RESOURCE_ROOT = "/audio/";
	public static final String MANIFEST_PATH = RESOURCE_ROOT + "audio_manifest.json";

	/** One manifest row. */
	public static final class Entry {
		public final String id;
		public final Kind kind;
		public final boolean loop;
		public final double loopEndSec;
		public final float volumeDb;
		public final double durationSec;
		public final int stage;

		Entry(String id, Kind kind, boolean loop, double loopEndSec, float volumeDb, double durationSec, int stage) {
			this.id = id;
			this.kind = kind;
			this.loop = loop;
			this.loopEndSec = loopEndSec;
			this.volumeDb = volumeDb;
			this.durationSec = durationSec;
			this.stage = stage;
		}

		/** Classpath location of the playable WAV, e.g. /audio/sfx/coin.wav. */
		public String resourcePath() {
			return RESOURCE_ROOT + kind.folder() + "/" + id + ".wav";
		}
	}

	private final Map<String, Entry> entries = new HashMap<>();

	public static AudioManifest load() {
		AudioManifest m = new AudioManifest();
		try (InputStream in = ResourceLocator.open(MANIFEST_PATH)) {
			if (in == null) {
				System.err.println("[Audio] ERROR: " + MANIFEST_PATH + " was not found on the classpath or in any "
						+ "'res' folder near " + java.nio.file.Paths.get("").toAbsolutePath()
						+ " - there will be NO music. Make sure res/audio/ exists in the project.");
				return m;
			}
			m.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			System.out.println("[Audio] Manifest loaded: " + m.size() + " sounds (resources: "
					+ ResourceLocator.describeSource(MANIFEST_PATH) + ")");
		} catch (IOException | RuntimeException e) {
			System.err.println("[Audio] Could not read manifest: " + e);
		}
		return m;
	}

	@SuppressWarnings("unchecked")
	void parse(String json) {
		Map<String, Object> root = (Map<String, Object>) JsonLite.parse(json);
		for (Map.Entry<String, Object> row : root.entrySet()) {
			if (!(row.getValue() instanceof Map))
				continue;
			Map<String, Object> f = (Map<String, Object>) row.getValue();
			Kind kind = Kind.fromCategory((String) f.get("category"));
			if (kind == null)
				continue;
			boolean loop = Boolean.TRUE.equals(f.get("loop"));
			entries.put(row.getKey(), new Entry(row.getKey(), kind, loop,
					num(f.get("loop_end_sec"), 0), (float) num(f.get("suggested_volume_db"), 0),
					num(f.get("duration_sec"), 0), (int) num(f.get("stage"), -1)));
		}
	}

	private static double num(Object o, double fallback) {
		return o instanceof Number ? ((Number) o).doubleValue() : fallback;
	}

	public Entry get(String id) {
		return entries.get(id);
	}

	public boolean contains(String id) {
		return entries.containsKey(id);
	}

	public int size() {
		return entries.size();
	}

	public Iterable<Entry> all() {
		return entries.values();
	}
}
