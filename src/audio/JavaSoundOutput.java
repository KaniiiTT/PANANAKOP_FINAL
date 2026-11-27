package audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/** Sends the mixer's output to the sound card through ONE javax.sound line and a daemon thread. */
public final class JavaSoundOutput implements AudioOutput {
	private static final int FRAMES_PER_WRITE = 1024;

	private SourceDataLine line;
	private Thread thread;
	private volatile boolean running;
	private String deviceName = "none";

	@Override
	public synchronized boolean start(AudioMixer mixer) {
		if (running)
			return true;
		AudioFormat format = new AudioFormat(AudioMixer.SAMPLE_RATE, 16, 2, true, false);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		int bufferBytes = FRAMES_PER_WRITE * 4 * 4; // ~93 ms of buffer

		// 1) the system's default output, 2) every other output the system lists, one after another.
		String firstError = null;
		try {
			if (AudioSystem.isLineSupported(info)) {
				line = (SourceDataLine) AudioSystem.getLine(info);
				line.open(format, bufferBytes);
				line.start();
				deviceName = "system default";
			} else {
				firstError = "the default output does not support 44.1 kHz 16-bit stereo";
			}
		} catch (Exception e) {
			firstError = e.toString();
			closeLine();
		}
		if (line == null) {
			for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
				try {
					Mixer candidate = AudioSystem.getMixer(mi);
					if (!candidate.isLineSupported(info))
						continue;
					SourceDataLine l = (SourceDataLine) candidate.getLine(info);
					l.open(format, bufferBytes);
					l.start();
					line = l;
					deviceName = mi.getName();
					break;
				} catch (Exception e) {
					// try the next output
				}
			}
		}
		if (line == null) {
			System.err.println("[Audio] ERROR: no usable audio output device"
					+ (firstError != null ? " (" + firstError + ")" : "") + " - the game will run WITHOUT music. "
					+ "Outputs reported by Java: " + AudioSystem.getMixerInfo().length);
			return false;
		}
		System.out.println("[Audio] Output opened: " + deviceName + ", 44100 Hz 16-bit stereo, buffer "
				+ line.getBufferSize() + " bytes");
		running = true;
		final SourceDataLine out = line;
		thread = new Thread(() -> {
			byte[] buffer = new byte[FRAMES_PER_WRITE * 4];
			try {
				while (running) {
					mixer.render(buffer, FRAMES_PER_WRITE);
					out.write(buffer, 0, buffer.length); // blocks until the card needs more = natural pacing
				}
			} catch (RuntimeException e) {
				running = false;
				System.err.println("[Audio] ERROR: the audio thread stopped (" + e + ") - music has stopped.");
			}
		}, "PANANAKOP-audio");
		thread.setDaemon(true);
		thread.start();
		return true;
	}

	@Override
	public synchronized void close() {
		running = false;
		if (thread != null) {
			try {
				thread.join(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			thread = null;
		}
		closeLine();
	}

	private void closeLine() {
		if (line != null) {
			try {
				line.stop();
				line.flush();
			} catch (RuntimeException ignored) {
				// already closed
			}
			line.close();
			line = null;
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}
