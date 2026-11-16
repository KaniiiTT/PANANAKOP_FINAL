package objects;

import static utilz.Constants.ObjectConstants.TRAP_BOUNCE_PAD;

public class BambooBouncePad extends Trap {
	public BambooBouncePad(int x, int y) {
		super(x, y, TRAP_BOUNCE_PAD);
	}
	@Override protected boolean isBouncePad() { return true; }
	@Override protected boolean isDangerousFrame(int frame) { return frame >= 3 && frame <= 5; }
}
