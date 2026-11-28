package GameEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
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
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import main.Game;
import levels.Level;
import levels.LevelDecoration;
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
			new StageOption(5, "Stage 5: Magellan Boss Arena")
	};

	private final Path projectRoot = LoadSave.GetProjectRoot();
	private final Path levelsDirectory = projectRoot.resolve("res").resolve("lvls");
	private final BufferedImage[] classicTiles = loadClassicTiles();
	private final JFrame frame = new JFrame("Pananakop Map Editor");
	private final Map<String, BufferedImage> previewCache = new TreeMap<>();
	private final List<BackgroundOption> backgroundOptions = new ArrayList<>();
	private final List<LevelDecoration> placedDecorations = new ArrayList<>();
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
	private JList<BackgroundOption> backgroundList;
	private JComboBox<String> decorationCategoryPicker;
	private JList<DecorationOption> decorationList;
	private JLabel decorationSizeLabel;
	private BackgroundOption selectedBackground;
	private DecorationOption selectedDecoration;
	private int decorationScale = 100;
	private Point decorationPreviewPosition;
	private boolean unsavedChanges;
	private boolean testGameRunning;
	private boolean updatingStagePicker;
	private boolean updatingBackgroundList;

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
		JButton arenaPreviewButton = new JButton("Boss arena view");
		JButton buildArenaButton = new JButton("Build boss arena");
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
		arenaPreviewButton.addActionListener(event -> showBossArenaPreview());
		buildArenaButton.addActionListener(event -> buildBossArena());

		toolbar.add(newButton);
		toolbar.add(openButton);
		toolbar.add(saveButton);
		toolbar.add(saveAsButton);
		toolbar.add(testButton);
		toolbar.add(arenaPreviewButton);
		toolbar.add(buildArenaButton);
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
		placedDecorations.clear();
		for (int y = 0; y < mapImage.getHeight(); y++)
			for (int x = 0; x < mapImage.getWidth(); x++)
				setPixel(x, y, y == Game.TILES_IN_HEIGHT - 1 ? 13 : EMPTY_TILE, NO_MARKER, NO_MARKER);

		if (stage.number == 5) {
			// The decorative sandstone floor is also the collision surface.
			for (int y = Game.TILES_IN_HEIGHT - 3; y < Game.TILES_IN_HEIGHT - 1; y++)
				for (int x = 0; x < width; x++) {
					setPixel(x, y, 13, NO_MARKER, NO_MARKER);
				}
			setPixel(2, Game.TILES_IN_HEIGHT - 4, EMPTY_TILE, 100, NO_MARKER);
			// Magellan's hitbox is taller than a normal mob, so its top-left
			// spawn marker starts three tiles above the floor.
			setPixel(width - 7, Game.TILES_IN_HEIGHT - 6, EMPTY_TILE, 1, NO_MARKER);
		} else {
			setPixel(2, Game.TILES_IN_HEIGHT - 2, EMPTY_TILE, 100, NO_MARKER);
		}

		selectedBackground = null;
		currentMapPath = levelsDirectory.resolve(stage.number + ".png");
		unsavedChanges = true;
		updateStageDisplay();
		syncBackgroundSelection();
		updateCanvas();
		status.setText(stage.label + " created with Magellan and the final arena layout. Save to use it.");
	}

	private void showBossArenaPreview() {
		try {
			BufferedImage preview = LoadSave.GetSpriteAtlas("pananakop_boss_magellan/previews/boss_arena_preview.png");
			JLabel image = new JLabel(new javax.swing.ImageIcon(preview));
			JOptionPane.showMessageDialog(frame, image, "Magellan Boss Arena", JOptionPane.PLAIN_MESSAGE);
		} catch (RuntimeException e) {
			showError("Unable to open boss arena preview", e);
		}
	}

	private void buildBossArena() {
		if (!isCurrentStage(5)) {
			selectStage(STAGES[4]);
			return;
		}
		int answer = JOptionPane.showConfirmDialog(frame,
				"Build the 50-tile Magellan arena and replace the current Stage 5 layout?",
				"Build boss arena", JOptionPane.YES_NO_OPTION);
		if (answer != JOptionPane.YES_OPTION)
			return;

		for (int y = 0; y < mapImage.getHeight(); y++)
			for (int x = 0; x < mapImage.getWidth(); x++) {
				setPixel(x, y, EMPTY_TILE, NO_MARKER, NO_MARKER);
				modernTilePaths[y][x] = null;
				if (y >= Game.TILES_IN_HEIGHT - 3)
					setPixel(x, y, 13, NO_MARKER, NO_MARKER);
			}
		setPixel(2, Game.TILES_IN_HEIGHT - 4, EMPTY_TILE, 100, NO_MARKER);
		setPixel(mapImage.getWidth() - 7, Game.TILES_IN_HEIGHT - 6, EMPTY_TILE, 1, NO_MARKER);
		unsavedChanges = true;
		updateCanvas();
		status.setText("Magellan arena built with sandstone floor, player start, and boss start. Save Stage 5.");
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
			Level testLevel = new Level(snapshot, loadModernTileImages(), placedDecorations);
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
		Map<String, BufferedImage> imageCache = new TreeMap<>();
		for (int y = 0; y < modernTilePaths.length; y++)
			for (int x = 0; x < modernTilePaths[y].length; x++) {
				String relativePath = modernTilePaths[y][x];
				if (relativePath == null)
					continue;
				BufferedImage image = imageCache.get(relativePath);
				if (image == null) {
					image = ImageIO.read(projectRoot.resolve(relativePath).toFile());
					if (image == null)
						throw new IOException("Unsupported or empty image: " + relativePath);
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
		materialPicker.addItem("Platformer");
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
		tabs.addTab("Decorations", createDecorationPalette());
		tabs.addTab("Mobs & objects", createEntityPalette());
		tabs.addTab("Background", createBackgroundPalette());
		palette.add(tabs, BorderLayout.CENTER);
		return palette;
	}

	private JPanel createBackgroundPalette() {
		backgroundList = new JList<>();
		backgroundList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		backgroundList.setCellRenderer(new BackgroundRenderer());
		backgroundList.addListSelectionListener(event -> {
			if (event.getValueIsAdjusting() || updatingBackgroundList)
				return;
			BackgroundOption option = backgroundList.getSelectedValue();
			if (option == null)
				return;
			selectedBackground = option;
			unsavedChanges = true;
			canvas.repaint();
			status.setText(option.image == null ? "Background cleared." : "Background set to " + option.label + ".");
		});

		JPanel panel = new JPanel(new BorderLayout(4, 4));
		panel.add(new JLabel("Choose a backdrop for this map:"), BorderLayout.NORTH);
		panel.add(new JScrollPane(backgroundList), BorderLayout.CENTER);
		loadBackgroundOptions();
		syncBackgroundSelection();
		return panel;
	}

	private JPanel createDecorationPalette() {
		decorationCategoryPicker = new JComboBox<>(new String[] { "Building", "Environment" });
		decorationCategoryPicker.addActionListener(event -> loadDecorationOptions());
		decorationList = new JList<>();
		decorationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		decorationList.setCellRenderer(new DecorationRenderer());
		decorationList.addListSelectionListener(event -> {
			if (event.getValueIsAdjusting())
				return;
			DecorationOption option = decorationList.getSelectedValue();
			if (option == null)
				return;
			try {
				option.image = ImageIO.read(option.path.toFile());
				if (option.image == null)
					throw new IOException("Unsupported or empty image: " + option.path);
			} catch (IOException e) {
				showError("Unable to load decoration " + option.label, e);
				return;
			}
			selectedDecoration = option;
			selectedTool = Tool.decoration();
			status.setText("Selected " + option.label + " — click the map to place it.");
		});

		JSlider sizeSlider = new JSlider(25, 200, 100);
		sizeSlider.setMajorTickSpacing(25);
		sizeSlider.setPaintTicks(true);
		decorationSizeLabel = new JLabel("Size: 100%");
		sizeSlider.addChangeListener(event -> {
			decorationScale = sizeSlider.getValue();
			decorationSizeLabel.setText("Size: " + decorationScale + "%");
		});
		JPanel sizePanel = new JPanel(new BorderLayout());
		sizePanel.add(decorationSizeLabel, BorderLayout.NORTH);
		sizePanel.add(sizeSlider, BorderLayout.CENTER);

		JPanel panel = new JPanel(new BorderLayout(4, 4));
		panel.add(decorationCategoryPicker, BorderLayout.NORTH);
		panel.add(new JScrollPane(decorationList), BorderLayout.CENTER);
		panel.add(sizePanel, BorderLayout.SOUTH);
		loadDecorationOptions();
		return panel;
	}

	private void loadDecorationOptions() {
		if (decorationList == null)
			return;
		String category = (String) decorationCategoryPicker.getSelectedItem();
		List<DecorationOption> options = new ArrayList<>();
		if (category != null) {
			Path directory = projectRoot.resolve("pananakop_modern_assets").resolve(category).normalize();
			if (Files.isDirectory(directory)) {
				try (var paths = Files.list(directory)) {
					paths.filter(Files::isRegularFile)
							.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
							.sorted()
							.forEach(path -> {
								try {
									BufferedImage image = ImageIO.read(path.toFile());
									if (image == null)
										return;
									String label = path.getFileName().toString().replaceFirst("(?i)\\.png$", "");
									int separator = label.lastIndexOf(" - ");
									if (separator >= 0)
										label = label.substring(separator + 3);
									options.add(new DecorationOption(label, path, createDecorationThumbnail(image)));
								} catch (IOException ignored) {
									// Skip files that cannot be read as images.
								}
							});
				} catch (IOException e) {
					showError("Unable to list " + category + " decorations", e);
				}
			}
		}
		decorationList.setListData(options.toArray(new DecorationOption[0]));
	}

	private BufferedImage createDecorationThumbnail(BufferedImage source) {
		BufferedImage thumbnail = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = thumbnail.createGraphics();
		float scale = Math.min(48f / source.getWidth(), 48f / source.getHeight());
		int width = Math.max(1, Math.round(source.getWidth() * scale));
		int height = Math.max(1, Math.round(source.getHeight() * scale));
		g.drawImage(source, (48 - width) / 2, (48 - height) / 2, width, height, null);
		g.dispose();
		return thumbnail;
	}

	private void loadBackgroundOptions() {
		backgroundOptions.clear();
		backgroundOptions.add(new BackgroundOption("No background", null, null));
		Path backgroundRoot = projectRoot.resolve("pananakop_modern_assets").resolve("Background").normalize();
		if (!Files.isDirectory(backgroundRoot)) {
			backgroundList.setListData(backgroundOptions.toArray(new BackgroundOption[0]));
			return;
		}

		try (var paths = Files.walk(backgroundRoot)) {
			paths.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().toLowerCase().startsWith("full_bg")
							&& path.getFileName().toString().toLowerCase().endsWith(".png"))
					.sorted()
					.forEach(path -> {
						try {
							BufferedImage image = ImageIO.read(path.toFile());
							if (image == null)
								return;
						String label = backgroundRoot.relativize(path).toString().replace('\\', '/');
						backgroundOptions.add(new BackgroundOption(label, path, image));
						} catch (IOException ignored) {
							// Skip broken background images and keep the editor usable.
						}
					});
		} catch (IOException e) {
			showError("Unable to list background images", e);
		}
		backgroundList.setListData(backgroundOptions.toArray(new BackgroundOption[0]));
	}

	private void syncBackgroundSelection() {
		if (backgroundList == null)
			return;
		updatingBackgroundList = true;
		for (int i = 0; i < backgroundOptions.size(); i++) {
			BackgroundOption option = backgroundOptions.get(i);
			if (selectedBackground == null ? option.image == null
					: option.path != null && option.path.equals(selectedBackground.path)) {
				backgroundList.setSelectedIndex(i);
				updatingBackgroundList = false;
				return;
			}
		}
		backgroundList.clearSelection();
		updatingBackgroundList = false;
	}

	private JPanel createEntityPalette() {
		JPanel buttons = new JPanel();
		buttons.setLayout(new javax.swing.BoxLayout(buttons, javax.swing.BoxLayout.Y_AXIS));
		addToolButton(buttons, "Player start", Tool.marker(100, NO_MARKER), "Player start");
		addToolButton(buttons, "Crabby mob", Tool.marker(0, NO_MARKER), "Crabby mob");
		addToolButton(buttons, "Boar mob", Tool.marker(2, NO_MARKER), "Boar mob");
		addToolButton(buttons, "Conquistador mob", Tool.marker(3, NO_MARKER), "Conquistador mob");
		addToolButton(buttons, "Espinosa mob", Tool.marker(4, NO_MARKER), "Espinosa mob");
		addToolButton(buttons, "Monkey mob", Tool.marker(5, NO_MARKER), "Monkey mob");
		try {
			BufferedImage portrait = LoadSave.GetSpriteAtlas("pananakop_boss_magellan/portrait/magellan_portrait_64.png");
			addToolButton(buttons, "Magellan boss (all skills)", Tool.marker(1, NO_MARKER),
					"Magellan boss (all skills)", portrait);
		} catch (RuntimeException e) {
			addToolButton(buttons, "Magellan boss (all skills)", Tool.marker(1, NO_MARKER),
					"Magellan boss (all skills)");
		}
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
		addToolButton(panel, label, tool, description, null);
	}

	private void addToolButton(JPanel panel, String label, Tool tool, String description, BufferedImage icon) {
		JButton button = new JButton(label);
		if (icon != null)
			button.setIcon(new javax.swing.ImageIcon(icon.getScaledInstance(36, 36, java.awt.Image.SCALE_FAST)));
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
			Path categoryDirectory = projectRoot.resolve("pananakop_modern_assets").resolve(material).normalize();
			if (Files.isDirectory(categoryDirectory)) {
				try (var paths = Files.list(categoryDirectory)) {
					paths.filter(Files::isRegularFile)
							.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
							.sorted()
							.forEach(path -> {
								try {
									BufferedImage image = ImageIO.read(path.toFile());
									if (image == null)
										return;
									String relativePath = projectRoot.relativize(path).toString().replace('\\', '/');
									String label = path.getFileName().toString().replaceFirst("(?i)\\.png$", "");
									int separator = label.lastIndexOf(" - ");
									if (separator >= 0)
										label = label.substring(separator + 3);
									// Keep the cell clear beneath transparent pixels, like an empty classic tile.
								options.add(new TileOption(label, image, EMPTY_TILE, relativePath));
								} catch (IOException ignored) {
									// Skip image files that cannot be decoded.
								}
							});
				} catch (IOException e) {
					showError("Unable to list " + material + " block tiles", e);
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
			placedDecorations.clear();
			for (int y = 0; y < mapImage.getHeight(); y++)
				for (int x = 0; x < mapImage.getWidth(); x++)
					setPixel(x, y, EMPTY_TILE, NO_MARKER, NO_MARKER);
			selectedBackground = null;
			currentMapPath = newMapPath;
			unsavedChanges = true;
			syncStagePicker();
			syncBackgroundSelection();
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
			BackgroundOption loadedBackground = loadBackground(absolutePath);
			List<LevelDecoration> loadedDecorations = loadDecorations(absolutePath, normalized);
			mapImage = normalized;
			currentMapPath = absolutePath;
			modernTilePaths = loadedModernPaths;
			selectedBackground = loadedBackground;
			placedDecorations.clear();
			placedDecorations.addAll(loadedDecorations);
			unsavedChanges = false;
			syncStagePicker();
			syncBackgroundSelection();
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
		Path sidecar = mapPath.resolveSibling(mapPath.getFileName().toString().replaceFirst("(?i)\\.png$", ".tiles"));
		if (!Files.isRegularFile(sidecar))
			return paths;
		Path assetsRoot = projectRoot.resolve("pananakop_modern_assets").normalize();
		for (String line : Files.readAllLines(sidecar)) {
			if (line.isBlank())
				continue;
			String[] fields = line.split("\\t", 3);
			if (fields.length != 3)
				continue;
			try {
				int x = Integer.parseInt(fields[0]);
				int y = Integer.parseInt(fields[1]);
				Path asset = projectRoot.resolve(fields[2]).normalize();
				boolean validCategory = asset.startsWith(assetsRoot.resolve("Building"))
						|| asset.startsWith(assetsRoot.resolve("Environment"))
						|| asset.startsWith(assetsRoot.resolve("Platformer"));
				if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()
						&& validCategory && Files.isRegularFile(asset))
					paths[y][x] = fields[2];
			} catch (NumberFormatException ignored) {
				// Ignore malformed or stale entries from removed asset packs.
			}
		}
		return paths;
	}

	private List<LevelDecoration> loadDecorations(Path mapPath, BufferedImage image) throws IOException {
		List<LevelDecoration> decorations = new ArrayList<>();
		Path sidecar = getDecorationsSidecar(mapPath);
		if (!Files.isRegularFile(sidecar))
			return decorations;
		Path assetsRoot = projectRoot.resolve("pananakop_modern_assets").normalize();
		for (String line : Files.readAllLines(sidecar)) {
			if (line.isBlank())
				continue;
			String[] fields = line.split("\\t", 5);
			if (fields.length != 5)
				continue;
			try {
				int x = Integer.parseInt(fields[0]);
				int y = Integer.parseInt(fields[1]);
				int width = Integer.parseInt(fields[2]);
				int height = Integer.parseInt(fields[3]);
				Path asset = projectRoot.resolve(fields[4]).normalize();
				boolean validCategory = asset.startsWith(assetsRoot.resolve("Building"))
						|| asset.startsWith(assetsRoot.resolve("Environment"));
				if (x < 0 || y < 0 || x >= image.getWidth() * TILE_SIZE || y >= image.getHeight() * TILE_SIZE
						|| width < 1 || height < 1 || !validCategory || !Files.isRegularFile(asset))
					continue;
				BufferedImage decorationImage = ImageIO.read(asset.toFile());
				if (decorationImage != null)
					decorations.add(new LevelDecoration(x, y, width, height, asset, decorationImage));
			} catch (NumberFormatException ignored) {
				// Skip malformed entries and continue opening the map.
			}
		}
		return decorations;
	}

	private Path getDecorationsSidecar(Path mapPath) {
		String fileName = mapPath.getFileName().toString();
		return mapPath.resolveSibling(fileName.replaceFirst("(?i)\\.png$", ".decorations"));
	}

	private BackgroundOption loadBackground(Path mapPath) throws IOException {
		Path sidecar = getBackgroundSidecar(mapPath);
		if (!Files.isRegularFile(sidecar))
			return null;

		String relativePath = Files.readString(sidecar).trim();
		if (relativePath.isEmpty())
			return null;
		Path backgroundRoot = projectRoot.resolve("pananakop_modern_assets").resolve("Background").normalize();
		Path imagePath = projectRoot.resolve(relativePath).normalize();
		if (!imagePath.startsWith(backgroundRoot) || !Files.isRegularFile(imagePath))
			throw new IOException("Invalid background image in " + sidecar + ": " + relativePath);
		BufferedImage image = ImageIO.read(imagePath.toFile());
		if (image == null)
			throw new IOException("Unsupported or empty background image: " + imagePath);
		return new BackgroundOption(backgroundRoot.relativize(imagePath).toString().replace('\\', '/'),
				imagePath, image);
	}

	private Path getBackgroundSidecar(Path mapPath) {
		String fileName = mapPath.getFileName().toString();
		return mapPath.resolveSibling(fileName.substring(0, fileName.length() - 4) + ".background");
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

			Path backgroundSidecar = getBackgroundSidecar(absolutePath);
			if (selectedBackground == null || selectedBackground.image == null)
				Files.deleteIfExists(backgroundSidecar);
			else {
				Path backgroundRoot = projectRoot.resolve("pananakop_modern_assets").resolve("Background").normalize();
				Path backgroundPath = selectedBackground.path.toAbsolutePath().normalize();
				if (!backgroundPath.startsWith(backgroundRoot) || !Files.isRegularFile(backgroundPath))
					throw new IOException("Selected background is outside the project's Background assets.");
				Files.writeString(backgroundSidecar,
						projectRoot.relativize(backgroundPath).toString().replace('\\', '/') + System.lineSeparator());
			}

			Path decorationsSidecar = getDecorationsSidecar(absolutePath);
			if (placedDecorations.isEmpty())
				Files.deleteIfExists(decorationsSidecar);
			else {
				List<String> decorationLines = new ArrayList<>();
				for (LevelDecoration decoration : placedDecorations) {
					Path asset = decoration.getAssetPath().toAbsolutePath().normalize();
					if (!Files.isRegularFile(asset))
						throw new IOException("Decoration image is missing: " + asset);
					decorationLines.add(decoration.getX() + "\t" + decoration.getY() + "\t"
							+ decoration.getWidth() + "\t" + decoration.getHeight() + "\t"
							+ projectRoot.relativize(asset).toString().replace('\\', '/'));
				}
				Files.write(decorationsSidecar, decorationLines);
			}

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
			if (removeDecorationAt(x * TILE_SIZE, y * TILE_SIZE)) {
				unsavedChanges = true;
				canvas.repaint();
				return;
			}
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
			if (selectedTool.green == 1)
				clearBossMarkers();
			setPixel(x, y, pixel.getRed(), selectedTool.green, pixel.getBlue());
			break;
		case OBJECT:
			setPixel(x, y, pixel.getRed(), pixel.getGreen(), selectedTool.blue);
			break;
		case DECORATION:
			break;
		}
		unsavedChanges = true;
		canvas.repaint();
	}

	/** Returns the first solid row in this column, then puts a marker above it. */
	private int getSurfaceMarkerY(int x, boolean bossMarker) {
		for (int y = 0; y < mapImage.getHeight(); y++) {
			Color pixel = new Color(mapImage.getRGB(x, y));
			if (pixel.getRed() >= 48 || modernTilePaths[y][x] != null)
				return Math.max(0, y - (bossMarker ? 3 : 1));
		}
		return Math.max(0, mapImage.getHeight() - (bossMarker ? 4 : 2));
	}

	private void clearCell(int x, int y) {
		setPixel(x, y, EMPTY_TILE, NO_MARKER, NO_MARKER);
		modernTilePaths[y][x] = null;
	}

	private boolean removeDecorationAt(int x, int y) {
		for (int i = placedDecorations.size() - 1; i >= 0; i--) {
			LevelDecoration decoration = placedDecorations.get(i);
			if (x >= decoration.getX() && x < decoration.getX() + decoration.getWidth()
					&& y >= decoration.getY() && y < decoration.getY() + decoration.getHeight()) {
				placedDecorations.remove(i);
				return true;
			}
		}
		return false;
	}

	private int getSelectedDecorationWidth() {
		return Math.max(1, selectedDecoration.image.getWidth() * decorationScale / 100);
	}

	private int getSelectedDecorationHeight() {
		return Math.max(1, selectedDecoration.image.getHeight() * decorationScale / 100);
	}

	private void updateDecorationPreview(MouseEvent event) {
		if (selectedDecoration == null || selectedDecoration.image == null)
			return;
		int x = event.getX() - getSelectedDecorationWidth() / 2;
		int y = event.getY() - getSelectedDecorationHeight() / 2;
		decorationPreviewPosition = new Point(x, y);
		canvas.repaint();
	}

	private void placeSelectedDecoration() {
		if (selectedDecoration == null || selectedDecoration.image == null || decorationPreviewPosition == null)
			return;
		placedDecorations.add(new LevelDecoration(decorationPreviewPosition.x, decorationPreviewPosition.y,
				getSelectedDecorationWidth(), getSelectedDecorationHeight(), selectedDecoration.path,
				selectedDecoration.image));
		decorationPreviewPosition = null;
		unsavedChanges = true;
		canvas.repaint();
	}

	private void clearPlayerMarkers() {
		for (int y = 0; y < mapImage.getHeight(); y++)
			for (int x = 0; x < mapImage.getWidth(); x++) {
				Color pixel = new Color(mapImage.getRGB(x, y));
				if (pixel.getGreen() == 100)
					setPixel(x, y, pixel.getRed(), NO_MARKER, pixel.getBlue());
			}
	}

	private void clearBossMarkers() {
		for (int y = 0; y < mapImage.getHeight(); y++)
			for (int x = 0; x < mapImage.getWidth(); x++) {
				Color pixel = new Color(mapImage.getRGB(x, y));
				if (pixel.getGreen() == 1)
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
					if (SwingUtilities.isLeftMouseButton(event) && selectedTool.kind == ToolKind.DECORATION) {
						updateDecorationPreview(event);
						return;
					}
					if (SwingUtilities.isLeftMouseButton(event))
						paintAt(event, false);
					else if (SwingUtilities.isRightMouseButton(event))
						paintAt(event, true);
				}

				@Override
				public void mouseReleased(MouseEvent event) {
					if (SwingUtilities.isLeftMouseButton(event) && selectedTool.kind == ToolKind.DECORATION)
						placeSelectedDecoration();
				}
			});
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseDragged(MouseEvent event) {
					if (selectedTool.kind == ToolKind.DECORATION
							&& (event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
						updateDecorationPreview(event);
						return;
					}
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
			drawBackground(g);
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
			for (LevelDecoration decoration : placedDecorations)
				g.drawImage(decoration.getImage
			(), decoration.getX(), decoration.getY(), decoration.getWidth(),
						decoration.getHeight(), null);
			if (decorationPreviewPosition != null && selectedDecoration != null && selectedDecoration.image != null) {
				g.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.65f));
				g.drawImage(selectedDecoration.image, decorationPreviewPosition.x, decorationPreviewPosition.y,
						getSelectedDecorationWidth(), getSelectedDecorationHeight(), null);
				g.setComposite(java.awt.AlphaComposite.SrcOver);
			}

			g.setColor(new Color(15, 18, 24, 105));
			for (int x = 0; x <= mapImage.getWidth(); x++)
				g.drawLine(x * TILE_SIZE, 0, x * TILE_SIZE, mapImage.getHeight() * TILE_SIZE);
			for (int y = 0; y <= mapImage.getHeight(); y++)
				g.drawLine(0, y * TILE_SIZE, mapImage.getWidth() * TILE_SIZE, y * TILE_SIZE);
			g.dispose();
		}

		private void drawBackground(Graphics2D g) {
			if (selectedBackground == null || selectedBackground.image == null)
				return;
			int height = mapImage.getHeight() * TILE_SIZE;
			int width = Math.max(1, selectedBackground.image.getWidth() * height
					/ selectedBackground.image.getHeight());
			for (int x = 0; x < mapImage.getWidth() * TILE_SIZE; x += width)
				g.drawImage(selectedBackground.image, x, 0, width, height, null);
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
			} else if (green == 1) {
				label = "B";
				color = new Color(255, 145, 35);
			} else if (green == 2) {
				label = "W";
				color = new Color(165, 105, 65);
			} else if (green == 3) {
				label = "C";
				color = new Color(190, 80, 45);
			} else if (green == 4) {
				label = "E";
				color = new Color(210, 55, 125);
			} else if (green == 5) {
				label = "N";
				color = new Color(130, 90, 45);
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

	private static class BackgroundOption {
		private final String label;
		private final Path path;
		private final BufferedImage image;

		private BackgroundOption(String label, Path path, BufferedImage image) {
			this.label = label;
			this.path = path;
			this.image = image;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	private static class DecorationOption {
		private final String label;
		private final Path path;
		private final BufferedImage thumbnail;
		private BufferedImage image;

		private DecorationOption(String label, Path path, BufferedImage thumbnail) {
			this.label = label;
			this.path = path;
			this.thumbnail = thumbnail;
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

	private static class BackgroundRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			BackgroundOption option = (BackgroundOption) value;
			label.setText(option.label);
			label.setIcon(option.image == null ? null : new javax.swing.ImageIcon(
					option.image.getScaledInstance(86, 48, java.awt.Image.SCALE_FAST)));
			label.setIconTextGap(8);
			label.setPreferredSize(new Dimension(220, 58));
			return label;
		}
	}

	private static class DecorationRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			DecorationOption option = (DecorationOption) value;
			label.setText(option.label);
			label.setIcon(new javax.swing.ImageIcon(option.thumbnail));
			label.setIconTextGap(8);
			label.setPreferredSize(new Dimension(220, 54));
			return label;
		}
	}

	private enum ToolKind {
		TILE, ERASE, MARKER, OBJECT, DECORATION
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

		private static Tool decoration() {
			return new Tool(ToolKind.DECORATION, EMPTY_TILE, null, NO_MARKER, NO_MARKER);
		}
	}
}
