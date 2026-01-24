package net.stonkcompanion.mixin.client;

import java.util.List;

import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.gson.JsonObject;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.stonkcompanion.main.Barrel;
import net.stonkcompanion.main.StonkCompanionClient;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

	@Inject(at = @At("HEAD"), method = "close()V")
	private void stonkCompanionOnCloseInject(CallbackInfo info) 
	{
		stonkCompanionOnCloseCheck((HandledScreen<?>) (Object) this);
		
	}
	
	@Inject(at = @At("HEAD"), method = "init()V")
	private void stonkCompanionOnInitInject(CallbackInfo info) 
	{
		initSetup((HandledScreen<?>) (Object) this);
	}
	
	private void initSetup(HandledScreen<?> screen) {
			
		// Just a wall of guard statements.		
		if(StonkCompanionClient.did_screen_resize) {
			StonkCompanionClient.did_screen_resize = false;
			return;
		}
		if(!StonkCompanionClient.checkpointing && !StonkCompanionClient.fairprice_detection && !StonkCompanionClient.is_mistrade_checking) return;	
		if(!StonkCompanionClient.getShard().equals("plots")) return;
		
		StonkCompanionClient.previous_action_qty_put = 0;
		StonkCompanionClient.previous_action_name_put = "";
		StonkCompanionClient.previous_action_qty_take = 0;
		StonkCompanionClient.previous_action_name_take = "";
		StonkCompanionClient.action_been_done = false;
		
		if(screen.getClass() != GenericContainerScreen.class) {
			if(!StonkCompanionClient.anti_monu && !StonkCompanionClient.anti_monu_inv_init) return;
			initHelper();
			return;
		}
		if(!StonkCompanionClient.anti_monu) return;
		if(!StonkCompanionClient.anti_monu_inv_init) {
			// Here a screen opened w/o the previous screen closing. This is likely Rem or the like.
			// So we should run mistradecheck if need be.
			initHelper();
			return;
		}
		StonkCompanionClient.anti_monu_inv_init = false;
		if(StonkCompanionClient.last_right_click == null) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click) == null) return;
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click).getType() != BlockEntityType.BARREL) return;
		
		// At this point we know we are looking, it is plots, it is a generic container, and we had likely just clicked a barrel.
		
		// Here's the thought. Detecting when a barrel is open vs Rem etc is open.
		// Since when those are opened while barrel is open the barrel close function doesn't get called.
		// Problem is since close doesn't get ran, we can't look at the final state of the barrel.
		// The solution. For mistrade check we only care about the sign and we can check for sign on first packet sent.
		//		For any issues with that we can just don't care, user fault.
		//		For fairprice and checkpointing we can just have them not run. Again user fault.
		//		Better to not run then to faultly run.
		
	}
	
	private void initHelper() {
		StonkCompanionClient.anti_monu_is_not_barrel = true;
		StonkCompanionClient.anti_monu = false;
		StonkCompanionClient.is_there_barrel_price = false;
		StonkCompanionClient.barrel_pos_found = "";
		if(StonkCompanionClient.last_right_click == null) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click) == null) return;
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click).getType() != BlockEntityType.BARREL) return;
		
		int barrelx = StonkCompanionClient.last_right_click.getX();
		int barrely = StonkCompanionClient.last_right_click.getY();
		int barrelz = StonkCompanionClient.last_right_click.getZ();	
		String barrel_pos = String.format("x%d/y%d/z%d", barrelx, barrely, barrelz);
		
		if(!StonkCompanionClient.barrel_transactions.containsKey(barrel_pos)) {
			StonkCompanionClient.barrel_prices.remove(barrel_pos);
			return;
		}
		
		StonkCompanionClient.mistradeCheck(barrel_pos, false);
	}
	
	private void stonkCompanionOnCloseCheck(HandledScreen<?> screen) 
	{
		
		if(!StonkCompanionClient.checkpointing && !StonkCompanionClient.fairprice_detection && !StonkCompanionClient.is_mistrade_checking) return;
		if(!StonkCompanionClient.getShard().equals("plots")) return;
		if(screen.getClass() != GenericContainerScreen.class) return;
		if(!StonkCompanionClient.anti_monu) return;
		StonkCompanionClient.anti_monu = false;
		if(StonkCompanionClient.anti_monu_is_not_barrel) return;
		if(StonkCompanionClient.last_right_click == null) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click) == null) return;
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click).getType() != BlockEntityType.BARREL) return;
		// TODO: Is this really needed and if so what is a better way since this doesn't work.
		// if(!screen.getTitle().getString().equals("Barrel")) return;
		
		//String containerType = screen.getTitle().getString();
		
		ScreenHandler container = screen.getScreenHandler();

		List<Slot> list_of_items = container.slots.stream().filter(slot -> slot.inventory.getClass() != PlayerInventory.class).toList();
		
		if(list_of_items.size() != 27) return;
		
		if(StonkCompanionClient.is_mistrade_checking) {
			handlingMistradesClose(list_of_items);
		}
		
		if(StonkCompanionClient.checkpointing) {
			stonkCompanionCreateCheckpoint(list_of_items);
		}
		
		if(StonkCompanionClient.fairprice_detection) {
			sendFairPriceMessage(list_of_items);
		}
		
	}
	
	private void handlingMistradesClose(List<Slot> items) {
		
		int barrelx = StonkCompanionClient.last_right_click.getX();
		int barrely = StonkCompanionClient.last_right_click.getY();
		int barrelz = StonkCompanionClient.last_right_click.getZ();	
		String barrel_pos = String.format("x%d/y%d/z%d", barrelx, barrely, barrelz);
		
		
		if(!StonkCompanionClient.barrel_transactions.containsKey(barrel_pos)) {
			StonkCompanionClient.barrel_transactions.remove(barrel_pos);
			return;
		};
				
		int currency_type = -1;
		String label = "";
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
					if (!item.getNbt().contains("plain") || !item.getNbt().getCompound("plain").contains("display") || !item.getNbt().getCompound("plain").getCompound("display").contains("Lore")) {
						continue;
					}
					
					NbtList sign_info = item.getNbt().getCompound("plain").getCompound("display").getList("Lore", NbtElement.STRING_TYPE);
					
					if(sign_info.size() > 2 && sign_info.get(1) != null) {					
						label = sign_info.get(1).asString().replace(">", "").trim();
					}
					
					for(NbtElement _e : sign_info) {
						
						String _sign_line = _e.asString().toLowerCase();
						
						int find_ask = _sign_line.indexOf("buy for");
						int find_bid = _sign_line.indexOf("sell for");
						
						if (find_ask != -1) {
							
							ask_price = _sign_line.substring(find_ask).trim();
							
							// We now have the line of text with the buy price.
							currency_type = StonkCompanionClient.getCurrencyType(_sign_line);
							
							ask_price_compressed = StonkCompanionClient.convertToBaseUnit(_sign_line.substring(find_ask+7));
							
						}else if(find_bid != -1) {
							
							bid_price = _sign_line.substring(find_bid).trim();
							
							// We now have the line of text with the sell price.
							currency_type = StonkCompanionClient.getCurrencyType(_sign_line);
							bid_price_compressed = StonkCompanionClient.convertToBaseUnit(_sign_line.substring(find_bid+8));
						}
					}
					
					// Break if we have found the sign. Assuming the first found correctly formatted sign is the correct sign. If it isn't. User error.
					if (currency_type != -1 && ask_price_compressed != -1 && bid_price_compressed != -1) {
						break;
					}
				}
			}
		}
		
		// StonkCompanionClient.LOGGER.info("Checked Barrel: " + currency_type + " " + ask_price_compressed + " " + bid_price_compressed);
		
		if (currency_type == -1 || ask_price_compressed == -1 || bid_price_compressed == -1) {
			return;
		}
		
		// Current assumption. Barrel doesn't change.
		if(!StonkCompanionClient.barrel_prices.containsKey(barrel_pos)) {
			
			// StonkCompanionClient.LOGGER.info("Created barrel at " + barrel_pos);
			StonkCompanionClient.barrel_prices.put(barrel_pos, new Barrel(label, barrel_pos, ask_price, bid_price, ask_price_compressed, bid_price_compressed, currency_type));
		}
		
		StonkCompanionClient.mistradeCheck(barrel_pos, false);
	}
	
	private void sendFairPriceMessage(List<Slot> items) {
		
		String[] fair_price_results = StonkCompanionClient.detectFairPrice(items);
		if (fair_price_results == null) return;
		double interpolated_price = Double.parseDouble(fair_price_results[0]);
		int currency_type = (int)Double.parseDouble(fair_price_results[1]);
		double demand_modifier = Double.parseDouble(fair_price_results[2]);
		String label = fair_price_results[3];
		
	    if (currency_type < 0) {
	    	// forex type
	    	
	    	String currency_one = "";
	    	String currency_two = "";
	    	
		    if (currency_type == -1) {
		    	currency_one = "hxp";
		       	currency_two = "hcs";
		    }else if(currency_type == -2) {
		    	currency_one = "hxp";
		      	currency_two = "har";
		    }else if(currency_type == -3) {
		    	currency_one = "hcs";
		       	currency_two = "har";
		    }

		    MinecraftClient mc = MinecraftClient.getInstance();
		    String fairprice_msg = String.format(
		    	"[StonkCompanion] %s's FairStonk is:\n1 %s -> %s %s\n1 %s -> %s %s", 
		    	StonkCompanionClient.categoreyMaker(label), 
		    	currency_one,
		    	StonkCompanionClient.df1.format(interpolated_price), 
		    	currency_two,
		    	currency_two,
		    	StonkCompanionClient.df1.format(1/interpolated_price),
		    	currency_one

		    );
		    if(StonkCompanionClient.is_showing_text) {
		        if(demand_modifier <= 0.005) {
		        	fairprice_msg = "[StonkCompanion] Look in lower barrel.";
		        }else if(demand_modifier >= 0.995) {
		        	fairprice_msg = "[StonkCompanion] Look in higher barrel.";
		        }
		        
		        mc.player.sendMessage(Text.literal(fairprice_msg));
		    }
	    } else {

		    String currency_str = StonkCompanionClient.currency_type_to_compressed_text.get(currency_type);
		    String hyper_str = StonkCompanionClient.currency_type_to_hyper_text.get(currency_type);

		    if (currency_type == 1) {
		    	hyper_str = "hxp";
		       	currency_str = "cxp";
		    }else if(currency_type == 2) {
		    	hyper_str = "hcs";
		      	currency_str = "ccs";
		    }else if(currency_type == 3) {
		    	hyper_str = "har";
		       	currency_str = "ar";
		    }

			int interpolated_hyper_amount = (int)(Math.floor(Math.abs(interpolated_price)/64));
			double interpolated_compressed_amount = (Math.abs(interpolated_price)%64);	    
		    
		    // TODO: Add label
		    MinecraftClient mc = MinecraftClient.getInstance();
		    String fairprice_msg = String.format("[StonkCompanion] %s's FairStonk is %s %s (%d %s %s %s).", StonkCompanionClient.categoreyMaker(label), StonkCompanionClient.df1.format(interpolated_price), currency_str, interpolated_hyper_amount, hyper_str, StonkCompanionClient.df1.format(interpolated_compressed_amount), currency_str);
		    
		    if(StonkCompanionClient.is_showing_text) {
		        if(demand_modifier <= 0.005) {
		        	fairprice_msg = "[StonkCompanion] Look in lower barrel.";
		        }else if(demand_modifier >= 0.995) {
		        	fairprice_msg = "[StonkCompanion] Look in higher barrel.";
		        }
		        
		        mc.player.sendMessage(Text.literal(fairprice_msg));
		    }
	    }
	}

		
	private void stonkCompanionCreateCheckpoint(List<Slot> items)
	{
					
		int barrelx = StonkCompanionClient.last_right_click.getX();
		int barrely = StonkCompanionClient.last_right_click.getY();
		int barrelz = StonkCompanionClient.last_right_click.getZ();	
		
		// Get all the items
		// First. Sign check!
		// TODO: Handle click-through	
		// Check for if you are within a certain bounds. Currently don't care about that.
		/*if(StonkCompanionClient.nw_coords[0] > barrelx
				   || StonkCompanionClient.se_coords[0] < barrelx
				   || StonkCompanionClient.nw_coords[1] > barrelz
				   || StonkCompanionClient.se_coords[1] < barrelz) {
			return;	
		}*/
		
		JsonObject barrel_inventory = new JsonObject();
		
        MinecraftClient mc = MinecraftClient.getInstance();
		
		// Assumed it is a barrel or chest so check 27 slots.
		for(int i = 0; i < 27; i++) {
			
			if(!items.get(i).hasStack()) continue;
			
			JsonObject indx_qtys = new JsonObject();
			
			ItemStack item = items.get(i).getStack();
			
			String item_name = "";
			int item_qty = item.getCount();	
			
			if(item.getNbt() == null || !item.getNbt().contains("Monumenta")) {
				item_name = item.getItem().getTranslationKey().substring(item.getItem().getTranslationKey().lastIndexOf('.')+1);				
				/*mc.player.sendMessage(Text.literal("Close: " + item_name));
				StonkCompanionClient.LOGGER.warn("Close: " + item_name);
				StonkCompanionClient.LOGGER.warn("Close: " + item.getItem().getTranslationKey());*/
			}else {
				item_name = item.getNbt().getCompound("plain").getCompound("display").getString("Name");				
			}
			
			if (barrel_inventory.has(item_name)){
				barrel_inventory.getAsJsonObject(item_name).addProperty(""+i, item_qty);
			}else {
				indx_qtys.addProperty(""+i, item_qty);
				barrel_inventory.add(item_name, indx_qtys);
			}
		}
		
		if (StonkCompanionClient.open_barrel_values.isBlank() || !barrel_inventory.toString().equals(StonkCompanionClient.open_barrel_values)) {
			StonkCompanionClient.open_barrel_time = 0;
			StonkCompanionClient.open_barrel_values = "";
			mc.player.sendMessage(Text.literal(String.format("[StonkCompanion] The barrel at x%d/y%d/z%d failed to be checkpointed.", barrelx, barrely, barrelz)));
			return;
		}
		
		/*
		 * Used for when we add glowing.
		if(StonkCompanionClient.lite_barrels.containsKey(new BlockPos(barrelx, barrely, barrelz)))
			StonkCompanionClient.lite_barrels.remove(new BlockPos(barrelx, barrely, barrelz));
		*/
		
		// Now we need to store for writing to json file.
		String barrel_pos = String.format("x%d/y%d/z%d", barrelx, barrely, barrelz);
		
		if (StonkCompanionClient.checkpoints.has(barrel_pos)){
			
			if (!StonkCompanionClient.checkpoints.getAsJsonObject(barrel_pos).has(""+StonkCompanionClient.open_barrel_time)) {
				StonkCompanionClient.checkpoints.getAsJsonObject(barrel_pos).addProperty(""+StonkCompanionClient.open_barrel_time, StonkCompanionClient.open_barrel_values);
			}
			
		}else {
			
			JsonObject _chkpt_values = new JsonObject();
			_chkpt_values.addProperty(""+StonkCompanionClient.open_barrel_time, StonkCompanionClient.open_barrel_values);
			
			StonkCompanionClient.checkpoints.add(barrel_pos, _chkpt_values);
		}
		

        mc.player.sendMessage(Text.literal("[StonkCompanion] Grabbed checkpoint for %s.".formatted(barrel_pos)));
		
		StonkCompanionClient.open_barrel_time = 0;
		StonkCompanionClient.open_barrel_values = "";
	}
	
}
