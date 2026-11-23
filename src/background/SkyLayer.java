package background;

import java.awt.image.BufferedImage;

/** The sky never scrolls: its parallax factor is fixed at 0. */
public class SkyLayer extends TiledBackgroundLayer {

    /**
     * The sky picture has a few faint clouds painted into it. They can never move,
     * so they look like marks on the sky once the real, drifting cloud layers are
     * in place. This returns a copy in which every row has been put back to its
     * own sky colour (the most common colour of that row), which removes them and
     * changes nothing else. The moving cloud layers take their place.
     */
    public static BufferedImage withoutClouds(BufferedImage art) {
        int w = art.getWidth();
        int h = art.getHeight();
        BufferedImage clean = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] row = new int[w];
        int[] red = new int[w];
        int[] green = new int[w];
        int[] blue = new int[w];
        for (int y = 0; y < h; y++) {
            art.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                red[x] = (row[x] >> 16) & 0xFF;
                green[x] = (row[x] >> 8) & 0xFF;
                blue[x] = row[x] & 0xFF;
            }
            // The middle value of each colour channel along the row; the clouds are only a small part of it.
            java.util.Arrays.sort(red);
            java.util.Arrays.sort(green);
            java.util.Arrays.sort(blue);
            int sky = 0xFF000000 | (red[w / 2] << 16) | (green[w / 2] << 8) | blue[w / 2];
            java.util.Arrays.fill(row, sky);
            clean.setRGB(0, y, w, 1, row, 0, w);
        }
        return clean;
    }

    public SkyLayer(BufferedImage image) {
        super(image, 0.0, 0);
    }

    @Override
    protected double horizontalPosition(int cameraX) {
        return 0.0;
    }
}
