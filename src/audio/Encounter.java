package audio;

/** Boss fights that replace the exploration music while they last. Only bosses that exist in the game are listed. */
public enum Encounter {
	MINI_BOSS("bgm_miniboss_captain"),   // Espinosa, the cave mini-boss
	FINAL_BOSS("bgm_boss_magellan_final"); // Magellan

	private final String musicId;

	Encounter(String musicId) {
		this.musicId = musicId;
	}

	public String musicId() {
		return musicId;
	}
}
