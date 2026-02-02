package net.stonkcompanion.main;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import org.joml.Math;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class StonkBarrel extends Barrel {
	
	public String ask_price = "";
	public String bid_price = "";
	public String ask_str = "";
	public String bid_str = "";
	public double compressed_ask_price = 0.0;
	public double compressed_bid_price = 0.0;
	public int currency_type = -1;
	public String barrel_transaction_solution_mats = "";
	public boolean wrong_currency = false;

	public StonkBarrel(String label, String coords, String ask_price, String bid_price, double compressed_ask_price,
			double compressed_bid_price, int currency_type) {
		super(label, coords);
		this.ask_price = ask_price;
		this.bid_price = bid_price;
		this.ask_str = ask_price.replace("buy for", "").trim();
		this.bid_str = bid_price.replace("sell for", "").trim();
		this.compressed_ask_price = compressed_ask_price;
		this.compressed_bid_price = compressed_bid_price;
		this.currency_type = currency_type;
		this.barrel_type = BarrelTypes.STONK;
	}
	
	public void convertSolutionToCompressed() {
		
		double other_items = barrel_actions[0];
		double actual_compressed = barrel_actions[1];
		
		double expected_compressed = (other_items < 0) ? Math.abs(other_items)*compressed_ask_price : -1*other_items*compressed_bid_price;
		
	    double currency_delta = expected_compressed - actual_compressed;
	    
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
	
	public void onClickActionAdd(String taken_item_name, int item_qty_taken, String put_item_name, int item_qty_put) {
		
		if(barrel_actions == null) {
			barrel_actions = new double[]{0.0, 0.0};
		}
		
		if(item_qty_taken != 0) {
			String taken_item_name_lc = taken_item_name.toLowerCase();
			
			int item_currency_type = StonkCompanionClient.getCurrencyType(taken_item_name_lc);
			
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
			
			int item_currency_type = StonkCompanionClient.getCurrencyType(put_item_name_lc);
			
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
		
		// This is the label / buy / sell / fair stonk
		this.gui_text[0] = new Text[4];
		
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
		
		gui_text[0][0] = Text.literal(label).formatted(Formatting.UNDERLINE);
		
		MutableText buy_for = Text.literal("Buy For: ");
		buy_for = buy_for.withColor(green_color);
		buy_for.append(Text.literal("%s".formatted(ask_str)).withColor(light_blue_color));
		
		gui_text[0][1] = buy_for;
		
		MutableText sell_for = Text.literal("Sell For: ");
		sell_for = sell_for.withColor(red_color);
		sell_for.append(Text.literal("%s".formatted(bid_str)).withColor(light_blue_color));
		
		gui_text[0][2] = sell_for;
		if(StonkCompanionClient.fairprice_detection && !fairprice_gui_message.isBlank()) gui_text[0][3] = Text.literal(fairprice_gui_message);
		
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
				
			gui_text[3][0] = barrel_transaction_validity ? Text.literal("Valid Trade").formatted(Formatting.BOLD) : Text.literal("Invalid Trade").formatted(Formatting.BOLD);
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
			gui_text[4][1] = Text.literal("/StonkCompanion ClearReports");
		}else {
			gui_text[4][0] = null;
			gui_text[4][1] = null;
		}
		
		super.calcuateGuiHeight();
	}
	
	public void calulateFairPrice(List<Slot> items) {
		
		double barrel_compressed_currency = 0;
		double barrel_mats = 0;
		
		for(int i = 0; i < 27; i++) {
			
			if(!items.get(i).hasStack()) continue;
			
			ItemStack item = items.get(i).getStack();
			
			String item_name = "";
			int item_qty = item.getCount();	
			
			if(item.getNbt() == null || !item.getNbt().contains("Monumenta")) {
				item_name = item.getItem().getTranslationKey().substring(item.getItem().getTranslationKey().lastIndexOf('.')+1);
				// StonkCompanionClient.LOGGER.info(item.getItem().getTranslationKey());
				
				if(item.getItem().getTranslationKey().endsWith("sign") || item.getItem().getTranslationKey().endsWith("written_book")) {
					continue;
				}
				
			}else {
				item_name = item.getNbt().getCompound("plain").getCompound("display").getString("Name");				
			}
			
			double mult = StonkCompanionClient.givenCurrReturnMult(item_name);
			
			if (currency_type < 0) {
				// Forex barrel

				int item_currency = StonkCompanionClient.getCurrencyType(item_name);

				// If this is a forex barrel, we will assume that one_to_two is the ask and two_to_one is the bid.		
				if (item_currency == 1) {
					if (currency_type == -1) barrel_compressed_currency += item_qty*mult;
					if (currency_type == -2) barrel_mats += item_qty*mult;
					if (currency_type == -3) ; // Could fire an error here as an unexpected currency was detected
				} else if (item_currency == 2) {
					if (currency_type == -1) barrel_mats += item_qty*mult;
					if (currency_type == -2) ; // Could fire an error here as an unexpected currency was detected
					if (currency_type == -3) barrel_compressed_currency += item_qty*mult;
				} else if (item_currency == 3) {
					if (currency_type == -1) ; // Could fire an error here as an unexpected currency was detected
					if (currency_type == -2) barrel_compressed_currency += item_qty*mult;
					if (currency_type == -3) barrel_mats += item_qty*mult;
				}

			} else {
				// Stonk barrel
				if(mult == -1) {
					barrel_mats += item_qty;
				}else {
					barrel_compressed_currency += item_qty * mult;
				}
			}
		}
		
		// Checking for stacks barrels.
		if(label.toLowerCase().startsWith("64x") || label.toLowerCase().contains("stack")) {
			barrel_mats = barrel_mats/64.0;
		}
		
		//LOGGER.info("Mats: %.3f".formatted(barrel_mats));
		//LOGGER.info("Comp: %.3f".formatted(barrel_compressed_currency));
		double spread = compressed_ask_price - compressed_bid_price;
		//LOGGER.info("Spread: %.3f".formatted(spread));
		double mid = compressed_bid_price + spread / 2.0;
		//LOGGER.info("Mid: %.3f".formatted(mid));
		    
		double mats_in_currency = barrel_compressed_currency / mid;
		//LOGGER.info("Mats in currency: %.3f".formatted(mats_in_currency));
		double effective_mats = barrel_mats + mats_in_currency;
		//LOGGER.info("Effective mats: %.3f".formatted(effective_mats));
		    
		if(effective_mats == 0) {
			this.fairprice_text_message = "";
			this.fairprice_gui_message = "";
			generateGuiText();
		  	return;
		}
		    
		double demand_modifier = mats_in_currency / effective_mats;
		//LOGGER.info("Demand modifier: %.3f".formatted(demand_modifier));
		double intraspread_factor = demand_modifier * spread;
		//LOGGER.info("intraspread_factor: %.3f".formatted(intraspread_factor));
		double interpolated_price = compressed_bid_price + intraspread_factor;	
	
		String currency_str = StonkCompanionClient.currency_type_to_compressed_text.get(currency_type);
		String hyper_str = StonkCompanionClient.currency_type_to_hyper_text.get(currency_type);

		int interpolated_hyper_amount = (int)(Math.floor(Math.abs(interpolated_price)/64));
		double interpolated_compressed_amount = (Math.abs(interpolated_price)%64);	    
		    
		// TODO: Add label
		String fairprice_msg = String.format("[StonkCompanion] %s's FairStonk is %s %s (%d %s %s %s).", StonkCompanionClient.categoreyMaker(label), StonkCompanionClient.df1.format(interpolated_price), currency_str, interpolated_hyper_amount, hyper_str, StonkCompanionClient.df1.format(interpolated_compressed_amount), currency_str);
		String fairprice_gui_msg = String.format("Fair Stonk Price: %s %s", StonkCompanionClient.df1.format(interpolated_price), currency_str);
		
		if(demand_modifier <= 0.005) {
			fairprice_msg = "[StonkCompanion] Look in lower barrel.";
			fairprice_gui_msg = "Look in lower barrel.";
		}else if(demand_modifier >= 0.995) {
		   	fairprice_msg = "[StonkCompanion] Look in higher barrel.";
		   	fairprice_gui_msg = "Look in higher barrel.";
		}		
		
		this.fairprice_text_message = fairprice_msg;
		this.fairprice_gui_message = fairprice_gui_msg;
		generateGuiText();
	}
	
	public boolean validateTransaction() {
		
		if (barrel_transactions.isEmpty()) {
			barrel_transaction_validity = true;
			barrel_transaction_solution = "";
			barrel_transaction_solution_mats = "";
			mistrade_text_message = "";
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
			int item_curr = StonkCompanionClient.getCurrencyType(traded_item_lc);
			
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
		double expected_compressed = (other_items < 0) ? Math.abs(other_items)*compressed_ask_price : -1*other_items*compressed_bid_price;
		double actual_compressed = 0.0;
		boolean valid_transaction = false;
		wrong_currency = false;
			
		if(currency_type == 1) {
			valid_transaction = r1_compressed == expected_compressed;
			actual_compressed = r1_compressed;
				
			if(r2_compressed != 0.0 || r3_compressed != 0.0) {
				wrong_currency = true;
			}
		}
			
		if(currency_type == 2) {
			valid_transaction = r2_compressed == expected_compressed;
			actual_compressed = r2_compressed;
			
			if(r1_compressed != 0.0 || r3_compressed != 0.0) {
				wrong_currency = true;
			}
		}
			
		if(currency_type == 3) {
			valid_transaction = r3_compressed == expected_compressed;
			actual_compressed = r3_compressed;
				
			if(r2_compressed != 0.0 || r1_compressed != 0.0) {
				wrong_currency = true;
			}
		}
		
		if (wrong_currency) valid_transaction = false;
			
		/*
		 * Here is where hell resides. Pretty printing the mistrades.
		 * Barrel Name (Barrel Coordinates)
		 * Buy: price in compressed (price as written on barrel)
		 * Sell: price in compressed (price as written on barrel)
		 * Sold/Bought: number of mats
		 * Paid/Took: Net Currency
		 * Unit Price: (net currency / number of mats)
		 * Correction amount: (+/- currency to resolve mistrade.)
		 * (If wrong_currency) Wrong Currency was used!
		 */
		
	    String currency_str = StonkCompanionClient.currency_type_to_compressed_text.get(currency_type);
	    String hyper_str = StonkCompanionClient.currency_type_to_hyper_text.get(currency_type);
	        
	    double currency_delta = expected_compressed - actual_compressed;
	    
	    // Bounds check.
	    if(currency_delta < 0.0005 && currency_delta > -0.0005) currency_delta = 0;
	    
	    
	    // Funny check if can be fixed by adding / taking mats.
	    int mats_delta = 0;
	    
	    // Either player bought mats or added currency and took no mats.
	    // StonkCompanionClient.LOGGER.info(other_items + " " + currency_delta);
	    if(other_items < 0 || (currency_delta < 0 && other_items == 0)) {
	    	
	    	// Player bought mats
	    	mats_delta = (currency_delta % compressed_ask_price == 0) ? (int)(currency_delta / compressed_ask_price) : 0;		    	
	    
	    // Either player sold mats or took currency and put in no mats.
	    }else if (other_items > 0 || (currency_delta > 0 && other_items == 0)) {
	    	
	    	// Player sold mats
	    	mats_delta = (currency_delta % compressed_bid_price == 0) ? (int)(currency_delta / compressed_bid_price) : 0;
	    	
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
				barrel_transaction_solution_mats = "(OR) %s %d mat%s".formatted(correction_dir, Math.abs(mats_delta), Math.abs(mats_delta) == 1 ? "" : "s");
			}else {
				barrel_transaction_solution_mats = "";
			}
		}else {
			barrel_transaction_solution = "";
			barrel_transaction_solution_mats = "";
		}
	    	
		StringBuilder build_mistrade_text = new StringBuilder();
		
		build_mistrade_text.append("---[StonkCompanion]---");
		build_mistrade_text.append("\n%s (%s)".formatted(label, coords));
		build_mistrade_text.append("\nBuy: %s %s (%s)".formatted(StonkCompanionClient.df1.format(compressed_ask_price), currency_str, ask_price));
		build_mistrade_text.append("\nSell: %s %s (%s)".formatted(StonkCompanionClient.df1.format(compressed_bid_price), currency_str, bid_price));
		build_mistrade_text.append("\n%s: %s".formatted((other_items < 0) ? "Bought" : "Sold", StonkCompanionClient.df1.format(Math.abs(other_items))));
		build_mistrade_text.append("\n%s: %s %s (%d %s %s %s)".formatted((actual_compressed < 0) ? "Took" : "Paid", StonkCompanionClient.df1.format(Math.abs(actual_compressed)), currency_str, actual_hyper_amount, hyper_str, StonkCompanionClient.df1.format(actual_compressed_amount), currency_str));
		if(other_items!=0) build_mistrade_text.append("\nUnit Price: %s".formatted(StonkCompanionClient.df1.format(Math.abs(actual_compressed / (other_items)))));
		if(barrel_transaction_validity) build_mistrade_text.append("\nValid Transaction");
		if(currency_delta != 0) build_mistrade_text.append("\nCorrection amount: %s %s %s (%d %s %s %s)".formatted(correction_dir, StonkCompanionClient.df1.format(Math.abs(currency_delta)), currency_str, corrective_hyper_amount, hyper_str, StonkCompanionClient.df1.format(corrective_compressed_amount), currency_str));
		if(mats_delta != 0) build_mistrade_text.append("\n(OR) Correction amount: %s %d mats".formatted(correction_dir, Math.abs(mats_delta)));
		build_mistrade_text.append("\nTime since last log: %ds/%ds".formatted(time_since_last_movement/20, transaction_lifetime/20));
		if(wrong_currency) build_mistrade_text.append("\nWrong currency was used!");
		build_mistrade_text.append("\n--------------------");
		
		mistrade_text_message = build_mistrade_text.toString();
	    
		generateGuiText();
		
	    if(other_items == 0 && currency_delta == 0 && !wrong_currency) {
			return true;
		}
	    return false;
	}
	
	public String toString() {
		String start = super.toString();
		return start + " ask_str: " + ask_str + " bid_str: " + bid_str;
	}

}
