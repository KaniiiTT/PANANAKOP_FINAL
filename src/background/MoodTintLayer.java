package background;

import java.awt.Color;
import java.awt.Graphics2D;

import main.Game;

/**
 * Lays the current mood colour (orange sunset, blue night...) over everything
 * drawn before it. It draws nothing during the day, when the tint is fully
 * transparent.
 */
public class MoodTintLayer implements BackgroundLayer {
    private final DayNightCycle cycle;

    public MoodTintLayer(DayNightCycle cycle) {
        this.cycle = cycle;
    }

    @Override
    public void draw(Graphics2D g, int cameraX) {
        Color tint = cycle.getTint();
        if (tint.getAlpha() == 0)
            return;
        Color original = g.getColor();
        g.setColor(tint);
        g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
        g.setColor(original);
    }
}
