package audio;

/** Boss fights that replace the exploration music while they last. Only bosses that exist in the game are listed. */
public enum Encounter {
	MINI_BOSS("bgm_miniboss_captain", MusicPriority.MINI_BOSS, MixLevels.MINI_BOSS_AMBIENCE),     // Espinosa
	FINAL_BOSS("bgm_boss_magellan_final", MusicPriority.FINAL_BOSS, MixLevels.FINAL_BOSS_AMBIENCE); // Magellan

	private final String musicId;
	private final MusicPriority priority;
	private final float ambienceLevel;

	Encounter(String musicId, MusicPriority priority, float ambienceLevel) {
		this.musicId = musicId;
		this.priority = priority;
		this.ambienceLevel = ambienceLevel;
	}

	public String musicId() {
		return musicId;
	}

	public MusicPriority priority() {
		return priority;
	}

	/** How loud the area's ambience stays during this fight (1 = unchanged). */
	public float ambienceLevel() {
		return ambienceLevel;
	}
}
