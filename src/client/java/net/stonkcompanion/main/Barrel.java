package net.stonkcompanion.main;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class Barrel {

	public enum BarrelTypes {
		BASE,
		FOREX,
		STONK
	}
	
	public static int transaction_lifetime = 15*60*20; // Measured in ticks. Thus 15 mins is 15 mins * 60 seconds per minute * 20 ticks per second.
	
	public String label = "";
	public String coords = "";
	public String category = "";
	public int fairprice_dir = 0;
	public int barrel_number = -1;
	public int time_since_last_movement = 0;
	public HashMap<String, Integer> barrel_transactions = new HashMap<>();
	public double[] barrel_actions = new double[]{0.0, 0.0};
	public boolean barrel_transaction_validity = true;
	public String barrel_transaction_solution = "";
	public String mistrade_text_message = "";
	public String fairprice_text_message = "";
	public String fairprice_gui_message = "";
	public Text[][] gui_text = null;
	public int gui_height = -1;
	
	public String previous_action_name_take = "";
	public int previous_action_qty_take = 0;
	public String previous_action_name_put = "";
	public int previous_action_qty_put = 0;
	
	private static final MinecraftClient client = MinecraftClient.getInstance();
	
	public static String previous_barrel_category = "";
	public static int previous_barrel_number = -1;
	public static int previous_barrel_fairprice = 0;
	
	public static String current_barrel_category = "";
	public static int current_barrel_number = -1;
	public static int current_barrel_fairprice = 0;
	
	public BarrelTypes barrel_type = BarrelTypes.BASE;

	public Barrel (String label, String coords) {
		
		if(StonkCompanionClient.latest_mod_version.isBlank()) {
			// Here we are going to check if we can get the latest version of stonkcompanion and required version.
			checkVersionSign();			
		}
		
		this.label = label.trim();
		this.coords = coords;
		splitLabel();
	}
	
	private void checkVersionSign() {
		BlockPos sign_coord = new BlockPos(-631, 38, 1067);
		
		MinecraftClient client = MinecraftClient.getInstance();
		
		if(!client.world.isClient) return;
		
		BlockEntity test_block_entity = client.player.getWorld().getBlockEntity(sign_coord);
		
		if(test_block_entity == null) return;		
			
		if(!test_block_entity.getType().equals(BlockEntityType.SIGN)) return;
		
		Optional<SignBlockEntity> test_sign_opt = client.player.getWorld().getBlockEntity(sign_coord, BlockEntityType.SIGN);
		
		SignBlockEntity test_sign = test_sign_opt.orElse(null);
			
		if(test_sign == null) {
			return;
		}
		
		String line2 = test_sign.getFrontText().getMessage(1, false).getString().toLowerCase().trim();
		String line4 = test_sign.getFrontText().getMessage(3, false).getString().toLowerCase().replace(">=", "").trim();
		
		StonkCompanionClient.latest_mod_version = line2;
		StonkCompanionClient.mininum_mod_version = line4;	
		
		if(isCurrentVersionOlder(line2)) {
			StonkCompanionClient.is_latest_version = false;
		}
		
		if(isCurrentVersionOlder(line4)) {
			StonkCompanionClient.is_stopping_mistrade_dect = true;
		}
		
	}		
	
	private boolean isCurrentVersionOlder(String test_version) {
		
		if(!test_version.contains(".")) return false;
		
		String[] test_version_split = test_version.replace("v","").split("\\.");
		String[] current_version_split = StonkCompanionClient.current_mod_version.replace("v", "").split("\\.");
		
		if(Integer.parseInt(test_version_split[0]) > Integer.parseInt(current_version_split[0])) {
			return true;
		}
		
		if(Integer.parseInt(test_version_split[1]) > Integer.parseInt(current_version_split[1])) {
			return true;
		}
		
		if(Integer.parseInt(test_version_split[2]) > Integer.parseInt(current_version_split[2])) {
			return true;
		}
		
		return false;
	}
	
	private void splitLabel() {
		// Gotta split the label into category and barrel_number.
		// If barrel_number does not exist, then it is 0.
		// Assumed barrels are of the form (category)( )(#)
		// Category is assumed to either not end with a number or have a space between end of category and the number.
		// "-" is considered a number for the start (In case negative stonks exist?)
		
		boolean found_number = false;
		String gathering_number = "";

		for(int i = label.length()-1; i >= 0; i--) {
			
			char _char = label.charAt(i);
			
			if('0' <= _char && _char <= '9') {
				// It is number.
				if (found_number) found_number = true;
				gathering_number = _char + gathering_number;
			} else if (found_number && _char == '-') {
				gathering_number = "-" + gathering_number;
			}else {
				category = label.substring(0, i+1);
				break;
			}
		}
		
		if(gathering_number.isBlank()) {
			barrel_number = 0;
		}else {
			try {
				barrel_number = Integer.parseInt(gathering_number);
			} catch (Exception e){
				StonkCompanionClient.LOGGER.error("Failed to parse number: \"" + gathering_number + "\" for " + coords);
			}
		}
				
		
	}
	
	public void incrementTime() { time_since_last_movement++;}
	public void resetTime() {time_since_last_movement = 0;}
	public boolean isTimeOver() {return time_since_last_movement >= transaction_lifetime;}
	
	protected void calcuateGuiHeight() {
		if (gui_text == null) {
			gui_height = -1;
			return;
		}
		
		gui_height = 6 + client.textRenderer.fontHeight;
		gui_height += StonkCompanionClient.is_latest_version ? 0 : client.textRenderer.fontHeight + 1;
		
		for(Text[] given_text_segment : gui_text) {
			if(given_text_segment == null) continue;
			boolean printed_line = false;
			for(Text given_text : given_text_segment) {
				if(given_text == null) continue;
				if(!printed_line) {
					
					gui_height += 2 + 1;
					printed_line = true;
				}
				
				gui_height += client.textRenderer.fontHeight + 1;
			}
		}
		
		gui_height += 2 + 1;
		
	}
	
	// For subclass usages. Since subclasses should override these if they have them, and all of them should have these 4 functions just different implementations.
	public boolean validateTransaction() { return true;
		}
	public void generateGuiText() {}
	public void calulateFairPrice(List<Slot> items) {}
	public void onClickActionAdd(String taken_item_name, int item_qty_taken, String put_item_name, int item_qty_put) {}
	public void convertSolutionToCompressed() {}
	
	public void clearBarrelTransactions() {
		resetTime();
		barrel_actions = new double[] {0.0, 0.0};
		barrel_transactions = new HashMap<>();
		barrel_transaction_validity = true;
		barrel_transaction_solution = "";
		mistrade_text_message = "";
		fairprice_text_message = "";
		fairprice_gui_message = "";
		generateGuiText();
	}
	
	public void updateGuiTimestamp(){
		
		if(gui_text == null) return;
		if(barrel_transactions.isEmpty() || StonkCompanionClient.is_stopping_mistrade_dect) {
			if(gui_text[2] != null) gui_text[2] = null;
			return;
		}
		if(gui_text[2] == null) return;
		
		int time_left = -1;

		time_left = (int)((Barrel.transaction_lifetime - time_since_last_movement)/20);
			
		if (time_left != -1) {
			int seconds_since_last_interaction = (int)time_since_last_movement/20;
			ZonedDateTime current_time = ZonedDateTime.now().minusSeconds(seconds_since_last_interaction);	

			gui_text[2][0] = Text.literal("Last Interaction: %s".formatted(current_time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))));
			
			gui_text[2][1] = Text.literal("Refund period ends in: %d:%02d".formatted((int)time_left/60, time_left%60));
		}
	}
	
	public String toString() {
		return "Label: " + label + " Coords: " + coords + " time: " + time_since_last_movement;
	}
	
}
