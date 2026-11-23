package entities;

import static utilz.Constants.PlayerConstants.*;
import static utilz.HelpMethods.*;
import static utilz.Constants.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import gamestates.Playing;
import main.Game;
import utilz.LoadSave;
import utilz.SoundManager;

public class Player extends Entity {

	private BufferedImage[][] animations;
	private AnimationDefinition[] animationDefinitions;
	private boolean moving = false, attacking = false, blocking = false;
	private boolean left, right, jump, running;
	private boolean jumpConsumed, dashing, usingUltimate, victory;
	private int jumpsUsed;
	private int landingTicks, hitTicks;
	private int[][] lvlData;

	// Jumping / Gravity
	private float jumpSpeed = -2.25f * Game.SCALE;
	private float fallSpeedAfterCollision = 0.5f * Game.SCALE;

	// Dash (Hollow Knight style: fixed distance, fixed direction, no gravity)
	// === DASH TUNING ===
	private float dashSpeed = 2.8f * Game.SCALE; // pixels per tick (lower = slower)
	private float dashDistance = 175f * Game.SCALE; // total travel distance
	private float dashRemaining = 0;
	private int dashDir = 1;
	private int dashCooldownTicks = 0;
	private static final int DASH_COOLDOWN = 25;
	private boolean airDashUsed = false; // one dash per airtime, reset on landing

	// StatusBarUI
	private BufferedImage statusBarImg;

	private int statusBarWidth = (int) (192 * Game.SCALE);
	private int statusBarHeight = (int) (58 * Game.SCALE);
	private int statusBarX = (int) (10 * Game.SCALE);
	private int statusBarY = (int) (10 * Game.SCALE);

	private int healthBarWidth = (int) (150 * Game.SCALE);
	private int healthBarHeight = (int) (4 * Game.SCALE);
	private int healthBarXStart = (int) (34 * Game.SCALE);
	private int healthBarYStart = (int) (14 * Game.SCALE);
	private int healthWidth = healthBarWidth;

	private int flipW = 1;

	private int attackCheckedFrame = -1;
	private AttackPhase attackPhase = AttackPhase.NONE;
	private boolean attackQueued;
	private int attackGraceTicks;
	private float attackLungeRemaining;
	private float horizontalMomentum;
	private boolean attackStartedInAir;
	private final Set<Enemy> attackHitTargets = Collections.newSetFromMap(new IdentityHashMap<>());
	private Playing playing;

	private int tileY = 0;
	private String spriteSheet = LoadSave.MALE_PLAYER_ATLAS;

	public Player(float x, float y, int width, int height, Playing playing) {
		super(x, y, width, height);
		this.playing = playing;
		this.state = IDLE;
		this.maxHealth = 100;
		this.currentHealth = 35;
		this.walkSpeed = Game.SCALE * 1.0f;
		loadAnimations();
		initHitbox(20, 27);
		initAttackBox();
	}

	public void setSpawn(Point spawn) {
		this.x = spawn.x;
		this.y = spawn.y;
		hitbox.x = x;
		hitbox.y = y;
		// Spawn markers are tile positions, not the final hitbox floor position.
		// Let gravity settle the player onto the first solid tile before walking.
		inAir = true;
		airSpeed = 0;
		jumpsUsed = 0;
	}

	private void initAttackBox() {
		attackBox = new Rectangle2D.Float(x, y, (int) (20 * Game.SCALE), (int) (20 * Game.SCALE));
	}

	public void update() {
		updateHealthBar();

		if (currentHealth <= 0) {
			if (state != DEAD) {
				state = DEAD;
				aniTick = 0;
				aniIndex = 0;
				playing.setPlayerDying(true);
			} else if (aniIndex == getAnimationFrameCount(DEAD) - 1
					&& aniTick >= getAnimationSpeed(DEAD) - 1) {
				playing.setGameOver(true);
			} else
				updateAnimationTick();

			return;
		}

		updateAttackBox();

		boolean wasInAir = inAir;
		updatePos();
		if (wasInAir && !inAir)
			landingTicks = ANI_SPEED;
		if (landingTicks > 0)
			landingTicks--;
		if (hitTicks > 0)
			hitTicks--;
		if (dashCooldownTicks > 0)
			dashCooldownTicks--;
		if (attackGraceTicks > 0 && attackPhase == AttackPhase.NONE)
			attackGraceTicks--;
		if (moving) {
			checkPotionTouched();
			checkSpikesTouched();
			tileY = (int) (hitbox.y / Game.TILES_SIZE);
		}
		if (attackPhase != AttackPhase.NONE || usingUltimate)
			checkAttack();

		updateAnimationTick();
		setAnimation();
	}

	private void checkSpikesTouched() {
		playing.checkSpikesTouched(this);

	}

	private void checkPotionTouched() {
		playing.checkPotionTouched(hitbox);
	}

	private void checkAttack() {
		if (attackPhase == AttackPhase.NONE) {
			int hitFrame = getAttackHitFrame(aniIndex);
			if (hitFrame < 0 || attackCheckedFrame == hitFrame)
				return;
			attackCheckedFrame = hitFrame;
			playing.checkEnemyHit(attackBox);
			playing.checkObjectHit(attackBox);
			return;
		}

		AttackDefinition attack = getCurrentAttackDefinition();
		if (aniIndex < attack.hitActiveStart || aniIndex > attack.hitActiveEnd)
			return;
		playing.checkEnemyHit(attackBox, attack.damage, attack.knockback, flipW, attackHitTargets);
		playing.checkObjectHit(attackBox);
	}

	private void updateAttackBox() {
		if (attackPhase != AttackPhase.NONE) {
			AttackDefinition attack = getCurrentAttackDefinition();
			attackBox.width = attack.hitboxWidth * Game.SCALE;
			attackBox.height = attack.hitboxHeight * Game.SCALE;
			attackBox.x = flipW > 0
					? hitbox.x + hitbox.width + attack.hitboxOffsetX * Game.SCALE
					: hitbox.x - attackBox.width - attack.hitboxOffsetX * Game.SCALE;
			attackBox.y = hitbox.y + attack.hitboxOffsetY * Game.SCALE;
			return;
		}
		if (dashing) {
			// Follow the dash direction rather than the held keys.
			if (dashDir > 0)
				attackBox.x = hitbox.x + hitbox.width + (int) (Game.SCALE * 10);
			else
				attackBox.x = hitbox.x - hitbox.width - (int) (Game.SCALE * 10);
		} else if (right)
			attackBox.x = hitbox.x + hitbox.width + (int) (Game.SCALE * 10);
		else if (left)
			attackBox.x = hitbox.x - hitbox.width - (int) (Game.SCALE * 10);

		attackBox.y = hitbox.y + (Game.SCALE * 10);
	}

	private void updateHealthBar() {
		healthWidth = (int) ((currentHealth / (float) maxHealth) * healthBarWidth);
	}

	public void render(Graphics g, int lvlOffset) {
		AnimationDefinition animation = animationDefinitions[state];
		int frameWidth = (int) (animation.frameWidth * Game.SCALE);
		int frameHeight = (int) (animation.frameHeight * Game.SCALE);
		int anchorX = (int) (hitbox.x + hitbox.width / 2f) - lvlOffset;
		int groundY = (int) (hitbox.y + hitbox.height + PLAYER_SPRITE_OFFSET_Y * Game.SCALE);
		int drawX = anchorX - frameWidth / 2;
		int drawY = groundY - frameHeight;
		Graphics2D graphics = (Graphics2D) g.create();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		if (flipW == -1)
			drawX += frameWidth;
		graphics.drawImage(animations[state][aniIndex], drawX, drawY, frameWidth * flipW, frameHeight, null);
		graphics.dispose();
//		drawHitbox(g, lvlOffset);
//		drawAttackBox(g, lvlOffset);
		drawUI(g);
	}

	private void drawUI(Graphics g) {
		g.drawImage(statusBarImg, statusBarX, statusBarY, statusBarWidth, statusBarHeight, null);
		g.setColor(Color.red);
		g.fillRect(healthBarXStart + statusBarX, healthBarYStart + statusBarY, healthWidth, healthBarHeight);
	}

	private void updateAnimationTick() {
		aniTick++;
		int speed = attackPhase != AttackPhase.NONE ? getCurrentAttackDefinition().frameDuration
				: getAnimationSpeed(state);
		if (aniTick >= speed) {
			aniTick = 0;
			if (attackPhase != AttackPhase.NONE) {
				aniIndex++;
				AttackDefinition attack = getCurrentAttackDefinition();
				if (attackPhase == AttackPhase.HORIZONTAL && attackQueued
						&& aniIndex > attack.hitActiveEnd) {
					beginAttackPhase(AttackPhase.VERTICAL);
					return;
				}
				if (aniIndex > attack.endFrame)
					finishAttackPhase();
				return;
			}
			AnimationDefinition animation = animationDefinitions[state];
			if (animation.holdFinalFrame) {
				aniIndex = Math.min(aniIndex + 1, animation.frameCount - 1);
				return;
			}
			aniIndex++;
			if (animation.looping && aniIndex > animation.loopEndFrame) {
				aniIndex = animation.loopStartFrame;
			} else if (!animation.looping && aniIndex >= animation.frameCount) {
				aniIndex = animation.frameCount - 1;
				attacking = false;
				attackCheckedFrame = -1;
				// Dash now ends by distance (see updateDash), not by animation length.
				if (state == ULTIMATE)
					usingUltimate = false;
			}
		}
	}

	private void setAnimation() {
		int startAni = state;

		if (victory)
			state = VICTORY;
		else if (dashing)
			state = DASH;
		else if (usingUltimate)
			state = ULTIMATE;
		else if (blocking)
			state = BLOCK;
		else if (attacking)
			state = ATTACK;
		else if (hitTicks > 0)
			state = HIT;
		else if (landingTicks > ANI_SPEED / 2)
			state = GROUND;
		else if (landingTicks > 0)
			state = LANDING;
		else if (inAir && jumpsUsed >= 2 && airSpeed < 0)
			state = DOUBLE_JUMP;
		else if (inAir && airSpeed < 0)
			state = JUMP;
		else if (inAir)
			state = FALLING;
		else if (moving && running)
			state = RUNNING;
		else if (moving)
			state = WALKING;
		else
			state = IDLE;
		if (startAni != state)
			resetAniTick();
	}

	private void resetAniTick() {
		aniTick = 0;
		aniIndex = animationDefinitions[state].initialFrame;
		if (state == ATTACK)
			attackCheckedFrame = -1;
	}

	private void beginAttackPhase(AttackPhase phase) {
		attackPhase = phase;
		attackQueued = false;
		attackGraceTicks = 0;
		attackHitTargets.clear();
		attacking = true;
		attackStartedInAir = inAir;
		attackLungeRemaining = ATTACK_LUNGE_DISTANCE * Game.SCALE;
		if (inAir && (horizontalMomentum == 0 || left != right)) {
			float speed = running ? walkSpeed * 1.7f : walkSpeed;
			horizontalMomentum = right ? speed : left ? -speed : 0;
		}
		if (right)
			flipW = 1;
		else if (left)
			flipW = -1;
		AttackDefinition attack = getCurrentAttackDefinition();
		aniIndex = attack.startFrame;
		aniTick = 0;
		attackCheckedFrame = -1;
	}

	private void finishAttackPhase() {
		if (attackPhase == AttackPhase.HORIZONTAL && attackQueued) {
			beginAttackPhase(AttackPhase.VERTICAL);
			return;
		}
		if (attackPhase == AttackPhase.VERTICAL && attackQueued) {
			beginAttackPhase(AttackPhase.HORIZONTAL);
			return;
		}
		boolean horizontal = attackPhase == AttackPhase.HORIZONTAL;
		attackPhase = AttackPhase.NONE;
		attackQueued = false;
		attacking = false;
		attackGraceTicks = horizontal ? ATTACK_CHAIN_GRACE_TICKS : 0;
		attackHitTargets.clear();
		attackCheckedFrame = -1;
		attackLungeRemaining = 0;
		attackStartedInAir = false;
		aniIndex = 0;
	}

	private void cancelAttackPhase() {
		attackPhase = AttackPhase.NONE;
		attackQueued = false;
		attacking = false;
		attackGraceTicks = 0;
		attackHitTargets.clear();
		attackCheckedFrame = -1;
		attackLungeRemaining = 0;
		attackStartedInAir = false;
	}

	private AttackDefinition getCurrentAttackDefinition() {
		return attackPhase == AttackPhase.VERTICAL ? ATTACK_VERTICAL : ATTACK_HORIZONTAL;
	}

	private boolean canCancelAttack() {
		if (attackPhase == AttackPhase.NONE)
			return true;
		return aniIndex > getCurrentAttackDefinition().hitActiveEnd;
	}

	private void updatePos() {
		moving = false;

		// Dash overrides everything: fixed direction, fixed distance, no gravity.
		if (dashing) {
			updateDash();
			return;
		}

		if (jump && !jumpConsumed) {
			jump();
			jumpConsumed = true;
		} else if (!jump)
			jumpConsumed = false;

		boolean hasHorizontalInput = left != right;
		boolean attackMovementLocked = LOCK_MOVEMENT_DURING_ATTACK && attackPhase != AttackPhase.NONE;
		boolean attackLungeActive = attackPhase != AttackPhase.NONE && !attackStartedInAir
				&& attackLungeRemaining > 0;
		if (!inAir && !hasHorizontalInput)
			horizontalMomentum = 0;
		if (!inAir && !hasHorizontalInput && !attackLungeActive)
			return;

		float xSpeed = 0;
		float speed = running ? walkSpeed * 1.7f : walkSpeed;

		if (!attackMovementLocked) {
			if (left) {
				xSpeed -= speed;
				horizontalMomentum = -speed;
				flipW = -1;
			}
			if (right) {
				xSpeed += speed;
				horizontalMomentum = speed;
				flipW = 1;
			}
		}
		if (attackPhase != AttackPhase.NONE && inAir)
			xSpeed += horizontalMomentum;
		if (attackLungeActive) {
			float lungeStep = Math.min(ATTACK_LUNGE_SPEED * Game.SCALE, attackLungeRemaining);
			xSpeed += flipW * lungeStep;
			attackLungeRemaining -= lungeStep;
		}

		if (!inAir)
			if (!IsEntityOnFloor(hitbox, lvlData) && !isOnCrumblingPlatform())
				inAir = true;

		if (inAir) {
			float platformTop = getCrumblingPlatformLandingY(hitbox.y + hitbox.height + airSpeed);
			if (platformTop >= 0 && airSpeed >= 0) {
				hitbox.y = platformTop - hitbox.height;
				resetInAir();
				updateXPos(xSpeed);
			} else if (CanMoveHere(hitbox.x, hitbox.y + airSpeed, hitbox.width, hitbox.height, lvlData)) {
				hitbox.y += airSpeed;
				airSpeed += GRAVITY;
				updateXPos(xSpeed);
			} else {
				hitbox.y = GetEntityYPosUnderRoofOrAboveFloor(hitbox, airSpeed);
				if (airSpeed > 0)
					resetInAir();
				else
					airSpeed = fallSpeedAfterCollision;
				updateXPos(xSpeed);
			}

		} else
			updateXPos(xSpeed);
		moving = hasHorizontalInput;
	}

	private boolean isOnCrumblingPlatform() {
		return getCrumblingPlatformLandingY(hitbox.y + hitbox.height + 1) >= 0;
	}

	private float getCrumblingPlatformLandingY(float nextFeetY) {
		if (playing.getObjectManager() == null)
			return -1;
		float currentFeetY = hitbox.y + hitbox.height;
		for (objects.Trap trap : playing.getObjectManager().getTraps()) {
			if (!(trap instanceof objects.CrumblingPlatformTrap))
				continue;
			objects.CrumblingPlatformTrap platform = (objects.CrumblingPlatformTrap) trap;
			if (!platform.isPlatformSolid())
				continue;
			boolean horizontalOverlap = hitbox.x < platform.getPlatformRight()
					&& hitbox.x + hitbox.width > platform.getPlatformLeft();
			float platformTop = platform.getPlatformTop();
			if (horizontalOverlap && currentFeetY <= platformTop + 2 && nextFeetY >= platformTop)
				return platformTop;
		}
		return -1;
	}

	private void updateDash() {
		float step = Math.min(dashSpeed, dashRemaining);
		float xSpeed = step * dashDir;

		if (CanMoveHere(hitbox.x + xSpeed, hitbox.y, hitbox.width, hitbox.height, lvlData)) {
			hitbox.x += xSpeed;
			dashRemaining -= step;
		} else {
			// Hit a wall: snap next to it and end the dash early.
			hitbox.x = GetEntityXPosNextToWall(hitbox, xSpeed);
			dashRemaining = 0;
		}

		airSpeed = 0; // no gravity while dashing
		moving = true;

		if (dashRemaining <= 0)
			endDash();
	}

	private void endDash() {
		dashing = false;
		dashRemaining = 0;
		dashCooldownTicks = DASH_COOLDOWN;
		airSpeed = 0;

		// If the dash carried us off a ledge, start falling.
		if (!IsEntityOnFloor(hitbox, lvlData))
			inAir = true;
	}

	private void jump() {
		if (jumpsUsed >= 2)
			return;
		SoundManager.play(SoundManager.Effect.JUMP);
		inAir = true;
		airSpeed = jumpSpeed;
		jumpsUsed++;
	}

	public void bounceFromTrap() {
		if (inAir && airSpeed < 0)
			return;
		SoundManager.play(SoundManager.Effect.BOUNCE);
		inAir = true;
		airSpeed = jumpSpeed * 1.5f;
		jumpsUsed = 0;
	}

	private void resetInAir() {
		inAir = false;
		airSpeed = 0;
		jumpsUsed = 0;
		airDashUsed = false;
	}

	private void updateXPos(float xSpeed) {
		if (CanMoveHere(hitbox.x + xSpeed, hitbox.y, hitbox.width, hitbox.height, lvlData))
			hitbox.x += xSpeed;
		else
			hitbox.x = GetEntityXPosNextToWall(hitbox, xSpeed);
	}

	public void changeHealth(int value) {
		if (blocking && value < 0)
			return;
		if (value < 0)
			SoundManager.play(SoundManager.Effect.HIT);
		currentHealth += value;
		if (value < 0)
			hitTicks = ANI_SPEED * 2;

		if (currentHealth <= 0)
			currentHealth = 0;
		else if (currentHealth >= maxHealth)
			currentHealth = maxHealth;
	}

	public void kill() {
		if (!blocking)
			currentHealth = 0;
	}

	public boolean isBlocking() {
		return blocking;
	}

	public boolean isFacingSource(float sourceX) {
		float playerCenterX = hitbox.x + hitbox.width / 2;
		return flipW > 0 ? sourceX >= playerCenterX : sourceX <= playerCenterX;
	}

	public void changePower(int value) {
		System.out.println("Added power!");
	}

	private void loadAnimations() {
		boolean splitMaleSheet = LoadSave.MALE_PLAYER_ATLAS.equals(spriteSheet);
		animationDefinitions = splitMaleSheet ? createMaleAnimationDefinitions() : createLegacyAnimationDefinitions();
		animations = new BufferedImage[animationDefinitions.length][];
		for (int state = 0; state < animationDefinitions.length; state++) {
			AnimationDefinition animation = animationDefinitions[state];
			BufferedImage sheet = LoadSave.GetSpriteAtlas(animation.sheet);
			validateSheet(animation, sheet);
			animations[state] = new BufferedImage[animation.frameCount];
			for (int frame = 0; frame < animation.frameCount; frame++)
				animations[state][frame] = sheet.getSubimage(frame * animation.frameWidth,
						animation.row * animation.frameHeight, animation.frameWidth, animation.frameHeight);
		}

		statusBarImg = LoadSave.GetSpriteAtlas(LoadSave.STATUS_BAR);
	}

	private AnimationDefinition[] createMaleAnimationDefinitions() {
		String main = LoadSave.MALE_PLAYER_ATLAS;
		String continuation = LoadSave.MALE_PLAYER_CONTINUATION_ATLAS;
		return new AnimationDefinition[] {
			new AnimationDefinition(main, 64, 64, 0, 9, 17, true, false),
			new AnimationDefinition(main, 64, 64, 1, 9, 17, true, false),
			new AnimationDefinition(continuation, 96, 64, 1, 9, continuationFrameTicks(RUNNING_FRAME_SECONDS),
					continuationFrameTicks(RUNNING_FRAME_SECONDS), 0, 1, 2, 8, true, false),
			new AnimationDefinition(continuation, 96, 64, 0, 13,
					continuationFrameTicks(ATTACK_HORIZONTAL_FRAME_SECONDS), false, false),
			new AnimationDefinition(main, 64, 64, 2, 5, 20, false, true),
			new AnimationDefinition(main, 64, 64, 3, 5, 20, false, false),
			new AnimationDefinition(main, 64, 64, 4, 9, 12, false, false),
			new AnimationDefinition(main, 64, 64, 5, 5, 20, false, false),
			new AnimationDefinition(main, 64, 64, 6, 9, 14, false, false),
			new AnimationDefinition(main, 64, 64, 7, 5, 20, true, false),
			new AnimationDefinition(main, 64, 64, 8, 5, 20, false, false),
			new AnimationDefinition(main, 64, 64, 9, 9, 18, false, true),
			new AnimationDefinition(main, 64, 64, 10, 9, 18, false, true),
			new AnimationDefinition(continuation, 96, 64, 2, 5, continuationFrameTicks(DASH_FRAME_SECONDS), false, true),
			new AnimationDefinition(continuation, 96, 64, 3, 9, continuationFrameTicks(ULTIMATE_FRAME_SECONDS), false, false)
		};
	}

	private AnimationDefinition[] createLegacyAnimationDefinitions() {
		int[] frameCounts = { 6, 8, 7, 6, 4, 4, 6, 4, 6, 3, 4, 8, 6, 5, 8 };
		AnimationDefinition[] definitions = new AnimationDefinition[15];
		for (int state = 0; state < definitions.length; state++)
			definitions[state] = new AnimationDefinition(spriteSheet, 64, 64, state,
					frameCounts[state], ANI_SPEED, false, state == BLOCK || state == DASH);
		return definitions;
	}

	private void validateSheet(AnimationDefinition animation, BufferedImage sheet) {
		int requiredWidth = animation.frameWidth * animation.frameCount;
		int requiredHeight = animation.frameHeight * (animation.row + 1);
		if (sheet.getWidth() < requiredWidth || sheet.getHeight() < requiredHeight)
			throw new IllegalStateException("Sprite sheet '" + animation.sheet + "' is too small for player animation row "
					+ animation.row + ". Expected at least " + requiredWidth + "x" + requiredHeight + ".");
	}

	private int getAnimationFrameCount(int state) {
		return animationDefinitions[state].frameCount;
	}

	private int getAnimationSpeed(int state) {
		AnimationDefinition animation = animationDefinitions[state];
		return animation.hasIntro() && aniIndex <= animation.introEndFrame
				? animation.introFrameDelay : animation.frameDelay;
	}

	private int getAttackHitFrame(int frame) {
		if (state == ULTIMATE)
			return frame == ULTIMATE_HIT_FRAME ? frame : -1;
		return -1;
	}

	private static class AnimationDefinition {
		private final String sheet;
		private final int frameWidth, frameHeight, row, frameCount;
		private final int introFrameDelay, frameDelay, initialFrame, introEndFrame;
		private final int loopStartFrame, loopEndFrame;
		private final boolean looping, holdFinalFrame;

		private AnimationDefinition(String sheet, int frameWidth, int frameHeight, int row, int frameCount,
				int frameDelay, boolean looping, boolean holdFinalFrame) {
			this(sheet, frameWidth, frameHeight, row, frameCount, frameDelay, frameDelay, looping ? RUN_START_FRAME : 0,
					-1, looping ? RUN_START_FRAME : 0, looping ? frameCount - 1 : -1, looping, holdFinalFrame);
		}

		private AnimationDefinition(String sheet, int frameWidth, int frameHeight, int row, int frameCount,
				int introFrameDelay, int frameDelay, int initialFrame, int introEndFrame, int loopStartFrame,
				int loopEndFrame, boolean looping, boolean holdFinalFrame) {
			this.sheet = sheet;
			this.frameWidth = frameWidth;
			this.frameHeight = frameHeight;
			this.row = row;
			this.frameCount = frameCount;
			this.introFrameDelay = introFrameDelay;
			this.frameDelay = frameDelay;
			this.initialFrame = initialFrame;
			this.introEndFrame = introEndFrame;
			this.loopStartFrame = loopStartFrame;
			this.loopEndFrame = loopEndFrame;
			this.looping = looping;
			this.holdFinalFrame = holdFinalFrame;
		}

		private boolean hasIntro() {
			return introEndFrame >= initialFrame;
		}
	}

	private enum AttackPhase {
		NONE,
		HORIZONTAL,
		VERTICAL
	}

	// This changes the hero without creating a second Player class.
	public void setSpriteSheet(String spriteSheet) {
		this.spriteSheet = spriteSheet;
		loadAnimations();
		state = IDLE;
		aniTick = 0;
		aniIndex = 0;
	}

	public void loadLvlData(int[][] lvlData) {
		this.lvlData = lvlData;
		if (!IsEntityOnFloor(hitbox, lvlData))
			inAir = true;
	}

	public void resetDirBooleans() {
		left = false;
		right = false;
		running = false;
		blocking = false;
	}

	public void setAttacking(boolean attacking) {
		if (!attacking)
			return;
		if (attackPhase == AttackPhase.HORIZONTAL) {
			attackQueued = true;
			return;
		}
		if (attackPhase == AttackPhase.VERTICAL) {
			if (aniIndex >= ATTACK_VERTICAL.endFrame - 1)
				attackQueued = true;
			return;
		}
		beginAttackPhase(attackGraceTicks > 0 ? AttackPhase.VERTICAL : AttackPhase.HORIZONTAL);
	}

	public void setBlocking(boolean blocking) {
		if (blocking && !canCancelAttack())
			return;
		if (blocking)
			cancelAttackPhase();
		this.blocking = blocking;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	// Call this once per key press. The direction is locked to where the
	// player is facing when the dash starts, no matter what keys are held.
	public void setDash(boolean dash) {
		if (!dash || dashing || dashCooldownTicks > 0 || !canCancelAttack())
			return;
		if (inAir && airDashUsed)
			return;

		dashing = true;
		dashDir = flipW;
		dashRemaining = dashDistance;
		if (inAir)
			airDashUsed = true;
		cancelAttackPhase();
		attackCheckedFrame = -1;
		aniTick = 0;
		aniIndex = 0;
	}

	public void setUltimate(boolean ultimate) {
		if (ultimate && !usingUltimate) {
			usingUltimate = true;
			aniTick = 0;
			aniIndex = 0;
		}
	}

	public void setVictory(boolean victory) {
		this.victory = victory;
	}

	public boolean isLeft() {
		return left;
	}

	public void setLeft(boolean left) {
		this.left = left;
	}

	public boolean isRight() {
		return right;
	}

	public void setRight(boolean right) {
		this.right = right;
	}

	public void setJump(boolean jump) {
		if (jump && !canCancelAttack())
			return;
		if (jump)
			cancelAttackPhase();
		this.jump = jump;
	}

	public void resetAll() {
		resetDirBooleans();
		inAir = false;
		attacking = false;
		attackPhase = AttackPhase.NONE;
		attackQueued = false;
		attackGraceTicks = 0;
		attackHitTargets.clear();
		attackCheckedFrame = -1;
		blocking = false;
		dashing = false;
		dashRemaining = 0;
		dashCooldownTicks = 0;
		airDashUsed = false;
		usingUltimate = false;
		victory = false;
		moving = false;
		jumpsUsed = 0;
		landingTicks = 0;
		hitTicks = 0;
		state = IDLE;
		currentHealth = maxHealth;

		hitbox.x = x;
		hitbox.y = y;

		if (!IsEntityOnFloor(hitbox, lvlData))
			inAir = true;
	}

	public int getTileY() {
		return tileY;
	}

}
