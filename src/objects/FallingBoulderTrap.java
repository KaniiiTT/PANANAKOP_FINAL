package objects;

import static utilz.Constants.ObjectConstants.TRAP_FALLING_BOULDER;

public class FallingBoulderTrap extends Trap {
	public FallingBoulderTrap(int x, int y) {
		super(x, y, TRAP_FALLING_BOULDER);
	}
	@Override protected boolean isOneShot() { return true; }
	@Override protected boolean isDangerousFrame(int frame) { return frame == 4; }
}
