package background;

import java.awt.image.BufferedImage;

/** The sky never scrolls: its parallax factor is fixed at 0. */
public class SkyLayer extends TiledBackgroundLayer {

    public SkyLayer(BufferedImage image) {
        super(image, 0.0, 0);
    }

    @Override
    protected double horizontalPosition(int cameraX) {
        return 0.0;
    }
}
