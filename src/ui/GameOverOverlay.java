package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import gamestates.Gamestate;
import gamestates.Playing;
import main.Game;
import utilz.LoadSave;

public class GameOverOverlay {
	private static final Color PALE_BLUE = new Color(190, 211, 226);
	private final Playing playing;
	private final ImageButton restart;
	private final ImageButton menu;

	public GameOverOverlay(Playing playing) {
		this.playing = playing;
		float scale = Math.min(Game.GAME_WIDTH / 1280f, Game.GAME_HEIGHT / 640f);
		int buttonWidth = Math.round(480 * scale * 0.78f);
		int buttonHeight = Math.round(72 * scale * 0.78f);
		int centerX = Game.GAME_WIDTH / 2;
		restart = new ImageButton("restart", centerX - buttonWidth / 2,
				Math.round(Game.GAME_HEIGHT * 0.59f), buttonWidth, buttonHeight);
		menu = new ImageButton("mainmenu", centerX - buttonWidth / 2,
				Math.round(Game.GAME_HEIGHT * 0.69f), buttonWidth, buttonHeight);
	}

	public void draw(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(new Color(5, 5, 14, 188));
		g2.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

		int centerX = Game.GAME_WIDTH / 2;
		float scale = Math.min(Game.GAME_WIDTH / 1280f, Game.GAME_HEIGHT / 640f);
		int titleY = Math.round(Game.GAME_HEIGHT * 0.42f);
		g2.setFont(new Font(Font.SERIF, Font.BOLD, Math.max(32, Math.round(58 * scale))));
		g2.setColor(new Color(92, 31, 48));
		drawCentered(g2, "YOU HAVE FALLEN", centerX + 2, titleY + 3);
		g2.setColor(new Color(241, 237, 233));
		drawCentered(g2, "YOU HAVE FALLEN", centerX, titleY);

		int dividerY = Math.round(Game.GAME_HEIGHT * 0.49f);
		drawDivider(g2, centerX, dividerY, Math.round(310 * scale));
		g2.setFont(new Font(Font.SERIF, Font.PLAIN, Math.max(12, Math.round(16 * scale))));
		g2.setColor(PALE_BLUE);
		drawCentered(g2, "THE SHORES OF MACTAN WAIT FOR YOU", centerX,
				Math.round(Game.GAME_HEIGHT * 0.535f));
		restart.draw(g2);
		menu.draw(g2);
		g2.dispose();
	}

	private void drawDivider(Graphics2D g, int centerX, int y, int halfWidth) {
		g.setStroke(new BasicStroke(Math.max(1f, Game.SCALE)));
		g.setColor(new Color(153, 168, 187, 190));
		g.drawLine(centerX - halfWidth, y, centerX - 14, y);
		g.drawLine(centerX + 14, y, centerX + halfWidth, y);
		g.setColor(PALE_BLUE);
		int[] x = { centerX, centerX + 5, centerX, centerX - 5 };
		int[] yy = { y - 5, y, y + 5, y };
		g.fillPolygon(x, yy, 4);
		g.fillOval(centerX - halfWidth - 3, y - 2, 4, 4);
		g.fillOval(centerX + halfWidth - 1, y - 2, 4, 4);
	}

	private void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
		FontMetrics metrics = g.getFontMetrics();
		g.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
	}

	public void update() {
		restart.update();
		menu.update();
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			playing.resetAll();
			Gamestate.state = Gamestate.MENU;
		}
	}

	private boolean isIn(ImageButton button, MouseEvent e) {
		return button.bounds.contains(e.getX(), e.getY());
	}

	public void mouseMoved(MouseEvent e) {
		restart.mouseOver = isIn(restart, e);
		menu.mouseOver = isIn(menu, e);
	}

	public void mouseReleased(MouseEvent e) {
		if (restart.bounds.contains(e.getPoint()) && restart.mousePressed)
			playing.resetAll();
		else if (menu.bounds.contains(e.getPoint()) && menu.mousePressed) {
			playing.resetAll();
			Gamestate.state = Gamestate.MENU;
		}
		restart.reset();
		menu.reset();
	}

	public void mousePressed(MouseEvent e) {
		restart.mousePressed = isIn(restart, e);
		menu.mousePressed = isIn(menu, e);
	}

	private static class ImageButton {
		private final BufferedImage[] images = new BufferedImage[3];
		private final Rectangle bounds;
		private boolean mouseOver;
		private boolean mousePressed;

		private ImageButton(String name, int x, int y, int width, int height) {
			String[] states = { "normal", "hover", "pressed" };
			for (int i = 0; i < states.length; i++)
				images[i] = LoadSave.GetSpriteAtlas("buttons/btn_" + name + "_" + states[i] + ".png");
			bounds = new Rectangle(x, y, width, height);
		}

		private void draw(Graphics g) {
			int state = mousePressed ? 2 : mouseOver ? 1 : 0;
			g.drawImage(images[state], bounds.x, bounds.y, bounds.width, bounds.height, null);
		}

		private void update() { }

		private void reset() {
			mousePressed = false;
			mouseOver = false;
		}
	}
}
