package entities;

import static utilz.Constants.Directions.*;
import static utilz.Constants.EnemyConstants.*;
import static utilz.Constants.ANI_SPEED;
import static utilz.Constants.CONQUISTADOR_ANI_SPEED;
import static utilz.Constants.CONQUISTADOR_FIRST_FRAME_ANI_SPEED;
import static utilz.Constants.CONQUISTADOR_ATTACK_HOLD_FRAME;
import static utilz.Constants.CONQUISTADOR_WALK_FIRST_FRAME_ANI_SPEED;
import static utilz.Constants.EnemyConstants.CONQUISTADOR_STOP_DISTANCE_DEFAULT;
import static utilz.HelpMethods.*;

import java.awt.geom.Rectangle2D;

import main.Game;

/** A patrolling enemy with walk, sword attack, hit, and death animations. */
public class Conquistador extends Enemy {
	private static final int PATROL_TILES = 3;
	// The attack has nine frames; frame numbers 5-9 are indices 4-8.
	private static final int DASH_START_FRAME = 4;
	private static final int DASH_END_FRAME = 8;
	// Tune these two values to adjust the dash without changing its animation window.
	private static final float DASH_SPEED = 2f * Game.SCALE;
	private static final float DASH_DISTANCE = 150f * Game.SCALE;
	private final float patrolLeft;
	private final float patrolRight;
	private float dashDistanceRemaining;
	private int patrolIdleTicks;
	private boolean idleForAttackCooldown;
	private boolean reverseAfterPatrolIdle;

	public Conquistador(float x, float y) {
		super(x, y, CONQUISTADOR_WIDTH, CONQUISTADOR_HEIGHT, CONQUISTADOR);
		patrolLeft = x - PATROL_TILES * Game.TILES_SIZE;
		patrolRight = x + PATROL_TILES * Game.TILES_SIZE;
		initHitbox(54, 28);
		attackDistance = CONQUISTADOR_ATTACK_DISTANCE_DEFAULT * Game.SCALE;
		attackBox = new Rectangle2D.Float(x, y, 72 * Game.SCALE, 35 * Game.SCALE);
	}

	public void update(int[][] lvlData, Player player) {
		updateAttackCooldown();
		if (firstUpdate)
			firstUpdateCheck(lvlData);
		if (inAir)
			updateInAir(lvlData);
		else {
			switch (state) {
			case IDLE -> {
				if (patrolIdleTicks > 0)
					patrolIdleTicks--;
				else if (idleForAttackCooldown && !canAttack()
						&& isPlayerCloseForAttack(player)) {
					// Remain idle until the attack cooldown ends or the player moves away.
				}
				else {
					idleForAttackCooldown = false;
					if (reverseAfterPatrolIdle) {
						changeWalkDir();
						reverseAfterPatrolIdle = false;
					}
					newState(RUNNING);
				}
			}
			case RUNNING -> {
				if (canSeePlayer(lvlData, player)) {
					turnTowardsPlayer(player);
					if (isPlayerCloseForAttack(player)) {
						if (canAttack()) {
							idleForAttackCooldown = false;
							newState(ATTACK);
							startAttackCooldown();
							dashDistanceRemaining = DASH_DISTANCE;
						} else {
							idleForAttackCooldown = true;
							newState(IDLE);
						}
						break;
					}
				} else {
					patrol(lvlData);
					break;
				}
					move(lvlData);
			}
			case ATTACK -> {
				if (aniIndex == 0)
					attackChecked = false;
				if (dashDistanceRemaining > 0 && aniIndex >= DASH_START_FRAME && aniIndex <= DASH_END_FRAME)
					dash(lvlData);
				if (aniIndex == 4 && !attackChecked)
					checkPlayerHit(attackBox, player);
			}
			case HIT -> { }
			}
		}
			updateAnimationTick();
		attackBox.x = walkDir == RIGHT
				? hitbox.x + hitbox.width - 2 * Game.SCALE
				: hitbox.x - attackBox.width + 2 * Game.SCALE;
		attackBox.y = hitbox.y;
	}

	private void dash(int[][] lvlData) {
		float dashStep = Math.min(DASH_SPEED, dashDistanceRemaining);
		float xSpeed = walkDir == LEFT ? -dashStep : dashStep;
		if (CanMoveHere(hitbox.x + xSpeed, hitbox.y, hitbox.width, hitbox.height, lvlData)
				&& IsFloor(hitbox, xSpeed, lvlData))
			hitbox.x += xSpeed;
		else {
			dashDistanceRemaining = 0;
			return;
		}
		dashDistanceRemaining -= dashStep;
	}

	private void patrol(int[][] lvlData) {
		if ((walkDir == LEFT && hitbox.x <= patrolLeft)
				|| (walkDir == RIGHT && hitbox.x >= patrolRight)) {
			patrolIdleTicks = CONQUISTADOR_PATROL_IDLE_TICKS;
			idleForAttackCooldown = false;
			reverseAfterPatrolIdle = true;
			newState(IDLE);
			return;
		}
		move(lvlData);
	}

	@Override
	protected boolean isPlayerCloseForAttack(Player player) {
		float conquistadorCenter = hitbox.x + hitbox.width / 2;
		float playerCenter = player.getHitbox().x + player.getHitbox().width / 2;
		return Math.abs(playerCenter - conquistadorCenter)
				<= CONQUISTADOR_STOP_DISTANCE_DEFAULT * Game.SCALE;
	}

	public int flipX() {
		return walkDir == LEFT ? width : 0;
	}

	public int flipW() {
		return walkDir == LEFT ? -1 : 1;
	}

	@Override
	protected int getAnimationSpeed() {
		if (state == ATTACK && aniIndex == CONQUISTADOR_ATTACK_HOLD_FRAME)
			return CONQUISTADOR_FIRST_FRAME_ANI_SPEED;
		if (state == RUNNING && aniIndex == 0)
			return CONQUISTADOR_WALK_FIRST_FRAME_ANI_SPEED;
		return state == RUNNING || state == ATTACK || state == HIT
				? CONQUISTADOR_ANI_SPEED : ANI_SPEED;
	}

	@Override
	public void resetEnemy() {
		super.resetEnemy();
		dashDistanceRemaining = 0;
		patrolIdleTicks = 0;
		idleForAttackCooldown = false;
		reverseAfterPatrolIdle = false;
	}
}
