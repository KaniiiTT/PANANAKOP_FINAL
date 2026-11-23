package background;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Paints the tropical scenery that is added to the Stage 1 background: distant
 * mountains, rolling hills, coconut palms for the far forest, the middle
 * forest (banana plants, broad leaves, varied trees) and ferns for the near
 * bushes.
 *
 * Everything is drawn pixel by pixel, ONCE, when the background is built, in the
 * same 960x544 pixel grid and the same small colour palettes as the existing
 * artwork. The results are ordinary images that the existing layer classes
 * ({@link ForestLayer}, {@link SwayingForestLayer}) scroll, shift and sway, so no
 * second parallax system exists and nothing is created while playing.
 *
 * Every picture is horizontally periodic (what leaves the right edge re-enters
 * on the left), so the wraparound of the layers never shows a seam. A fixed
 * random seed makes the scenery identical on every start.
 */
public final class TropicalScenery {
    /** Size of the source artwork, the same as the existing Flora images. */
    public static final int WIDTH = 960;
    public static final int HEIGHT = 544;

    private static final int[][] BAYER = { { 0, 8, 2, 10 }, { 12, 4, 14, 6 }, { 3, 11, 1, 9 }, { 15, 7, 13, 5 } };

    /** A small set of related colours, from the lit side to the deepest shadow. */
    private static final class Palette {
        final int light;
        final int body;
        final int dark;
        final int deep;
        final int trunk;
        final int trunkDark;

        Palette(int light, int body, int dark, int deep, int trunk, int trunkDark) {
            this.light = 0xFF000000 | light;
            this.body = 0xFF000000 | body;
            this.dark = 0xFF000000 | dark;
            this.deep = 0xFF000000 | deep;
            this.trunk = 0xFF000000 | trunk;
            this.trunkDark = 0xFF000000 | trunkDark;
        }
    }

    // Colours are picked from the existing art (far forest 7e89a2..92a6b2, near forest 5f6a71..94ad8e).
    private static final Palette FAR_MOUNTAIN = new Palette(0xA3B0CF, 0x90A0C6, 0x8190BA, 0x7B8AB4, 0, 0);
    private static final Palette NEAR_MOUNTAIN = new Palette(0x93A9B9, 0x8499B0, 0x7A8FA8, 0x7186A0, 0, 0);
    private static final Palette HILL = new Palette(0x95B0A0, 0x86A294, 0x7A9689, 0x6F8B84, 0, 0);
    private static final Palette FAR_PALM = new Palette(0x8A98B0, 0x7F8CA6, 0x78849F, 0x727D99, 0x7A8099, 0x727893);
    private static final Palette MID = new Palette(0x8EA891, 0x7C9689, 0x6E8882, 0x5F777A, 0x6C6A74, 0x5D5A68);
    private static final Palette NEAR = new Palette(0x94AD8E, 0x8AA08D, 0x738488, 0x5F6A71, 0x635964, 0x5D525D);

    private TropicalScenery() {
    }

    // ------------------------------------------------------------------ canvas

    /** A plain pixel buffer that wraps horizontally, so scenery near an edge continues on the other side. */
    private static final class Canvas {
        final int[] px = new int[WIDTH * HEIGHT];

        void set(int x, int y, int argb) {
            if (y < 0 || y >= HEIGHT)
                return;
            x %= WIDTH;
            if (x < 0)
                x += WIDTH;
            px[y * WIDTH + x] = argb;
        }

        int get(int x, int y) {
            if (y < 0 || y >= HEIGHT)
                return 0;
            x %= WIDTH;
            if (x < 0)
                x += WIDTH;
            return px[y * WIDTH + x];
        }

        void line(double x0, double y0, double x1, double y1, int argb) {
            int steps = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) * 1.5) + 1;
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                set((int) Math.round(x0 + (x1 - x0) * t), (int) Math.round(y0 + (y1 - y0) * t), argb);
            }
        }

        /** Paints the opaque pixels of an existing picture over this canvas. */
        void over(BufferedImage img) {
            for (int y = 0; y < HEIGHT; y++)
                for (int x = 0; x < WIDTH; x++) {
                    int argb = img.getRGB(x, y);
                    if ((argb >>> 24) != 0)
                        px[y * WIDTH + x] = argb;
                }
        }

        BufferedImage toImage() {
            BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, WIDTH, HEIGHT, px, 0, WIDTH);
            return img;
        }
    }

    /** Smooth periodic wobble: a sum of sine waves whose frequencies are whole numbers, so it tiles. */
    private static final class Wobble {
        private final int[] freq;
        private final double[] amp;
        private final double[] phase;

        Wobble(Random r, int[] freq, double[] amp) {
            this.freq = freq;
            this.amp = amp;
            this.phase = new double[freq.length];
            for (int i = 0; i < freq.length; i++)
                phase[i] = r.nextDouble() * Math.PI * 2;
        }

        double at(int x) {
            double u = 2 * Math.PI * x / WIDTH;
            double v = 0;
            for (int i = 0; i < freq.length; i++)
                v += amp[i] * Math.sin(freq[i] * u + phase[i]);
            return v;
        }
    }

    private static int wrapDistance(int a, int b) {
        int d = Math.abs(a - b) % WIDTH;
        return Math.min(d, WIDTH - d);
    }

    // ------------------------------------------------------------------ mountains and hills

    // A mountain range: peaks of different height and width overlap, the lit side (facing the sun on the
    // left) is lighter, ravines run down the slopes and the foot of the range fades into the haze with an
    // ordered dither. See range(...).

    /** The far (pale, blue) mountain range. */
    public static BufferedImage farMountains() {
        return range(11L, FAR_MOUNTAIN, 5, 200, 290, 440, 0.30);
    }

    /** The nearer (greener, darker) mountain range, in front of the far one. */
    public static BufferedImage nearMountains() {
        return range(29L, NEAR_MOUNTAIN, 7, 120, 190, 456, 0.28);
    }

    private static BufferedImage range(long seed, Palette p, int count, int minHigh, int maxHigh, int baseY,
            double fogShare) {
        Random r = new Random(seed);
        int[] centre = new int[count];
        int[] high = new int[count];
        int[] half = new int[count];
        for (int i = 0; i < count; i++) {
            centre[i] = (int) ((i + 0.5) * WIDTH / count + (r.nextDouble() - 0.5) * WIDTH / count * 0.55);
            high[i] = minHigh + r.nextInt(maxHigh - minHigh + 1);
            half[i] = (int) (high[i] * (1.35 + r.nextDouble() * 0.9));
        }
        Wobble rough = new Wobble(r, new int[] { 7, 17, 31, 53 }, new double[] { 5.0, 3.0, 1.8, 0.9 });
        int[] ridge = new int[WIDTH];
        for (int x = 0; x < WIDTH; x++) {
            double best = 0;
            for (int i = 0; i < count; i++) {
                double d = wrapDistance(x, centre[i]) / (double) half[i];
                if (d < 1) {
                    double h = high[i] * Math.pow(1 - d, 1.0);
                    // shoulders: a second, smaller bump on one side of each peak
                    double ds = wrapDistance(x, centre[i] + half[i] / 3) / (half[i] * 0.45);
                    if (ds < 1)
                        h = Math.max(h, high[i] * 0.66 * Math.pow(1 - ds, 1.1));
                    best = Math.max(best, h);
                }
            }
            ridge[x] = (int) Math.round(baseY - best - 6 + rough.at(x));
        }
        Canvas c = new Canvas();
        int lowest = baseY;
        for (int x = 0; x < WIDTH; x++) {
            int top = ridge[x];
            double bandDepth = Math.max(8, (baseY - top) * 0.75);
            for (int y = top; y <= lowest; y++) {
                double depth = y - top;
                int base = p.body;
                // Which way the slope faces is read a little to the side and further along as we go down,
                // so the crease between a lit and a shaded face runs diagonally instead of straight down.
                int sx = Math.floorMod(x + (int) Math.round(depth * 0.55 + 1.2 * Math.sin(y * 0.35)), WIDTH);
                int slope = ridge[(sx + 6) % WIDTH] - ridge[(sx - 6 + WIDTH) % WIDTH]; // < 0: rising to the right
                boolean lit = slope < -2;
                boolean shaded = slope > 2;
                if (lit && depth < bandDepth)
                    base = dither(x, y, depth / bandDepth, p.light, p.body, 0.35);
                else if (shaded && depth < bandDepth)
                    base = dither(x, y, depth / bandDepth, p.dark, p.body, 0.35);
                if (y == top && ridge[(x + 4) % WIDTH] < ridge[(x - 4 + WIDTH) % WIDTH])
                    base = p.light;
                // the foot of the range dissolves into the haze
                double fogStart = (baseY - top) * (1 - fogShare);
                if (depth > fogStart) {
                    double f = Math.min(1, (depth - fogStart) / Math.max(1, (baseY - top) * fogShare));
                    if (f * 16 > BAYER[y & 3][x & 3] + 0.5)
                        base = p.deep;
                }
                c.set(x, y, base);
            }
        }
        // ravines: short dark strokes that run down the slopes
        int ravines = count * 9;
        for (int i = 0; i < ravines; i++) {
            int x = r.nextInt(WIDTH);
            int slope = ridge[(x + 4) % WIDTH] - ridge[(x - 4 + WIDTH) % WIDTH];
            if (Math.abs(slope) < 2)
                continue;
            double dx = slope > 0 ? 0.42 : -0.42;
            double px = x;
            int len = 10 + r.nextInt(26);
            for (int k = 0; k < len; k++) {
                int yy = ridge[(int) Math.floorMod((int) px, WIDTH)] + 3 + k;
                if (yy > lowest - 4)
                    break;
                c.set((int) px, yy, k % 5 == 4 ? p.body : p.dark);
                px += dx;
            }
        }
        return c.toImage();
    }

    /** Mixes two colours with an ordered dither; {@code t} 0 = all {@code a}, 1 = all {@code b}. */
    private static int dither(int x, int y, double t, int a, int b, double softness) {
        // a is used near t = 0, b near t = 1; the transition zone is dithered.
        double lo = 1 - softness * 2;
        if (t < lo)
            return a;
        double f = (t - lo) / (1 - lo);
        return f * 16 > BAYER[y & 3][x & 3] ? b : a;
    }

    /**
     * Rolling green hills: a smooth, periodic skyline with a lit rim, scattered clumps of trees on top,
     * and a darker foot. They sit in front of the mountains and behind the far forest.
     */
    public static BufferedImage hills() {
        Random r = new Random(53L);
        Canvas c = new Canvas();
        Wobble w = new Wobble(r, new int[] { 2, 3, 5, 8, 14 }, new double[] { 22, 15, 9, 4, 1.6 });
        int baseY = 392;
        int[] top = new int[WIDTH];
        for (int x = 0; x < WIDTH; x++)
            top[x] = (int) Math.round(baseY + w.at(x));
        int bottom = 470;
        for (int x = 0; x < WIDTH; x++) {
            int slope = top[(x + 5) % WIDTH] - top[(x - 5 + WIDTH) % WIDTH];
            for (int y = top[x]; y <= bottom; y++) {
                double depth = y - top[x];
                int col = HILL.body;
                if (depth < 3 && slope <= 1)
                    col = HILL.light;
                else if (depth < 14 && slope < -1)
                    col = dither(x, y, depth / 14.0, HILL.light, HILL.body, 0.5);
                else if (depth > 34) {
                    double f = Math.min(1, (depth - 34) / 36.0);
                    if (f * 16 > BAYER[y & 3][x & 3])
                        col = HILL.deep;
                    else if (depth > 22)
                        col = HILL.dark;
                }
                c.set(x, y, col);
            }
        }
        // clumps of distant trees along the hilltops
        for (int i = 0; i < 46; i++) {
            int x = r.nextInt(WIDTH);
            int rad = 3 + r.nextInt(4);
            blob(c, r, x, top[x] - rad / 2 + 2, rad, HILL, 0.6);
        }
        return c.toImage();
    }

    // ------------------------------------------------------------------ shared drawing helpers

    /**
     * A round clump of leaves with a ragged edge, lit from the upper left.
     *
     * @param contrast how strongly the light and shadow tones are used (0.5 = soft, 1 = strong)
     */
    private static void blob(Canvas c, Random r, double cx, double cy, double rad, Palette p, double contrast) {
        double ph1 = r.nextDouble() * 6.28;
        double ph2 = r.nextDouble() * 6.28;
        int range = (int) Math.ceil(rad * 1.3) + 1;
        for (int dy = -range; dy <= range; dy++)
            for (int dx = -range; dx <= range; dx++) {
                double d = Math.hypot(dx, dy);
                double ang = Math.atan2(dy, dx);
                double edge = rad * (1 + 0.15 * Math.sin(3 * ang + ph1) + 0.10 * Math.sin(7 * ang + ph2));
                if (d > edge)
                    continue;
                if (d > edge - 1.4 && r.nextInt(5) == 0)
                    continue;
                double lit = (-dx * 0.6 - dy * 0.8) / Math.max(1, rad) + (r.nextDouble() - 0.5) * 0.4;
                lit *= contrast / 0.8;
                int col;
                if (lit > 0.42)
                    col = p.light;
                else if (lit > -0.12)
                    col = p.body;
                else if (lit > -0.52)
                    col = p.dark;
                else
                    col = p.deep;
                c.set((int) Math.round(cx + dx), (int) Math.round(cy + dy), col);
            }
    }

    /** A leaf blade: a curved, pointed strip with a darker midrib (banana leaf, fern blade). */
    private static void blade(Canvas c, double x0, double y0, double angle, double len, double wmax, double gravity,
            Palette p) {
        int steps = (int) (len * 2);
        double dx = Math.cos(angle);
        double dy = Math.sin(angle);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = x0 + dx * len * t;
            double y = y0 + dy * len * t + gravity * len * t * t;
            double tx = dx * len;
            double ty = dy * len + 2 * gravity * len * t;
            double n = Math.hypot(tx, ty);
            double nx = -ty / n;
            double ny = tx / n;
            double w = wmax * Math.pow(Math.sin(Math.PI * Math.min(1, t * 0.98 + 0.02)), 0.65);
            double litSign = Math.signum(nx * -0.6 + ny * -0.8);
            for (double s = -w; s <= w; s += 0.5) {
                double a = w == 0 ? 0 : s / w * litSign;
                int col = a > 0.35 ? p.light : a < -0.45 ? p.dark : p.body;
                if (Math.abs(s) < 0.6)
                    col = p.deep;
                c.set((int) Math.round(x + nx * s), (int) Math.round(y + ny * s), col);
            }
        }
    }

    /** One palm frond: a drooping rib with short leaflets on both sides. */
    private static void frond(Canvas c, double x0, double y0, double angle, double len, double gravity, Palette p) {
        int steps = (int) (len * 1.5);
        double dx = Math.cos(angle);
        double dy = Math.sin(angle);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = x0 + dx * len * t;
            double y = y0 + dy * len * t + gravity * len * t * t;
            c.set((int) Math.round(x), (int) Math.round(y), p.dark);
            if (i % 2 == 0 && t > 0.1) {
                double tx = dx * len;
                double ty = dy * len + 2 * gravity * len * t;
                double n = Math.hypot(tx, ty);
                tx /= n;
                ty /= n;
                double ll = Math.max(2, len * 0.24 * Math.pow(Math.sin(Math.PI * Math.min(1, t * 1.1)), 0.7));
                for (int side = -1; side <= 1; side += 2) {
                    // rotate the tangent by about 60 degrees and let the leaflet hang down
                    double cs = Math.cos(1.05 * side);
                    double sn = Math.sin(1.05 * side);
                    double lx = tx * cs - ty * sn;
                    double ly = tx * sn + ty * cs + 0.55;
                    double ln = Math.hypot(lx, ly);
                    lx /= ln;
                    ly /= ln;
                    int col = ly < 0.35 ? p.light : p.body;
                    for (int k = 1; k <= ll; k++)
                        c.set((int) Math.round(x + lx * k), (int) Math.round(y + ly * k), k == (int) ll ? p.dark : col);
                }
            }
        }
    }

    /** A coconut palm: a gently curved, ringed trunk and a crown of drooping fronds. */
    private static void palm(Canvas c, Random r, Palette p, double bx, double by, double height, double lean) {
        double cx = bx + lean;
        double cy = by - height;
        double kx = bx + lean * 0.05;
        double ky = by - height * 0.55;
        int steps = (int) (height * 1.6);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double u = 1 - t;
            double x = u * u * bx + 2 * u * t * kx + t * t * cx;
            double y = u * u * by + 2 * u * t * ky + t * t * cy;
            int xi = (int) Math.round(x);
            int yi = (int) Math.round(y);
            int w = t < 0.35 ? 2 : 1;
            for (int k = -w; k <= w; k++)
                c.set(xi + k, yi, k >= w ? p.trunkDark : p.trunk);
            if ((yi & 3) == 0)
                for (int k = -w; k <= w; k++)
                    c.set(xi + k, yi, p.trunkDark);
        }
        int fronds = 9 + r.nextInt(3);
        double len = Math.max(18, Math.min(42, height * 0.36 + 8));
        for (int i = 0; i < fronds; i++) {
            double t = (i + 0.5) / fronds;
            double angle = -Math.PI * (0.97 - 0.94 * t) + (r.nextDouble() - 0.5) * 0.18;
            double side = Math.abs(Math.cos(angle)); // 1 = sideways, 0 = straight up
            double g = 0.55 + side * 0.85;
            frond(c, cx, cy, angle, len * (0.78 + 0.3 * side) * (0.9 + r.nextDouble() * 0.2), g, p);
        }
        // a small cluster of coconuts under the crown
        for (int i = 0; i < 3; i++)
            for (int k = 0; k < 2; k++)
                c.set((int) cx - 1 + i, (int) cy + 2 + k, p.trunkDark);
    }

    // ------------------------------------------------------------------ far forest: coconut palms

    /**
     * The existing far forest with coconut palm silhouettes added. The palms stand
     * in gaps of the forest, BEHIND the existing trees, so their trunks emerge from the bushes.
     */
    public static BufferedImage withPalms(BufferedImage far) {
        Random r = new Random(77L);
        Canvas c = new Canvas();
        int[] top = new int[WIDTH];
        for (int x = 0; x < WIDTH; x++) {
            top[x] = HEIGHT;
            for (int y = 0; y < HEIGHT; y++)
                if ((far.getRGB(x, y) >>> 24) != 0) {
                    top[x] = y;
                    break;
                }
        }
        int placed = 0;
        int lastX = -1000;
        for (int attempt = 0; attempt < 400 && placed < 8; attempt++) {
            int x = r.nextInt(WIDTH);
            if (wrapDistance(x, lastX) < 70)
                continue;
            int rim = top[x];
            // a clear gap: the forest is low around the palm
            boolean gap = true;
            for (int dx = -26; dx <= 26 && gap; dx += 4)
                gap = top[(x + dx + WIDTH) % WIDTH] > 262;
            if (!gap)
                continue;
            double height = 85 + r.nextInt(75);
            double lean = (r.nextDouble() - 0.5) * 36;
            palm(c, r, FAR_PALM, x, rim + 14, height + 14, lean);
            lastX = x;
            placed++;
            // sometimes a smaller palm next to it
            if (r.nextInt(3) == 0)
                palm(c, r, FAR_PALM, x + 22 + r.nextInt(14), top[(x + 30) % WIDTH] + 14,
                        60 + r.nextInt(30), -lean * 0.6);
        }
        c.over(far);
        return c.toImage();
    }

    // ------------------------------------------------------------------ middle forest

    /**
     * The middle forest: groups of varied trees, banana plants, broad-leaf plants and a
     * palm or two, with open gaps between the groups, on a low bank of bushes.
     */
    public static BufferedImage midForest() {
        Random r = new Random(101L);
        Canvas c = new Canvas();
        Wobble bank = new Wobble(r, new int[] { 3, 6, 11 }, new double[] { 9, 5, 2.5 });
        int baseY = 424;
        // 1. the bank of bushes, built from leaf clumps
        for (int x = 0; x < WIDTH; x++)
            for (int y = (int) (baseY + bank.at(x)); y < HEIGHT; y++)
                c.set(x, y, y > baseY + 40 ? MID.dark : MID.body);
        for (int x = 0; x < WIDTH; x += 11)
            blob(c, r, x + r.nextInt(6), baseY + bank.at(x) + 3 + r.nextInt(5), 12 + r.nextInt(8), MID, 0.8);

        // 2. groups, with open gaps between them
        int groups = 8;
        int[] kinds = { 0, 1, 0, 3, 2, 0, 4, 1 }; // 0 tree cluster, 1 banana grove, 2 broad leaves, 3 palms, 4 gap
        for (int i = kinds.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = kinds[i];
            kinds[i] = kinds[j];
            kinds[j] = t;
        }
        for (int g = 0; g < groups; g++) {
            int centre = (int) ((g + 0.5) * WIDTH / groups + (r.nextDouble() - 0.5) * 30);
            int ground = (int) (baseY + bank.at(centre)) + 8;
            switch (kinds[g]) {
            case 0:
                treeCluster(c, r, centre, ground);
                break;
            case 1:
                bananaGrove(c, r, centre, ground);
                break;
            case 2:
                broadLeaves(c, r, centre, ground);
                break;
            case 3:
                palm(c, r, MID, centre - 14, ground, 120 + r.nextInt(30), -14 - r.nextInt(10));
                palm(c, r, MID, centre + 16, ground, 90 + r.nextInt(25), 12 + r.nextInt(10));
                break;
            default:
                break; // an open gap
            }
        }
        // 3. a few low bushes in front, so the groups sit in the bank and do not float
        for (int x = 0; x < WIDTH; x += 17)
            blob(c, r, x + r.nextInt(8), baseY + bank.at(x) + 10, 8 + r.nextInt(5), MID, 0.8);
        return c.toImage();
    }

    private static void treeCluster(Canvas c, Random r, int centre, int ground) {
        int trees = 2 + r.nextInt(2);
        for (int i = 0; i < trees; i++) {
            int x = centre + (i - trees / 2) * (26 + r.nextInt(14));
            double h = 52 + r.nextInt(48);
            double lean = (r.nextDouble() - 0.5) * 12;
            double topY = ground - h;
            // trunk
            for (int s = 0; s <= h; s++) {
                double t = s / h;
                int tx = (int) Math.round(x + lean * t * t);
                int ty = (int) (ground - s);
                c.set(tx - 2, ty, MID.trunk);
                c.set(tx - 1, ty, MID.trunk);
                c.set(tx, ty, MID.trunk);
                c.set(tx + 1, ty, MID.trunkDark);
            }
            // a branch or two
            for (int b = 0; b < 2; b++) {
                double side = b == 0 ? -1 : 1;
                c.line(x + lean * 0.6, topY + h * 0.22, x + lean + side * (8 + r.nextInt(8)), topY + h * 0.05, MID.trunkDark);
            }
            // a crown of clumps; each tree gets its own shape (wide, tall or round)
            double wide = 0.7 + r.nextDouble() * 0.9;
            int clumps = 5 + r.nextInt(3);
            for (int k = 0; k < clumps; k++) {
                double ox = (r.nextDouble() - 0.5) * 44 * wide;
                double oy = (r.nextDouble() - 0.5) * 20 + (k == 0 ? -4 : 0);
                blob(c, r, x + lean + ox, topY + 12 + oy, 15 + r.nextInt(10), MID, 0.9);
            }
        }
    }

    private static void bananaGrove(Canvas c, Random r, int centre, int ground) {
        int plants = 2 + r.nextInt(2);
        for (int i = 0; i < plants; i++) {
            int x = centre + (i - plants / 2) * (22 + r.nextInt(10));
            bananaPlant(c, r, x, ground, 42 + r.nextInt(24));
        }
    }

    private static void bananaPlant(Canvas c, Random r, int x, int ground, int height) {
        int topY = ground - height;
        for (int y = topY; y <= ground; y++) {
            c.set(x - 1, y, MID.light);
            c.set(x, y, MID.body);
            c.set(x + 1, y, MID.dark);
        }
        int leaves = 6 + r.nextInt(3);
        for (int i = 0; i < leaves; i++) {
            double t = (i + 0.5) / leaves;
            double angle = -Math.PI * (0.92 - 0.84 * t) + (r.nextDouble() - 0.5) * 0.2;
            double side = Math.abs(Math.cos(angle));
            blade(c, x, topY, angle, 34 + r.nextInt(14), 6.0, 0.55 + side * 0.9, MID);
        }
        // a hanging bunch of bananas
        if (r.nextInt(2) == 0)
            for (int k = 0; k < 5; k++)
                c.set(x + 2 + (k & 1), topY + 3 + k, 0xFF000000 | 0x8FA06E);
    }

    private static void broadLeaves(Canvas c, Random r, int centre, int ground) {
        int plants = 3 + r.nextInt(2);
        for (int i = 0; i < plants; i++) {
            int x = centre + (i - plants / 2) * (16 + r.nextInt(10));
            int stalks = 5 + r.nextInt(3);
            for (int s = 0; s < stalks; s++) {
                double t = (s + 0.5) / stalks;
                double angle = -Math.PI * (0.88 - 0.76 * t) + (r.nextDouble() - 0.5) * 0.2;
                double len = 26 + r.nextInt(18);
                double ex = x + Math.cos(angle) * len * 0.8;
                double ey = ground - 4 + Math.sin(angle) * len * 0.9;
                c.line(x, ground - 2, ex, ey, MID.dark);
                // heart-shaped leaf: a short wide blade pointing away from the stalk
                blade(c, ex - Math.cos(angle) * 3, ey - Math.sin(angle) * 3, angle, 18 + r.nextInt(6), 8.0, 0.35, MID);
            }
        }
    }

    /**
     * The same picture moved up by {@code rows} source pixels. The rows that appear at the bottom are a
     * mirror image of the last rows, so the artwork continues smoothly. Used so the trunks and bushes of
     * a forest stand ON the ground line of the level instead of sinking below it.
     */
    public static BufferedImage raised(BufferedImage art, int rows) {
        Canvas c = new Canvas();
        for (int y = 0; y < HEIGHT; y++) {
            int from = y + rows;
            if (from >= HEIGHT)
                from = 2 * HEIGHT - 1 - from; // mirror
            from = Math.max(0, Math.min(HEIGHT - 1, from));
            for (int x = 0; x < WIDTH; x++)
                c.px[y * WIDTH + x] = art.getRGB(x, from);
        }
        return c.toImage();
    }

    // ------------------------------------------------------------------ near vegetation

    /**
     * The existing near forest with ferns, grass tufts and small shrubs added along the top of
     * the bushes, in front of them. They are part of the same image, so the existing wind sway
     * moves them together with the bushes.
     */
    public static BufferedImage withFerns(BufferedImage near) {
        Random r = new Random(131L);
        Canvas c = new Canvas();
        c.over(near);
        int[] top = new int[WIDTH];
        for (int x = 0; x < WIDTH; x++) {
            top[x] = HEIGHT;
            for (int y = 0; y < HEIGHT; y++)
                if ((near.getRGB(x, y) >>> 24) != 0) {
                    top[x] = y;
                    break;
                }
        }
        int x = r.nextInt(30);
        int placed = 0;
        while (x < WIDTH && placed < 40) {
            int rim = top[x];
            // only on the low bush line, never under a tall tree or across a trunk
            boolean flat = rim >= 392;
            for (int dx = -10; dx <= 10 && flat; dx += 5)
                flat = top[(x + dx + WIDTH) % WIDTH] >= 380;
            if (flat) {
                int kind = r.nextInt(5);
                if (kind <= 1)
                    fern(c, r, x, rim + 7, 14 + r.nextInt(12));
                else if (kind == 2)
                    tuft(c, r, x, rim + 4, 9 + r.nextInt(6));
                else if (kind == 3)
                    blob(c, r, x, rim + 1, 6 + r.nextInt(3), NEAR, 1.0);
                else
                    fern(c, r, x, rim + 8, 20 + r.nextInt(10));
                placed++;
                x += 34 + r.nextInt(46);
            } else {
                x += 7;
            }
        }
        return c.toImage();
    }

    private static void fern(Canvas c, Random r, int x, int y, int size) {
        int fronds = 7 + r.nextInt(3);
        for (int i = 0; i < fronds; i++) {
            double t = (i + 0.5) / fronds;
            double angle = -Math.PI * (0.95 - 0.9 * t) + (r.nextDouble() - 0.5) * 0.15;
            double side = Math.abs(Math.cos(angle));
            double len = size * (0.7 + 0.5 * side) * (0.85 + r.nextDouble() * 0.3);
            int steps = (int) (len * 1.5);
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);
            double gravity = 0.25 + side * 0.9;
            for (int s = 0; s <= steps; s++) {
                double u = s / (double) steps;
                double px = x + dx * len * u;
                double py = y + dy * len * u + gravity * len * u * u;
                c.set((int) Math.round(px), (int) Math.round(py), NEAR.dark);
                if (s % 2 == 0 && u > 0.12) {
                    double ll = Math.max(1.5, len * 0.2 * Math.sin(Math.PI * Math.min(1, u * 1.05)));
                    for (int sd = -1; sd <= 1; sd += 2)
                        for (int k = 1; k <= ll; k++)
                            c.set((int) Math.round(px + sd * k * 0.9), (int) Math.round(py + k * 0.45 - 0.2),
                                    k == (int) ll ? NEAR.dark : (sd < 0 ? NEAR.light : NEAR.body));
                }
            }
        }
    }

    private static void tuft(Canvas c, Random r, int x, int y, int size) {
        for (int i = 0; i < 6; i++) {
            double angle = -Math.PI * (0.8 - 0.6 * i / 5.0) + (r.nextDouble() - 0.5) * 0.2;
            double len = size * (0.6 + r.nextDouble() * 0.6);
            for (int s = 0; s <= len; s++) {
                double u = s / len;
                c.set((int) Math.round(x + Math.cos(angle) * s + u * u * (angle < -Math.PI / 2 ? -2 : 2)),
                        (int) Math.round(y + Math.sin(angle) * s), u > 0.7 ? NEAR.light : NEAR.body);
            }
        }
    }
}
