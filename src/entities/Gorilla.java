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

/** A miniboss that chases the player and uses a slam or leap attack. */
public class Gorilla extends Enemy {
	private static final int CELL = 64;
	private static final float SPRITE_SCALE = 2f;
	private static final int SIGHT_RANGE = 450;
	private static final int SLAM_RANGE = 96;
	private static final int LEAP_RANGE = 220;
	private static final int VERTICAL_TOLERANCE = 64;
	private static final int MAX_HEALTH = 80;
	private static final int SLAM_DAMAGE = 25;
	private static final int LEAP_DAMAGE = 20;
	private static final int ATTACK_COOLDOWN_TICKS = 300;
	private static final int IDLE_FRAME_TICKS = 32;
	private static final int WALK_FRAME_TICKS = 28;
	private static final int ATTACK_FRAME_TICKS = 24;
	private static final int DEATH_FRAME_TICKS = 20;
	private static final int LEAP_TAKEOFF_FIRST = 2;
	private static final int LEAP_TAKEOFF_LAST = 4;
	private static final int ATTACK_HIT_FRAME = 5;
	private static final float WALK_SPEED = 0.45f * Game.SCALE;
	private static final float JUMP_SPEED = -1.75f * Game.SCALE;
	private static final float LEAP_SPEED = 1.25f * Game.SCALE;
	private static final float MAX_LEAP_SPEED = 2.5f * Game.SCALE;
	private static final float GRAVITY = 0.04f * Game.SCALE;
	private static final float MAX_FALL_SPEED = 4f * Game.SCALE;

	private enum AttackType { SLAM, LEAP }

	private static BufferedImage[] idleFrames;
	private static BufferedImage[] walkFrames;
	private static BufferedImage[] slamFrames;
	private static BufferedImage[] leapFrames;
	private static BufferedImage[] deathFrames;
	private static BufferedImage[] idleWhiteFrames;
	private static BufferedImage[] walkWhiteFrames;
	private static BufferedImage[] slamWhiteFrames;
	private static BufferedImage[] leapWhiteFrames;
	private static BufferedImage[] deathWhiteFrames;

	private AttackType attackType;
	private int attackDirection;
	private boolean leaping;
	private float leapSpeed = LEAP_SPEED;

	public Gorilla(float x, float y) {
		super(x, y, CELL, CELL, GORILLA);
		maxHealth = MAX_HEALTH;
		currentHealth = maxHealth;
		initHitbox(44, 30);
		attackBox = new Rectangle2D.Float(x, y, 62 * Game.SCALE, 34 * Game.SCALE);
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

		if (state == ATTACK)
			updateAttack(lvlData, player);
		else if (!inAir && !IsEntityOnFloor(hitbox, lvlData))
			inAir = true;
		else if (inAir)
			fall(lvlData);
		else
			updateGround(lvlData, player);

		updateAnimationTick();
		updateAttackBox();
	}

	private void updateGround(int[][] lvlData, Player player) {
		if (state == IDLE) {
			if (canSeeGorilla(lvlData, player)) {
				turnTowardsPlayer(player);
				newState(RUNNING);
			}
			return;
		}
		if (state == HIT) {
			return;
		}

		if (!canSeeGorilla(lvlData, player)) {
			newState(IDLE);
			return;
		}
		turnTowardsPlayer(player);
		float distance = Math.abs(centerX(player.getHitbox()) - centerX(hitbox));
		if (canAttack()) {
			if (distance <= SLAM_RANGE * Game.SCALE)
				startAttack(AttackType.SLAM, player);
			else if (distance >= LEAP_RANGE * Game.SCALE)
				startAttack(AttackType.LEAP, player);
			else
				moveTowardPlayer(lvlData, player);
			return;
		}
		if (distance > SLAM_RANGE * Game.SCALE)
			moveTowardPlayer(lvlData, player);
	}

	private void moveTowardPlayer(int[][] lvlData, Player player) {
		float dx = centerX(player.getHitbox()) > centerX(hitbox) ? WALK_SPEED : -WALK_SPEED;
		if (CanMoveHere(hitbox.x + dx, hitbox.y, hitbox.width, hitbox.height, lvlData)
				&& IsFloor(hitbox, dx, lvlData))
			hitbox.x += dx;
	}

	private boolean canSeeGorilla(int[][] lvlData, Player player) {
		float dx = Math.abs(centerX(player.getHitbox()) - centerX(hitbox));
		float dy = Math.abs((player.getHitbox().y + player.getHitbox().height)
				- (hitbox.y + hitbox.height));
		return dx <= SIGHT_RANGE * Game.SCALE && dy <= VERTICAL_TOLERANCE * Game.SCALE
				&& IsAllTilesClear(Math.min((int) (hitbox.x / Game.TILES_SIZE),
						(int) (player.getHitbox().x / Game.TILES_SIZE)),
						Math.max((int) (hitbox.x / Game.TILES_SIZE),
								(int) (player.getHitbox().x / Game.TILES_SIZE)), tileY, lvlData);
	}

	private void startAttack(AttackType type, Player player) {
		attackType = type;
		attackDirection = centerX(player.getHitbox()) >= centerX(hitbox) ? RIGHT : LEFT;
		walkDir = attackDirection;
		leaping = false;
		if (type == AttackType.LEAP) {
			float distance = Math.abs(centerX(player.getHitbox()) - centerX(hitbox));
			leapSpeed = Math.min(MAX_LEAP_SPEED,
					Math.max(LEAP_SPEED, distance / (3f * ATTACK_FRAME_TICKS)));
		}
		newState(ATTACK);
		attackChecked = false;
		attackCooldown = ATTACK_COOLDOWN_TICKS;
	}

	private void updateAttack(int[][] lvlData, Player player) {
		if (attackType == AttackType.LEAP) {
			if (aniIndex >= LEAP_TAKEOFF_FIRST && aniIndex <= LEAP_TAKEOFF_LAST) {
				if (!leaping) {
					leaping = true;
					inAir = true;
					airSpeed = JUMP_SPEED;
				}
				moveLeap(lvlData);
			}
			if (inAir)
				fall(lvlData);
		}
		if (!attackChecked && (attackType == AttackType.LEAP || aniIndex == ATTACK_HIT_FRAME)) {
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
		attackBox.width = 62 * Game.SCALE;
		attackBox.height = 34 * Game.SCALE;
		int direction = state == ATTACK ? attackDirection : walkDir;
		attackBox.x = direction == RIGHT ? hitbox.x + hitbox.width : hitbox.x - attackBox.width;
		attackBox.y = hitbox.y;
	}

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

	@Override
	public void hurt(int amount) {
		if (!active || state == DEAD)
			return;
		currentHealth -= amount;
		if (currentHealth <= 0) {
			currentHealth = 0;
			attackType = null;
			attackChecked = true;
			leaping = false;
			attackBox.width = 0;
			attackBox.height = 0;
			newState(DEAD);
		} else if (state != ATTACK) {
			newState(HIT);
		}
	}

	@Override
	protected void checkPlayerHit(Rectangle2D.Float attackBox, Player player) {
		if (!attackBox.intersects(player.getHitbox()) && !hitbox.intersects(player.getHitbox())) {
			return;
		}
		if (player.isBlocking()) {
			float gorillaCenterX = hitbox.x + hitbox.width / 2f;
			if (player.isFacingSource(gorillaCenterX))
				parry();
		} else {
			player.changeHealth(-(attackType == AttackType.SLAM ? SLAM_DAMAGE : LEAP_DAMAGE));
		}
		attackChecked = true;
	}

	@Override
	protected int getAnimationSpeed() {
		if (state == DEAD)
			return DEATH_FRAME_TICKS;
		if (state == ATTACK)
			return ATTACK_FRAME_TICKS;
		return state == RUNNING ? WALK_FRAME_TICKS : IDLE_FRAME_TICKS;
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
		if (previousState == ATTACK && state == IDLE) {
			attackType = null;
			leaping = false;
		}
	}

	public void draw(Graphics g, int xLvlOffset) {
		if (!active)
			return;
		BufferedImage frame;
		if (state == DEAD)
			frame = deathFrames[aniIndex];
		else if (state == ATTACK)
			frame = attackType == AttackType.SLAM ? slamFrames[aniIndex] : leapFrames[aniIndex];
		else if (state == RUNNING)
			frame = walkFrames[aniIndex];
		else
			frame = idleFrames[aniIndex];
		if (getFlashTimer() > 0) {
			if (state == DEAD)
				frame = deathWhiteFrames[aniIndex];
			else if (state == ATTACK)
				frame = attackType == AttackType.SLAM ? slamWhiteFrames[aniIndex] : leapWhiteFrames[aniIndex];
			else if (state == RUNNING)
				frame = walkWhiteFrames[aniIndex];
			else
				frame = idleWhiteFrames[aniIndex];
		}
		int size = (int) (CELL * SPRITE_SCALE * Game.SCALE);
		int drawX = (int) centerX(hitbox) - xLvlOffset - size / 2;
		int drawY = (int) (hitbox.y + hitbox.height - size);
		if (state == DEAD)
			drawY += (int) (5 * Game.SCALE);
		if (walkDir == LEFT)
			g.drawImage(frame, drawX, drawY, size, size, null);
		else
			g.drawImage(frame, drawX + size, drawY, -size, size, null);
	}

	@Override
	public void resetEnemy() {
		super.resetEnemy();
		attackType = null;
		attackDirection = LEFT;
		leaping = false;
		leapSpeed = LEAP_SPEED;
		attackChecked = false;
	}

	public int getHealth() {
		return currentHealth;
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	private static float centerX(Rectangle2D.Float box) {
		return box.x + box.width / 2f;
	}

	private static synchronized void loadFrames() {
		if (idleFrames != null)
			return;
		BufferedImage sheet = LoadSave.GetSpriteAtlas(LoadSave.GORILLA_SPRITE);
		idleFrames = new BufferedImage[6];
		walkFrames = new BufferedImage[8];
		slamFrames = new BufferedImage[8];
		leapFrames = new BufferedImage[8];
		deathFrames = new BufferedImage[9];
		for (int i = 0; i < 6; i++)
			idleFrames[i] = sheet.getSubimage(i * CELL, 0, CELL, CELL);
		for (int i = 0; i < 8; i++) {
			walkFrames[i] = sheet.getSubimage(i * CELL, CELL, CELL, CELL);
			slamFrames[i] = sheet.getSubimage(i * CELL, 2 * CELL, CELL, CELL);
			leapFrames[i] = sheet.getSubimage(i * CELL, 3 * CELL, CELL, CELL);
		}
		for (int i = 0; i < 9; i++)
			deathFrames[i] = sheet.getSubimage(i * CELL, 4 * CELL, CELL, CELL);
		idleWhiteFrames = whiteFrames(idleFrames);
		walkWhiteFrames = whiteFrames(walkFrames);
		slamWhiteFrames = whiteFrames(slamFrames);
		leapWhiteFrames = whiteFrames(leapFrames);
		deathWhiteFrames = whiteFrames(deathFrames);
	}

	private static BufferedImage[] whiteFrames(BufferedImage[] source) {
		BufferedImage[] result = new BufferedImage[source.length];
		for (int i = 0; i < source.length; i++)
			result[i] = HitEffects.whiteSilhouette(source[i]);
		return result;
	}
}
