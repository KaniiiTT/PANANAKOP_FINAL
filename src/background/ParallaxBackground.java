package background;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import main.Game;
import utilz.LoadSave;

/**
 * Stage 1 daytime backdrop.
 *
 * The scene is built from separate layers and drawn in a fixed order:
 *  0. If the five Background/x32 layer files are present (sky, clouds, two
 *     forest layers) they are drawn as a polymorphic list of
 *     {@link BackgroundLayer}s in insertion order, with parallax scrolling.
 *     This replaces the old single static Full_bg image.
 *  1. sky, sun and clouds (whole screen);
 *  2. mountains, hills and trees, clipped to the area ABOVE the terrain;
 *  3. underground soil, clipped to the area BELOW the terrain.
 * Playing then draws the level tiles, objects, enemies and the player on top.
 *
 * Steps 2 and 3 use the same {@link TerrainProfile}, which is derived from
 * the current level's tile data, so the boundary follows the real ground
 * (steps, pits, different levels) instead of a fixed screen row.
 */
public class ParallaxBackground {
    private static final String ROOT = "backgrounds/day/";

    // Parallax factors: layerOffset = cameraOffset * factor.
    private static final double FAR_FOREST_FACTOR = 0.12;
    private static final double NEAR_FOREST_FACTOR = 0.30;
    // Distant scenery, from the farthest (slowest) to the middle forest, between the sky and the near forest.
    private static final double FAR_MOUNTAIN_FACTOR = 0.025;
    private static final double NEAR_MOUNTAIN_FACTOR = 0.045;
    private static final double HILLS_FACTOR = 0.075;
    private static final double MID_FOREST_FACTOR = 0.20;
    // The near forest is moved up by this many artwork pixels so its trunks and bushes stand on the ground line.
    private static final int NEAR_FOREST_LIFT = 38;
    private static final double LIGHT_SHAFT_FACTOR = 0.08;
    private static final double BIRD_FACTOR = 0.05;

    // Cloud bands, listed from the TOP of the sky down. Higher clouds look
    // nearer, so they move faster than the ones close to the horizon.
    private static final double[] CLOUD_FACTORS = { 0.06, 0.04, 0.03 };
    private static final double[] CLOUD_DRIFT_PX_PER_SECOND = { 9.0, 6.0, 4.0 };
    private static final double UPDATES_PER_SECOND = 200.0; // Game.UPS_SET

    // Day/night: one whole day lasts this many seconds of play (a new game starts in the morning).
    // For testing, start the game with -Dpananakop.daySeconds=60 to watch a whole day in one minute.
    private static final double DAY_SECONDS = Double.parseDouble(System.getProperty("pananakop.daySeconds", "600"));
    private static final double START_TIME_OF_DAY = 0.14; // a new game starts in clear daylight

    // Vertical parallax: how much each layer follows the player's up/down movement
    // (pixels of layer shift per pixel of player movement; closer layers move more).
    private static final double[] CLOUD_VERTICAL = { 0.020, 0.015, 0.010 };
    private static final double BIRD_VERTICAL = 0.020;
    private static final double BAT_VERTICAL = 0.040;
    private static final double FAR_FOREST_VERTICAL = 0.040;
    private static final double FAR_MOUNTAIN_VERTICAL = 0.022;
    private static final double NEAR_MOUNTAIN_VERTICAL = 0.028;
    private static final double HILLS_VERTICAL = 0.034;
    private static final double MID_FOREST_VERTICAL = 0.065;
    private static final double LIGHT_SHAFT_VERTICAL = 0.050;
    private static final double NEAR_FOREST_VERTICAL = 0.090;
    private static final double BAT_FACTOR = 0.10;

    // The viewer position is eased so the background glides instead of jerking, and a slower
    // "rest position" follows it, so after a jump or fall the scene settles back to its normal look.
    private static final double FOCUS_FOLLOW = 0.030;   // per update
    private static final double REST_FOLLOW = 0.0008;   // per update (about 6 s)
    private static final double TELEPORT_DISTANCE = Game.GAME_HEIGHT * 0.25; // a bigger jump is a respawn

    // Wind sway of the near bushes: pixels the tree tops lean at full wind (0 turns the sway off).
    private static final int NEAR_FOREST_SWAY = Math.max(2, Math.round(Game.GAME_WIDTH / 480f));

    // Haze over the far forest and the light beams: both are meant to be subtle.
    private static final Color HAZE_TINT = new Color(185, 196, 230);
    private static final float HAZE_STRENGTH = 0.45f;
    private static final float MID_HAZE_STRENGTH = 0.18f;
    private static final Color MOUNTAIN_NIGHT_TINT = new Color(18, 26, 70); // the colour of the night sky
    private static final float LIGHT_SHAFT_ALPHA = 0.14f;

    private final BackgroundLayer sky;
    private final List<BackgroundLayer> sceneryLayers;
    private final BackgroundLayer underground;
    private final BackgroundLayer fallbackBackground;
    /**
     * Draw order = insertion order: sky, night sky (stars, moon), shooting stars, cloud bands,
     * birds, far mountains, near mountains, hills, far forest (hazy, with palms), bats, light shafts, middle forest, near forest (swaying, with ferns), forest life,
     * mood tint, fireflies.
     */
    private final ArrayList<BackgroundLayer> layers = new ArrayList<>();
    private final DayNightCycle cycle = new DayNightCycle(DAY_SECONDS, START_TIME_OF_DAY, UPDATES_PER_SECOND);
    // The synthesized wind/birds/crickets (AmbientSoundscape) are no longer played here: the audio engine now plays
    // the environment ambience files per area, and running both put two ambiences on top of the music.

    // Vertical parallax state, in screen pixels of player height.
    private double focusTarget;   // where the player really is
    private double focusEased;    // the same, glided towards
    private double restPosition;  // where the scene is "at rest"
    private boolean focusKnown;
    private final TerrainAnalyzer terrainAnalyzer = new TerrainAnalyzer();

    private TerrainProfile terrain;

    public ParallaxBackground() {
        Path assetsRoot = LoadSave.GetProjectRoot().resolve("pananakop_modern_assets");
        Path assets = assetsRoot.resolve(ROOT);
        Path skyPath = assets.resolve("1_sky.png");
        Path mountainsPath = assets.resolve("2_far_mountains.png");
        Path midJunglePath = assets.resolve("3_mid_jungle.png");
        Path nearJunglePath = assets.resolve("4_near_jungle.png");
        Path mistPath = assets.resolve("5_front_mist.png");
        Path soilPath = assets.resolve("6_underground_soil.png");

        if (Files.isRegularFile(skyPath) && Files.isRegularFile(mountainsPath)
                && Files.isRegularFile(midJunglePath) && Files.isRegularFile(nearJunglePath)
                && Files.isRegularFile(mistPath) && Files.isRegularFile(soilPath)) {
            sky = new ParallaxLayer(load(skyPath), 0.0);
            sceneryLayers = List.of(
                    new ParallaxLayer(load(mountainsPath), 0.12, -65),
                    new ParallaxLayer(load(midJunglePath), 0.30, -75),
                    new ParallaxLayer(load(nearJunglePath), 0.58, -85),
                    new ParallaxLayer(load(mistPath), 0.82, -85));
            underground = new UndergroundLayer(load(soilPath));
            fallbackBackground = null;
        } else {
            sky = null;
            sceneryLayers = List.of();
            underground = null;
            Path bg = assetsRoot.resolve("Background/x32");
            Path skyImg = bg.resolve("Skyx32.png");
            Path cloudImg = bg.resolve("Clouds_x32.png");
            Path farImg = bg.resolve("Flora1x32.png");
            Path nearImg = bg.resolve("Flora2x32.png");
            if (Files.isRegularFile(skyImg) && Files.isRegularFile(cloudImg)
                    && Files.isRegularFile(farImg) && Files.isRegularFile(nearImg)) {
                // The order of insertion is the order of drawing (back to front).
                BufferedImage skyArt = SkyLayer.withoutClouds(load(skyImg));
                layers.add(new SkyLayer(skyArt));
                layers.add(new NightSkyLayer(cycle, skyArt, UPDATES_PER_SECOND));
                layers.add(new ShootingStarLayer(cycle, UPDATES_PER_SECOND));
                // Lowest (farthest) cloud band first, highest (nearest) last.
                List<BufferedImage> cloudBands = CloudLayer.splitIntoBands(load(cloudImg), CLOUD_FACTORS.length);
                for (int band = CLOUD_FACTORS.length - 1; band >= 0; band--)
                    layers.add(new CloudLayer(cloudBands.get(band), CLOUD_FACTORS[band], CLOUD_VERTICAL[band],
                            CLOUD_DRIFT_PX_PER_SECOND[band] / UPDATES_PER_SECOND, cycle));
                layers.add(new BirdLayer(cycle, BIRD_FACTOR, BIRD_VERTICAL, UPDATES_PER_SECOND));
                // Distant scenery: two mountain ranges and rolling hills, then the far forest with palms.
                layers.add(new MountainLayer(TropicalScenery.farMountains(), FAR_MOUNTAIN_FACTOR,
                        FAR_MOUNTAIN_VERTICAL, cycle, MOUNTAIN_NIGHT_TINT, 0.58f));
                layers.add(new MountainLayer(TropicalScenery.nearMountains(), NEAR_MOUNTAIN_FACTOR,
                        NEAR_MOUNTAIN_VERTICAL, cycle, MOUNTAIN_NIGHT_TINT, 0.68f));
                layers.add(new ForestLayer(TropicalScenery.hills(), HILLS_FACTOR, HILLS_VERTICAL, HAZE_TINT, 0.20f));
                layers.add(new ForestLayer(TropicalScenery.withPalms(load(farImg)), FAR_FOREST_FACTOR,
                        FAR_FOREST_VERTICAL, HAZE_TINT, HAZE_STRENGTH));
                layers.add(new BatLayer(cycle, BAT_FACTOR, BAT_VERTICAL, UPDATES_PER_SECOND));
                layers.add(new LightShaftLayer(LIGHT_SHAFT_FACTOR, LIGHT_SHAFT_VERTICAL, LIGHT_SHAFT_ALPHA,
                        UPDATES_PER_SECOND, cycle));
                // Middle forest: banana plants, broad leaves and varied trees, between the far and near forest.
                layers.add(new ForestLayer(TropicalScenery.midForest(), MID_FOREST_FACTOR, MID_FOREST_VERTICAL,
                        HAZE_TINT, MID_HAZE_STRENGTH));
                SwayingForestLayer nearForest = new SwayingForestLayer(TropicalScenery.raised(TropicalScenery.withFerns(load(nearImg)), NEAR_FOREST_LIFT),
                        NEAR_FOREST_FACTOR,
                        NEAR_FOREST_VERTICAL, NEAR_FOREST_SWAY, UPDATES_PER_SECOND);
                layers.add(nearForest);
                layers.add(new ForestLifeLayer(cycle, nearForest, NEAR_FOREST_VERTICAL, UPDATES_PER_SECOND));
                layers.add(new MoodTintLayer(cycle));
                // After the tint, so the glow is not darkened by the night colour.
                layers.add(new FireflyLayer(cycle, NEAR_FOREST_VERTICAL, UPDATES_PER_SECOND));
                fallbackBackground = null;
            } else {
                fallbackBackground = new ParallaxLayer(
                        load(bg.resolve("Full_bgx32.png")), 0.0);
            }
        }
    }

    /** The day/night clock, for example to jump to night for a boss fight. */
    public DayNightCycle getCycle() {
        return cycle;
    }

    private BufferedImage load(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null)
                throw new IOException("Unsupported image format");
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load background layer: " + path, e);
        }
    }

    /** Called from the game's update loop; lets self-moving layers (clouds) advance. */
    public void update() {
        cycle.update();
        focusEased += (focusTarget - focusEased) * FOCUS_FOLLOW;
        restPosition += (focusEased - restPosition) * REST_FOLLOW;
        for (BackgroundLayer layer : layers)
            layer.update();
    }

    /**
     * Tells the backdrop where the player is vertically (screen pixels, larger = lower).
     * Call it every update. The layers shift a little against the player's movement,
     * the closer ones more, and glide back to their normal place when the player
     * stays at one height. A sudden big jump (a respawn) is followed at once.
     */
    public void setFocusY(double y) {
        if (!focusKnown || Math.abs(y - focusTarget) > TELEPORT_DISTANCE) {
            focusEased = y;
            restPosition = y;
            focusKnown = true;
        }
        focusTarget = y;
    }

    /** Shift in pixels for a layer: it moves against the player, scaled by its own factor. */
    private int verticalShift(BackgroundLayer layer) {
        double factor = layer.getVerticalFactor();
        if (factor == 0.0)
            return 0;
        double shift = (restPosition - focusEased) * factor;
        double limit = BackgroundLayer.MAX_VERTICAL_SHIFT;
        return (int) Math.round(Math.max(-limit, Math.min(limit, shift)));
    }

    public void draw(Graphics g, int cameraX, int[][] levelData) {
        Graphics2D g2 = (Graphics2D) g;
        if (!layers.isEmpty()) {
            for (BackgroundLayer layer : layers) {
                int shift = verticalShift(layer);
                if (shift != 0)
                    g2.translate(0, shift);
                layer.draw(g2, cameraX);
                if (shift != 0)
                    g2.translate(0, -shift);
            }
            return;
        }
        if (fallbackBackground != null) {
            fallbackBackground.draw(g2, cameraX);
            return;
        }
        TerrainProfile profile = terrainFor(levelData);
        int tile = Game.TILES_SIZE;

        // 1) Sky stays visible across the entire scene.
        sky.draw(g2, cameraX);

        // 2) Natural scenery, only above the terrain surface.
        drawClipped(g2, profile.aboveGroundClip(cameraX, tile, Game.GAME_WIDTH, Game.GAME_HEIGHT),
                sceneryLayers, cameraX);

        // 3) Soil, only below the terrain surface (open pits included).
        drawClipped(g2, profile.undergroundClip(cameraX, tile, Game.GAME_WIDTH, Game.GAME_HEIGHT),
                List.of(underground), cameraX);
    }

    /** Analyses the level once and reuses the result until the level changes. */
    private TerrainProfile terrainFor(int[][] levelData) {
        if (terrain == null || !terrain.isBuiltFrom(levelData))
            terrain = terrainAnalyzer.analyze(levelData);
        return terrain;
    }

    private void drawClipped(Graphics2D g, Shape clip, List<BackgroundLayer> layers, int cameraX) {
        Shape originalClip = g.getClip();
        try {
            g.clip(clip);
            for (BackgroundLayer layer : layers)
                layer.draw(g, cameraX);
        } finally {
            g.setClip(originalClip);
        }
    }
}
