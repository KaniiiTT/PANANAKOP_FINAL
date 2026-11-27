package audio;

/** Decoded, immutable PCM audio held in memory: 16-bit samples, interleaved when stereo. */
public final class SoundData {
	private final String id;
	private final Kind kind;
	private final short[] pcm;
	private final int channels;
	private final int sampleRate;
	private final int frames;
	private final boolean loop;
	private final int loopEndFrame;
	private final float baseGain;

	public SoundData(String id, Kind kind, short[] pcm, int channels, int sampleRate, boolean loop,
			double loopEndSeconds, float volumeDb) {
		this.id = id;
		this.kind = kind;
		this.pcm = pcm;
		this.channels = channels;
		this.sampleRate = sampleRate;
		this.frames = pcm.length / channels;
		this.loop = loop;
		int end = loopEndSeconds > 0 ? (int) Math.round(loopEndSeconds * sampleRate) : frames;
		this.loopEndFrame = Math.max(1, Math.min(end, frames));
		this.baseGain = (float) Math.pow(10.0, volumeDb / 20.0);
	}

	public String id() { return id; }
	public Kind kind() { return kind; }
	public int channels() { return channels; }
	public int sampleRate() { return sampleRate; }
	public int frames() { return frames; }
	public boolean isLoop() { return loop; }
	public int loopEndFrame() { return loopEndFrame; }
	public float baseGain() { return baseGain; }
	public double durationSeconds() { return frames / (double) sampleRate; }

	/** Package-private on purpose: only the mixer reads raw samples (encapsulation). */
	short[] samples() { return pcm; }
}
