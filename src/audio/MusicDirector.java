package audio;

/**
 * Decides WHAT should be heard from what is happening in the game (menu, position in the world, boss fights,
 * game over) and asks the {@link AudioEngine} for it. It owns two things:
 *
 * <p><b>Music, by priority.</b> Each {@link MusicPriority} can hold one wish ("claim") for a track. The highest
 * claim is the one that plays; releasing it lets the next one down take over. So a boss claim silences the
 * exploration music without ever stopping the exploration claim, and when the boss goes away the right area
 * music is simply what is left, even if the player moved to another area in between.
 *
 * <p><b>Ambience, by area.</b> The ambience is the current area's environment sound, played on its own channel. It
 * is absent in the menus, sinks under boss fights and is restored afterwards.
 *
 * <p>It also holds the rules that stop the audio from flickering: crossing a zone border inside a map only takes
 * effect after {@link #AREA_DWELL_SECONDS}.
 */
public final class MusicDirector {
	public static final float AREA_DWELL_SECONDS = 1.5f;

	private enum Mode { MENU, EXPLORING, SILENT }

	private final AudioEngine engine;
	private final MusicZones zones;
	private final String[] claims = new String[MusicPriority.VALUES.length];

	private Mode mode = Mode.SILENT;
	private MusicArea currentArea;       // the area whose music and ambience are (or will be) playing
	private MusicArea pendingArea;       // candidate area that has not been inside long enough yet
	private float pendingSeconds;
	private int lastLevelIndex = Integer.MIN_VALUE;
	private Encounter encounter;

	public MusicDirector(AudioEngine engine, MusicZones zones) {
		this.engine = engine;
		this.zones = zones;
	}

	// ------------------------------------------------------------------ priority claims

	/** Asks for a track at a priority. The track only plays if no higher priority has a claim. */
	public void claim(MusicPriority priority, String musicId, float fadeSeconds) {
		claims[priority.ordinal()] = musicId;
		resolve(fadeSeconds);
	}

	/** Withdraws the claim of a priority; the next highest claim (if any) takes over. */
	public void release(MusicPriority priority, float fadeSeconds) {
		if (claims[priority.ordinal()] == null)
			return;
		claims[priority.ordinal()] = null;
		resolve(fadeSeconds);
	}

	/** The priority that is deciding the music right now, or null if nothing is wanted. */
	public MusicPriority activePriority() {
		for (int i = claims.length - 1; i >= 0; i--)
			if (claims[i] != null)
				return MusicPriority.VALUES[i];
		return null;
	}

	private void resolve(float fadeSeconds) {
		MusicPriority top = activePriority();
		if (top == null) {
			engine.stopMusic(fadeSeconds);
			return;
		}
		boolean boss = top == MusicPriority.MINI_BOSS || top == MusicPriority.FINAL_BOSS;
		engine.playMusic(claims[top.ordinal()], fadeSeconds, boss ? MixLevels.BOSS_MUSIC_LEVEL : 1f);
	}

	private void clearClaims() {
		java.util.Arrays.fill(claims, null);
	}

	// ------------------------------------------------------------------ game situations

	/** Menu, character select and options screens: all gameplay music and ambience stops. */
	public void enterMenu() {
		if (mode == Mode.MENU)
			return;
		mode = Mode.MENU;
		encounter = null;
		currentArea = null;
		pendingArea = null;
		lastLevelIndex = Integer.MIN_VALUE;
		clearClaims();
		engine.setGamePaused(false);
		engine.stopAmbience(MixLevels.MENU_CROSSFADE);
		claim(MusicPriority.MENU, MusicArea.MENU.musicId(), MixLevels.MENU_CROSSFADE);
	}

	/**
	 * Call every update while the player explores. A different map file takes effect at once (it is a deliberate
	 * load); crossing a zone border inside a map needs the player to stay for AREA_DWELL_SECONDS.
	 */
	public void updateExploration(int levelIndex, int playerTileX, float dt) {
		if (mode == Mode.SILENT || mode == Mode.MENU) {
			mode = Mode.EXPLORING;
			currentArea = null;
			claims[MusicPriority.MENU.ordinal()] = null; // gameplay takes over from the menu
			engine.setAmbienceEncounterLevel(1f, MixLevels.AMBIENCE_RESTORE_FADE);
		}
		MusicArea here = zones.areaAt(levelIndex, playerTileX);
		boolean newMap = levelIndex != lastLevelIndex;
		lastLevelIndex = levelIndex;

		if (currentArea == null || newMap) {
			pendingArea = null;
			setArea(here);
			return;
		}
		if (here == currentArea) {
			pendingArea = null;
			pendingSeconds = 0f;
			return;
		}
		if (here != pendingArea) {
			pendingArea = here;
			pendingSeconds = 0f;
		}
		pendingSeconds += dt;
		if (pendingSeconds >= AREA_DWELL_SECONDS) {
			pendingArea = null;
			setArea(here);
		}
	}

	/** The player is now in {@code area}: its music is claimed at EXPLORATION priority and its ambience starts. */
	private void setArea(MusicArea area) {
		currentArea = area;
		claim(MusicPriority.EXPLORATION, area.musicId(), MixLevels.AREA_CROSSFADE); // a boss claim, if any, stays on top
		if (area.ambienceId() == null)
			engine.stopAmbience(MixLevels.AMBIENCE_CROSSFADE);
		else
			engine.playAmbience(area.ambienceId(), MixLevels.AMBIENCE_CROSSFADE);
	}

	/** A boss fight begins: the boss track replaces the exploration music and the ambience sinks. Repeats do nothing. */
	public void beginEncounter(Encounter e) {
		if (mode != Mode.EXPLORING || encounter == e)
			return;
		if (encounter != null)
			claims[encounter.priority().ordinal()] = null;
		encounter = e;
		claim(e.priority(), e.musicId(), MixLevels.BOSS_FADE_IN);
		engine.setAmbienceEncounterLevel(e.ambienceLevel(), MixLevels.AMBIENCE_BOSS_FADE);
	}

	/** The boss is gone (defeated or the run was reset): exploration music and ambience of this location return. */
	public void endEncounter() {
		if (encounter == null)
			return;
		MusicPriority p = encounter.priority();
		encounter = null;
		release(p, MixLevels.BOSS_FADE_OUT);
		engine.setAmbienceEncounterLevel(1f, MixLevels.AMBIENCE_RESTORE_FADE);
	}

	public boolean isInEncounter() {
		return encounter != null;
	}

	/** Player died: music and ambience fade away. The next updateExploration (after the restart) brings them back. */
	public void gameOver() {
		if (mode == Mode.SILENT)
			return;
		mode = Mode.SILENT;
		encounter = null;
		currentArea = null;
		pendingArea = null;
		lastLevelIndex = Integer.MIN_VALUE;
		clearClaims();
		engine.stopMusic(MixLevels.STOP_FADE);
		engine.stopAmbience(MixLevels.STOP_FADE);
	}

	/** Pause menu on/off. */
	public void setPaused(boolean paused) {
		engine.setGamePaused(paused);
	}

	public MusicArea currentArea() {
		return currentArea;
	}
}
