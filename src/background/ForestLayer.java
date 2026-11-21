package background;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Forest artwork that scrolls with the camera at its own (slower than the world) speed.
 *
 * A distant forest can optionally be given atmospheric haze: a soft tint that
 * grows from nothing at the top of the trees to {@code hazeStrength} at the
 * bottom. The haze is applied once, to the forest's own pixels, when the layer
 * is built. It therefore never tints the sky, always stays glued to the trees,
 * and costs nothing extra while playing.
 */
public class ForestLayer extends TiledBackgroundLayer {

    public ForestLayer(BufferedImage image, double parallaxFactor) {
        super(image, parallaxFactor, 0);
    }

    /**
     * @param hazeTint     haze colour (the alpha of the colour is ignored)
     * @param hazeStrength tint strength at the bottom of the artwork, 0..1
     */
    public ForestLayer(BufferedImage image, double parallaxFactor, Color hazeTint, float hazeStrength) {
        super(applyHaze(image, hazeTint, hazeStrength), parallaxFactor, 0);
    }

    /** Returns a hazy copy; the source image is left untouched. */
    private static BufferedImage applyHaze(BufferedImage art, Color tint, float strength) {
        int w = art.getWidth();
        int h = art.getHeight();
        int[] pixels = art.getRGB(0, 0, w, h, null, 0, w);

        int topRow = h;
        for (int i = 0; i < pixels.length; i++)
            if ((pixels[i] >>> 24) != 0) {
                topRow = i / w;
                break;
            }

        int tr = tint.getRed();
        int tg = tint.getGreen();
        int tb = tint.getBlue();
        for (int y = topRow; y < h; y++) {
            double mix = strength * (y - topRow) / Math.max(1, h - 1 - topRow);
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int argb = pixels[i];
                if ((argb >>> 24) == 0)
                    continue;
                int r = (int) Math.round(((argb >> 16) & 0xFF) * (1 - mix) + tr * mix);
                int g = (int) Math.round(((argb >> 8) & 0xFF) * (1 - mix) + tg * mix);
                int b = (int) Math.round((argb & 0xFF) * (1 - mix) + tb * mix);
                pixels[i] = (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
            }
        }
        BufferedImage hazy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        hazy.setRGB(0, 0, w, h, pixels, 0, w);
        return hazy;
    }

    @Override
    protected double horizontalPosition(int cameraX) {
        return cameraOffset(cameraX);
    }
}
