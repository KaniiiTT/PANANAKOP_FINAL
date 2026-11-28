package levels;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import entities.Crabby;
import entities.Magellan;
import entities.Boar;
import entities.Conquistador;
import entities.Espinosa;
import entities.Monkey;
import entities.Gorilla;
import main.Game;
import objects.Cannon;
import objects.GameContainer;
import objects.Potion;
import objects.Spike;
import objects.Trap;
import utilz.HelpMethods;

import static utilz.HelpMethods.GetLevelData;
import static utilz.HelpMethods.GetCrabs;
import static utilz.HelpMethods.GetPlayerSpawn;

public class Level {

	private BufferedImage img;
	private BufferedImage[][] modernTiles;
	private List<LevelDecoration> decorations;
	private int[][] lvlData;
	private ArrayList<Crabby> crabs;
	private ArrayList<Magellan> magellans;
	private ArrayList<Boar> boars;
	private ArrayList<Conquistador> conquistadors;
	private ArrayList<Espinosa> espinosas;          // with the other enemy lists
	private ArrayList<Monkey> monkeys;
	private ArrayList<Gorilla> gorillas;
	private ArrayList<Potion> potions;
	private ArrayList<Spike> spikes;
	private ArrayList<Trap> traps;
	private ArrayList<GameContainer> containers;
	private ArrayList<Cannon> cannons;
	private int lvlTilesWide;
	private int maxTilesOffset;
	private int maxLvlOffsetX;
	private Point playerSpawn;

	public Level(BufferedImage img, int levelNumber) {
		this(img, utilz.LoadSave.GetModernLevelTiles(levelNumber, img.getWidth(), img.getHeight()),
				utilz.LoadSave.GetLevelDecorations(levelNumber));
	}

	public Level(BufferedImage img, BufferedImage[][] modernTiles) {
		this(img, modernTiles, List.of());
	}

	public Level(BufferedImage img, BufferedImage[][] modernTiles, List<LevelDecoration> decorations) {
		this.img = img;
		if (modernTiles.length != img.getHeight())
			throw new IllegalArgumentException("Modern tile height must match the level image.");
		for (BufferedImage[] row : modernTiles)
			if (row.length != img.getWidth())
				throw new IllegalArgumentException("Modern tile width must match the level image.");
		this.modernTiles = modernTiles;
		this.decorations = new ArrayList<>(decorations);
		createLevelData();
		createEnemies();
		createPotions();
		createContainers();
		createSpikes();
		createTraps();
		createCannons();
		calcLvlOffsets();
		calcPlayerSpawn();
	}

	private void createCannons() {
		cannons = HelpMethods.GetCannons(img);
	}

	private void createSpikes() {
		spikes = HelpMethods.GetSpikes(img);
	}

	private void createTraps() {
		traps = HelpMethods.GetTraps(img);
	}

	private void createContainers() {
		containers = HelpMethods.GetContainers(img);
	}

	private void createPotions() {
		potions = HelpMethods.GetPotions(img);
	}

	private void calcPlayerSpawn() {
		playerSpawn = GetPlayerSpawn(img);
	}

	private void calcLvlOffsets() {
		lvlTilesWide = img.getWidth();
		maxTilesOffset = lvlTilesWide - Game.TILES_IN_WIDTH;
		maxLvlOffsetX = Game.TILES_SIZE * maxTilesOffset;
	}

	private void createEnemies() {
		crabs = GetCrabs(img);
		magellans = HelpMethods.GetMagellans(img);
		boars = HelpMethods.GetBoars(img);
		conquistadors = HelpMethods.GetConquistadors(img);
		espinosas = HelpMethods.GetEspinosas(img);
		monkeys = HelpMethods.GetMonkeys(img);
		gorillas = HelpMethods.GetGorillas(img);
	}

	private void createLevelData() {
		lvlData = GetLevelData(img);
		// Modern block tiles are stored in the sidecar file, so they must also
		// become solid collision cells just like classic ground tiles.
		for (int y = 0; y < modernTiles.length; y++)
			for (int x = 0; x < modernTiles[y].length; x++)
				if (modernTiles[y][x] != null)
					lvlData[y][x] = 0;
	}

	public int getSpriteIndex(int x, int y) {
		// A custom tile has its own sprite; keep the classic cell underneath empty.
		if (modernTiles[y][x] != null)
			return 11;
		return lvlData[y][x];
	}

	public BufferedImage getModernTile(int x, int y) {
		return modernTiles[y][x];
	}

	public List<LevelDecoration> getDecorations() {
		return decorations;
	}

	public int[][] getLevelData() {
		return lvlData;
	}

	public int getLvlOffset() {
		return maxLvlOffsetX;
	}

	public ArrayList<Crabby> getCrabs() {
		return crabs;
	}

	public ArrayList<Magellan> getMagellans() {
		return magellans;
	}

	public ArrayList<Boar> getBoars() {
		return boars;
	}

	public ArrayList<Conquistador> getConquistadors() {
		return conquistadors;
	}
	public ArrayList<Espinosa> getEspinosas() {
		return espinosas;
	}

	public ArrayList<Monkey> getMonkeys() {
		return monkeys;
	}

	public ArrayList<Gorilla> getGorillas() {
		return gorillas;
	}

	public Point getPlayerSpawn() {
		return playerSpawn;
	}

	public ArrayList<Potion> getPotions() {
		return potions;
	}

	public ArrayList<GameContainer> getContainers() {
		return containers;
	}

	public ArrayList<Spike> getSpikes() {
		return spikes;
	}

	public ArrayList<Trap> getTraps() {
		return traps;
	}
	
	public ArrayList<Cannon> getCannons(){
		return cannons;
	}

}
