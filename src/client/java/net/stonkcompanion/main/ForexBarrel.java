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

public class ForexBarrel extends Barrel {

	public String curr_one_str = "";
	public String curr_two_str = "";
	public double one_to_two = 0;
	public double two_to_one = 0;
	public int curr_one_type = -1;
	public int curr_two_type = -1;
	public boolean wrong_currency = false;
	
	private String barrel_transaction_full_hyper_solution = "";
	
	public ForexBarrel(String label, String coords, String curr_one_str, String curr_two_str, double one_to_two, double two_to_one, int curr_type) {
		super(label, coords);
		this.barrel_type = BarrelTypes.FOREX;
		this.curr_one_str = curr_one_str;
		this.curr_two_str = curr_two_str;
		this.one_to_two = one_to_two;
		this.two_to_one = two_to_one;
		
		switch(curr_type) {
		case(1):
			this.curr_one_type = 1;
			this.curr_two_type = 2;
			break;
		case(2):
			this.curr_one_type = 1;
			this.curr_two_type = 3;
			break;
		case(3):
			this.curr_one_type = 2;
			this.curr_two_type = 3;
			break;
		default:
			break;
		}
		
	}
	
	public void clearBarrelTransactions() {
		super.clearBarrelTransactions();
		wrong_currency = false;
		barrel_transaction_full_hyper_solution = "";
	}
	
	public void onClickActionAdd(String taken_item_name, int item_qty_taken, String put_item_name, int item_qty_put) {
		
		if(barrel_actions == null) {
			barrel_actions = new double[]{0.0, 0.0};
		}
		
		if(item_qty_taken != 0) {
			String taken_item_name_lc = taken_item_name.toLowerCase();
			
			int item_currency_type = StonkCompanionClient.getCurrencyType(taken_item_name_lc);
			
			if(curr_one_type == item_currency_type) {
				barrel_actions[0] -= StonkCompanionClient.givenCurrReturnMult(taken_item_name_lc)*(double)(item_qty_taken);
			}else if(curr_two_type == item_currency_type) {
				barrel_actions[1] -= StonkCompanionClient.givenCurrReturnMult(taken_item_name_lc)*(double)(item_qty_taken);
			}
		}
		
		if(item_qty_put != 0) {
			String put_item_name_lc = put_item_name.toLowerCase();
			
			int item_currency_type = StonkCompanionClient.getCurrencyType(put_item_name_lc);
			
			if(curr_one_type == item_currency_type) {
				barrel_actions[0] += StonkCompanionClient.givenCurrReturnMult(put_item_name_lc)*(double)(item_qty_put);
			}else if(curr_two_type == item_currency_type) {
				barrel_actions[1] += StonkCompanionClient.givenCurrReturnMult(put_item_name_lc)*(double)(item_qty_put);
			}
		}
	}
		
	public void generateGuiText() {
		
		this.gui_text = new Text[5][];
		
		// This is the label / buy / sell / fair stonk
		this.gui_text[0] = new Text[6];
		
		// This is the recent interactions / (added/removed) X mats / (added / removed) Y currency.
		this.gui_text[1] = new Text[3];
		
		//This is last interaction / refund period
		this.gui_text[2] = new Text[2];
		
		// This is mistrade portion.
		this.gui_text[3] = new Text[5];
		
		// This is just the tack on if error report do clear reports.
		this.gui_text[4] = new Text[2];
		
		int red_color = 0xffff0000;
		int light_blue_color = 0xff00ffff;
		
		gui_text[0][0] = Text.literal(label).formatted(Formatting.UNDERLINE);
		
		MutableText curr_one_str_text = Text.literal("%s".formatted(curr_one_str)).withColor(light_blue_color);
		
		gui_text[0][1] = curr_one_str_text;
		
		MutableText curr_two_str_text = Text.literal("%s".formatted(curr_two_str)).withColor(light_blue_color);
		
		gui_text[0][2] = curr_two_str_text;
		
		if(StonkCompanionClient.fairprice_detection && !fairprice_gui_message.isBlank()) {
			
			if(fairprice_gui_message.contains("|")) {
				String[] fairprice_gui_msg_split = fairprice_gui_message.split("\\|");
				gui_text[0][3] = Text.literal("Fairstonk Price:");
				gui_text[0][4] = Text.literal(fairprice_gui_msg_split[0]);
				gui_text[0][5] = Text.literal(fairprice_gui_msg_split[1]);
			}else {
				gui_text[0][3] = Text.literal(fairprice_gui_message);
			}
		}
		
		if (barrel_actions[0] != 0 || barrel_actions[1] != 0) {
			gui_text[1][0] = Text.literal("Recent Interactions:");
			if(barrel_actions[0] != 0) {
				double barrel_actions_money = barrel_actions[0];
				if(StonkCompanionClient.is_compressed_only) {
					gui_text[1][1] = Text.literal("%s %s %s".formatted((barrel_actions_money < 0) ? "Removed" : "Added", StonkCompanionClient.df1.format(Math.abs(barrel_actions_money)), StonkCompanionClient.currency_type_to_compressed_text.get(curr_one_type)));
				}else {
					gui_text[1][1] = Text.literal("%s %d %s %s %s".formatted((barrel_actions_money < 0) ? "Removed" : "Added", (int)((Math.abs(barrel_actions_money)/64)), StonkCompanionClient.currency_type_to_hyper_text.get(curr_one_type), StonkCompanionClient.df1.format(Math.abs(barrel_actions_money%64)), StonkCompanionClient.currency_type_to_compressed_text.get(curr_one_type)));
				}
			}else {
				gui_text[1][1] = null;
			}
			if(barrel_actions[1] != 0) {
				double barrel_actions_money = barrel_actions[1];
				if(StonkCompanionClient.is_compressed_only) {
					gui_text[1][2] = Text.literal("%s %s %s".formatted((barrel_actions_money < 0) ? "Removed" : "Added", StonkCompanionClient.df1.format(Math.abs(barrel_actions_money)), StonkCompanionClient.currency_type_to_compressed_text.get(curr_two_type)));
				}else {
					gui_text[1][2] = Text.literal("%s %d %s %s %s".formatted((barrel_actions_money < 0) ? "Removed" : "Added", (int)((Math.abs(barrel_actions_money)/64)), StonkCompanionClient.currency_type_to_hyper_text.get(curr_two_type), StonkCompanionClient.df1.format(Math.abs(barrel_actions_money%64)), StonkCompanionClient.currency_type_to_compressed_text.get(curr_two_type)));
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
			
			if (!barrel_transaction_solution.isBlank()) {
				gui_text[3][1] = Text.literal("Suggested Fix:");
				if (!barrel_transaction_solution.isBlank()) gui_text[3][2] = Text.literal(barrel_transaction_solution);
				if (!barrel_transaction_full_hyper_solution.isBlank()) gui_text[3][3] = Text.literal(barrel_transaction_full_hyper_solution);
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
		  
		  double comp_curr_one = 0;
		  double comp_curr_two = 0;
		    
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

		    int item_currency = StonkCompanionClient.getCurrencyType(item_name);

		    // If this is a forex barrel, we will assume that one_to_two is the ask and two_to_one is the bid.    
		    if (item_currency == curr_one_type) {
		      comp_curr_one += item_qty*mult;
		    } else if (item_currency == curr_two_type) {
		      comp_curr_two += item_qty*mult;
		    }
		  }

		  // Forex barrel fair price handling goes here. 
		  // The actual pair type doesnt matter, only that it is forex, since we have the rates and nets.    
		  // LOGGER.info(one_to_two+" "+two_to_one);

		  double spread = (1/(two_to_one/64)) - (1/(64/one_to_two));
		  double mid = (1/(64/one_to_two)) + spread/2;
		      
		  // LOGGER.info(mid+" "+spread);

		  double net_currency_in_barrel = comp_curr_one + comp_curr_two/mid;
		      
		  // LOGGER.info(barrel_compressed_currency+" "+comp_curr_two);

		  if (net_currency_in_barrel == 0){
		    fairprice_gui_message = "";
		    fairprice_text_message = "";
		    generateGuiText();
		    return;
		  }

		  double demand_modifier = comp_curr_one / net_currency_in_barrel;
		  double intraspread_factor = demand_modifier * spread;
		  double interpolated_price = (1/(two_to_one/64)) - intraspread_factor;
		  // LOGGER.info(demand_modifier+" "+intraspread_factor+" " + interpolated_price);

		  // Fair forex price is a bijection of currency_one <-> currency_two, so multiplying/dividing by it will give
		  // the other side's value.
		        
		    String currency_one = StonkCompanionClient.currency_type_to_hyper_text.get(curr_one_type);
		    String currency_two = StonkCompanionClient.currency_type_to_hyper_text.get(curr_two_type);

		    fairprice_text_message = String.format(
		      "[StonkCompanion] %s's FairStonk is:\n1 %s -> %s %s\n1 %s -> %s %s", 
		      StonkCompanionClient.categoreyMaker(label), 
		      currency_one,
		      StonkCompanionClient.df1.format(interpolated_price), 
		      currency_two,
		      currency_two,
		      StonkCompanionClient.df1.format(1/interpolated_price),
		      currency_one
		    );

		    		    
		    String fairprice_gui_msg_1 = "1 %s -> %s %s".formatted(currency_one, StonkCompanionClient.df1.format(interpolated_price), currency_two);
		    String fairprice_gui_msg_2 = "1 %s -> %s %s".formatted(currency_two, StonkCompanionClient.df1.format(1/interpolated_price), currency_one);
		    	    
		    fairprice_gui_message = fairprice_gui_msg_1 + "|" + fairprice_gui_msg_2;
		    
		    // Check for if there is not enough to actually do a trade.
		    if(one_to_two > comp_curr_two) {
		    	demand_modifier = 1;
		    }else if (two_to_one > comp_curr_one) {
		    	demand_modifier = 0;
		    }

		    if(demand_modifier <= 0.005) {
		    	if(curr_one_type == 2) {
		    		fairprice_text_message = "[StonkCompanion] Look in lower barrel.";
		    		fairprice_gui_message = "Look in lower barrel.";
		    	}else {
		    		fairprice_text_message = "[StonkCompanion] Look in higher barrel.";
		    		fairprice_gui_message = "Look in higher barrel.";
		    	}
		    }else if(demand_modifier >= 0.995) {
		    	if(curr_one_type == 2) {
		    		fairprice_text_message = "[StonkCompanion] Look in higher barrel.";
		    		fairprice_gui_message = "Look in higher barrel.";
		    	}else {
		    		fairprice_text_message = "[StonkCompanion] Look in lower barrel.";
		    		fairprice_gui_message = "Look in lower barrel.";  
		    	}
		    }
		    generateGuiText();

		}
	
	public boolean validateTransaction() {
		if (barrel_transactions.isEmpty()) {
			barrel_transaction_validity = true;
			barrel_transaction_solution = "";
			barrel_transaction_full_hyper_solution = "";
			mistrade_text_message = "";
			generateGuiText();
			return true;
		}
		
		barrel_transaction_full_hyper_solution = "";
		barrel_transaction_solution = "";
		
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
		
        // This is a foreign exchange transaction
        boolean valid_transaction = false;
        wrong_currency = false;
        double currency_one = 0.0;
        double currency_two = 0.0;
        double expected_compressed = 0.0;
        double actual_compressed = 0.0;
        
        if (curr_one_type == 1 && curr_two_type == 2) {
            currency_one = r1_compressed;
            currency_two = r2_compressed;
            if (r3_compressed != 0.0) {
                wrong_currency = true;
            }

        } else if (curr_one_type == 1 && curr_two_type == 3) {
            currency_one = r1_compressed;
            currency_two = r3_compressed;
            if (r2_compressed != 0.0) {
                wrong_currency = true;
            }

        } else if (curr_one_type == 2 && curr_two_type == 3) {
            currency_one = r2_compressed;
            currency_two = r3_compressed;
            if (r1_compressed != 0.0) {
                wrong_currency = true;
            }
        }
        
	    String currency_corrective_str = "";
	    String hyper_corrective_str = "";
	    
	    String currency_full_str = "";
	    String hyper_full_str = "";
	    
	    String currency_one_str = StonkCompanionClient.currency_type_to_compressed_text.get(curr_one_type);
	    String hyper_one_str = StonkCompanionClient.currency_type_to_hyper_text.get(curr_one_type);
	    
	    String currency_two_str = StonkCompanionClient.currency_type_to_compressed_text.get(curr_two_type);
	    String hyper_two_str = StonkCompanionClient.currency_type_to_hyper_text.get(curr_two_type);
	    
	    double full_hyper_owed = 0.0;

	    // TODO: Full hyper input checking and flagging if not.
	    
        if (currency_one >= 0) {
            // currency_one was added, so currency_two's expected amount can be predicted from it
        	if (currency_one % 64 != 0) {
        		// Currency_one was added, but not in a full hyper amount. Therefore this is a non-full hyper input mistrade.
        		// I guess add the difference till it reaches 64? So if 1 hcs 1 ccs then they owe +63 ccs?
        		full_hyper_owed = currency_one%64;
                expected_compressed = -1.0*((currency_one+full_hyper_owed)/64)*one_to_two;
                
                currency_full_str = currency_one_str;
                hyper_full_str = hyper_one_str;
                
        	}else {
                expected_compressed = -1.0*(currency_one/64)*one_to_two;        		
        	}
        	
            actual_compressed = currency_two;
            
            currency_corrective_str = currency_two_str;
            hyper_corrective_str = hyper_two_str;

        } else {
            // currency_one was taken, so currency_one's expected amount can be predicted from it
        	
        	if (currency_two > 0 && currency_two % 64 != 0) {
        		full_hyper_owed = currency_two%64;
        		expected_compressed = -1.0*((currency_two+full_hyper_owed)/64)*two_to_one;
        		
                currency_full_str = currency_two_str;
                hyper_full_str = hyper_two_str;
        		
        	}else {
        		expected_compressed = -1.0*(currency_two/64)*two_to_one;
        	}
        	
            actual_compressed = currency_one;
            
            currency_corrective_str = currency_one_str;
            hyper_corrective_str = hyper_one_str;
        }
        // TODO: Pretty printing
        
	    double currency_delta = expected_compressed - actual_compressed;
	    
	    // Bounds check.
	    if(currency_delta < 0.0005 && currency_delta > -0.0005) currency_delta = 0;
	    
	    valid_transaction = barrel_transaction_validity = (currency_delta == 0 && !wrong_currency && full_hyper_owed == 0);
	    
		if (!valid_transaction) {
			// TODO: Check for if compressed or not toggle.
			if(currency_delta != 0) convertSolutionToCompressed(currency_delta, currency_corrective_str, hyper_corrective_str);
			if(full_hyper_owed != 0) barrel_transaction_full_hyper_solution = "%s 0 %s %s %s".formatted("Put in", hyper_full_str, StonkCompanionClient.df1.format(full_hyper_owed), currency_full_str);
			// barrel_transaction_solution = "Correction amount: %s %s %s (%d %s %s %s)".formatted(correction_dir, StonkCompanionClient.df1.format(Math.abs(currency_delta)), currency_str, corrective_hyper_amount, hyper_str, StonkCompanionClient.df1.format(corrective_compressed_amount), currency_str);

		}else {
			barrel_transaction_solution = "";
		}
		
		StringBuilder build_mistrade_text = new StringBuilder();
	    
	    String correction_dir = (currency_delta < 0) ? "Take out" : "Put in";
	    
		int corrective_hyper_amount = (int)(Math.floor(Math.abs(currency_delta)/64));
		double corrective_compressed_amount = (Math.abs(currency_delta)%64);
		
		build_mistrade_text.append("---[StonkCompanion]---");
		build_mistrade_text.append("\n%s (%s)".formatted(label, coords));
		build_mistrade_text.append("\n%s".formatted(curr_one_str));
		build_mistrade_text.append("\n%s".formatted(curr_two_str));
		build_mistrade_text.append("\n%s: %s %s (%d %s %s %s)".formatted((currency_one < 0) ? "Took" : "Paid", StonkCompanionClient.df1.format(Math.abs(currency_one)), currency_one_str, (int)(Math.floor(Math.abs(currency_one)/64)), hyper_one_str, Math.abs(currency_one)%64, currency_one_str));
		build_mistrade_text.append("\n%s: %s %s (%d %s %s %s)".formatted((currency_two < 0) ? "Took" : "Paid", StonkCompanionClient.df1.format(Math.abs(currency_two)), currency_two_str, (int)(Math.floor(Math.abs(currency_two)/64)), hyper_two_str, Math.abs(currency_two)%64, currency_two_str));
		if(barrel_transaction_validity) build_mistrade_text.append("\nValid Transaction");
		if(full_hyper_owed != 0) build_mistrade_text.append("\nCorrection amount: %s 0 %s %s %s".formatted("Put in", hyper_full_str, StonkCompanionClient.df1.format(full_hyper_owed), currency_full_str));
		if(currency_delta != 0) build_mistrade_text.append("\nCorrection amount: %s %s %s (%d %s %s %s)".formatted(correction_dir, StonkCompanionClient.df1.format(Math.abs(currency_delta)), currency_corrective_str, corrective_hyper_amount, hyper_corrective_str, StonkCompanionClient.df1.format(corrective_compressed_amount), currency_corrective_str));
		build_mistrade_text.append("\nTime since last log: %ds/%ds".formatted(time_since_last_movement/20, transaction_lifetime/20));
		if(wrong_currency) build_mistrade_text.append("\nWrong currency was used!");
		build_mistrade_text.append("\n--------------------");
		
		mistrade_text_message = build_mistrade_text.toString();
		
		generateGuiText();
		
	    if(other_items == 0 && currency_delta == 0 && !wrong_currency && full_hyper_owed != 0 && barrel_transactions.isEmpty()) {
			return true;
		}
		
		return false;
	}
	
	public void convertSolutionToCompressed(double currency_delta, String currency_str, String hyper_str) {

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
		return start + " curr_one_str: " + curr_one_str + " curr_two_str: " + curr_two_str;
	}

}
