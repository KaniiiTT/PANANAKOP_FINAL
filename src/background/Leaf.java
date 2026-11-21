package background;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import main.Game;

/**
 * A leaf that sways down through the scene, turning as it swings, and becomes
 * inactive once it has fallen past the bottom of the view.
 */
public class Leaf extends AmbientCreature {
    private BufferedImage[] frames; // four tilts of the current colour
    private double fallSpeed;       // pixels per second
    private double windSpeed;       // pixels per second, to the right
    private double swayPhase;
    private double swayRate;
    private double swayAmplitude;

    public Leaf(double parallaxFactor, double secondsPerUpdate) {
        super(parallaxFactor, true, secondsPerUpdate);
    }

    public void launch(BufferedImage[] frames, double screenX, double fallSpeed, double windSpeed,
            double swayRate, double swayAmplitude, double swayPhase, int cameraX) {
        this.frames = frames;
        this.fallSpeed = fallSpeed;
        this.windSpeed = windSpeed;
        this.swayRate = swayRate;
        this.swayAmplitude = swayAmplitude;
        this.swayPhase = swayPhase;
        placeOnScreen(screenX, -frames[0].getHeight(), cameraX);
        setActive(true);
    }

    @Override
    public void update() {
        swayPhase += swayRate * dt();
        moveBy(windSpeed * dt(), fallSpeed * dt());
        if (getY() > Game.GAME_HEIGHT + 10)
            setActive(false);
    }

    @Override
    protected void render(Graphics2D g, int screenX, int screenY) {
        // The leaf tilts one way while swinging right and the other way while swinging left.
        int tilt = (int) ((Math.cos(swayPhase) + 1.0) * 1.999);
        int swing = (int) Math.round(Math.sin(swayPhase) * swayAmplitude);
        g.drawImage(frames[tilt], screenX + swing, screenY, null);
    }
}
