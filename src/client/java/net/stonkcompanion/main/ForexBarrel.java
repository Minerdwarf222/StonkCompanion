package net.stonkcompanion.main;

public class ForexBarrel extends Barrel {

	public ForexBarrel(String label, String coords) {
		super(label, coords);
		this.barrel_type = BarrelTypes.FOREX;
	}

}
