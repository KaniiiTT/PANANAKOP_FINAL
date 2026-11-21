package background;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import main.Game;

/**
 * A few soft diagonal beams of light that slowly brighten and fade.
 *
 * Each beam is drawn once into its own image at construction. Every frame only
 * reuses that image with a pre-built alpha level, so nothing is allocated while
 * playing. The beams follow the camera at a slow parallax speed and wrap around
 * the view like the other layers.
 */
public class LightShaftLayer implements BackgroundLayer {
    private static final int ALPHA_STEPS = 32;
    private static final int LIGHT_RGB = 0xFFF4D6; // warm white
    private static final double MIN_BRIGHTNESS = 0.30; // share of max alpha at the dimmest point

    /** One beam: its artwork, where it sits and how its brightness pulses. */
    private static final class Shaft {
        private final BufferedImage image;
        private final int baseX;
        private final double phaseStep; // radians per game update
        private double phase;

        private Shaft(BufferedImage image, int baseX, double periodSeconds, double startPhase, double ups) {
            this.image = image;
            this.baseX = baseX;
            this.phaseStep = 2 * Math.PI / (periodSeconds * ups);
            this.phase = startPhase;
        }
    }

    private final ArrayList<Shaft> shafts = new ArrayList<>();
    private final AlphaComposite[] levels = new AlphaComposite[ALPHA_STEPS + 1];
    private final double parallaxFactor;
    private final DayNightCycle cycle;

    /**
     * @param parallaxFactor camera follow speed of the beams
     * @param maxAlpha       opacity of a beam at its brightest, 0..1 (keep it small)
     * @param updatesPerSecond how often update() is called by the game loop
     * @param cycle          day/night clock; the beams are sunlight, so they fade out at night
     */
    public LightShaftLayer(double parallaxFactor, float maxAlpha, double updatesPerSecond, DayNightCycle cycle) {
        this.parallaxFactor = parallaxFactor;
        this.cycle = cycle;
        for (int i = 0; i <= ALPHA_STEPS; i++)
            levels[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, maxAlpha * i / ALPHA_STEPS);

        int w = Game.GAME_WIDTH;
        int h = Game.GAME_HEIGHT;
        // fraction of view width for: position, beam width, pulse period (s), start phase
        addShaft((int) (w * 0.16), (int) (w * 0.075), h, 13.0, 0.0, updatesPerSecond);
        addShaft((int) (w * 0.50), (int) (w * 0.110), h, 17.0, 2.1, updatesPerSecond);
        addShaft((int) (w * 0.79), (int) (w * 0.060), h, 11.0, 4.2, updatesPerSecond);
    }

    private void addShaft(int baseX, int beamWidth, int viewHeight, double period, double phase, double ups) {
        shafts.add(new Shaft(createBeam(beamWidth, viewHeight), baseX, period, phase, ups));
    }

    /** Renders one beam leaning down to the right, fading out softly at the sides and bottom. */
    private BufferedImage createBeam(int beamWidth, int viewHeight) {
        int height = (int) (viewHeight * 0.85);
        int skew = (int) (viewHeight * 0.22);
        BufferedImage image = new BufferedImage(beamWidth + skew, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            double vertical = Math.pow(1.0 - y / (double) height, 1.5);
            int left = (int) Math.round(skew * y / (double) height);
            for (int i = 0; i < beamWidth; i++) {
                double side = Math.sin(Math.PI * (i + 0.5) / beamWidth);
                int a = (int) Math.round(255 * side * side * vertical);
                image.setRGB(left + i, y, (a << 24) | LIGHT_RGB);
            }
        }
        return image;
    }

    @Override
    public void update() {
        for (Shaft shaft : shafts) {
            shaft.phase += shaft.phaseStep;
            if (shaft.phase > 2 * Math.PI)
                shaft.phase -= 2 * Math.PI;
        }
    }

    @Override
    public void draw(Graphics2D g, int cameraX) {
        double daylight = cycle.getDaylight();
        if (daylight < 0.02)
            return;
        int viewWidth = Game.GAME_WIDTH;
        Composite original = g.getComposite();
        try {
            for (Shaft shaft : shafts) {
                double pulse = 0.5 + 0.5 * Math.sin(shaft.phase); // 0..1
                double brightness = (MIN_BRIGHTNESS + (1.0 - MIN_BRIGHTNESS) * pulse) * daylight;
                g.setComposite(levels[(int) Math.round(brightness * ALPHA_STEPS)]);

                double position = shaft.baseX - cameraX * parallaxFactor;
                int x = (int) (position - Math.floor(position / viewWidth) * viewWidth);
                g.drawImage(shaft.image, x, 0, null);
                if (x + shaft.image.getWidth() > viewWidth)
                    g.drawImage(shaft.image, x - viewWidth, 0, null);
            }
        } finally {
            g.setComposite(original);
        }
    }
}
