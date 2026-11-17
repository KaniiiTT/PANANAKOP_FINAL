package objects;

import static utilz.Constants.ObjectConstants.TRAP_BAMBOO_SPIKES;

public class BambooSpikeTrap extends Trap {
	public BambooSpikeTrap(int x, int y) {
		super(x, y, TRAP_BAMBOO_SPIKES);
	}
	@Override protected boolean isDangerousFrame(int frame) { return frame == 4 || frame == 5; }
}
