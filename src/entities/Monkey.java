package entities;

import static utilz.Constants.Directions.*;
import static utilz.Constants.EnemyConstants.*;
import static utilz.HelpMethods.*;

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import effects.HitEffects;
import main.Game;
import utilz.LoadSave;

/** A stationary monkey that leaps at the player when he enters its sight range. */
public class Monkey extends Enemy {
	private static final int CELL = 64;
	private static final int SIGHT_RANGE = 300;
	private static final int VERTICAL_TOLERANCE = 64;
	private static final int MAX_HEALTH = 10;
	private static final int ATTACK_DAMAGE = 15;
	private static final int ATTACK_COOLDOWN_TICKS = 300;
	private static final int IDLE_FRAME_TICKS = 24; // 120 ms at 200 updates/second
	private static final int ATTACK_FRAME_TICKS = 16; // 80 ms at 200 updates/second
	private static final int DEATH_FRAME_TICKS = 20; // 100 ms at 200 updates/second
	private static final float JUMP_SPEED = -1.25f * Game.SCALE;
	private static final float LEAP_SPEED = 2.5f * Game.SCALE;
	private static final float MAX_LEAP_SPEED = 5f * Game.SCALE;
	private static final float GRAVITY = 0.04f * Game.SCALE;
	private static final float MAX_FALL_SPEED = 4f * Game.SCALE;

	private static BufferedImage[] idleFrames;
	private static BufferedImage[] attackFrames;
	private static BufferedImage[] deathFrames;
	private static BufferedImage[] idleWhiteFrames;
	private static BufferedImage[] attackWhiteFrames;
	private static BufferedImage[] deathWhiteFrames;
	private int attackDirection;
	private boolean leaping;
	private float leapSpeed = LEAP_SPEED;

	public Monkey(float x, float y) {
		super(x, y, CELL, CELL, MONKEY);
		maxHealth = MAX_HEALTH;
		currentHealth = maxHealth;
		// Must be shorter than one tile, or GetEntityYPosUnderRoofOrAboveFloor snaps a tile too high.
		initHitbox(40, 30);
		attackBox = new Rectangle2D.Float(x, y, 42 * Game.SCALE, 28 * Game.SCALE);
		loadFrames();
	}

	public void update(int[][] lvlData, Player player) {
		updateAttackCooldown();
		if (firstUpdate) {
			tileY = (int) (hitbox.y / Game.TILES_SIZE);
			firstUpdateCheck(lvlData);
		}
		if (state == DEAD) {
			if (inAir)
				fall(lvlData);
			else
				updateAnimationTick();
			return;
		}
		updateAttackBox();
		if (state == ATTACK)
			updateAttack(lvlData, player);
		else {
			if (!inAir && !IsEntityOnFloor(hitbox, lvlData))
				inAir = true;
			if (inAir)
				fall(lvlData);
			else if (state == IDLE) {
				turnTowardsPlayer(player);
				if (canSeeMonkey(lvlData, player) && canAttack())
					startAttack(player);
			}
		}

		updateAnimationTick();
		updateAttackBox();
	}

	/** The monkey's own gravity, so it does not depend on the base class's updateInAir. */
	private void fall(int[][] lvlData) {
		if (CanMoveHere(hitbox.x, hitbox.y + airSpeed, hitbox.width, hitbox.height, lvlData)) {
			hitbox.y += airSpeed;
			airSpeed = Math.min(airSpeed + GRAVITY, MAX_FALL_SPEED);
		} else {
			hitbox.y = GetEntityYPosUnderRoofOrAboveFloor(hitbox, airSpeed);
			airSpeed = 0;
			inAir = false;
		}
		tileY = (int) (hitbox.y / Game.TILES_SIZE);
	}

	private boolean canSeeMonkey(int[][] lvlData, Player player) {
		float dx = Math.abs(centerX(player.getHitbox()) - centerX(hitbox));
		float dy = Math.abs(player.getHitbox().y - hitbox.y);
		return dx <= SIGHT_RANGE * Game.SCALE && dy <= VERTICAL_TOLERANCE * Game.SCALE
				&& IsAllTilesClear(Math.min((int) (hitbox.x / Game.TILES_SIZE),
						(int) (player.getHitbox().x / Game.TILES_SIZE)),
						Math.max((int) (hitbox.x / Game.TILES_SIZE),
								(int) (player.getHitbox().x / Game.TILES_SIZE)), tileY, lvlData);
	}

	private void startAttack(Player player) {
		float playerCenterX = player.getHitbox().x + player.getHitbox().width / 2f;
		float monkeyCenterX = hitbox.x + hitbox.width / 2f;
		attackDirection = playerCenterX >= monkeyCenterX ? RIGHT : LEFT;
		walkDir = attackDirection;
		leaping = false;
		float distance = Math.abs(playerCenterX - monkeyCenterX);
		leapSpeed = Math.min(MAX_LEAP_SPEED, Math.max(LEAP_SPEED, distance / (2f * ATTACK_FRAME_TICKS)));
		newState(ATTACK);
		attackChecked = false;
		attackCooldown = ATTACK_COOLDOWN_TICKS;
	}

	private void updateAttack(int[][] lvlData, Player player) {
		if (aniIndex >= 3) {
			if (aniIndex <= 4) {
				if (!leaping) {
					leaping = true;
					inAir = true;
					airSpeed = JUMP_SPEED;
				}
				moveLeap(lvlData);
			}
			if (inAir)
				fall(lvlData); // keep falling until the real floor
		}
		if (!attackChecked) {
			updateAttackBox();
			checkPlayerHit(attackBox, player);
		}
	}

	private void moveLeap(int[][] lvlData) {
		float dx = attackDirection == LEFT ? -leapSpeed : leapSpeed;
		if (CanMoveHere(hitbox.x + dx, hitbox.y, hitbox.width, hitbox.height, lvlData))
			hitbox.x += dx;
	}

	private void updateAttackBox() {
		attackBox.width = 42 * Game.SCALE;
		attackBox.height = 28 * Game.SCALE;
		int direction = state == ATTACK ? attackDirection : walkDir;
		attackBox.x = direction == RIGHT ? hitbox.x + hitbox.width : hitbox.x - attackBox.width;
		attackBox.y = hitbox.y + 2 * Game.SCALE;
	}

	@Override
	public void hurt(int amount) {
		if (!active || state == DEAD)
			return;
		super.hurt(amount);
		if (state == DEAD) {
			attackChecked = true;
			leaping = false;
			attackCooldown = 0;
		}
	}

	@Override
	protected void checkPlayerHit(Rectangle2D.Float attackBox, Player player) {
		if (attackBox.intersects(player.hitbox) || hitbox.intersects(player.hitbox)) {
			if (player.isBlocking()) {
				float monkeyCenterX = hitbox.x + hitbox.width / 2;
				if (player.isFacingSource(monkeyCenterX))
					parry();
			} else
				player.changeHealth(-ATTACK_DAMAGE);
			attackChecked = true; // only used up on a hit, so a miss is checked again next tick
		}
	}

	@Override
	protected int getAnimationSpeed() {
		if (state == DEAD)
			return DEATH_FRAME_TICKS;
		return state == ATTACK ? ATTACK_FRAME_TICKS : IDLE_FRAME_TICKS;
	}

	@Override
	protected void updateAnimationTick() {
		if (state == DEAD) {
			if (aniIndex < 8) {
				aniTick++;
				if (aniTick >= DEATH_FRAME_TICKS) {
					aniTick = 0;
					aniIndex++;
				}
			} else
				active = false;
			return;
		}
		int previousState = state;
		super.updateAnimationTick();
		if (previousState == ATTACK && state == IDLE)
			leaping = false; // inAir is left alone, so it keeps falling if it is still in the air
	}

	public void draw(Graphics g, int xLvlOffset) {
		if (!active)
			return;
		BufferedImage frame = state == DEAD ? deathFrames[aniIndex]
				: state == ATTACK ? attackFrames[aniIndex] : idleFrames[aniIndex];
		if (getFlashTimer() > 0)
			frame = state == DEAD ? deathWhiteFrames[aniIndex]
					: state == ATTACK ? attackWhiteFrames[aniIndex] : idleWhiteFrames[aniIndex];
		int size = (int) (CELL * Game.SCALE);
		int drawX = (int) (hitbox.x + hitbox.width / 2f) - xLvlOffset - size / 2;
		// Sprite bottom sits on the hitbox bottom, whatever the hitbox height is.
		int drawY = (int) (hitbox.y + hitbox.height - size);
		if (walkDir == LEFT)
			g.drawImage(frame, drawX, drawY, size, size, null);
		else
			g.drawImage(frame, drawX + size, drawY, -size, size, null);
	}

	@Override
	public void resetEnemy() {
		super.resetEnemy();
		attackDirection = LEFT;
		leaping = false;
		leapSpeed = LEAP_SPEED;
		attackChecked = false;
	}

	private static float centerX(Rectangle2D.Float box) {
		return box.x + box.width / 2f;
	}

	private static synchronized void loadFrames() {
		if (idleFrames != null)
			return;
		BufferedImage sheet = LoadSave.GetSpriteAtlas(LoadSave.MONKEY_SPRITE);
		idleFrames = new BufferedImage[8];
		attackFrames = new BufferedImage[8];
		deathFrames = new BufferedImage[9];
		for (int i = 0; i < 8; i++) {
			idleFrames[i] = sheet.getSubimage(i * CELL, 0, CELL, CELL);
			attackFrames[i] = sheet.getSubimage(i * CELL, CELL, CELL, CELL);
		}
		for (int i = 0; i < 9; i++)
			deathFrames[i] = sheet.getSubimage(i * CELL, 2 * CELL, CELL, CELL);
		idleWhiteFrames = whiteFrames(idleFrames);
		attackWhiteFrames = whiteFrames(attackFrames);
		deathWhiteFrames = whiteFrames(deathFrames);
	}

	private static BufferedImage[] whiteFrames(BufferedImage[] source) {
		BufferedImage[] result = new BufferedImage[source.length];
		for (int i = 0; i < source.length; i++)
			result[i] = HitEffects.whiteSilhouette(source[i]);
		return result;
	}
}
