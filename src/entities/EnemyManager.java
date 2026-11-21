package entities;

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import gamestates.Playing;
import levels.Level;
import utilz.LoadSave;
import static utilz.Constants.EnemyConstants.*;

public class EnemyManager {

	private Playing playing;
	private BufferedImage[][] crabbyArr;
	private BufferedImage[][] boarArr;
	private ArrayList<Crabby> crabbies = new ArrayList<>();
	private ArrayList<Boar> boars = new ArrayList<>();
	private ArrayList<Magellan> magellans = new ArrayList<>();

	public EnemyManager(Playing playing) {
		this.playing = playing;
		loadEnemyImgs();
	}

	public void loadEnemies(Level level) {
		crabbies = level.getCrabs();
		boars = level.getBoars();
		magellans = level.getMagellans();
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
		for (Magellan boss : magellans)
			if (boss.isActive()) {
				boss.update(lvlData, player);
				isAnyActive = true;
			}
		if (!isAnyActive)
			playing.setLevelCompleted(true);
	}

	public void draw(Graphics g, int xLvlOffset) {
		drawCrabs(g, xLvlOffset);
		drawBoars(g, xLvlOffset);
		for (Magellan boss : magellans)
			if (boss.isActive())
				boss.draw(g, xLvlOffset);
	}

	private void drawBoars(Graphics g, int xLvlOffset) {
		for (Boar boar : boars)
			if (boar.isActive())
				g.drawImage(boarArr[boar.getState()][boar.getAniIndex()],
						(int) boar.getHitbox().x - xLvlOffset - BOAR_DRAWOFFSET_X + boar.flipX(),
						(int) boar.getHitbox().y - BOAR_DRAWOFFSET_Y,
						BOAR_WIDTH * boar.flipW(), BOAR_HEIGHT, null);
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

				g.drawImage(crabbyArr[c.getState()][c.getAniIndex()], (int) c.getHitbox().x - xLvlOffset - CRABBY_DRAWOFFSET_X + c.flipX(), (int) c.getHitbox().y - CRABBY_DRAWOFFSET_Y,
						CRABBY_WIDTH * c.flipW(), CRABBY_HEIGHT, null);

//				c.drawHitbox(g, xLvlOffset);
//				c.drawAttackBox(g, xLvlOffset);
			}

	}

	public void checkEnemyHit(Rectangle2D.Float attackBox) {
		for (Crabby c : crabbies)
			if (c.isActive())
				if (attackBox.intersects(c.getHitbox())) {
					c.hurt(10);
					return;
				}
		for (Boar boar : boars)
			if (boar.isActive() && attackBox.intersects(boar.getHitbox())) {
				boar.hurt(10);
				return;
			}
		for (Magellan boss : magellans)
			if (boss.isActive() && attackBox.intersects(boss.getHitbox())) {
				boss.hurt(10);
				return;
			}
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
	}

	public void resetAllEnemies() {
		for (Crabby c : crabbies)
			c.resetEnemy();
		for (Boar boar : boars)
			boar.resetEnemy();
		for (Magellan boss : magellans)
			boss.resetEnemy();
	}

}
