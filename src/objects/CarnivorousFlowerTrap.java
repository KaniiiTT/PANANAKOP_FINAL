package objects;

import static utilz.Constants.ObjectConstants.TRAP_CARNIVOROUS_FLOWER;

public class CarnivorousFlowerTrap extends Trap {
	public CarnivorousFlowerTrap(int x, int y) {
		super(x, y, TRAP_CARNIVOROUS_FLOWER);
	}
	@Override protected boolean isDangerousFrame(int frame) { return frame == 6; }
}
