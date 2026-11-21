package background;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import main.Game;

/**
 * A bird crossing the sky in a straight line with a gentle bob and a looping
 * wing flap. It becomes inactive once it has flown off the far side.
 */
public class Bird extends AmbientCreature {
    // Order in which the wing frames are shown (up ... down ... up).
    private static final int[] FLAP_CYCLE = { 0, 1, 2, 3, 2, 1 };
    private static final double FLAPS_PER_SECOND = 9.0; // frames shown per second

    private final BufferedImage[] frames;
    private int direction; // 1 = flying right, -1 = flying left
    private double speed;  // pixels per second
    private double baseY;
    private double bobPhase;
    private double bobAmplitude;
    private double bobRate;
    private double flapClock;

    public Bird(BufferedImage[] frames, double parallaxFactor, double secondsPerUpdate) {
        super(parallaxFactor, false, secondsPerUpdate);
        this.frames = frames;
    }

    public int getSpriteWidth() {
        return frames[0].getWidth();
    }

    /**
     * @param screenX where the bird appears (just outside the view)
     * @param flapOffset random starting point of the wing cycle
     */
    public void launch(int direction, double screenX, double y, double speed, double flapOffset, double bobPhase,
            int cameraX) {
        this.direction = direction;
        this.speed = speed;
        this.baseY = y;
        this.bobPhase = bobPhase;
        this.bobAmplitude = Game.GAME_HEIGHT * 0.006;
        this.bobRate = 1.6;
        this.flapClock = flapOffset;
        placeOnScreen(screenX, y, cameraX);
        setActive(true);
    }

    @Override
    public void update() {
        moveBy(direction * speed * dt(), 0);
        bobPhase += bobRate * dt();
        flapClock += FLAPS_PER_SECOND * dt();
        setY(baseY + Math.sin(bobPhase) * bobAmplitude);

        double sx = screenX();
        boolean leftTheView = (direction > 0 && sx > Game.GAME_WIDTH + WRAP_MARGIN)
                || (direction < 0 && sx < -getSpriteWidth() - WRAP_MARGIN);
        // The camera can also carry a bird far behind its flight direction.
        boolean leftBehind = sx < -3.0 * Game.GAME_WIDTH || sx > 4.0 * Game.GAME_WIDTH;
        if (leftTheView || leftBehind)
            setActive(false);
    }

    @Override
    protected void render(Graphics2D g, int screenX, int screenY) {
        g.drawImage(frames[FLAP_CYCLE[(int) flapClock % FLAP_CYCLE.length]], screenX, screenY, null);
    }
}
