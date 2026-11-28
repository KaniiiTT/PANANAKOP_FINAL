package audio;

/**
 * Who is allowed to decide the music. Several of these can want music at the same time; the highest one wins and
 * the others wait. When a higher priority is released, the next one down takes over again automatically, so
 * "boss music replaces exploration music" and "exploration music returns after the boss" need no special cases.
 */
public enum MusicPriority {
	MENU,         // lowest: title screen, character select, options
	EXPLORATION,  // the music of the area the player is in
	CUTSCENE,     // story scenes (no cutscene exists in the game yet; the slot is reserved)
	MINI_BOSS,
	FINAL_BOSS;   // highest

	public static final MusicPriority[] VALUES = values();
}
