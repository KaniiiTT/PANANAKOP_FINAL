package audio;

/** The three user-adjustable mixer channels. Every playing sound belongs to exactly one bus. */
public enum Bus {
	MUSIC, AMBIENCE, SFX;

	public static final Bus[] VALUES = values();
}
