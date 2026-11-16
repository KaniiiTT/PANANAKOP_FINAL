package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import entities.Magellan;
import entities.Player;
import main.Game;
import utilz.LoadSave;

/** Draws the active boss health bar and Magellan's introduction dialogue. */
public class BossUI {
	private static final int INTRO_LINE_TICKS = 240;
	private static final int INTRO_RANGE_TILES = 8;
	private static final String[] INTRO_LINES = {
			"So... you finally arrived.",
			"You dare challenge Magellan?",
			"Kukuku... your journey ends here!"
	};

	private final BufferedImage portrait;
	private int dialogueTicks;
	private int dialogueLine;
	private boolean introductionShown;

	public BossUI() {
		portrait = LoadSave.GetSpriteAtlas("pananakop_boss_magellan/portrait/magellan_portrait_64.png");
	}

	public void update(Player player, Magellan boss) {
		if (boss == null || !boss.isActive()) {
			dialogueTicks = 0;
			return;
		}

		if (!introductionShown && isPlayerNearBoss(player, boss)) {
			introductionShown = true;
			dialogueLine = 0;
			dialogueTicks = INTRO_LINE_TICKS * INTRO_LINES.length;
		}

		if (dialogueTicks > 0) {
			dialogueTicks--;
			dialogueLine = Math.min(INTRO_LINES.length - 1,
					(INTRO_LINE_TICKS * INTRO_LINES.length - dialogueTicks) / INTRO_LINE_TICKS);
		}
	}

	public void draw(Graphics graphics, Magellan boss) {
		if (boss == null || !boss.isActive())
			return;

		Graphics g = graphics.create();
		int scale = Math.max(1, Math.round(Game.SCALE));
		int barWidth = 420 * scale;
		int barHeight = 24 * scale;
		int x = (Game.GAME_WIDTH - barWidth) / 2;
		int y = 18 * scale;

		g.setColor(new Color(20, 15, 25, 220));
		g.fillRoundRect(x - 8 * scale, y - 8 * scale, barWidth + 16 * scale, 62 * scale, 8 * scale, 8 * scale);
		g.drawImage(portrait, x - 70 * scale, y - 4 * scale, 56 * scale, 56 * scale, null);

		g.setColor(Color.WHITE);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16 * scale));
		g.drawString("FERDINAND MAGELLAN", x, y + 2 * scale);

		g.setColor(new Color(75, 25, 35));
		g.fillRect(x, y + 10 * scale, barWidth, barHeight);
		int healthWidth = (int) (barWidth * boss.getHealth() / (float) boss.getMaxHealth());
		g.setColor(new Color(205, 35, 45));
		g.fillRect(x, y + 10 * scale, Math.max(0, healthWidth), barHeight);
		g.setColor(Color.WHITE);
		g.drawRect(x, y + 10 * scale, barWidth, barHeight);

		if (dialogueTicks > 0)
			drawDialogue(g, scale);
		g.dispose();
	}

	private void drawDialogue(Graphics g, int scale) {
		int width = Math.min(Game.GAME_WIDTH - 40 * scale, 620 * scale);
		int x = (Game.GAME_WIDTH - width) / 2;
		int y = Game.GAME_HEIGHT - 112 * scale;
		g.setColor(new Color(15, 18, 28, 235));
		g.fillRoundRect(x, y, width, 78 * scale, 10 * scale, 10 * scale);
		g.setColor(new Color(205, 150, 45));
		g.drawRoundRect(x, y, width, 78 * scale, 10 * scale, 10 * scale);
		g.setColor(Color.WHITE);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15 * scale));
		g.drawString("MAGELLAN", x + 16 * scale, y + 24 * scale);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14 * scale));
		g.drawString(INTRO_LINES[dialogueLine], x + 16 * scale, y + 52 * scale);
	}

	private boolean isPlayerNearBoss(Player player, Magellan boss) {
		float playerCenter = player.getHitbox().x + player.getHitbox().width / 2;
		float bossCenter = boss.getHitbox().x + boss.getHitbox().width / 2;
		return Math.abs(playerCenter - bossCenter) <= Game.TILES_SIZE * INTRO_RANGE_TILES;
	}

	public void reset() {
		dialogueTicks = 0;
		dialogueLine = 0;
		introductionShown = false;
	}
}
