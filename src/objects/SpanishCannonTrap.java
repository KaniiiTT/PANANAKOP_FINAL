package objects;

import static utilz.Constants.ObjectConstants.TRAP_SPANISH_CANNON;

public class SpanishCannonTrap extends Trap {
	public SpanishCannonTrap(int x, int y) {
		super(x, y, TRAP_SPANISH_CANNON);
	}
	@Override protected boolean isOneShot() { return true; }
	@Override protected boolean isDangerousFrame(int frame) { return frame == 3; }
}
