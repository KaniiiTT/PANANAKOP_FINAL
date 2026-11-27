package audio;

/**
 * Decides WHICH track should play from what is happening in the game (menu, position in the world, boss
 * fights, game over) and asks the {@link AudioEngine} for it. It contains the rules that stop the music from
 * flickering: an area must be entered for {@link #AREA_DWELL_SECONDS} before the music changes, and a
 * boss fight remembers the area so exploration music returns afterwards.
 */
public final class MusicDirector {
	public static final float AREA_DWELL_SECONDS = 1.5f;
	public static final float AREA_CROSSFADE = 2.5f;
	public static final float MENU_CROSSFADE = 1.2f;
	public static final float BOSS_FADE = 0.6f;
	public static final float STOP_FADE = 1.0f;

	private enum Mode { MENU, EXPLORING, ENCOUNTER, SILENT }

	private final AudioEngine engine;
	private final MusicZones zones;

	private Mode mode = Mode.SILENT;
	private MusicArea currentArea;       // the area whose music is (or will be) playing
	private MusicArea pendingArea;       // candidate area that has not been inside long enough yet
	private float pendingSeconds;
	private int lastLevelIndex = Integer.MIN_VALUE;
	private Encounter encounter;

	public MusicDirector(AudioEngine engine, MusicZones zones) {
		this.engine = engine;
		this.zones = zones;
	}

	/** Menu, character select and options screens. */
	public void enterMenu() {
		if (mode == Mode.MENU)
			return;
		mode = Mode.MENU;
		encounter = null;
		currentArea = null;
		pendingArea = null;
		lastLevelIndex = Integer.MIN_VALUE;
		engine.setGamePaused(false);
		engine.playMusic(MusicArea.MENU.musicId(), MENU_CROSSFADE);
	}

	/**
	 * Call every update while the player explores. A different map file takes effect at once (it is a deliberate
	 * load); crossing a zone border inside a map needs the player to stay for AREA_DWELL_SECONDS.
	 */
	public void updateExploration(int levelIndex, int playerTileX, float dt) {
		if (mode == Mode.SILENT || mode == Mode.MENU) {
			mode = Mode.EXPLORING;
			currentArea = null;
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

	private void setArea(MusicArea area) {
		currentArea = area;
		if (mode == Mode.ENCOUNTER)
			return; // remembered; restored when the encounter ends
		engine.playMusic(area.musicId(), AREA_CROSSFADE);
	}

	/** A boss fight begins: the boss track replaces the exploration music. Repeated calls do nothing. */
	public void beginEncounter(Encounter e) {
		if (mode == Mode.ENCOUNTER && encounter == e)
			return;
		encounter = e;
		mode = Mode.ENCOUNTER;
		engine.playMusic(e.musicId(), BOSS_FADE);
	}

	/** The boss is gone (defeated or the run was reset): exploration music of the current location returns. */
	public void endEncounter() {
		if (mode != Mode.ENCOUNTER)
			return;
		encounter = null;
		mode = Mode.EXPLORING;
		if (currentArea != null)
			engine.playMusic(currentArea.musicId(), AREA_CROSSFADE);
	}

	public boolean isInEncounter() {
		return mode == Mode.ENCOUNTER;
	}

	/** Player died: the music fades away. The next updateExploration (after the restart) starts the area music again. */
	public void gameOver() {
		if (mode == Mode.SILENT)
			return;
		mode = Mode.SILENT;
		encounter = null;
		currentArea = null;
		pendingArea = null;
		lastLevelIndex = Integer.MIN_VALUE;
		engine.stopMusic(STOP_FADE);
	}

	/** Pause menu on/off. */
	public void setPaused(boolean paused) {
		engine.setGamePaused(paused);
	}

	public MusicArea currentArea() {
		return currentArea;
	}
}
