package audio;

/** Where mixed audio goes. Different implementations (sound card, nothing) are used through this one type. */
public interface AudioOutput {
	/** Begins pulling audio from the mixer. Returns false if no output device could be opened. */
	boolean start(AudioMixer mixer);

	/** Stops pulling audio and releases the device. Safe to call more than once. */
	void close();

	boolean isRunning();
}
