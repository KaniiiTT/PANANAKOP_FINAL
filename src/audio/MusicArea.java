package audio;

/**
 * The places of the world that have their own exploration music and ambience. The track ids come from the audio pack's
 * manifest; changing a track or adding an area (e.g. a new region) only means editing this enum.
 */
public enum MusicArea {
	MENU("Main Menu", "bgm_main_menu", null),                       // no gameplay ambience in the menus
	VILLAGE("Village of Mactan", "bgm_stage1_barangay", "amb_stage1_village"),
	FOREST("Forest", "bgm_stage2_forest", "amb_stage2_forest"),
	TRIBAL_VILLAGE("Enemy Tribal Village", "bgm_stage3_island", "amb_stage3_beach"),
	CAVE("Cave", "bgm_stage4_punta_engano", "amb_stage4_cliff"),
	SHORELINE("Shoreline and Sea", "bgm_stage5_liberty_shrine", "amb_stage5_shrine_night");

	private final String displayName;
	private final String musicId;
	private final String ambienceId;

	MusicArea(String displayName, String musicId, String ambienceId) {
		this.displayName = displayName;
		this.musicId = musicId;
		this.ambienceId = ambienceId;
	}

	public String displayName() {
		return displayName;
	}

	public String musicId() {
		return musicId;
	}

	/** The looping environment sound of this area, or null if the area has none. */
	public String ambienceId() {
		return ambienceId;
	}
}
