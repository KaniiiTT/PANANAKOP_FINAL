package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;

import gamestates.Gamestate;
import gamestates.Playing;
import main.Game;
import utilz.LoadSave;
import utilz.SoundManager;

/** Pause menu artwork and input, using the light menu/options asset set. */
public class PauseOverlay {
	private static final float DESIGN_WIDTH = 1280f;
	private static final float DESIGN_HEIGHT = 720f;
	private final Playing playing;
	private final float scaleX = Game.GAME_WIDTH / DESIGN_WIDTH;
	private final float scaleY = Game.GAME_HEIGHT / DESIGN_HEIGHT;
	private final float buttonScale = Math.min(scaleX, scaleY) * (360f / 480f);
	private final BufferedImage panel = LoadSave.GetSpriteAtlas("menu/panel_main.png");
	private final BufferedImage divider = LoadSave.GetSpriteAtlas("menu/divider.png");
	private final BufferedImage togglesPanel = LoadSave.GetSpriteAtlas("options/panel_toggles.png");
	private final BufferedImage musicLabel = LoadSave.GetSpriteAtlas("options/label_music.png");
	private final BufferedImage sfxLabel = LoadSave.GetSpriteAtlas("options/label_sfx.png");
	private final BufferedImage volumePlaque = LoadSave.GetSpriteAtlas("options/plaque_volume.png");
	private final BufferedImage volumeLabel = LoadSave.GetSpriteAtlas("options/label_volume.png");
	private final BufferedImage sliderTrack = LoadSave.GetSpriteAtlas("menu/slider_track.png");
	private final BufferedImage sliderFill = LoadSave.GetSpriteAtlas("menu/slider_fill.png");
	private final BufferedImage sliderKnob = LoadSave.GetSpriteAtlas("menu/slider_knob.png");
	private final Rectangle musicBounds = rect(724, 165, 80, 82);
	private final Rectangle sfxBounds = rect(724, 239, 80, 82);
	private final Rectangle sliderBounds = rect(480, 419, 320, 24);
	private final MenuButton menuButton;
	private final MenuButton restartButton;
	private final MenuButton resumeButton;
	private final AudioOptions audioOptions;
	private boolean musicPressed;
	private boolean sfxPressed;
	private boolean sliderPressed;

	public PauseOverlay(Playing playing) {
		this.playing = playing;
		this.audioOptions = playing.getGame().getAudioOptions();
		int centerX = Game.GAME_WIDTH / 2;
		menuButton = new MenuButton(centerX, sy(470), "mainmenu", Gamestate.MENU, buttonScale);
		restartButton = new MenuButton(centerX, sy(528), "restart", Gamestate.PLAYING, buttonScale);
		resumeButton = new MenuButton(centerX, sy(586), "resume", Gamestate.PLAYING, buttonScale);
	}

	private Rectangle rect(int x, int y, int width, int height) {
		return new Rectangle(sx(x), sy(y), sx(width), sy(height));
	}

	private int sx(int value) { return Math.round(value * scaleX); }
	private int sy(int value) { return Math.round(value * scaleY); }

	public void update() {
		menuButton.update();
		restartButton.update();
		resumeButton.update();
		// Keep legacy audio controls synchronized for the standalone options screen.
		audioOptions.update();
	}

	public void draw(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.setColor(new Color(13, 20, 34, 155));
		g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
		drawAsset(g, panel, 360, 35, 560, 650);

		g.setColor(new Color(35, 42, 58));
		g.setFont(new Font(Font.SERIF, Font.PLAIN, Math.max(16, sy(50))));
		String title = "PAUSED";
		int titleWidth = g.getFontMetrics().stringWidth(title);
		g.drawString(title, Game.GAME_WIDTH / 2 - titleWidth / 2, sy(106));
		drawAsset(g, divider, 430, 116, 420, 28);
		drawAsset(g, togglesPanel, 440, 152, 400, 180);
		drawAsset(g, musicLabel, 484, 164, 160, 60);
		drawAsset(g, sfxLabel, 484, 238, 160, 60);
		drawSpeaker(g, musicBounds, SoundManager.isMusicMuted());
		drawSpeaker(g, sfxBounds, SoundManager.isMuted());
		drawAsset(g, volumePlaque, 525, 342, 230, 52);
		drawAsset(g, volumeLabel, 520, 348, 240, 40);
		drawAsset(g, sliderTrack, 480, 419, 320, 20);
		int fillX = sx(492);
		int fillWidth = Math.round(sx(296) * SoundManager.getVolume());
		g.drawImage(sliderFill, fillX, sy(426), fillWidth, sy(6), null);
		g.drawImage(sliderKnob, fillX + fillWidth - sx(16), sy(423), sx(32), sy(32), null);

		menuButton.draw(g);
		restartButton.draw(g);
		resumeButton.draw(g);
		g.dispose();
	}

	private void drawAsset(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
		g.drawImage(image, sx(x), sy(y), sx(width), sy(height), null);
	}

	private void drawSpeaker(Graphics graphics, Rectangle bounds, boolean muted) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.translate(bounds.x, bounds.y);
		g.scale(bounds.width / 80.0, bounds.height / 82.0);
		g.setColor(new Color(119, 131, 146));
		g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawPolygon(new Polygon(new int[] { 40, 68, 68, 40, 12, 12 },
				new int[] { 2, 18, 64, 80, 64, 18 }, 6));
		g.drawPolygon(new Polygon(new int[] { 20, 30, 48, 48, 30, 20 },
				new int[] { 34, 34, 24, 58, 48, 48 }, 6));
		if (muted) {
			g.drawLine(54, 32, 65, 48);
			g.drawLine(65, 32, 54, 48);
		} else {
			g.draw(new Arc2D.Double(43, 28, 18, 26, -60, 120, Arc2D.OPEN));
			g.draw(new Arc2D.Double(43, 20, 30, 42, -60, 120, Arc2D.OPEN));
		}
		g.dispose();
	}

	public void mouseDragged(MouseEvent e) {
		if (sliderPressed)
			setVolume(e.getX());
	}

	public void mousePressed(MouseEvent e) {
		if (isIn(e, menuButton)) menuButton.setMousePressed(true);
		else if (isIn(e, restartButton)) restartButton.setMousePressed(true);
		else if (isIn(e, resumeButton)) resumeButton.setMousePressed(true);
		else {
			musicPressed = musicBounds.contains(e.getPoint());
			sfxPressed = sfxBounds.contains(e.getPoint());
			sliderPressed = sliderBounds.contains(e.getPoint());
			if (sliderPressed) setVolume(e.getX());
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (isIn(e, menuButton) && menuButton.isMousePressed()) {
			Gamestate.state = Gamestate.MENU;
			playing.unpauseGame();
		} else if (isIn(e, restartButton) && restartButton.isMousePressed()) {
			playing.resetAll();
			playing.unpauseGame();
		} else if (isIn(e, resumeButton) && resumeButton.isMousePressed()) {
			playing.unpauseGame();
		} else {
			if (musicPressed && musicBounds.contains(e.getPoint()))
				SoundManager.setMusicMuted(!SoundManager.isMusicMuted());
			if (sfxPressed && sfxBounds.contains(e.getPoint()))
				SoundManager.setMuted(!SoundManager.isMuted());
		}
		musicPressed = sfxPressed = sliderPressed = false;
		menuButton.resetBools();
		restartButton.resetBools();
		resumeButton.resetBools();
	}

	public void mouseMoved(MouseEvent e) {
		menuButton.setMouseOver(isIn(e, menuButton));
		restartButton.setMouseOver(isIn(e, restartButton));
		resumeButton.setMouseOver(isIn(e, resumeButton));
	}

	private void setVolume(int mouseX) {
		int start = sx(492);
		int end = sx(492 + 296);
		float volume = (mouseX - start) / (float) (end - start);
		SoundManager.setVolume(Math.max(0f, Math.min(1f, volume)));
	}

	private boolean isIn(MouseEvent e, MenuButton button) {
		return button.getBounds().contains(e.getPoint());
	}
}
