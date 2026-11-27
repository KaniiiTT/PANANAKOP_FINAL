package ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import main.Game;
import utilz.LoadSave;

/** Draws the unlocked-stage list and matching stage artwork. */
public class LevelSelectUI {
	private static final int LEVEL_COUNT = 5;
	private static final float DESIGN_WIDTH = 1280f;
	private static final float DESIGN_HEIGHT = 720f;
	private static final String[] ROMAN = { "I", "II", "III", "IV", "V" };
	private static final String[] NAMES = {
			"BARANGAY MACTAN", "FOREST OF MACTAN", "MACTAN ISLAND", "PUNTA ENGAÑO", "MACTAN LIBERTY SHRINE"
	};
	private static final String[] SUBTITLES = {
			"TUTORIAL", "HOSTILE WILDLIFE  ·  MINI-BOSS", "SPANISH FORCES",
			"MAGELLAN'S RIGHT HAND  ·  MINI-BOSS", "FINAL BOSS  ·  MAGELLAN"
	};

	private final float scaleX = Game.GAME_WIDTH / DESIGN_WIDTH;
	private final float scaleY = Game.GAME_HEIGHT / DESIGN_HEIGHT;
	private final BufferedImage background = LoadSave.GetSpriteAtlas("menu/menu_background.png");
	private final BufferedImage rowPanel = LoadSave.GetSpriteAtlas("menu/dialog_box.png");
	private final BufferedImage backButton = LoadSave.GetSpriteAtlas("menu/buttons/btn_back_normal.png");
	private final BufferedImage[] areaTitles = new BufferedImage[LEVEL_COUNT];
	private final Rectangle[] rows = new Rectangle[LEVEL_COUNT];
	private final Rectangle backBounds;
	private int hoveredLevel = -1;

	public LevelSelectUI() {
		for (int i = 0; i < LEVEL_COUNT; i++) {
			areaTitles[i] = LoadSave.GetSpriteAtlas("menu/area_title_" + (i + 1) + ".png");
			rows[i] = rect(70, 12 + i * 130, 458, 116);
		}
		backBounds = rect(300, 648, 300, 72);
	}

	private Rectangle rect(int x, int y, int width, int height) {
		return new Rectangle(sx(x), sy(y), sx(width), sy(height));
	}

	private int sx(int value) {
		return Math.round(value * scaleX);
	}

	private int sy(int value) {
		return Math.round(value * scaleY);
	}

	private float sx(float value) {
		return value * scaleX;
	}

	public void draw(Graphics graphics, int selectedLevel, int unlockedCount) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.drawImage(background, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
		for (int i = 0; i < LEVEL_COUNT; i++)
			drawRow(g, i, selectedLevel, i < unlockedCount, unlockedCount);

		if (selectedLevel >= 0 && selectedLevel < LEVEL_COUNT) {
			drawAreaTitle(g, selectedLevel, 150);
			if (selectedLevel != LEVEL_COUNT - 1)
				drawAreaTitle(g, LEVEL_COUNT - 1, 405);
		}
		drawBackButton(g);
		g.dispose();
	}

	private void drawRow(Graphics2D g, int index, int selectedLevel, boolean unlocked, int unlockedCount) {
		Rectangle row = rows[index];
		Graphics2D card = (Graphics2D) g.create();
		if (!unlocked)
			card.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.52f));
		card.drawImage(rowPanel, row.x, row.y, row.width, row.height, null);
		card.setColor(new Color(34, 42, 60));
		card.setFont(new Font(Font.SERIF, Font.PLAIN, Math.max(18, sx(36))));
		int numeralWidth = card.getFontMetrics().stringWidth(ROMAN[index]);
		card.drawString(ROMAN[index], row.x + sx(50) - numeralWidth / 2, row.y + sy(74));
		card.setStroke(new BasicStroke(Math.max(1f, sx(1.2f))));
		int dividerX = row.x + sx(108);
		card.drawLine(dividerX, row.y + sy(18), dividerX, row.y + row.height - sy(18));
		card.setFont(new Font(Font.SERIF, Font.PLAIN, Math.max(11, sx(21))));
		card.drawString(NAMES[index], row.x + sx(150), row.y + sy(49));
		card.setColor(new Color(103, 112, 127));
		card.setFont(new Font(Font.SERIF, Font.PLAIN, Math.max(8, sx(11))));
		card.drawString(SUBTITLES[index], row.x + sx(150), row.y + sy(82));
		if (unlocked)
			drawDiamond(card, row.x + row.width - sx(28), row.y + sy(20), index < unlockedCount - 1);
		else
			drawLock(card, row.x + row.width - sx(29), row.y + sy(16));
		card.dispose();

		if (index == selectedLevel && unlocked) {
			g.setColor(new Color(229, 174, 73));
			g.setStroke(new BasicStroke(Math.max(1.2f, sx(2f))));
			g.drawRect(row.x + sx(5), row.y + sy(5), row.width - sx(10), row.height - sy(10));
		}
		if (index == hoveredLevel && unlocked && index != selectedLevel) {
			g.setColor(new Color(229, 174, 73, 150));
			g.setStroke(new BasicStroke(Math.max(1f, sx(1f))));
			g.drawRect(row.x + sx(5), row.y + sy(5), row.width - sx(10), row.height - sy(10));
		}
	}

	private void drawAreaTitle(Graphics2D g, int index, int designY) {
		BufferedImage image = areaTitles[index];
		int width = sx(610);
		int height = sy(92);
		int x = sx(640) + (Game.GAME_WIDTH - sx(640) - width) / 2;
		g.drawImage(image, x, sy(designY), width, height, null);
	}

	private void drawDiamond(Graphics2D g, int centerX, int centerY, boolean completed) {
		int r = Math.max(4, sx(10));
		g.setColor(completed ? new Color(68, 126, 188) : new Color(226, 177, 72));
		g.setStroke(new BasicStroke(Math.max(1f, sx(2f))));
		g.drawPolygon(new int[] { centerX, centerX + r, centerX, centerX - r },
				new int[] { centerY - r, centerY, centerY + r, centerY }, 4);
	}

	private void drawLock(Graphics2D g, int x, int y) {
		g.setColor(new Color(119, 131, 146));
		g.setStroke(new BasicStroke(Math.max(1.2f, sx(2f))));
		int w = sx(17);
		int h = sy(13);
		g.drawRoundRect(x, y + h / 2, w, h, sx(2), sy(2));
		g.drawArc(x + sx(3), y, w - sx(6), h, 0, 180);
	}

	private void drawBackButton(Graphics2D g) {
		int centerX = backBounds.x + backBounds.width / 2;
		int y = backBounds.y;
		int width = sx(480);
		int height = sy(72);
		g.drawImage(backButton, centerX - width / 2, y, width, height, null);
	}

	public int getLevelAt(int x, int y) {
		for (int i = 0; i < rows.length; i++)
			if (rows[i].contains(x, y))
				return i;
		return -1;
	}

	public boolean isBackAt(int x, int y) {
		return backBounds.contains(x, y);
	}

	public void setHoveredLevel(int level) {
		hoveredLevel = level;
	}
}
