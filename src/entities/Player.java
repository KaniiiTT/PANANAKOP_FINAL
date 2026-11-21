package entities;

import static utilz.Constants.PlayerConstants.*;
import static utilz.HelpMethods.*;
import static utilz.Constants.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import gamestates.Playing;
import main.Game;
import utilz.LoadSave;
import utilz.SoundManager;

public class Player extends Entity {

	private BufferedImage[][] animations;
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
	private float dashDistance = 125f * Game.SCALE; // total travel distance
	private int dashAniSpeed = ANI_SPEED; // computed per dash so the animation fits it
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

	private boolean attackChecked;
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
			} else if (aniIndex == GetSpriteAmount(DEAD) - 1 && aniTick >= ANI_SPEED - 1) {
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
		if (moving) {
			checkPotionTouched();
			checkSpikesTouched();
			tileY = (int) (hitbox.y / Game.TILES_SIZE);
		}
		if (attacking || usingUltimate)
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
		if (attackChecked || aniIndex != 1)
			return;
		attackChecked = true;
		playing.checkEnemyHit(attackBox);
		playing.checkObjectHit(attackBox);
	}

	private void updateAttackBox() {
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
		// Keep the sprite's feet on the bottom of the collision box.
		// This is important because the new sprite sheet uses 64x64 frames.
		int drawX = (int) (hitbox.x - (width - hitbox.width) / 2) - lvlOffset;
		int drawY = (int) (hitbox.y + hitbox.height - height + PLAYER_SPRITE_OFFSET_Y);
		if (flipW == -1)
			drawX += width;
		g.drawImage(animations[state][aniIndex], drawX, drawY, width * flipW, height, null);
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
		int speed = (state == DASH && dashing) ? dashAniSpeed : ANI_SPEED;
		if (aniTick >= speed) {
			aniTick = 0;
			if (state == BLOCK) {
				// Blocking is a hold pose: play the transition once, then keep
				// displaying its final frame while the block button is held.
				aniIndex = Math.min(aniIndex + 1, GetSpriteAmount(BLOCK) - 1);
				return;
			}
			aniIndex++;
			if (state == DASH && dashing && aniIndex >= GetSpriteAmount(DASH)) {
				aniIndex = GetSpriteAmount(DASH) - 1; // hold last frame, don't loop
			} else if (aniIndex >= GetSpriteAmount(state)) {
				aniIndex = state == RUNNING ? RUN_START_FRAME : 0;
				attacking = false;
				attackChecked = false;
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
		aniIndex = state == RUNNING ? RUN_START_FRAME : 0;
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

		if (!inAir)
			if ((!left && !right) || (right && left))
				return;

		float xSpeed = 0;
		float speed = running ? walkSpeed * 1.7f : walkSpeed;

		if (left) {
			xSpeed -= speed;
			flipW = -1;
		}
		if (right) {
			xSpeed += speed;
			flipW = 1;
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
		moving = true;
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
		BufferedImage img = LoadSave.GetSpriteAtlas(spriteSheet);
		animations = new BufferedImage[15][];
		for (int j = 0; j < animations.length; j++) {
			animations[j] = new BufferedImage[GetSpriteAmount(j)];
			for (int i = 0; i < animations[j].length; i++)
				animations[j][i] = img.getSubimage(i * 64, j * 64, 64, 64);
		}

		statusBarImg = LoadSave.GetSpriteAtlas(LoadSave.STATUS_BAR);
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
		this.attacking = attacking;
	}

	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	// Call this once per key press. The direction is locked to where the
	// player is facing when the dash starts, no matter what keys are held.
	public void setDash(boolean dash) {
		if (!dash || dashing || dashCooldownTicks > 0)
			return;
		if (inAir && airDashUsed)
			return;

		dashing = true;
		dashDir = flipW;
		dashRemaining = dashDistance;
		// Stretch the dash animation so it spans the whole dash.
		int dashTicks = Math.round(dashDistance / dashSpeed);
		dashAniSpeed = Math.max(1, Math.round(dashTicks / (float) GetSpriteAmount(DASH)));
		if (inAir)
			airDashUsed = true;
		attacking = false;
		attackChecked = false;
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
		this.jump = jump;
	}

	public void resetAll() {
		resetDirBooleans();
		inAir = false;
		attacking = false;
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
