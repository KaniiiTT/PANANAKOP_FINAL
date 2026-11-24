package effects;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public class HitEffects {
	private static HitEffects activeInstance;
	private static final float HIT_STOP_SECONDS = 0.06f;
	private static final float HEAVY_HIT_STOP_SECONDS = 0.09f;
	private static final float FLASH_SECONDS = 0.1f;
	private static final float SHAKE_SECONDS = 0.1f;
	private static final float HEAVY_SHAKE_SECONDS = 0.15f;
	private static final int SHAKE_STRENGTH = 5;
	private static final int HEAVY_SHAKE_STRENGTH = 7;

	private final Random random = new Random();
	private boolean hitEffectsEnabled = true;
	private float hitStopTimer;
	private float shakeTimer;
	private int shakeX, shakeY;

	public HitEffects() {
		activeInstance = this;
	}

	public static void triggerAttackShake() {
		if (activeInstance == null || !activeInstance.hitEffectsEnabled)
			return;
		activeInstance.shakeTimer = Math.max(activeInstance.shakeTimer, SHAKE_SECONDS);
		activeInstance.shakeX = activeInstance.random.nextInt(SHAKE_STRENGTH * 2 + 1) - SHAKE_STRENGTH;
		activeInstance.shakeY = activeInstance.random.nextInt(SHAKE_STRENGTH * 2 + 1) - SHAKE_STRENGTH;
	}

	public void trigger(boolean heavy) {
		if (!hitEffectsEnabled)
			return;
		hitStopTimer = Math.max(hitStopTimer, heavy ? HEAVY_HIT_STOP_SECONDS : HIT_STOP_SECONDS);
		shakeTimer = Math.max(shakeTimer, heavy ? HEAVY_SHAKE_SECONDS : SHAKE_SECONDS);
		int strength = heavy ? HEAVY_SHAKE_STRENGTH : SHAKE_STRENGTH;
		shakeX = random.nextInt(strength * 2 + 1) - strength;
		shakeY = random.nextInt(strength * 2 + 1) - strength;
	}

	public void update(float dt) {
		hitStopTimer = Math.max(0, hitStopTimer - dt);
		shakeTimer = Math.max(0, shakeTimer - dt);
	}

	public boolean isFrozen() {
		return hitEffectsEnabled && hitStopTimer > 0;
	}

	public void applyShake(Graphics2D g) {
		if (hitEffectsEnabled && shakeTimer > 0)
			g.translate(shakeX, shakeY);
	}

	public void removeShake(Graphics2D g) {
		if (hitEffectsEnabled && shakeTimer > 0)
			g.translate(-shakeX, -shakeY);
	}

	public static BufferedImage whiteSilhouette(BufferedImage src) {
		BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < src.getHeight(); y++)
			for (int x = 0; x < src.getWidth(); x++) {
				int alpha = src.getRGB(x, y) >>> 24;
				copy.setRGB(x, y, alpha << 24 | 0x00FFFFFF);
			}
		return copy;
	}

	public float flashSeconds() {
		return FLASH_SECONDS;
	}
}
