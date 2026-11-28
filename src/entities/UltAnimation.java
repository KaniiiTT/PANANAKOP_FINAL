package entities;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.Random;

/** Visual timing and impact effects for the male Lapulapu ultimate. */
public class UltAnimation {
	private static final int UPS = 200;
	private static final int ULT_HOLD_FRAME = 10;
	public static final int ULT_IMPACT_FRAME = 11;

	// ---- timings (seconds, converted to ticks with UPS) ----
	private static final float HOLD_SECONDS = 0.33f;
	private static final float DIM_RAMP_SECONDS = 0.10f;
	private static final float HITSTOP_SECONDS = 0.23f;
	private static final float SHAKE_SECONDS = 0.27f;
	private static final float SLASH_TRAIL_FADE_SECONDS = 0.17f;
	private static final float GROUND_CRACK_SPREAD_SECONDS = 0.10f;
	private static final float CRACK_LIFE_SECONDS = 0.8f;
	private static final float SPARK_ADD_INTERVAL_SECONDS = 0.033f;
	private static final float JITTER_SECONDS = 1f / 60f; // how often sparks/shake re-roll

	// ---- hold look ----
	private static final int DIM_MAX_ALPHA = 115;
	private static final float TREMBLE_SPEED = 0.75f; // radians per tick
	private static final double SWORD_ANGLE = -1.9; // raised, leaning back (facing right)
	private static final int HAND_X = 24; // sword pivot relative to anchor
	private static final int HAND_Y = 12;
	private static final int GLOW_LENGTH = 64;
	private static final int GLOW_WIDTH = 16;
	private static final Color GLOW_COLOR = new Color(255, 255, 255, 64);
	private static final int MAX_SPARKS = 13;

	// ---- impact look ----
	private static final double SLASH_ANGLE = 1.45; // near vertical
	private static final double SLASH_THIN_ANGLE = 1.62; // crosses the main slash
	private static final int SLASH_BLACK = 32;
	private static final int SLASH_WHITE = 20;
	private static final int SLASH_THIN = 4;
	private static final int SLASH_THIN_SHIFT = 8;
	private static final float TRAIL_MAX_THICKNESS = 24f;
	private static final float TARGET_FLASH_FRACTION = 0.6f; // first 60% of the trail
	private static final float SHAKE_RANGE = 5f; // +-px, halves after half the shake time
	public static final float KNOCKBACK = 30;
	private static final float KNOCKBACK_EASE_PER_TICK = 0.07f;

	// ---- ground crack ----
	private static final int CRACK_GROUND_OFFSET = 48; // target centre -> ground
	private static final float CRACK_SCALE = 1.5f;
	private static final int[][] CRACK_LEFT = { { 0, 0 }, { -18, 4 }, { -33, 0 }, { -53, 6 }, { -70, 2 } };
	private static final int[][] CRACK_RIGHT = { { 0, 0 }, { 20, 5 }, { 37, 1 }, { 60, 7 } };
	private static final BasicStroke CRACK_STROKE = new BasicStroke(2);

	private static final RescaleOp INVERT = new RescaleOp(new float[] { -1, -1, -1, 1 },
			new float[] { 255, 255, 255, 0 }, null);

	private final int holdTicks = Math.round(HOLD_SECONDS * UPS);
	private final int dimRampTicks = Math.round(DIM_RAMP_SECONDS * UPS);
	private final int hitstopTicks = Math.round(HITSTOP_SECONDS * UPS);
	private final int shakeTicks = Math.round(SHAKE_SECONDS * UPS);
	private final int trailTicks = Math.round(SLASH_TRAIL_FADE_SECONDS * UPS);
	private final int crackTicks = Math.round(GROUND_CRACK_SPREAD_SECONDS * UPS);
	private final int crackLifeTicks = Math.round(CRACK_LIFE_SECONDS * UPS);
	private final int sparkIntervalTicks = Math.max(1, Math.round(SPARK_ADD_INTERVAL_SECONDS * UPS));
	private final int jitterTicks = Math.max(1, Math.round(JITTER_SECONDS * UPS));

	private final Random rng = new Random();
	private final Point2D.Float shakeOffset = new Point2D.Float();
	private final Point2D.Float trembleOffset = new Point2D.Float();
	private final float[] sparkX = new float[MAX_SPARKS];
	private final float[] sparkY = new float[MAX_SPARKS];
	private BufferedImage impactImage;
	private boolean active;
	private boolean facingRight;
	private boolean holdStarted;
	private boolean impactTriggered;
	private boolean finished;
	private float holdTimer;
	private float holdElapsed;
	private float impactTimer;
	private float trailTimer;
	private float crackLifeTimer;
	private float shakeTimer;
	private float sparkTimer;
	private float jitterTimer;
	private float knockback;
	private float anchorX, anchorY;
	private float targetX, targetY, targetWorldX;
	private int cameraOffset;

	public void start(boolean facingRight) {
		reset();
		active = true;
		finished = false;
		this.facingRight = facingRight;
		rerollSparks();
	}

	public void update(int currentAnimFrame) {
		if (!active || finished)
			return;
		if (currentAnimFrame == ULT_HOLD_FRAME && !holdStarted && !impactTriggered) {
			holdStarted = true;
			holdTimer = holdTicks;
			holdElapsed = 0;
			sparkTimer = 0;
			rerollSparks();
		}
	}

	public void updateTimers(float dt) {
		if (!active)
			return;
		float ticks = dt * UPS;

		// Hit-stop freezes every other effect, like the preview: the trail, shake,
		// crack and knockback all start only after the freeze ends.
		if (impactTimer > 0) {
			impactTimer = Math.max(0, impactTimer - ticks);
			return;
		}

		jitterTimer += ticks;
		boolean reroll = false;
		if (jitterTimer >= jitterTicks) {
			jitterTimer -= jitterTicks;
			reroll = true;
		}

		if (holdTimer > 0) {
			holdTimer = Math.max(0, holdTimer - ticks);
			holdElapsed += ticks;
			sparkTimer += ticks;
			if (reroll)
				rerollSparks();
			if (holdTimer > 0)
				trembleOffset.setLocation(Math.round(Math.cos(holdElapsed * TREMBLE_SPEED * 0.8f)),
						Math.round(Math.sin(holdElapsed * TREMBLE_SPEED) * 1.5f));
			else
				trembleOffset.setLocation(0, 0);
		}

		if (trailTimer > 0)
			trailTimer = Math.max(0, trailTimer - ticks);
		if (crackLifeTimer > 0)
			crackLifeTimer = Math.max(0, crackLifeTimer - ticks);

		if (shakeTimer > 0) {
			shakeTimer = Math.max(0, shakeTimer - ticks);
			if (shakeTimer == 0)
				shakeOffset.setLocation(0, 0);
			else if (reroll) {
				float range = shakeTimer > shakeTicks * 0.5f ? SHAKE_RANGE : SHAKE_RANGE * 0.5f;
				shakeOffset.setLocation(Math.round((rng.nextFloat() * 2 - 1) * range),
						Math.round((rng.nextFloat() * 2 - 1) * range));
			}
		}

		if (impactTriggered) {
			float k = 1f - (float) Math.pow(1f - KNOCKBACK_EASE_PER_TICK, ticks);
			knockback += (KNOCKBACK - knockback) * k;
		}

		if (finished && holdTimer <= 0 && trailTimer <= 0 && crackLifeTimer <= 0 && shakeTimer <= 0)
			active = false;
	}

	public boolean shouldHoldFrame() {
		return holdTimer > 0;
	}

	public boolean isWorldFrozen() {
		return shouldHoldFrame() || impactTimer > 0;
	}

	public void onImpact(float targetX, float targetY) {
		if (!active || impactTriggered)
			return;
		impactTriggered = true;
		this.targetWorldX = targetX;
		this.targetX = targetX - cameraOffset;
		this.targetY = targetY;
		impactTimer = hitstopTicks;
		trailTimer = trailTicks;
		crackLifeTimer = crackLifeTicks;
		shakeTimer = shakeTicks;
		knockback = 0;
	}

	public void finish() {
		holdTimer = 0;
		holdStarted = false;
		impactTriggered = false;
		finished = true;
		knockback = 0;
		trembleOffset.setLocation(0, 0);
	}

	public void setAnchor(float x, float y, int cameraOffset) {
		anchorX = x - cameraOffset;
		anchorY = y;
		this.cameraOffset = cameraOffset;
		targetX = targetWorldX - cameraOffset;
	}

	/** Dim overlay and sword glow. Draw after the world and enemies, before the player. */
	public void renderBelowPlayer(Graphics2D g, int w, int h) {
		if (!active || holdTimer <= 0)
			return;
		int alpha = (int) (DIM_MAX_ALPHA * Math.min(1f, holdElapsed / dimRampTicks));
		g.setColor(new Color(0, 0, 0, alpha));
		g.fillRect(0, 0, w, h);

		int dir = facingRight ? 1 : -1;
		AffineTransform old = g.getTransform();
		g.translate(anchorX + dir * HAND_X + trembleOffset.x, anchorY - HAND_Y + trembleOffset.y);
		g.rotate(facingRight ? SWORD_ANGLE : -Math.PI - SWORD_ANGLE);
		g.setColor(GLOW_COLOR);
		g.fillRect(-2, -GLOW_WIDTH / 2, GLOW_LENGTH, GLOW_WIDTH);
		g.setTransform(old);
	}

	/** Charge sparks, slash trail, and ground crack. Draw after the player. */
	public void renderAbovePlayer(Graphics2D g, int w, int h) {
		if (!active)
			return;
		int dir = facingRight ? 1 : -1;

		if (holdTimer > 0) {
			int count = Math.min(MAX_SPARKS, 3 + (int) (sparkTimer / sparkIntervalTicks));
			float pivotX = anchorX + dir * HAND_X + trembleOffset.x;
			float pivotY = anchorY - HAND_Y + trembleOffset.y;
			g.setColor(Color.WHITE);
			for (int i = 0; i < count; i++)
				g.fillRect(Math.round(pivotX + dir * sparkX[i]), Math.round(pivotY + sparkY[i]), 3, 3);
		}

		// The trail and crack only appear after the hit-stop (the freeze shows the
		// inverted frame instead).
		if (impactTimer > 0)
			return;

		if (crackLifeTimer > 0)
			drawCrack(g);

		if (trailTimer > 0) {
			float thickness = TRAIL_MAX_THICKNESS * trailTimer / trailTicks;
			g.setColor(Color.WHITE);
			fillBar(g, targetX, targetY, facingRight ? SLASH_ANGLE : -SLASH_ANGLE, (int) Math.hypot(w, h),
					thickness);
		}
	}

	/** Returns the inverted scene with the slash drawn on it, for the hit-stop frames. */
	public BufferedImage applyImpactFrame(BufferedImage scene) {
		if (impactTimer <= 0)
			return scene;
		int w = scene.getWidth(), h = scene.getHeight();
		if (impactImage == null || impactImage.getWidth() != w || impactImage.getHeight() != h)
			impactImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = impactImage.createGraphics();
		g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, w, h);
		g.setComposite(AlphaComposite.SrcOver);
		g.drawImage(scene, INVERT, 0, 0);

		int len = (int) Math.hypot(w, h);
		int dir = facingRight ? 1 : -1;
		g.setColor(Color.BLACK);
		fillBar(g, targetX, targetY, dir * SLASH_ANGLE, len, SLASH_BLACK);
		g.setColor(Color.WHITE);
		fillBar(g, targetX, targetY, dir * SLASH_ANGLE, len, SLASH_WHITE);
		fillBar(g, targetX - dir * SLASH_THIN_SHIFT, targetY, dir * SLASH_THIN_ANGLE, len, SLASH_THIN);
		g.dispose();
		return impactImage;
	}

	/** A bar of the given thickness through (cx, cy), long enough to span the screen. */
	private void fillBar(Graphics2D g, float cx, float cy, double angle, int len, float thickness) {
		AffineTransform old = g.getTransform();
		g.translate(cx, cy);
		g.rotate(angle);
		int t = Math.max(1, Math.round(thickness));
		g.fillRect(-len, -t / 2, len * 2, t);
		g.setTransform(old);
	}

	private void drawCrack(Graphics2D g) {
		float age = crackLifeTicks - crackLifeTimer;
		float spread = Math.min(1f, age / crackTicks);
		float fade = Math.min(1f, crackLifeTimer / (crackLifeTicks * 0.25f));
		Stroke old = g.getStroke();
		g.setStroke(CRACK_STROKE);
		g.setColor(new Color(136, 135, 128, Math.max(0, (int) (255 * fade))));
		drawPolyline(g, CRACK_LEFT, spread);
		drawPolyline(g, CRACK_RIGHT, spread);
		g.setStroke(old);
	}

	private void drawPolyline(Graphics2D g, int[][] pts, float spread) {
		int segments = (int) (spread * (pts.length - 1));
		int gx = Math.round(targetX);
		int gy = Math.round(targetY) + CRACK_GROUND_OFFSET;
		for (int i = 0; i < segments; i++)
			g.drawLine(gx + Math.round(pts[i][0] * CRACK_SCALE), gy + Math.round(pts[i][1] * CRACK_SCALE),
					gx + Math.round(pts[i + 1][0] * CRACK_SCALE), gy + Math.round(pts[i + 1][1] * CRACK_SCALE));
	}

	/** Random sparks around the raised sword, relative to the sword pivot (mirrored when facing left). */
	private void rerollSparks() {
		for (int i = 0; i < MAX_SPARKS; i++) {
			sparkX[i] = -20 + rng.nextFloat() * 70;
			sparkY[i] = -100 + rng.nextFloat() * 80;
		}
	}

	public Point2D getShakeOffset() {
		return shakeOffset;
	}

	/** 1-2px offset to add to the player's draw position while the hold is active. */
	public Point2D getTrembleOffset() {
		return trembleOffset;
	}

	public float getKnockback() {
		return knockback;
	}

	/** True right after the freeze, so the hit enemy can be drawn white. */
	public boolean isTargetFlashing() {
		return active && impactTriggered && impactTimer <= 0 && trailTimer > trailTicks * TARGET_FLASH_FRACTION;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isImpactActive() {
		return impactTimer > 0;
	}

	public void reset() {
		active = false;
		finished = false;
		holdStarted = false;
		impactTriggered = false;
		holdTimer = 0;
		holdElapsed = 0;
		impactTimer = 0;
		trailTimer = 0;
		crackLifeTimer = 0;
		shakeTimer = 0;
		sparkTimer = 0;
		jitterTimer = 0;
		knockback = 0;
		shakeOffset.setLocation(0, 0);
		trembleOffset.setLocation(0, 0);
	}
}