package net.stonkcompanion.main;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.joml.Math;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class BuyBarrel extends Barrel {

	public String ask_price = "";
	public String ask_str = "";
	public double compressed_ask_price = 0.0;
	public int currency_type = -1;
	public boolean wrong_currency = false;
	public String barrel_transaction_solution_mats = "";
	
	public BuyBarrel(String label, String coords, String ask_price, double compressed_ask_price, int currency_type) {
		super(label, coords);
		this.ask_price = ask_price;
		this.ask_str = ask_price.replace("buy for", "").trim();
		this.compressed_ask_price = compressed_ask_price;
		this.currency_type = currency_type;
		this.barrel_type = BarrelTypes.BUY;
	}
	
	public void clearBarrelTransactions() {
		super.clearBarrelTransactions();
		wrong_currency = false;
		barrel_transaction_solution_mats = "";
	}
	
	public void onClickActionAdd(String taken_item_name, int item_qty_taken, String put_item_name, int item_qty_put) {
		
		if(barrel_actions == null) {
			barrel_actions = new double[]{0.0, 0.0};
		}
		
		if(item_qty_taken != 0) {
			String taken_item_name_lc = taken_item_name.toLowerCase();
			
			int item_currency_type = StonkCompanionClient.getItemCurrencyType(taken_item_name_lc);
			
			if(currency_type == item_currency_type) {
				barrel_actions[1] -= StonkCompanionClient.givenCurrReturnMult(taken_item_name_lc)*(double)(item_qty_taken);
			}else if(item_currency_type == 0) {
				if(label.toLowerCase().startsWith("64x") || label.toLowerCase().contains("stack")) {
					barrel_actions[0] -= (double)(item_qty_taken)/64.0;
				}else {
					barrel_actions[0] -= item_qty_taken;
				}
			}
		}
		
		if(item_qty_put != 0) {
			String put_item_name_lc = put_item_name.toLowerCase();
			
			int item_currency_type = StonkCompanionClient.getItemCurrencyType(put_item_name_lc);
			
			if(currency_type == item_currency_type) {
				barrel_actions[1] += StonkCompanionClient.givenCurrReturnMult(put_item_name_lc)*(double)(item_qty_put);
			}else if(item_currency_type == 0) {
				if(label.toLowerCase().startsWith("64x") || label.toLowerCase().contains("stack")) {
					barrel_actions[0] += (double)(item_qty_put)/64.0;
				}else {
					barrel_actions[0] += item_qty_put;
				}
			}
		}
	}
	
	public void generateGuiText() {
		
		this.gui_text = new Text[5][];
		
		// This is the label / buy
		this.gui_text[0] = new Text[2];
		
		// This is the recent interactions / (added/removed) X mats / (added / removed) Y currency.
		this.gui_text[1] = new Text[3];
		
		//This is laster interaction / refund period
		this.gui_text[2] = new Text[2];
		
		// This is mistrade portion.
		this.gui_text[3] = new Text[5];
		
		// This is just the tack on if error report do clear reports.
		this.gui_text[4] = new Text[2];
		
		int red_color = 0xffff0000;
		int green_color = 0xff00ff00;
		int light_blue_color = 0xff00ffff;
		int light_red_color = 0xffff5555;
		
		gui_text[0][0] = Text.literal(label).formatted(Formatting.UNDERLINE);
		
		MutableText buy_for = Text.literal("Buy For: ");
		buy_for = buy_for.withColor(green_color);
		buy_for.append(Text.literal("%s".formatted(ask_str)).withColor(light_blue_color));
		
		gui_text[0][1] = buy_for;;
		
		if(StonkCompanionClient.is_stopping_mistrade_dect) {
			this.gui_text[1] = new Text[4];	
			gui_text[1][0] = Text.literal("StonkCompanion is out of date.").withColor(light_red_color);
			gui_text[1][1] = Text.literal("Installed: {" + StonkCompanionClient.current_mod_version + "}").withColor(light_red_color);
			gui_text[1][2] = Text.literal("Update to {" + StonkCompanionClient.mininum_mod_version + "} or higher").withColor(light_red_color);
			gui_text[1][3] = Text.literal("to re-enable mistrade checking.").withColor(light_red_color);
			super.calcuateGuiHeight();
			return;
		}
		
		if (barrel_actions[0] != 0 || barrel_actions[1] != 0) {
			gui_text[1][0] = Text.literal("Recent Interactions:");
			if(barrel_actions[0] != 0) {
				gui_text[1][1] = Text.literal("%s %s Mat%s".formatted((barrel_actions[0] < 0) ? "Removed" : "Added", StonkCompanionClient.df1.format(Math.abs(barrel_actions[0])), Math.abs(barrel_actions[0]) == 1 ? "" : "s"));
			}else {
				gui_text[1][1] = null;
			}
			if(barrel_actions[1] != 0) {
				double barrel_actions_money = barrel_actions[1];
				if(StonkCompanionClient.is_compressed_only) {
					gui_text[1][2] = Text.literal("%s %s %s".formatted((barrel_actions_money < 0) ? "Removed" : "Added", StonkCompanionClient.df1.format(Math.abs(barrel_actions_money)), StonkCompanionClient.currency_type_to_compressed_text.get(currency_type)));
				}else {
					gui_text[1][2] = Text.literal("%s %d %s %s %s".formatted((barrel_actions_money < 0) ? "Removed" : "Added", (int)((Math.abs(barrel_actions_money)/64)), StonkCompanionClient.currency_type_to_hyper_text.get(currency_type), StonkCompanionClient.df1.format(Math.abs(barrel_actions_money%64)), StonkCompanionClient.currency_type_to_compressed_text.get(currency_type)));
				}
			}else {
				gui_text[1][2] = null;
			}
			// draw_context.drawTextWithShadow(client.textRenderer, "===========================", dimension.x+1, dimension.y+y_diff_text, light_blue_color);
		}
		
		if(!barrel_transactions.isEmpty()) {
			int time_left = -1;
	
			time_left = (int)((Barrel.transaction_lifetime - time_since_last_movement)/20);
					
			if (time_left != -1) {
				int seconds_since_last_interaction = (int)time_since_last_movement/20;
				ZonedDateTime current_time = ZonedDateTime.now().minusSeconds(seconds_since_last_interaction);	
	
				gui_text[2][0] = Text.literal("Last Interaction: %s".formatted(current_time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))));
					
				gui_text[2][1] = Text.literal("Refund period ends in: %d:%02d".formatted((int)time_left/60, time_left%60));
			}
				
			gui_text[3][0] = barrel_transaction_validity ? Text.literal("Valid Trade").formatted(Formatting.BOLD) : Text.literal("Invalid Trade").formatted(Formatting.BOLD).withColor(light_red_color);
		}else {
			gui_text[2][0] = null;
			gui_text[2][1] = null;
			gui_text[3][0] = null;
		}
		//draw_context.drawTextWithShadow(client.textRenderer, StonkCompanionClient.barrel_transaction_validity.get(given_barrel.coords) ? "Valid" : "Mistrade Detected", dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
		if(!barrel_transaction_validity) {
			
			if(!barrel_transaction_solution.isBlank()) {
				gui_text[3][1] = Text.literal("Suggested Fix:");
				gui_text[3][2] = Text.literal(barrel_transaction_solution);
			}
			if(!barrel_transaction_solution_mats.isBlank()) {
				gui_text[3][3] = Text.literal(barrel_transaction_solution_mats);
			}
			if(wrong_currency) {
				gui_text[3][4] = Text.literal("Wrong Currency").formatted(Formatting.BOLD).withColor(red_color);
			}
				
		}else {
			gui_text[3][1] = null;
			gui_text[3][2] = null;
			gui_text[3][3] = null;
			gui_text[3][4] = null;
		}
			
		if(!barrel_transaction_validity) {
			gui_text[4][0] = Text.literal("If this report is in error, type:");
			gui_text[4][1] = Text.literal("/sc mistrades clear");
		}else {
			gui_text[4][0] = null;
			gui_text[4][1] = null;
		}
		
		super.calcuateGuiHeight();
	}
	
	public boolean validateTransaction() {
		
		if (barrel_transactions.isEmpty()) {
			barrel_transaction_validity = true;
			barrel_transaction_solution = "";
			mistrade_text_message = "";
			barrel_transaction_solution_mats = "";
			generateGuiText();
			return true;
		}
		
		if(StonkCompanionClient.is_stopping_mistrade_dect) {
			barrel_transaction_validity = true;
			barrel_transaction_solution = "";
			barrel_transaction_solution_mats = "";
			
			StringBuilder build_mistrade_text = new StringBuilder();
			
			build_mistrade_text.append("§7---[§eStonk§aCo§bmpanion §a" + StonkCompanionClient.current_mod_version + "§7]---");
			build_mistrade_text.append("\n§cStonkCompanion is out of date.");
			build_mistrade_text.append("\nInstalled: {" + StonkCompanionClient.current_mod_version + "}");
			build_mistrade_text.append("\nUpdate to {" + StonkCompanionClient.mininum_mod_version +"} or higher to re-enable mistrade checking.");

			mistrade_text_message = build_mistrade_text.toString();
			
			generateGuiText();
			return true;
		}
			
		// So we should have a map of items to their total qtyies traded in this barrel
		// and a map of barrel position to all the needed price info.
		// Time to check if it is a mistrade or not.

		// TODO: Clean up currency conversions.
		// TODO: Look into maybe checking what is being traded and not just assuming only the correct item is being traded.
		double r1_compressed = 0.0;
		double r2_compressed = 0.0;
		double r3_compressed = 0.0;
		double other_items = 0;		
			
		for (String traded_item : barrel_transactions.keySet()) {
			
											
			int item_qty = barrel_transactions.get(traded_item);
				
			String traded_item_lc = traded_item.toLowerCase();
			
			double mult = StonkCompanionClient.givenCurrReturnMult(traded_item_lc);
			int item_curr = StonkCompanionClient.getItemCurrencyType(traded_item_lc);
			
			if(item_curr == 1) {
				r1_compressed += mult*(double)(item_qty);
			}else if(item_curr == 2) {
				r2_compressed += mult*(double)(item_qty);
			}else if(item_curr == 3) {
				r3_compressed += mult*(double)(item_qty);
			}else {
				other_items += item_qty;
			}
		}
		
		// Checking for stacks barrels.
		if(label.toLowerCase().startsWith("64x") || label.toLowerCase().contains("stack")) {
			other_items = other_items/64.0;
		}
		
		// Okay we have all our ducks in a row. Now to verify if this trade was correct.
		// We are expecting items to be taken, otherwise it is not a valid trade. So expecting 0 compressed?
		double actual_compressed = 0.0;
		boolean valid_transaction = false;
		wrong_currency = false;
			
		if(currency_type == 1) {
			actual_compressed = r1_compressed;
				
			if(r2_compressed != 0.0 || r3_compressed != 0.0) {
				wrong_currency = true;
			}
		}
			
		if(currency_type == 2) {
			actual_compressed = r2_compressed;
			
			if(r1_compressed != 0.0 || r3_compressed != 0.0) {
				wrong_currency = true;
			}
		}
			
		if(currency_type == 3) {
			actual_compressed = r3_compressed;
				
			if(r2_compressed != 0.0 || r1_compressed != 0.0) {
				wrong_currency = true;
			}
		}
		
		double expected_compressed = (other_items < 0 && actual_compressed >= 0) ? Math.abs(other_items)*compressed_ask_price : 0;
		
	    String currency_str = StonkCompanionClient.currency_type_to_compressed_text.get(currency_type);
	    String hyper_str = StonkCompanionClient.currency_type_to_hyper_text.get(currency_type);
	        
	    double currency_delta = expected_compressed - actual_compressed;
	    
	    // Bounds check.
	    if(currency_delta < 0.0005 && currency_delta > -0.0005) currency_delta = 0;
	    
		if (currency_delta == 0) valid_transaction = true;
		if (wrong_currency) valid_transaction = false;
		// Sold items therefore invalid trade.
		if (other_items > 0) valid_transaction = false;
	    
	    // Funny check if can be fixed by adding / taking mats.
	    double mats_delta = 0.0;
	    
	    /*
	     * Buy cases:
	     * 
	     * 0 mats, 0 currency
	     * 	Should not exist at this point.
	     * 
	     * 0 mats, + currency
	     * 	Possibly a valid trade if the currency mod ask price is 0.
	     * 	Take money back
	     * 	If currency % price == 0: (OR) take X mats.
	     * 
	     * 0 mats, - currency
	     *  Invalid trade / no way to know what the valid trade is so
	     *  Put money back
	     *  
	     * + mats, 0 currency
	     * 	Invalid trade / no way to know what the valid trade is so
	     *  take mats back
	     *  
	     * - mats, 0 currency
	     *  Possible valid trade.
	     *  Put corresponding money in
	     *  (OR) put back mats.
	     *  
	     * + mats, - currency
	     *  Invalid trade 
	     *  Put money in
	     *  Take mats back
	     *  
	     * + mats, + currency
	     *  Maybe invalid trade?
	     *  Complicated 3 options here.
	     *  I will go with the take both money back and mats back option.
	     *  
	     * - mats, + currency
	     *  Possibly valid trade.
	     *  if invalid:
	     *  Put/take currency
	     *  if currency_delta%price == 0: (OR) take/put mats.
	     *  
	     * - mats, - currency
	     *  Invalid trade.
	     *  I will go with the put correct money back and mats.
	     */
	    
	    String correction_dir_mats = "";
	    
	    if(other_items == 0 && actual_compressed > 0) {
	    	mats_delta = (currency_delta % compressed_ask_price == 0) ? (int)(currency_delta / compressed_ask_price) : 0;
	    	correction_dir_mats = "Take out";
	    }else if(other_items > 0 && actual_compressed == 0) {
	    	mats_delta = -1.0*other_items;
	    	correction_dir_mats = "Take out";
	    }else if(other_items < 0 && actual_compressed == 0) {
	    	mats_delta = -1.0*other_items;
	    	correction_dir_mats = "Put in";
	    }else if(other_items > 0 && actual_compressed < 0) {
	    	mats_delta = -1.0*other_items;
	    	correction_dir_mats = "Take out";
	    }else if(other_items > 0 && actual_compressed > 0) {
	    	mats_delta = -1.0*other_items;
	    	correction_dir_mats = "Take out";
	    }else if(other_items < 0 && actual_compressed > 0) {
	    	mats_delta = (currency_delta % compressed_ask_price == 0) ? (int)(currency_delta / compressed_ask_price) : 0;
	    	if (mats_delta != 0)
	    		correction_dir_mats = mats_delta < 0 ? "Take out" : "Put in";
	    }else if(other_items < 0 && actual_compressed < 0) {
	    	mats_delta = -1.0*other_items;
	    	currency_delta = -1.0*actual_compressed;
	    	correction_dir_mats = "Put in";
	    }
		
		int actual_hyper_amount = (int)(Math.floor(Math.abs(actual_compressed)/64));
		double actual_compressed_amount = (Math.abs(actual_compressed)%64);
	    
		int corrective_hyper_amount = (int)(Math.floor(Math.abs(currency_delta)/64));
		double corrective_compressed_amount = (Math.abs(currency_delta)%64);
		
	    String correction_dir = (currency_delta < 0) ? "Take out" : "Put in";
	    
	    // if(currency_delta != 0) mc.player.sendMessage(Text.literal("[StonkCompanion] Mistrade detected in " + label));
	    
		barrel_transaction_validity = valid_transaction;
		if (!valid_transaction) {
			// TODO: Check for if compressed or not toggle.
			convertSolutionToCompressed();
			// barrel_transaction_solution = "Correction amount: %s %s %s (%d %s %s %s)".formatted(correction_dir, StonkCompanionClient.df1.format(Math.abs(currency_delta)), currency_str, corrective_hyper_amount, hyper_str, StonkCompanionClient.df1.format(corrective_compressed_amount), currency_str);
		
			// Check if it can be resolved via mats.
			if (mats_delta != 0) {
				
				String mats_delta_str = StonkCompanionClient.df1.format(Math.abs(mats_delta));
				
				if(other_items <= 0 && actual_compressed >= 0) {
					barrel_transaction_solution_mats = "(OR) %s %s mat%s".formatted(correction_dir_mats, mats_delta_str, Math.abs(mats_delta) == 1 ? "" : "s");
				}else {
					barrel_transaction_solution_mats = "%s %s mat%s".formatted(correction_dir_mats, mats_delta_str, Math.abs(mats_delta) == 1 ? "" : "s");
				}
			}else {
				barrel_transaction_solution_mats = "";
			}
		}else {
			barrel_transaction_solution = "";
			barrel_transaction_solution_mats = "";
		}
	    	
		StringBuilder build_mistrade_text = new StringBuilder();
		
		if(!StonkCompanionClient.is_latest_version) {
			build_mistrade_text.append("§7[§eStonk§aCo§bmpanion§7] There is a newer version! §e" + StonkCompanionClient.latest_mod_version +"\n");
		}
		
		build_mistrade_text.append("§7---[§eStonk§aCo§bmpanion §e" + StonkCompanionClient.current_mod_version + "§7]---");
		build_mistrade_text.append("\n%s (%s)".formatted(label, coords));
		build_mistrade_text.append("\nBuy: %s %s (%s)".formatted(StonkCompanionClient.df1.format(compressed_ask_price), currency_str, ask_price));
		if(other_items < 0) build_mistrade_text.append("\n%s: %s".formatted("Bought", StonkCompanionClient.df1.format(Math.abs(other_items))));
		if(actual_compressed != 0) build_mistrade_text.append("\n%s: %s %s (%d %s %s %s)".formatted((actual_compressed < 0) ? "Took" : "Paid", StonkCompanionClient.df1.format(Math.abs(actual_compressed)), currency_str, actual_hyper_amount, hyper_str, StonkCompanionClient.df1.format(actual_compressed_amount), currency_str));
		if(other_items!=0) build_mistrade_text.append("\nUnit Price: %s".formatted(StonkCompanionClient.df1.format(Math.abs(actual_compressed / (other_items)))));
		if(barrel_transaction_validity) build_mistrade_text.append("\nValid Transaction");
		if(!barrel_transaction_validity) build_mistrade_text.append("\n§cMistrade Detected");
		if(currency_delta != 0) build_mistrade_text.append("\nCorrection amount: %s %s %s (%d %s %s %s)".formatted(correction_dir, StonkCompanionClient.df1.format(Math.abs(currency_delta)), currency_str, corrective_hyper_amount, hyper_str, StonkCompanionClient.df1.format(corrective_compressed_amount), currency_str));
		if(mats_delta != 0) {			
			if(other_items <= 0 && actual_compressed >= 0) {
				build_mistrade_text.append("\n(OR) Correction amount: %s %s mat%s".formatted(correction_dir_mats, StonkCompanionClient.df1.format(Math.abs(mats_delta)), Math.abs(mats_delta) == 1 ? "" : "s"));
			}else {
				build_mistrade_text.append("\nCorrection amount: %s %s mat%s".formatted(correction_dir_mats, StonkCompanionClient.df1.format(Math.abs(mats_delta)), Math.abs(mats_delta) == 1 ? "" : "s"));
			}			
		}
		build_mistrade_text.append("\n§7Time since last log: %ds/%ds".formatted(time_since_last_movement/20, transaction_lifetime/20));
		if(wrong_currency) build_mistrade_text.append("\n§cWrong currency was used!");
		if(other_items > 0) build_mistrade_text.append("\nSold mats instead of bought mats!");
		build_mistrade_text.append("\n§7--------------------");
		
		mistrade_text_message = build_mistrade_text.toString();
	    
		generateGuiText();
		
	    if(other_items == 0 && currency_delta == 0 && !wrong_currency && barrel_transactions.isEmpty()) {
			return true;
		}
		
		return false;
	}
	
	public void convertSolutionToCompressed() {
		
		double other_items = barrel_actions[0];
		double actual_compressed = barrel_actions[1];
		
		double expected_compressed = (other_items < 0) ? Math.abs(other_items)*compressed_ask_price : 0;
		
	    double currency_delta = expected_compressed - actual_compressed;
	    
	    if(other_items < 0 && actual_compressed < 0) currency_delta = -1.0*actual_compressed;
	    
	    String currency_str = StonkCompanionClient.currency_type_to_compressed_text.get(currency_type);
	    String hyper_str = StonkCompanionClient.currency_type_to_hyper_text.get(currency_type);
	    
	    // Bounds check.
	    if(currency_delta < 0.0005 && currency_delta > -0.0005) currency_delta = 0;

	    if(currency_delta != 0) {
	    	double abs_currency_delta = Math.abs(currency_delta);
	    	String correction_dir = currency_delta<0 ? "Take out" : "Put in";
	    	// TODO: Turn this into an array of two strings.
	    	if(StonkCompanionClient.is_compressed_only) {
		    	barrel_transaction_solution = "%s %s %s".formatted(correction_dir, StonkCompanionClient.df1.format(Math.abs(currency_delta)), currency_str);	
	    	}else {
		    	barrel_transaction_solution = "%s %d %s %s %s".formatted(correction_dir, (int)(abs_currency_delta/64), hyper_str, StonkCompanionClient.df1.format(abs_currency_delta%64), currency_str);
	    	}
	    }else {
	    	barrel_transaction_solution = "";
	    }
	}
	
	public String toString() {
		String start = super.toString();
		
		return start + " ask_str: " + ask_str;
	}

}
