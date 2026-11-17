package objects;

import static utilz.Constants.ObjectConstants.TRAP_ROLLING_BOULDER;

public class RollingBoulderTrap extends Trap {
	public RollingBoulderTrap(int x, int y) {
		super(x, y, TRAP_ROLLING_BOULDER);
	}
	@Override protected boolean isAlwaysActive() { return true; }
	@Override protected boolean isDangerousFrame(int frame) { return true; }
}
