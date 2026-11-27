package levels;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import gamestates.Gamestate;
import main.Game;
import utilz.LoadSave;

public class LevelManager {

	private Game game;
	private BufferedImage[] levelSprite;
	private ArrayList<Level> levels;
	private int lvlIndex = 0;

	public LevelManager(Game game) {
		this(game, null);
	}

	public LevelManager(Game game, Level testLevel) {
		this.game = game;
		importOutsideSprites();
		levels = new ArrayList<>();
		if (testLevel == null)
			buildAllLevels();
		else
			levels.add(testLevel);
	}

	public void loadNextLevel() {
		lvlIndex++;
		if (lvlIndex >= levels.size()) {
			lvlIndex = 0;
			System.out.println("No more levels! Game Completed!");
			Gamestate.state = Gamestate.MENU;
		}

		Level newLevel = levels.get(lvlIndex);
		game.getPlaying().getEnemyManager().loadEnemies(newLevel);
		game.getPlaying().getPlayer().loadLvlData(newLevel.getLevelData());
		game.getPlaying().setMaxLvlOffset(newLevel.getLvlOffset());
		game.getPlaying().getObjectManager().loadObjects(newLevel);
	}

	private void buildAllLevels() {
		BufferedImage[] allLevels = LoadSave.GetAllLevels();
		for (int i = 0; i < allLevels.length; i++)
			levels.add(new Level(allLevels[i], i + 1));

	}

	private void importOutsideSprites() {
		BufferedImage img = LoadSave.GetSpriteAtlas(LoadSave.LEVEL_ATLAS);
		levelSprite = new BufferedImage[48];
		for (int j = 0; j < 4; j++)
			for (int i = 0; i < 12; i++) {
				int index = j * 12 + i;
				levelSprite[index] = img.getSubimage(i * 32, j * 32, 32, 32);
			}
	}

	public void draw(Graphics g, int lvlOffset) {
		Level currentLevel = levels.get(lvlIndex);
		for (int j = 0; j < Game.TILES_IN_HEIGHT; j++)
			for (int i = 0; i < currentLevel.getLevelData()[0].length; i++) {
				int index = currentLevel.getSpriteIndex(i, j);
				g.drawImage(levelSprite[index], Game.TILES_SIZE * i - lvlOffset, Game.TILES_SIZE * j, Game.TILES_SIZE, Game.TILES_SIZE, null);
				BufferedImage modernTile = currentLevel.getModernTile(i, j);
				if (modernTile != null)
					g.drawImage(modernTile, Game.TILES_SIZE * i - lvlOffset, Game.TILES_SIZE * j,
							Game.TILES_SIZE, Game.TILES_SIZE, null);
			}
		float decorationScale = (float) Game.TILES_SIZE / Game.TILES_DEFAULT_SIZE;
		for (LevelDecoration decoration : currentLevel.getDecorations()) {
			int x = Math.round(decoration.getX() * decorationScale) - lvlOffset;
			int y = Math.round(decoration.getY() * decorationScale);
			int width = Math.max(1, Math.round(decoration.getWidth() * decorationScale));
			int height = Math.max(1, Math.round(decoration.getHeight() * decorationScale));
			g.drawImage(decoration.getImage(), x, y, width, height, null);
		}
	}

	public void update() {

	}

	public int getLevelIndex() {
		return lvlIndex;
	}

	public Level getCurrentLevel() {
		return levels.get(lvlIndex);
	}

	public int getAmountOfLevels() {
		return levels.size();
	}

}
