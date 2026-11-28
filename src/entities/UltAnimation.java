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
	private static final int ULT_HOLD_FRAME = 8;
	public static final int ULT_IMPACT_FRAME = 9;

	// ---- timings (seconds, converted to ticks with UPS) ----
	private static final float HOLD_SECONDS = 0.33f;
	private static final float DIM_RAMP_SECONDS = 0.10f;
	private static final float HITSTOP_SECONDS = 0.23f;
	private static final float SHAKE_SECONDS = 0.27f;
	private static final float SLASH_TRAIL_FADE_SECONDS = 0.17f;
	private static final float GROUND_CRACK_SPREAD_SECONDS = 0.10f;
	private static final float CRACK_LIFE_SECONDS = 0.8f;
	private static final float JITTER_SECONDS = 1f / 60f; // how often the shake re-rolls

	// ---- hold look ----
	private static final int DIM_MAX_ALPHA = 115;
	private static final float TREMBLE_SPEED = 0.75f; // radians per tick
	private static final double SWORD_ANGLE = -1.9; // raised, leaning back (facing right)
	private static final int HAND_X = 24; // sword pivot relative to anchor
	private static final int HAND_Y = 12;
	private static final int GLOW_LENGTH = 64;
	private static final int GLOW_WIDTH = 16;
	private static final Color GLOW_COLOR = new Color(255, 255, 255, 64);

	// ---- gather particles (frames before the impact frame, including the hold) ----
	private static final int GATHER_START_FRAME = 6; // first overhead frame
	private static final int MAX_PARTICLES = 48;
	private static final float GATHER_SPAWN_START_PER_SEC = 40f;
	private static final float GATHER_SPAWN_END_PER_SEC = 160f;
	private static final float GATHER_RAMP_SECONDS = 0.6f; // how long the spawn rate takes to build up
	private static final float GATHER_LIFE_MIN_SECONDS = 0.22f;
	private static final float GATHER_LIFE_MAX_SECONDS = 0.40f;
	private static final float GATHER_RADIUS_MIN = 45f; // spawn distance from the sword
	private static final float GATHER_RADIUS_MAX = 95f;
	private static final float GATHER_SWIRL = 1.2f; // radians the particle curves while flying in
	private static final float GATHER_TARGET_DIST = 40f; // pivot -> point on the blade they fly to
	private static final Color TAIL_COLOR = new Color(255, 255, 255, 140);

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
	private final int jitterTicks = Math.max(1, Math.round(JITTER_SECONDS * UPS));
	private final float gatherLifeMinTicks = GATHER_LIFE_MIN_SECONDS * UPS;
	private final float gatherLifeMaxTicks = GATHER_LIFE_MAX_SECONDS * UPS;
	private final float gatherRampTicks = GATHER_RAMP_SECONDS * UPS;

	private final Random rng = new Random();
	private final Point2D.Float shakeOffset = new Point2D.Float();
	private final Point2D.Float trembleOffset = new Point2D.Float();
	// particle pool: a particle is alive while pAge < pLife
	private final float[] pAngle = new float[MAX_PARTICLES];
	private final float[] pRadius = new float[MAX_PARTICLES];
	private final float[] pAge = new float[MAX_PARTICLES];
	private final float[] pLife = new float[MAX_PARTICLES];
	private BufferedImage impactImage;
	private boolean active;
	private boolean facingRight;
	private boolean gathering;
	private boolean holdStarted;
	private boolean impactTriggered;
	private boolean finished;
	private float holdTimer;
	private float holdElapsed;
	private float impactTimer;
	private float trailTimer;
	private float crackLifeTimer;
	private float shakeTimer;
	private float jitterTimer;
	private float gatherElapsed;
	private float spawnAccum;
	private float knockback;
	private float anchorX, anchorY;
	private float targetX, targetY, targetWorldX;
	private int cameraOffset;

	public void start(boolean facingRight) {
		reset();
		active = true;
		finished = false;
		this.facingRight = facingRight;
	}

	public void update(int currentAnimFrame) {
		if (!active || finished)
			return;
		if (!gathering && !impactTriggered && currentAnimFrame >= GATHER_START_FRAME
				&& currentAnimFrame < ULT_IMPACT_FRAME) {
			gathering = true;
			gatherElapsed = 0;
			spawnAccum = 0;
		}
		if (currentAnimFrame == ULT_HOLD_FRAME && !holdStarted && !impactTriggered) {
			holdStarted = true;
			holdTimer = holdTicks;
			holdElapsed = 0;
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

		updateGather(ticks);

		if (holdTimer > 0) {
			holdTimer = Math.max(0, holdTimer - ticks);
			holdElapsed += ticks;
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

	/** Spawns new gather particles (rate builds up over time) and ages the live ones. */
	private void updateGather(float ticks) {
		if (gathering) {
			gatherElapsed += ticks;
			float ramp = Math.min(1f, gatherElapsed / gatherRampTicks);
			float rate = GATHER_SPAWN_START_PER_SEC + (GATHER_SPAWN_END_PER_SEC - GATHER_SPAWN_START_PER_SEC) * ramp;
			spawnAccum += rate * ticks / UPS;
			while (spawnAccum >= 1f) {
				spawnAccum -= 1f;
				spawnParticle();
			}
		}
		for (int i = 0; i < MAX_PARTICLES; i++)
			if (pAge[i] < pLife[i])
				pAge[i] += ticks;
	}

	private void spawnParticle() {
		for (int i = 0; i < MAX_PARTICLES; i++) {
			if (pAge[i] >= pLife[i]) {
				pAngle[i] = rng.nextFloat() * (float) (Math.PI * 2);
				pRadius[i] = GATHER_RADIUS_MIN + rng.nextFloat() * (GATHER_RADIUS_MAX - GATHER_RADIUS_MIN);
				pAge[i] = 0;
				pLife[i] = gatherLifeMinTicks + rng.nextFloat() * (gatherLifeMaxTicks - gatherLifeMinTicks);
				return;
			}
		}
	}

	private void clearParticles() {
		for (int i = 0; i < MAX_PARTICLES; i++) {
			pAge[i] = 0;
			pLife[i] = 0;
		}
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
		gathering = false;
		clearParticles();
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
		gathering = false;
		clearParticles();
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

	/** Gather particles, slash trail, and ground crack. Draw after the player. */
	public void renderAbovePlayer(Graphics2D g, int w, int h) {
		if (!active)
			return;

		if (impactTimer > 0)
			return; // the freeze shows the inverted frame instead

		drawGather(g);

		if (crackLifeTimer > 0)
			drawCrack(g);

		if (trailTimer > 0) {
			float thickness = TRAIL_MAX_THICKNESS * trailTimer / trailTicks;
			g.setColor(Color.WHITE);
			fillBar(g, targetX, targetY, facingRight ? SLASH_ANGLE : -SLASH_ANGLE, (int) Math.hypot(w, h),
					thickness);
		}
	}

	/** Particles spawn on a ring around the raised sword and curve inward, accelerating into the blade. */
	private void drawGather(Graphics2D g) {
		int dir = facingRight ? 1 : -1;
		double a = facingRight ? SWORD_ANGLE : -Math.PI - SWORD_ANGLE;
		float cx = anchorX + dir * HAND_X + trembleOffset.x + (float) (Math.cos(a) * GATHER_TARGET_DIST);
		float cy = anchorY - HAND_Y + trembleOffset.y + (float) (Math.sin(a) * GATHER_TARGET_DIST);
		for (int i = 0; i < MAX_PARTICLES; i++) {
			if (pAge[i] >= pLife[i])
				continue;
			float p = pAge[i] / pLife[i];
			float tail = Math.max(0f, p - 0.12f);
			g.setColor(TAIL_COLOR);
			g.fillRect(particleX(cx, i, tail, dir), particleY(cy, i, tail, dir), 2, 2);
			int size = p > 0.7f ? 2 : 3;
			g.setColor(Color.WHITE);
			g.fillRect(particleX(cx, i, p, dir), particleY(cy, i, p, dir), size, size);
		}
	}

	private int particleX(float cx, int i, float p, int dir) {
		float e = p * p; // accelerates inward
		return Math.round(cx + (float) Math.cos(pAngle[i] + dir * GATHER_SWIRL * e) * pRadius[i] * (1f - e));
	}

	private int particleY(float cy, int i, float p, int dir) {
		float e = p * p;
		return Math.round(cy + (float) Math.sin(pAngle[i] + dir * GATHER_SWIRL * e) * pRadius[i] * (1f - e));
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
		gathering = false;
		holdStarted = false;
		impactTriggered = false;
		holdTimer = 0;
		holdElapsed = 0;
		impactTimer = 0;
		trailTimer = 0;
		crackLifeTimer = 0;
		shakeTimer = 0;
		jitterTimer = 0;
		gatherElapsed = 0;
		spawnAccum = 0;
		knockback = 0;
		clearParticles();
		shakeOffset.setLocation(0, 0);
		trembleOffset.setLocation(0, 0);
	}
}
