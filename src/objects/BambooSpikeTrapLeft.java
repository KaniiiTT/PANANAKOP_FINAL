package objects;

import static utilz.Constants.ObjectConstants.TRAP_BAMBOO_SPIKES_LEFT;

public class BambooSpikeTrapLeft extends Trap {
	public BambooSpikeTrapLeft(int x, int y) {
		super(x, y, TRAP_BAMBOO_SPIKES_LEFT);
	}
	@Override protected boolean isDangerousFrame(int frame) { return frame == 4 || frame == 5; }
}
