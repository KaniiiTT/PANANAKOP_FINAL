package gamestates;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import main.Game;
import ui.AudioOptions;
import ui.OptionsScreenUI;

public class GameOptions extends State implements Statemethods {
	private final AudioOptions audioOptions;
	private final OptionsScreenUI optionsUI;
	private boolean backPressed;

	public GameOptions(Game game) {
		super(game);
		audioOptions = game.getAudioOptions();
		optionsUI = new OptionsScreenUI();
	}

	@Override
	public void update() {
		audioOptions.update();
	}

	@Override
	public void draw(Graphics g) {
		optionsUI.draw(g);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		backPressed = optionsUI.isBackAt(e);
		if (!backPressed)
			optionsUI.mousePressed(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (backPressed && optionsUI.isBackAt(e))
			Gamestate.state = Gamestate.MENU;
		else
			optionsUI.mouseReleased(e);
		backPressed = false;
	}

	public void mouseDragged(MouseEvent e) {
		optionsUI.mouseDragged(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			Gamestate.state = Gamestate.MENU;
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}
}
