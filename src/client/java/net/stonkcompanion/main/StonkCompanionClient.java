package net.stonkcompanion.main;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.stonkcompanion.mixin.client.PlayerListHudAccessor;

public class StonkCompanionClient implements ClientModInitializer{

	public static final String MOD_ID = "stonkcompanion";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static String top_dir = FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion").toString();
	
	// Versioning:
	public static final String current_mod_version = "v0.1.0";
	public static String latest_mod_version = "";
	public static String mininum_mod_version = "";
	public static boolean is_stopping_mistrade_dect = false;
	public static boolean is_latest_version = true;
	
	
	// Coreprotect changes. Like changing the hovertext literal to the monu item name.
	public static boolean change_coreprotect = true;
	
	// Bool
	public static boolean is_verbose_logging = true;
	public static boolean is_showing_text = false;
	public static boolean is_showing_gui = true;
	public static boolean fairprice_detection = true;
	public static boolean is_mistrade_checking = true;
	public static boolean has_offhandswap_off = false;
	
	public static final Map<Integer, String> currency_type_to_compressed_text = Map.of(1, "cxp", 2, "ccs", 3, "ar");
	public static final Map<Integer, String> currency_type_to_hyper_text = Map.of(1, "hxp", 2, "hcs", 3, "har");
	public static final int yellow_color = 0xffffff00;
	public static final int green_color = 0xff00ff00;
	public static final int light_blue_color = 0xff00ffff;
	public static final int coreprotect_gray_color = 0xffaaaaaa;
	public static final MutableText stonk_companion_logo = Text.literal("Stonk").withColor(yellow_color).append(Text.literal("Co").withColor(green_color)).append(Text.literal("mpanion").withColor(light_blue_color));
	
	// In seconds how long verbose logging logs can exist.
	// Set it to a week for rn.
	public static int action_lifetime_seconds = 60*60*24*7;
	public static ArrayList<String> action_buffer = new ArrayList<String>();
	public static String potential_action_log = "";
	
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
	public static boolean did_screen_resize = false;
	public static double fairprice_val = 0.0;
	public static String fairprice_currency_str = "";
	
	// Shamelessly stolen from UMM.
	private static final Pattern shardGetterPattern = Pattern.compile(".*<(?<shard>[-\\w\\d]*)>.*");
	public static String cachedShard = null;
	private static long lastUpdateTimeShard = 0;
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	
	// Mistrade checking :fire:
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
		
		if(s.endsWith("har") || s.endsWith("ar")) {
			return 3;
		}else if(s.endsWith("hcs") || s.endsWith("ccs") || s.endsWith("cs")) {
			return 2;
		}else if(s.endsWith("hxp") || s.endsWith("cxp") || s.endsWith("xp")) {
			return 1;
		}else {
			return 0;
		}
	}
	
	public static void writeInteractionToFile() {
		
		int barrelx = StonkCompanionClient.last_right_click.getX();
		int barrely = StonkCompanionClient.last_right_click.getY();
		int barrelz = StonkCompanionClient.last_right_click.getZ();	
		String barrel_pos = String.format("x%d_y%d_z%d", barrelx, barrely, barrelz);
		
		if(!StonkCompanionClient.action_buffer.isEmpty() && StonkCompanionClient.barrel_prices.containsKey(barrel_pos.replace("_", "/"))) {
			
			// Save all the actions to the file.
			if(Files.isDirectory(FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion/interaction_logs"))) {

				File interaction_log_file = FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion/interaction_logs/"+barrel_pos+".txt").toFile();
				
				if(!interaction_log_file.exists()) {
					try {
						interaction_log_file.createNewFile();
						StonkCompanionClient.action_buffer.add(0,"(Timestamp, Barrel, Inventory Type, Slot Number, Button, Action, In Cursor, Below Cursor)\n");
					} catch (IOException e) {
						// e.printStackTrace();
						StonkCompanionClient.LOGGER.error("StonkCompanioned attempted to create file but failed. " + barrel_pos);
					}
				}
				
				
				if(interaction_log_file.exists()) {
					
					// StonkCompanionClient.LOGGER.info(""+StonkCompanionClient.action_timestamp);
					// StonkCompanionClient.LOGGER.info(StonkCompanionClient.action_buffer);
				
					try (BufferedWriter bw = new BufferedWriter(new FileWriter(interaction_log_file, true))) {
						
						for(String s : StonkCompanionClient.action_buffer) {
							bw.append(s);
						}
						
					} catch (IOException e) {

						StonkCompanionClient.LOGGER.error("Could not find or read json file. " + barrel_pos);

					}
				}
			}			
		}	
		
		StonkCompanionClient.action_buffer.clear();
		
	}
	
	public static int getItemCurrencyType(String _s) {
		String s = _s.toLowerCase().trim();
		
		if(s.equals("hyperchromatic archos ring") || s.equals("archos ring")) {
			return 3;
		}else if(s.equals("hyper crystalline shard") || s.equals("compressed crystalline shard") || s.equals("crystalline shard")) {
			return 2;
		}else if(s.equals("hyperexperience") || s.equals("concentrated experience") || s.equals("experience bottle")) {
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
			mult = 1.0/8.0;
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
									ask_price_compressed = StonkCompanionClient.convertToBaseUnit(_sign_line.substring(find_arrow+2)); // +1 skips arrow, +2 skips space

								} else {
									// This is the two_to_one value
									bid_price_compressed = StonkCompanionClient.convertToBaseUnit(_sign_line.substring(find_arrow+2)); // +1 skips arrow, +2 skips space
								}
							}

						} else {

							if (find_ask != -1) {
								// We now have the line of text with the buy price.
								currency_type = StonkCompanionClient.getCurrencyType(_sign_line);
								
								ask_price_compressed = StonkCompanionClient.convertToBaseUnit(_sign_line.substring(find_ask+7));
								
							} else if(find_bid != -1) {
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
	
	public static void writeConfig() {
		
		JsonObject config_stuff = new JsonObject();		
		
		// Just adding all the config vars.
		config_stuff.addProperty("mistrade_check", is_mistrade_checking);
		config_stuff.addProperty("fairprice_detection", fairprice_detection);
		config_stuff.addProperty("is_compressed_only", is_compressed_only);
		config_stuff.addProperty("is_showing_text", is_showing_text);
		config_stuff.addProperty("is_showing_gui", is_showing_gui);
		config_stuff.addProperty("offhand_swap", has_offhandswap_off);
		config_stuff.addProperty("is_verbose_logging", is_verbose_logging);
		config_stuff.addProperty("change_coreprotect", change_coreprotect);
		config_stuff.addProperty("action_lifetime_seconds", action_lifetime_seconds);
		
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
				
				if(test_obj.has("is_verbose_logging")) {
					is_verbose_logging = test_obj.get("is_verbose_logging").getAsBoolean();
				}
				
				if(test_obj.has("change_coreprotect")) {
					change_coreprotect = test_obj.get("change_coreprotect").getAsBoolean();
				}
				
				if(test_obj.has("action_lifetime_seconds")) {
					action_lifetime_seconds = test_obj.get("action_lifetime_seconds").getAsInt();
				}

			} catch (IOException e) {

				LOGGER.error("Could not find or read json config file.");

			}
		}
	}
	
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
		if(active_barrel.barrel_transactions.get(quick_craft_item_name) == 0) active_barrel.barrel_transactions.remove(quick_craft_item_name);
		
		if(StonkCompanionClient.is_verbose_logging && total_put_in_via_quick_craft != 0) {
			
			if(!StonkCompanionClient.potential_action_log.isEmpty()) StonkCompanionClient.action_buffer.add(StonkCompanionClient.potential_action_log);
			StonkCompanionClient.potential_action_log = "";
			
			String new_interaction = "(%d, \"%s\", %d, \"%s\")\n".formatted(
					Instant.now().getEpochSecond(),
					active_barrel.label,
					total_put_in_via_quick_craft,
					quick_craft_item_name
					);
			
			StonkCompanionClient.action_buffer.add(new_interaction);
			
			// StonkCompanionClient.LOGGER.info("Player put " + total_put_in_via_quick_craft + " of " + quick_craft_item_name + " into the barrel.");
		}
			
		active_barrel.onClickActionAdd("", 0, quick_craft_item_name.toLowerCase(), total_put_in_via_quick_craft);
		active_barrel.validateTransaction();
	
	}
	
	// Go through every verbose log file on start up.
	// Delete all actions that have expired.
	// Delete all files that no longer have actions.
	// Not going to bother with index file atm.
	private void verboseLoggingCleanup() {
		
		if(!is_verbose_logging) return;
		
		File verbose_logging_dir = FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion/interaction_logs").toFile();
		
		ArrayList<File> remove_files = new ArrayList<File>();		
		
		long current_time = Instant.now().getEpochSecond();
		
		if(verbose_logging_dir.listFiles() != null) {
			for(File barrel_verbose_log : verbose_logging_dir.listFiles()) {

				BufferedWriter bw = null;
				File _temp_file = FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion/interaction_logs/temp_log_file").toFile();
				
				try (BufferedReader br = new BufferedReader(new FileReader(barrel_verbose_log))) {
					
					// "(Timestamp, Barrel, Inventory Type, Slot Number, Button, Action, In Cursor, Below Cursor)\n"
					
					String log_line = "";
					boolean found_valid_timestamp = false;
					boolean first_line_check = false;
					boolean skipped_header = false;
					
					while ((log_line = br.readLine()) != null) {
						
						if(!skipped_header) {
							skipped_header = true;
							continue;
						}
						
						if(bw != null && found_valid_timestamp) {
							bw.write(log_line+"\n");
							continue;
						}
						
						String get_log_timestamp = log_line.substring(1, log_line.indexOf(','));
						
						long log_timestamp = -1;
						
						try {
							log_timestamp = Long.parseLong(get_log_timestamp);
						} catch(NumberFormatException e) {
							LOGGER.warn(barrel_verbose_log.getName() + " had an invalid timestamp: " + get_log_timestamp);
							continue;
						}
						
						if(log_timestamp < current_time-action_lifetime_seconds) {
							// This log has expired so we will ignore it.
							if (bw == null) {
								// If the first_line is not expired then early stop.
								first_line_check = true;
								bw = new BufferedWriter(new FileWriter(_temp_file));
							}
							continue;
						}
						
						found_valid_timestamp = true;
						
						if(!first_line_check) {
							// No point in reading and writing if nothing needs to change.
							break;
						}
						
						// Re-add header since file is being rewritten.
						bw.write("(Timestamp, Barrel, Inventory Type, Slot Number, Button, Action, In Cursor, Below Cursor)\n");
						bw.write(log_line+"\n");
						
					}
					
					if(!found_valid_timestamp) {
						remove_files.add(barrel_verbose_log);
					}
					
				} catch (IOException e) {
					
					LOGGER.error("Could not find or read interaction logging file. " + barrel_verbose_log.getName());
					
				}
				
				if (bw != null) {
					// temp file was created and written to and so the current log needs to be replaced.
					try {
						bw.close();
					} catch (IOException e) {
						LOGGER.error("Could not write to file." + barrel_verbose_log.getName());
					}
					barrel_verbose_log.delete();
					_temp_file.renameTo(barrel_verbose_log);
					_temp_file.delete();
				}
			}
			
			for(File empty_file : remove_files) {
				empty_file.delete();
			}
			
			File _temp_file = FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion/interaction_logs/temp_log_file").toFile();
			if(_temp_file.exists()) {
				_temp_file.delete();
			}
		}
		
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
		
		if(!Files.isDirectory(FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion/interaction_logs"))) {
			try {
				Files.createDirectory(FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion/interaction_logs"));
			} catch (IOException e) {
				
				LOGGER.error("StonkCompanion failed to create the interaction_logs config directory!");
			}
		} else {
			verboseLoggingCleanup();
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
				
				if(barrel_prices.get(pos).barrel_transactions.isEmpty()) {
					continue;
				}
				
				barrel_prices.get(pos).incrementTime();
				
				// Only update timestamp gui if there is a barrel open and it is the barrel we are looking at.
				if(!anti_monu_is_not_barrel && pos.equals(barrel_pos_found)) {
					if(barrel_prices.get(pos).isTimeOver()) {
						barrel_prices.get(pos).clearBarrelTransactions();
					}
					barrel_prices.get(pos).updateGuiTimestamp();
				}
			}
			
			barrel_prices.entrySet().removeIf(entry -> (entry.getValue().isTimeOver()));
			
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
				
				// Hard coding this since I hate Text, and this is the only item with this issue atm I think?
				// literal{I}[style={color=#BE93E4,bold}, siblings=[literal{nversion Aegis}[style={color=white}],
				if(temp_name_item.equals("I")) {
					if (backup_name_item.substring(57, 71).equals("nversion Aegis")) {
						temp_name_item = "Inversion Aegis";
					}
				}
				
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
		
	    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> { LiteralCommandNode<FabricClientCommandSource> stonk_companion_node = dispatcher.register(ClientCommandManager.literal("stonkcompanion")
	    .then(ClientCommandManager.literal("showtext")
	    		.executes(context -> {
	    			context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Mistrades and fairprice " + (is_showing_text ? "are" : "are not") +" being printed to chat."));
	    			return 1;
	    		})
	    		.then(ClientCommandManager.literal("toggle")
	    				.executes(context -> {
		    				context.getSource().sendFeedback(Text.literal(is_showing_text ? "§7[§eStonk§aCo§bmpanion§7] Stopped printing mistrade and fairprice text." : "§7[§eStonk§aCo§bmpanion§7] Will print mistrade and fairprice text."));
		    				is_showing_text = !is_showing_text;	
	    					return 1;
	    					})
	    		)
	    		.then(ClientCommandManager.literal("on")
	    				.executes(context -> {
	    					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Will print mistrade and fairprice text."));
	    					is_showing_text = true;	
	    					return 1;
	    				})
	    		)
	    		.then(ClientCommandManager.literal("off")
	    				.executes(context -> {
	    					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Stopped printing mistrade and fairprice text."));
	    					is_showing_text = false;	
	    					return 1;
	    				})
	    		)
	    )
	    .then(ClientCommandManager.literal("clearreports")
	    		.executes(context -> {
	    			context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Clearing all transactions."));
    				barrel_prices.clear();
	    			return 1;
	    		})
	    )
	    .then(ClientCommandManager.literal("showgui")
	    		.executes(context -> {
	    			context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Barrel gui " + (is_showing_gui ? "is" : "is not") +" being shown."));
	    			return 1;
	    		})
	    		.then(ClientCommandManager.literal("toggle")
	    				.executes(context -> {
		    				context.getSource().sendFeedback(Text.literal(is_showing_gui ? "§7[§eStonk§aCo§bmpanion§7] Stopped showing barrel gui." : "§7[§eStonk§aCo§bmpanion§7] Showing barrel gui."));
		    				is_showing_gui = !is_showing_gui;	
	    					return 1;
	    					})
	    		)
	    		.then(ClientCommandManager.literal("on")
	    				.executes(context -> {
	    					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Showing barrel gui."));
	    					is_showing_gui = true;	
	    					return 1;
	    				})
	    		)
	    		.then(ClientCommandManager.literal("off")
	    				.executes(context -> {
	    					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Stopped showing barrel gui."));
	    					is_showing_gui = false;	
	    					return 1;
	    				})
	    		)
	    )
	    .then(ClientCommandManager.literal("compressed")
	    		.executes(context -> {
	    			context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Currency " + (is_showing_text ? "is" : "is not") +" being shown only in compressed."));
	    			return 1;
	    		})
	    		.then(ClientCommandManager.literal("toggle")
	    				.executes(context -> {
		    				context.getSource().sendFeedback(Text.literal(is_compressed_only ? "§7[§eStonk§aCo§bmpanion§7] Showing hyper and compressed now." : "§7[§eStonk§aCo§bmpanion§7] Showing only in compressed."));
		    				is_compressed_only = !is_compressed_only;
		    				
		    				for(String barrel_pos : barrel_prices.keySet()) {
		    					
		    					if(barrel_prices.get(barrel_pos) == null) {
		    						barrel_prices.remove(barrel_pos);
		    						continue;
		    					}
		    					
		    					barrel_prices.get(barrel_pos).convertSolutionToCompressed();

		    				}
	    					return 1;
	    					})
	    		)
	    		.then(ClientCommandManager.literal("on")
	    				.executes(context -> {
	    					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Showing only in compressed."));
	    					
	    					if(is_compressed_only) return 1;
	    					
		    				for(String barrel_pos : barrel_prices.keySet()) {
		    					
		    					if(barrel_prices.get(barrel_pos) == null) {
		    						barrel_prices.remove(barrel_pos);
		    						continue;
		    					}
		    					
		    					barrel_prices.get(barrel_pos).convertSolutionToCompressed();

		    				}
		    				
	    					is_compressed_only = true;	
	    					
	    					return 1;
	    				})
	    		)
	    		.then(ClientCommandManager.literal("off")
	    				.executes(context -> {
	    					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Showing hyper and compressed now."));
	    					
	    					if(!is_compressed_only) return 1;
	    					
		    				for(String barrel_pos : barrel_prices.keySet()) {
		    					
		    					if(barrel_prices.get(barrel_pos) == null) {
		    						barrel_prices.remove(barrel_pos);
		    						continue;
		    					}
		    					
		    					barrel_prices.get(barrel_pos).convertSolutionToCompressed();

		    				}
		    				
	    					is_compressed_only = false;	
	    					
	    					return 1;
	    				})
	    		)
	    )
	    .then(ClientCommandManager.literal("mistrades")
				.executes(context -> {
					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Checking transactions..."));
    				mistradeCheck();
					return 1;
				})
				.then(ClientCommandManager.literal("clear")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Clearing all transactions."));
		    				barrel_prices.clear();
							return 1;
						})
				)
				.then(ClientCommandManager.literal("toggle")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal(is_mistrade_checking ? "§7[§eStonk§aCo§bmpanion§7] Stopped detecting mistrades." : "§7[§eStonk§aCo§bmpanion§7] Detecting mistrades."));
		    				is_mistrade_checking = !is_mistrade_checking;	
							return 1;
						})
				)
				.then(ClientCommandManager.literal("on")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Detecting mistrades."));
		    				is_mistrade_checking = true;	
							return 1;
						})
				)
				.then(ClientCommandManager.literal("off")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Stopped detecting mistrades."));
		    				is_mistrade_checking = false;	
							return 1;
						})
				)
	    )
		.then(ClientCommandManager.literal("checkpoints")
				.executes(context -> {
					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Checkpointing is " + (checkpointing ? "on" : "off") + ". Checkpoints are located in config/StonkCompanion."));
					return 1;
				})
				.then(ClientCommandManager.literal("clear")
						.executes(context -> {
			    			context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Cleared existing checkpoints."));
			    			checkpoints = new JsonObject();
							return 1;
						})
				)
				.then(ClientCommandManager.literal("toggle")
						.executes(context -> {
			    			context.getSource().sendFeedback(Text.literal(checkpointing ? "§7[§eStonk§aCo§bmpanion§7] Stopped getting checkpoints." : "§7[§eStonk§aCo§bmpanion§7] Getting checkpoints."));
			    			checkpointing = !checkpointing;
			    			if(!checkpointing) {
			    				writeCheckpoints();
			    			}
							return 1;
						})
				)
				.then(ClientCommandManager.literal("on")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Getting checkpoints."));
							checkpointing = true;	
							return 1;
						})
				)
				.then(ClientCommandManager.literal("off")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Stopped getting checkpoints."));
			    			if(!checkpointing) {
			    				writeCheckpoints();
			    			}
							checkpointing = false;	
							return 1;
						})
				)
	    )
		.then(ClientCommandManager.literal("coreprotect")
				.executes(context -> {
					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Coreprotect changing is " + (change_coreprotect ? "on" : "off") +"."));
					return 1;
				})
				.then(ClientCommandManager.literal("toggle")
						.executes(context -> {
			    			context.getSource().sendFeedback(Text.literal(change_coreprotect ? "§7[§eStonk§aCo§bmpanion§7] Stopped changing coreprotect." : "§7[§eStonk§aCo§bmpanion§7] Changing coreprotect."));
			    			change_coreprotect = !change_coreprotect;
							return 1;
						})
				)
				.then(ClientCommandManager.literal("on")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Changing coreprotect."));
							change_coreprotect = true;	
							return 1;
						})
				)
				.then(ClientCommandManager.literal("off")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Stopped changing coreprotect."));
			    			change_coreprotect = false;	
							return 1;
						})
				)
	    )
		.then(ClientCommandManager.literal("fairprice")
				.executes(context -> {
					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Fairprice is " + (fairprice_detection ? "on" : "off") +"."));
					return 1;
				})
				.then(ClientCommandManager.literal("toggle")
						.executes(context -> {
		    				context.getSource().sendFeedback(Text.literal(fairprice_detection ? "§7[§eStonk§aCo§bmpanion§7] Stopped detecting Fairprice." : "§7[§eStonk§aCo§bmpanion§7] Detecting Fairprice."));
		    				fairprice_detection = !fairprice_detection;
							return 1;
						})
				)
				.then(ClientCommandManager.literal("on")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Detecting Fairprice."));
							fairprice_detection = true;	
							return 1;
						})
				)
				.then(ClientCommandManager.literal("off")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Stopped detecting Fairprice."));
							fairprice_detection = false;	
							return 1;
						})
				)
	    )
		.then(ClientCommandManager.literal("actionlogs")
				.executes(context -> {
					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Actionlogs are being " + (is_verbose_logging ? "tracked" : "not tracked") +"."));
					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Actionlogs are stored in config/StonkCompanion/interaction_logs."));					
					context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Actionlogs are stored for "+ action_lifetime_seconds/60/60/24 +" days before being pruned."));
					return 1;
				})
				.then(ClientCommandManager.literal("toggle")
						.executes(context -> {
		    				context.getSource().sendFeedback(Text.literal(is_verbose_logging ? "§7[§eStonk§aCo§bmpanion§7] Stopped actionlog tracking." : "§7[§eStonk§aCo§bmpanion§7] Started actionlog tracking."));
		    				is_verbose_logging = !is_verbose_logging;	
							return 1;
						})
				)
				.then(ClientCommandManager.literal("on")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Started actionlog tracking."));
							is_verbose_logging = true;	
							return 1;
						})
				)
				.then(ClientCommandManager.literal("off")
						.executes(context -> {
							context.getSource().sendFeedback(Text.literal("§7[§eStonk§aCo§bmpanion§7] Stopped actionlog tracking."));
							is_verbose_logging = false;	
							return 1;
						})
				)
				.then(ClientCommandManager.literal("max_age")
						.then(argument("given_days", IntegerArgumentType.integer(1, 30)))
							.executes(context -> {
								int given_days = IntegerArgumentType.getInteger(context, "given_days");
								action_lifetime_seconds = given_days*24*60*60;	
								return 1;
							})
				)
	    )
		);
	    dispatcher.register(ClientCommandManager.literal("sc").redirect(stonk_companion_node));
	    });		
	}

}