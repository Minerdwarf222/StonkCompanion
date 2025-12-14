package net.stonkcompanion.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.gson.JsonObject;

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
import net.stonkcompanion.main.StonkCompanionClient;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

	@Inject(at = @At("HEAD"), method = "close()V")
	private void stonkCompanionOnCloseInject(CallbackInfo info) 
	{
		
		stonkCompanionOnCloseCheck((HandledScreen<?>) (Object) this);
		
	}
	
	private void stonkCompanionOnCloseCheck(HandledScreen<?> screen) 
	{
		
		if(!StonkCompanionClient.checkpointing && !StonkCompanionClient.fairprice_detection) return;
		if(screen.getClass() != GenericContainerScreen.class) return;
		if(!screen.getTitle().getString().equals("Barrel")) return;
		
		//String containerType = screen.getTitle().getString();
		
		ScreenHandler container = screen.getScreenHandler();
		List<Slot> list_of_items = container.slots.stream().filter(slot -> slot.inventory.getClass() != PlayerInventory.class).toList();
		
		if(StonkCompanionClient.checkpointing && StonkCompanionClient.last_right_click != null && StonkCompanionClient.anti_monu && list_of_items.size() == 27) {
			StonkCompanionClient.anti_monu = false;
			stonkCompanionCreateCheckpoint(list_of_items);
		}
		
		if(StonkCompanionClient.fairprice_detection && StonkCompanionClient.getShard().equals("plots") && list_of_items.size() == 27) {
			detectFairPrice(list_of_items);
		}
		
	}
	
	private void detectFairPrice(List<Slot> items) {
		
		int barrel_compressed_currency = 0;
		
		// The assumption is that there is basically just currency and mats in the barrel and 1 sign that says the price.
		int barrel_mats = -1;
		
		int currency_type = -1;
		double ask_price_compressed = -1;
		double bid_price_compressed = -1;
		
		// Assumed it is a barrel or chest so check 27 slots. First pass is to check for sign.
		for(int i = 0; i < 27; i++) {
			
			if(!items.get(i).hasStack()) continue;
			
			ItemStack item = items.get(i).getStack();
			
			String item_name = "";
			
			if(item.getNbt() == null || !item.getNbt().contains("Monumenta")) {
				item_name = item.getItem().getName().getString();
				
				if(item_name.toLowerCase().endsWith("sign")) {
					// Okay we have a sign. Now to look to see if it has buy sell on it.
					if (!item.getNbt().contains("plain") || !item.getNbt().getCompound("plain").contains("display") || !item.getNbt().getCompound("plain").getCompound("display").contains("Lore")) {
						return;
					}
					
					NbtList sign_info = item.getNbt().getCompound("plain").getCompound("display").getList("Lore", NbtElement.STRING_TYPE);
					
					for(NbtElement _e : sign_info) {
						
						String _sign_line = _e.asString().toLowerCase();
						
						int find_ask = _sign_line.indexOf("buy for");
						int find_bid = _sign_line.indexOf("sell for");
						
						if (find_ask != -1) {
							
							// We now have the line of text with the buy price.
							currency_type = StonkCompanionClient.getCurrencyType(_sign_line);
							
							ask_price_compressed = StonkCompanionClient.convertToBaseUnit(_sign_line.substring(find_ask+7));
							
						}else if(find_bid != -1) {
							
							// We now have the line of text with the sell price.
							currency_type = StonkCompanionClient.getCurrencyType(_sign_line);
							bid_price_compressed = StonkCompanionClient.convertToBaseUnit(_sign_line.substring(find_bid+8));
						}
						
					}
				}
			}
		}
		
		if (currency_type == -1 || ask_price_compressed == -1 || bid_price_compressed == -1) {
			return;
		}
		
		for(int i = 0; i < 27; i++) {
			
			if(!items.get(i).hasStack()) continue;
			
			ItemStack item = items.get(i).getStack();
			
			String item_name = "";
			int item_qty = item.getCount();	
			
			if(item.getNbt() == null || !item.getNbt().contains("Monumenta")) {
				item_name = item.getItem().getName().getString();
			}else {
				item_name = item.getNbt().getCompound("plain").getCompound("display").getString("Name");				
			}
			
			double mult = StonkCompanionClient.givenCurrReturnMult(item_name);
			
			if(mult == -1) {
				barrel_mats += item_qty;
			}else {
				barrel_compressed_currency += item_qty * mult;
			}
		}
		
		//To account for the sign since we have 0 clue what the barrel item is actually.
		barrel_mats--;
		
		double spread = ask_price_compressed - bid_price_compressed;
        double mid = bid_price_compressed + spread / 2;
        
        double mats_in_currency = barrel_compressed_currency / mid;
        double effective_mats = barrel_mats + mats_in_currency;
        
        if(effective_mats == 0) {
        	return;
        }
        
        double demand_modifier = mats_in_currency / effective_mats;
        double intraspread_factor = demand_modifier * spread;
        double interpolated_price = bid_price_compressed + intraspread_factor;
        
        String currency_str = "";
        
        if (currency_type == 1) {
        	currency_str = "cxp";
        }else if(currency_type == 2) {
        	currency_str = "ccs";
        }else if(currency_type == 3) {
        	currency_str = "ar";
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        String fairprice_msg = String.format("FairStonk is %.1f %s.", interpolated_price, currency_str);
        mc.player.sendMessage(Text.literal(fairprice_msg));
        
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
		
		// Assumed it is a barrel or chest so check 27 slots.
		for(int i = 0; i < 27; i++) {
			
			if(!items.get(i).hasStack()) continue;
			
			JsonObject indx_qtys = new JsonObject();
			
			ItemStack item = items.get(i).getStack();
			
			String item_name = "";
			int item_qty = item.getCount();	
			
			if(item.getNbt() == null || !item.getNbt().contains("Monumenta")) {
				item_name = item.getItem().getName().getString();
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
			StonkCompanionClient.LOGGER.warn("The barrel at " + String.format("x%d/y%d/z%d", barrelx, barrely, barrelz) + " failed to be checkpointed.");
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
		
		StonkCompanionClient.open_barrel_time = 0;
		StonkCompanionClient.open_barrel_values = "";
	}
	
}
