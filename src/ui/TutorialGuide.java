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
			"WELCOME TO MACTAN VILLAGE", "MOVE AND SPRINT", "JUMP AND DOUBLE JUMP",
			"BASIC ATTACK", "DASH THROUGH THE COURSE", "BLOCK AND PARRY",
			"CHAIN YOUR STRIKES", "FIGHT YOUR WAY THROUGH", "UNLEASH YOUR ULTIMATE",
			"FINAL COMBAT TRIAL", "FINAL ENCOUNTER"
	};
	private static final String[] INSTRUCTIONS = {
			"Move with A / D. Hold SHIFT to sprint. Clear each wave to open the village path.",
			"Use A / D to move. Hold SHIFT to sprint across the training ground.",
			"Press SPACE to jump. Press SPACE again in mid-air to double jump.",
			"Left-click to attack. Time a second click to chain a powerful vertical strike.",
			"Press Q to dash in the direction you are facing. Use it to cross the spike course.",
			"Hold right-click to block. Face an incoming attack to parry and stun the enemy.",
			"Practice the second-click vertical strike, then clear the enemies to open the gate.",
			"Combine movement, jumping, dashing, and chained attacks to clear this encounter.",
			"Press R to unleash your ultimate. Use it when enemies are close together.",
			"Use every skill you learned. Defeat this group to open the final route.",
			"Defeat the final enemy to complete the tutorial and unlock the next stage."
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
		int scale = Math.max(1, Math.round(Game.SCALE));
		int width = Math.min(Game.GAME_WIDTH - 20 * scale, 340 * scale);
		int height = 82 * scale;
		int x = Game.GAME_WIDTH - width - 14 * scale;
		int y = 12 * scale;
		if (x < 220 * scale) {
			x = Math.max(8 * scale, (Game.GAME_WIDTH - width) / 2);
			y += Game.TILES_SIZE + 8 * scale;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		drawVillageBoard(g, x, y, width, height, scale);
		int iconSize = 48 * scale;
		if (villageBoard != null)
			g.drawImage(villageBoard, x + 8 * scale, y + (height - iconSize) / 2, iconSize, iconSize, null);
		else
			drawVillageEmblem(g, x + 8 * scale, y + 15 * scale, 44 * scale);
		int textX = x + 64 * scale;
		int textWidth = width - 76 * scale;
		g.setColor(new Color(255, 232, 187));
		g.setFont(new Font(Font.SERIF, Font.BOLD, 12 * scale));
		g.drawString(TITLES[lesson], textX, y + 23 * scale);
		g.setColor(new Color(255, 246, 220));
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10 * scale));
		int lineY = drawWrappedText(g, INSTRUCTIONS[lesson], textX, y + 43 * scale,
				textWidth, 12 * scale);
		int gate = firstClosedGate();
		if (gate >= 0 && tileX >= GATES[gate] - 5 && tileX <= GATES[gate]) {
			int remaining = countEnemiesForGate(gate);
			g.setColor(new Color(255, 207, 115));
			String challenge = "CHALLENGE GATE: defeat " + remaining + " more "
					+ (remaining == 1 ? "enemy" : "enemies") + " to proceed.";
			drawWrappedText(g, challenge, textX, Math.max(y + 72 * scale, lineY + 2 * scale),
					textWidth, 11 * scale);
		} else {
			g.setColor(new Color(235, 195, 128));
			g.drawString(VILLAGE_LABEL, textX, Math.max(y + 72 * scale, lineY + 2 * scale));
		}
		g.dispose();
	}

	private void drawVillageBoard(Graphics2D g, int x, int y, int width, int height, int scale) {
		g.setColor(new Color(8, 13, 15, 145));
		g.fillRoundRect(x + 4 * scale, y + 5 * scale, width, height, 10 * scale, 10 * scale);
		g.setColor(new Color(45, 29, 22, 245));
		g.fillRoundRect(x, y, width, height, 10 * scale, 10 * scale);
		g.setColor(new Color(111, 66, 38, 248));
		g.fillRoundRect(x + 3 * scale, y + 3 * scale, width - 6 * scale, height - 6 * scale,
				8 * scale, 8 * scale);
		g.setColor(new Color(151, 94, 53, 235));
		g.fillRect(x + 5 * scale, y + 5 * scale, width - 10 * scale, 15 * scale);
		g.setColor(new Color(76, 45, 29, 190));
		g.drawLine(x + 5 * scale, y + 24 * scale, x + width - 5 * scale, y + 24 * scale);
		g.drawLine(x + 5 * scale, y + 62 * scale, x + width - 5 * scale, y + 62 * scale);
		g.setColor(new Color(224, 181, 104));
		g.setStroke(new java.awt.BasicStroke(Math.max(1, scale * 2)));
		g.drawRoundRect(x + 2 * scale, y + 2 * scale, width - 5 * scale, height - 5 * scale,
				8 * scale, 8 * scale);
		g.setColor(new Color(235, 203, 141, 190));
		g.fillOval(x + width - 14 * scale, y + 10 * scale, 4 * scale, 4 * scale);
		g.fillOval(x + width - 14 * scale, y + height - 14 * scale, 4 * scale, 4 * scale);
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

	private int drawWrappedText(Graphics2D g, String text, int x, int baseline, int maxWidth, int lineHeight) {
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (line.length() > 0 && g.getFontMetrics().stringWidth(candidate) > maxWidth) {
				g.drawString(line.toString(), x, baseline);
				baseline += lineHeight;
				line.setLength(0);
			}
			if (line.length() > 0)
				line.append(' ');
			line.append(word);
		}
		if (line.length() > 0)
			g.drawString(line.toString(), x, baseline);
		return baseline;
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
