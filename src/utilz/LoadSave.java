package utilz;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

public class LoadSave {
	private static final String MODERN_ASSETS_DIRECTORY = "pananakop_modern_assets";

	public static final String MALE_PLAYER_ATLAS = "male_spritesheet.png";
	public static final String FEMALE_PLAYER_ATLAS = "female_spritesheet.png";
	public static final String PLAYER_ATLAS = MALE_PLAYER_ATLAS;
	public static final String LEVEL_ATLAS = "outside_sprites.png";
	public static final String MENU_BUTTONS = "button_atlas.png";
	public static final String MENU_BACKGROUND = "menu_background.png";
	public static final String PAUSE_BACKGROUND = "pause_menu.png";
	public static final String SOUND_BUTTONS = "sound_button.png";
	public static final String URM_BUTTONS = "urm_buttons.png";
	public static final String VOLUME_BUTTONS = "volume_buttons.png";
	public static final String MENU_BACKGROUND_IMG = "background_menu.png";
	public static final String PLAYING_BG_IMG = "playing_bg_img.png";
	public static final String BIG_CLOUDS = "big_clouds.png";
	public static final String SMALL_CLOUDS = "small_clouds.png";
	public static final String CRABBY_SPRITE = "crabby_sprite.png";
	public static final String STATUS_BAR = "health_power_bar.png";
	public static final String COMPLETED_IMG = "completed_sprite.png";
	public static final String POTION_ATLAS = "potions_sprites.png";
	public static final String CONTAINER_ATLAS = "objects_sprites.png";
	public static final String TRAP_ATLAS = "trap_atlas.png";
	public static final String CANNON_ATLAS = "cannon_atlas.png";
	public static final String CANNON_BALL = "ball.png";
	public static final String DEATH_SCREEN = "death_screen.png";
	public static final String OPTIONS_MENU = "options_background.png";

	public static BufferedImage GetSpriteAtlas(String fileName) {
		try (InputStream is = openResource(fileName)) {
			BufferedImage image = ImageIO.read(is);
			if (image == null)
				throw new IOException("Unsupported or empty image: " + fileName);
			return image;
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load game asset '" + fileName
					+ "'. Expected it on the classpath or under res/.", e);
		}
	}

	private static InputStream openResource(String fileName) throws IOException {
		InputStream classpathStream = LoadSave.class.getResourceAsStream("/" + fileName);
		if (classpathStream != null)
			return classpathStream;

		Path resourcePath = findProjectResource(fileName);
		if (!Files.exists(resourcePath))
			throw new IOException("Resource not found: " + fileName);
		return Files.newInputStream(resourcePath);
	}

	private static Path findProjectResource(String fileName) {
		Path startDirectory = Paths.get("").toAbsolutePath().normalize();
		Path directory = startDirectory;
		for (int i = 0; i < 8 && directory != null; i++) {
			Path resourcePath = directory.resolve("res").resolve(fileName);
			if (Files.exists(resourcePath))
				return resourcePath;
			directory = directory.getParent();
		}

		// Also support opening the outer repository folder in VS Code.
		try (Stream<Path> paths = Files.walk(startDirectory, 4)) {
			Path nestedResource = paths
					.filter(Files::isDirectory)
					.filter(path -> path.getFileName().toString().equals("res"))
					.map(path -> path.resolve(fileName))
					.filter(Files::exists)
					.findFirst()
					.orElse(null);
			if (nestedResource != null)
				return nestedResource;
		} catch (IOException ignored) {
			// The normal path checks above still provide the useful error below.
		}
		return Paths.get("res", fileName);
	}

	public static Path GetProjectRoot() {
		Path startDirectory = Paths.get("").toAbsolutePath().normalize();
		Path directory = startDirectory;
		for (int i = 0; i < 8 && directory != null; i++) {
			if (Files.isDirectory(directory.resolve("src"))
					&& Files.isDirectory(directory.resolve("res")))
				return directory;
			directory = directory.getParent();
		}

		try (Stream<Path> paths = Files.walk(startDirectory, 6)) {
			Path projectRoot = paths
					.filter(Files::isDirectory)
					.filter(path -> Files.isDirectory(path.resolve("src"))
							&& Files.isDirectory(path.resolve("res")))
					.findFirst()
					.orElse(null);
			if (projectRoot != null)
				return projectRoot;
		} catch (IOException e) {
			throw new IllegalStateException("Unable to find the game project under " + startDirectory, e);
		}

		throw new IllegalStateException("Unable to find the game project (expected src/ and res/).");
	}

	public static List<String> GetModernBlockMaterials() {
		Path blocksDirectory = GetProjectRoot().resolve(MODERN_ASSETS_DIRECTORY).resolve("blocks");
		if (!Files.isDirectory(blocksDirectory))
			throw new IllegalStateException("Modern block assets not found under " + blocksDirectory);

		try (Stream<Path> paths = Files.list(blocksDirectory)) {
			return paths
					.filter(Files::isDirectory)
					.map(path -> path.getFileName().toString())
					.sorted()
					.toList();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to list modern block assets under " + blocksDirectory, e);
		}
	}

	public static List<Path> GetModernBlockTiles(String material) {
		Path root = GetProjectRoot();
		Path tilesDirectory = root.resolve(MODERN_ASSETS_DIRECTORY)
				.resolve("blocks").resolve(material).resolve("tiles").normalize();
		if (!tilesDirectory.startsWith(root.resolve(MODERN_ASSETS_DIRECTORY).resolve("blocks").normalize())
				|| !Files.isDirectory(tilesDirectory))
			throw new IllegalArgumentException("Invalid modern block material: " + material);

		try (Stream<Path> paths = Files.list(tilesDirectory)) {
			return paths
					.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().matches("\\d{3}\\.png"))
					.sorted()
					.toList();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to list block tiles under " + tilesDirectory, e);
		}
	}

	public static BufferedImage GetTrapSpriteStrip(String fileName) {
		Path trapDirectory = GetProjectRoot().resolve(MODERN_ASSETS_DIRECTORY).resolve("traps").normalize();
		Path imagePath = trapDirectory.resolve(fileName).normalize();
		if (!imagePath.startsWith(trapDirectory) || !Files.isRegularFile(imagePath))
			throw new IllegalArgumentException("Invalid trap sprite strip: " + fileName);

		try {
			BufferedImage image = ImageIO.read(imagePath.toFile());
			if (image == null)
				throw new IOException("Unsupported or empty image: " + imagePath);
			return image;
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load trap sprite strip " + imagePath, e);
		}
	}

	public static BufferedImage[][] GetModernLevelTiles(int levelNumber, int width, int height) {
		BufferedImage[][] tiles = new BufferedImage[height][width];
		Path levelDirectory = findProjectLevelDirectory();
		if (levelDirectory == null)
			throw new IllegalStateException("Unable to find level resources under res/lvls.");

		Path sidecar = levelDirectory.resolve(levelNumber + ".tiles");
		if (!Files.exists(sidecar))
			return tiles;

		Path projectRoot = GetProjectRoot();
		Path modernAssetsRoot = projectRoot.resolve(MODERN_ASSETS_DIRECTORY).normalize();
		Map<String, BufferedImage> imageCache = new HashMap<>();
		try {
			List<String> lines = Files.readAllLines(sidecar);
			for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
				String line = lines.get(lineNumber).trim();
				if (line.isEmpty())
					continue;

				String[] fields = line.split("\t", 3);
				if (fields.length != 3)
					throw new IOException("Malformed tile entry on line " + (lineNumber + 1));

				int x;
				int y;
				try {
					x = Integer.parseInt(fields[0]);
					y = Integer.parseInt(fields[1]);
				} catch (NumberFormatException e) {
					throw new IOException("Invalid tile position on line " + (lineNumber + 1), e);
				}
				if (x < 0 || x >= width || y < 0 || y >= height)
					throw new IOException("Tile position is outside level bounds on line " + (lineNumber + 1));

				Path imagePath = projectRoot.resolve(fields[2]).normalize();
				if (!imagePath.startsWith(modernAssetsRoot) || !Files.isRegularFile(imagePath))
					throw new IOException("Invalid modern tile asset on line " + (lineNumber + 1) + ": " + fields[2]);

				BufferedImage tile = imageCache.get(fields[2]);
				if (tile == null) {
					tile = ImageIO.read(imagePath.toFile());
					if (tile == null)
						throw new IOException("Unsupported or empty modern tile image: " + imagePath);
					imageCache.put(fields[2], tile);
				}
				tiles[y][x] = tile;
			}
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load modern tiles from " + sidecar, e);
		}
		return tiles;
	}

	public static BufferedImage[] GetAllLevels() {
		URL url = LoadSave.class.getResource("/lvls");
		Path levelDirectory = null;

		if (url != null && url.getProtocol().equals("file")) {
			try {
				Path classpathDirectory = Paths.get(url.toURI());
				if (containsLevelFiles(classpathDirectory))
					levelDirectory = classpathDirectory;
			} catch (URISyntaxException e) {
				throw new IllegalStateException("Invalid level resource path", e);
			}
		}
		if (levelDirectory == null)
			levelDirectory = findProjectLevelDirectory();
		if (levelDirectory == null)
			throw new IllegalStateException("Unable to find level resources under res/lvls.");

		List<Path> levelFiles = new ArrayList<>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(levelDirectory, "*.png")) {
			for (Path path : files)
				if (Files.isRegularFile(path) && path.getFileName().toString().matches("\\d+\\.png"))
					levelFiles.add(path);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read level resources from " + levelDirectory, e);
		}
		levelFiles.sort(Comparator.comparingInt(path -> Integer.parseInt(
				path.getFileName().toString().replace(".png", ""))));
		if (levelFiles.isEmpty())
			throw new IllegalStateException("No numbered level images found under " + levelDirectory);

		BufferedImage[] images = new BufferedImage[levelFiles.size()];
		for (int i = 0; i < levelFiles.size(); i++) {
			String expectedName = (i + 1) + ".png";
			if (!levelFiles.get(i).getFileName().toString().equals(expectedName))
				throw new IllegalStateException("Missing level image " + expectedName + " under " + levelDirectory);
			try {
				images[i] = ImageIO.read(levelFiles.get(i).toFile());
				if (images[i] == null)
					throw new IOException("Unsupported or empty image: " + levelFiles.get(i));
			} catch (IOException e) {
				throw new IllegalStateException("Unable to load level image " + levelFiles.get(i), e);
			}
		}
		return images;
	}

	private static Path findProjectLevelDirectory() {
		Path startDirectory = Paths.get("").toAbsolutePath().normalize();
		Path directory = startDirectory;
		for (int i = 0; i < 8 && directory != null; i++) {
			Path levelDirectory = directory.resolve("res").resolve("lvls");
			if (containsLevelFiles(levelDirectory))
				return levelDirectory;
			directory = directory.getParent();
		}

		try (Stream<Path> paths = Files.walk(startDirectory, 4)) {
			return paths
					.filter(Files::isDirectory)
					.filter(path -> path.getFileName().toString().equals("lvls"))
					.filter(path -> path.getParent() != null
							&& path.getParent().getFileName().toString().equals("res"))
					.filter(LoadSave::containsLevelFiles)
					.findFirst()
					.orElse(null);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to search for level resources under " + startDirectory, e);
		}
	}

	private static boolean containsLevelFiles(Path directory) {
		if (!Files.isDirectory(directory))
			return false;
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.png")) {
			for (Path path : files)
				if (Files.isRegularFile(path) && path.getFileName().toString().matches("\\d+\\.png"))
					return true;
			return false;
		} catch (IOException e) {
			throw new IllegalStateException("Unable to inspect level resources under " + directory, e);
		}
	}

}
