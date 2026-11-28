package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import entities.EnemyManager;
import levels.Level;
import main.Game;
import utilz.LoadSave;

/** Skill prompts and enemy-gated checkpoints for the Stage 1 tutorial. */
public class TutorialGuide {
	private static final int[] GATES = { 52, 94, 136, 173 };
	private static final int[][] LESSONS = {
			{ 0, 13 }, { 13, 25 }, { 25, 36 }, { 36, 52 }, { 52, 70 },
			{ 70, 94 }, { 94, 112 }, { 112, 136 }, { 136, 154 }, { 154, 173 }, { 173, Integer.MAX_VALUE }
	};
	private static final String[] TITLES = {
			"WELCOME TO MACTAN", "MOVE + SPRINT", "JUMP + DOUBLE JUMP",
			"BASIC ATTACK", "DASH COURSE", "BLOCK + PARRY",
			"ATTACK CHAIN", "COMBAT TRIAL", "UNLEASH ULTIMATE",
			"FINAL TRIAL", "FINAL ENCOUNTER"
	};
	private static final String[] INSTRUCTIONS = {
			"A / D move  ·  SHIFT sprint",
			"A / D move  ·  SHIFT sprint",
			"SPACE twice to double jump",
			"Left-click; time your next hit",
			"Q dashes forward across spikes",
			"Right-click to block and parry",
			"Time a click for a heavy strike",
			"Mix skills. Clear every enemy.",
			"Press R to unleash your ultimate",
			"Defeat the wave to advance",
			"Defeat the final enemy"
	};

	private final Level level;
	private final EnemyManager enemies;
	private final BufferedImage villageBoard = loadVillageBoard();
	private final boolean[] openGates = new boolean[GATES.length];
	private static final String VILLAGE_LABEL = "MACTAN VILLAGE  /  SKILL TRAIL";

	public TutorialGuide(Level level, EnemyManager enemies) {
		this.level = level;
		this.enemies = enemies;
		reset();
	}

	public static boolean supports(Level level) {
		return level != null && level.getLevelData()[0].length >= 190;
	}

	public void update(float playerWorldX) {
		for (int i = 0; i < GATES.length; i++) {
			if (openGates[i])
				continue;
			int firstTile = i == 0 ? 0 : GATES[i - 1] + 1;
			if (!enemies.hasActiveEnemyBetweenTiles(firstTile, GATES[i])) {
				openGates[i] = true;
				level.setTutorialGateOpen(GATES[i], true);
			}
		}
	}

	public void drawGates(Graphics graphics, int cameraX) {
		Graphics2D g = (Graphics2D) graphics.create();
		int gateHeight = Game.GAME_HEIGHT - Game.TILES_SIZE * 2;
		int scale = Math.max(1, Math.round(Game.SCALE));
		for (int i = 0; i < GATES.length; i++) {
			if (openGates[i])
				continue;
			int x = GATES[i] * Game.TILES_SIZE - cameraX;
			if (x < -Game.TILES_SIZE || x > Game.GAME_WIDTH)
				continue;
			g.setColor(new Color(18, 25, 39, 228));
			g.fillRect(x - 4 * scale, 0, Game.TILES_SIZE + 8 * scale, gateHeight);
			g.setColor(new Color(218, 171, 76));
			g.setStroke(new java.awt.BasicStroke(Math.max(1, scale * 2)));
			g.drawRect(x - 4 * scale, 0, Game.TILES_SIZE + 8 * scale, gateHeight);
			g.drawLine(x + 7 * scale, 0, x + 7 * scale, gateHeight);
			g.drawLine(x + 24 * scale, 0, x + 24 * scale, gateHeight);
			for (int y = 40 * scale; y < gateHeight; y += 72 * scale)
				g.drawLine(x - 4 * scale, y, x + Game.TILES_SIZE + 4 * scale, y);
		}
		g.dispose();
	}

	public void drawPrompt(Graphics graphics, float playerWorldX) {
		int tileX = Math.max(0, (int) (playerWorldX / Game.TILES_SIZE));
		int lesson = lessonAt(tileX);
		// Keep the noticeboard text readable at the same scale as the game HUD.
		int scale = Math.max(1, Math.round(Game.SCALE));
		int width = Math.min(Game.GAME_WIDTH - 20 * scale, 170 * scale);
		int height = 44 * scale;
		int x = Game.GAME_WIDTH - width - 14 * scale;
		int y = 12 * scale;
		if (x < 220 * scale) {
			x = Math.max(8 * scale, (Game.GAME_WIDTH - width) / 2);
			y += Game.TILES_SIZE + 8 * scale;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		drawVillageBoard(g, x, y, width, height, scale);
		int iconSize = 26 * scale;
		if (villageBoard != null)
			g.drawImage(villageBoard, x + 5 * scale, y + (height - iconSize) / 2, iconSize, iconSize, null);
		else
			drawVillageEmblem(g, x + 5 * scale, y + 9 * scale, 26 * scale);
		int textX = x + 35 * scale;
		g.setColor(new Color(255, 232, 187));
		g.setFont(new Font(Font.SERIF, Font.BOLD, 9 * scale));
		g.drawString(TITLES[lesson], textX, y + 14 * scale);
		g.setColor(new Color(255, 246, 220));
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8 * scale));
		g.drawString(INSTRUCTIONS[lesson], textX, y + 27 * scale);
		int gate = firstClosedGate();
		if (gate >= 0 && tileX >= GATES[gate] - 5 && tileX <= GATES[gate]) {
			int remaining = countEnemiesForGate(gate);
			g.setColor(new Color(255, 207, 115));
			String challenge = "GATE " + (gate + 1) + "  ·  " + remaining + " "
					+ (remaining == 1 ? "ENEMY" : "ENEMIES") + " LEFT";
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 7 * scale));
			g.drawString(challenge, textX, y + 39 * scale);
		} else {
			g.setColor(new Color(235, 195, 128));
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 7 * scale));
			g.drawString(VILLAGE_LABEL, textX, y + 39 * scale);
		}
		g.dispose();
	}

	private void drawVillageBoard(Graphics2D g, int x, int y, int width, int height, int scale) {
		g.setColor(new Color(8, 13, 15, 145));
		g.fillRoundRect(x + 2 * scale, y + 3 * scale, width, height, 7 * scale, 7 * scale);
		g.setColor(new Color(45, 29, 22, 245));
		g.fillRoundRect(x, y, width, height, 7 * scale, 7 * scale);
		g.setColor(new Color(111, 66, 38, 248));
		g.fillRoundRect(x + 2 * scale, y + 2 * scale, width - 4 * scale, height - 4 * scale,
				5 * scale, 5 * scale);
		g.setColor(new Color(151, 94, 53, 235));
		g.fillRect(x + 4 * scale, y + 3 * scale, width - 8 * scale, 10 * scale);
		g.setColor(new Color(76, 45, 29, 190));
		g.drawLine(x + 4 * scale, y + 17 * scale, x + width - 4 * scale, y + 17 * scale);
		g.drawLine(x + 4 * scale, y + 32 * scale, x + width - 4 * scale, y + 32 * scale);
		g.setColor(new Color(224, 181, 104));
		g.setStroke(new java.awt.BasicStroke(Math.max(1, scale * 2)));
		g.drawRoundRect(x + 1 * scale, y + 1 * scale, width - 3 * scale, height - 3 * scale,
				6 * scale, 6 * scale);
		g.setColor(new Color(235, 203, 141, 190));
		g.fillOval(x + width - 9 * scale, y + 6 * scale, 3 * scale, 3 * scale);
		g.fillOval(x + width - 9 * scale, y + height - 9 * scale, 3 * scale, 3 * scale);
	}

	private void drawVillageEmblem(Graphics2D g, int x, int y, int size) {
		g.setColor(new Color(225, 190, 113));
		g.fillPolygon(new int[] { x, x + size / 2, x + size },
				new int[] { y + size / 3, y, y + size / 3 }, 3);
		g.setColor(new Color(73, 46, 30));
		g.fillRect(x + size / 7, y + size / 3, size * 5 / 7, size / 2);
		g.setColor(new Color(242, 218, 167));
		g.drawRect(x + size / 3, y + size / 2, size / 5, size / 3);
	}

	private static BufferedImage loadVillageBoard() {
		Path asset = LoadSave.GetProjectRoot().resolve("pananakop_modern_assets")
				.resolve("Environment")
				.resolve("Cartoon_Medieval_Field_Work_Level_Set_Environment - Quest Board.png");
		try {
			return ImageIO.read(asset.toFile());
		} catch (IOException | RuntimeException exception) {
			return null;
		}
	}

	public void reset() {
		for (int i = 0; i < GATES.length; i++) {
			openGates[i] = false;
			level.setTutorialGateOpen(GATES[i], false);
		}
	}

	private int lessonAt(int tileX) {
		for (int i = 0; i < LESSONS.length; i++)
			if (tileX >= LESSONS[i][0] && tileX < LESSONS[i][1])
				return i;
		return LESSONS.length - 1;
	}

	private int firstClosedGate() {
		for (int i = 0; i < openGates.length; i++)
			if (!openGates[i])
				return i;
		return -1;
	}

	private int countEnemiesForGate(int gate) {
		int firstTile = gate == 0 ? 0 : GATES[gate - 1] + 1;
		int count = 0;
		for (int tile = firstTile; tile <= GATES[gate]; tile++)
			if (enemies.hasActiveEnemyBetweenTiles(tile, tile))
				count++;
		return count;
	}
}
