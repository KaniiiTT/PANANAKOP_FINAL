package entities;

import static utilz.Constants.EnemyConstants.*;
import static utilz.Constants.GRAVITY;
import static utilz.HelpMethods.*;

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;

import effects.HitEffects;
import main.Game;
import utilz.LoadSave;

/**
 * Espinosa miniboss. Self-contained: owns its animation, AI and drawing, like Magellan.
 * Sheet: res/espinosa_spritesheet.png, 96x96 cells, one action per row.
 */
public class Espinosa extends Enemy {

	// ===================== CONFIG =====================
	// Distances and speeds are in unscaled sprite pixels; they are multiplied by Game.SCALE at use.
	// Frame indexes below are 0-based (the spec's frame numbers minus 1).

	/** Fixed step of the game loop (Playing.update() runs at 200 updates per second). */
	private static final float UPDATE_DT = 1f / 200f;

	// Sprite / hitbox
	private static final int CELL = 96;
	private static final float SPRITE_SCALE = 1.35f; // draw size relative to the 96px cell
	private static final int FEET_Y = 91;            // row inside the cell where the feet touch the ground (measured from the sheet)
	private static final int HITBOX_WIDTH = 54;
	private static final int HITBOX_HEIGHT = 116;

	// Health / stagger
	private static final int MAX_HEALTH = 120;
	private static final float STAGGER_SECONDS = 0.25f;

	// Movement / flow
	private static final float WALK_SPEED = 60f;           // pixels per second
	private static final float COOLDOWN_SECONDS = 1.2f;    // global pause after every attack
	private static final float MELEE_STOP_DISTANCE = 45f;  // chase stops walking this close (center to center)
	private static final float VERTICAL_TOLERANCE = 58f;   // max feet-height difference to start an attack

	// Frame durations (seconds per frame)
	private static final float IDLE_FRAME_SECONDS = 0.14f;
	private static final float WALK_FRAME_SECONDS = 0.10f;
	private static final float ATTACK_1_FRAME_SECONDS = 0.11f;
	private static final float ATTACK_2_FRAME_SECONDS = 0.12f;
	private static final float ATTACK_3_FRAME_SECONDS = 0.11f;
	private static final float ATTACK_FIRST_FRAME_HOLD_SECONDS = 0.25f;
	private static final float DEAD_FRAME_SECONDS = 0.12f;

	// ATTACK_1 (close)
	private static final int ATTACK_1_HIT_FIRST = 2, ATTACK_1_HIT_LAST = 5; // frames 3-6
	private static final int ATTACK_1_DAMAGE = 15;
	private static final float ATTACK_1_COOLDOWN = 2f;
	private static final float ATTACK_1_RANGE = 84f;
	private static final float ATTACK_1_REACH = 72f;

	// ATTACK_2 shield bash (mid range, runs twice)
	private static final int ATTACK_2_HIT_FIRST = 4, ATTACK_2_HIT_LAST = 5; // frames 5-6
	private static final int ATTACK_2_DAMAGE = 20;                          // per bash
	private static final float ATTACK_2_COOLDOWN = 4f;
	private static final float ATTACK_2_MIN_RANGE = 120f;
	private static final float ATTACK_2_MAX_RANGE = 276f;
	private static final float ATTACK_2_REACH = 60f;
	private static final float BASH_DISTANCE = 220f;  // fixed dash distance per bash
	private static final int BASH_DASH_FIRST = 3, BASH_DASH_LAST = 5; // frames 4-6
	private static final float BASH_GAP_SECONDS = 0.25f; // idle gap between the two bashes

	// ATTACK_3 (close)
	private static final int ATTACK_3_HIT_FIRST = 4, ATTACK_3_HIT_LAST = 5; // frames 5-6
	private static final int ATTACK_3_DAMAGE = 25;
	private static final float ATTACK_3_COOLDOWN = 5f;
	private static final float ATTACK_3_RANGE = 84f;
	private static final float ATTACK_3_REACH = 84f;

	// Shared attack box shape
	private static final float ATTACK_BOX_HEIGHT = 90f;
	private static final float ATTACK_BOX_Y_OFFSET = 14.4f; // from the top of the hitbox
	// ==================================================

	/** Rows in the sheet, in order: row index == ordinal. */
	private enum Anim {
		IDLE(6, true, IDLE_FRAME_SECONDS, IDLE_FRAME_SECONDS),
		WALKING(8, true, WALK_FRAME_SECONDS, WALK_FRAME_SECONDS),
		ATTACK_1(9, false, ATTACK_1_FRAME_SECONDS, ATTACK_FIRST_FRAME_HOLD_SECONDS),
		ATTACK_2(8, false, ATTACK_2_FRAME_SECONDS, ATTACK_FIRST_FRAME_HOLD_SECONDS),
		ATTACK_3(11, false, ATTACK_3_FRAME_SECONDS, ATTACK_FIRST_FRAME_HOLD_SECONDS),
		DEAD(8, false, DEAD_FRAME_SECONDS, DEAD_FRAME_SECONDS);

		final int frames;
		final boolean loops;
		final float frameSeconds, firstFrameSeconds;

		Anim(int frames, boolean loops, float frameSeconds, float firstFrameSeconds) {
			this.frames = frames;
			this.loops = loops;
			this.frameSeconds = frameSeconds;
			this.firstFrameSeconds = firstFrameSeconds;
		}

		float durationForFrame(int frame) {
			return frame == 0 ? firstFrameSeconds : frameSeconds;
		}
	}

	private enum Attack {
		ATTACK_1(Anim.ATTACK_1, ATTACK_1_HIT_FIRST, ATTACK_1_HIT_LAST, ATTACK_1_DAMAGE, ATTACK_1_COOLDOWN,
				0f, ATTACK_1_RANGE, ATTACK_1_REACH),
		ATTACK_2(Anim.ATTACK_2, ATTACK_2_HIT_FIRST, ATTACK_2_HIT_LAST, ATTACK_2_DAMAGE, ATTACK_2_COOLDOWN,
				ATTACK_2_MIN_RANGE, ATTACK_2_MAX_RANGE, ATTACK_2_REACH),
		ATTACK_3(Anim.ATTACK_3, ATTACK_3_HIT_FIRST, ATTACK_3_HIT_LAST, ATTACK_3_DAMAGE, ATTACK_3_COOLDOWN,
				0f, ATTACK_3_RANGE, ATTACK_3_REACH);

		final Anim anim;
		final int hitFirst, hitLast, damage;
		final float cooldownSeconds, minRange, maxRange, reach;

		Attack(Anim anim, int hitFirst, int hitLast, int damage, float cooldownSeconds, float minRange,
				float maxRange, float reach) {
			this.anim = anim;
			this.hitFirst = hitFirst;
			this.hitLast = hitLast;
			this.damage = damage;
			this.cooldownSeconds = cooldownSeconds;
			this.minRange = minRange;
			this.maxRange = maxRange;
			this.reach = reach;
		}
	}

	private enum Phase {
		CHASE, COOLDOWN, ATTACK, STAGGER, DEAD
	}

	private static final Attack[] ATTACKS = Attack.values();
	private static BufferedImage[][] frames; // [anim][frame], shared by every Espinosa
	private static BufferedImage[][] whiteFrames;

	private final Random random = new Random();
	private final Attack[] candidates = new Attack[ATTACKS.length];
	private final float[] attackCooldowns = new float[ATTACKS.length];

	// Animation
	private Anim anim = Anim.IDLE;
	private int frame;
	private float frameTimer;
	private boolean animFinished;

	// Behavior
	private Phase phase = Phase.CHASE;
	private Phase resumePhase = Phase.CHASE;
	private float cooldownTimer;
	private float staggerTimer;
	private boolean facingRight;
	private boolean defeated;

	// Attack
	private Attack currentAttack;
	private Attack lastAttack;
	private boolean hitLanded;
	private int bashesDone;
	private boolean inBashGap;
	private float gapTimer;
	private boolean dashStarted;
	private float dashRemaining;

	public Espinosa(float x, float y) {
		super(x, y, size(), size(), ESPINOSA);
		initHitbox(HITBOX_WIDTH, HITBOX_HEIGHT);
		attackBox = new Rectangle2D.Float(x, y, 0, 0);
		maxHealth = MAX_HEALTH;
		currentHealth = maxHealth;
		loadFrames();
	}

	// ---------------------------------------------------------------- update

	public void update(int[][] lvlData, Player player) {
		if (firstUpdate) {
			resolveSpawn(lvlData);
			firstUpdateCheck(lvlData);
		}
		for (int i = 0; i < attackCooldowns.length; i++)
			if (attackCooldowns[i] > 0f)
				attackCooldowns[i] -= UPDATE_DT;

		advanceAnimation(UPDATE_DT);
		applyGravity(lvlData);

		switch (phase) {
		case CHASE -> updateChase(lvlData, player);
		case COOLDOWN -> {
			cooldownTimer -= UPDATE_DT;
			if (cooldownTimer <= 0f)
				changePhase(Phase.CHASE);
		}
		case ATTACK -> updateAttack(lvlData, player);
		case STAGGER -> {
			staggerTimer -= UPDATE_DT;
			if (staggerTimer <= 0f)
				changePhase(resumePhase);
		}
		case DEAD -> {
			if (animFinished) {
				defeated = true;
				active = false;
			}
		}
		}
	}

	private void updateChase(int[][] lvlData, Player player) {
		faceTowards(player);
		if (!inAir) {
			Attack choice = pickAttack(player);
			if (choice != null) {
				startAttack(choice, player);
				return;
			}
		}
		float dx = centerX(player.getHitbox()) - centerX(hitbox);
		if (!inAir && Math.abs(dx) > px(MELEE_STOP_DISTANCE)) {
			setAnim(Anim.WALKING);
			float step = px(WALK_SPEED) * UPDATE_DT * (dx > 0 ? 1 : -1);
			if (canMove(hitbox.x + step, hitbox.y, lvlData) && IsFloor(hitbox, step, lvlData))
				hitbox.x += step;
		} else
			setAnim(Anim.IDLE);
	}

	private Attack pickAttack(Player player) {
		int n = 0;
		for (Attack a : ATTACKS)
			if (attackCooldowns[a.ordinal()] <= 0f && inRange(a, player))
				candidates[n++] = a;
		if (n == 0)
			return null;
		// Never the same attack twice in a row if another one is available.
		if (n > 1 && lastAttack != null)
			for (int i = 0; i < n; i++)
				if (candidates[i] == lastAttack) {
					candidates[i] = candidates[--n];
					break;
				}
		return candidates[random.nextInt(n)];
	}

	private boolean inRange(Attack a, Player player) {
		Rectangle2D.Float p = player.getHitbox();
		float dx = Math.abs(centerX(p) - centerX(hitbox));
		float dy = Math.abs((p.y + p.height) - (hitbox.y + hitbox.height));
		return dx >= px(a.minRange) && dx <= px(a.maxRange) && dy <= px(VERTICAL_TOLERANCE);
	}

	private void startAttack(Attack a, Player player) {
		faceTowards(player); // facing is locked from here until the attack ends
		currentAttack = a;
		lastAttack = a;
		hitLanded = false;
		bashesDone = 0;
		inBashGap = false;
		dashStarted = false;
		dashRemaining = 0f;
		changePhase(Phase.ATTACK);
		setAnim(a.anim);
	}

	private void updateAttack(int[][] lvlData, Player player) {
		if (inBashGap) {
			gapTimer -= UPDATE_DT;
			if (gapTimer <= 0f) {
				inBashGap = false;
				faceTowards(player); // face the player again before the second bash
				hitLanded = false;
				dashStarted = false;
				dashRemaining = 0f;
				setAnim(Anim.ATTACK_2);
			}
			return;
		}

		Attack a = currentAttack;
		if (a == Attack.ATTACK_2) {
			if (frame >= BASH_DASH_FIRST && frame <= BASH_DASH_LAST)
				bashStep(lvlData);
			else if (dashStarted && dashRemaining > 0f && frame > BASH_DASH_LAST) {
				// Spend the sub-step rounding left over so every bash travels the same distance.
				moveX(facingRight ? dashRemaining : -dashRemaining, lvlData);
				dashRemaining = 0f;
			}
		}
		if (!hitLanded && frame >= a.hitFirst && frame <= a.hitLast)
			tryHitPlayer(a, player);

		if (animFinished) {
			if (a == Attack.ATTACK_2 && bashesDone == 0) {
				bashesDone = 1;
				inBashGap = true;
				gapTimer = BASH_GAP_SECONDS;
				setAnim(Anim.IDLE);
			} else
				endAttack();
		}
	}

	private void bashStep(int[][] lvlData) {
		if (!dashStarted) {
			dashStarted = true;
			dashRemaining = px(BASH_DISTANCE);
		}
		// Constant speed: the whole distance is covered across the dash frames.
		int dashFrames = BASH_DASH_LAST - BASH_DASH_FIRST + 1;
		float speed = px(BASH_DISTANCE) / (dashFrames * ATTACK_2_FRAME_SECONDS);
		float step = Math.min(speed * UPDATE_DT, dashRemaining);
		if (step <= 0f)
			return;
		if (moveX(facingRight ? step : -step, lvlData))
			dashRemaining -= step;
		else
			dashRemaining = 0f; // hit a wall: stop early
	}

	private void tryHitPlayer(Attack a, Player player) {
		attackBox.width = px(a.reach);
		attackBox.height = px(ATTACK_BOX_HEIGHT);
		attackBox.x = facingRight ? hitbox.x + hitbox.width : hitbox.x - attackBox.width;
		attackBox.y = hitbox.y + px(ATTACK_BOX_Y_OFFSET);
		if (attackBox.intersects(player.getHitbox())) {
			hitLanded = true; // one hit per swing, even if the player was blocking
			player.changeHealth(-a.damage);
		}
	}

	private void endAttack() {
		attackCooldowns[currentAttack.ordinal()] = currentAttack.cooldownSeconds;
		clearAttackState();
		cooldownTimer = COOLDOWN_SECONDS;
		changePhase(Phase.COOLDOWN);
		setAnim(Anim.IDLE);
	}

	private void clearAttackState() {
		currentAttack = null;
		hitLanded = false;
		bashesDone = 0;
		inBashGap = false;
		dashStarted = false;
		dashRemaining = 0f;
		gapTimer = 0f;
		attackBox.width = 0;
		attackBox.height = 0;
	}

	// ---------------------------------------------------------------- damage

	@Override
	public void hurt(int amount) {
		if (!active || phase == Phase.DEAD)
			return;
		currentHealth -= amount;
		if (currentHealth <= 0) {
			currentHealth = 0;
			die();
		} else if (phase != Phase.ATTACK && phase != Phase.STAGGER) {
			// Stagger only when not attacking; hits during a stagger don't extend it.
			resumePhase = phase;
			staggerTimer = STAGGER_SECONDS;
			changePhase(Phase.STAGGER);
			setAnim(Anim.IDLE);
		}
	}

	private void die() {
		clearAttackState();
		changePhase(Phase.DEAD);
		state = DEAD; // lets EnemyManager's existing "already dead" checks skip him
		setAnim(Anim.DEAD);
	}

	/** A miniboss is not pushed around by the player's attacks. */
	@Override
	public void applyKnockback(int amount, int direction) {
	}

	/** Blocking doesn't stagger him; the player's block already cancels the damage. */
	@Override
	public void parry() {
	}

	// ---------------------------------------------------------------- helpers

	private void changePhase(Phase next) {
		if (next == phase)
			return;
		System.out.println("[Espinosa] " + phase + " -> " + next
				+ (next == Phase.ATTACK && currentAttack != null ? " (" + currentAttack + ")" : ""));
		phase = next;
	}

	private void setAnim(Anim next) {
		if (next == anim)
			return;
		anim = next;
		frame = 0;
		frameTimer = 0f;
		animFinished = false;
	}

	private void advanceAnimation(float dt) {
		if (animFinished)
			return;
		frameTimer += dt;
		while (frameTimer >= anim.durationForFrame(frame)) {
			frameTimer -= anim.durationForFrame(frame);
			frame++;
			if (frame >= anim.frames) {
				if (anim.loops)
					frame = 0;
				else {
					frame = anim.frames - 1;
					animFinished = true;
					frameTimer = 0f;
					return;
				}
			}
		}
	}

	private void faceTowards(Player player) {
		facingRight = centerX(player.getHitbox()) > centerX(hitbox);
	}

	private static float centerX(Rectangle2D.Float r) {
		return r.x + r.width / 2f;
	}

	private static float px(float unscaled) {
		return unscaled * Game.SCALE;
	}

	private static int size() {
		return (int) (CELL * SPRITE_SCALE * Game.SCALE);
	}

	/** The hitbox is taller than a tile, so it also checks the body's middle against walls. */
	private boolean canMove(float newX, float newY, int[][] lvlData) {
		return CanMoveHere(newX, newY, hitbox.width, hitbox.height, lvlData)
				&& CanMoveHere(newX, newY + hitbox.height / 2f, hitbox.width, 0f, lvlData);
	}

	/** Moves horizontally one pixel at a time; returns false if a wall stopped the move. */
	private boolean moveX(float dx, int[][] lvlData) {
		float dir = Math.signum(dx);
		float left = Math.abs(dx);
		while (left > 0f) {
			float step = Math.min(1f, left);
			if (!canMove(hitbox.x + dir * step, hitbox.y, lvlData))
				return false;
			hitbox.x += dir * step;
			left -= step;
		}
		return true;
	}

	/** Marker tiles sit above the floor, but a tall hitbox would start inside it: lift it out first. */
	private void resolveSpawn(int[][] lvlData) {
		for (int i = 0; i < 8 && !canMove(hitbox.x, hitbox.y, lvlData); i++)
			hitbox.y -= Game.TILES_SIZE;
	}

	private void applyGravity(int[][] lvlData) {
		if (!inAir && !IsEntityOnFloor(hitbox, lvlData))
			inAir = true;
		if (!inAir)
			return;
		if (CanMoveHere(hitbox.x, hitbox.y + airSpeed, hitbox.width, hitbox.height, lvlData)) {
			hitbox.y += airSpeed;
			airSpeed += GRAVITY;
			return;
		}
		// Same landing approach as Magellan: Enemy's helper assumes hitboxes shorter than a tile.
		int leftTile = Math.max(0, (int) (hitbox.x / Game.TILES_SIZE));
		int rightTile = Math.min(lvlData[0].length - 1, (int) ((hitbox.x + hitbox.width) / Game.TILES_SIZE));
		int row = (int) ((hitbox.y + hitbox.height) / Game.TILES_SIZE);
		while (row < lvlData.length && !IsTileSolid(leftTile, row, lvlData) && !IsTileSolid(rightTile, row, lvlData))
			row++;
		if (row < lvlData.length) {
			hitbox.y = row * Game.TILES_SIZE - hitbox.height - 1;
			tileY = (int) (hitbox.y / Game.TILES_SIZE);
			inAir = false;
			airSpeed = 0;
		}
	}

	// ---------------------------------------------------------------- drawing

	public void draw(Graphics g, int xLvlOffset) {
		BufferedImage img = (flashTimer > 0 ? whiteFrames : frames)[anim.ordinal()][frame];
		int size = size();
		int drawX = (int) centerX(hitbox) - xLvlOffset - size / 2;
		int drawY = (int) (hitbox.y + hitbox.height) - (int) (FEET_Y * SPRITE_SCALE * Game.SCALE);
		if (facingRight) // the sheet faces right
			g.drawImage(img, drawX, drawY, size, size, null);
		else
			g.drawImage(img, drawX + size, drawY, -size, size, null);
	}

	private static synchronized void loadFrames() {
		if (frames != null)
			return;
		BufferedImage sheet = LoadSave.GetSpriteAtlas(LoadSave.ESPINOSA_SPRITE);
		Anim[] anims = Anim.values();
		int maxFrames = 0;
		for (Anim a : anims)
			maxFrames = Math.max(maxFrames, a.frames);
		if (sheet.getWidth() < maxFrames * CELL || sheet.getHeight() < anims.length * CELL)
			throw new IllegalStateException("Espinosa sheet must be at least " + maxFrames * CELL + "x"
					+ anims.length * CELL + " (96x96 cells) but is " + sheet.getWidth() + "x" + sheet.getHeight());
		BufferedImage[][] loaded = new BufferedImage[anims.length][];
		BufferedImage[][] white = new BufferedImage[anims.length][];
		for (Anim a : anims) {
			loaded[a.ordinal()] = new BufferedImage[a.frames];
			white[a.ordinal()] = new BufferedImage[a.frames];
			for (int f = 0; f < a.frames; f++) {
				loaded[a.ordinal()][f] = sheet.getSubimage(f * CELL, a.ordinal() * CELL, CELL, CELL);
				white[a.ordinal()][f] = HitEffects.whiteSilhouette(loaded[a.ordinal()][f]);
			}
		}
		whiteFrames = white;
		frames = loaded;
	}

	// ---------------------------------------------------------------- misc

	/** True once the death animation has finished (he stays drawn on the last frame). */
	public boolean isDefeated() {
		return defeated;
	}

	public int getHealth() {
		return currentHealth;
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	@Override
	public void resetEnemy() {
		super.resetEnemy();
		Arrays.fill(attackCooldowns, 0f);
		clearAttackState();
		lastAttack = null;
		phase = Phase.CHASE;
		resumePhase = Phase.CHASE;
		cooldownTimer = 0f;
		staggerTimer = 0f;
		defeated = false;
		facingRight = false;
		setFlashTimer(0f);
		anim = Anim.IDLE;
		frame = 0;
		frameTimer = 0f;
		animFinished = false;
	}
}
