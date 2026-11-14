package background;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

import main.Game;
import utilz.LoadSave;

/**
 * Stage 1 daytime backdrop.
 *
 * The scene is built from separate layers and drawn in a fixed order:
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

    private final BackgroundLayer sky;
    private final List<BackgroundLayer> sceneryLayers;
    private final BackgroundLayer underground;
    private final TerrainAnalyzer terrainAnalyzer = new TerrainAnalyzer();

    private TerrainProfile terrain;

    public ParallaxBackground() {
        Path assets = LoadSave.GetProjectRoot().resolve("pananakop_modern_assets").resolve(ROOT);
        sky = new ParallaxLayer(load(assets.resolve("1_sky.png")), 0.0);
        sceneryLayers = List.of(
                new ParallaxLayer(load(assets.resolve("2_far_mountains.png")), 0.12, -65),
                new ParallaxLayer(load(assets.resolve("3_mid_jungle.png")), 0.30, -75),
                new ParallaxLayer(load(assets.resolve("4_near_jungle.png")), 0.58, -85),
                new ParallaxLayer(load(assets.resolve("5_front_mist.png")), 0.82, -85));
        underground = new UndergroundLayer(load(assets.resolve("6_underground_soil.png")));
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

    public void update() {
        // Static daytime scenery. Horizontal parallax follows the level camera.
    }

    public void draw(Graphics g, int cameraX, int[][] levelData) {
        Graphics2D g2 = (Graphics2D) g;
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
