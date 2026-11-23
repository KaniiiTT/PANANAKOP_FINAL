package entities;

import static utilz.Constants.Directions.*;
import static utilz.Constants.EnemyConstants.*;
import static utilz.Constants.ANI_SPEED;
import static utilz.Constants.BOAR_ANI_SPEED;

import java.awt.geom.Rectangle2D;

import main.Game;

/** A charging enemy with running and tusk-attack animations. */
public class Boar extends Enemy {
	private static final int PATROL_TILES = 3;
	private final float patrolLeft;
	private final float patrolRight;

	public Boar(float x, float y) {
		super(x, y, BOAR_WIDTH, BOAR_HEIGHT, BOAR);
		patrolLeft = x - PATROL_TILES * Game.TILES_SIZE;
		patrolRight = x + PATROL_TILES * Game.TILES_SIZE;
		// Keep the collision body inside the one-tile-high space used by editor spawn markers.
		initHitbox(54, 28);
		// Use a shorter distance than the full hitbox width so the boar does not stop too far away.
		attackDistance = BOAR_ATTACK_DISTANCE_DEFAULT * Game.SCALE;
		attackBox = new Rectangle2D.Float(x, y, 72 * Game.SCALE, 35 * Game.SCALE);
	}

	@Override
	public void hurt(int amount) {
		if (!active || state == DEAD)
			return;
		currentHealth -= amount;
		newState(currentHealth <= 0 ? DEAD : IDLE);
	}

	public void update(int[][] lvlData, Player player) {
		updateAttackCooldown();
		if (firstUpdate)
			firstUpdateCheck(lvlData);
		if (inAir)
			updateInAir(lvlData);
		else {
			switch (state) {
			case IDLE:
				newState(RUNNING);
				break;
			case RUNNING:
				boolean playerVisible = canSeePlayer(lvlData, player);
				if (playerVisible) {
					turnTowardsPlayer(player);
					if (isPlayerCloseForAttack(player) && canAttack()) {
						newState(ATTACK);
						startAttackCooldown();
						break;
					}
				} else {
					patrol(lvlData);
					break;
				}
				move(lvlData);
				break;
			case ATTACK:
				if (aniIndex == 0)
					attackChecked = false;
				if (aniIndex == 4 && !attackChecked)
					checkPlayerHit(attackBox, player);
				break;
			case HIT:
				break;
			}
		}
		updateAnimationTick();
		attackBox.x = walkDir == RIGHT
				? hitbox.x + hitbox.width - 2 * Game.SCALE
				: hitbox.x - attackBox.width + 2 * Game.SCALE;
		attackBox.y = hitbox.y;
	}

	private void patrol(int[][] lvlData) {
		if ((walkDir == LEFT && hitbox.x <= patrolLeft)
				|| (walkDir == RIGHT && hitbox.x >= patrolRight))
			changeWalkDir();

		move(lvlData);
	}

	public int flipX() {
		return walkDir == RIGHT ? width : 0;
	}

	public int flipW() {
		return walkDir == RIGHT ? -1 : 1;
	}

	@Override
	protected int getAnimationSpeed() {
		return state == RUNNING || state == ATTACK || state == HIT ? BOAR_ANI_SPEED : ANI_SPEED;
	}
}
