package objects;

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Optional;

import entities.Player;
import main.Game;
import static utilz.Constants.ObjectConstants.*;

public abstract class Trap extends GameObject {
	private static final int ANIMATION_SPEED = 8;

	private final Type type;
	private BufferedImage[] frames;
	private int frameTick;
	private boolean touchingPlayer;

	protected Trap(int x, int y, int objType) {
		super(x, y, objType);
		type = Type.forObjectId(objType)
				.orElseThrow(() -> new IllegalArgumentException("Unknown trap type: " + objType));
		initHitbox(32, 32);
		hitbox.y -= Game.TILES_SIZE - (int) (8 * Game.SCALE);
	}

	public static Trap create(int x, int y, int objType) {
		Type type = Type.forObjectId(objType)
				.orElseThrow(() -> new IllegalArgumentException("Unknown trap type: " + objType));
		switch (type) {
		case BOUNCE_PAD:
			return new BambooBouncePad(x, y);
		case BAMBOO_SPIKES:
			return new BambooSpikeTrap(x, y);
		case BAMBOO_SPIKES_DOWN:
			return new BambooSpikeTrapDown(x, y);
		case BAMBOO_SPIKES_LEFT:
			return new BambooSpikeTrapLeft(x, y);
		case BAMBOO_SPIKES_RIGHT:
			return new BambooSpikeTrapRight(x, y);
		case BLOWGUN:
			return new BlowgunDartTrap(x, y);
		case CARNIVOROUS_FLOWER:
			return new CarnivorousFlowerTrap(x, y);
		case CRUMBLING_PLATFORM:
			return new CrumblingPlatformTrap(x, y);
		case FALLING_BOULDER:
			return new FallingBoulderTrap(x, y);
		case KAMPILAN_PENDULUM:
			return new KampilanPendulumTrap(x, y);
		case ROLLING_BOULDER:
			return new RollingBoulderTrap(x, y);
		case SAW_BLADE:
			return new SawBladeTrap(x, y);
		case SAW_RAIL:
			return new SawRailTrap(x, y);
		case SPANISH_CANNON:
			return new SpanishCannonTrap(x, y);
		case VOLCANIC_FIRE_JET:
			return new VolcanicFireJetTrap(x, y);
		default:
			throw new IllegalArgumentException("Unsupported trap type: " + type);
		}
	}

	public void setFrames(BufferedImage[] frames) {
		if (frames == null || frames.length == 0)
			throw new IllegalArgumentException("Trap animation must contain at least one frame.");
		this.frames = Arrays.copyOf(frames, frames.length);
	}

	public void update(Player player) {
		if (frames == null)
			throw new IllegalStateException("Trap animation has not been loaded: " + type.label);

		Rectangle2D.Float playerHitbox = player.getHitbox();
		Rectangle2D.Float activationArea = new Rectangle2D.Float(
				hitbox.x - Game.TILES_SIZE,
				hitbox.y - Game.TILES_SIZE,
				hitbox.width + Game.TILES_SIZE * 2,
				hitbox.height + Game.TILES_SIZE * 2);
		boolean nearby = activationArea.intersects(playerHitbox);
		if (nearby) {
			frameTick++;
			if (frameTick >= ANIMATION_SPEED) {
				frameTick = 0;
				aniIndex = (aniIndex + 1) % frames.length;
			}
		} else {
			frameTick = 0;
			aniIndex = 0;
		}

		boolean touching = hitbox.intersects(playerHitbox);
		if (touching && !touchingPlayer) {
			if (type.bouncePad) {
				int playerFeet = (int) (playerHitbox.y + playerHitbox.height);
				if (playerFeet <= y + (int) (8 * Game.SCALE))
					player.bounceFromTrap();
			} else
				player.changeHealth(-12);
		}
		touchingPlayer = touching;
	}

	public void draw(Graphics g, int xLvlOffset) {
		if (frames == null)
			throw new IllegalStateException("Trap animation has not been loaded: " + type.label);

		int drawWidth = (int) (type.frameWidth * Game.SCALE);
		int drawHeight = (int) (type.frameHeight * Game.SCALE);
		int drawX = x + (Game.TILES_SIZE - drawWidth) / 2 - xLvlOffset;
		int drawY = y + Game.TILES_SIZE - drawHeight;
		g.drawImage(frames[aniIndex], drawX, drawY, drawWidth, drawHeight, null);
	}

	@Override
	public void reset() {
		super.reset();
		frameTick = 0;
		touchingPlayer = false;
	}

	public static Type[] getTypes() {
		return Type.values();
	}

	public static Optional<Type> getType(int objectId) {
		return Type.forObjectId(objectId);
	}

	public static BufferedImage[] sliceFrames(BufferedImage strip, Type type) {
		if (strip.getHeight() != type.frameHeight || strip.getWidth() % type.frameWidth != 0)
			throw new IllegalArgumentException("Unexpected sprite-strip dimensions for " + type.label
					+ ": " + strip.getWidth() + "x" + strip.getHeight());

		int frameCount = strip.getWidth() / type.frameWidth;
		BufferedImage[] result = new BufferedImage[frameCount];
		for (int i = 0; i < frameCount; i++)
			result[i] = strip.getSubimage(i * type.frameWidth, 0, type.frameWidth, type.frameHeight);
		return result;
	}

	public enum Type {
		BOUNCE_PAD(TRAP_BOUNCE_PAD, "Bamboo bounce pad", "bamboo_bounce_pad_strip.png", 32, 32, true),
		BAMBOO_SPIKES(TRAP_BAMBOO_SPIKES, "Bamboo spikes", "bamboo_spike_trap_strip.png", 32, 32),
		BAMBOO_SPIKES_DOWN(TRAP_BAMBOO_SPIKES_DOWN, "Bamboo spikes down", "bamboo_spike_trap_down_strip.png", 32, 32),
		BAMBOO_SPIKES_LEFT(TRAP_BAMBOO_SPIKES_LEFT, "Bamboo spikes left", "bamboo_spike_trap_left_strip.png", 32, 32),
		BAMBOO_SPIKES_RIGHT(TRAP_BAMBOO_SPIKES_RIGHT, "Bamboo spikes right", "bamboo_spike_trap_right_strip.png", 32, 32),
		BLOWGUN(TRAP_BLOWGUN, "Blowgun dart trap", "blowgun_dart_trap_strip.png", 32, 32),
		CARNIVOROUS_FLOWER(TRAP_CARNIVOROUS_FLOWER, "Carnivorous flower", "carnivorous_flower_strip.png", 64, 56),
		CRUMBLING_PLATFORM(TRAP_CRUMBLING_PLATFORM, "Crumbling platform", "crumbling_platform_strip.png", 32, 28),
		FALLING_BOULDER(TRAP_FALLING_BOULDER, "Falling boulder", "falling_boulder_strip.png", 40, 40),
		KAMPILAN_PENDULUM(TRAP_KAMPILAN_PENDULUM, "Kampilan pendulum", "kampilan_pendulum_strip.png", 128, 128),
		ROLLING_BOULDER(TRAP_ROLLING_BOULDER, "Rolling boulder", "rolling_boulder_strip.png", 48, 48),
		SAW_BLADE(TRAP_SAW_BLADE, "Saw blade", "saw_blade_strip.png", 32, 32),
		SAW_RAIL(TRAP_SAW_RAIL, "Saw rail", "saw_rail_strip.png", 32, 32),
		SPANISH_CANNON(TRAP_SPANISH_CANNON, "Spanish cannon trap", "spanish_cannon_trap_strip.png", 48, 48),
		VOLCANIC_FIRE_JET(TRAP_VOLCANIC_FIRE_JET, "Volcanic fire jet", "volcanic_fire_jet_strip.png", 32, 112);

		public final int objectId;
		public final String label;
		public final String stripFile;
		public final int frameWidth;
		public final int frameHeight;
		public final boolean bouncePad;
		private static final Type[] TYPES = values();

		Type(int objectId, String label, String stripFile, int frameWidth, int frameHeight) {
			this(objectId, label, stripFile, frameWidth, frameHeight, false);
		}

		Type(int objectId, String label, String stripFile, int frameWidth, int frameHeight, boolean bouncePad) {
			this.objectId = objectId;
			this.label = label;
			this.stripFile = stripFile;
			this.frameWidth = frameWidth;
			this.frameHeight = frameHeight;
			this.bouncePad = bouncePad;
		}

		private static Optional<Type> forObjectId(int objectId) {
			for (Type type : TYPES)
				if (type.objectId == objectId)
					return Optional.of(type);
			return Optional.empty();
		}
	}
}
