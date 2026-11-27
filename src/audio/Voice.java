package audio;

/** One playing instance of a sound. Pooled and reused by the mixer, so playing never allocates. */
final class Voice {
	SoundData data;
	Bus bus;
	boolean loop;
	boolean active;
	double pos;
	double step;
	float fade;        // 0..1, multiplied with the sound's own base gain
	float fadeTarget;
	float fadeStep;    // per output frame
	boolean stopAtZero;

	void start(SoundData d, float fadeInSeconds, int outRate) {
		data = d;
		bus = d.kind().bus();
		loop = d.isLoop();
		pos = 0;
		step = d.sampleRate() / (double) outRate;
		fadeTarget = 1f;
		if (fadeInSeconds > 0.001f) {
			fade = 0f;
			fadeStep = 1f / (fadeInSeconds * outRate);
		} else {
			fade = 1f;
			fadeStep = 0f;
		}
		stopAtZero = false;
		active = true;
	}

	void fadeOut(float seconds, int outRate) {
		fadeTarget = 0f;
		stopAtZero = true;
		if (seconds <= 0.001f || fade <= 0f) {
			fade = 0f;
			active = false;
		} else
			fadeStep = fade / (seconds * outRate);
	}

	boolean isFadingOut() {
		return stopAtZero;
	}

	/** Adds up to n frames of this voice into the bus buffers. */
	void mix(float[] left, float[] right, int n) {
		final short[] s = data.samples();
		final int ch = data.channels();
		final int end = loop ? data.loopEndFrame() : data.frames();
		final float base = data.baseGain() * (1f / 32768f);
		for (int i = 0; i < n; i++) {
			if (fade != fadeTarget) {
				fade += fadeTarget > fade ? fadeStep : -fadeStep;
				if (fadeStep <= 0f || (fadeTarget > 0f ? fade >= fadeTarget : fade <= fadeTarget)) {
					fade = fadeTarget;
					if (stopAtZero && fade <= 0f) {
						active = false;
						return;
					}
				}
			}
			int idx = (int) pos;
			float fr = (float) (pos - idx);
			int next = idx + 1;
			if (next >= end)
				next = loop ? 0 : idx;
			float g = base * fade;
			if (ch == 1) {
				float v = (s[idx] + (s[next] - s[idx]) * fr) * g;
				left[i] += v;
				right[i] += v;
			} else {
				int a = idx * 2, b = next * 2;
				left[i] += (s[a] + (s[b] - s[a]) * fr) * g;
				right[i] += (s[a + 1] + (s[b + 1] - s[a + 1]) * fr) * g;
			}
			pos += step;
			if (pos >= end) {
				if (loop)
					pos -= end;
				else {
					active = false;
					return;
				}
			}
		}
	}

	void release() {
		data = null;
		active = false;
	}
}
