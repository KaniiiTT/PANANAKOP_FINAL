package background;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

import levels.Level;
import main.Game;

/**
 * Life at ground level: tufts of grass, tall grass, ferns and small flowers that
 * grow on the grassy ground of the level and bend away when the player runs
 * past, and now and then a small animal (a bird by day, a frog by night).
 *
 * This layer lives in the WORLD, not in the backdrop: it is drawn right after the
 * level tiles and before the player, so the plants stand on the real ground.
 *
 * <b>Data structures.</b> All plants of the level are stored once, in parallel
 * arrays sorted by their x position (the level is scanned from left to right).
 * Both {@link #update} and {@link #draw} find the plants near the player or the
 * camera with a binary search and visit only those. Each plant is a small spring
 * (lean and speed) that is pushed by the wind and by the player. The pictures are
 * pre-built for nine lean steps, so bending costs one image draw. The animals are
 * a fixed pool in one {@code ArrayList} (no animal is created while playing).
 *
 * <b>Continuing world.</b> The plant colours are kept in one small palette
 * ({@link #GRASS}) so a different region of the world can use another one later.
 */
public class GroundLifeLayer {
    /** Colours of the grass, taken from the grass caps of the ground tiles. */
    private static final int[] GRASS = { 0xFF224224, 0xFF42742C, 0xFF70A830, 0xFFB0D440 }; // deep, dark, mid, light

    private static final int LEAN_STEPS = 9; // from fully bent left to fully bent right
    private static final int KINDS = 4; // tuft, tall grass, fern, flowering tuft
    private static final int ART_WIDTH = 46;
    private static final int ART_HEIGHT = 22;

    private static final double SPRING = 15.0; // how quickly a plant returns to rest (radians per second)
    private static final double DAMPING = 0.38;

    private static final int MAX_BIRDS = 2;
    private static final int MAX_FROGS = 1;

    private final DayNightCycle cycle;
    private final double secondsPerUpdate;
    private final int tile = Game.TILES_SIZE;
    private final Random random = new Random(5);

    private final BufferedImage[][] frames = new BufferedImage[KINDS][LEAN_STEPS];
    private final ArrayList<GroundCritter> critters = new ArrayList<>();

    // the plants of the level, sorted by x
    private int plantCount;
    private float[] plantX = new float[0];
    private int[] plantBase = new int[0];
    private byte[] plantKind = new byte[0];
    private float[] plantPhase = new float[0];
    private float[] lean = new float[0];
    private float[] leanSpeed = new float[0];

    private int[][] builtFrom;
    private boolean[][] surface = new boolean[0][0];

    private double windClock;
    private double spawnTimer = 4;
    private double playerX;
    private double playerFeetY;
    private double playerSpeed; // 0..1, how fast the player is moving sideways
    private double previousPlayerX = Double.NaN;

    public GroundLifeLayer(DayNightCycle cycle, double updatesPerSecond) {
        this.cycle = cycle;
        this.secondsPerUpdate = 1.0 / updatesPerSecond;
        buildFrames();
        for (int i = 0; i < MAX_BIRDS; i++)
            critters.add(new GroundBird(this));
        for (int i = 0; i < MAX_FROGS; i++)
            critters.add(new Frog(this));
    }

    // ------------------------------------------------------------------ access for the animals

    int getTileSize() {
        return tile;
    }

    Random random() {
        return random;
    }

    DayNightCycle getCycle() {
        return cycle;
    }

    double getPlayerX() {
        return playerX;
    }

    double getPlayerFeetY() {
        return playerFeetY;
    }

    boolean isSurface(int column, int row) {
        return row >= 0 && row < surface.length && column >= 0 && column < surface[row].length && surface[row][column];
    }

    // ------------------------------------------------------------------ building the level's plants

    private void build(Level level) {
        int[][] data = level.getLevelData();
        builtFrom = data;
        int rows = data.length;
        int columns = data[0].length;
        surface = new boolean[rows][columns];
        TileClassifier classifier = new TileClassifier();
        int surfaces = 0;
        for (int row = 0; row < rows; row++)
            for (int column = 0; column < columns; column++)
                // a grass-topped ground tile (a custom block tile is not grass) with open air above it
                if (classifier.isWalkableSurface(level.getSpriteIndex(column, row))
                        && classifier.isSolid(data, column, row)
                        && (row == 0 || !classifier.isSolid(data, column, row - 1))) {
                    surface[row][column] = true;
                    surfaces++;
                }
        int capacity = surfaces * 2;
        plantX = new float[capacity];
        plantBase = new int[capacity];
        plantKind = new byte[capacity];
        plantPhase = new float[capacity];
        lean = new float[capacity];
        leanSpeed = new float[capacity];
        plantCount = 0;
        for (int column = 0; column < columns; column++)
            for (int row = 0; row < rows; row++) {
                if (!surface[row][column])
                    continue;
                int h = hash(column, row);
                int roll = (h >>> 8) & 255;
                int plants = roll < 80 ? 0 : roll < 190 ? 1 : 2;
                for (int j = 0; j < plants; j++) {
                    int offset = 8 + ((h >>> (j * 9 + 3)) & 0x7F) % (tile - 16);
                    int pick = (h >>> (j * 7 + 5)) & 63;
                    plantX[plantCount] = column * tile + offset;
                    plantBase[plantCount] = row * tile;
                    plantKind[plantCount] = (byte) (pick < 28 ? 0 : pick < 44 ? 1 : pick < 55 ? 2 : 3);
                    plantPhase[plantCount] = (h & 1023) * 0.0061f;
                    plantCount++;
                }
            }
        for (GroundCritter critter : critters)
            critter.active = false;
    }

    private static int hash(int column, int row) {
        int h = column * 374761393 + row * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    /** First plant whose x is not less than {@code x} (binary search; the plants are sorted by x). */
    private int firstPlantFrom(double x) {
        int low = 0;
        int high = plantCount;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (plantX[mid] < x)
                low = mid + 1;
            else
                high = mid;
        }
        return low;
    }

    // ------------------------------------------------------------------ update

    public void update(Level level, Rectangle2D.Float playerHitbox) {
        if (builtFrom != level.getLevelData())
            build(level);
        double centre = playerHitbox.x + playerHitbox.width / 2.0;
        playerFeetY = playerHitbox.y + playerHitbox.height;
        double speed = Double.isNaN(previousPlayerX) ? 0 : Math.abs(centre - previousPlayerX);
        previousPlayerX = centre;
        playerX = centre;
        // eased, so one stray frame does not make the grass jump
        playerSpeed += (Math.min(1, speed / (tile * 0.03)) - playerSpeed) * 0.25;

        double dt = secondsPerUpdate;
        windClock += dt;
        double gust = 0.55 + 0.45 * Math.sin(windClock * 0.31) * Math.sin(windClock * 0.113 + 1.0);

        double reach = tile * 0.95;
        int from = firstPlantFrom(centre - Game.GAME_WIDTH * 0.75 - 60);
        double to = centre + Game.GAME_WIDTH * 0.75 + 60;
        for (int i = from; i < plantCount && plantX[i] <= to; i++) {
            double target = 0.16 * gust * Math.sin(windClock * 1.6 + plantX[i] * 0.012 + plantPhase[i]);
            double distance = plantX[i] - centre;
            double above = plantBase[i] - playerFeetY; // > 0 when the player is above the plant
            if (Math.abs(distance) < reach && above > -tile * 0.6 && above < tile * 0.1) {
                // pushed away from the player: harder the closer and the faster the player is
                double push = (1 - Math.abs(distance) / reach) * (0.12 + 0.88 * playerSpeed);
                target += Math.signum(distance) * push * 1.4;
            }
            target = Math.max(-1, Math.min(1, target));
            double acceleration = -SPRING * SPRING * (lean[i] - target) - 2 * DAMPING * SPRING * leanSpeed[i];
            leanSpeed[i] += (float) (acceleration * dt);
            lean[i] += (float) (leanSpeed[i] * dt);
            lean[i] = Math.max(-1.2f, Math.min(1.2f, lean[i]));
        }

        for (GroundCritter critter : critters)
            critter.update(dt);
        spawnTimer -= dt;
        if (spawnTimer <= 0) {
            spawnTimer = 7 + random.nextDouble() * 9;
            trySpawn();
        }
    }

    private void trySpawn() {
        GroundCritter candidate = null;
        int free = 0;
        for (GroundCritter critter : critters)
            if (!critter.isActive() && critter.livesNow(cycle) && random.nextInt(++free) == 0)
                candidate = critter; // a random one of the free animals that suit the time of day
        if (candidate == null || surface.length == 0)
            return;
        int playerColumn = (int) (playerX / tile);
        for (int attempt = 0; attempt < 14; attempt++) {
            int column = playerColumn + random.nextInt(19) - 9;
            if (Math.abs(column - playerColumn) < 4)
                continue;
            int row = surfaceRowNear(column, playerFeetY);
            if (row < 0 || runLength(column, row) < 3)
                continue;
            candidate.spawn(column, row);
            return;
        }
    }

    /** The surface row of a column that is nearest (vertically) to {@code y}; -1 when the column has none. */
    private int surfaceRowNear(int column, double y) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int row = 0; row < surface.length; row++)
            if (column >= 0 && column < surface[row].length && surface[row][column]
                    && Math.abs(row * tile - y) < bestDistance) {
                bestDistance = Math.abs(row * tile - y);
                best = row;
            }
        return best;
    }

    private int runLength(int column, int row) {
        int length = 1;
        for (int c = column - 1; isSurface(c, row); c--)
            length++;
        for (int c = column + 1; isSurface(c, row); c++)
            length++;
        return length;
    }

    // ------------------------------------------------------------------ draw

    public void draw(Graphics2D g, int cameraX) {
        int from = firstPlantFrom(cameraX - ART_WIDTH * 2);
        int right = cameraX + Game.GAME_WIDTH + ART_WIDTH * 2;
        for (int i = from; i < plantCount && plantX[i] <= right; i++) {
            int step = Math.round((lean[i] + 1f) * (LEAN_STEPS - 1) / 2f);
            step = Math.max(0, Math.min(LEAN_STEPS - 1, step));
            BufferedImage image = frames[plantKind[i]][step];
            int x = Math.round(plantX[i]) - cameraX - image.getWidth() / 2;
            int y = plantBase[i] - image.getHeight() + 3; // the roots sit in the grass cap
            g.drawImage(image, x, y, null);
        }
        for (GroundCritter critter : critters)
            critter.draw(g, cameraX);
    }

    // ------------------------------------------------------------------ the plant pictures

    /** A small raster of art pixels (one art pixel is one pixel of the 32-pixel ground tiles). */
    private static final class Raster {
        final int[] px = new int[ART_WIDTH * ART_HEIGHT];

        void set(int x, int y, int argb) {
            if (x >= 0 && x < ART_WIDTH && y >= 0 && y < ART_HEIGHT)
                px[y * ART_WIDTH + x] = argb;
        }
    }

    private void buildFrames() {
        double scale = tile / 32.0;
        for (int kind = 0; kind < KINDS; kind++) {
            Raster raster = new Raster();
            Random r = new Random(1000 + kind);
            paint(raster, kind, r);
            int top = ART_HEIGHT;
            for (int y = 0; y < ART_HEIGHT && top == ART_HEIGHT; y++)
                for (int x = 0; x < ART_WIDTH; x++)
                    if (raster.px[y * ART_WIDTH + x] != 0) {
                        top = y;
                        break;
                    }
            int content = Math.max(1, ART_HEIGHT - 1 - top);
            for (int step = 0; step < LEAN_STEPS; step++) {
                double bend = (step - (LEAN_STEPS - 1) / 2.0) / ((LEAN_STEPS - 1) / 2.0); // -1..1
                BufferedImage art = new BufferedImage(ART_WIDTH, ART_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                for (int y = top; y < ART_HEIGHT; y++) {
                    double up = (ART_HEIGHT - 1 - y) / (double) content; // 0 at the roots, 1 at the top
                    int shift = (int) Math.round(bend * content * 0.55 * Math.pow(up, 1.5));
                    for (int x = 0; x < ART_WIDTH; x++) {
                        int argb = raster.px[y * ART_WIDTH + x];
                        if (argb != 0 && x + shift >= 0 && x + shift < ART_WIDTH)
                            art.setRGB(x + shift, y, argb);
                    }
                }
                BufferedImage big = new BufferedImage((int) Math.round(ART_WIDTH * scale),
                        (int) Math.round(ART_HEIGHT * scale), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = big.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(art, 0, 0, big.getWidth(), big.getHeight(), null);
                g.dispose();
                frames[kind][step] = big;
            }
        }
    }

    private static void paint(Raster r, int kind, Random rnd) {
        int cx = ART_WIDTH / 2;
        int base = ART_HEIGHT - 1;
        switch (kind) {
        case 0: // a low tuft
            for (int i = 0; i < 7; i++)
                blade(r, cx - 3 + i, base, 5 + rnd.nextInt(5), (i - 3) * 0.45 + (rnd.nextDouble() - 0.5));
            break;
        case 1: // tall grass
            for (int i = 0; i < 8; i++)
                blade(r, cx - 4 + i, base, 9 + rnd.nextInt(7), (i - 3.5) * 0.7 + (rnd.nextDouble() - 0.5) * 1.2);
            break;
        case 2: // a fern: arching fronds with leaflets
            for (int i = 0; i < 9; i++) {
                double angle = Math.PI * (0.10 + 0.80 * i / 8.0); // from the right, over the top, to the left
                frond(r, cx, base, angle, 11 + rnd.nextInt(4));
            }
            break;
        default: // a tuft with small flowers
            for (int i = 0; i < 6; i++)
                blade(r, cx - 3 + i, base, 5 + rnd.nextInt(4), (i - 2.5) * 0.4);
            int[] petals = { 0xFFF4F0E0, 0xFFF0C8D8, 0xFFF0D860 };
            for (int f = 0; f < 3; f++) {
                int fx = cx - 4 + f * 4 + rnd.nextInt(2);
                int fy = base - 9 - rnd.nextInt(4);
                for (int y = fy + 1; y < base; y++)
                    r.set(fx, y, GRASS[1]);
                int petal = petals[rnd.nextInt(petals.length)];
                r.set(fx - 1, fy, petal);
                r.set(fx + 1, fy, petal);
                r.set(fx, fy - 1, petal);
                r.set(fx, fy + 1, petal);
                r.set(fx, fy, 0xFFE8A030);
            }
            break;
        }
    }

    private static void blade(Raster r, int x0, int base, int height, double curve) {
        for (int s = 0; s <= height; s++) {
            double t = s / (double) height;
            int x = (int) Math.round(x0 + curve * t * t * height * 0.45);
            int y = base - s;
            int colour = t < 0.30 ? GRASS[1] : t < 0.78 ? GRASS[2] : GRASS[3];
            r.set(x, y, colour);
            if (s > 0 && s < height - 1)
                r.set(x + 1, y, t < 0.5 ? GRASS[1] : GRASS[2]); // blades are two pixels wide
        }
    }

    private static void frond(Raster r, int cx, int base, double angle, int length) {
        double dx = Math.cos(angle);
        double dy = -Math.sin(angle);
        for (int s = 0; s <= length * 2; s++) {
            double t = s / (double) (length * 2);
            double x = cx + dx * length * t;
            double y = base + dy * length * t + 0.55 * length * t * t; // arches over and droops
            r.set((int) Math.round(x), (int) Math.round(y), GRASS[1]);
            if (s % 2 == 0 && t > 0.15) {
                int leaflet = Math.max(1, (int) Math.round(3 * Math.sin(Math.PI * Math.min(1, t * 1.05))));
                for (int k = 1; k <= leaflet; k++) {
                    r.set((int) Math.round(x) - k, (int) Math.round(y) + (k + 1) / 2, k == leaflet ? GRASS[3] : GRASS[2]);
                    r.set((int) Math.round(x) + k, (int) Math.round(y) + (k + 1) / 2, k == leaflet ? GRASS[3] : GRASS[2]);
                }
            }
        }
    }
}
