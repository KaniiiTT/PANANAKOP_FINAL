package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;

import main.Game;
import utilz.LoadSave;
import utilz.SoundManager;

/** Artwork and input hit areas for the full options screen. */
public class OptionsScreenUI {
	private static final float DESIGN_WIDTH = 1280f;
	private static final float DESIGN_HEIGHT = 720f;
	private final float scaleX = Game.GAME_WIDTH / DESIGN_WIDTH;
	private final float scaleY = Game.GAME_HEIGHT / DESIGN_HEIGHT;
	private final BufferedImage background = LoadSave.GetSpriteAtlas("menu/menu_background.png");
	private final BufferedImage mainPanel = LoadSave.GetSpriteAtlas("menu/panel_main.png");
	private final BufferedImage togglesPanel = LoadSave.GetSpriteAtlas("options/panel_toggles.png");
	private final BufferedImage volumePlaque = LoadSave.GetSpriteAtlas("options/plaque_volume.png");
	private final BufferedImage title = LoadSave.GetSpriteAtlas("options/title_options.png");
	private final BufferedImage divider = LoadSave.GetSpriteAtlas("menu/divider.png");
	private final BufferedImage musicLabel = LoadSave.GetSpriteAtlas("options/label_music.png");
	private final BufferedImage sfxLabel = LoadSave.GetSpriteAtlas("options/label_sfx.png");
	private final BufferedImage volumeLabel = LoadSave.GetSpriteAtlas("options/label_volume.png");
	private final BufferedImage sliderTrack = LoadSave.GetSpriteAtlas("menu/slider_track.png");
	private final BufferedImage sliderFill = LoadSave.GetSpriteAtlas("menu/slider_fill.png");
	private final BufferedImage sliderKnob = LoadSave.GetSpriteAtlas("menu/slider_knob.png");
	private final Rectangle musicBounds = rect(724, 217, 80, 82);
	private final Rectangle sfxBounds = rect(724, 307, 80, 82);
	private final Rectangle sliderBounds = rect(480, 495, 320, 24);
	private final Rectangle backBounds = rect(600, 558, 80, 80);
	private boolean musicPressed;
	private boolean sfxPressed;
	private boolean sliderPressed;

	private Rectangle rect(int x, int y, int width, int height) {
		return new Rectangle(sx(x), sy(y), sx(width), sy(height));
	}

	private int sx(int value) {
		return Math.round(value * scaleX);
	}

	private int sy(int value) {
		return Math.round(value * scaleY);
	}

	public void draw(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.drawImage(background, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
		drawAsset(g, mainPanel, 400, 80, 480, 560);
		drawAsset(g, title, 420, 102, 440, 70);
		drawAsset(g, divider, 430, 168, 420, 28);
		drawAsset(g, togglesPanel, 440, 204, 400, 200);
		drawAsset(g, musicLabel, 484, 237, 160, 60);
		drawAsset(g, sfxLabel, 484, 327, 160, 60);
		drawSpeaker(g, musicBounds, SoundManager.isMusicMuted());
		drawSpeaker(g, sfxBounds, SoundManager.isMuted());
		drawAsset(g, volumePlaque, 525, 422, 230, 52);
		drawAsset(g, volumeLabel, 520, 428, 240, 40);
		drawAsset(g, sliderTrack, 480, 495, 320, 20);

		int fillX = sx(492);
		int fillY = sy(502);
		int fillWidth = Math.round(sx(296) * SoundManager.getVolume());
		g.drawImage(sliderFill, fillX, fillY, fillWidth, sy(6), null);
		int knobX = fillX + fillWidth - sx(16);
		g.drawImage(sliderKnob, knobX, sy(499), sx(32), sy(32), null);
		drawHome(g, backBounds);
		g.dispose();
	}

	private void drawAsset(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
		g.drawImage(image, sx(x), sy(y), sx(width), sy(height), null);
	}

	private void drawSpeaker(Graphics2D graphics, Rectangle bounds, boolean muted) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.translate(bounds.x, bounds.y);
		g.scale(bounds.width / 80.0, bounds.height / 82.0);
		g.setColor(new Color(119, 131, 146));
		g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Polygon hex = new Polygon(new int[] { 40, 68, 68, 40, 12, 12 },
				new int[] { 2, 18, 64, 80, 64, 18 }, 6);
		g.drawPolygon(hex);
		Polygon speaker = new Polygon(new int[] { 20, 30, 48, 48, 30, 20 },
				new int[] { 34, 34, 24, 58, 48, 48 }, 6);
		g.drawPolygon(speaker);
		if (muted) {
			g.drawLine(54, 32, 65, 48);
			g.drawLine(65, 32, 54, 48);
		} else {
			g.draw(new Arc2D.Double(43, 28, 18, 26, -60, 120, Arc2D.OPEN));
			g.draw(new Arc2D.Double(43, 20, 30, 42, -60, 120, Arc2D.OPEN));
		}
		g.dispose();
	}

	private void drawHome(Graphics2D graphics, Rectangle bounds) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.translate(bounds.x, bounds.y);
		g.scale(bounds.width / 80.0, bounds.height / 80.0);
		g.setColor(new Color(119, 131, 146));
		g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawOval(3, 3, 74, 74);
		g.drawLine(20, 39, 40, 20);
		g.drawLine(40, 20, 60, 39);
		g.drawLine(25, 36, 25, 59);
		g.drawLine(55, 36, 55, 59);
		g.drawLine(25, 59, 55, 59);
		g.drawLine(36, 59, 36, 44);
		g.drawLine(36, 44, 44, 44);
		g.drawLine(44, 44, 44, 59);
		g.dispose();
	}

	public boolean isBackAt(MouseEvent e) {
		return backBounds.contains(e.getPoint());
	}

	public void mousePressed(MouseEvent e) {
		musicPressed = musicBounds.contains(e.getPoint());
		sfxPressed = sfxBounds.contains(e.getPoint());
		sliderPressed = sliderBounds.contains(e.getPoint());
		if (sliderPressed)
			setVolume(e.getX());
	}

	public void mouseDragged(MouseEvent e) {
		if (sliderPressed)
			setVolume(e.getX());
	}

	public void mouseReleased(MouseEvent e) {
		if (musicPressed && musicBounds.contains(e.getPoint()))
			SoundManager.setMusicMuted(!SoundManager.isMusicMuted());
		if (sfxPressed && sfxBounds.contains(e.getPoint()))
			SoundManager.setMuted(!SoundManager.isMuted());
		musicPressed = false;
		sfxPressed = false;
		sliderPressed = false;
	}

	private void setVolume(int mouseX) {
		int start = sx(492);
		int end = sx(492 + 296);
		float volume = (mouseX - start) / (float) (end - start);
		SoundManager.setVolume(Math.max(0f, Math.min(1f, volume)));
	}
}
