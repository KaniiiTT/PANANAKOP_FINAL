package objects;

import static utilz.Constants.ObjectConstants.TRAP_BLOWGUN;

public class BlowgunDartTrap extends Trap {
	public BlowgunDartTrap(int x, int y) {
		super(x, y, TRAP_BLOWGUN);
	}
	@Override protected boolean isOneShot() { return true; }
	@Override protected boolean isDangerousFrame(int frame) { return frame == 3; }
}
