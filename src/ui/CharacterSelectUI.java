package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import main.Game;
import utilz.LoadSave;

/** Draws the hero selection screen. Input and game state stay in CharacterSelect. */
public class CharacterSelectUI {
	private final Rectangle maleBox;
	private final Rectangle femaleBox;
	private final BufferedImage malePreview;
	private final BufferedImage femalePreview;
	private final float uiScale = Game.SCALE / 2f;

	public CharacterSelectUI() {
		maleBox = new Rectangle(scaled(470), scaled(250), scaled(280), scaled(430));
		femaleBox = new Rectangle(scaled(914), scaled(250), scaled(280), scaled(430));
		malePreview = LoadSave.GetSpriteAtlas(LoadSave.MALE_PLAYER_ATLAS).getSubimage(0, 0, 64, 64);
		femalePreview = LoadSave.GetSpriteAtlas(LoadSave.FEMALE_PLAYER_ATLAS).getSubimage(0, 0, 64, 64);
	}

	private int scaled(int value) {
		return Math.round(value * uiScale);
	}

	public void draw(Graphics g, int selected) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2.setColor(new Color(30, 42, 65));
		g2.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

		g2.setColor(Color.WHITE);
		g2.setFont(new Font("Arial", Font.BOLD, scaled(42)));
		String title = "CHOOSE YOUR HERO";
		g2.drawString(title, (Game.GAME_WIDTH - g2.getFontMetrics().stringWidth(title)) / 2, scaled(145));

		drawCard(g2, maleBox, malePreview, "MALE", selected == 0);
		drawCard(g2, femaleBox, femalePreview, "FEMALE", selected == 1);

		g2.setFont(new Font("Arial", Font.PLAIN, scaled(20)));
		g2.setColor(Color.WHITE);
		String instructions = "Click a hero or use Left/Right, then press Enter";
		String backInstruction = "Press Escape to return to the menu";
		g2.drawString(instructions, (Game.GAME_WIDTH - g2.getFontMetrics().stringWidth(instructions)) / 2, scaled(770));
		g2.drawString(backInstruction, (Game.GAME_WIDTH - g2.getFontMetrics().stringWidth(backInstruction)) / 2, scaled(805));
	}

	private void drawCard(Graphics2D g, Rectangle box, BufferedImage preview, String name, boolean chosen) {
		g.setColor(chosen ? new Color(255, 190, 60) : new Color(90, 105, 130));
		g.fillRoundRect(box.x, box.y, box.width, box.height, scaled(24), scaled(24));
		g.setColor(chosen ? Color.WHITE : new Color(190, 200, 215));
		g.setStroke(new BasicStroke(scaled(chosen ? 6 : 2)));
		g.drawRoundRect(box.x, box.y, box.width, box.height, scaled(24), scaled(24));

		g.drawImage(preview, box.x + scaled(46), box.y + scaled(45), scaled(190), scaled(190), null);
		g.setFont(new Font("Arial", Font.BOLD, scaled(28)));
		int nameX = box.x + (box.width - g.getFontMetrics().stringWidth(name)) / 2;
		g.drawString(name, nameX, box.y + scaled(300));
		if (chosen)
			g.drawString("SELECTED", box.x + scaled(78), box.y + scaled(350));
	}

	/** Returns 0 for male, 1 for female, and -1 outside the hero cards. */
	public int getHeroAt(int x, int y) {
		if (maleBox.contains(x, y))
			return 0;
		if (femaleBox.contains(x, y))
			return 1;
		return -1;
	}
}
