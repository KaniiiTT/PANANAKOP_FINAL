package background;

import java.awt.image.BufferedImage;

import main.Game;

/**
 * Builds the tiny pixel-art sprites of the ambient creatures from text
 * patterns, so no extra image files are needed.
 *
 * In a pattern '.' is transparent and the letters 'a', 'b', ... pick the
 * colours of the palette in order. Every sprite is rendered once, at load
 * time, and then reused for the whole game.
 */
final class PixelSprites {

    private PixelSprites() {
    }

    /** Size in screen pixels of one sprite pixel; matches the chunky look of the backdrop art. */
    static int pixelUnit() {
        return Math.max(2, Math.round(Game.GAME_WIDTH / 480f));
    }

    /**
     * @param rows    pattern rows, all of the same length
     * @param unit    screen pixels per pattern pixel
     * @param palette ARGB colours for 'a', 'b', 'c', ...
     */
    static BufferedImage create(String[] rows, int unit, int... palette) {
        int columns = rows[0].length();
        BufferedImage image = new BufferedImage(columns * unit, rows.length * unit, BufferedImage.TYPE_INT_ARGB);
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < columns; column++) {
                char symbol = rows[row].charAt(column);
                if (symbol == '.')
                    continue;
                int color = palette[symbol - 'a'];
                for (int dy = 0; dy < unit; dy++)
                    for (int dx = 0; dx < unit; dx++)
                        image.setRGB(column * unit + dx, row * unit + dy, color);
            }
        }
        return image;
    }
}
