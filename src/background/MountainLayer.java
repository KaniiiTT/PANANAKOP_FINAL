package background;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import main.Game;

/**
 * Distant mountains that darken with the night sky.
 *
 * A mountain range is far away, so at night it must sink into the dark sky
 * instead of staying a pale blue shape in front of it. The layer owns a second,
 * night-coloured copy of its artwork, made once when the layer is built, and
 * cross-fades it over the day artwork as the night comes (one alpha blit, from
 * the shared pre-built composites; nothing is created while playing).
 *
 * It extends {@link ForestLayer}, so the scrolling, wraparound and vertical
 * parallax are exactly those of every other scenery layer.
 */
public class MountainLayer extends ForestLayer {
    private final DayNightCycle cycle;
    private final BufferedImage nightImage;

    /**
     * @param nightColor    colour the mountains take at full night
     * @param nightStrength how far towards that colour they go (0..1)
     */
    public MountainLayer(BufferedImage art, double parallaxFactor, double verticalFactor, DayNightCycle cycle,
            Color nightColor, float nightStrength) {
        super(art, parallaxFactor, verticalFactor);
        this.cycle = cycle;
        this.nightImage = darkened(getImage(), nightColor, nightStrength);
    }

    private static BufferedImage darkened(BufferedImage day, Color tint, float strength) {
        int w = day.getWidth();
        int h = day.getHeight();
        int[] px = day.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < px.length; i++) {
            int argb = px[i];
            if ((argb >>> 24) == 0)
                continue;
            int r = Math.round(((argb >> 16) & 0xFF) * (1 - strength) + tint.getRed() * strength);
            int g = Math.round(((argb >> 8) & 0xFF) * (1 - strength) + tint.getGreen() * strength);
            int b = Math.round((argb & 0xFF) * (1 - strength) + tint.getBlue() * strength);
            px[i] = (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    }

    @Override
    protected void paint(Graphics2D g, int x, int y) {
        super.paint(g, x, y);
        double night = cycle.getNightness();
        if (night < 0.02)
            return;
        Composite original = g.getComposite();
        g.setComposite(AlphaSteps.shared().get(night));
        g.drawImage(nightImage, x, y, null);
        if (x + getTileWidth() < Game.GAME_WIDTH)
            g.drawImage(nightImage, x + getTileWidth(), y, null);
        g.setComposite(original);
    }
}
