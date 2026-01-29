package net.stonkcompanion.main;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joml.Math;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.stonkcompanion.suggestions.StonkCompanionCommandsSuggestions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.stonkcompanion.main.Barrel.BarrelTypes;
import net.stonkcompanion.mixin.client.PlayerListHudAccessor;

public class StonkCompanionClient implements ClientModInitializer{

	public static final String MOD_ID = "stonkcompanion";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static String top_dir = FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion").toString();
	
	// Coreprotect changes. Like changing the hovertext literal to the monu item name.
	private boolean change_coreprotect = true;
	
	// Bool
	public static boolean is_verbose_logging = true;
	public static boolean is_showing_text = true;
	public static boolean is_showing_gui = true;
	
	public static final Map<Integer, String> currency_type_to_compressed_text = Map.of(1, "cxp", 2, "ccs", 3, "ar");
	public static final Map<Integer, String> currency_type_to_hyper_text = Map.of(1, "hxp", 2, "hcs", 3, "har");
	
	// Anti-monu flag
	
	// Quick Craft Handling
	public static boolean is_quick_crafting = false;
	public static int quick_craft_button = -1;
	public static int time_since_start_of_quick_craft = 0;
	public static int max_time_for_quick_craft  = 3;
	public static HashMap<Integer, Integer> quick_craft_slot_qty = new HashMap<>();
	public static String quick_craft_item_name = "";
	public static int quick_craft_item_qty = 0;
	public static int quick_craft_item_max_stack = 0;
	public static String quick_craft_barrel_pos = "";
	public static int quick_craft_in_player_inv = 0;
	public static ItemStack quick_craft_itemstk = null;

	// Checkpoint vars. Probs move elsewhere later.
	public static boolean checkpointing = false;
	public static BlockPos last_right_click = null;
	public static boolean anti_monu = false;
	public static long open_barrel_time;
	public static String open_barrel_values = "";
	public static JsonObject checkpoints = new JsonObject();
	
	// I need to find a better way to do this. Oh well
	public static boolean fairprice_detection = true;
	public static boolean did_screen_resize = false;
	public static double fairprice_val = 0.0;
	public static String fairprice_currency_str = "";
	
	// Shamelessly stolen from UMM.
	private static final Pattern shardGetterPattern = Pattern.compile(".*<(?<shard>[-\\w\\d]*)>.*");
	public static String cachedShard = null;
	private static long lastUpdateTimeShard = 0;
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	
	// Mistrade checking :fire:
	public static boolean is_mistrade_checking = true;
	// public static HashMap<String, HashMap<String, Integer>> barrel_transactions = new HashMap<>();
	// public static HashMap<String, Integer> barrel_timeout = new HashMap<>();
	public static HashMap<String, Barrel> barrel_prices = new HashMap<>();
	// public static HashMap<String, double[]> barrel_actions = new HashMap<>();
	// public static HashMap<String, Boolean> barrel_transaction_validity = new HashMap<>();
	// public static HashMap<String, String> barrel_transaction_solution = new HashMap<>();
	public static boolean anti_monu_inv_init = false;
	public static boolean anti_monu_is_not_barrel = false;
	public static boolean is_there_barrel_price = false;
	public static String barrel_pos_found = "";
	public static boolean is_compressed_only = false;
	public static final DecimalFormat df1 = new DecimalFormat( "#.###" );
	public static boolean has_offhandswap_off = false;
	
	public static HashMap<Integer, ItemStack> barrel_changes = new HashMap<>();
	
	public static int previous_action_qty_put = 0;
	public static String previous_action_name_put = "";
	public static int previous_action_qty_take = 0;
	public static String previous_action_name_take = "";
	public static boolean action_been_done = false;
	
	
	public static String getShard() {
		if (cachedShard != null && lastUpdateTimeShard + 2000 > System.currentTimeMillis()) {
			return cachedShard;
		}
		Text header = ((PlayerListHudAccessor) mc.inGameHud.getPlayerListHud()).getHeader();
		String shard = "unknown";
		if (header != null) {
			String text = header.getString();
			Matcher matcher = shardGetterPattern.matcher(text);
			if (matcher.matches()) {
				shard = matcher.group("shard");
			}
		}

		cachedShard = shard;
		lastUpdateTimeShard = System.currentTimeMillis();
		return shard;
	}
	
	public static int getCurrencyType(String _s) {
		
		String s = _s.toLowerCase().trim();
		
		if(s.endsWith("har") || s.endsWith("ar") || s.equals("hyperchromatic archos ring") || s.equals("archos ring")) {
			return 3;
		}else if(s.endsWith("hcs") || s.endsWith("ccs") || s.endsWith("cs") || s.equals("hyper crystalline shard") || s.equals("compressed crystalline shard") || s.equals("crystalline shard")) {
			return 2;
		}else if(s.endsWith("hxp") || s.endsWith("cxp") || s.endsWith("xp") || s.equals("hyperexperience") || s.equals("concentrated experience") || s.equals("experience bottle")) {
			return 1;
		}else {
			return 0;
		}
	}
	
	public static double givenCurrReturnMult(String _s) {
		String s = _s.toLowerCase().trim();
		double mult = -1;
		
		if(s.equals("hyperexperience") || s.equals("hyper crystalline shard") || s.equals("hyperchromatic archos ring")) {
			mult = 64;
		}else if(s.equals("concentrated experience") || s.equals("compressed crystalline shard") || s.equals("archos ring")) {
			mult = 1;
		}else if(s.equals("experience bottle") || s.equals("crystalline shard")) {
			mult = 1/8;
		}
		
		return mult;
	}
	
	//Given a string of the form "#( )[currency]"
	//Returns a string that is just the # in the lowest form of that currency.
	// 1 hxp -> 512 xp for example.
	public static double convertToBaseUnit(String s) {
		
		if (s.length() > 0 && s.contains("per")) {
			s = s.replace("per", "");
		}
		s = s.trim();
		
		double amount = -1;
		String detect_amount = "";
		String currency = "";
		
		for(int i = 0; i < s.length(); i++) {
			
			char c = s.charAt(i);
			
			if((c >= '0' && c <= '9') || c == '.') {
				detect_amount += c;
			}else {
				currency = s.substring(i).trim();
				break;
			}
		}
		
		if(!currency.isEmpty() && detect_amount != "") {
			
			amount = Double.parseDouble(detect_amount);
			
			if(currency.equals("har") || currency.equals("hxp") || currency.equals("hcs")) {
				amount = amount*64.0;
			}else if(currency.equals("xp") || currency.equals("cs")) {
				amount = amount/8.0;
			}
				
		}
		
		return amount;
	}
	
	public static String[] detectFairPrice(List<Slot> items) {
		
		double barrel_compressed_currency = 0;
		
		// The assumption is that there is basically just currency and mats in the barrel and 1 sign that says the price.
		// needmoney edit: In a forex pair, the barrel mats are currency_two
		double barrel_mats = 0;
		
		int currency_type = 0;
		boolean is_forex = false;
		// I have modified the default currency_type to 0 instead of -1. This allows for indexing in two directions.
		// If the currency type is positive, it is the format expected. If the currency type is negative, each index
		// represents a special handling type for foreign exchange.
		// -1 = R1R2
		// -2 = R1R3
		// -3 = R2R3

		String label = "";

		// If this is a forex barrel, we will assume that one_to_two is the ask and two_to_one is the bid.
		String ask_price = "";
		String bid_price = "";
		double ask_price_compressed = -1;
		double bid_price_compressed = -1;
		
		// Assumed it is a barrel or chest so check 27 slots. First pass is to check for sign.
		for(int i = 0; i < 27; i++) {
			
			if(!items.get(i).hasStack()) continue;
			
			ItemStack item = items.get(i).getStack();
			
			String item_name = "";
			
			if(item.getNbt() == null) {
				continue;
			}
			
			if(!item.getNbt().contains("Monumenta")) {

				item_name = item.getItem().getTranslationKey().substring(item.getItem().getTranslationKey().lastIndexOf('.')+1);
				
				if(item_name.toLowerCase().endsWith("sign")) {
					// Okay we have a sign. Now to look to see if it has buy sell on it.
					if (!item.getNbt().contains("BlockEntityTag") || !item.getNbt().getCompound("BlockEntityTag").contains("back_text") || !item.getNbt().getCompound("BlockEntityTag").getCompound("back_text").contains("messages")) {
						continue;
					}
					if (!item.getNbt().contains("BlockEntityTag") || !item.getNbt().getCompound("BlockEntityTag").contains("front_text") || !item.getNbt().getCompound("BlockEntityTag").getCompound("front_text").contains("messages")) {
						continue;
					}
					
					NbtList sign_info = item.getNbt().getCompound("BlockEntityTag").getCompound("front_text").getList("messages", NbtElement.STRING_TYPE);
					
					if(sign_info.size() > 2 && sign_info.get(0) != null) {					
						label = sign_info.get(0).asString().replace("\"", "").trim();
					}
					
					for(NbtElement _e : sign_info) {
						
						String _sign_line = _e.asString().toLowerCase().replace("\"", "").trim();
						
						int find_ask = _sign_line.indexOf("buy for");
						int find_bid = _sign_line.indexOf("sell for");
						int find_forex = _sign_line.indexOf("currency");
						int find_arrow = _sign_line.indexOf("→");

						if (find_forex != -1) {
							// Out of a lack of desire to standardize arbitrary forex pairs of any type, I will define them as special cases.
							// If we end up with many special cases, we can figure out a standard.
							is_forex = true;

							if (_sign_line.toLowerCase().contains("r1r2")) {
								currency_type = -1;

							} else if (_sign_line.toLowerCase().contains("r1r3")) {
								currency_type = -2;

							} else if (_sign_line.toLowerCase().contains("r2r3")) {
								currency_type = -3;

							} else {
								// sign has the word currency but not a recognized pair. Error.
								continue;
							}

						}

						if (is_forex) {
							if (find_arrow != -1) {
								String left_side = _sign_line.substring(0,find_arrow);
								String right_side = _sign_line.substring(find_arrow+1);

								int left_side_currency = StonkCompanionClient.getCurrencyType(left_side.trim());
								int right_side_currency = StonkCompanionClient.getCurrencyType(right_side);
								
								// LOGGER.info(_sign_line);
								// LOGGER.info(left_side_currency + " " + right_side_currency);

								if (left_side_currency < right_side_currency) {
									// This is the one_to_two value
									ask_price = _sign_line.trim();
									ask_price_compressed = StonkCompanionClient.convertToBaseUnit(_sign_line.substring(find_arrow+2)); // +1 skips arrow, +2 skips space

								} else {
									// This is the two_to_one value
									bid_price = _sign_line.trim();
									bid_price_compressed = StonkCompanionClient.convertToBaseUnit(_sign_line.substring(find_arrow+2)); // +1 skips arrow, +2 skips space
								}
							}

						} else {

							if (find_ask != -1) {
								
								ask_price = _sign_line.substring(find_ask).trim();
								
								// We now have the line of text with the buy price.
								currency_type = StonkCompanionClient.getCurrencyType(_sign_line);
								
								ask_price_compressed = StonkCompanionClient.convertToBaseUnit(_sign_line.substring(find_ask+7));
								
							} else if(find_bid != -1) {
								
								bid_price = _sign_line.substring(find_bid).trim();
								
								// We now have the line of text with the sell price.
								currency_type = StonkCompanionClient.getCurrencyType(_sign_line);
								bid_price_compressed = StonkCompanionClient.convertToBaseUnit(_sign_line.substring(find_bid+8));
							}
						}
					}
					
					// Break if we have found the sign. Assuming the first found correctly formatted sign is the correct sign. If it isn't. User error.
					if (currency_type != 0 && ask_price_compressed != -1 && bid_price_compressed != -1) {
						break;
					}
				}
			}
		}
		
		// LOGGER.info(currency_type+"");
		// LOGGER.info(ask_price_compressed+"");
		// LOGGER.info(bid_price_compressed+"");
		
		if (currency_type == 0 || ask_price_compressed == -1 || bid_price_compressed == -1) {
			return null;
		}
		
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
		if (currency_type < 0) {
			// Forex barrel fair price handling goes here. 
			// The actual pair type doesnt matter, only that it is forex, since we have the rates and nets.

			// We should have access to:
			// net_currency_one is stored in barrel_compressed_currency
			// net_currency_two is stored in barrel_mats
			// one_to_two is stored in ask_price_compressed
			// two_to_one is stored in bid_price_compressed
			double one_to_two = ask_price_compressed;
			double two_to_one = bid_price_compressed;
			
			// LOGGER.info(one_to_two+" "+two_to_one);

			double spread = (1/(two_to_one/64)) - (1/(64/one_to_two));
			double mid = (1/(64/one_to_two)) + spread/2;
			
			// LOGGER.info(mid+" "+spread);

			double net_currency_in_barrel = barrel_compressed_currency + barrel_mats/mid;
			
			// LOGGER.info(barrel_compressed_currency+" "+barrel_mats);

			double demand_modifier = barrel_compressed_currency / net_currency_in_barrel;
			double intraspread_factor = demand_modifier * spread;
			double interpolated_price = (1/(two_to_one/64)) - intraspread_factor;
			// LOGGER.info(demand_modifier+" "+intraspread_factor+" " + interpolated_price);

			// Fair forex price is a bijection of currency_one <-> currency_two, so multiplying/dividing by it will give
			// the other side's value.

			// return null; // uncomment below when its all done
			// TODO: Add handling for -1 to -3 in currency type handling
			return new String[]{interpolated_price+"", currency_type+"", demand_modifier+"", label};

		} else {
			double spread = ask_price_compressed - bid_price_compressed;
			//LOGGER.info("Spread: %.3f".formatted(spread));
		    double mid = bid_price_compressed + spread / 2.0;
		    //LOGGER.info("Mid: %.3f".formatted(mid));
		    
		    double mats_in_currency = barrel_compressed_currency / mid;
		    //LOGGER.info("Mats in currency: %.3f".formatted(mats_in_currency));
		    double effective_mats = barrel_mats + mats_in_currency;
		    //LOGGER.info("Effective mats: %.3f".formatted(effective_mats));
		    
		    if(effective_mats == 0) {
		    	return null;
		    }
		    
		    double demand_modifier = mats_in_currency / effective_mats;
		    //LOGGER.info("Demand modifier: %.3f".formatted(demand_modifier));
		    double intraspread_factor = demand_modifier * spread;
		    //LOGGER.info("intraspread_factor: %.3f".formatted(intraspread_factor));
		    double interpolated_price = bid_price_compressed + intraspread_factor;
		    
		    return new String[]{interpolated_price+"", currency_type+"", demand_modifier+"", label};
		}
	    
	}
	
	public void writeCheckpoints() {
		
		String current_timestamp = ""+Instant.now().getEpochSecond();
		
		if (checkpoints.isEmpty()) {
			return;
		}
		
		try (FileWriter writer = new FileWriter(top_dir+"/"+current_timestamp+"_checkpoints.json")){
			Gson gson = new GsonBuilder().create();
			gson.toJson(checkpoints, writer);
			checkpoints = new JsonObject();
		} catch (IOException e) {
			LOGGER.error("StonkCompanion failed to create the checkpoints json!");
		}
	}
	
	private void writeConfig() {
		
		JsonObject config_stuff = new JsonObject();		
		
		// Just adding all the config vars.
		config_stuff.addProperty("mistrade_check", is_mistrade_checking);
		config_stuff.addProperty("fairprice_detection", fairprice_detection);
		config_stuff.addProperty("is_compressed_only", is_compressed_only);
		config_stuff.addProperty("is_showing_text", is_showing_text);
		config_stuff.addProperty("is_showing_gui", is_showing_gui);
		config_stuff.addProperty("offhand_swap", has_offhandswap_off);
		
		try (FileWriter writer = new FileWriter(top_dir+"/StonkCompanionConfig.json")){
			Gson gson = new GsonBuilder().create();
			gson.toJson(config_stuff, writer);
			config_stuff = new JsonObject();
		} catch (IOException e) {
			LOGGER.error("StonkCompanion failed to create the config json!");
		}
		
	}
	
	private void readConfig() {
		File test_for_json = new File(top_dir+"/StonkCompanionConfig.json");

		if(test_for_json.exists()) {

			try {

				JsonObject test_obj = JsonParser.parseString(Files.readString(Paths.get(top_dir+"/StonkCompanionConfig.json"))).getAsJsonObject();

				if(test_obj.has("mistrade_check")) {
					is_mistrade_checking = test_obj.get("mistrade_check").getAsBoolean();
				}
				
				if(test_obj.has("fairprice_detection")) {
					fairprice_detection = test_obj.get("fairprice_detection").getAsBoolean();
				}
				
				if(test_obj.has("is_compressed_only")) {
					is_compressed_only = test_obj.get("is_compressed_only").getAsBoolean();
				}
				
				if(test_obj.has("is_showing_text")) {
					is_showing_text = test_obj.get("is_showing_text").getAsBoolean();
				}
				
				if(test_obj.has("is_showing_gui")) {
					is_showing_gui = test_obj.get("is_showing_gui").getAsBoolean();
				}
				
				if(test_obj.has("offhand_swap")){
					has_offhandswap_off = test_obj.get("offhand_swap").getAsBoolean();
				}

			} catch (IOException e) {

				LOGGER.error("Could not find or read json config file.");

			}
		}
	}
	
	/*
	 * As the name implies, this function is to go through every barrel in barrel_transactions and try to detect if any were mistrades.	
	 */
	/*public static void mistradeCheck(String barrel_pos, boolean hide_valid) {
		
		if (!barrel_prices.containsKey(barrel_pos)) {
			return;
		}
		
		if (!barrel_transactions.containsKey(barrel_pos)) {
			return;
		}
			
		// So we should have a map of items to their total qtyies traded in this barrel
		// and a map of barrel position to all the needed price info.
		// Time to check if it is a mistrade or not.
			
		Barrel traded_barrel = barrel_prices.get(barrel_pos);
			
		// TODO: Clean up currency conversions.
		// TODO: Look into maybe checking what is being traded and not just assuming only the correct item is being traded.
		double r1_compressed = 0.0;
		double r2_compressed = 0.0;
		double r3_compressed = 0.0;
		double other_items = 0;
			
		for (String traded_item : barrel_transactions.get(barrel_pos).keySet()) {
								
			int item_qty = barrel_transactions.get(barrel_pos).get(traded_item);
				
			String traded_item_lc = traded_item.toLowerCase();
				
			// EEEEEEE
			if(traded_item_lc.equals("hyperexperience")) {
				r1_compressed += 64*item_qty;
			}else if(traded_item_lc.equals("concentrated experience")) {
				r1_compressed += item_qty;
			}else if(traded_item_lc.equals("experience bottle")) {
				r1_compressed += item_qty/8.0;
			}else if(traded_item_lc.equals("hyper crystalline shard")) {
				r2_compressed += 64*item_qty;
			}else if(traded_item_lc.equals("compressed crystalline shard")) {
				r2_compressed += item_qty;
			}else if(traded_item_lc.equals("crystalline shard")) {
				r2_compressed += item_qty/8.0;
			}else if(traded_item_lc.equals("hyperchromatic archos ring")) {
				r3_compressed += 64*item_qty;
			}else if(traded_item_lc.equals("archos ring")) {
				r3_compressed += item_qty;
			}else {
				other_items += item_qty;
			}
		}
		
		// Checking for stacks barrels.
		if(traded_barrel.label.toLowerCase().startsWith("64x") || traded_barrel.label.toLowerCase().contains("stack")) {
			other_items = other_items/64.0;
		}
			
		// Okay we have all our ducks in a row. Now to verify if this trade was correct.
		double expected_compressed = (other_items < 0) ? Math.abs(other_items)*traded_barrel.compressed_ask_price : -1*other_items*traded_barrel.compressed_bid_price;
		double actual_compressed = 0.0;
		boolean valid_transaction = false;
		boolean wrong_currency = false;
			
		if(traded_barrel.currency_type == 1) {
			valid_transaction = r1_compressed == expected_compressed;
			actual_compressed = r1_compressed;
				
			if(r2_compressed != 0.0 || r3_compressed != 0.0) {
				wrong_currency = true;
			}
		}
			
		if(traded_barrel.currency_type == 2) {
			valid_transaction = r2_compressed == expected_compressed;
			actual_compressed = r2_compressed;
			
			if(r1_compressed != 0.0 || r3_compressed != 0.0) {
				wrong_currency = true;
			}
		}
			
		if(traded_barrel.currency_type == 3) {
			valid_transaction = r3_compressed == expected_compressed;
			actual_compressed = r3_compressed;
				
			if(r2_compressed != 0.0 || r1_compressed != 0.0) {
				wrong_currency = true;
			}
		}
			
		/
		 * Here is where hell resides. Pretty printing the mistrades.
		 * Barrel Name (Barrel Coordinates)
		 * Buy: price in compressed (price as written on barrel)
		 * Sell: price in compressed (price as written on barrel)
		 * Sold/Bought: number of mats
		 * Paid/Took: Net Currency
		 * Unit Price: (net currency / number of mats)
		 * Correction amount: (+/- currency to resolve mistrade.)
		 * (If wrong_currency) Wrong Currency was used!
		 /
			
		if(valid_transaction && hide_valid) return;
		
	    String currency_str = currency_type_to_compressed_text.get(traded_barrel.currency_type);
	    String hyper_str = currency_type_to_hyper_text.get(traded_barrel.currency_type);
	        
	    double currency_delta = expected_compressed - actual_compressed;
	    
	    // Bounds check.
	    if(currency_delta < 0.0005 && currency_delta > -0.0005) currency_delta = 0;
	    
	    
	    // Funny check if can be fixed by adding / taking mats.
	    int mats_delta = 0;
	    
	    if(other_items < 0) {
	    	
	    	// Player bought mats
	    	mats_delta = (currency_delta % traded_barrel.compressed_ask_price == 0) ? (int)(currency_delta / traded_barrel.compressed_ask_price) : 0;		    	
	    	
	    }else if (other_items > 0) {
	    	
	    	// Player sold mats
	    	mats_delta = (currency_delta % traded_barrel.compressed_bid_price == 0) ? (int)(currency_delta / traded_barrel.compressed_bid_price) : 0;
	    	
	    }
		
		int actual_hyper_amount = (int)(Math.floor(Math.abs(actual_compressed)/64));
		double actual_compressed_amount = (Math.abs(actual_compressed)%64);
	    
		int corrective_hyper_amount = (int)(Math.floor(Math.abs(currency_delta)/64));
		double corrective_compressed_amount = (Math.abs(currency_delta)%64);
		
	    String correction_dir = (currency_delta < 0) ? "Take out" : "Put in";
	        
	    MinecraftClient mc = MinecraftClient.getInstance();
	    
	    if(currency_delta != 0)mc.player.sendMessage(Text.literal("[StonkCompanion] Mistrade detected in " + traded_barrel.label));
	    
	    if(is_showing_text) {
		    mc.player.sendMessage(Text.literal("---[StonkCompanion]---"));
		    mc.player.sendMessage(Text.literal("%s (%s)".formatted(traded_barrel.label, barrel_pos)));
		    mc.player.sendMessage(Text.literal("Buy: %s %s (%s)".formatted(df1.format(traded_barrel.compressed_ask_price), currency_str, traded_barrel.ask_price)));
		    mc.player.sendMessage(Text.literal("Sell: %s %s (%s)".formatted(df1.format(traded_barrel.compressed_bid_price), currency_str, traded_barrel.bid_price)));
		    mc.player.sendMessage(Text.literal("%s: %s".formatted((other_items < 0) ? "Bought" : "Sold", df1.format(Math.abs(other_items)))));
		    mc.player.sendMessage(Text.literal("%s: %s %s (%d %s %s %s)".formatted((actual_compressed < 0) ? "Took" : "Paid", df1.format(Math.abs(actual_compressed)), currency_str, actual_hyper_amount, hyper_str, df1.format(actual_compressed_amount), currency_str)));
		    if(other_items!=0) mc.player.sendMessage(Text.literal("Unit Price: %s".formatted(df1.format(Math.abs(actual_compressed / (other_items))))));
		    if(currency_delta == 0) mc.player.sendMessage(Text.literal("Valid Transaction"));
		    if(currency_delta != 0) {
			    mc.player.sendMessage(Text.literal("Correction amount: %s %s %s (%d %s %s %s)".formatted(correction_dir, df1.format(Math.abs(currency_delta)), currency_str, corrective_hyper_amount, hyper_str, df1.format(corrective_compressed_amount), currency_str)));
		    }
		    if(mats_delta != 0) mc.player.sendMessage(Text.literal("(OR) Correction amount: %s %d mats".formatted(correction_dir, Math.abs(mats_delta))));
		    mc.player.sendMessage(Text.literal("Time since last log: %ds/%ds".formatted(barrel_timeout.get(barrel_pos)/20, transaction_lifetime/20)));
		    if(wrong_currency) mc.player.sendMessage(Text.literal("Wrong currency was used!"));
		    mc.player.sendMessage(Text.literal("--------------------"));
	    }
	    
	    if(other_items == 0 && currency_delta == 0 && !wrong_currency) {
			barrel_transactions.remove(barrel_pos);
			barrel_prices.remove(barrel_pos);
			barrel_timeout.remove(barrel_pos);
			barrel_actions.remove(barrel_pos);
			barrel_transaction_validity.remove(barrel_pos);
			barrel_transaction_solution.remove(barrel_pos);
		}
	    
	}*/
	
	public static String categoreyMaker(String label) {
		// Changing barrel label "Stonk #" -> "Stonk"
		// Case 1: label is of the form "Stonk #".
		// Case 2: label is of the form "Stonk0#". Why? Idk. Just was originally written that way.

		String category = label.trim();
		int label_len = label.length();
		if(label_len > 0 && '0' <= label.charAt(label_len-1) && label.charAt(label_len-1) <= '9') {
		  category = label.substring(0, label_len-1).trim();
		}else if (label_len > 1 && label.charAt(label_len-1) == '0' && '0' <= label.charAt(label_len-2) && label.charAt(label_len-2) <= '9') {
		  category = label.substring(0, label_len-2).trim();
		}
		
		return category;
	}
	
	private void mistradeCheck() {
		// barrel_timeout.clear();
		
		// LOGGER.info("Checking Mistrades!");
		
		/*for(String barrel_pos : barrel_transactions.keySet()) {
			LOGGER.info(barrel_pos +"'s changes:");
			for(String traded_item : barrel_transactions.get(barrel_pos).keySet()) {
				int item_qty = barrel_transactions.get(barrel_pos).get(traded_item);
				LOGGER.info(traded_item + " " + item_qty);
			}
			LOGGER.info("Barrel's barrel info: ");
			if(!barrel_prices.containsKey(barrel_pos)) {
				LOGGER.info("Doesn't Exist.");
			}else {
				Barrel traded_barrel = barrel_prices.get(barrel_pos);
				LOGGER.info(traded_barrel.coords);
				LOGGER.info(traded_barrel.label);
				LOGGER.info(traded_barrel.ask_price);
				LOGGER.info(traded_barrel.bid_price);
				LOGGER.info("" + traded_barrel.compressed_ask_price);
				LOGGER.info("" + traded_barrel.compressed_bid_price);
				LOGGER.info("" + traded_barrel.currency_type);
			}
			
		}*/
		
		MinecraftClient mc = MinecraftClient.getInstance();
		
		if(barrel_prices.isEmpty()) {
			mc.player.sendMessage(Text.literal("[StonkCompanion] There are no transactions."));
		}else {	
			for(String barrel_pos : barrel_prices.keySet()) {
				
				Barrel active_barrel = barrel_prices.get(barrel_pos);
				
				boolean remove_barrel = active_barrel.validateTransaction();
				
			    if(!active_barrel.barrel_transaction_validity) mc.player.sendMessage(Text.literal("[StonkCompanion] Mistrade detected in " + active_barrel.label));
				
			    if(!active_barrel.mistrade_text_message.isBlank() && !active_barrel.barrel_transaction_validity) mc.player.sendMessage(Text.literal(active_barrel.mistrade_text_message));
				
				if(remove_barrel) {
					StonkCompanionClient.barrel_prices.remove(barrel_pos);
				}
			}
			
			mc.player.sendMessage(Text.literal("[StonkCompanion] Done."));
		}
		
	}
	
	private void quickCraftManagement() {
		
		if(quick_craft_slot_qty.isEmpty()) return;
		if(barrel_prices.get(quick_craft_barrel_pos) == null) return;

		int total_quick_craft_slots = quick_craft_in_player_inv + quick_craft_slot_qty.size();
		int quick_craft_split_amount = (quick_craft_button == 5 ) ? 1 : (int)(quick_craft_item_qty/total_quick_craft_slots);
			
		int total_put_in_via_quick_craft = 0;
			
		for(int i : quick_craft_slot_qty.keySet()) {
			
			int slot_qty = quick_craft_slot_qty.get(i);
				
			if(slot_qty+quick_craft_split_amount > quick_craft_item_max_stack) {
				total_put_in_via_quick_craft += quick_craft_item_max_stack - slot_qty;
				StonkCompanionClient.barrel_changes.put(i, quick_craft_itemstk.copyWithCount(quick_craft_item_max_stack));
			}else {
				total_put_in_via_quick_craft += quick_craft_split_amount;
				StonkCompanionClient.barrel_changes.put(i, quick_craft_itemstk.copyWithCount(quick_craft_split_amount));
			}
				
		}
		
		if(total_put_in_via_quick_craft == 0) return;
		
		action_been_done = true;
		
		Barrel active_barrel = barrel_prices.get(quick_craft_barrel_pos);
		active_barrel.time_since_last_movement = 0;
		active_barrel.barrel_transactions.put(quick_craft_item_name, active_barrel.barrel_transactions.getOrDefault(quick_craft_item_name, 0) + total_put_in_via_quick_craft);
		
		// barrel_transactions.putIfAbsent(quick_craft_barrel_pos, new HashMap<String, Integer>());
		// barrel_timeout.put(quick_craft_barrel_pos, 0);
		// barrel_transactions.get(quick_craft_barrel_pos).put(quick_craft_item_name, barrel_transactions.get(quick_craft_barrel_pos).getOrDefault(quick_craft_item_name, 0) + total_put_in_via_quick_craft);
		if(StonkCompanionClient.is_verbose_logging && total_put_in_via_quick_craft != 0) StonkCompanionClient.LOGGER.info("Player put " + total_put_in_via_quick_craft + " of " + quick_craft_item_name + " into the barrel.");
			
		active_barrel.onClickActionAdd(quick_craft_item_name.toLowerCase(), total_put_in_via_quick_craft, "", 0);
		active_barrel.validateTransaction();
		
		/*int currency_type = barrel_prices.get(quick_craft_barrel_pos).currency_type;
		String label = barrel_prices.get(quick_craft_barrel_pos).label;
			
		barrel_actions.putIfAbsent(quick_craft_barrel_pos, new double[]{0.0, 0.0});
			
		double[] barrel_actions_change = StonkCompanionClient.barrel_actions.get(quick_craft_barrel_pos);
			
		String taken_item_name_lc = quick_craft_item_name.toLowerCase();
					
		if(currency_type==1 && taken_item_name_lc.equals("hyperexperience")) {
			barrel_actions_change[1] += 64*total_put_in_via_quick_craft;
		}else if(currency_type==1 && taken_item_name_lc.equals("concentrated experience")) {
			barrel_actions_change[1] += total_put_in_via_quick_craft;
		}else if(currency_type==1 && taken_item_name_lc.equals("experience bottle")) {
			barrel_actions_change[1] += (double)(total_put_in_via_quick_craft)/8.0;
		}else if(currency_type==2 && taken_item_name_lc.equals("hyper crystalline shard")) {
			barrel_actions_change[1] += 64*total_put_in_via_quick_craft;
		}else if(currency_type==2 && taken_item_name_lc.equals("compressed crystalline shard")) {
			barrel_actions_change[1] += total_put_in_via_quick_craft;
		}else if(currency_type==2 && taken_item_name_lc.equals("crystalline shard")) {
			barrel_actions_change[1] += (double)(total_put_in_via_quick_craft)/8.0;
		}else if(currency_type==3 && taken_item_name_lc.equals("hyperchromatic archos ring")) {
			barrel_actions_change[1] += 64*total_put_in_via_quick_craft;
		}else if(currency_type==3 && taken_item_name_lc.equals("archos ring")) {
			barrel_actions_change[1] += total_put_in_via_quick_craft;
		}else {
			if(label.toLowerCase().startsWith("64x") || label.toLowerCase().contains("stack")) {
				barrel_actions_change[0] += (double)(total_put_in_via_quick_craft)/64.0;
			}else {
				barrel_actions_change[0] += total_put_in_via_quick_craft;
			}
		}
				
		Barrel traded_barrel = StonkCompanionClient.barrel_prices.get(quick_craft_barrel_pos);
		double other_items = StonkCompanionClient.barrel_actions.get(quick_craft_barrel_pos)[0];
		double actual_compressed = StonkCompanionClient.barrel_actions.get(quick_craft_barrel_pos)[1];
			
		double expected_compressed = (other_items < 0) ? Math.abs(other_items)*traded_barrel.compressed_ask_price : -1*other_items*traded_barrel.compressed_bid_price;
		
		double currency_delta = expected_compressed - actual_compressed;
			    
		String currency_str = StonkCompanionClient.currency_type_to_compressed_text.get(traded_barrel.currency_type);
		String hyper_str = StonkCompanionClient.currency_type_to_hyper_text.get(traded_barrel.currency_type);
		    
		// Bounds check.
		if(currency_delta < 0.0005 && currency_delta > -0.0005) currency_delta = 0;

		if(currency_delta == 0) {
		  	StonkCompanionClient.barrel_transaction_validity.put(quick_craft_barrel_pos, true);
		   	StonkCompanionClient.barrel_transaction_solution.remove(quick_craft_barrel_pos);
		}else if(currency_delta != 0) {
		  	StonkCompanionClient.barrel_transaction_validity.put(quick_craft_barrel_pos, false);	
		   	double abs_currency_delta = Math.abs(currency_delta);
		  	// TODO: Turn this into an array of two strings.
		   	if(StonkCompanionClient.is_compressed_only) {
		    	StonkCompanionClient.barrel_transaction_solution.put(quick_craft_barrel_pos, "%s %s %s".formatted(currency_delta<0 ? "Take" : "Add", StonkCompanionClient.df1.format(Math.abs(currency_delta)), currency_str));	
		   	}else {
		    	StonkCompanionClient.barrel_transaction_solution.put(quick_craft_barrel_pos, "%s %d %s %s %s".formatted(currency_delta<0 ? "Take" : "Add", (int)(abs_currency_delta/64), hyper_str, StonkCompanionClient.df1.format(abs_currency_delta%64), currency_str));
		   	}
		}*/
	}
	
	// @SuppressWarnings("resource")
	@Override
	public void onInitializeClient() {

		if (!Files.isDirectory(FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion"))) {		
			try {
				Files.createDirectory(FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion"));
			} catch (IOException e) {
				
				LOGGER.error("StonkCompanion failed to create the StonkCompanion config directory!");
			}
		}else {
			readConfig();
		}
		
		ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> {
			writeConfig();
			writeCheckpoints();
		});
		
		ClientTickEvents.START_CLIENT_TICK.register((client) -> {
			
			if(is_quick_crafting) {
				
				if(time_since_start_of_quick_craft >= max_time_for_quick_craft ) {
					// Do logic
					
					quickCraftManagement();
					
					is_quick_crafting = false;
					time_since_start_of_quick_craft = 0;
					quick_craft_slot_qty.clear();
					quick_craft_item_name = "";
					quick_craft_item_qty = 0;
					quick_craft_item_max_stack = 0;
					quick_craft_barrel_pos = "";
					quick_craft_in_player_inv = 0;
					quick_craft_itemstk = null;
					quick_craft_button = -1;
					
				}else {
					time_since_start_of_quick_craft++;
				}
			}
			
			// Every tick increase the lifetime of every barrel and then check if it has exceeded it's lifetime.
			for (String pos : barrel_prices.keySet()) {
				barrel_prices.get(pos).increment_time();
				barrel_prices.get(pos).updateGuiTimestamp();
			}
			
			barrel_prices.entrySet().removeIf(entry -> (entry.getValue().is_time_over()));
			
		});
		
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
	    	
    		if(!world.isClient) return ActionResult.PASS;
    	
    		last_right_click =  hitResult.getBlockPos();
        
    		is_there_barrel_price = false;
    		anti_monu = true;
    		anti_monu_inv_init = true;
    		anti_monu_is_not_barrel = false;
    		barrel_pos_found = "";
    		fairprice_val = 0.0;
    		fairprice_currency_str = "";
    		StonkCompanionClient.action_been_done = false;
    		barrel_changes.clear();

        	return ActionResult.PASS;
    	});
		
		ClientReceiveMessageEvents.MODIFY_GAME.register((message, bool) -> {
			
			if(!change_coreprotect) {
				return message;
			}
			
			String raw_msg = message.toString();
			
			if(!((raw_msg.contains("§c-") || raw_msg.contains("§a+")) && (raw_msg.contains("added")||raw_msg.contains("removed")) && !raw_msg.contains("-----") && !raw_msg.contains("§m"))) {
				return message;
			}
			
			List<Text> parts_of_msg = message.getSiblings();

			List<Text> item_interaction = parts_of_msg.get(3).getSiblings();

			String name_item = "";

			if(item_interaction.size() != 0) {
				
				HoverEvent item_transaction = parts_of_msg.get(3).getStyle().getHoverEvent();		
				
				String temp_name_item = item_transaction.getValue(HoverEvent.Action.SHOW_TEXT).toString();

				// written_books or writable_books have hoverevent, but it doesn't show anything? So just check if it starts with a literal anyway.
				// Since it is either not something we care about or it is not in the expected format.
				if (!temp_name_item.startsWith("literal{")) return message;
				
				String backup_name_item = temp_name_item;
				temp_name_item = temp_name_item.replace("literal{", "");
				temp_name_item = temp_name_item.substring(0, temp_name_item.indexOf("}"));
				
				if (temp_name_item.equals("Tesseract of Knowledge (u)")) {
					int indx_of_anvil_count = backup_name_item.indexOf("literal{Stored anvils: }");
					   
					if (indx_of_anvil_count != -1) {
						temp_name_item += " (Contains: " + backup_name_item.substring(indx_of_anvil_count+55, backup_name_item.indexOf('}', indx_of_anvil_count+55))+ " Anvils)";
					}
				}				
				   
				name_item = temp_name_item;
				
				// I hate minecraft Text and I hate how seemingly hard it is to find help.
				
				MutableText test = Text.literal(message.getLiteralString() == null ? "" : message.getLiteralString());
				test.setStyle(message.getStyle());
				
				test.append(parts_of_msg.get(0));
				test.append(parts_of_msg.get(1));
				test.append(parts_of_msg.get(2));
				
				MutableText test2 = Text.literal(parts_of_msg.get(3).getLiteralString() == null ? "" : parts_of_msg.get(3).getLiteralString());
				test2.setStyle(parts_of_msg.get(3).getStyle());
				
				MutableText edited_section = Text.literal(name_item);
				edited_section.setStyle(message.getSiblings().get(3).getSiblings().get(0).getStyle());
				
				test2.append(edited_section);
				test.append(test2);
				
				for(int i = 4; i < parts_of_msg.size(); i++) {
					test.append(parts_of_msg.get(i));
				}
				

				return test;
				
			}else {
				return message;
			}
		});
		
	    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("StonkCompanion")
		.then(argument("command", StringArgumentType.string())
				.suggests(StonkCompanionCommandsSuggestions.commandsSUGGESTION_PROVIDER)
	    		.executes(context -> {
	    			String given_command = StringArgumentType.getString(context, "command");
	    			if (given_command.equals("ToggleCoreprotect")){
		    			context.getSource().sendFeedback(Text.literal(change_coreprotect ? "[StonkCompanion] Stopped changing coreprotect." : "[StonkCompanion] Changing coreprotect."));
		    			change_coreprotect = !change_coreprotect;
	    			}else if(given_command.equals("ToggleCheckpointing")) {
		    			context.getSource().sendFeedback(Text.literal(checkpointing ? "[StonkCompanion] Stopped getting checkpoints." : "[StonkCompanion] Getting checkpoints."));
		    			checkpointing = !checkpointing;
		    			if(!checkpointing) {
		    				writeCheckpoints();
		    			}
	    			}else if(given_command.equals("ToggleFairPrice")) {
	    				context.getSource().sendFeedback(Text.literal(fairprice_detection ? "[StonkCompanion] Stopped detecting FairStonk." : "[StonkCompanion] Detecting FairStonk."));
	    				fairprice_detection = !fairprice_detection;
	    			}else if(given_command.equals("ToggleMistradeCheck")) {
	    				context.getSource().sendFeedback(Text.literal(is_mistrade_checking ? "[StonkCompanion] Stopped detecting mistrades." : "[StonkCompanion] Detecting mistrades."));
	    				is_mistrade_checking = !is_mistrade_checking;	
	    			}else if(given_command.equals("MistradeCheck")) {
	    				context.getSource().sendFeedback(Text.literal("[StonkCompanion] Checking transactions..."));
	    				mistradeCheck();
	    			}else if(given_command.equals("ToggleVerboseLogging")) {
	    				context.getSource().sendFeedback(Text.literal(is_verbose_logging ? "[StonkCompanion] Stopped verbose logging." : "[StonkCompanion] Started verbose logging."));
	    				is_verbose_logging = !is_verbose_logging;	
	    			}else if(given_command.equals("ToggleShowingText")) {
	    				context.getSource().sendFeedback(Text.literal(is_showing_text ? "[StonkCompanion] Stopped printing mistrade and fairprice text." : "[StonkCompanion] Will print mistrade and fairprice text."));
	    				is_showing_text = !is_showing_text;	
	    			}else if(given_command.equals("ToggleShowingGui")) {
	    				context.getSource().sendFeedback(Text.literal(is_showing_gui ? "[StonkCompanion] Stopped showing barrel gui." : "[StonkCompanion] Showing barrel gui."));
	    				is_showing_gui = !is_showing_gui;	
	    				
	    			}else if(given_command.equals("ToggleHavingOffhandSwapOn")) {
	    				context.getSource().sendFeedback(Text.literal(has_offhandswap_off ? "[StonkCompanion] Registering that offhand swap is allowed in peb." : "[StonkCompanion] Registering that offhand swap is not allowed in peb."));
	    				has_offhandswap_off = !has_offhandswap_off;	
	    			}else if(given_command.equals("ClearReports")) {
	    				context.getSource().sendFeedback(Text.literal("[StonkCompanion] Clearing all transactions."));
	    				
	    				//barrel_timeout.clear();
	    				//barrel_transactions.clear();
	    				barrel_prices.clear();
	    				//barrel_actions.clear();
	    				//barrel_transaction_solution.clear();
	    				//barrel_transaction_validity.clear();
	    				
	    			}else if(given_command.equals("ToggleCompressed")) {
	    				context.getSource().sendFeedback(Text.literal(is_compressed_only ? "[StonkCompanion] Showing hyper and compressed now." : "[StonkCompanion] Showing only in compressed."));
	    				is_compressed_only = !is_compressed_only;
	    				
	    				for(String barrel_pos : barrel_prices.keySet()) {
	    					
	    					if(barrel_prices.get(barrel_pos) == null) {
	    						barrel_prices.remove(barrel_pos);
	    						continue;
	    					}
	    					
	    					barrel_prices.get(barrel_pos).convertSolutionToCompressed();
	    					
	    					/*if (StonkCompanionClient.barrel_prices.get(barrel_pos) == null) continue;
	    					if (StonkCompanionClient.barrel_actions.get(barrel_pos) == null) continue;
	    					
	    					Barrel traded_barrel = StonkCompanionClient.barrel_prices.get(barrel_pos);
	    					double other_items = StonkCompanionClient.barrel_actions.get(barrel_pos)[0];
	    					double actual_compressed = StonkCompanionClient.barrel_actions.get(barrel_pos)[1];
	    					
	    					double expected_compressed = (other_items < 0) ? Math.abs(other_items)*traded_barrel.compressed_ask_price : -1*other_items*traded_barrel.compressed_bid_price;
	    					
	    				    double currency_delta = expected_compressed - actual_compressed;
	    				    
	    				    String currency_str = StonkCompanionClient.currency_type_to_compressed_text.get(traded_barrel.currency_type);
	    				    String hyper_str = StonkCompanionClient.currency_type_to_hyper_text.get(traded_barrel.currency_type);
	    				    
	    				    // Bounds check.
	    				    if(currency_delta < 0.0005 && currency_delta > -0.0005) currency_delta = 0;
	
	    				    if(currency_delta != 0) {
	    				    	double abs_currency_delta = Math.abs(currency_delta);
	    				    	// TODO: Turn this into an array of two strings.
	    				    	if(StonkCompanionClient.is_compressed_only) {
	    					    	StonkCompanionClient.barrel_transaction_solution.put(barrel_pos, "%s %s %s".formatted(currency_delta<0 ? "Take" : "Add", StonkCompanionClient.df1.format(Math.abs(currency_delta)), currency_str));	
	    				    	}else {
	    					    	StonkCompanionClient.barrel_transaction_solution.put(barrel_pos, "%s %d %s %s %s".formatted(currency_delta<0 ? "Take" : "Add", (int)(abs_currency_delta/64), hyper_str, StonkCompanionClient.df1.format(abs_currency_delta%64), currency_str));
	    				    	}
	    				    }*/
	    				}	    
	    			}
	    			return 1;
	    		}
	    	))));
		
	}

}