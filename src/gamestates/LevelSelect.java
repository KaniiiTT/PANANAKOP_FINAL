package gamestates;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import main.Game;
import ui.LevelSelectUI;

/** Lets the player choose a stage that has been unlocked. */
public class LevelSelect extends State implements Statemethods {
	private final LevelSelectUI ui = new LevelSelectUI();
	private int selectedLevel;
	private int pressedLevel = -1;
	private boolean backPressed;

	public LevelSelect(Game game) {
		super(game);
		setSelectedLevel(game.getUnlockedLevelCount() - 1);
	}

	public void setSelectedLevel(int level) {
		selectedLevel = Math.max(0, Math.min(game.getUnlockedLevelCount() - 1, level));
	}

	@Override
	public void update() {
	}

	@Override
	public void draw(Graphics g) {
		ui.draw(g, selectedLevel, game.getUnlockedLevelCount());
	}

	@Override
	public void mousePressed(MouseEvent e) {
		backPressed = ui.isBackAt(e.getX(), e.getY());
		pressedLevel = backPressed ? -1 : ui.getLevelAt(e.getX(), e.getY());
		if (pressedLevel >= 0 && pressedLevel < game.getUnlockedLevelCount())
			selectedLevel = pressedLevel;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (backPressed && ui.isBackAt(e.getX(), e.getY())) {
			Gamestate.state = Gamestate.MENU;
		} else {
			int level = ui.getLevelAt(e.getX(), e.getY());
			if (level == pressedLevel && level >= 0 && level < game.getUnlockedLevelCount())
				game.startUnlockedLevel(level + 1);
		}
		pressedLevel = -1;
		backPressed = false;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		int level = ui.getLevelAt(e.getX(), e.getY());
		ui.setHoveredLevel(level);
		if (level >= 0 && level < game.getUnlockedLevelCount())
			selectedLevel = level;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			Gamestate.state = Gamestate.MENU;
		} else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_LEFT) {
			selectedLevel = Math.max(0, selectedLevel - 1);
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_RIGHT) {
			selectedLevel = Math.min(game.getUnlockedLevelCount() - 1, selectedLevel + 1);
		} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			game.startUnlockedLevel(selectedLevel + 1);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}
}
