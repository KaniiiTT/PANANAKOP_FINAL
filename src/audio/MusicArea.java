package audio;

/**
 * The places of the world that have their own exploration music. The track ids come from the audio pack's
 * manifest; changing a track or adding an area (e.g. a new region) only means editing this enum.
 */
public enum MusicArea {
	MENU("Main Menu", "bgm_main_menu"),
	VILLAGE("Village of Mactan", "bgm_stage1_barangay"),
	FOREST("Forest", "bgm_stage2_forest"),
	TRIBAL_VILLAGE("Enemy Tribal Village", "bgm_stage3_island"),
	CAVE("Cave", "bgm_stage4_punta_engano"),
	SHORELINE("Shoreline and Sea", "bgm_stage5_liberty_shrine");

	private final String displayName;
	private final String musicId;

	MusicArea(String displayName, String musicId) {
		this.displayName = displayName;
		this.musicId = musicId;
	}

	public String displayName() {
		return displayName;
	}

	public String musicId() {
		return musicId;
	}
}
