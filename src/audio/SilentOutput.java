package audio;

/** Output that plays nothing; used when there is no sound device (or in tests, which render the mixer by hand). */
public final class SilentOutput implements AudioOutput {
	private boolean running;

	@Override
	public boolean start(AudioMixer mixer) {
		running = true;
		return true;
	}

	@Override
	public void close() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}
