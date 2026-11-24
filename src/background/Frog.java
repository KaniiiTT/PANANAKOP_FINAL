package background;

import java.awt.image.BufferedImage;

/** A small frog of the night: it sits still, hops now and then, and leaps away from the player. */
final class Frog extends GroundCritter {
    private static final String[] SIT = { ".d.....d.", "dbbbbbbbd", ".bbbbbbb.", "bbbcccbbb", "abbcccbba", "aa.....aa" };
    private static final String[] JUMP = { "...d.d...", "..bbbbb..", ".bbbbbbbd", "abbcccbb.", "aab.cc.b.", "a.a...aa." };

    private static BufferedImage[] sprites;
    private static BufferedImage[] spritesLeft;

    private boolean jumping;
    private double timer;
    private double clock;
    private double from;
    private double distance;
    private double duration;
    private double height;

    Frog(GroundLifeLayer world) {
        super(world);
        if (sprites == null) {
            int unit = 2;
            int[] palette = { 0xFF3D6B3A, 0xFF5F9A4A, 0xFFA7D070, 0xFFF0E070 };
            sprites = new BufferedImage[] { PixelSprites.create(SIT, unit, palette),
                    PixelSprites.create(JUMP, unit, palette) };
            spritesLeft = mirrored(sprites);
        }
    }

    @Override
    boolean livesNow(DayNightCycle cycle) {
        return cycle.getNightness() > 0.6;
    }

    @Override
    protected void begin() {
        jumping = false;
        timer = 1.2 + world.random().nextDouble() * 2.5;
    }

    @Override
    protected void act(double dt) {
        // when the morning comes the frog fades away (it hides among the plants)
        if (world.getCycle().getNightness() < 0.4) {
            alpha -= dt / 1.5;
            if (alpha <= 0)
                active = false;
        }
        if (jumping) {
            clock += dt;
            double t = Math.min(1, clock / duration);
            x = from + facing * distance * t;
            lift = height * 4 * t * (1 - t);
            if (t >= 1) {
                jumping = false;
                lift = 0;
                timer = 1.5 + world.random().nextDouble() * 3.0;
            }
        } else {
            timer -= dt;
            boolean scared = playerNear(2.0);
            if (scared || timer <= 0)
                startJump(scared);
        }
    }

    private void startJump(boolean flee) {
        facing = flee ? (x >= world.getPlayerX() ? 1 : -1) : (world.random().nextBoolean() ? 1 : -1);
        distance = flee ? 55 + world.random().nextInt(25) : 26 + world.random().nextInt(26);
        if (!groundAt(x + facing * (distance + 8)))
            facing = -facing;
        if (!groundAt(x + facing * (distance + 8))) {
            distance = 0;
            timer = 1;
            return;
        }
        duration = flee ? 0.32 : 0.38;
        height = flee ? 22 : 16;
        jumping = true;
        clock = 0;
        from = x;
    }

    @Override
    protected BufferedImage currentSprite() {
        return (facing >= 0 ? sprites : spritesLeft)[jumping ? 1 : 0];
    }
}
