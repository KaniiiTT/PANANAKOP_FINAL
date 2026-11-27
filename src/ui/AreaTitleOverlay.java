package ui;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import main.Game;
import utilz.LoadSave;

/** Briefly announces the current area when a level begins. */
public class AreaTitleOverlay {
	private static final int LEVEL_COUNT = 5;
	private static final float DISPLAY_SECONDS = 3.5f;
	private static final float ENTER_SECONDS = 0.28f;
	private static final float FADE_SECONDS = 0.45f;
	private static final float WIDTH_RATIO = 0.78f;
	private final BufferedImage[] titles = new BufferedImage[LEVEL_COUNT];
	private BufferedImage currentTitle;
	private float remaining;

	public AreaTitleOverlay() {
		for (int i = 0; i < titles.length; i++)
			titles[i] = LoadSave.GetSpriteAtlas("menu/area_title_" + (i + 1) + ".png");
	}

	public void showLevel(int levelNumber) {
		if (levelNumber < 1 || levelNumber > LEVEL_COUNT) {
			currentTitle = null;
			remaining = 0;
			return;
		}
		currentTitle = titles[levelNumber - 1];
		remaining = DISPLAY_SECONDS;
	}

	public void update(float dt) {
		if (remaining > 0)
			remaining = Math.max(0, remaining - dt);
	}

	public void draw(Graphics graphics) {
		if (currentTitle == null || remaining <= 0)
			return;

		int width = Math.round(Game.GAME_WIDTH * WIDTH_RATIO);
		int height = Math.round(width * currentTitle.getHeight() / (float) currentTitle.getWidth());
		int x = (Game.GAME_WIDTH - width) / 2;
		int targetY = Math.round(Game.GAME_HEIGHT * 0.055f);
		float elapsed = DISPLAY_SECONDS - remaining;
		float enterProgress = Math.min(1f, elapsed / ENTER_SECONDS);
		float easedEnter = 1f - (1f - enterProgress) * (1f - enterProgress);
		int y = Math.round(-height + (targetY + height) * easedEnter);
		float alpha = Math.min(1f, elapsed / 0.12f);
		if (remaining < FADE_SECONDS)
			alpha = Math.min(alpha, remaining / FADE_SECONDS);

		Graphics2D g = (Graphics2D) graphics.create();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
		g.drawImage(currentTitle, x, y, width, height, null);
		g.dispose();
	}
}
