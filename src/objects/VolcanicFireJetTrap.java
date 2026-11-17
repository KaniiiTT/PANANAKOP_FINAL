package objects;

import static utilz.Constants.ObjectConstants.TRAP_VOLCANIC_FIRE_JET;

public class VolcanicFireJetTrap extends Trap {
	public VolcanicFireJetTrap(int x, int y) {
		super(x, y, TRAP_VOLCANIC_FIRE_JET);
	}
	@Override protected boolean isDangerousFrame(int frame) { return frame >= 4 && frame <= 8; }
}
