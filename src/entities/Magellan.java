package entities;

import static utilz.Constants.Directions.LEFT;
import static utilz.Constants.Directions.RIGHT;
import static utilz.Constants.EnemyConstants.*;
import static utilz.Constants.GRAVITY;
import static utilz.HelpMethods.*;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import main.Game;
import effects.HitEffects;
import utilz.LoadSave;

/** Dedicated final-stage boss. All Magellan animations are owned by this class. */
public class Magellan extends Enemy {
	private static final String[] ANIMATION_NAMES = { "idle", "walk", "run", "jump", "block_parry",
			"attack_1_slash", "attack_2_lunge", "attack_3_spin_cut", "skill_pistol_shot",
			"skill_cannon_command", "skill_summon_crew", "skill_rage", "hurt", "die" };
	private final Map<String, BufferedImage[]> leftFrames = new HashMap<>();
	private final Map<String, BufferedImage[]> rightFrames = new HashMap<>();
	private final Map<String, BufferedImage[]> leftWhiteFrames = new HashMap<>();
	private final Map<String, BufferedImage[]> rightWhiteFrames = new HashMap<>();
	private final Map<String, BufferedImage[]> effects = new HashMap<>();
	private static final String[] EFFECT_NAMES = { "slash_wave", "thrust_streak", "bullet", "muzzle_flash",
			"smoke_puff", "cannon_shell", "target_marker", "cannon_explosion", "shockwave", "summon_sigil",
			"spawn_flash", "rage_aura", "parry_spark" };
	private boolean hitChecked;
	private int attackCooldown;
	private boolean rageUsed;
	private boolean chasingPlayer;
	private float effectTargetX;
	private final Queue<Integer> attackPattern = new ArrayDeque<>();
	private static final int GAME_UPDATES_PER_SECOND = 200;
	// The sprite frame is 128px tall, but the visible feet end at row 122.
	private static final int SPRITE_FEET_Y = 122;

	public Magellan(float x, float y) {
		super(x, y, MAGELLAN_WIDTH, MAGELLAN_HEIGHT, MAGELLAN);
		initHitbox(48, 90);
		attackBox = new java.awt.geom.Rectangle2D.Float(x, y, 120 * Game.SCALE, 80 * Game.SCALE);
		for (String animation : ANIMATION_NAMES) {
			leftFrames.put(animation, LoadSave.GetMagellanAnimation("left", animation));
			rightFrames.put(animation, LoadSave.GetMagellanAnimation("right", animation));
			leftWhiteFrames.put(animation, whiteFrames(leftFrames.get(animation)));
			rightWhiteFrames.put(animation, whiteFrames(rightFrames.get(animation)));
		}
		loadEffects();
		walkDir = LEFT;
		loadAttackPattern();
	}

	private void loadEffects() {
		int[][] sizes = { { 5, 88, 72 }, { 6, 120, 28 }, { 2, 24, 10 }, { 4, 56, 40 },
				{ 6, 48, 48 }, { 2, 28, 56 }, { 8, 96, 30 }, { 8, 112, 112 }, { 6, 144, 36 },
				{ 10, 144, 48 }, { 6, 72, 72 }, { 8, 160, 128 }, { 4, 44, 44 } };
		for (int i = 0; i < EFFECT_NAMES.length; i++) {
			BufferedImage strip = LoadSave.GetMagellanEffect(EFFECT_NAMES[i]);
			BufferedImage[] frames = new BufferedImage[sizes[i][0]];
			for (int frame = 0; frame < frames.length; frame++)
				frames[frame] = strip.getSubimage(frame * sizes[i][1], 0, sizes[i][1], sizes[i][2]);
			effects.put(EFFECT_NAMES[i], frames);
		}
	}

	// Queue = simple attack plan. Each move is used in order, then repeated.
	private void loadAttackPattern() {
		attackPattern.clear();
		attackPattern.add(MAGELLAN_SLASH);
		attackPattern.add(MAGELLAN_LUNGE);
		attackPattern.add(MAGELLAN_SPIN_CUT);
		attackPattern.add(MAGELLAN_PISTOL);
		attackPattern.add(MAGELLAN_CANNON);
		attackPattern.add(MAGELLAN_SUMMON);
	}

	public void update(int[][] lvlData, Player player) {
		if (firstUpdate)
			firstUpdateCheck(lvlData);
		if (attackCooldown > 0)
			attackCooldown--;
		if (inAir) {
			if (state != MAGELLAN_JUMP)
				newState(MAGELLAN_JUMP);
			updateBossInAir(lvlData);
		} else if (state == MAGELLAN_JUMP) {
			newState(IDLE);
		} else if (state == IDLE || state == RUNNING) {
			boolean playerDetected = isPlayerDetected(player);
			chasingPlayer = playerDetected;
			if (playerDetected)
				turnTowardsPlayer(player);
			if (isPlayerInAttackRange(player) && attackCooldown == 0)
				startNextSkill(player);
			else {
				if (state != RUNNING)
					newState(RUNNING);
				// No player detected: keep the current direction and patrol forward.
				move(lvlData);
			}
		} else if (isAttackState(state)) {
			if (!hitChecked && isHitFrame()) {
				checkPlayerHit(attackBox, player);
				hitChecked = true;
			}
		}
		updateAttackBox();
		updateAnimationTick();
	}

	private boolean isPlayerDetected(Player player) {
		float bossCenter = hitbox.x + hitbox.width / 2;
		float playerCenter = player.getHitbox().x + player.getHitbox().width / 2;
		return Math.abs(playerCenter - bossCenter) <= Game.TILES_SIZE * 12;
	}

	private boolean isPlayerInAttackRange(Player player) {
		float bossCenter = hitbox.x + hitbox.width / 2;
		float playerCenter = player.getHitbox().x + player.getHitbox().width / 2;
		return Math.abs(playerCenter - bossCenter) <= Game.TILES_SIZE * 5;
	}

	/** Lands the large boss on the top of the first solid tile below its feet. */
	private void updateBossInAir(int[][] lvlData) {
		if (CanMoveHere(hitbox.x, hitbox.y + airSpeed, hitbox.width, hitbox.height, lvlData)) {
			hitbox.y += airSpeed;
			airSpeed += GRAVITY;
			return;
		}

		int leftTile = Math.max(0, (int) (hitbox.x / Game.TILES_SIZE));
		int rightTile = Math.min(lvlData[0].length - 1,
				(int) ((hitbox.x + hitbox.width) / Game.TILES_SIZE));
		int firstSolidTile = (int) ((hitbox.y + hitbox.height) / Game.TILES_SIZE);
		while (firstSolidTile < lvlData.length
				&& !IsTileSolid(leftTile, firstSolidTile, lvlData)
				&& !IsTileSolid(rightTile, firstSolidTile, lvlData))
			firstSolidTile++;

		if (firstSolidTile < lvlData.length) {
			hitbox.y = firstSolidTile * Game.TILES_SIZE - hitbox.height - 1;
			tileY = (int) (hitbox.y / Game.TILES_SIZE);
			inAir = false;
			airSpeed = 0;
		}
	}

	private void startNextSkill(Player player) {
		effectTargetX = player.getHitbox().x + player.getHitbox().width / 2;
		if (!rageUsed && currentHealth <= maxHealth / 2) {
			state = MAGELLAN_RAGE;
			rageUsed = true;
		} else {
			state = attackPattern.remove();
			attackPattern.add(state);
		}
		aniTick = 0;
		aniIndex = 0;
		hitChecked = false;
		attackCooldown = 0;
	}

	private boolean isAttackState(int currentState) {
		return currentState >= MAGELLAN_SLASH && currentState <= MAGELLAN_RAGE;
	}

	@Override
	public void parry() {
		if (active && state != DEAD) {
			state = MAGELLAN_BLOCK;
			aniTick = 0;
			aniIndex = 0;
		}
	}

	private boolean isHitFrame() {
		return (state == MAGELLAN_SLASH && (aniIndex == 4 || aniIndex == 5))
				|| (state == MAGELLAN_LUNGE && aniIndex >= 3 && aniIndex <= 5)
				|| (state == MAGELLAN_SPIN_CUT && aniIndex >= 2 && aniIndex <= 7)
				|| (state == MAGELLAN_PISTOL && aniIndex == 3)
				|| (state == MAGELLAN_CANNON && aniIndex >= 3 && aniIndex <= 6)
				|| (state == MAGELLAN_SUMMON && aniIndex == 5)
				|| (state == MAGELLAN_RAGE && aniIndex >= 2 && aniIndex <= 5);
	}

	private void updateAttackBox() {
		float reach = state == MAGELLAN_PISTOL || state == MAGELLAN_CANNON ? 240 : 120;
		attackBox.width = reach * Game.SCALE;
		attackBox.x = walkDir == LEFT ? hitbox.x - attackBox.width : hitbox.x + hitbox.width;
		attackBox.y = hitbox.y + 12 * Game.SCALE;
		attackBox.height = state == MAGELLAN_RAGE ? 110 * Game.SCALE : 80 * Game.SCALE;
	}

	@Override
	protected void updateAnimationTick() {
		aniTick++;
		if (aniTick < animationFrameDelay())
			return;
		aniTick = 0;
		aniIndex++;
		String animation = animationName();
		if (aniIndex >= GetSpriteAmount(MAGELLAN, state)) {
			aniIndex = 0;
			if (state == DEAD)
				active = false;
			else if (isAttackState(state)) {
				attackCooldown = cooldownAfterSkill(state);
				state = IDLE;
			} else if (state == MAGELLAN_JUMP && inAir) {
				// Keep the jump animation looping until the boss lands.
				state = MAGELLAN_JUMP;
			} else if (state != RUNNING)
				state = IDLE;
		}
		// Resolve the name here so a malformed package fails at the boss, not later in rendering.
		if (!leftFrames.containsKey(animation))
			throw new IllegalStateException("Missing Magellan animation " + animation);
	}

	// The asset pack gives each animation its own FPS. This keeps movement and skills smooth.
	private int animationFrameDelay() {
		int fps = switch (state) {
		case IDLE -> 8;
		case RUNNING -> 14;
		case HIT -> 12;
		case DEAD -> 9;
		case MAGELLAN_BLOCK -> 12;
		case MAGELLAN_JUMP -> 10;
		case MAGELLAN_SLASH, MAGELLAN_LUNGE, MAGELLAN_SPIN_CUT -> 14;
		case MAGELLAN_PISTOL, MAGELLAN_RAGE -> 12;
		case MAGELLAN_CANNON, MAGELLAN_SUMMON -> 10;
		default -> 8;
		};
		return Math.max(1, Math.round((float) GAME_UPDATES_PER_SECOND / fps));
	}

	private int cooldownAfterSkill(int skill) {
		return switch (skill) {
		case MAGELLAN_SLASH -> 35;
		case MAGELLAN_LUNGE -> 45;
		case MAGELLAN_SPIN_CUT -> 55;
		case MAGELLAN_PISTOL -> 75;
		case MAGELLAN_CANNON -> 130;
		case MAGELLAN_SUMMON -> 110;
		case MAGELLAN_RAGE -> 90;
		default -> 50;
		};
	}

	private String animationName() {
		return switch (state) {
		case IDLE -> "idle";
		case HIT -> "hurt";
		case DEAD -> "die";
		case MAGELLAN_BLOCK -> "block_parry";
		case MAGELLAN_JUMP -> "jump";
		case RUNNING -> chasingPlayer ? "run" : "walk";
		case MAGELLAN_SLASH -> "attack_1_slash";
		case MAGELLAN_LUNGE -> "attack_2_lunge";
		case MAGELLAN_SPIN_CUT -> "attack_3_spin_cut";
		case MAGELLAN_PISTOL -> "skill_pistol_shot";
		case MAGELLAN_CANNON -> "skill_cannon_command";
		case MAGELLAN_SUMMON -> "skill_summon_crew";
		case MAGELLAN_RAGE -> "skill_rage";
		default -> "idle";
		};
	}

	public void draw(Graphics g, int xLvlOffset) {
		BufferedImage[] frames = walkDir == LEFT ? leftFrames.get(animationName()) : rightFrames.get(animationName());
		BufferedImage[] whiteFrames = walkDir == LEFT ? leftWhiteFrames.get(animationName())
				: rightWhiteFrames.get(animationName());
		BufferedImage frame = frames[Math.min(aniIndex, frames.length - 1)];
		if (flashTimer > 0)
			frame = whiteFrames[Math.min(aniIndex, whiteFrames.length - 1)];
		int drawX = (int) (hitbox.x - xLvlOffset - (walkDir == LEFT ? 48 : 64) * Game.SCALE);
		int drawY = (int) (hitbox.y + hitbox.height - (SPRITE_FEET_Y * Game.SCALE));
		g.drawImage(frame, drawX, drawY, MAGELLAN_WIDTH, MAGELLAN_HEIGHT, null);
		drawSkillEffects(g, xLvlOffset, drawX, drawY);
	}

	private BufferedImage[] whiteFrames(BufferedImage[] frames) {
		BufferedImage[] white = new BufferedImage[frames.length];
		for (int frame = 0; frame < frames.length; frame++)
			white[frame] = HitEffects.whiteSilhouette(frames[frame]);
		return white;
	}

	public int getHealth() {
		return currentHealth;
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	/** Draws the effect that belongs to the current animation frame. */
	private void drawSkillEffects(Graphics g, int xLvlOffset, int bossX, int bossY) {
		int frame = aniIndex;
		int direction = walkDir == LEFT ? -1 : 1;
		if (state == MAGELLAN_SLASH && frame >= 4)
			drawEffect(g, "slash_wave", frame - 4, bossX + direction * scaled(65), bossY + scaled(28), 88, 72, direction);
		if (state == MAGELLAN_LUNGE && frame >= 3 && frame <= 5)
			drawEffect(g, "thrust_streak", frame - 3, bossX + direction * scaled(30), bossY + scaled(42), 120, 28, direction);
		if (state == MAGELLAN_PISTOL) {
			if (frame == 3) drawEffect(g, "muzzle_flash", 1, bossX + direction * scaled(75), bossY + scaled(36), 56, 40, direction);
			if (frame >= 3 && frame <= 4) drawEffect(g, "bullet", frame - 3, (int) effectTargetX - xLvlOffset, bossY + scaled(45), 24, 10, direction);
			if (frame >= 5) drawEffect(g, "smoke_puff", Math.min(frame - 5, 5), bossX + direction * scaled(70), bossY + scaled(32), 48, 48, direction);
		}
		if (state == MAGELLAN_CANNON) {
			int target = (int) effectTargetX - xLvlOffset;
			if (frame <= 3) drawEffect(g, "target_marker", frame, target - scaled(48), (int) hitbox.y + scaled(62), 96, 30, 1);
			if (frame >= 4 && frame <= 5) drawEffect(g, "cannon_shell", frame - 4, target - scaled(14), (int) hitbox.y - scaled(85) + frame * scaled(18), 28, 56, 1);
			if (frame >= 6) {
				drawEffect(g, "cannon_explosion", frame - 6, target - scaled(56), (int) hitbox.y - scaled(18), 112, 112, 1);
				drawEffect(g, "shockwave", Math.min(frame - 6, 5), target - scaled(72), (int) hitbox.y + scaled(66), 144, 36, 1);
			}
		}
		if (state == MAGELLAN_SUMMON) {
			if (frame >= 3) drawEffect(g, "summon_sigil", Math.min(frame - 3, 9), bossX - scaled(72), bossY + scaled(78), 144, 48, 1);
			if (frame == 5) drawEffect(g, "spawn_flash", 3, bossX - scaled(36), bossY + scaled(28), 72, 72, 1);
		}
		if (state == MAGELLAN_RAGE)
			drawEffect(g, "rage_aura", Math.min(frame, 7), bossX, bossY, 160, 128, 1);
	}

	private void drawEffect(Graphics g, String name, int frame, int x, int y, int width, int height, int direction) {
		BufferedImage[] frames = effects.get(name);
		if (frames == null || frames.length == 0)
			return;
		BufferedImage image = frames[Math.max(0, Math.min(frame, frames.length - 1))];
		int scaledWidth = scaled(width);
		int scaledHeight = scaled(height);
		int drawWidth = direction < 0 ? -scaledWidth : scaledWidth;
		int drawX = direction < 0 ? x + scaledWidth : x;
		g.drawImage(image, drawX, y, drawWidth, scaledHeight, null);
	}

	@Override
	public void hurt(int amount) {
		// Rage animation is the boss's short invulnerable phase.
		if (state == MAGELLAN_RAGE && aniIndex <= 6)
			return;
		super.hurt(amount);
	}

	private int scaled(int pixels) {
		return (int) (pixels * Game.SCALE);
	}

	@Override
	public void resetEnemy() {
		super.resetEnemy();
		hitChecked = false;
		attackCooldown = 0;
		rageUsed = false;
		effectTargetX = hitbox.x;
		loadAttackPattern();
	}

	public int flipX() {
		return walkDir == RIGHT ? width : 0;
	}

	public int flipW() {
		return walkDir == RIGHT ? -1 : 1;
	}
}
