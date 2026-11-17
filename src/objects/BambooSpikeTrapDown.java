package objects;

import static utilz.Constants.ObjectConstants.TRAP_BAMBOO_SPIKES_DOWN;

public class BambooSpikeTrapDown extends Trap {
	public BambooSpikeTrapDown(int x, int y) {
		super(x, y, TRAP_BAMBOO_SPIKES_DOWN);
	}
	@Override protected boolean isDangerousFrame(int frame) { return frame == 4 || frame == 5; }
}
