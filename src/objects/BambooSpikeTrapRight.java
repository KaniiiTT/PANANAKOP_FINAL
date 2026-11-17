package objects;

import static utilz.Constants.ObjectConstants.TRAP_BAMBOO_SPIKES_RIGHT;

public class BambooSpikeTrapRight extends Trap {
	public BambooSpikeTrapRight(int x, int y) {
		super(x, y, TRAP_BAMBOO_SPIKES_RIGHT);
	}
	@Override protected boolean isDangerousFrame(int frame) { return frame == 4 || frame == 5; }
}
