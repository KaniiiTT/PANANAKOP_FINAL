
package gamestates;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import background.ParallaxBackground;
import entities.EnemyManager;
import entities.Player;
import levels.LevelManager;
import main.Game;
import objects.ObjectManager;
import ui.BossUI;
import ui.GameOverOverlay;
import ui.LevelCompletedOverlay;
import ui.PauseOverlay;
import utilz.LoadSave;
import static utilz.Constants.Environment.*;
import utilz.SoundManager;

public class Playing extends State implements Statemethods {

    private Player player;
    private LevelManager levelManager;
    private EnemyManager enemyManager;
    private ObjectManager objectManager;

    private BufferedImage backgroundImg, bigCloud, smallCloud;
    private ParallaxBackground parallaxBackground;
    private int[] smallCloudsPos;
    private Random rnd = new Random();
    private PauseOverlay pauseOverlay;
    private GameOverOverlay gameOverOverlay;
    private LevelCompletedOverlay levelCompletedOverlay;
    private BossUI bossUI;

    private boolean paused = false;

    // Camera
    private float xLvlOffset;
    private int maxLvlOffsetX;

    // Lower value = slower camera catch-up.
    private float cameraFollowSpeed = 0.06f;

    private boolean gameOver;
    private boolean lvlCompleted;
    private boolean playerDying;

    public Playing(Game game) {
        this(game, null);
    }

    public Playing(Game game, levels.Level testLevel) {
        super(game);
        initClasses(testLevel);
        parallaxBackground = new ParallaxBackground();

        backgroundImg = LoadSave.GetSpriteAtlas(LoadSave.PLAYING_BG_IMG);
        bigCloud = LoadSave.GetSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallCloud = LoadSave.GetSpriteAtlas(LoadSave.SMALL_CLOUDS);
        smallCloudsPos = new int[8];
        for (int i = 0; i < smallCloudsPos.length; i++)
            smallCloudsPos[i] = (int) (90 * Game.SCALE) + rnd.nextInt((int) (100 * Game.SCALE));

        calcLvlOffset();
        loadStartLevel();
    }

    public void loadNextLevel() {
        resetAll();
        levelManager.loadNextLevel();
        player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());

        calcLvlOffset();
        xLvlOffset = 0;
    }

    private void loadStartLevel() {
        enemyManager.loadEnemies(levelManager.getCurrentLevel());
        objectManager.loadObjects(levelManager.getCurrentLevel());
    }

    private void calcLvlOffset() {
        maxLvlOffsetX = levelManager.getCurrentLevel().getLvlOffset();
    }

    private void initClasses(levels.Level testLevel) {
        levelManager = new LevelManager(game, testLevel);
        enemyManager = new EnemyManager(this);
        objectManager = new ObjectManager(this);

        player = new Player(
                200, 200,
                (int) (64 * Game.SCALE),
                (int) (64 * Game.SCALE),
                this
        );

        player.loadLvlData(levelManager.getCurrentLevel().getLevelData());
        player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());

        pauseOverlay = new PauseOverlay(this);
        gameOverOverlay = new GameOverOverlay(this);
        levelCompletedOverlay = new LevelCompletedOverlay(this);
        bossUI = new BossUI();
    }

    @Override
    public void update() {
        if (paused) {
            pauseOverlay.update();

        } else if (lvlCompleted) {
            levelCompletedOverlay.update();

        } else if (gameOver) {
            gameOverOverlay.update();

        } else if (playerDying) {
            player.update();

        } else {
            levelManager.update();

            objectManager.update(
                    levelManager.getCurrentLevel().getLevelData(), player
            );

            player.update();

            enemyManager.update(
                    levelManager.getCurrentLevel().getLevelData(), player
            );

            bossUI.update(player, enemyManager.getActiveBoss());

            checkCloseToBorder();
        }
    }

    // Smooth horizontal camera follow.
    private void checkCloseToBorder() {

        // Get the player's center in world coordinates.
        float playerCenterX =
                player.getHitbox().x + player.getHitbox().width / 2f;

        // Calculate the ideal camera position.
        float targetOffset =
                playerCenterX - Game.GAME_WIDTH / 2f;

        // Keep the camera inside the level.
        targetOffset = Math.max(
                0,
                Math.min(targetOffset, maxLvlOffsetX)
        );

        // Gradually catch up to the target position.
        xLvlOffset +=
                (targetOffset - xLvlOffset) * cameraFollowSpeed;

        // Snap to the target when extremely close.
        if (Math.abs(targetOffset - xLvlOffset) < 0.5f) {
            xLvlOffset = targetOffset;
        }
    }

    @Override
    public void draw(Graphics g) {

        int cameraX = Math.round(xLvlOffset);

        parallaxBackground.draw(g, cameraX, levelManager.getCurrentLevel().getLevelData());

        levelManager.draw(g, cameraX);
        player.render(g, cameraX);
        enemyManager.draw(g, cameraX);
        objectManager.draw(g, cameraX);

        bossUI.draw(g, enemyManager.getActiveBoss());

        if (paused) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
            pauseOverlay.draw(g);

        } else if (gameOver) {
            gameOverOverlay.draw(g);

        } else if (lvlCompleted) {
            levelCompletedOverlay.draw(g);
        }
    }

    private void drawClouds(Graphics g) {
        for (int i = 0; i < 3; i++)
            g.drawImage(bigCloud, i * BIG_CLOUD_WIDTH - (int) (xLvlOffset * 0.3),
                    (int) (204 * Game.SCALE), BIG_CLOUD_WIDTH, BIG_CLOUD_HEIGHT, null);

        for (int i = 0; i < smallCloudsPos.length; i++)
            g.drawImage(smallCloud, SMALL_CLOUD_WIDTH * 4 * i - (int) (xLvlOffset * 0.7),
                    smallCloudsPos[i], SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT, null);
    }

    public void resetAll() {
        gameOver = false;
        paused = false;
        lvlCompleted = false;
        playerDying = false;

        player.resetAll();
        enemyManager.resetAllEnemies();
        objectManager.resetAllObjects();
        bossUI.reset();
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public void checkObjectHit(Rectangle2D.Float attackBox) {
        objectManager.checkObjectHit(attackBox);
    }

    public void checkEnemyHit(Rectangle2D.Float attackBox) {
        enemyManager.checkEnemyHit(attackBox);
    }

    public void checkPotionTouched(Rectangle2D.Float hitbox) {
        objectManager.checkObjectTouched(hitbox);
    }

    public void checkSpikesTouched(Player p) {
        objectManager.checkSpikesTouched(p);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Attack is started on mousePressed.
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            gameOverOverlay.keyPressed(e);
        } else {
            switch (e.getKeyCode()) {

            case KeyEvent.VK_A:
                player.setLeft(true);
                break;

            case KeyEvent.VK_D:
                player.setRight(true);
                break;

            case KeyEvent.VK_SPACE:
                player.setJump(true);
                break;

            case KeyEvent.VK_SHIFT:
                player.setRunning(true);
                break;

            case KeyEvent.VK_Q:
                player.setDash(true);
                break;

            case KeyEvent.VK_R:
                player.setUltimate(true);
                break;

            case KeyEvent.VK_V:
                player.setVictory(true);
                break;

            case KeyEvent.VK_ESCAPE:
                paused = !paused;
                break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!gameOver) {
            switch (e.getKeyCode()) {

            case KeyEvent.VK_A:
                player.setLeft(false);
                break;

            case KeyEvent.VK_D:
                player.setRight(false);
                break;

            case KeyEvent.VK_SPACE:
                player.setJump(false);
                break;

            case KeyEvent.VK_SHIFT:
                player.setRunning(false);
                break;
            }
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (!gameOver && paused) {
            pauseOverlay.mouseDragged(e);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!gameOver) {

            if (e.getButton() == MouseEvent.BUTTON1
                    && !paused && !lvlCompleted) {

                player.setAttacking(true);
                SoundManager.play(SoundManager.Effect.ATTACK);

            } else if (e.getButton() == MouseEvent.BUTTON3
                    && !paused && !lvlCompleted) {

                player.setBlocking(true);

            } else if (paused) {
                pauseOverlay.mousePressed(e);

            } else if (lvlCompleted) {
                levelCompletedOverlay.mousePressed(e);
            }

        } else {
            gameOverOverlay.mousePressed(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!gameOver) {

            if (e.getButton() == MouseEvent.BUTTON3) {
                player.setBlocking(false);

            } else if (paused) {
                pauseOverlay.mouseReleased(e);

            } else if (lvlCompleted) {
                levelCompletedOverlay.mouseReleased(e);
            }

        } else {
            gameOverOverlay.mouseReleased(e);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!gameOver) {

            if (paused) {
                pauseOverlay.mouseMoved(e);

            } else if (lvlCompleted) {
                levelCompletedOverlay.mouseMoved(e);
            }

        } else {
            gameOverOverlay.mouseMoved(e);
        }
    }

    public void setLevelCompleted(boolean levelCompleted) {
        this.lvlCompleted = levelCompleted;
    }

    public void setMaxLvlOffset(int lvlOffset) {
        this.maxLvlOffsetX = lvlOffset;
    }

    public void unpauseGame() {
        paused = false;
    }

    public void windowFocusLost() {
        player.resetDirBooleans();
    }

    public Player getPlayer() {
        return player;
    }

    public EnemyManager getEnemyManager() {
        return enemyManager;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public void setPlayerDying(boolean playerDying) {
        this.playerDying = playerDying;
    }
}
