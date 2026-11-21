package background;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Clouds move on their own and also follow the camera very slowly.
 *
 * The drift is kept in a separate position that grows a little every update.
 * It is never reset: the base class wraps the final position around the image
 * width, so the clouds travel continuously and re-enter without a jump.
 *
 * The cloud artwork can be split into several bands with
 * {@link #splitIntoBands(BufferedImage, int)}; giving each band its own drift
 * and parallax speed makes some clouds look nearer than others.
 */
public class CloudLayer extends TiledBackgroundLayer {
    // Pixels closer than this (in x and y) belong to the same cloud group.
    private static final int GROUP_RADIUS = 3;

    // At night the clouds become this much more transparent (0 = unchanged, 1 = invisible).
    private static final double NIGHT_FADE = 0.55;

    private final double driftPerUpdate;
    private final DayNightCycle cycle;
    private double drift;

    /**
     * @param driftPerUpdate pixels the clouds drift to the right on each game
     *                       update (negative values drift left)
     * @param cycle          day/night clock; clouds turn thin and dark at night
     */
    public CloudLayer(BufferedImage image, double parallaxFactor, double driftPerUpdate, DayNightCycle cycle) {
        super(image, parallaxFactor, 0);
        this.driftPerUpdate = driftPerUpdate;
        this.cycle = cycle;
    }

    @Override
    protected double opacity() {
        return 1.0 - NIGHT_FADE * cycle.getNightness();
    }

    @Override
    public void update() {
        drift += driftPerUpdate;
        // Keep the value small so precision never degrades in long sessions.
        if (Math.abs(drift) > 1_000_000)
            drift %= 1_000_000;
    }

    @Override
    protected double horizontalPosition(int cameraX) {
        return cameraOffset(cameraX) - drift;
    }

    /**
     * Splits a cloud image into vertical bands WITHOUT cutting any cloud.
     *
     * Opaque pixels are grouped into clouds with a flood fill (a breadth-first
     * search over a queue). Groups wrap around the left/right edge, because the
     * layer repeats horizontally. Each whole group is then assigned to a band by
     * its average height. Every returned image has the size of the source, so
     * drawing all bands at the same position rebuilds the original image.
     *
     * @return bands ordered from the top of the sky (index 0) downwards
     */
    public static List<BufferedImage> splitIntoBands(BufferedImage source, int bandCount) {
        int w = source.getWidth();
        int h = source.getHeight();
        int[] argb = source.getRGB(0, 0, w, h, null, 0, w);

        int[] group = new int[w * h]; // 0 = not visited / transparent
        int[] queue = new int[w * h];
        List<long[]> stats = new ArrayList<>(); // per group: pixel count, sum of y
        int minY = h;
        int maxY = -1;

        for (int start = 0; start < argb.length; start++) {
            if ((argb[start] >>> 24) == 0 || group[start] != 0)
                continue;
            int id = stats.size() + 1;
            long count = 0;
            long sumY = 0;
            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            group[start] = id;
            while (head < tail) {
                int p = queue[head++];
                int px = p % w;
                int py = p / w;
                count++;
                sumY += py;
                minY = Math.min(minY, py);
                maxY = Math.max(maxY, py);
                for (int dy = -GROUP_RADIUS; dy <= GROUP_RADIUS; dy++) {
                    int ny = py + dy;
                    if (ny < 0 || ny >= h)
                        continue;
                    for (int dx = -GROUP_RADIUS; dx <= GROUP_RADIUS; dx++) {
                        int nx = Math.floorMod(px + dx, w); // wraps across the seam
                        int n = ny * w + nx;
                        if ((argb[n] >>> 24) != 0 && group[n] == 0) {
                            group[n] = id;
                            queue[tail++] = n;
                        }
                    }
                }
            }
            stats.add(new long[] { count, sumY });
        }

        List<BufferedImage> bands = new ArrayList<>();
        int[][] bandPixels = new int[bandCount][w * h];
        int span = Math.max(1, maxY - minY + 1);
        for (int p = 0; p < argb.length; p++) {
            if (group[p] == 0)
                continue;
            long[] s = stats.get(group[p] - 1);
            double centerY = (double) s[1] / s[0];
            int band = (int) ((centerY - minY) * bandCount / span);
            band = Math.max(0, Math.min(bandCount - 1, band));
            bandPixels[band][p] = argb[p];
        }
        for (int b = 0; b < bandCount; b++) {
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, w, h, bandPixels[b], 0, w);
            bands.add(image);
        }
        return bands;
    }
}
