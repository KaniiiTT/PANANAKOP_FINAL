package audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Data table that says which {@link MusicArea} a position belongs to. A zone starts at (levelIndex, tileX)
 * and lasts until the next zone starts. Today every map file is one area; when the maps are merged into one
 * continuous world, add zones with larger tileX values (or change the level index to 0) - no other code changes.
 */
public final class MusicZones {
	private static final class Zone {
		final int level;
		final int startTileX;
		final MusicArea area;

		Zone(int level, int startTileX, MusicArea area) {
			this.level = level;
			this.startTileX = startTileX;
			this.area = area;
		}
	}

	private final List<Zone> zones = new ArrayList<>(); // kept sorted by (level, startTileX)

	/** The five areas of the journey, one per current map file. */
	public static MusicZones defaultWorld() {
		return new MusicZones()
				.add(0, 0, MusicArea.VILLAGE)
				.add(1, 0, MusicArea.FOREST)
				.add(2, 0, MusicArea.TRIBAL_VILLAGE)
				.add(3, 0, MusicArea.CAVE)
				.add(4, 0, MusicArea.SHORELINE);
	}

	public MusicZones add(int level, int startTileX, MusicArea area) {
		Zone z = new Zone(level, startTileX, area);
		int i = 0;
		while (i < zones.size() && before(zones.get(i), z))
			i++;
		zones.add(i, z);
		return this;
	}

	private static boolean before(Zone a, Zone b) {
		return a.level < b.level || (a.level == b.level && a.startTileX <= b.startTileX);
	}

	/** The area at a position: the last zone that starts at or before it (first zone if before all of them). */
	public MusicArea areaAt(int level, int tileX) {
		if (zones.isEmpty())
			return MusicArea.VILLAGE;
		MusicArea found = zones.get(0).area;
		for (int i = 0; i < zones.size(); i++) {
			Zone z = zones.get(i);
			if (z.level < level || (z.level == level && z.startTileX <= tileX))
				found = z.area;
			else
				break;
		}
		return found;
	}
}
