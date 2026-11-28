package audio;

/**
 * Every default level and transition time of the audio mix, in one place so the balance can be tuned without
 * touching any logic. Levels are linear (1.0 = full scale); times are in seconds.
 *
 * <p>These are mix ratios between the buses. The player's volume slider stays the master volume on top of them.
 */
public final class MixLevels {
	private MixLevels() {}

	// ---- bus levels
	public static final float MUSIC_BUS = 0.70f;
	public static final float AMBIENCE_BUS = 0.35f;
	public static final float SFX_BUS = 0.80f;
	/** Boss tracks are mixed at 80% while exploration music is 70%: prominent, not louder than the limiter allows. */
	public static final float BOSS_MUSIC_LEVEL = 0.80f / MUSIC_BUS;

	// ---- ducking
	public static final float PAUSE_DUCK = 0.35f;
	public static final float PAUSE_DUCK_SECONDS = 0.4f;
	public static final float MINI_BOSS_AMBIENCE = 0.50f;
	public static final float FINAL_BOSS_AMBIENCE = 0.20f;

	// ---- transitions
	public static final float AREA_CROSSFADE = 2.5f;       // exploration -> exploration
	public static final float MENU_CROSSFADE = 2.0f;       // menu <-> game
	public static final float BOSS_FADE_IN = 1.5f;         // exploration -> boss
	public static final float BOSS_FADE_OUT = 2.5f;        // boss -> exploration
	public static final float STOP_FADE = 1.0f;            // death
	public static final float AMBIENCE_CROSSFADE = 3.0f;   // area -> area
	public static final float AMBIENCE_BOSS_FADE = 2.0f;   // ambience sinks under a boss fight
	public static final float AMBIENCE_RESTORE_FADE = 3.0f;
	/** A track that is already fading out and gets pushed aside by yet another change is finished this quickly. */
	public static final float RETIRE_FADE = 0.12f;

	// ---- ambience loudness matching (done when a file is loaded; see SoundLibrary)
	public static final float AMBIENCE_TARGET_RMS_DB = -20f;
	public static final float AMBIENCE_PEAK_CEILING_DB = -1f;
}
