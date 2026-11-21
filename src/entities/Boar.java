package entities;

import static utilz.Constants.Directions.*;
import static utilz.Constants.EnemyConstants.*;
import static utilz.Constants.ANI_SPEED;
import static utilz.Constants.BOAR_ANI_SPEED;

import java.awt.geom.Rectangle2D;

import main.Game;

/** A charging enemy with running and tusk-attack animations. */
public class Boar extends Enemy {
	public Boar(float x, float y) {
		super(x, y, BOAR_WIDTH, BOAR_HEIGHT, BOAR);
		// Keep the collision body inside the one-tile-high space used by editor spawn markers.
		initHitbox(54, 28);
		// Use a shorter distance than the full hitbox width so the boar does not stop too far away.
		attackDistance = BOAR_ATTACK_DISTANCE_DEFAULT * Game.SCALE;
		attackBox = new Rectangle2D.Float(x, y, 72 * Game.SCALE, 35 * Game.SCALE);
	}

	public void update(int[][] lvlData, Player player) {
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
				turnTowardsPlayer(player);
				if (canSeePlayer(lvlData, player)) {
					if (isPlayerCloseForAttack(player)) {
						newState(ATTACK);
						break;
					}
				}
				move(lvlData);
				break;
			case ATTACK:
				if (aniIndex == 0)
					attackChecked = false;
				if (aniIndex == 5 && !attackChecked)
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
