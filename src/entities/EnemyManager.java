package entities;

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import gamestates.Playing;
import effects.HitEffects;
import levels.Level;
import utilz.LoadSave;
import static utilz.Constants.EnemyConstants.*;

public class EnemyManager {

	private Playing playing;
	private BufferedImage[][] crabbyArr;
	private BufferedImage[][] boarArr;
	private BufferedImage[][] conquistadorArr;
	private BufferedImage[][] crabbyWhiteArr;
	private BufferedImage[][] boarWhiteArr;
	private BufferedImage[][] conquistadorWhiteArr;
	private ArrayList<Crabby> crabbies = new ArrayList<>();
	private ArrayList<Boar> boars = new ArrayList<>();
	private ArrayList<Conquistador> conquistadors = new ArrayList<>();
	private ArrayList<Magellan> magellans = new ArrayList<>();
	private ArrayList<Espinosa> espinosas = new ArrayList<>();

	public EnemyManager(Playing playing) {
		this.playing = playing;
		loadEnemyImgs();
	}

	public void loadEnemies(Level level) {
		crabbies = level.getCrabs();
		boars = level.getBoars();
		conquistadors = level.getConquistadors();
		magellans = level.getMagellans();
		espinosas = level.getEspinosas();
	}

	public void update(int[][] lvlData, Player player) {
		boolean isAnyActive = false;
		for (Crabby c : crabbies)
			if (c.isActive()) {
				c.update(lvlData, player);
				isAnyActive = true;
			}
		for (Boar boar : boars)
			if (boar.isActive()) {
				boar.update(lvlData, player);
				isAnyActive = true;
			}
		for (Conquistador conquistador : conquistadors)
			if (conquistador.isActive()) {
				conquistador.update(lvlData, player);
				isAnyActive = true;
			}
		for (Magellan boss : magellans)
			if (boss.isActive()) {
				boss.update(lvlData, player);
				isAnyActive = true;
			}
		for (Espinosa e : espinosas)
			if (e.isActive()) {
				e.update(lvlData, player);
				// A dead Espinosa stays drawn on his last frame but no longer blocks level completion.
				if (!e.isDefeated())
					isAnyActive = true;
			}
		if (!isAnyActive)
			playing.setLevelCompleted(true);
	}

	public void updateFlash(float dt) {
		for (Crabby c : crabbies)
			c.updateFlash(dt);
		for (Boar boar : boars)
			boar.updateFlash(dt);
		for (Conquistador conquistador : conquistadors)
			conquistador.updateFlash(dt);
		for (Magellan boss : magellans)
			boss.updateFlash(dt);
		for (Espinosa e : espinosas)
			e.updateFlash(dt);
	}

	public void draw(Graphics g, int xLvlOffset) {
		drawCrabs(g, xLvlOffset);
		drawBoars(g, xLvlOffset);
		drawConquistadors(g, xLvlOffset);
		for (Magellan boss : magellans)
			if (boss.isActive())
				boss.draw(g, xLvlOffset);
		for (Espinosa e : espinosas)
			if (e.isActive())
				e.draw(g, xLvlOffset);
	}

	private void drawBoars(Graphics g, int xLvlOffset) {
		for (Boar boar : boars)
			if (boar.isActive())
				g.drawImage(boar.getFlashTimer() > 0 ? boarWhiteArr[boar.getState()][boar.getAniIndex()]
						: boarArr[boar.getState()][boar.getAniIndex()],
						(int) boar.getHitbox().x - xLvlOffset - BOAR_DRAWOFFSET_X + boar.flipX(),
						(int) boar.getHitbox().y - BOAR_DRAWOFFSET_Y,
						BOAR_WIDTH * boar.flipW(), BOAR_HEIGHT, null);
	}

	private void drawConquistadors(Graphics g, int xLvlOffset) {
		for (Conquistador conquistador : conquistadors)
			if (conquistador.isActive())
				g.drawImage(conquistador.getFlashTimer() > 0
						? conquistadorWhiteArr[conquistador.getState()][conquistador.getAniIndex()]
						: conquistadorArr[conquistador.getState()][conquistador.getAniIndex()],
						(int) conquistador.getHitbox().x - xLvlOffset - CONQUISTADOR_DRAWOFFSET_X + conquistador.flipX(),
						(int) conquistador.getHitbox().y - CONQUISTADOR_DRAWOFFSET_Y - 2,
						CONQUISTADOR_WIDTH * conquistador.flipW(), CONQUISTADOR_HEIGHT, null);
	}

	/**
	 * A living boss (Magellan or the Espinosa mini-boss) within {@code range} pixels of the player, or null.
	 * Used by the music system to start boss music.
	 */
	public Enemy findBossNear(Player player, float range) {
		float px = player.getHitbox().x + player.getHitbox().width / 2f;
		float py = player.getHitbox().y;
		for (Magellan boss : magellans)
			if (boss.isActive() && boss.getState() != DEAD && isNear(boss, px, py, range))
				return boss;
		for (Espinosa e : espinosas)
			if (e.isActive() && !e.isDefeated() && e.getState() != DEAD && isNear(e, px, py, range))
				return e;
		return null;
	}

	private static boolean isNear(Enemy enemy, float px, float py, float range) {
		return Math.abs(enemy.getHitbox().x + enemy.getHitbox().width / 2f - px) <= range
				&& Math.abs(enemy.getHitbox().y - py) <= range; // same floor area, not just the same column
	}

	public Magellan getActiveBoss() {
		for (Magellan boss : magellans)
			if (boss.isActive())
				return boss;
		return null;
	}

	private void drawCrabs(Graphics g, int xLvlOffset) {
		for (Crabby c : crabbies)
			if (c.isActive()) {

				g.drawImage(c.getFlashTimer() > 0 ? crabbyWhiteArr[c.getState()][c.getAniIndex()]
						: crabbyArr[c.getState()][c.getAniIndex()], (int) c.getHitbox().x - xLvlOffset - CRABBY_DRAWOFFSET_X + c.flipX(), (int) c.getHitbox().y - CRABBY_DRAWOFFSET_Y,
						CRABBY_WIDTH * c.flipW(), CRABBY_HEIGHT, null);

//				c.drawHitbox(g, xLvlOffset);
//				c.drawAttackBox(g, xLvlOffset);
			}

	}

	public boolean checkEnemyHit(Rectangle2D.Float attackBox) {
		return checkEnemyHit(attackBox, 10, 0, 1, new HashSet<>());
	}

	public boolean checkEnemyHit(Rectangle2D.Float attackBox, int damage, int knockback, int direction,
			Set<Enemy> hitTargets) {
		return checkEnemyHit(attackBox, damage, knockback, direction, hitTargets, false);
	}

	public boolean checkEnemyHit(Rectangle2D.Float attackBox, int damage, int knockback, int direction,
			Set<Enemy> hitTargets, boolean heavy) {
		for (Crabby c : crabbies)
			if (hitEnemy(c, attackBox, damage, knockback, direction, hitTargets, heavy))
				return true;
		for (Boar boar : boars)
			if (hitEnemy(boar, attackBox, damage, knockback, direction, hitTargets, heavy))
				return true;
		for (Conquistador conquistador : conquistadors)
			if (hitEnemy(conquistador, attackBox, damage, knockback, direction, hitTargets, heavy))
				return true;
		for (Magellan boss : magellans)
			if (hitEnemy(boss, attackBox, damage, knockback, direction, hitTargets, heavy))
				return true;
		for (Espinosa e : espinosas)
			if (hitEnemy(e, attackBox, damage, knockback, direction, hitTargets, heavy))
				return true;
		return false;
	}

	private boolean hitEnemy(Enemy enemy, Rectangle2D.Float attackBox, int damage, int knockback, int direction,
			Set<Enemy> hitTargets, boolean heavy) {
		if (!enemy.isActive() || enemy.getState() == DEAD || hitTargets.contains(enemy)
				|| !attackBox.intersects(enemy.getHitbox()))
			return false;
		hitTargets.add(enemy);
		enemy.hurt(damage);
		enemy.applyKnockback(knockback, direction);
		enemy.setFlashTimer(playing.getHitEffects().flashSeconds());
		playing.getHitEffects().trigger(heavy);
		return true;
	}

	private void loadEnemyImgs() {
		crabbyArr = new BufferedImage[5][9];
		BufferedImage temp = LoadSave.GetSpriteAtlas(LoadSave.CRABBY_SPRITE);
		for (int j = 0; j < crabbyArr.length; j++)
			for (int i = 0; i < crabbyArr[j].length; i++)
				crabbyArr[j][i] = temp.getSubimage(i * CRABBY_WIDTH_DEFAULT, j * CRABBY_HEIGHT_DEFAULT, CRABBY_WIDTH_DEFAULT, CRABBY_HEIGHT_DEFAULT);
		boarArr = new BufferedImage[5][10];
		BufferedImage boarSheet = LoadSave.GetSpriteAtlas(LoadSave.BOAR_SPRITE);
		for (int i = 0; i < 10; i++) {
			boarArr[IDLE][i] = boarSheet.getSubimage(i * BOAR_WIDTH_DEFAULT, 0, BOAR_WIDTH_DEFAULT, BOAR_HEIGHT_DEFAULT);
			boarArr[RUNNING][i] = boarArr[IDLE][i];
			if (i < 9)
				boarArr[ATTACK][i] = boarSheet.getSubimage(i * BOAR_WIDTH_DEFAULT, BOAR_HEIGHT_DEFAULT, BOAR_WIDTH_DEFAULT, BOAR_HEIGHT_DEFAULT);
			if (i < 9)
				boarArr[HIT][i] = boarSheet.getSubimage(i * BOAR_WIDTH_DEFAULT,
						BOAR_HEIGHT_DEFAULT * 2, BOAR_WIDTH_DEFAULT, BOAR_HEIGHT_DEFAULT);
			if (i < 9)
				boarArr[DEAD][i] = boarSheet.getSubimage(i * BOAR_WIDTH_DEFAULT,
						BOAR_HEIGHT_DEFAULT * 3, BOAR_WIDTH_DEFAULT, BOAR_HEIGHT_DEFAULT);
		}
		conquistadorArr = new BufferedImage[5][9];
		BufferedImage conquistadorSheet = LoadSave.GetSpriteAtlas(LoadSave.CONQUISTADOR_SPRITE);
		for (int i = 0; i < 8; i++) {
			conquistadorArr[RUNNING][i] = conquistadorSheet.getSubimage(i * CONQUISTADOR_WIDTH_DEFAULT, 0,
					CONQUISTADOR_WIDTH_DEFAULT, CONQUISTADOR_HEIGHT_DEFAULT);
			conquistadorArr[IDLE][i] = conquistadorSheet.getSubimage(i * CONQUISTADOR_WIDTH_DEFAULT,
					CONQUISTADOR_HEIGHT_DEFAULT * 4, CONQUISTADOR_WIDTH_DEFAULT, CONQUISTADOR_HEIGHT_DEFAULT);
		}
		for (int i = 0; i < 9; i++) {
			conquistadorArr[ATTACK][i] = conquistadorSheet.getSubimage(i * CONQUISTADOR_WIDTH_DEFAULT,
					CONQUISTADOR_HEIGHT_DEFAULT, CONQUISTADOR_WIDTH_DEFAULT, CONQUISTADOR_HEIGHT_DEFAULT);
		}
		for (int i = 0; i < 4; i++) {
			conquistadorArr[HIT][i] = conquistadorSheet.getSubimage(i * CONQUISTADOR_WIDTH_DEFAULT,
					CONQUISTADOR_HEIGHT_DEFAULT * 2, CONQUISTADOR_WIDTH_DEFAULT, CONQUISTADOR_HEIGHT_DEFAULT);
		}
		for (int i = 0; i < 8; i++) {
			conquistadorArr[DEAD][i] = conquistadorSheet.getSubimage(i * CONQUISTADOR_WIDTH_DEFAULT,
					CONQUISTADOR_HEIGHT_DEFAULT * 3, CONQUISTADOR_WIDTH_DEFAULT, CONQUISTADOR_HEIGHT_DEFAULT);
		}
		crabbyWhiteArr = whiteFrames(crabbyArr);
		boarWhiteArr = whiteFrames(boarArr);
		conquistadorWhiteArr = whiteFrames(conquistadorArr);
	}

	private BufferedImage[][] whiteFrames(BufferedImage[][] frames) {
		BufferedImage[][] white = new BufferedImage[frames.length][];
		for (int state = 0; state < frames.length; state++) {
			white[state] = new BufferedImage[frames[state].length];
			for (int frame = 0; frame < frames[state].length; frame++)
				if (frames[state][frame] != null)
					white[state][frame] = HitEffects.whiteSilhouette(frames[state][frame]);
		}
		return white;
	}

	public void resetAllEnemies() {
		for (Crabby c : crabbies)
			c.resetEnemy();
		for (Boar boar : boars)
			boar.resetEnemy();
		for (Conquistador conquistador : conquistadors)
			conquistador.resetEnemy();
		for (Magellan boss : magellans)
			boss.resetEnemy();
		for (Espinosa e : espinosas)
			e.resetEnemy();
	}

}
