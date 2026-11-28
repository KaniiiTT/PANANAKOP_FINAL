package utilz;

import main.Game;

public class Constants {

	public static final float GRAVITY = 0.04f * Game.SCALE;
	public static final int ANI_SPEED = 25;
	/** Frame interval for the boar's faster movement, attack, and hurt animations. */
	public static final int BOAR_ANI_SPEED = 15;
	/** Frame interval for the Conquistador's combat animations. */
	public static final int CONQUISTADOR_ANI_SPEED = 15;
	/** Longer interval for holding the first Conquistador animation frame. */
	public static final int CONQUISTADOR_FIRST_FRAME_ANI_SPEED = 50;
	/** Zero-based animation index for the Conquistador's held attack frame 4. */
	public static final int CONQUISTADOR_ATTACK_HOLD_FRAME = 3;
	/** Longer interval for holding the first Conquistador walk frame. */
	public static final int CONQUISTADOR_WALK_FIRST_FRAME_ANI_SPEED = 30;
	
	public static class Projectiles{
		public static final int CANNON_BALL_DEFAULT_WIDTH = 15;
		public static final int CANNON_BALL_DEFAULT_HEIGHT = 15;
		
		public static final int CANNON_BALL_WIDTH = (int)(Game.SCALE * CANNON_BALL_DEFAULT_WIDTH);
		public static final int CANNON_BALL_HEIGHT = (int)(Game.SCALE * CANNON_BALL_DEFAULT_HEIGHT);
		public static final float SPEED = 0.75f * Game.SCALE;
	}

	public static class ObjectConstants {

		public static final int RED_POTION = 0;
		public static final int BLUE_POTION = 1;
		public static final int BARREL = 2;
		public static final int BOX = 3;
		public static final int SPIKE = 4;
		public static final int CANNON_LEFT = 5;
		public static final int CANNON_RIGHT = 6;
		public static final int TRAP_BOUNCE_PAD = 7;
		public static final int TRAP_BAMBOO_SPIKES = 8;
		public static final int TRAP_BAMBOO_SPIKES_DOWN = 9;
		public static final int TRAP_BAMBOO_SPIKES_LEFT = 10;
		public static final int TRAP_BAMBOO_SPIKES_RIGHT = 11;
		public static final int TRAP_BLOWGUN = 12;
		public static final int TRAP_CARNIVOROUS_FLOWER = 13;
		public static final int TRAP_CRUMBLING_PLATFORM = 14;
		public static final int TRAP_FALLING_BOULDER = 15;
		public static final int TRAP_KAMPILAN_PENDULUM = 16;
		public static final int TRAP_ROLLING_BOULDER = 17;
		public static final int TRAP_SAW_BLADE = 18;
		public static final int TRAP_SAW_RAIL = 19;
		public static final int TRAP_SPANISH_CANNON = 20;
		public static final int TRAP_VOLCANIC_FIRE_JET = 21;

		public static final int RED_POTION_VALUE = 15;
		public static final int BLUE_POTION_VALUE = 10;

		public static final int CONTAINER_WIDTH_DEFAULT = 40;
		public static final int CONTAINER_HEIGHT_DEFAULT = 30;
		public static final int CONTAINER_WIDTH = (int) (Game.SCALE * CONTAINER_WIDTH_DEFAULT);
		public static final int CONTAINER_HEIGHT = (int) (Game.SCALE * CONTAINER_HEIGHT_DEFAULT);

		public static final int POTION_WIDTH_DEFAULT = 12;
		public static final int POTION_HEIGHT_DEFAULT = 16;
		public static final int POTION_WIDTH = (int) (Game.SCALE * POTION_WIDTH_DEFAULT);
		public static final int POTION_HEIGHT = (int) (Game.SCALE * POTION_HEIGHT_DEFAULT);

		public static final int SPIKE_WIDTH_DEFAULT = 32;
		public static final int SPIKE_HEIGHT_DEFAULT = 32;
		public static final int SPIKE_WIDTH = (int) (Game.SCALE * SPIKE_WIDTH_DEFAULT);
		public static final int SPIKE_HEIGHT = (int) (Game.SCALE * SPIKE_HEIGHT_DEFAULT);

		public static final int CANNON_WIDTH_DEFAULT = 40;
		public static final int CANNON_HEIGHT_DEFAULT = 26;
		public static final int CANNON_WIDTH = (int) (CANNON_WIDTH_DEFAULT * Game.SCALE);
		public static final int CANNON_HEIGHT = (int) (CANNON_HEIGHT_DEFAULT * Game.SCALE);

		public static int GetSpriteAmount(int object_type) {
			switch (object_type) {
			case RED_POTION, BLUE_POTION:
				return 7;
			case BARREL, BOX:
				return 8;
			case CANNON_LEFT, CANNON_RIGHT:
				return 7;
			}
			return 1;
		}
	}

	public static class EnemyConstants {
		public static final int CRABBY = 0;
		/** Green channel marker used by the editor and level loader for Magellan. */
		public static final int MAGELLAN = 1;
		/** Green channel marker used by the editor and level loader for Boar. */
		public static final int BOAR = 2;
		/** Green channel marker used by the editor and level loader for Conquistador. */
		public static final int CONQUISTADOR = 3;
		/** Green channel marker used by the editor and level loader for Espinosa. */
		public static final int ESPINOSA = 4;

		public static final int IDLE = 0;
		public static final int RUNNING = 1;
		public static final int ATTACK = 2;
		public static final int HIT = 3;
		public static final int DEAD = 4;
		public static final int MAGELLAN_SLASH = 5;
		public static final int MAGELLAN_LUNGE = 6;
		public static final int MAGELLAN_SPIN_CUT = 7;
		public static final int MAGELLAN_PISTOL = 8;
		public static final int MAGELLAN_CANNON = 9;
		public static final int MAGELLAN_SUMMON = 10;
		public static final int MAGELLAN_RAGE = 11;
		public static final int MAGELLAN_BLOCK = 12;
		public static final int MAGELLAN_JUMP = 13;

		public static final int CRABBY_WIDTH_DEFAULT = 72;
		public static final int CRABBY_HEIGHT_DEFAULT = 32;

		public static final int CRABBY_WIDTH = (int) (CRABBY_WIDTH_DEFAULT * Game.SCALE);
		public static final int CRABBY_HEIGHT = (int) (CRABBY_HEIGHT_DEFAULT * Game.SCALE);

		public static final int CRABBY_DRAWOFFSET_X = (int) (26 * Game.SCALE);
		public static final int CRABBY_DRAWOFFSET_Y = (int) (9 * Game.SCALE);
		public static final int BOAR_WIDTH_DEFAULT = 96;
		public static final int BOAR_HEIGHT_DEFAULT = 64;
		public static final int BOAR_WIDTH = (int) (BOAR_WIDTH_DEFAULT * Game.SCALE);
		public static final int BOAR_HEIGHT = (int) (BOAR_HEIGHT_DEFAULT * Game.SCALE);
		public static final int BOAR_DRAWOFFSET_X = (int) (16 * Game.SCALE);
		/** Horizontal distance between boar/player hitbox origins when the boar attacks. */
		public static final int BOAR_ATTACK_DISTANCE_DEFAULT = 34;
		// The sprite has transparent padding below its feet; align the visible feet
		// with the bottom of the boar hitbox instead of the marker's top edge.
		public static final int BOAR_DRAWOFFSET_Y = (int) (31 * Game.SCALE);
		public static final int CONQUISTADOR_WIDTH_DEFAULT = 96;
		public static final int CONQUISTADOR_HEIGHT_DEFAULT = 64;
		public static final int CONQUISTADOR_WIDTH = (int) (CONQUISTADOR_WIDTH_DEFAULT * Game.SCALE);
		public static final int CONQUISTADOR_HEIGHT = (int) (CONQUISTADOR_HEIGHT_DEFAULT * Game.SCALE);
		public static final int CONQUISTADOR_DRAWOFFSET_X = (int) (16 * Game.SCALE);
		public static final int CONQUISTADOR_DRAWOFFSET_Y = (int) (32 * Game.SCALE);
		public static final int CONQUISTADOR_ATTACK_DISTANCE_DEFAULT = 64;
		/** Center-to-center distance at which the Conquistador stops and attacks. */
		public static final int CONQUISTADOR_STOP_DISTANCE_DEFAULT = 84;
		/** Cooldown between regular enemy attacks, measured in game updates. */
		public static final int ENEMY_ATTACK_COOLDOWN_TICKS = 320;
		/** Pause after the Conquistador reaches a patrol boundary, in game updates. */
		public static final int CONQUISTADOR_PATROL_IDLE_TICKS = 300;
		public static final int MAGELLAN_WIDTH_DEFAULT = 160;
		public static final int MAGELLAN_HEIGHT_DEFAULT = 128;
		public static final int MAGELLAN_WIDTH = (int) (MAGELLAN_WIDTH_DEFAULT * Game.SCALE);
		public static final int MAGELLAN_HEIGHT = (int) (MAGELLAN_HEIGHT_DEFAULT * Game.SCALE);

		public static int GetSpriteAmount(int enemy_type, int enemy_state) {

			switch (enemy_type) {
			case CRABBY:
				switch (enemy_state) {
				case IDLE:
					return 9;
				case RUNNING:
					return 6;
				case ATTACK:
					return 7;
				case HIT:
					return 4;
				case DEAD:
					return 5;
				}
				return 0;
			case MAGELLAN:
				return switch (enemy_state) {
				case IDLE -> 8;
				case RUNNING -> 8;
				case ATTACK -> 8;
				case HIT -> 4;
				case DEAD -> 12;
				case MAGELLAN_SLASH -> 8;
				case MAGELLAN_LUNGE -> 8;
				case MAGELLAN_SPIN_CUT -> 10;
				case MAGELLAN_PISTOL -> 8;
				case MAGELLAN_CANNON -> 10;
				case MAGELLAN_SUMMON -> 11;
				case MAGELLAN_RAGE -> 8;
				case MAGELLAN_BLOCK -> 5;
				case MAGELLAN_JUMP -> 5;
				default -> 1;
				};
			case BOAR:
				return switch (enemy_state) {
				case ATTACK, HIT, DEAD -> 9;
				default -> 10;
				};
			case CONQUISTADOR:
				return switch (enemy_state) {
				case IDLE, RUNNING -> 8;
				case ATTACK -> 9;
				case HIT -> 4;
				case DEAD -> 8;
				default -> 1;
				};
			}

			return 0;

		}

		public static int GetMaxHealth(int enemy_type) {
			switch (enemy_type) {
			case CRABBY:
				return 10;
			case MAGELLAN:
				return 300;
			case BOAR:
				return 20;
			case CONQUISTADOR:
				return 30;
			default:
				return 1;
			}
		}

		public static int GetEnemyDmg(int enemy_type) {
			switch (enemy_type) {
			case CRABBY:
				return 15;
			case MAGELLAN:
				return 30;
			case BOAR:
				return 20;
			case CONQUISTADOR:
				return 25;
			default:
				return 0;
			}

		}

	}

	public static class Environment {
		public static final int BIG_CLOUD_WIDTH_DEFAULT = 448;
		public static final int BIG_CLOUD_HEIGHT_DEFAULT = 101;
		public static final int SMALL_CLOUD_WIDTH_DEFAULT = 74;
		public static final int SMALL_CLOUD_HEIGHT_DEFAULT = 24;

		public static final int BIG_CLOUD_WIDTH = (int) (BIG_CLOUD_WIDTH_DEFAULT * Game.SCALE);
		public static final int BIG_CLOUD_HEIGHT = (int) (BIG_CLOUD_HEIGHT_DEFAULT * Game.SCALE);
		public static final int SMALL_CLOUD_WIDTH = (int) (SMALL_CLOUD_WIDTH_DEFAULT * Game.SCALE);
		public static final int SMALL_CLOUD_HEIGHT = (int) (SMALL_CLOUD_HEIGHT_DEFAULT * Game.SCALE);
	}

	public static class UI {
		public static class Buttons {
			public static final int B_WIDTH_DEFAULT = 140;
			public static final int B_HEIGHT_DEFAULT = 56;
			public static final int B_WIDTH = (int) (B_WIDTH_DEFAULT * Game.SCALE);
			public static final int B_HEIGHT = (int) (B_HEIGHT_DEFAULT * Game.SCALE);
		}

		public static class PauseButtons {
			public static final int SOUND_SIZE_DEFAULT = 42;
			public static final int SOUND_SIZE = (int) (SOUND_SIZE_DEFAULT * Game.SCALE);
		}

		public static class URMButtons {
			public static final int URM_DEFAULT_SIZE = 56;
			public static final int URM_SIZE = (int) (URM_DEFAULT_SIZE * Game.SCALE);

		}

		public static class VolumeButtons {
			public static final int VOLUME_DEFAULT_WIDTH = 28;
			public static final int VOLUME_DEFAULT_HEIGHT = 44;
			public static final int SLIDER_DEFAULT_WIDTH = 215;

			public static final int VOLUME_WIDTH = (int) (VOLUME_DEFAULT_WIDTH * Game.SCALE);
			public static final int VOLUME_HEIGHT = (int) (VOLUME_DEFAULT_HEIGHT * Game.SCALE);
			public static final int SLIDER_WIDTH = (int) (SLIDER_DEFAULT_WIDTH * Game.SCALE);
		}
	}

	public static class Directions {
		public static final int LEFT = 0;
		public static final int UP = 1;
		public static final int RIGHT = 2;
		public static final int DOWN = 3;
	}

	public static class PlayerConstants {
		/** Vertical render adjustment for aligning the player's feet with the ground. */
		public static final int PLAYER_SPRITE_OFFSET_Y = 2;
		/** Shared collision dimensions for both male and female player sprites. */
		public static final int PLAYER_HITBOX_WIDTH = 20;
		public static final int PLAYER_HITBOX_HEIGHT = 27;
		public static final int IDLE = 0;
		public static final int WALKING = 1;
		public static final int RUNNING = 2;
		public static final int RUN_START_FRAME = 0;
		public static final int ATTACK = 3;
		public static final int BLOCK = 4;
		public static final int HIT = 5;
		public static final int GROUND = 6;
		public static final int JUMP = 7;
		public static final int DOUBLE_JUMP = 8;
		public static final int FALLING = 9;
		public static final int LANDING = 10;
		public static final int DEAD = 11;
		public static final int VICTORY = 12;
		public static final int DASH = 13;
		public static final int ULTIMATE = 14;
		/** Game updates per second used to convert continuation animation timings. */
		public static final int PLAYER_UPDATES_PER_SECOND = 200;
		public static final boolean LOCK_MOVEMENT_DURING_ATTACK = true;
		/** Forward movement during each attack phase, in unscaled sprite pixels. */
		public static final float ATTACK_LUNGE_DISTANCE = 18f;
		public static final float ATTACK_LUNGE_SPEED = 1.5f;
		/** Tuning multiplier for continuation-sheet animations only. */
		public static final float CONTINUATION_SPEED_MULTIPLIER = 1.0f;
		public static final float ATTACK_HORIZONTAL_FRAME_SECONDS = 0.07f;
		public static final float ATTACK_VERTICAL_FRAME_SECONDS = 0.08f;
		public static final float RUNNING_FRAME_SECONDS = 0.08f;
		public static final float DASH_FRAME_SECONDS = 0.05f;
		public static final float ULTIMATE_FRAME_SECONDS = 0.09f;

		public static int continuationFrameTicks(float seconds) {
			return Math.max(1, Math.round(seconds * PLAYER_UPDATES_PER_SECOND / CONTINUATION_SPEED_MULTIPLIER));
		}

		public static int GetSpriteAmount(int player_action) {
			int[] frameCounts = { 9, 9, 9, 13, 5, 5, 9, 5, 9, 5, 5, 9, 9, 5, 9 };
			if (player_action < 0 || player_action >= frameCounts.length)
				return 1;
			return frameCounts[player_action];
		}

		public static final int ATTACK_CHAIN_GRACE_TICKS = 80;

		public static final AttackDefinition ATTACK_HORIZONTAL = new AttackDefinition(
				0, 7, 2, 4, continuationFrameTicks(0.055f), 10, 64, 32, 0, 8, 3);
		public static final AttackDefinition ATTACK_VERTICAL = new AttackDefinition(
				8, 15, 2, 4, continuationFrameTicks(0.055f), 20, 80, 40, 0, 4, 7);
		public static final UltimateHitDefinition[] ULTIMATE_HITS = {
				new UltimateHitDefinition(7, 10, 96, 96, 0, 0, false),
				new UltimateHitDefinition(8, 10, 96, 96, 0, 0, false),
				new UltimateHitDefinition(9, 10, 96, 96, 0, 0, false),
				new UltimateHitDefinition(10, 10, 96, 96, 0, 0, false),
				new UltimateHitDefinition(11, 10, 96, 96, 0, 0, false),
				new UltimateHitDefinition(12, 20, 96, 96, 0, 0, true)
		};

		public static class AttackDefinition {
			public final int startFrame;
			public final int endFrame;
			public final int hitActiveStart;
			public final int hitActiveEnd;
			public final int frameDuration;
			public final int damage;
			public final int hitboxWidth;
			public final int hitboxHeight;
			public final int hitboxOffsetX;
			public final int hitboxOffsetY;
			public final int knockback;

			private AttackDefinition(int startFrame, int endFrame, int hitActiveStart, int hitActiveEnd,
					int frameDuration, int damage, int hitboxWidth, int hitboxHeight, int hitboxOffsetX,
					int hitboxOffsetY, int knockback) {
				this.startFrame = startFrame;
				this.endFrame = endFrame;
				this.hitActiveStart = hitActiveStart;
				this.hitActiveEnd = hitActiveEnd;
				this.frameDuration = frameDuration;
				this.damage = damage;
				this.hitboxWidth = hitboxWidth;
				this.hitboxHeight = hitboxHeight;
				this.hitboxOffsetX = hitboxOffsetX;
				this.hitboxOffsetY = hitboxOffsetY;
				this.knockback = knockback;
			}
		}

		public static class UltimateHitDefinition {
			public final int frame;
			public final int damage;
			public final int hitboxWidth;
			public final int hitboxHeight;
			public final int hitboxOffsetX;
			public final int hitboxOffsetY;
			public final boolean heavy;

			private UltimateHitDefinition(int frame, int damage, int hitboxWidth, int hitboxHeight,
					int hitboxOffsetX, int hitboxOffsetY, boolean heavy) {
				this.frame = frame;
				this.damage = damage;
				this.hitboxWidth = hitboxWidth;
				this.hitboxHeight = hitboxHeight;
				this.hitboxOffsetX = hitboxOffsetX;
				this.hitboxOffsetY = hitboxOffsetY;
				this.heavy = heavy;
			}
		}
	}

}
