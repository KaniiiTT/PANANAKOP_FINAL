package ui;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import gamestates.Gamestate;
import main.Game;
import utilz.LoadSave;

/** Draws the hero selection screen. Input and game state stay in CharacterSelect. */
public class CharacterSelectUI {
	private static final float DESIGN_WIDTH = 1280f;
	private static final float DESIGN_HEIGHT = 720f;

	private final Rectangle maleBox;
	private final Rectangle femaleBox;
	private final BufferedImage background;
	private final BufferedImage title;
	private final BufferedImage[] cards = new BufferedImage[2];
	private final BufferedImage[][] heroes = new BufferedImage[2][2];
	private final BufferedImage[][] labels = new BufferedImage[2][2];
	private final BufferedImage selectedLabel;
	private final float scaleX;
	private final float scaleY;
	private final MenuButton backButton;
	private final MenuButton playButton;

	public CharacterSelectUI() {
		scaleX = Game.GAME_WIDTH / DESIGN_WIDTH;
		scaleY = Game.GAME_HEIGHT / DESIGN_HEIGHT;
		background = LoadSave.GetSpriteAtlas("menu/menu_background.png");
		title = LoadSave.GetSpriteAtlas("charselect/title_choose_hero.png");
		cards[0] = LoadSave.GetSpriteAtlas("charselect/card_normal.png");
		cards[1] = LoadSave.GetSpriteAtlas("charselect/card_selected.png");
		heroes[0][0] = LoadSave.GetSpriteAtlas("charselect/hero_male_dim.png");
		heroes[0][1] = LoadSave.GetSpriteAtlas("charselect/hero_male.png");
		BufferedImage femalePortrait = LoadSave.GetSpriteAtlas(LoadSave.FEMALE_LAPULAPU_ATLAS).getSubimage(0, 0, 64, 64);
		heroes[1][0] = femalePortrait;
		heroes[1][1] = femalePortrait;
		labels[0][0] = LoadSave.GetSpriteAtlas("charselect/label_male_normal.png");
		labels[0][1] = LoadSave.GetSpriteAtlas("charselect/label_male_selected.png");
		labels[1][0] = LoadSave.GetSpriteAtlas("charselect/label_female_normal.png");
		labels[1][1] = LoadSave.GetSpriteAtlas("charselect/label_female_selected.png");
		selectedLabel = LoadSave.GetSpriteAtlas("charselect/label_selected.png");

		maleBox = rectangle(186, 136, 360, 420);
		femaleBox = rectangle(636, 136, 360, 420);
		float buttonScale = Math.min(scaleX, scaleY);
		int buttonY = sy(580);
		backButton = new MenuButton(sx(350), buttonY, "back", Gamestate.MENU, buttonScale);
		playButton = new MenuButton(sx(930), buttonY, "play", Gamestate.PLAYING, buttonScale);
	}

	private Rectangle rectangle(int x, int y, int width, int height) {
		return new Rectangle(sx(x), sy(y), sx(width), sy(height));
	}

	private int sx(int value) {
		return Math.round(value * scaleX);
	}

	private int sy(int value) {
		return Math.round(value * scaleY);
	}

	public void draw(Graphics g, int selected) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.drawImage(background, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);

		int titleWidth = sx(900);
		int titleHeight = sy(80);
		g2.drawImage(title, (Game.GAME_WIDTH - titleWidth) / 2, sy(36), titleWidth, titleHeight, null);

		drawCard(g2, maleBox, 0, selected == 0);
		drawCard(g2, femaleBox, 1, selected == 1);
		backButton.update();
		playButton.update();
		backButton.draw(g2);
		playButton.draw(g2);
		g2.dispose();
	}

	private void drawCard(Graphics2D g, Rectangle box, int heroIndex, boolean chosen) {
		g.drawImage(cards[chosen ? 1 : 0], box.x, box.y, box.width, box.height, null);

		int heroWidth = sx(heroIndex == 0 ? 240 : 180);
		int heroHeight = sy(heroIndex == 0 ? 286 : 226);
		if (heroIndex == 1)
			heroWidth = heroHeight;
		int heroX = box.x + (box.width - heroWidth) / 2;
		int heroY = box.y + sy(heroIndex == 0 ? 18 : 83);
		Object oldInterpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(heroes[heroIndex][chosen ? 1 : 0], heroX, heroY, heroWidth, heroHeight, null);
		if (oldInterpolation != null)
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);

		BufferedImage label = labels[heroIndex][chosen ? 1 : 0];
		int labelWidth = sx(360);
		int labelHeight = sy(60);
		int labelY = box.y + sy(330);
		g.drawImage(label, box.x + (box.width - labelWidth) / 2, labelY, labelWidth, labelHeight, null);
		if (chosen) {
			int selectedWidth = sx(180);
			int selectedHeight = sy(30);
			g.drawImage(selectedLabel, box.x + (box.width - selectedWidth) / 2,
					box.y + sy(379), selectedWidth, selectedHeight, null);
		}
	}

	/** Returns 0 for Play, 1 for Back, and -1 outside the action buttons. */
	public int getActionAt(int x, int y) {
		if (playButton.getBounds().contains(x, y))
			return 0;
		if (backButton.getBounds().contains(x, y))
			return 1;
		return -1;
	}

	public void setActionMouseOver(int action) {
		playButton.setMouseOver(action == 0);
		backButton.setMouseOver(action == 1);
	}

	public void setActionMousePressed(int action, boolean pressed) {
		if (action == 0)
			playButton.setMousePressed(pressed);
		else if (action == 1)
			backButton.setMousePressed(pressed);
	}

	public boolean isActionMousePressed(int action) {
		return action == 0 ? playButton.isMousePressed() : action == 1 && backButton.isMousePressed();
	}

	public void resetActionButtons() {
		playButton.resetBools();
		backButton.resetBools();
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
