package main;

import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import gamestates.GameOptions;
import gamestates.Gamestate;
import gamestates.Menu;
import gamestates.Playing;
import gamestates.CharacterSelect;
import levels.Level;
import ui.AudioOptions;

public class Game implements Runnable {

	private GamePanel gamePanel;
	private Thread gameThread;
	private final int FPS_SET = 120;
	private final int UPS_SET = 200;

	private Playing playing;
	private Menu menu;
	private GameOptions gameOptions;
	private CharacterSelect characterSelect;
	private AudioOptions audioOptions;
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
		gameOptions = new GameOptions(this);
		characterSelect = new CharacterSelect(this);
	}

	private void startGameLoop() {
		gameThread = new Thread(this);
		gameThread.start();
	}

	public void update() {
		switch (Gamestate.state) {
		case MENU:
			menu.update();
			break;
		case PLAYING:
			playing.update();
			break;
		case CHARACTER_SELECT:
			characterSelect.update();
			break;
		case OPTIONS:
			gameOptions.update();
			break;
		case QUIT:
		default:
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
				update();
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

	public void windowFocusLost() {
		if (Gamestate.state == Gamestate.PLAYING)
			playing.getPlayer().resetDirBooleans();
	}

	public void chooseCharacter(String spriteSheet) {
		playing.getPlayer().setSpriteSheet(spriteSheet);
		Gamestate.state = Gamestate.PLAYING;
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

}
