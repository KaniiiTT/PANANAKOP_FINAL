package objects;

import entities.Player;
import java.awt.Graphics;
import main.Game;
import static utilz.Constants.ObjectConstants.TRAP_CRUMBLING_PLATFORM;

public class CrumblingPlatformTrap extends Trap {
	private static final long DISAPPEAR_DELAY = 1000;
	private static final long RESPAWN_DELAY = 4000;
	private static final int COLLISION_REMOVAL_FRAME = 5;
	private long touchTime = -1;
	private long disappearTime = -1;

	public CrumblingPlatformTrap(int x, int y) {
		super(x, y, TRAP_CRUMBLING_PLATFORM);
	}
	@Override protected boolean isOneShot() { return true; }

	public float getPlatformLeft() {
		return x + (Game.TILES_SIZE - 40 * Game.SCALE) / 2f;
	}

	public float getPlatformRight() {
		return getPlatformLeft() + 40 * Game.SCALE;
	}

	public float getPlatformTop() {
		return y + Game.TILES_SIZE - 28 * Game.SCALE;
	}

	public boolean isPlatformSolid() {
		if (!active)
			return false;
		if (touchTime < 0)
			return true;
		long elapsed = System.currentTimeMillis() - touchTime;
		int frameCount = getAnimationFrameCount();
		return frameCount == 0 || elapsed < DISAPPEAR_DELAY * COLLISION_REMOVAL_FRAME / frameCount;
	}

	@Override
	public void update(Player player) {
		long now = System.currentTimeMillis();
		if (!active) {
			if (disappearTime >= 0 && now - disappearTime >= RESPAWN_DELAY) {
				active = true;
				touchTime = -1;
				disappearTime = -1;
				aniIndex = 0;
			}
			return;
		}

		if (touchTime < 0 && isPlayerTouchingPlatform(player))
			touchTime = now;
		if (touchTime >= 0) {
			long elapsed = now - touchTime;
			int frameCount = getAnimationFrameCount();
			if (frameCount > 0)
				aniIndex = Math.min(frameCount - 1, (int) (elapsed * frameCount / DISAPPEAR_DELAY));
		}
		if (touchTime >= 0 && now - touchTime >= DISAPPEAR_DELAY) {
			active = false;
			disappearTime = now;
			aniIndex = 0;
		}
	}

	private boolean isPlayerTouchingPlatform(Player player) {
		java.awt.geom.Rectangle2D.Float playerBox = player.getHitbox();
		boolean horizontalOverlap = playerBox.x < getPlatformRight()
				&& playerBox.x + playerBox.width > getPlatformLeft();
		float feet = playerBox.y + playerBox.height;
		return horizontalOverlap && Math.abs(feet - getPlatformTop()) <= 3 * Game.SCALE;
	}

	@Override
	public void draw(Graphics g, int xLvlOffset) {
		if (active)
			super.draw(g, xLvlOffset);
	}

	@Override
	public void reset() {
		super.reset();
		touchTime = -1;
		disappearTime = -1;
	}
}
