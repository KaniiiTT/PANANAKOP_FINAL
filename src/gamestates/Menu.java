package gamestates;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import background.ParallaxBackground;
import main.Game;
import ui.MenuButton;
import utilz.HelpMethods;
import utilz.LoadSave;
import utilz.SoundManager;

public class Menu extends State implements Statemethods {
	private static final float DESIGN_WIDTH = 1280f;

	private final MenuButton[] buttons = new MenuButton[4];
	private final ParallaxBackground background;
	private final int[][] backgroundLevelData;
	private final BufferedImage titleLogo;
	private final int titleWidth;
	private final int titleHeight;

	public Menu(Game game) {
		super(game);
		background = new ParallaxBackground();
		backgroundLevelData = HelpMethods.GetLevelData(LoadSave.GetAllLevels()[0]);
		titleLogo = LoadSave.GetSpriteAtlas("menu/title_logo.png");

		float scale = Game.GAME_WIDTH / DESIGN_WIDTH;
		int centerX = Game.GAME_WIDTH / 2;
		String[] names = { "play", "continue", "options", "exit" };
		Gamestate[] states = { Gamestate.CHARACTER_SELECT, Gamestate.CHARACTER_SELECT,
				Gamestate.OPTIONS, Gamestate.QUIT };
		float[] centerY = { 0.52f, 0.625f, 0.73f, 0.835f };
		for (int i = 0; i < buttons.length; i++) {
			int buttonHeight = Math.round(72 * scale);
			int y = Math.round(Game.GAME_HEIGHT * centerY[i] - buttonHeight / 2f);
			buttons[i] = new MenuButton(centerX, y, names[i], states[i], scale);
		}

		titleWidth = Math.round(800 * scale);
		titleHeight = Math.round(250 * scale);
	}

	@Override
	public void update() {
		background.update(false);
		for (MenuButton button : buttons)
			button.update();
	}

	@Override
	public void draw(Graphics g) {
		background.draw(g, 0, backgroundLevelData);
		int titleX = (Game.GAME_WIDTH - titleWidth) / 2;
		int titleY = Math.round(12 * Game.GAME_WIDTH / DESIGN_WIDTH);
		g.drawImage(titleLogo, titleX, titleY, titleWidth, titleHeight, null);

		for (MenuButton button : buttons)
			button.draw(g);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		for (MenuButton button : buttons)
			if (isIn(e, button))
				button.setMousePressed(true);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		for (MenuButton button : buttons) {
			if (isIn(e, button)) {
				if (button.isMousePressed()) {
					SoundManager.play(SoundManager.Effect.MENU);
					button.applyGamestate();
				}
				break;
			}
		}
		resetButtons();
	}

	private void resetButtons() {
		for (MenuButton button : buttons)
			button.resetBools();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		for (MenuButton button : buttons)
			button.setMouseOver(isIn(e, button));
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
			Gamestate.state = Gamestate.CHARACTER_SELECT;
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
}
