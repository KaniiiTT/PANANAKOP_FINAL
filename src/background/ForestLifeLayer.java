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
    private final TiledBackgroundLayer canopy; // the forest the leaves fall from
    private final int[] crownColumns;          // columns of that forest's artwork that hold a tree crown
    private double secondsUntilNextLeaf;

    /**
     * @param canopy the near forest: leaves come loose from the tree crowns in its artwork
     */
    public ForestLifeLayer(DayNightCycle cycle, TiledBackgroundLayer canopy, double verticalFactor,
            double updatesPerSecond) {
        super(updatesPerSecond, verticalFactor);
        this.canopy = canopy;
        this.crownColumns = findCrownColumns(canopy);
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
            add(new Leaf(canopy.getParallaxFactor(), dt())); // same depth as the trees they fall from
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

    /**
     * Finds the columns of the forest artwork that belong to tall trees rather than
     * to the low bushes: columns whose highest visible pixel is clearly above the
     * usual bush height (the bush height is the 75th percentile of all column tops).
     */
    private static int[] findCrownColumns(TiledBackgroundLayer forest) {
        int w = Game.GAME_WIDTH;
        int[] tops = new int[w];
        int[] sorted = new int[w];
        for (int column = 0; column < w; column++) {
            int top = forest.topVisibleRow(column);
            tops[column] = top;
            sorted[column] = top < 0 ? Game.GAME_HEIGHT : top;
        }
        java.util.Arrays.sort(sorted);
        double bushLevel = sorted[(int) (w * 0.75)];
        double limit = bushLevel - Game.GAME_HEIGHT * 0.04;

        int count = 0;
        for (int top : tops)
            if (top >= 0 && top < limit)
                count++;
        int[] columns = new int[count];
        int next = 0;
        for (int column = 0; column < w; column++)
            if (tops[column] >= 0 && tops[column] < limit)
                columns[next++] = column;
        return columns;
    }

    @Override
    protected void spawn() {
        secondsUntilNextLeaf -= dt();
        if (secondsUntilNextLeaf > 0)
            return;
        secondsUntilNextLeaf = 1.2 + random.nextDouble() * 1.8;

        Leaf leaf = findInactive(Leaf.class);
        if (leaf == null || crownColumns.length == 0)
            return;
        int w = Game.GAME_WIDTH;
        int h = Game.GAME_HEIGHT;
        // Pick a spot inside a tree crown: a column of the forest artwork, a little below its top.
        // Try again if the spot falls into a gap between branches; give up for now after a few tries.
        int column = 0;
        double startY = 0;
        boolean found = false;
        for (int attempt = 0; attempt < 8 && !found; attempt++) {
            column = crownColumns[random.nextInt(crownColumns.length)];
            startY = canopy.topVisibleRow(column) + h * (0.02 + 0.07 * random.nextDouble());
            found = canopy.isVisibleAt(column, (int) startY);
        }
        if (!found)
            return;
        double startX = canopy.screenXOfColumn(column, getCameraX());
        BufferedImage[] sprites = leafSprites.get(random.nextInt(leafSprites.size()));
        leaf.launch(sprites,
                startX,
                startY,
                h * (0.16 + 0.18 * random.nextDouble()),
                h * (0.040 + 0.030 * random.nextDouble()),
                w * (0.006 + 0.010 * random.nextDouble()),
                1.2 + random.nextDouble() * 1.2,
                10 + random.nextDouble() * 16,
                random.nextDouble() * Math.PI * 2,
                getCameraX());
    }
}
