package levels;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

public class LevelDecoration {
	private final int x;
	private final int y;
	private final int width;
	private final int height;
	private final int rotationDegrees;
	private final Path assetPath;
	private final BufferedImage image;

	public LevelDecoration(int x, int y, int width, int height, Path assetPath, BufferedImage image) {
		this(x, y, width, height, 0, assetPath, image);
	}

	public LevelDecoration(int x, int y, int width, int height, int rotationDegrees, Path assetPath,
			BufferedImage image) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.rotationDegrees = Math.floorMod(rotationDegrees, 360);
		this.assetPath = assetPath;
		this.image = image;
	}

	public int getX() { return x; }
	public int getY() { return y; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public int getRotationDegrees() { return rotationDegrees; }
	public Path getAssetPath() { return assetPath; }
	public BufferedImage getImage() { return image; }
}
