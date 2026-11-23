package background;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;

import main.Game;

/**
 * A shooting star: a bright head with a fading tail that streaks diagonally
 * across the sky in about a second and is then inactive again.
 *
 * It fades in at the start and out at the end of its short life. The colours
 * of the tail are built once and shared by every shooting star.
 */
public class ShootingStar extends AmbientCreature {
    private static final int TAIL_SEGMENTS = 24;
    private static final double FADE_IN = 0.12;   // share of the life spent fading in
    private static final double FADE_OUT = 0.40;  // share of the life spent fading out

    private final AlphaSteps alpha = AlphaSteps.shared();
    private final Color[] tail;
    private final int pixel;
    private final double segmentLength; // pixels between tail segments
    private double directionX;
    private double directionY;
    private double speed; // pixels per second
    private double age;
    private double life;

    public ShootingStar(Color[] tail, int pixel, double secondsPerUpdate) {
        super(0.0, false, secondsPerUpdate); // the sky does not follow the camera
        this.tail = tail;
        this.pixel = pixel;
        this.segmentLength = pixel * 1.5;
    }

    /** A tail that is white at the head and fades to nothing, shared by all shooting stars. */
    public static Color[] createTail() {
        Color[] colors = new Color[TAIL_SEGMENTS];
        for (int i = 0; i < colors.length; i++) {
            double t = i / (double) (colors.length - 1);
            int alpha = (int) Math.round(255 * Math.pow(1 - t, 1.2));
            colors[i] = new Color(235, 240 - (int) (30 * t), 255, alpha);
        }
        return colors;
    }

    /**
     * @param angle direction of travel in radians (0 = right, positive = down)
     * @param life  seconds the streak lasts
     */
    public void launch(double screenX, double screenY, double angle, double speed, double life, int cameraX) {
        this.directionX = Math.cos(angle);
        this.directionY = Math.sin(angle);
        this.speed = speed;
        this.life = life;
        this.age = 0;
        placeOnScreen(screenX, screenY, cameraX);
        setActive(true);
    }

    @Override
    public void update() {
        age += dt();
        moveBy(directionX * speed * dt(), directionY * speed * dt());
        if (age >= life || getY() > Game.GAME_HEIGHT || screenX() < -80 || screenX() > Game.GAME_WIDTH + 80)
            setActive(false);
    }

    @Override
    protected void render(Graphics2D g, int screenX, int screenY) {
        double t = age / life;
        double fade = Math.min(1.0, Math.min(t / FADE_IN, (1.0 - t) / FADE_OUT));
        if (alpha.index(fade) == 0)
            return;
        Composite original = g.getComposite();
        g.setComposite(alpha.get(fade));
        for (int i = 0; i < tail.length; i++) {
            g.setColor(tail[i]);
            // The head is a little bigger than the tail pixels.
            int size = i < 2 ? pixel + 1 : pixel;
            g.fillRect((int) Math.round(screenX - directionX * segmentLength * i),
                    (int) Math.round(screenY - directionY * segmentLength * i), size, size);
        }
        g.setComposite(original);
    }
}
