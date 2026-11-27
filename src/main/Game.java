package main;

import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.prefs.Preferences;

import gamestates.GameOptions;
import gamestates.Gamestate;
import gamestates.Menu;
import gamestates.Playing;
import gamestates.CharacterSelect;
import gamestates.LevelSelect;
import levels.Level;
import ui.AudioOptions;
import utilz.SoundManager;

public class Game implements Runnable {
	private static final Preferences PROGRESS = Preferences.userNodeForPackage(Game.class);
	private static final String UNLOCKED_LEVELS_KEY = "unlockedLevelCount";

	private GamePanel gamePanel;
	private Thread gameThread;
	private final int FPS_SET = 120;
	private final int UPS_SET = 200;

	private Playing playing;
	private Menu menu;
	private GameOptions gameOptions;
	private CharacterSelect characterSelect;
	private LevelSelect levelSelect;
	private AudioOptions audioOptions;
	private int selectedLevelIndex;
	private int unlockedLevelCount = 1;
	private GameWindow gameWindow;
	private volatile boolean running = true;
	private final boolean testMode;

	public final static int TILES_DEFAULT_SIZE = 32;
	public final static float SCALE = calculateScale();
	public final static int TILES_IN_WIDTH = 26;
	public final static int TILES_IN_HEIGHT = 14;
	public final static int TILES_SIZE = (int) (TILES_DEFAULT_SIZE * SCALE);
	public final static int GAME_WIDTH = TILES_SIZE * TILES_IN_WIDTH;
	public final static int GAME_HEIGHT = TILES_SIZE * TILES_IN_HEIGHT;

	private static float calculateScale() {
		// Reserve room for the operating-system window frame and taskbar so the
		// complete game area fits on smaller laptop displays.
		Rectangle usableScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		float widthScale = (usableScreen.width - 40f) / (TILES_DEFAULT_SIZE * TILES_IN_WIDTH);
		float heightScale = (usableScreen.height - 80f) / (TILES_DEFAULT_SIZE * TILES_IN_HEIGHT);
		return Math.max(0.5f, Math.min(2f, Math.min(widthScale, heightScale)));
	}

	public Game() {
		this(null, null);
	}

	public Game(Level testLevel, Runnable onTestClosed) {
		testMode = testLevel != null;
		initClasses(testLevel);

		gamePanel = new GamePanel(this);
		gameWindow = new GameWindow(gamePanel, testMode, onTestClosed);
		gamePanel.setFocusable(true);
		gamePanel.requestFocus();
		if (testLevel != null)
			Gamestate.state = Gamestate.PLAYING;

		startGameLoop();
	}

	private void initClasses(Level testLevel) {
		audioOptions = new AudioOptions();
		menu = new Menu(this);
		playing = new Playing(this, testLevel);
		unlockedLevelCount = Math.min(playing.getLevelManager().getAmountOfLevels(),
				Math.max(3, PROGRESS.getInt(UNLOCKED_LEVELS_KEY, 3)));
		gameOptions = new GameOptions(this);
		characterSelect = new CharacterSelect(this);
		levelSelect = new LevelSelect(this);
	}

	private void startGameLoop() {
		gameThread = new Thread(this);
		gameThread.start();
	}

	public void update() {
		update(1f / UPS_SET);
	}

	public void update(float dt) {
		switch (Gamestate.state) {
		case MENU:
			SoundManager.musicMenu();
			menu.update();
			break;
		case PLAYING:
			playing.update(dt);
			break;
		case CHARACTER_SELECT:
			SoundManager.musicMenu();
			characterSelect.update();
			break;
		case LEVEL_SELECT:
			SoundManager.musicMenu();
			levelSelect.update();
			break;
		case OPTIONS:
			SoundManager.musicMenu();
			gameOptions.update();
			break;
		case QUIT:
		default:
			SoundManager.shutdown();
			if (testMode)
				gameWindow.close();
			else
				System.exit(0);
			break;

		}
	}

	public void render(Graphics g) {
		switch (Gamestate.state) {
		case MENU:
			menu.draw(g);
			break;
		case PLAYING:
			playing.draw(g);
			break;
		case CHARACTER_SELECT:
			characterSelect.draw(g);
			break;
		case LEVEL_SELECT:
			levelSelect.draw(g);
			break;
		case OPTIONS:
			gameOptions.draw(g);
			break;
		default:
			break;
		}
	}

	@Override
	public void run() {

		double timePerFrame = 1000000000.0 / FPS_SET;
		double timePerUpdate = 1000000000.0 / UPS_SET;

		long previousTime = System.nanoTime();
		long previousUpdateTime = previousTime;

		int frames = 0;
		int updates = 0;
		long lastCheck = System.currentTimeMillis();

		double deltaU = 0;
		double deltaF = 0;

		while (running) {
			long currentTime = System.nanoTime();

			deltaU += (currentTime - previousTime) / timePerUpdate;
			deltaF += (currentTime - previousTime) / timePerFrame;
			previousTime = currentTime;

			if (deltaU >= 1) {
				float realDeltaTime = Math.min(0.1f, (currentTime - previousUpdateTime) / 1000000000f);
				previousUpdateTime = currentTime;
				update(realDeltaTime);
				updates++;
				deltaU--;
			}

			if (deltaF >= 1) {
				gamePanel.repaint();
				frames++;
				deltaF--;
			}

			if (System.currentTimeMillis() - lastCheck >= 1000) {
				lastCheck = System.currentTimeMillis();
				System.out.println("FPS: " + frames + " | UPS: " + updates);
				frames = 0;
				updates = 0;

			}
		}

	}

	public void stopGameLoop() {
		running = false;
	}

	public void windowFocusGained() {
		SoundManager.windowFocusChanged(true);
	}

	public void windowFocusLost() {
		SoundManager.windowFocusChanged(false);
		if (Gamestate.state == Gamestate.PLAYING)
			playing.getPlayer().resetDirBooleans();
	}

	public void chooseCharacter(String spriteSheet) {
		playing.startLevel(selectedLevelIndex);
		playing.getPlayer().setSpriteSheet(spriteSheet);
		Gamestate.state = Gamestate.PLAYING;
	}

	public void beginNewGameSelection() {
		selectedLevelIndex = 0;
		Gamestate.state = Gamestate.CHARACTER_SELECT;
	}

	public boolean startUnlockedLevel(int levelNumber) {
		if (levelNumber < 1 || levelNumber > unlockedLevelCount
				|| levelNumber > playing.getLevelManager().getAmountOfLevels())
			return false;
		selectedLevelIndex = levelNumber - 1;
		playing.startLevel(selectedLevelIndex);
		Gamestate.state = Gamestate.PLAYING;
		return true;
	}

	public int getUnlockedLevelCount() {
		return unlockedLevelCount;
	}

	public void recordLevelCompletion(int levelNumber) {
		if (levelNumber == unlockedLevelCount
				&& unlockedLevelCount < playing.getLevelManager().getAmountOfLevels()) {
			unlockedLevelCount++;
			PROGRESS.putInt(UNLOCKED_LEVELS_KEY, unlockedLevelCount);
		}
	}

	public void openLevelSelect() {
		levelSelect.setSelectedLevel(unlockedLevelCount - 1);
		Gamestate.state = Gamestate.LEVEL_SELECT;
	}

	public Menu getMenu() {
		return menu;
	}

	public Playing getPlaying() {
		return playing;
	}

	public GameOptions getGameOptions() {
		return gameOptions;
	}

	public AudioOptions getAudioOptions() {
		return audioOptions;
	}

	public CharacterSelect getCharacterSelect() {
		return characterSelect;
	}

	public LevelSelect getLevelSelect() {
		return levelSelect;
	}

}
