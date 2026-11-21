package background;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import main.Game;

/**
 * The living foreground of the forest: pollen, falling leaves and butterflies.
 *
 * It is drawn in front of the near forest. Each kind of creature has its own
 * depth (parallax factor); the closer ones follow the camera more. Pollen and
 * butterflies are created once and never go away. Leaves come from a small
 * pool and are woken up at random intervals.
 */
public class ForestLifeLayer extends AmbientLayer {
    // Depth of each kind of creature (higher = closer to the viewer).
    private static final double POLLEN_FACTOR = 0.22;
    private static final double LEAF_FACTOR = 0.28;
    private static final double BUTTERFLY_FACTOR = 0.38;

    private static final int POLLEN_COUNT = 28;
    private static final int BUTTERFLY_COUNT = 5;
    private static final int LEAF_POOL_SIZE = 8;

    private static final String[][] LEAF_FRAMES = {
            { ".aa.", "aaaa", ".aa." },
            { "aa..", ".aaa", "..aa" },
            { ".a.", "aaa", "aaa", ".a." },
            { "..aa", "aaa.", "aa.." } };
    // Warmer and lighter than the bushes, so the leaves stand out against the greenery.
    private static final int[] LEAF_COLORS = { 0xFFB4CC5C, 0xFFE0D470, 0xFFD89448, 0xFFA8D090 };

    // Nine-wide frames: wings open, half open, folded. 'a' = wing, 'b' = body.
    private static final String[][] BUTTERFLY_FRAMES = {
            { "aaa.b.aaa", "aaaabaaaa", ".aaabaaa.", "..aabaa.." },
            { "..a.b.a..", "..aabaa..", "..aabaa..", "...aba..." },
            { "....b....", "...aba...", "...aba...", "....b...." } };
    private static final int BUTTERFLY_BODY = 0xFF3C2E2A;
    private static final int[] BUTTERFLY_WINGS = { 0xFFF0D25A, 0xFFEB9646, 0xFF96BEF0, 0xFFF0F0F5 };

    private final ArrayList<BufferedImage[]> leafSprites = new ArrayList<>();
    private double secondsUntilNextLeaf;

    public ForestLifeLayer(DayNightCycle cycle, double updatesPerSecond) {
        super(updatesPerSecond);
        int unit = PixelSprites.pixelUnit();
        int small = Math.max(1, unit - 1);
        int w = Game.GAME_WIDTH;
        int h = Game.GAME_HEIGHT;

        // Back to front: pollen, leaves, butterflies.
        Color[] twinkle = new Color[8];
        for (int i = 0; i < twinkle.length; i++)
            twinkle[i] = new Color(255, 250, 215, 90 + i * 23);
        for (int i = 0; i < POLLEN_COUNT; i++)
            add(new Pollen(twinkle, random.nextInt(4) == 0 ? unit : small, POLLEN_FACTOR, dt(), random, h * 0.30, h * 0.92));

        for (int color : LEAF_COLORS) {
            BufferedImage[] tilts = new BufferedImage[LEAF_FRAMES.length];
            for (int i = 0; i < tilts.length; i++)
                tilts[i] = PixelSprites.create(LEAF_FRAMES[i], unit, color);
            leafSprites.add(tilts);
        }
        for (int i = 0; i < LEAF_POOL_SIZE; i++)
            add(new Leaf(LEAF_FACTOR, dt()));
        secondsUntilNextLeaf = 1 + random.nextDouble() * 2;

        for (int i = 0; i < BUTTERFLY_COUNT; i++) {
            BufferedImage[] frames = new BufferedImage[BUTTERFLY_FRAMES.length];
            int wing = BUTTERFLY_WINGS[i % BUTTERFLY_WINGS.length];
            for (int f = 0; f < frames.length; f++)
                frames[f] = PixelSprites.create(BUTTERFLY_FRAMES[f], unit, wing, BUTTERFLY_BODY);
            double speed = w * (0.020 + 0.015 * random.nextDouble());
            add(new Butterfly(frames, cycle, BUTTERFLY_FACTOR, dt(), random, speed, h * 0.45, h * 0.85));
        }
    }

    @Override
    protected void spawn() {
        secondsUntilNextLeaf -= dt();
        if (secondsUntilNextLeaf > 0)
            return;
        secondsUntilNextLeaf = 1.2 + random.nextDouble() * 1.8;

        Leaf leaf = findInactive(Leaf.class);
        if (leaf == null)
            return;
        int w = Game.GAME_WIDTH;
        int h = Game.GAME_HEIGHT;
        BufferedImage[] sprites = leafSprites.get(random.nextInt(leafSprites.size()));
        leaf.launch(sprites,
                random.nextDouble() * w,
                h * (0.040 + 0.030 * random.nextDouble()),
                w * (0.006 + 0.010 * random.nextDouble()),
                1.2 + random.nextDouble() * 1.2,
                10 + random.nextDouble() * 16,
                random.nextDouble() * Math.PI * 2,
                getCameraX());
    }
}
