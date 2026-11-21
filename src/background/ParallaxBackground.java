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
    private static final double LIGHT_SHAFT_FACTOR = 0.08;
    private static final double BIRD_FACTOR = 0.05;

    // Cloud bands, listed from the TOP of the sky down. Higher clouds look
    // nearer, so they move faster than the ones close to the horizon.
    private static final double[] CLOUD_FACTORS = { 0.06, 0.04, 0.03 };
    private static final double[] CLOUD_DRIFT_PX_PER_SECOND = { 9.0, 6.0, 4.0 };
    private static final double UPDATES_PER_SECOND = 200.0; // Game.UPS_SET

    // Haze over the far forest and the light beams: both are meant to be subtle.
    private static final Color HAZE_TINT = new Color(185, 196, 230);
    private static final float HAZE_STRENGTH = 0.45f;
    private static final float LIGHT_SHAFT_ALPHA = 0.14f;

    private final BackgroundLayer sky;
    private final List<BackgroundLayer> sceneryLayers;
    private final BackgroundLayer underground;
    private final BackgroundLayer fallbackBackground;
    /** Draw order = insertion order: sky, cloud bands, far forest (hazy), light shafts, near forest. */
    private final ArrayList<BackgroundLayer> layers = new ArrayList<>();
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
                layers.add(new SkyLayer(load(skyImg)));
                // Lowest (farthest) cloud band first, highest (nearest) last.
                List<BufferedImage> cloudBands = CloudLayer.splitIntoBands(load(cloudImg), CLOUD_FACTORS.length);
                for (int band = CLOUD_FACTORS.length - 1; band >= 0; band--)
                    layers.add(new CloudLayer(cloudBands.get(band), CLOUD_FACTORS[band],
                            CLOUD_DRIFT_PX_PER_SECOND[band] / UPDATES_PER_SECOND));
                layers.add(new BirdLayer(BIRD_FACTOR, UPDATES_PER_SECOND));
                layers.add(new ForestLayer(load(farImg), FAR_FOREST_FACTOR, HAZE_TINT, HAZE_STRENGTH));
                layers.add(new LightShaftLayer(LIGHT_SHAFT_FACTOR, LIGHT_SHAFT_ALPHA, UPDATES_PER_SECOND));
                layers.add(new ForestLayer(load(nearImg), NEAR_FOREST_FACTOR));
                layers.add(new ForestLifeLayer(UPDATES_PER_SECOND));
                fallbackBackground = null;
            } else {
                fallbackBackground = new ParallaxLayer(
                        load(bg.resolve("Full_bgx32.png")), 0.0);
            }
        }
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
        for (BackgroundLayer layer : layers)
            layer.update();
    }

    public void draw(Graphics g, int cameraX, int[][] levelData) {
        Graphics2D g2 = (Graphics2D) g;
        if (!layers.isEmpty()) {
            for (BackgroundLayer layer : layers)
                layer.draw(g2, cameraX);
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
