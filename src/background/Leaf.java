package background;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import main.Game;

/**
 * A leaf that comes loose from a tree crown, sways down for a short distance,
 * turning as it swings, and fades away before it reaches the bushes. It is
 * inactive again once it has finished falling.
 */
public class Leaf extends AmbientCreature {
    private static final double FADE_IN_PIXELS = Game.GAME_HEIGHT * 0.02; // it appears out of the foliage
    private static final double FADE_OUT_SHARE = 0.35;                    // last share of the fall spent fading

    private final AlphaSteps alpha = AlphaSteps.shared();
    private BufferedImage[] frames; // four tilts of the current colour
    private double fallSpeed;       // pixels per second
    private double windSpeed;       // pixels per second, to the right
    private double swayPhase;
    private double swayRate;
    private double swayAmplitude;
    private double startY;
    private double fallDistance;

    public Leaf(double parallaxFactor, double secondsPerUpdate) {
        super(parallaxFactor, true, secondsPerUpdate);
    }

    /**
     * @param screenX      where the leaf starts, inside a tree crown
     * @param screenY      height where the leaf starts
     * @param fallDistance how far it falls (pixels) before it has faded out
     */
    public void launch(BufferedImage[] frames, double screenX, double screenY, double fallDistance, double fallSpeed,
            double windSpeed, double swayRate, double swayAmplitude, double swayPhase, int cameraX) {
        this.frames = frames;
        this.fallDistance = fallDistance;
        this.fallSpeed = fallSpeed;
        this.windSpeed = windSpeed;
        this.swayRate = swayRate;
        this.swayAmplitude = swayAmplitude;
        this.swayPhase = swayPhase;
        this.startY = screenY;
        placeOnScreen(screenX, screenY, cameraX);
        setActive(true);
    }

    @Override
    public void update() {
        swayPhase += swayRate * dt();
        moveBy(windSpeed * dt(), fallSpeed * dt());
        if (getY() - startY >= fallDistance || getY() > Game.GAME_HEIGHT + 10)
            setActive(false);
    }

    @Override
    protected void render(Graphics2D g, int screenX, int screenY) {
        double travelled = getY() - startY;
        double fade = Math.min(1.0, Math.min(travelled / FADE_IN_PIXELS, (1.0 - travelled / fallDistance) / FADE_OUT_SHARE));
        if (alpha.index(fade) == 0)
            return;
        // The leaf tilts one way while swinging right and the other way while swinging left.
        int tilt = (int) ((Math.cos(swayPhase) + 1.0) * 1.999);
        int swing = (int) Math.round(Math.sin(swayPhase) * swayAmplitude);
        Composite original = g.getComposite();
        if (fade < 0.99)
            g.setComposite(alpha.get(fade));
        g.drawImage(frames[tilt], screenX + swing, screenY, null);
        g.setComposite(original);
    }
}
