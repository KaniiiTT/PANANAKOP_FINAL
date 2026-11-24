package background;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A small animal that lives on the ground of the level: it stands on a grassy
 * surface tile, moves along it and runs or flies away when the player comes close.
 *
 * Abstraction: the base class owns what every ground animal shares (position,
 * facing, fading, drawing a sprite on the ground) and leaves two things to the
 * subclasses: WHEN the animal lives ({@link #livesNow}) and HOW it behaves
 * ({@link #act}). {@link GroundLifeLayer} keeps a fixed pool of them in one list
 * and treats them all alike (polymorphism), so no animal is created while playing.
 */
abstract class GroundCritter {
    protected final GroundLifeLayer world;
    protected boolean active;
    protected double x; // world position of the feet
    protected double feetY;
    protected double lift; // height above the ground (hops, flight)
    protected int facing = 1; // 1 = looking right, -1 = looking left
    protected double alpha = 1;

    protected GroundCritter(GroundLifeLayer world) {
        this.world = world;
    }

    boolean isActive() {
        return active;
    }

    /** True when the time of day suits this animal (a bird by day, a frog by night). */
    abstract boolean livesNow(DayNightCycle cycle);

    /** Puts the animal on the ground tile (column, row) and starts its behaviour. */
    void spawn(int column, int row) {
        int tile = world.getTileSize();
        x = column * tile + tile / 2.0;
        feetY = row * tile;
        lift = 0;
        alpha = 1;
        facing = world.random().nextBoolean() ? 1 : -1;
        active = true;
        begin();
    }

    protected abstract void begin();

    /** One step of behaviour; {@code dt} is in seconds. */
    protected abstract void act(double dt);

    /** The picture to draw now, already turned to the way the animal faces. */
    protected abstract BufferedImage currentSprite();

    /** Mirror images of the sprites, made once, so facing left costs no more than facing right. */
    protected static BufferedImage[] mirrored(BufferedImage[] sprites) {
        BufferedImage[] result = new BufferedImage[sprites.length];
        for (int i = 0; i < sprites.length; i++) {
            int w = sprites[i].getWidth();
            int h = sprites[i].getHeight();
            BufferedImage flipped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    flipped.setRGB(w - 1 - x, y, sprites[i].getRGB(x, y));
            result[i] = flipped;
        }
        return result;
    }

    void update(double dt) {
        if (active)
            act(dt);
    }

    void draw(Graphics2D g, int cameraX) {
        if (!active || alpha <= 0.01)
            return;
        BufferedImage sprite = currentSprite();
        int w = sprite.getWidth();
        int h = sprite.getHeight();
        int sx = (int) Math.round(x - cameraX - w / 2.0);
        int sy = (int) Math.round(feetY - lift - h + 2);
        Composite original = g.getComposite();
        if (alpha < 0.99)
            g.setComposite(AlphaSteps.shared().get(alpha));
        g.drawImage(sprite, sx, sy, null);
        g.setComposite(original);
    }

    /** True while the ground to the side is still a grassy surface at the same height. */
    protected boolean groundAt(double worldX) {
        int tile = world.getTileSize();
        return world.isSurface((int) Math.floor(worldX / tile), (int) Math.round(feetY / tile));
    }

    protected boolean playerNear(double tilesAway) {
        int tile = world.getTileSize();
        return Math.abs(world.getPlayerX() - x) < tilesAway * tile
                && Math.abs(world.getPlayerFeetY() - feetY) < 2 * tile;
    }
}
