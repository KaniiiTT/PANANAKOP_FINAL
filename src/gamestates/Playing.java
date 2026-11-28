
package gamestates;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.Set;

import background.GroundLifeLayer;
import background.ParallaxBackground;
import entities.EnemyManager;
import entities.Enemy;
import entities.Magellan;
import entities.Player;
import levels.LevelManager;
import main.Game;
import objects.ObjectManager;
import ui.BossUI;
import ui.AreaTitleOverlay;
import effects.HitEffects;
import ui.GameOverOverlay;
import ui.LevelCompletedOverlay;
import ui.PauseOverlay;
import utilz.LoadSave;
import static utilz.Constants.EnemyConstants.DEAD;
import static utilz.Constants.Environment.*;
import utilz.SoundManager;

public class Playing extends State implements Statemethods {

    private Player player;
    private LevelManager levelManager;
    private EnemyManager enemyManager;
    private ObjectManager objectManager;

    private BufferedImage backgroundImg, bigCloud, smallCloud;
    private ParallaxBackground parallaxBackground;
    private GroundLifeLayer groundLife;
    private int[] smallCloudsPos;
    private Random rnd = new Random();
    private PauseOverlay pauseOverlay;
    private GameOverOverlay gameOverOverlay;
    private LevelCompletedOverlay levelCompletedOverlay;
    private BossUI bossUI;
    private final AreaTitleOverlay areaTitle = new AreaTitleOverlay();
    private final HitEffects hitEffects = new HitEffects();
    private BufferedImage ultimateScene;

    private boolean paused = false;

    // Camera
    private float xLvlOffset;
    private int maxLvlOffsetX;

    // Lower value = slower camera catch-up.
    private float cameraFollowSpeed = 0.06f;

    private boolean gameOver;
    private static final int BOSS_MUSIC_RANGE_TILES = 8; // same distance as Magellan's introduction
    private static final int BOSS_MUSIC_LEAVE_TILES = 22; // far wider than the entry range: no flicker at the edge
    private Enemy musicBoss; // the boss whose fight the music is currently following
    private boolean lvlCompleted;
    private boolean playerDying;

    public Playing(Game game) {
        this(game, null);
    }

    public Playing(Game game, levels.Level testLevel) {
        super(game);
        initClasses(testLevel);
        parallaxBackground = new ParallaxBackground();
        groundLife = new GroundLifeLayer(parallaxBackground.getCycle(), 200.0); // 200 = Game.UPS_SET

        backgroundImg = LoadSave.GetSpriteAtlas(LoadSave.PLAYING_BG_IMG);
        bigCloud = LoadSave.GetSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallCloud = LoadSave.GetSpriteAtlas(LoadSave.SMALL_CLOUDS);
        smallCloudsPos = new int[8];
        for (int i = 0; i < smallCloudsPos.length; i++)
            smallCloudsPos[i] = (int) (90 * Game.SCALE) + rnd.nextInt((int) (100 * Game.SCALE));

        calcLvlOffset();
        loadStartLevel();
        areaTitle.showLevel(levelManager.getLevelIndex() + 1);
    }

    public void loadNextLevel() {
        resetAll();
        game.recordLevelCompletion(levelManager.getLevelIndex() + 1);
        levelManager.loadNextLevel();
        areaTitle.showLevel(levelManager.getLevelIndex() + 1);
        player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());

        calcLvlOffset();
        xLvlOffset = 0;
    }

    public void startLevel(int levelIndex) {
        resetAll();
        levelManager.loadLevel(levelIndex);
        player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());
        calcLvlOffset();
        xLvlOffset = 0;
        areaTitle.showLevel(levelIndex + 1);
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
        update(1f / 200f);
    }

    public void update(float dt) {
        SoundManager.musicPaused(paused);
        hitEffects.update(dt);
        player.updateFlash(dt);
        enemyManager.updateFlash(dt);
        player.updateUltimateEffect(dt);
        if (hitEffects.isFrozen() || player.isUltimateWorldFrozen())
            return;
        if (paused) {
            pauseOverlay.update();

        } else if (lvlCompleted) {
            levelCompletedOverlay.update();

        } else if (gameOver) {
            gameOverOverlay.update();

        } else if (playerDying) {
            player.update();

        } else {
            areaTitle.update(dt);
            parallaxBackground.setFocusY(player.getHitbox().y + player.getHitbox().height / 2f);
            parallaxBackground.update();
            levelManager.update();

            objectManager.update(
                    levelManager.getCurrentLevel().getLevelData(), player
            );

            player.update();
            groundLife.update(levelManager.getCurrentLevel(), player.getHitbox());

            enemyManager.update(
                    levelManager.getCurrentLevel().getLevelData(), player
            );

            bossUI.update(player, enemyManager.getActiveBoss());
            updateMusic(dt);

            checkCloseToBorder();
        }
    }

    // Tells the music system where the player is and whether a boss fight is on.
    private void updateMusic(float dt) {
        if (playerDying || gameOver)
            return; // the death just faded the music out; do not start it again this tick
        float centerX = player.getHitbox().x + player.getHitbox().width / 2f;
        SoundManager.musicWorldUpdate(levelManager.getLevelIndex(), (int) (centerX / Game.TILES_SIZE), dt);

        if (musicBoss == null) {
            musicBoss = enemyManager.findBossNear(player, BOSS_MUSIC_RANGE_TILES * Game.TILES_SIZE);
            if (musicBoss != null)
                SoundManager.musicBossEngaged(musicBoss instanceof Magellan);
        } else if (!musicBoss.isActive() || musicBoss.getState() == DEAD
                || Math.abs(musicBoss.getHitbox().x - player.getHitbox().x) > BOSS_MUSIC_LEAVE_TILES * Game.TILES_SIZE) {
            musicBoss = null;
            SoundManager.musicBossEnded();
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
		if (player.isUltimateEffectActive()) {
			BufferedImage scene = renderUltimateScene(cameraX);
			g.drawImage(player.applyUltimateImpactFrame(scene), 0, 0, null);
			player.renderUI(g);
			bossUI.draw(g, enemyManager.getActiveBoss());
			return;
		}
		Graphics2D world = (Graphics2D) g.create();
		hitEffects.applyShake(world);

        parallaxBackground.draw(world, cameraX, levelManager.getCurrentLevel().getLevelData());

        levelManager.draw(world, cameraX);
        groundLife.draw(world, cameraX); // grass, ferns and small animals stand on the ground, behind the player
        player.render(world, cameraX);
        enemyManager.draw(world, cameraX);
        objectManager.draw(world, cameraX);
		hitEffects.removeShake(world);
		world.dispose();
		player.renderUI(g);

        bossUI.draw(g, enemyManager.getActiveBoss());

        if (!paused && !gameOver && !lvlCompleted)
            areaTitle.draw(g);

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

	private BufferedImage renderUltimateScene(int cameraX) {
		if (ultimateScene == null || ultimateScene.getWidth() != Game.GAME_WIDTH
				|| ultimateScene.getHeight() != Game.GAME_HEIGHT)
			ultimateScene = new BufferedImage(Game.GAME_WIDTH, Game.GAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D scene = ultimateScene.createGraphics();
		scene.setComposite(java.awt.AlphaComposite.Clear);
		scene.fillRect(0, 0, ultimateScene.getWidth(), ultimateScene.getHeight());
		scene.setComposite(java.awt.AlphaComposite.SrcOver);
		hitEffects.applyShake(scene);
		parallaxBackground.draw(scene, cameraX, levelManager.getCurrentLevel().getLevelData());
		levelManager.draw(scene, cameraX);
		groundLife.draw(scene, cameraX);
		enemyManager.draw(scene, cameraX);
		objectManager.draw(scene, cameraX);
		player.render(scene, cameraX);
		hitEffects.removeShake(scene);
		scene.dispose();
		return ultimateScene;
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
        musicBoss = null;
        SoundManager.musicBossEnded();
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
        if (gameOver && !this.gameOver)
            SoundManager.musicGameOver();
        this.gameOver = gameOver;
    }

    public void checkObjectHit(Rectangle2D.Float attackBox) {
        objectManager.checkObjectHit(attackBox);
    }

    public boolean checkEnemyHit(Rectangle2D.Float attackBox) {
        return enemyManager.checkEnemyHit(attackBox);
    }

    public boolean checkEnemyHit(Rectangle2D.Float attackBox, int damage, int knockback, int direction,
            Set<Enemy> hitTargets) {
        return enemyManager.checkEnemyHit(attackBox, damage, knockback, direction, hitTargets, false);
    }

    public boolean checkEnemyHit(Rectangle2D.Float attackBox, int damage, int knockback, int direction,
            Set<Enemy> hitTargets, boolean heavy) {
        return enemyManager.checkEnemyHit(attackBox, damage, knockback, direction, hitTargets, heavy);
    }

    public boolean checkAllEnemiesHit(Rectangle2D.Float attackBox, int damage, int knockback, int direction,
            Set<Enemy> hitTargets, boolean heavy) {
        return enemyManager.checkAllEnemiesHit(attackBox, damage, knockback, direction, hitTargets, heavy);
    }

    public HitEffects getHitEffects() {
        return hitEffects;
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
        if (playerDying)
            SoundManager.musicGameOver(); // the music starts fading as the death animation begins
        this.playerDying = playerDying;
    }
}
