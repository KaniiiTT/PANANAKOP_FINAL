package background;

import java.awt.image.BufferedImage;

/**
 * A little ground bird: it pecks at the grass, hops a few steps, and takes off
 * when the player comes close or when evening arrives.
 */
final class GroundBird extends GroundCritter {
    private enum State { PECK, HOP, FLY }

    private static final String[] STAND = { "...........", "......bbb..", ".....bbbed.", ".....bbbb..", "..aabbbbb..",
            ".aabbbbbc..", ".aaabbcc...", "..aabbc....", "....f.f...." };
    private static final String[] PECK = { "...........", "...........", "...........", "..aabbb....", ".aaabbbbb..",
            ".aabbbbbbbe", ".aaabbccbbd", "..aabbc....", "....f.f...." };
    private static final String[] WING_UP = { ".....a.....", "....aab....", "...aabb.bb.", "..aabbbbbed", ".aaabbbbbbd",
            ".aabbbcc...", "..aabcc....", "...........", "..........." };
    private static final String[] WING_DOWN = { "...........", "......bb...", ".....bbbed.", "..aabbbbbbd", ".aaabbbbbb.",
            ".aaabbcc.b.", "..aabc..bb.", "......b.b..", "..........." };

    private static BufferedImage[] sprites;
    private static BufferedImage[] spritesLeft;

    private State state;
    private double timer;
    private int hopsLeft;
    private double hopClock;
    private double hopFrom;
    private double hopDistance;
    private double flightClock;
    private double flySpeedX;
    private double flySpeedY;

    GroundBird(GroundLifeLayer world) {
        super(world);
        if (sprites == null) {
            int unit = 2;
            int[] palette = { 0xFF6B5238, 0xFF9A7B52, 0xFFD8C9A3, 0xFFE0A030, 0xFF1A1410, 0xFF4A3A28 };
            sprites = new BufferedImage[] { PixelSprites.create(STAND, unit, palette),
                    PixelSprites.create(PECK, unit, palette), PixelSprites.create(WING_UP, unit, palette),
                    PixelSprites.create(WING_DOWN, unit, palette) };
            spritesLeft = mirrored(sprites);
        }
    }

    @Override
    boolean livesNow(DayNightCycle cycle) {
        return cycle.getNightness() < 0.25;
    }

    @Override
    protected void begin() {
        state = State.PECK;
        timer = 0.7 + world.random().nextDouble() * 1.3;
        hopClock = 0;
    }

    @Override
    protected void act(double dt) {
        switch (state) {
        case PECK:
            timer -= dt;
            if (playerNear(2.4) || !livesNow(world.getCycle()) && world.getCycle().getNightness() > 0.4)
                takeOff();
            else if (timer <= 0)
                startHop();
            break;
        case HOP:
            hopClock += dt;
            double t = Math.min(1, hopClock / 0.20);
            x = hopFrom + facing * hopDistance * t;
            lift = 9 * 4 * t * (1 - t);
            if (t >= 1) {
                lift = 0;
                if (--hopsLeft <= 0) {
                    state = State.PECK;
                    timer = 0.6 + world.random().nextDouble() * 1.4;
                } else
                    startHop();
            }
            if (playerNear(2.4))
                takeOff();
            break;
        default:
            flightClock += dt;
            x += flySpeedX * dt;
            flySpeedY -= 150 * dt; // climbs faster and faster (up is negative)
            lift -= flySpeedY * dt;
            if (flightClock > 1.5)
                alpha = Math.max(0, 1 - (flightClock - 1.5) / 0.7);
            if (flightClock > 2.2)
                active = false;
        }
    }

    private void startHop() {
        if (state == State.PECK) {
            hopsLeft = 1 + world.random().nextInt(3);
            if (world.random().nextInt(3) == 0)
                facing = -facing;
        }
        hopDistance = 12 + world.random().nextInt(11);
        // turn round at the edge of the grass instead of hopping off it
        if (!groundAt(x + facing * (hopDistance + 8)))
            facing = -facing;
        if (!groundAt(x + facing * (hopDistance + 8))) {
            state = State.PECK;
            timer = 1;
            return;
        }
        state = State.HOP;
        hopClock = 0;
        hopFrom = x;
    }

    private void takeOff() {
        state = State.FLY;
        facing = x >= world.getPlayerX() ? 1 : -1;
        flySpeedX = facing * (90 + world.random().nextInt(60));
        flySpeedY = -40;
        flightClock = 0;
    }

    @Override
    protected BufferedImage currentSprite() {
        BufferedImage[] set = facing >= 0 ? sprites : spritesLeft;
        switch (state) {
        case PECK:
            return set[((int) (timer * 4)) % 2 == 0 ? 1 : 0];
        case HOP:
            return set[0];
        default:
            return set[((int) (flightClock * 14)) % 2 == 0 ? 2 : 3];
        }
    }
}
