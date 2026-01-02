package net.stonkcompanion.mixin.client;

import java.time.Instant;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.gson.JsonObject;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.stonkcompanion.main.Barrel;
import net.stonkcompanion.main.StonkCompanionClient;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkMixin {

	@Inject(at = @At(value = "TAIL"), method = "onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V", cancellable = true)
	private void onInventoryPKT(InventoryS2CPacket packet, CallbackInfo ci) {

		StonkCompanionClient.is_there_barrel_price = false;
		if(!StonkCompanionClient.checkpointing && !StonkCompanionClient.is_mistrade_checking) return;
		if(!StonkCompanionClient.getShard().equals("plots")) return;
		if(StonkCompanionClient.last_right_click == null) return;
		if(!StonkCompanionClient.anti_monu) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click) == null) return;
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click).getType() != BlockEntityType.BARREL) return;
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity playerEntity = client.player;
		if (!(packet.getSyncId() != 0 && packet.getSyncId() == playerEntity.currentScreenHandler.syncId)) {
			return;
		}
		
		List<Slot> list_of_slots = playerEntity.currentScreenHandler.slots.stream().filter(slot -> slot.inventory.getClass() != PlayerInventory.class).toList();
		
		if(list_of_slots.size() != 27) return;
		
		// int barrelx = StonkCompanionClient.last_right_click.getX();
		// int barrelz = StonkCompanionClient.last_right_click.getZ();	
		
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
		
		if(StonkCompanionClient.is_mistrade_checking) {
			
			String[] fair_price_results = StonkCompanionClient.detectFairPrice(list_of_slots);
			
			if (fair_price_results == null || !StonkCompanionClient.fairprice_detection) {
				StonkCompanionClient.fairprice_currency_str = "N/A";
				StonkCompanionClient.fairprice_val = 0.0;
			}else {
				double interpolated_price = Double.parseDouble(fair_price_results[0]);
				int currency_type = (int)Double.parseDouble(fair_price_results[1]);
				
				StonkCompanionClient.fairprice_currency_str = StonkCompanionClient.currency_type_to_compressed_text.get(currency_type);
	
			    StonkCompanionClient.fairprice_val = interpolated_price;
			}
		}
		
		if(StonkCompanionClient.is_mistrade_checking) {
			
			int barrelx = StonkCompanionClient.last_right_click.getX();
			int barrely = StonkCompanionClient.last_right_click.getY();
			int barrelz = StonkCompanionClient.last_right_click.getZ();	
			String barrel_pos = String.format("x%d/y%d/z%d", barrelx, barrely, barrelz);
					
			int currency_type = -1;
			String label = "";
			String ask_price = "";
			String bid_price = "";
			double ask_price_compressed = -1;
			double bid_price_compressed = -1;
			
			// Assumed it is a barrel or chest so check 27 slots. First pass is to check for sign.
			for(int i = 0; i < 27; i++) {
				
				if(!list_of_slots.get(i).hasStack()) continue;
				
				ItemStack item = list_of_slots.get(i).getStack();
				
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

			}else {	
				// Current assumption. Barrel doesn't change.
				if(!StonkCompanionClient.barrel_prices.containsKey(barrel_pos)) {
					
					// StonkCompanionClient.LOGGER.info("Created barrel at " + barrel_pos);
					StonkCompanionClient.barrel_prices.put(barrel_pos, new Barrel(label, barrel_pos, ask_price, bid_price, ask_price_compressed, bid_price_compressed, currency_type));
				}
				
				StonkCompanionClient.barrel_pos_found = barrel_pos;
				StonkCompanionClient.is_there_barrel_price = true;
			}
			
		}
		if(StonkCompanionClient.checkpointing) {
				
			StonkCompanionClient.open_barrel_time = Instant.now().getEpochSecond();
				
			//playerEntity.currentScreenHandler.
				
			//System.out.println("Getting open values.");
				
			JsonObject barrel_inventory = new JsonObject();
				
			// Assumed it is a barrel or chest so check 27 slots.
			for(int i = 0; i < 27; i++) {
					
				if(!list_of_slots.get(i).hasStack()) continue;
					
				JsonObject indx_qtys = new JsonObject();
					
				ItemStack item = list_of_slots.get(i).getStack();
					
				String item_name = "";
				int item_qty = item.getCount();	
					
				if(item.getNbt() == null || !item.getNbt().contains("Monumenta")) {
					item_name = item.getItem().getTranslationKey().substring(item.getItem().getTranslationKey().lastIndexOf('.')+1);
					/*StonkCompanionClient.LOGGER.warn("Open: " + item_name);
					StonkCompanionClient.LOGGER.warn("Open: " + item.getItem().getTranslationKey());*/
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
				
			StonkCompanionClient.open_barrel_values = barrel_inventory.toString();
		}
	}
}
