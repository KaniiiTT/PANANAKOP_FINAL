package audio;

/**
 * What a sound file is for. Each kind knows which bus it plays on and which pack folder it lives in,
 * so the rest of the engine never has to hard-code that.
 */
public enum Kind {
	MUSIC(Bus.MUSIC, "music"),
	JINGLE(Bus.MUSIC, "jingles"),
	AMBIENCE(Bus.AMBIENCE, "ambience"),
	SFX(Bus.SFX, "sfx");

	private final Bus bus;
	private final String folder;

	Kind(Bus bus, String folder) {
		this.bus = bus;
		this.folder = folder;
	}

	public Bus bus() {
		return bus;
	}

	public String folder() {
		return folder;
	}

	/** Maps the "category" string of audio_manifest.json to a kind (null = not something we play). */
	public static Kind fromCategory(String category) {
		if (category == null)
			return null;
		if (category.equals("music"))
			return MUSIC;
		if (category.equals("jingle"))
			return JINGLE;
		if (category.equals("ambience"))
			return AMBIENCE;
		if (category.startsWith("sfx"))
			return SFX;
		return null;
	}
}
