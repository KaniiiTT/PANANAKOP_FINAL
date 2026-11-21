package background;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

import main.Game;

/**
 * A speck of pollen drifting slowly upwards and sideways while its brightness
 * twinkles. It loops around the field forever.
 */
public class Pollen extends AmbientCreature {
    private final Color[] brightness; // shared, dimmest to brightest
    private final int size;
    private final double windSpeed;   // pixels per second
    private final double riseSpeed;   // pixels per second (upwards)
    private final double minY;
    private final double maxY;
    private final double bobRate;
    private final double bobAmplitude;
    private final double twinkleRate;
    private double bobPhase;
    private double twinklePhase;

    public Pollen(Color[] brightness, int size, double parallaxFactor, double secondsPerUpdate, Random random,
            double minY, double maxY) {
        super(parallaxFactor, true, secondsPerUpdate);
        this.brightness = brightness;
        this.size = size;
        this.minY = minY;
        this.maxY = maxY;
        this.windSpeed = Game.GAME_WIDTH * (0.008 + 0.010 * random.nextDouble());
        this.riseSpeed = Game.GAME_HEIGHT * (0.003 + 0.004 * random.nextDouble());
        this.bobRate = 0.8 + random.nextDouble() * 1.2;
        this.bobAmplitude = 6 + random.nextDouble() * 8;
        this.twinkleRate = 0.8 + random.nextDouble() * 1.4;
        this.bobPhase = random.nextDouble() * Math.PI * 2;
        this.twinklePhase = random.nextDouble() * Math.PI * 2;
        placeOnScreen(random.nextDouble() * Game.GAME_WIDTH, minY + random.nextDouble() * (maxY - minY), 0);
        setActive(true);
    }

    @Override
    public void update() {
        bobPhase += bobRate * dt();
        twinklePhase += twinkleRate * dt();
        // The bob term is the derivative of a sine, so the speck sways without drifting off course.
        moveBy(windSpeed * dt(), (-riseSpeed + Math.cos(bobPhase) * bobAmplitude * bobRate) * dt());
        if (getY() < minY)
            setY(maxY);
    }

    @Override
    protected void render(Graphics2D g, int screenX, int screenY) {
        double twinkle = 0.5 + 0.5 * Math.sin(twinklePhase);
        g.setColor(brightness[(int) Math.round(twinkle * (brightness.length - 1))]);
        g.fillRect(screenX, screenY, size, size);
    }
}
