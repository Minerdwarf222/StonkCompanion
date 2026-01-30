package net.stonkcompanion.main;

import java.util.HashMap;
import java.util.List;

import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

public class Barrel {

	public enum BarrelTypes {
		BASE,
		FOREX,
		STONK
	}
	
	public static int transaction_lifetime = 15*60*20; // Measured in ticks. Thus 15 mins is 15 mins * 60 seconds per minute * 20 ticks per second.
	
	public String label = "";
	public String coords = "";
	public int time_since_last_movement = 0;
	public HashMap<String, Integer> barrel_transactions = new HashMap<>();
	public double[] barrel_actions = new double[]{0.0, 0.0};
	public boolean barrel_transaction_validity = true;
	public String barrel_transaction_solution = "";
	public String mistrade_text_message = "";
	public String fairprice_text_message = "";
	public String fairprice_gui_message = "";
	public Text[][] gui_text = null;
	
	public String previous_action_name_take = "";
	public int previous_action_qty_take = 0;
	public String previous_action_name_put = "";
	public int previous_action_qty_put = 0;
	
	public BarrelTypes barrel_type = BarrelTypes.BASE;

	public Barrel (String label, String coords) {
		this.label = label;
		this.coords = coords;
	}
	
	public void increment_time() { time_since_last_movement++;}
	public void reset_time() {time_since_last_movement = 0;}
	public boolean is_time_over() {return time_since_last_movement >= transaction_lifetime;}
	
	// For subclass usages. Since subclasses should override these if they have them, and all of them should have these 4 functions just different implementations.
	public boolean validateTransaction() {return true;}
	public void generateGuiText() {}
	public void updateGuiTimestamp() {}
	public void calulateFairPrice(List<Slot> items) {}
	public void onClickActionAdd(String taken_item_name, int item_qty_taken, String put_item_name, int item_qty_put) {}
	public void convertSolutionToCompressed() {}
	
	public String toString() {
		return "Label: " + label + " Coords: " + coords + " time: " + time_since_last_movement;
	}
	
}
