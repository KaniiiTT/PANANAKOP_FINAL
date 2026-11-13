package gamestates;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import main.Game;
import ui.CharacterSelectUI;
import utilz.LoadSave;

/** A small screen where the player chooses a hero before the level starts. */
public class CharacterSelect extends State implements Statemethods {

	private final CharacterSelectUI ui;
	private int selected = 0;

	public CharacterSelect(Game game) {
		super(game);
		ui = new CharacterSelectUI();
	}

	@Override
	public void update() {
		// This screen does not need an update loop.
	}

	@Override
	public void draw(Graphics g) {
		ui.draw(g, selected);
	}

	private void choose() {
		String sheet = selected == 0 ? LoadSave.MALE_PLAYER_ATLAS : LoadSave.FEMALE_PLAYER_ATLAS;
		game.chooseCharacter(sheet);
	}

	private int heroAt(MouseEvent e) {
		return ui.getHeroAt(e.getX(), e.getY());
	}

	private void selectFromMouse(MouseEvent e) {
		int hero = heroAt(e);
		if (hero >= 0)
			selected = hero;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		selectFromMouse(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (heroAt(e) >= 0)
			choose();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		selectFromMouse(e);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
			selected = 0;
		else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
			selected = 1;
		else if (e.getKeyCode() == KeyEvent.VK_ENTER)
			choose();
		else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			Gamestate.state = Gamestate.MENU;
	}

	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void keyReleased(KeyEvent e) { }
}
