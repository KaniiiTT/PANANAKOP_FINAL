package GameEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
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
	private static final int MIN_MAP_HEIGHT = 1;
	private static final int MAX_MAP_SIZE = 500;
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
	private final BufferedImage menuBackground = loadMenuImage("menu/menu_background.png");
	private final BufferedImage menuPanel = loadMenuImage("menu/panel_main.png");
	private final BufferedImage menuDivider = loadMenuImage("menu/divider.png");
	private final BufferedImage menuLogo = loadMenuImage("menu/title_logo.png");
	private final JFrame frame = new JFrame("Pananakop Map Editor");
	private final Map<String, BufferedImage> previewCache = new TreeMap<>();
	private final List<BackgroundOption> backgroundOptions = new ArrayList<>();
	private final List<LevelDecoration> placedDecorations = new ArrayList<>();
	private final Deque<MapSnapshot> undoHistory = new ArrayDeque<>();
	private final Deque<MapSnapshot> redoHistory = new ArrayDeque<>();
	private final EditorCanvas canvas = new EditorCanvas();
	private final DefaultListModel<SceneEntry> sceneModel = new DefaultListModel<>();
	private final javax.swing.Timer sceneRefreshTimer = new javax.swing.Timer(120,
			event -> refreshSceneHierarchy());
	private JList<SceneEntry> sceneList;
	private JLabel sceneInspector;
	private SceneEntry selectedSceneEntry;
	private int sceneDragOffsetX;
	private int sceneDragOffsetY;
	private boolean sceneObjectMoved;
	private double zoom = 1.0;
	private boolean showGrid = true;
	private JLabel zoomLabel;
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
	private MapSnapshot savedSnapshot;
	private boolean restoringSnapshot;

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
		sceneRefreshTimer.setRepeats(false);
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
		JPanel root = new JPanel(new BorderLayout(10, 10)) {
			private static final long serialVersionUID = 1L;
			@Override protected void paintComponent(Graphics graphics) {
				super.paintComponent(graphics);
				if (menuBackground != null)
					graphics.drawImage(menuBackground, 0, 0, getWidth(), getHeight(), null);
				else {
					graphics.setColor(new Color(29, 37, 53));
					graphics.fillRect(0, 0, getWidth(), getHeight());
				}
			}
		};
		root.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		root.setOpaque(false);
		root.add(createToolbar(), BorderLayout.NORTH);
		root.add(createPalette(), BorderLayout.WEST);
		JScrollPane mapScroll = new JScrollPane(canvas);
		mapScroll.setBorder(BorderFactory.createLineBorder(new Color(31, 42, 61), 3));
		mapScroll.getViewport().setBackground(new Color(22, 29, 42));
		mapScroll.setBackground(new Color(22, 29, 42));
		JSplitPane workspace = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapScroll, createScenePanel());
		workspace.setResizeWeight(1.0);
		workspace.setDividerLocation(0.78);
		workspace.setBorder(null);
		root.add(workspace, BorderLayout.CENTER);
		status.setForeground(new Color(247, 240, 222));
		status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
		status.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
		root.add(status, BorderLayout.SOUTH);
		frame.setContentPane(root);
		installKeyboardShortcuts();
		frame.setMinimumSize(new Dimension(1000, 650));
		frame.setSize(1380, 850);
		frame.setLocationRelativeTo(null);
	}

	private JPanel createToolbar() {
		JPanel toolbar = new JPanel(new BorderLayout(8, 5));
		toolbar.setOpaque(false);
		JPanel header = new JPanel(new BorderLayout(12, 0));
		header.setOpaque(false);
		if (menuLogo != null) {
			JLabel logo = new JLabel(new javax.swing.ImageIcon(menuLogo.getScaledInstance(210, 66,
					java.awt.Image.SCALE_SMOOTH)));
			logo.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 8));
			header.add(logo, BorderLayout.WEST);
		}
		JLabel editorTitle = new JLabel("WORLD BUILDER");
		editorTitle.setForeground(new Color(30, 43, 64));
		editorTitle.setFont(new Font(Font.SERIF, Font.BOLD, 20));
		header.add(editorTitle, BorderLayout.CENTER);
		JPanel stageRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
		stageRow.setOpaque(false);
		stageRow.add(new JLabel("STAGE"));
		stageRow.add(currentMapLabel);
		stageRow.add(stagePicker = new JComboBox<>(STAGES));
		stagePicker.addActionListener(event -> {
			if (!updatingStagePicker)
				selectStage((StageOption) stagePicker.getSelectedItem());
		});
		syncStagePicker();
		header.add(stageRow, BorderLayout.EAST);
		toolbar.add(header, BorderLayout.NORTH);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		actions.setOpaque(false);
		JButton newButton = new JButton("New map");
		JButton selectButton = new JButton("Select");
		JButton resizeButton = new JButton("Resize map");
		JButton undoButton = new JButton("Undo");
		JButton redoButton = new JButton("Redo");
		JButton resetButton = new JButton("Reset map");
		JButton openButton = new JButton("Open PNG");
		JButton saveButton = new JButton("Save");
		JButton saveAsButton = new JButton("Save as...");
		JButton testButton = new JButton("Test map");
		JButton arenaPreviewButton = new JButton("Boss arena view");
		JButton buildArenaButton = new JButton("Build boss arena");

		newButton.addActionListener(event -> createNewMap());
		resizeButton.addActionListener(event -> resizeCurrentMap());
		undoButton.addActionListener(event -> undo());
		redoButton.addActionListener(event -> redo());
		resetButton.addActionListener(event -> resetMap());
		openButton.addActionListener(event -> openMap());
		saveButton.addActionListener(event -> saveMap(currentMapPath));
		saveAsButton.addActionListener(event -> saveMapAs());
		testButton.setToolTipText("Play the current map, including unsaved edits.");
		testButton.addActionListener(event -> testCurrentMap());
		arenaPreviewButton.addActionListener(event -> showBossArenaPreview());
		buildArenaButton.addActionListener(event -> buildBossArena());

		selectButton.setToolTipText("Select and move objects in the viewport.");
		selectButton.addActionListener(event -> {
			selectedTool = Tool.select();
			status.setText("Select mode: click an object, then drag to move it.");
		});
		for (JButton button : new JButton[] { selectButton, newButton, resizeButton, undoButton, redoButton, resetButton,
				openButton, saveButton, saveAsButton, testButton, arenaPreviewButton, buildArenaButton })
			styleMenuButton(button);
		for (JButton button : new JButton[] { selectButton, newButton, resizeButton, undoButton, redoButton, resetButton,
				openButton, saveButton, saveAsButton, testButton, arenaPreviewButton, buildArenaButton })
			actions.add(button);
		styleMenuControl(stagePicker);
		styleMenuControl(currentMapLabel);
		JCheckBox gridToggle = new JCheckBox("Grid", showGrid);
		gridToggle.setOpaque(false);
		gridToggle.setForeground(new Color(34, 47, 69));
		gridToggle.addActionListener(event -> {
			showGrid = gridToggle.isSelected();
			canvas.repaint();
		});
		stageRow.add(gridToggle);
		JButton zoomOut = new JButton("−");
		JButton zoomIn = new JButton("+");
		zoomOut.addActionListener(event -> setZoom(zoom - 0.1));
		zoomIn.addActionListener(event -> setZoom(zoom + 0.1));
		styleMenuButton(zoomOut);
		styleMenuButton(zoomIn);
		zoomLabel = new JLabel("100%");
		styleMenuControl(zoomLabel);
		stageRow.add(zoomOut);
		stageRow.add(zoomLabel);
		stageRow.add(zoomIn);
		toolbar.add(actions, BorderLayout.SOUTH);
		toolbar.setBorder(BorderFactory.createEmptyBorder(3, 4, 7, 4));
		return toolbar;
	}

	private static BufferedImage loadMenuImage(String path) {
		try {
			return LoadSave.GetSpriteAtlas(path);
		} catch (RuntimeException exception) {
			return null;
		}
	}

	private static void styleMenuButton(JButton button) {
		button.setFont(new Font(Font.SERIF, Font.BOLD, 13));
		button.setForeground(new Color(36, 48, 68));
		button.setBackground(new Color(244, 238, 221));
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(78, 91, 112), 1),
				BorderFactory.createEmptyBorder(6, 9, 6, 9)));
		button.setOpaque(true);
	}

	private static void styleMenuControl(JLabel label) {
		label.setForeground(new Color(34, 47, 69));
		label.setFont(new Font(Font.SERIF, Font.BOLD, 13));
	}

	private static void styleMenuControl(JComboBox<?> combo) {
		combo.setFont(new Font(Font.SERIF, Font.BOLD, 13));
		combo.setForeground(new Color(36, 48, 68));
		combo.setBackground(new Color(244, 238, 221));
	}

	private JPanel createScenePanel() {
		JPanel panel = new JPanel(new BorderLayout(6, 8));
		panel.setPreferredSize(new Dimension(245, 0));
		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(39, 51, 72), 2),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		panel.setBackground(new Color(239, 233, 216));
		JLabel title = new JLabel("HIERARCHY");
		title.setFont(new Font(Font.SERIF, Font.BOLD, 15));
		title.setForeground(new Color(36, 48, 68));
		panel.add(title, BorderLayout.NORTH);
		sceneList = new JList<>(sceneModel);
		sceneList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sceneList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		sceneList.setBackground(new Color(250, 247, 237));
		sceneList.addListSelectionListener(event -> {
			if (event.getValueIsAdjusting())
				return;
			selectedSceneEntry = sceneList.getSelectedValue();
			updateSceneInspector();
			canvas.repaint();
		});
		JButton deleteButton = new JButton("Delete selected");
		styleMenuButton(deleteButton);
		deleteButton.addActionListener(event -> deleteSelectedSceneObject());
		sceneInspector = new JLabel("<html><b>Inspector</b><br>Select an object to view its position.</html>");
		sceneInspector.setVerticalAlignment(JLabel.TOP);
		sceneInspector.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(170, 158, 132)),
				BorderFactory.createEmptyBorder(9, 2, 6, 2)));
		JPanel lower = new JPanel(new BorderLayout(4, 8));
		lower.setOpaque(false);
		lower.add(sceneInspector, BorderLayout.CENTER);
		lower.add(deleteButton, BorderLayout.SOUTH);
		panel.add(new JScrollPane(sceneList), BorderLayout.CENTER);
		panel.add(lower, BorderLayout.SOUTH);
		return panel;
	}

	private void refreshSceneHierarchy() {
		if (sceneModel == null)
			return;
		SceneEntry previous = selectedSceneEntry;
		sceneModel.clear();
		if (mapImage == null)
			return;
		String[] markerNames = { "Crabby", "Magellan", "Boar", "Conquistador", "Espinosa", "Monkey", "Gorilla" };
		String[] objectNames = { "Red potion", "Blue potion", "Barrel", "Crate", "Spike", "Cannon left", "Cannon right" };
		for (int y = 0; y < mapImage.getHeight(); y++)
			for (int x = 0; x < mapImage.getWidth(); x++) {
				Color pixel = new Color(mapImage.getRGB(x, y));
				int green = pixel.getGreen();
				if (green == 100)
					sceneModel.addElement(new SceneEntry("Player Start", x, y, 1, null));
				else if (green >= 0 && green < markerNames.length)
					sceneModel.addElement(new SceneEntry(markerNames[green] + "  •  " + x + ", " + y, x, y, 1, null));
				int blue = pixel.getBlue();
				if (blue >= 0 && blue < objectNames.length)
					sceneModel.addElement(new SceneEntry(objectNames[blue] + "  •  " + x + ", " + y, x, y, 2, null));
				else if (Trap.getType(blue).isPresent())
					sceneModel.addElement(new SceneEntry(Trap.getType(blue).get().label + "  •  " + x + ", " + y,
							x, y, 2, null));
			}
		for (LevelDecoration decoration : placedDecorations) {
			Path asset = decoration.getAssetPath();
			String name = asset == null ? "Decoration" : asset.getFileName().toString();
			sceneModel.addElement(new SceneEntry(name + "  •  " + decoration.getX() + ", " + decoration.getY(),
					decoration.getX(), decoration.getY(), 3, decoration));
		}
		if (sceneList != null && previous != null) {
			for (int i = 0; i < sceneModel.size(); i++) {
				SceneEntry entry = sceneModel.get(i);
				if (entry.sameObject(previous)) {
					sceneList.setSelectedIndex(i);
					return;
				}
			}
			sceneList.clearSelection();
		}
		updateSceneInspector();
	}

	private void updateSceneInspector() {
		if (sceneInspector == null)
			return;
		if (selectedSceneEntry == null) {
			sceneInspector.setText("<html><b>Inspector</b><br>Select an object to view its position.</html>");
			return;
		}
		String position = selectedSceneEntry.channel == 3
				? selectedSceneEntry.x + ", " + selectedSceneEntry.y + " px"
				: selectedSceneEntry.x + ", " + selectedSceneEntry.y + " tiles";
		sceneInspector.setText("<html><b>Inspector</b><br>" + selectedSceneEntry.label + "<br>Position: "
				+ position + "</html>");
	}

	private void deleteSelectedSceneObject() {
		if (selectedSceneEntry == null)
			return;
		recordHistory();
		if (selectedSceneEntry.channel == 3) {
			placedDecorations.remove(selectedSceneEntry.decoration);
		} else {
			Color pixel = new Color(mapImage.getRGB(selectedSceneEntry.x, selectedSceneEntry.y));
			if (selectedSceneEntry.channel == 1)
				setPixel(selectedSceneEntry.x, selectedSceneEntry.y, pixel.getRed(), NO_MARKER, pixel.getBlue());
			else
				setPixel(selectedSceneEntry.x, selectedSceneEntry.y, pixel.getRed(), pixel.getGreen(), NO_MARKER);
		}
		selectedSceneEntry = null;
		unsavedChanges = true;
		refreshSceneHierarchy();
		canvas.repaint();
		status.setText("Deleted selected scene object.");
	}

	private void selectSceneObjectAt(MouseEvent event) {
		int px = (int) (event.getX() / zoom);
		int py = (int) (event.getY() / zoom);
		SceneEntry found = null;
		for (int i = sceneModel.size() - 1; i >= 0; i--) {
			SceneEntry candidate = sceneModel.get(i);
			if (candidate.channel == 3) {
				LevelDecoration decoration = candidate.decoration;
				if (px >= decoration.getX() && px < decoration.getX() + decoration.getWidth()
						&& py >= decoration.getY() && py < decoration.getY() + decoration.getHeight()) {
					found = candidate;
					break;
				}
			} else if (candidate.x == px / TILE_SIZE && candidate.y == py / TILE_SIZE) {
				found = candidate;
				break;
			}
		}
		selectedSceneEntry = found;
		if (sceneList != null && found != null)
			sceneList.setSelectedValue(found, true);
		else if (sceneList != null)
			sceneList.clearSelection();
		updateSceneInspector();
		canvas.repaint();
		if (found == null) {
			sceneObjectMoved = false;
			return;
		}
		if (found.channel == 3) {
			sceneDragOffsetX = px - found.x;
			sceneDragOffsetY = py - found.y;
		} else {
			sceneDragOffsetX = px / TILE_SIZE - found.x;
			sceneDragOffsetY = py / TILE_SIZE - found.y;
		}
		sceneObjectMoved = false;
	}

	private void moveSelectedSceneObject(MouseEvent event) {
		if (selectedSceneEntry == null)
			return;
		int px = (int) (event.getX() / zoom);
		int py = (int) (event.getY() / zoom);
		SceneEntry current = selectedSceneEntry;
		int targetX;
		int targetY;
		if (current.channel == 3) {
			LevelDecoration old = current.decoration;
			targetX = Math.max(0, Math.min(mapImage.getWidth() * TILE_SIZE - old.getWidth(), px - sceneDragOffsetX));
			targetY = Math.max(0, Math.min(mapImage.getHeight() * TILE_SIZE - old.getHeight(), py - sceneDragOffsetY));
			if (targetX == old.getX() && targetY == old.getY())
				return;
			if (!sceneObjectMoved) {
				recordHistory();
				sceneObjectMoved = true;
			}
			LevelDecoration moved = new LevelDecoration(targetX, targetY, old.getWidth(), old.getHeight(),
					old.getAssetPath(), old.getImage());
			int index = placedDecorations.indexOf(old);
			placedDecorations.set(index, moved);
			String name = current.label.substring(0, current.label.lastIndexOf("  •  "));
			selectedSceneEntry = new SceneEntry(name + "  •  " + targetX + ", " + targetY,
					targetX, targetY, 3, moved);
		} else {
			targetX = Math.max(0, Math.min(mapImage.getWidth() - 1, px / TILE_SIZE - sceneDragOffsetX));
			targetY = Math.max(0, Math.min(mapImage.getHeight() - 1, py / TILE_SIZE - sceneDragOffsetY));
			if (targetX == current.x && targetY == current.y)
				return;
			Color target = new Color(mapImage.getRGB(targetX, targetY));
			if ((current.channel == 1 && target.getGreen() != NO_MARKER)
					|| (current.channel == 2 && target.getBlue() != NO_MARKER))
				return;
			if (!sceneObjectMoved) {
				recordHistory();
				sceneObjectMoved = true;
			}
			Color source = new Color(mapImage.getRGB(current.x, current.y));
			String name = current.label.substring(0, current.label.lastIndexOf("  •  "));
			if (current.channel == 1) {
				setPixel(current.x, current.y, source.getRed(), NO_MARKER, source.getBlue());
				setPixel(targetX, targetY, target.getRed(), source.getGreen(), target.getBlue());
			} else {
				setPixel(current.x, current.y, source.getRed(), source.getGreen(), NO_MARKER);
				setPixel(targetX, targetY, target.getRed(), target.getGreen(), source.getBlue());
			}
			selectedSceneEntry = new SceneEntry(name + "  •  " + targetX + ", " + targetY,
					targetX, targetY, current.channel, null);
		}
		unsavedChanges = true;
		updateSceneInspector();
		canvas.repaint();
	}

	private void installKeyboardShortcuts() {
		javax.swing.JComponent root = frame.getRootPane();
		root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
		root.getActionMap().put("undo", new javax.swing.AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(java.awt.event.ActionEvent event) { undo(); }
		});
		root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
		root.getActionMap().put("redo", new javax.swing.AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(java.awt.event.ActionEvent event) { redo(); }
		});
		root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
		root.getActionMap().put("save", new javax.swing.AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(java.awt.event.ActionEvent event) { saveMap(currentMapPath); }
		});
		root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "open");
		root.getActionMap().put("open", new javax.swing.AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(java.awt.event.ActionEvent event) { openMap(); }
		});
		root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelection");
		root.getActionMap().put("deleteSelection", new javax.swing.AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(java.awt.event.ActionEvent event) { deleteSelectedSceneObject(); }
		});
	}

	private void setZoom(double value) {
		zoom = Math.max(0.5, Math.min(2.0, Math.round(value * 10.0) / 10.0));
		if (zoomLabel != null)
			zoomLabel.setText((int) Math.round(zoom * 100) + "%");
		updateCanvas();
	}

	private void scheduleSceneHierarchyRefresh() {
		sceneRefreshTimer.restart();
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
		savedSnapshot = null;
		undoHistory.clear();
		redoHistory.clear();
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
		recordHistory();

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
		JPanel palette = new JPanel(new BorderLayout(4, 6)) {
			private static final long serialVersionUID = 1L;
			@Override protected void paintComponent(Graphics graphics) {
				super.paintComponent(graphics);
				if (menuPanel != null)
					graphics.drawImage(menuPanel, 0, 0, getWidth(), getHeight(), null);
				else {
					graphics.setColor(new Color(241, 235, 217));
					graphics.fillRect(0, 0, getWidth(), getHeight());
				}
			}
		};
		palette.setPreferredSize(new Dimension(260, 0));
		palette.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		JPanel paletteHeading = new JPanel(new BorderLayout(0, 3));
		paletteHeading.setOpaque(false);
		JLabel heading = new JLabel("ASSET LIBRARY");
		heading.setFont(new Font(Font.SERIF, Font.BOLD, 16));
		heading.setForeground(new Color(36, 48, 68));
		paletteHeading.add(heading, BorderLayout.NORTH);
		if (menuDivider != null)
			paletteHeading.add(new JLabel(new javax.swing.ImageIcon(menuDivider.getScaledInstance(220, 14,
					java.awt.Image.SCALE_SMOOTH))), BorderLayout.SOUTH);
		palette.add(paletteHeading, BorderLayout.NORTH);
		materialPicker = new JComboBox<>();
		materialPicker.addItem("Classic tiles");
		materialPicker.addItem("Platformer");
		materialPicker.addItem("Ground");
		materialPicker.addItem("Ground Beach");
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
		tabs.setFont(new Font(Font.SERIF, Font.BOLD, 12));
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
			recordHistory();
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
		addToolButton(buttons, "Gorilla miniboss", Tool.marker(6, NO_MARKER), "Gorilla miniboss");
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
								if (image == null || !hasVisiblePixel(image))
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

	private boolean hasVisiblePixel(BufferedImage image) {
		if (!image.getColorModel().hasAlpha())
			return true;
		for (int y = 0; y < image.getHeight(); y++)
			for (int x = 0; x < image.getWidth(); x++)
				if ((image.getRGB(x, y) >>> 24) != 0)
					return true;
		return false;
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
		JPanel panel = new JPanel(new java.awt.GridLayout(2, 2, 8, 8));
		JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(50, Game.TILES_IN_WIDTH, MAX_MAP_SIZE, 1));
		JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(Game.TILES_IN_HEIGHT, MIN_MAP_HEIGHT,
				MAX_MAP_SIZE, 1));
		panel.add(new JLabel("Width in tiles:"));
		panel.add(widthSpinner);
		panel.add(new JLabel("Height in tiles:"));
		panel.add(heightSpinner);
		int answer = JOptionPane.showConfirmDialog(frame, panel, "New map",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (answer != JOptionPane.OK_OPTION)
			return;

		try {
			int width = (Integer) widthSpinner.getValue();
			int height = (Integer) heightSpinner.getValue();
			int nextLevel = nextLevelNumber();
			Path newMapPath = levelsDirectory.resolve(nextLevel + ".png");
			if (Files.exists(newMapPath)) {
				int replaceAnswer = JOptionPane.showConfirmDialog(frame,
						newMapPath.getFileName() + " already exists. Replace it?",
						"Confirm replace", JOptionPane.YES_NO_OPTION);
				if (replaceAnswer != JOptionPane.YES_OPTION)
					return;
			}
			mapImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			modernTilePaths = new String[height][width];
			placedDecorations.clear();
			for (int y = 0; y < mapImage.getHeight(); y++)
				for (int x = 0; x < mapImage.getWidth(); x++)
					setPixel(x, y, EMPTY_TILE, NO_MARKER, NO_MARKER);
			selectedBackground = null;
			currentMapPath = newMapPath;
			unsavedChanges = true;
			savedSnapshot = null;
			undoHistory.clear();
			redoHistory.clear();
			syncStagePicker();
			syncBackgroundSelection();
			updateCanvas();
			status.setText("New " + width + " x " + height + " map.");
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(frame, "Map width must be from " + Game.TILES_IN_WIDTH
					+ " to " + MAX_MAP_SIZE + " and height must be from " + MIN_MAP_HEIGHT + " to "
					+ MAX_MAP_SIZE + ".",
					"Invalid map size", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void resizeCurrentMap() {
		if (mapImage == null)
			return;

		JPanel panel = new JPanel(new BorderLayout(8, 8));
		JPanel sizePanel = new JPanel(new java.awt.GridLayout(2, 2, 8, 8));
		JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(mapImage.getWidth(),
				Game.TILES_IN_WIDTH, MAX_MAP_SIZE, 1));
		JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(mapImage.getHeight(),
				MIN_MAP_HEIGHT, MAX_MAP_SIZE, 1));
		sizePanel.add(new JLabel("Width in tiles:"));
		sizePanel.add(widthSpinner);
		sizePanel.add(new JLabel("Height in tiles:"));
		sizePanel.add(heightSpinner);
		panel.add(sizePanel, BorderLayout.CENTER);

		int answer = JOptionPane.showConfirmDialog(frame, panel, "Resize map",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (answer != JOptionPane.OK_OPTION)
			return;

		int newWidth = (Integer) widthSpinner.getValue();
		int newHeight = (Integer) heightSpinner.getValue();
		if (newWidth == mapImage.getWidth() && newHeight == mapImage.getHeight())
			return;
		if (newWidth < mapImage.getWidth() || newHeight < mapImage.getHeight()) {
			int removedColumns = Math.max(0, mapImage.getWidth() - newWidth);
			int removedRows = Math.max(0, mapImage.getHeight() - newHeight);
			int confirm = JOptionPane.showConfirmDialog(frame,
					"This will remove " + removedColumns + " column" + (removedColumns == 1 ? "" : "s")
							+ " and " + removedRows + " row" + (removedRows == 1 ? "" : "s")
							+ " from the top/right edges, including anything placed there. Continue?",
					"Confirm resize", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (confirm != JOptionPane.YES_OPTION)
				return;
		}
		recordHistory();

		BufferedImage oldImage = mapImage;
		String[][] oldModernTilePaths = modernTilePaths;
		int oldWidth = oldImage.getWidth();
		int oldHeight = oldImage.getHeight();
		int verticalOffset = newHeight - oldHeight;
		mapImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		modernTilePaths = new String[newHeight][newWidth];
		for (int y = 0; y < mapImage.getHeight(); y++) {
			for (int x = 0; x < newWidth; x++) {
				int oldY = y - verticalOffset;
				if (x < oldWidth && oldY >= 0 && oldY < oldHeight) {
					mapImage.setRGB(x, y, oldImage.getRGB(x, oldY));
					modernTilePaths[y][x] = oldModernTilePaths[oldY][x];
				} else {
					setPixel(x, y, EMPTY_TILE, NO_MARKER, NO_MARKER);
				}
			}
		}

		List<LevelDecoration> resizedDecorations = new ArrayList<>();
		int newPixelWidth = newWidth * TILE_SIZE;
		int newPixelHeight = newHeight * TILE_SIZE;
		for (LevelDecoration decoration : placedDecorations) {
			int decorationY = decoration.getY() + verticalOffset * TILE_SIZE;
			if (decoration.getX() >= newPixelWidth || decorationY >= newPixelHeight)
				continue;
			int clippedWidth = Math.min(decoration.getWidth(), newPixelWidth - decoration.getX());
			int clippedTop = Math.max(0, -decorationY);
			int clippedHeight = Math.min(decoration.getHeight() - clippedTop,
					newPixelHeight - Math.max(0, decorationY));
			if (clippedWidth > 0 && clippedHeight > 0)
				resizedDecorations.add(new LevelDecoration(decoration.getX(), Math.max(0, decorationY), clippedWidth,
					clippedHeight, decoration.getAssetPath(), decoration.getImage()));
		}
		placedDecorations.clear();
		placedDecorations.addAll(resizedDecorations);
		unsavedChanges = true;
		updateCanvas();
		status.setText("Resized map to " + newWidth + " × " + mapImage.getHeight() + " tiles.");
	}

	private void undo() {
		if (undoHistory.isEmpty()) {
			status.setText("Nothing to undo.");
			return;
		}
		redoHistory.push(captureSnapshot());
		restoreSnapshot(undoHistory.pop());
		unsavedChanges = true;
		status.setText("Undid last map edit.");
	}

	private void redo() {
		if (redoHistory.isEmpty()) {
			status.setText("Nothing to redo.");
			return;
		}
		undoHistory.push(captureSnapshot());
		restoreSnapshot(redoHistory.pop());
		unsavedChanges = true;
		status.setText("Redid last map edit.");
	}

	private void resetMap() {
		if (savedSnapshot == null)
			return;
		if (!unsavedChanges) {
			status.setText("Map is already reset.");
			return;
		}
		int answer = JOptionPane.showConfirmDialog(frame,
				"Discard all unsaved edits and restore the last saved map?",
				"Reset map", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (answer != JOptionPane.YES_OPTION)
			return;
		restoreSnapshot(savedSnapshot);
		undoHistory.clear();
		redoHistory.clear();
		unsavedChanges = false;
		status.setText("Map reset to the last saved version.");
	}

	private void recordHistory() {
		if (restoringSnapshot || mapImage == null)
			return;
		undoHistory.push(captureSnapshot());
		redoHistory.clear();
	}

	private MapSnapshot captureSnapshot() {
		BufferedImage image = new BufferedImage(mapImage.getWidth(), mapImage.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.drawImage(mapImage, 0, 0, null);
		graphics.dispose();
		String[][] paths = new String[modernTilePaths.length][];
		for (int y = 0; y < modernTilePaths.length; y++)
			paths[y] = modernTilePaths[y].clone();
		return new MapSnapshot(image, paths, new ArrayList<>(placedDecorations), selectedBackground);
	}

	private void restoreSnapshot(MapSnapshot snapshot) {
		restoringSnapshot = true;
		try {
			mapImage = copyImage(snapshot.image);
			modernTilePaths = new String[snapshot.modernTilePaths.length][];
			for (int y = 0; y < snapshot.modernTilePaths.length; y++)
				modernTilePaths[y] = snapshot.modernTilePaths[y].clone();
			placedDecorations.clear();
			placedDecorations.addAll(snapshot.decorations);
			selectedBackground = snapshot.background;
			syncBackgroundSelection();
			updateCanvas();
		} finally {
			restoringSnapshot = false;
		}
	}

	private BufferedImage copyImage(BufferedImage source) {
		BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = copy.createGraphics();
		graphics.drawImage(source, 0, 0, null);
		graphics.dispose();
		return copy;
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
			if (loadedImage.getWidth() < Game.TILES_IN_WIDTH || loadedImage.getWidth() > MAX_MAP_SIZE
					|| loadedImage.getHeight() < MIN_MAP_HEIGHT || loadedImage.getHeight() > MAX_MAP_SIZE)
				throw new IOException("Level maps must be " + Game.TILES_IN_WIDTH + "-" + MAX_MAP_SIZE
						+ " tiles wide and " + MIN_MAP_HEIGHT + "-" + MAX_MAP_SIZE + " tiles high.");

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
			savedSnapshot = captureSnapshot();
			undoHistory.clear();
			redoHistory.clear();
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
			savedSnapshot = captureSnapshot();
			undoHistory.clear();
			redoHistory.clear();
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
		recordHistory();
		if (erase) {
			if (removeDecorationAt(x * TILE_SIZE, y * TILE_SIZE)) {
				unsavedChanges = true;
				refreshSceneHierarchy();
				canvas.repaint();
				return;
			}
			clearCell(x, y);
			unsavedChanges = true;
			refreshSceneHierarchy();
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
		case SELECT:
			break;
		}
		unsavedChanges = true;
		if (selectedTool.kind == ToolKind.MARKER || selectedTool.kind == ToolKind.OBJECT
				|| selectedTool.kind == ToolKind.ERASE)
			scheduleSceneHierarchyRefresh();
		canvas.repaint();
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
		int x = (int) (event.getX() / zoom) - getSelectedDecorationWidth() / 2;
		int y = (int) (event.getY() / zoom) - getSelectedDecorationHeight() / 2;
		decorationPreviewPosition = new Point(x, y);
		canvas.repaint();
	}

	private void placeSelectedDecoration() {
		if (selectedDecoration == null || selectedDecoration.image == null || decorationPreviewPosition == null)
			return;
		recordHistory();
		placedDecorations.add(new LevelDecoration(decorationPreviewPosition.x, decorationPreviewPosition.y,
				getSelectedDecorationWidth(), getSelectedDecorationHeight(), selectedDecoration.path,
				selectedDecoration.image));
		decorationPreviewPosition = null;
		unsavedChanges = true;
		refreshSceneHierarchy();
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
		canvas.setPreferredSize(new Dimension((int) Math.ceil(mapImage.getWidth() * TILE_SIZE * zoom),
				(int) Math.ceil(mapImage.getHeight() * TILE_SIZE * zoom)));
		canvas.revalidate();
		canvas.repaint();
		refreshSceneHierarchy();
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
					if (SwingUtilities.isLeftMouseButton(event) && selectedTool.kind == ToolKind.SELECT) {
						selectSceneObjectAt(event);
						return;
					}
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
					if (SwingUtilities.isLeftMouseButton(event) && selectedTool.kind == ToolKind.SELECT) {
						if (sceneObjectMoved)
							refreshSceneHierarchy();
						sceneObjectMoved = false;
						return;
					}
					if (SwingUtilities.isLeftMouseButton(event) && selectedTool.kind == ToolKind.DECORATION)
						placeSelectedDecoration();
				}
			});
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseDragged(MouseEvent event) {
					if (selectedTool.kind == ToolKind.SELECT
							&& (event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
						moveSelectedSceneObject(event);
						return;
					}
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
			addMouseWheelListener(event -> {
				if (event.isControlDown()) {
					setZoom(zoom - event.getPreciseWheelRotation() * 0.1);
					event.consume();
				}
			});
		}

		private void paintAt(MouseEvent event, boolean erase) {
			paintCell((int) (event.getX() / (TILE_SIZE * zoom)),
					(int) (event.getY() / (TILE_SIZE * zoom)), erase);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			if (mapImage == null)
				return;
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.scale(zoom, zoom);
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

			if (showGrid) {
				g.setColor(new Color(15, 18, 24, 105));
				for (int x = 0; x <= mapImage.getWidth(); x++)
					g.drawLine(x * TILE_SIZE, 0, x * TILE_SIZE, mapImage.getHeight() * TILE_SIZE);
				for (int y = 0; y <= mapImage.getHeight(); y++)
					g.drawLine(0, y * TILE_SIZE, mapImage.getWidth() * TILE_SIZE, y * TILE_SIZE);
			}
			if (selectedSceneEntry != null) {
				g.setColor(new Color(255, 202, 66));
				g.setStroke(new java.awt.BasicStroke(2));
				if (selectedSceneEntry.channel == 3) {
					LevelDecoration decoration = selectedSceneEntry.decoration;
					g.drawRect(decoration.getX(), decoration.getY(), decoration.getWidth(), decoration.getHeight());
				} else {
					g.drawRect(selectedSceneEntry.x * TILE_SIZE, selectedSceneEntry.y * TILE_SIZE,
							TILE_SIZE, TILE_SIZE);
				}
			}
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
			} else if (green == 6) {
				label = "G";
				color = new Color(95, 95, 95);
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

	private static class SceneEntry {
		private final String label;
		private final int x;
		private final int y;
		private final int channel;
		private final LevelDecoration decoration;

		private SceneEntry(String label, int x, int y, int channel, LevelDecoration decoration) {
			this.label = label;
			this.x = x;
			this.y = y;
			this.channel = channel;
			this.decoration = decoration;
		}

		private boolean sameObject(SceneEntry other) {
			return channel == other.channel && x == other.x && y == other.y
					&& (decoration == null ? other.decoration == null : decoration == other.decoration);
		}

		@Override public String toString() { return label; }
	}

	private static class MapSnapshot {
		private final BufferedImage image;
		private final String[][] modernTilePaths;
		private final List<LevelDecoration> decorations;
		private final BackgroundOption background;

		private MapSnapshot(BufferedImage image, String[][] modernTilePaths,
				List<LevelDecoration> decorations, BackgroundOption background) {
			this.image = image;
			this.modernTilePaths = modernTilePaths;
			this.decorations = decorations;
			this.background = background;
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
		TILE, ERASE, MARKER, OBJECT, DECORATION, SELECT
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

		private static Tool select() {
			return new Tool(ToolKind.SELECT, EMPTY_TILE, null, NO_MARKER, NO_MARKER);
		}
	}
}
