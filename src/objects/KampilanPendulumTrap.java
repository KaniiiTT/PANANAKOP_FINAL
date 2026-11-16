package objects;

import static utilz.Constants.ObjectConstants.TRAP_KAMPILAN_PENDULUM;

public class KampilanPendulumTrap extends Trap {
	public KampilanPendulumTrap(int x, int y) {
		super(x, y, TRAP_KAMPILAN_PENDULUM);
	}
	@Override protected boolean isAlwaysActive() { return true; }
	@Override protected boolean isDangerousFrame(int frame) { return frame == 11 || frame == 0 || frame == 1 || frame == 5 || frame == 6 || frame == 7; }
}
