package GameEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import main.Game;
import levels.Level;
import objects.Trap;
import utilz.LoadSave;

public class GameEditor {
	private static final int TILE_SIZE = 32;
	private static final int EMPTY_TILE = 11;
	private static final int NO_MARKER = 255;
	private static final StageOption[] STAGES = {
			new StageOption(1, "Stage 1: Tutorial"),
			new StageOption(2, "Stage 2"),
			new StageOption(3, "Stage 3"),
			new StageOption(4, "Stage 4"),
			new StageOption(5, "Stage 5: Boss Fight (placeholder)")
	};

	private final Path projectRoot = LoadSave.GetProjectRoot();
	private final Path levelsDirectory = projectRoot.resolve("res").resolve("lvls");
	private final BufferedImage[] classicTiles = loadClassicTiles();
	private final JFrame frame = new JFrame("Pananakop Map Editor");
	private final Map<String, BufferedImage> previewCache = new TreeMap<>();
	private final EditorCanvas canvas = new EditorCanvas();
	private final JLabel status = new JLabel("Select a block, then click or drag on the map.");
	private final JLabel currentMapLabel = new JLabel();
	private BufferedImage mapImage;
	private String[][] modernTilePaths;
	private Path currentMapPath;
	private Tool selectedTool = Tool.erase();
	private JComboBox<String> materialPicker;
	private JComboBox<StageOption> stagePicker;
	private JList<TileOption> tileList;
	private boolean unsavedChanges;
	private boolean testGameRunning;
	private boolean updatingStagePicker;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				new GameEditor().show();
			} catch (RuntimeException e) {
				JOptionPane.showMessageDialog(null, e.getMessage(), "Unable to open map editor",
						JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	private GameEditor() {
		if (!loadLevel(levelsDirectory.resolve("1.png")))
			throw new IllegalStateException("Unable to load the first level map.");
		buildWindow();
	}

	private void show() {
		frame.setVisible(true);
	}

	private void buildWindow() {
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				if (confirmDiscardChanges())
					frame.dispose();
			}
		});
		frame.setLayout(new BorderLayout(8, 8));
		frame.add(createToolbar(), BorderLayout.NORTH);
		frame.add(createPalette(), BorderLayout.WEST);
		frame.add(new JScrollPane(canvas), BorderLayout.CENTER);
		status.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
		frame.add(status, BorderLayout.SOUTH);
		frame.setMinimumSize(new Dimension(1000, 650));
		frame.setSize(1380, 850);
		frame.setLocationRelativeTo(null);
	}

	private JPanel createToolbar() {
		JPanel toolbar = new JPanel();
		JButton newButton = new JButton("New map");
		JButton openButton = new JButton("Open PNG");
		JButton saveButton = new JButton("Save");
		JButton saveAsButton = new JButton("Save as...");
		JButton testButton = new JButton("Test map");
		stagePicker = new JComboBox<>(STAGES);
		stagePicker.addActionListener(event -> {
			if (!updatingStagePicker)
				selectStage((StageOption) stagePicker.getSelectedItem());
		});
		syncStagePicker();

		newButton.addActionListener(event -> createNewMap());
		openButton.addActionListener(event -> openMap());
		saveButton.addActionListener(event -> saveMap(currentMapPath));
		saveAsButton.addActionListener(event -> saveMapAs());
		testButton.setToolTipText("Play the current map, including unsaved edits.");
		testButton.addActionListener(event -> testCurrentMap());

		toolbar.add(newButton);
		toolbar.add(openButton);
		toolbar.add(saveButton);
		toolbar.add(saveAsButton);
		toolbar.add(testButton);
		toolbar.add(stagePicker);
		toolbar.add(currentMapLabel);
		return toolbar;
	}

	private void selectStage(StageOption stage) {
		if (stage == null || isCurrentStage(stage.number))
			return;
		if (!confirmDiscardChanges()) {
			syncStagePicker();
			return;
		}

		Path stagePath = levelsDirectory.resolve(stage.number + ".png");
		if (Files.exists(stagePath)) {
			if (!loadLevel(stagePath))
				syncStagePicker();
		} else
			createStagePlaceholder(stage);
	}

	private boolean isCurrentStage(int stageNumber) {
		if (currentMapPath == null)
			return false;
		return currentMapPath.getFileName().toString().equals(stageNumber + ".png");
	}

	private void createStagePlaceholder(StageOption stage) {
		int width = 50;
		mapImage = new BufferedImage(width, Game.TILES_IN_HEIGHT, BufferedImage.TYPE_INT_RGB);
		modernTilePaths = new String[Game.TILES_IN_HEIGHT][width];
		for (int y = 0; y < mapImage.getHeight(); y++)
			for (int x = 0; x < mapImage.getWidth(); x++)
				setPixel(x, y, y == Game.TILES_IN_HEIGHT - 1 ? 13 : EMPTY_TILE, NO_MARKER, NO_MARKER);

		setPixel(2, Game.TILES_IN_HEIGHT - 2, EMPTY_TILE, 100, NO_MARKER);
		if (stage.number == 5)
			setPixel(width - 7, Game.TILES_IN_HEIGHT - 2, EMPTY_TILE, 0, NO_MARKER);

		currentMapPath = levelsDirectory.resolve(stage.number + ".png");
		unsavedChanges = true;
		updateStageDisplay();
		updateCanvas();
		status.setText(stage.label + " placeholder created. Place your boss, then save as " + stage.number + ".png.");
	}

	private void syncStagePicker() {
		if (stagePicker == null)
			return;
		int stageNumber = getNumberedStage(currentMapPath);
		updatingStagePicker = true;
		stagePicker.setSelectedItem(null);
		for (StageOption stage : STAGES)
			if (stage.number == stageNumber)
				stagePicker.setSelectedItem(stage);
		updatingStagePicker = false;
		updateStageDisplay();
	}

	private int getNumberedStage(Path path) {
		if (path == null)
			return -1;
		String name = path.getFileName().toString();
		if (!name.matches("[1-5]\\.png"))
			return -1;
		return Integer.parseInt(name.substring(0, name.length() - 4));
	}

	private void updateStageDisplay() {
		if (currentMapPath == null)
			return;
		int stageNumber = getNumberedStage(currentMapPath);
		String stageLabel = stageNumber > 0 ? STAGES[stageNumber - 1].label : "Custom map";
		currentMapLabel.setText("  " + stageLabel + " — " + currentMapPath.getFileName());
	}

	private void testCurrentMap() {
		if (testGameRunning) {
			status.setText("Close the current map test before starting another.");
			return;
		}

		try {
			BufferedImage snapshot = new BufferedImage(mapImage.getWidth(), mapImage.getHeight(),
					BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = snapshot.createGraphics();
			graphics.drawImage(mapImage, 0, 0, null);
			graphics.dispose();
			Level testLevel = new Level(snapshot, loadModernTileImages());
			testGameRunning = true;
			new Game(testLevel, () -> {
				testGameRunning = false;
				status.setText("Map test closed. You can continue editing.");
			});
			status.setText("Testing current map — close the game window to return to editing.");
		} catch (IOException | RuntimeException e) {
			testGameRunning = false;
			showError("Unable to start map test", e);
		}
	}

	private BufferedImage[][] loadModernTileImages() throws IOException {
		BufferedImage[][] tiles = new BufferedImage[modernTilePaths.length][modernTilePaths[0].length];
		Path assetsRoot = projectRoot.resolve("pananakop_modern_assets").normalize();
		Map<String, BufferedImage> imageCache = new TreeMap<>();
		for (int y = 0; y < modernTilePaths.length; y++)
			for (int x = 0; x < modernTilePaths[y].length; x++) {
				String relativePath = modernTilePaths[y][x];
				if (relativePath == null)
					continue;

				Path imagePath = projectRoot.resolve(relativePath).normalize();
				if (!imagePath.startsWith(assetsRoot) || !Files.isRegularFile(imagePath))
					throw new IOException("Invalid modern tile asset at map cell " + x + ", " + y + ": "
							+ relativePath);
				BufferedImage image = imageCache.get(relativePath);
				if (image == null) {
					image = ImageIO.read(imagePath.toFile());
					if (image == null)
						throw new IOException("Unsupported or empty image: " + imagePath);
					imageCache.put(relativePath, image);
				}
				tiles[y][x] = image;
			}
		return tiles;
	}

	private JPanel createPalette() {
		JPanel palette = new JPanel(new BorderLayout(4, 4));
		palette.setPreferredSize(new Dimension(260, 0));
		palette.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 2));
		materialPicker = new JComboBox<>();
		materialPicker.addItem("Classic tiles");
		for (String material : LoadSave.GetModernBlockMaterials())
			materialPicker.addItem(material);
		materialPicker.addActionListener(event -> refreshTilePalette());

		tileList = new JList<>();
		tileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tileList.setCellRenderer(new TileRenderer());
		tileList.addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting() && tileList.getSelectedValue() != null) {
				TileOption option = tileList.getSelectedValue();
				selectedTool = Tool.tile(option);
				status.setText("Painting " + option.label + " — click or drag on the map.");
			}
		});
		tileList.setName("tilePalette");
		JPanel blockPanel = new JPanel(new BorderLayout(4, 4));
		blockPanel.add(materialPicker, BorderLayout.NORTH);
		blockPanel.add(new JScrollPane(tileList), BorderLayout.CENTER);
		refreshTilePalette(tileList);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Blocks", blockPanel);
		tabs.addTab("Mobs & objects", createEntityPalette());
		palette.add(tabs, BorderLayout.CENTER);
		return palette;
	}

	private JPanel createEntityPalette() {
		JPanel buttons = new JPanel();
		buttons.setLayout(new javax.swing.BoxLayout(buttons, javax.swing.BoxLayout.Y_AXIS));
		addToolButton(buttons, "Player start", Tool.marker(100, NO_MARKER), "Player start");
		addToolButton(buttons, "Crabby mob", Tool.marker(0, NO_MARKER), "Crabby mob");
		addToolButton(buttons, "Red potion", Tool.object(0), "Red potion");
		addToolButton(buttons, "Blue potion", Tool.object(1), "Blue potion");
		addToolButton(buttons, "Barrel", Tool.object(2), "Barrel");
		addToolButton(buttons, "Crate", Tool.object(3), "Crate");
		addToolButton(buttons, "Spike trap", Tool.object(4), "Spike trap");
		addToolButton(buttons, "Cannon facing left", Tool.object(5), "Cannon facing left");
		addToolButton(buttons, "Cannon facing right", Tool.object(6), "Cannon facing right");
		for (Trap.Type type : Trap.getTypes())
			addTrapButton(buttons, type);
		addToolButton(buttons, "Erase cell", Tool.erase(), "Erase cell");

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JScrollPane(buttons), BorderLayout.CENTER);
		return panel;
	}

	private void addTrapButton(JPanel panel, Trap.Type type) {
		BufferedImage strip = LoadSave.GetTrapSpriteStrip(type.stripFile);
		BufferedImage frame = strip.getSubimage(0, 0, type.frameWidth, type.frameHeight);
		JButton button = new JButton(type.label,
				new javax.swing.ImageIcon(frame.getScaledInstance(28, 28, java.awt.Image.SCALE_FAST)));
		button.setAlignmentX(JButton.LEFT_ALIGNMENT);
		button.setToolTipText(type.bouncePad
				? "Animated trap — launches the player upward when touched."
				: "Animated trap — damages the player on contact.");
		button.addActionListener(event -> {
			selectedTool = Tool.object(type.objectId);
			status.setText("Selected " + type.label + " — click or drag on the map.");
		});
		panel.add(button);
	}

	private void addToolButton(JPanel panel, String label, Tool tool, String description) {
		JButton button = new JButton(label);
		button.setAlignmentX(JButton.LEFT_ALIGNMENT);
		button.addActionListener(event -> {
			selectedTool = tool;
			status.setText("Selected " + description + " — click or drag on the map.");
		});
		panel.add(button);
	}

	private void refreshTilePalette() {
		if (tileList != null)
			refreshTilePalette(tileList);
	}

	private void refreshTilePalette(JList<TileOption> tileList) {
		List<TileOption> options = new ArrayList<>();
		String material = (String) materialPicker.getSelectedItem();
		if (material == null)
			return;

		if (material.equals("Classic tiles")) {
			for (int i = 0; i < classicTiles.length; i++)
				options.add(new TileOption("Classic tile " + i, classicTiles[i], i, null));
		} else {
			for (Path tilePath : LoadSave.GetModernBlockTiles(material)) {
				try {
					BufferedImage image = ImageIO.read(tilePath.toFile());
					if (image == null)
						throw new IOException("Unsupported or empty image: " + tilePath);
					String relativePath = projectRoot.relativize(tilePath).toString().replace('\\', '/');
					options.add(new TileOption(material + " " + tilePath.getFileName().toString().replace(".png", ""),
							image, 0, relativePath));
				} catch (IOException e) {
					showError("Unable to load block tile " + tilePath, e);
					return;
				}
			}
		}

		tileList.setListData(options.toArray(new TileOption[0]));
	}

	private BufferedImage[] loadClassicTiles() {
		BufferedImage atlas = LoadSave.GetSpriteAtlas(LoadSave.LEVEL_ATLAS);
		BufferedImage[] tiles = new BufferedImage[48];
		for (int i = 0; i < tiles.length; i++)
			tiles[i] = atlas.getSubimage((i % 12) * TILE_SIZE, (i / 12) * TILE_SIZE, TILE_SIZE, TILE_SIZE);
		return tiles;
	}

	private void createNewMap() {
		if (!confirmDiscardChanges())
			return;
		String widthInput = JOptionPane.showInputDialog(frame, "Map width in tiles (height is fixed at 14):",
				"50");
		if (widthInput == null)
			return;

		try {
			int width = Integer.parseInt(widthInput.trim());
			if (width < Game.TILES_IN_WIDTH || width > 500)
				throw new NumberFormatException();
			int nextLevel = nextLevelNumber();
			Path newMapPath = levelsDirectory.resolve(nextLevel + ".png");
			if (Files.exists(newMapPath)) {
				int answer = JOptionPane.showConfirmDialog(frame,
						newMapPath.getFileName() + " already exists. Replace it?",
						"Confirm replace", JOptionPane.YES_NO_OPTION);
				if (answer != JOptionPane.YES_OPTION)
					return;
			}
			mapImage = new BufferedImage(width, Game.TILES_IN_HEIGHT, BufferedImage.TYPE_INT_RGB);
			modernTilePaths = new String[Game.TILES_IN_HEIGHT][width];
			for (int y = 0; y < mapImage.getHeight(); y++)
				for (int x = 0; x < mapImage.getWidth(); x++)
					setPixel(x, y, EMPTY_TILE, NO_MARKER, NO_MARKER);
			currentMapPath = newMapPath;
			unsavedChanges = true;
			syncStagePicker();
			updateCanvas();
			status.setText("New " + width + " × " + Game.TILES_IN_HEIGHT + " map.");
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(frame, "Map width must be a whole number from "
					+ Game.TILES_IN_WIDTH + " to 500.",
					"Invalid map size", JOptionPane.ERROR_MESSAGE);
		}
	}

	private int nextLevelNumber() {
		int highest = 0;
		try (var files = Files.newDirectoryStream(levelsDirectory, "*.png")) {
			for (Path path : files) {
				String name = path.getFileName().toString();
				if (name.matches("\\d+\\.png"))
					highest = Math.max(highest, Integer.parseInt(name.substring(0, name.length() - 4)));
			}
		} catch (IOException e) {
			showError("Unable to inspect levels directory " + levelsDirectory, e);
		}
		return highest + 1;
	}

	private void openMap() {
		JFileChooser chooser = new JFileChooser(levelsDirectory.toFile());
		chooser.setFileFilter(new FileNameExtensionFilter("PNG level maps", "png"));
		if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION && confirmDiscardChanges())
			loadLevel(chooser.getSelectedFile().toPath());
	}

	private boolean loadLevel(Path path) {
		try {
			BufferedImage loadedImage = ImageIO.read(path.toFile());
			if (loadedImage == null)
				throw new IOException("Unsupported or empty image: " + path);
			if (loadedImage.getHeight() != Game.TILES_IN_HEIGHT)
				throw new IOException("Level maps must be " + Game.TILES_IN_HEIGHT + " tiles high.");

			BufferedImage normalized = new BufferedImage(loadedImage.getWidth(), loadedImage.getHeight(),
					BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = normalized.createGraphics();
			graphics.drawImage(loadedImage, 0, 0, null);
			graphics.dispose();
			Path absolutePath = path.toAbsolutePath().normalize();
			String[][] loadedModernPaths = loadModernTilePaths(absolutePath, normalized);
			mapImage = normalized;
			currentMapPath = absolutePath;
			modernTilePaths = loadedModernPaths;
			unsavedChanges = false;
			syncStagePicker();
			updateCanvas();
			status.setText(path.getFileName() + " — " + mapImage.getWidth() + " × " + mapImage.getHeight()
					+ " tiles.");
			return true;
		} catch (IOException | RuntimeException e) {
			showError("Unable to open map " + path, e);
			return false;
		}
	}

	private String[][] loadModernTilePaths(Path mapPath, BufferedImage image) throws IOException {
		String[][] paths = new String[image.getHeight()][image.getWidth()];
		String fileName = mapPath.getFileName().toString();
		Path sidecar = mapPath.resolveSibling(fileName.substring(0, fileName.length() - 4) + ".tiles");
		if (!Files.exists(sidecar))
			return paths;

		Path assetsRoot = projectRoot.resolve("pananakop_modern_assets").normalize();
		List<String> lines = Files.readAllLines(sidecar);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.isEmpty())
				continue;
			String[] fields = line.split("\t", 3);
			if (fields.length != 3)
				throw new IOException("Malformed tile entry on line " + (i + 1));
			int x;
			int y;
			try {
				x = Integer.parseInt(fields[0]);
				y = Integer.parseInt(fields[1]);
			} catch (NumberFormatException e) {
				throw new IOException("Invalid tile position on line " + (i + 1), e);
			}
			Path asset = projectRoot.resolve(fields[2]).normalize();
			if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()
					|| !asset.startsWith(assetsRoot) || !Files.isRegularFile(asset))
				throw new IOException("Invalid modern tile entry on line " + (i + 1));
			paths[y][x] = fields[2];
		}
		return paths;
	}

	private void saveMapAs() {
		JFileChooser chooser = new JFileChooser(levelsDirectory.toFile());
		chooser.setFileFilter(new FileNameExtensionFilter("PNG level maps", "png"));
		chooser.setSelectedFile(currentMapPath.toFile());
		if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
			Path path = chooser.getSelectedFile().toPath();
			if (!path.getFileName().toString().toLowerCase().endsWith(".png"))
				path = path.resolveSibling(path.getFileName() + ".png");
			if (Files.exists(path) && !path.equals(currentMapPath)) {
				int answer = JOptionPane.showConfirmDialog(frame, "Replace " + path.getFileName() + "?",
						"Confirm replace", JOptionPane.YES_NO_OPTION);
				if (answer != JOptionPane.YES_OPTION)
					return;
			}
			saveMap(path);
		}
	}

	private void saveMap(Path path) {
		if (path == null || mapImage == null)
			return;
		try {
			Path absolutePath = path.toAbsolutePath().normalize();
			Files.createDirectories(absolutePath.getParent());
			if (!ImageIO.write(mapImage, "png", absolutePath.toFile()))
				throw new IOException("No PNG writer is available.");

			String fileName = absolutePath.getFileName().toString();
			Path sidecar = absolutePath.resolveSibling(fileName.substring(0, fileName.length() - 4) + ".tiles");
			List<String> lines = new ArrayList<>();
			for (int y = 0; y < modernTilePaths.length; y++)
				for (int x = 0; x < modernTilePaths[y].length; x++)
					if (modernTilePaths[y][x] != null)
						lines.add(x + "\t" + y + "\t" + modernTilePaths[y][x]);
			if (lines.isEmpty())
				Files.deleteIfExists(sidecar);
			else
				Files.write(sidecar, lines);

			currentMapPath = absolutePath;
			unsavedChanges = false;
			syncStagePicker();
			status.setText("Saved " + absolutePath.getFileName() + " — " + mapImage.getWidth() + " × "
					+ mapImage.getHeight() + " tiles.");
		} catch (IOException e) {
			showError("Unable to save map " + path, e);
		}
	}

	private void paintCell(int x, int y, boolean erase) {
		if (x < 0 || x >= mapImage.getWidth() || y < 0 || y >= mapImage.getHeight())
			return;
		if (erase) {
			clearCell(x, y);
			unsavedChanges = true;
			canvas.repaint();
			return;
		}

		Color pixel = new Color(mapImage.getRGB(x, y));
		switch (selectedTool.kind) {
		case TILE:
			setPixel(x, y, selectedTool.tileIndex, pixel.getGreen(), pixel.getBlue());
			modernTilePaths[y][x] = selectedTool.modernPath;
			break;
		case ERASE:
			setPixel(x, y, EMPTY_TILE, NO_MARKER, NO_MARKER);
			modernTilePaths[y][x] = null;
			break;
		case MARKER:
			if (selectedTool.green == 100)
				clearPlayerMarkers();
			setPixel(x, y, pixel.getRed(), selectedTool.green, pixel.getBlue());
			break;
		case OBJECT:
			setPixel(x, y, pixel.getRed(), pixel.getGreen(), selectedTool.blue);
			break;
		}
		unsavedChanges = true;
		canvas.repaint();
	}

	private void clearCell(int x, int y) {
		setPixel(x, y, EMPTY_TILE, NO_MARKER, NO_MARKER);
		modernTilePaths[y][x] = null;
	}

	private void clearPlayerMarkers() {
		for (int y = 0; y < mapImage.getHeight(); y++)
			for (int x = 0; x < mapImage.getWidth(); x++) {
				Color pixel = new Color(mapImage.getRGB(x, y));
				if (pixel.getGreen() == 100)
					setPixel(x, y, pixel.getRed(), NO_MARKER, pixel.getBlue());
			}
	}

	private void setPixel(int x, int y, int red, int green, int blue) {
		mapImage.setRGB(x, y, new Color(red, green, blue).getRGB());
	}

	private void updateCanvas() {
		canvas.setPreferredSize(new Dimension(mapImage.getWidth() * TILE_SIZE, mapImage.getHeight() * TILE_SIZE));
		canvas.revalidate();
		canvas.repaint();
	}

	private void showError(String message, Exception exception) {
		JOptionPane.showMessageDialog(frame, message + "\n" + exception.getMessage(),
				"Map editor error", JOptionPane.ERROR_MESSAGE);
	}

	private boolean confirmDiscardChanges() {
		if (!unsavedChanges)
			return true;
		int answer = JOptionPane.showConfirmDialog(frame, "Save changes to " + currentMapPath.getFileName() + "?",
				"Unsaved map changes", JOptionPane.YES_NO_CANCEL_OPTION);
		if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION)
			return false;
		if (answer == JOptionPane.YES_OPTION) {
			saveMap(currentMapPath);
			return !unsavedChanges;
		}
		return true;
	}

	private class EditorCanvas extends JPanel {
		private static final long serialVersionUID = 1L;

		private EditorCanvas() {
			setBackground(new Color(28, 31, 38));
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent event) {
					if (SwingUtilities.isLeftMouseButton(event))
						paintAt(event, false);
					else if (SwingUtilities.isRightMouseButton(event))
						paintAt(event, true);
				}
			});
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseDragged(MouseEvent event) {
					if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0)
						paintAt(event, false);
					else if ((event.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0)
						paintAt(event, true);
				}
			});
		}

		private void paintAt(MouseEvent event, boolean erase) {
			paintCell(event.getX() / TILE_SIZE, event.getY() / TILE_SIZE, erase);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			if (mapImage == null)
				return;
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			for (int y = 0; y < mapImage.getHeight(); y++)
				for (int x = 0; x < mapImage.getWidth(); x++) {
					int px = x * TILE_SIZE;
					int py = y * TILE_SIZE;
					Color pixel = new Color(mapImage.getRGB(x, y));
					int tileIndex = pixel.getRed();
					if (tileIndex >= 0 && tileIndex < classicTiles.length)
						g.drawImage(classicTiles[tileIndex], px, py, TILE_SIZE, TILE_SIZE, null);

					String modernPath = modernTilePaths[y][x];
					if (modernPath != null) {
						BufferedImage modernTile = previewCache.get(modernPath);
						if (modernTile == null) {
							Path path = projectRoot.resolve(modernPath);
							try {
								modernTile = ImageIO.read(path.toFile());
								if (modernTile == null)
									throw new IOException("Unsupported or empty image: " + path);
								previewCache.put(modernPath, modernTile);
							} catch (IOException e) {
								showError("Unable to draw modern tile " + path, e);
								modernTilePaths[y][x] = null;
							}
						}
						if (modernTile != null)
							g.drawImage(modernTile, px, py, TILE_SIZE, TILE_SIZE, null);
					}

					drawMarkers(g, pixel, px, py);
				}

			g.setColor(new Color(15, 18, 24, 105));
			for (int x = 0; x <= mapImage.getWidth(); x++)
				g.drawLine(x * TILE_SIZE, 0, x * TILE_SIZE, mapImage.getHeight() * TILE_SIZE);
			for (int y = 0; y <= mapImage.getHeight(); y++)
				g.drawLine(0, y * TILE_SIZE, mapImage.getWidth() * TILE_SIZE, y * TILE_SIZE);
			g.dispose();
		}

		private void drawMarkers(Graphics2D g, Color pixel, int x, int y) {
			int green = pixel.getGreen();
			int blue = pixel.getBlue();
			String label = null;
			Color color = null;
			if (green == 100) {
				label = "P";
				color = new Color(50, 210, 255);
			} else if (green == 0) {
				label = "M";
				color = new Color(255, 95, 95);
			} else if (blue <= 6) {
				String[] labels = { "R", "B", "B", "C", "S", "L", "R" };
				label = labels[blue];
				color = new Color(255, 215, 75);
			} else if (Trap.getType(blue).isPresent()) {
				label = "T" + (blue - 6);
				color = new Color(255, 125, 65);
			}
			if (label != null) {
				g.setColor(color);
				g.fillOval(x + 8, y + 8, 16, 16);
				g.setColor(Color.BLACK);
				g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, label.length() > 1 ? 9 : 12));
				g.drawString(label, x + (label.length() > 1 ? 9 : 13), y + 20);
			}
		}
	}

	private static class TileOption {
		private final String label;
		private final BufferedImage image;
		private final int tileIndex;
		private final String modernPath;

		private TileOption(String label, BufferedImage image, int tileIndex, String modernPath) {
			this.label = label;
			this.image = image;
			this.tileIndex = tileIndex;
			this.modernPath = modernPath;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	private static class StageOption {
		private final int number;
		private final String label;

		private StageOption(int number, String label) {
			this.number = number;
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	private static class TileRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			TileOption option = (TileOption) value;
			label.setText(option.label);
			label.setIcon(new javax.swing.ImageIcon(option.image.getScaledInstance(32, 32, java.awt.Image.SCALE_FAST)));
			label.setIconTextGap(10);
			label.setPreferredSize(new Dimension(220, 40));
			return label;
		}
	}

	private enum ToolKind {
		TILE, ERASE, MARKER, OBJECT
	}

	private static class Tool {
		private final ToolKind kind;
		private final int tileIndex;
		private final String modernPath;
		private final int green;
		private final int blue;

		private Tool(ToolKind kind, int tileIndex, String modernPath, int green, int blue) {
			this.kind = kind;
			this.tileIndex = tileIndex;
			this.modernPath = modernPath;
			this.green = green;
			this.blue = blue;
		}

		private static Tool tile(TileOption option) {
			return new Tool(ToolKind.TILE, option.tileIndex, option.modernPath, 0, 0);
		}

		private static Tool erase() {
			return new Tool(ToolKind.ERASE, EMPTY_TILE, null, NO_MARKER, NO_MARKER);
		}

		private static Tool marker(int green, int blue) {
			return new Tool(ToolKind.MARKER, EMPTY_TILE, null, green, blue);
		}

		private static Tool object(int blue) {
			return new Tool(ToolKind.OBJECT, EMPTY_TILE, null, NO_MARKER, blue);
		}
	}
}
