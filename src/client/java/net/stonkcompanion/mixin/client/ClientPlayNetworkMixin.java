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
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.stonkcompanion.main.StonkCompanionClient;
import net.stonkcompanion.main.Barrel;
import net.stonkcompanion.main.ForexBarrel;
import net.stonkcompanion.main.StonkBarrel;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkMixin {
	
	@Inject(at = @At(value = "TAIL"), method = "onScreenHandlerSlotUpdate(Lnet/minecraft/network/packet/s2c/play/ScreenHandlerSlotUpdateS2CPacket;)V", cancellable = true)
	private void onScreenHandlerSlotUpdatePacket(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
				
		//StonkCompanionClient.LOGGER.error("ScreenHandlerSlotUpdatePacket");
		
		MinecraftClient mc = MinecraftClient.getInstance();
		
        // Screen screen = mc.currentScreen;
        ClientPlayerEntity playerEntity = mc.player;
        
        // if(playerEntity == null) return;
        // if(screen != null) StonkCompanionClient.LOGGER.error(screen.getClass().getCanonicalName());
        if (true) {
            // ItemStack itemStack = packet.getStack();
            int i = packet.getSlot();
            //StonkCompanionClient.LOGGER.error("Doing something?: " + itemStack.getName().getString() + " Slot: " + i);
            if (packet.getSyncId() == ScreenHandlerSlotUpdateS2CPacket.UPDATE_CURSOR_SYNC_ID) {
            	//StonkCompanionClient.LOGGER.error("Setting cursor stack: " + itemStack.getName().getString());
            } else if (packet.getSyncId() == ScreenHandlerSlotUpdateS2CPacket.UPDATE_PLAYER_INVENTORY_SYNC_ID) {
            	//StonkCompanionClient.LOGGER.error("Setting playerInventory stack: " + itemStack.getName().getString() + " Slot: " + i);
            } else {
                if (packet.getSyncId() == 0 && PlayerScreenHandler.isInHotbar((int)i)) {
                    
                	//StonkCompanionClient.LOGGER.error("Changing hotbar stack?: " + itemStack.getName().getString() + " Slot: " + i + " packet revision?: " + packet.getRevision());

                } else if (!(packet.getSyncId() != playerEntity.currentScreenHandler.syncId || packet.getSyncId() == 0)) {
                	
                	//StonkCompanionClient.LOGGER.error("Changing screen slot?: " + itemStack.getName().getString() + " Slot: " + i + " packet revision?: " + packet.getRevision());
            
                }
            }
        }
	}
	
	
	private void checkIfDesync(List<Slot> list_of_slots) {
		
		if(!StonkCompanionClient.barrel_prices.containsKey(StonkCompanionClient.barrel_pos_found)) return;
		
		for(int i = 0; i < list_of_slots.size(); i++) {
			
			if(!StonkCompanionClient.barrel_changes.containsKey(i)) continue;
			if(StonkCompanionClient.barrel_changes.get(i) == null && !list_of_slots.get(i).hasStack()) continue;
			
			if (StonkCompanionClient.barrel_changes.get(i) == null || !ItemStack.areEqual(list_of_slots.get(i).getStack(), StonkCompanionClient.barrel_changes.get(i))) {
				// Revert previous action.
				
				//onClickActionAdd(StonkCompanionClient.barrel_pos_found, StonkCompanionClient.previous_action_name_put, StonkCompanionClient.previous_action_qty_put, StonkCompanionClient.previous_action_name_take, StonkCompanionClient.previous_action_qty_take);
				//onClickActionMistradeCheck(StonkCompanionClient.barrel_pos_found);
				
				//if(StonkCompanionClient.previous_action_qty_put != 0) StonkCompanionClient.barrel_transactions.get(StonkCompanionClient.barrel_pos_found).put(StonkCompanionClient.previous_action_name_put, StonkCompanionClient.barrel_transactions.get(StonkCompanionClient.barrel_pos_found).getOrDefault(StonkCompanionClient.previous_action_name_put, 0) - StonkCompanionClient.previous_action_qty_put);
				//if(StonkCompanionClient.previous_action_qty_take != 0) StonkCompanionClient.barrel_transactions.get(StonkCompanionClient.barrel_pos_found).put(StonkCompanionClient.previous_action_name_take, StonkCompanionClient.barrel_transactions.get(StonkCompanionClient.barrel_pos_found).getOrDefault(StonkCompanionClient.previous_action_name_take, 0) + StonkCompanionClient.previous_action_qty_take);
				
				Barrel active_barrel = StonkCompanionClient.barrel_prices.get(StonkCompanionClient.barrel_pos_found);
				
				StonkCompanionClient.LOGGER.error("DESYNC DETECTED IN " + StonkCompanionClient.barrel_pos_found + ".");
				if(active_barrel.previous_action_qty_put != 0) StonkCompanionClient.LOGGER.info("Prev put: x" + active_barrel.previous_action_qty_put + " " + active_barrel.previous_action_name_put);
				if(active_barrel.previous_action_qty_take != 0) StonkCompanionClient.LOGGER.info("Prev take: x" + active_barrel.previous_action_qty_take + " " + active_barrel.previous_action_name_take);
				active_barrel.previous_action_qty_put = 0;
				active_barrel.previous_action_name_put = "";
				active_barrel.previous_action_qty_take = 0;
				active_barrel.previous_action_name_take = "";
				StonkCompanionClient.action_been_done = false;
				StonkCompanionClient.barrel_changes.clear();				
				return;
			}
		}
		
	}
	
	/*private void onClickActionMistradeCheck(String barrel_pos) {
		
		if (StonkCompanionClient.barrel_prices.get(barrel_pos) == null) return;
		if (StonkCompanionClient.barrel_actions.get(barrel_pos) == null) return;
		
		Barrel traded_barrel = StonkCompanionClient.barrel_prices.get(barrel_pos);
		double other_items = StonkCompanionClient.barrel_actions.get(barrel_pos)[0];
		double actual_compressed = StonkCompanionClient.barrel_actions.get(barrel_pos)[1];
		
		double expected_compressed = (other_items < 0) ? Math.abs(other_items)*traded_barrel.compressed_ask_price : -1*other_items*traded_barrel.compressed_bid_price;
		
	    double currency_delta = expected_compressed - actual_compressed;
	    
	    String currency_str = StonkCompanionClient.currency_type_to_compressed_text.get(traded_barrel.currency_type);
	    String hyper_str = StonkCompanionClient.currency_type_to_hyper_text.get(traded_barrel.currency_type);
	    
	    // Bounds check.
	    if(currency_delta < 0.0005 && currency_delta > -0.0005) currency_delta = 0;

	    if(currency_delta == 0) {
	    	StonkCompanionClient.barrel_transaction_validity.put(barrel_pos, true);
	    	StonkCompanionClient.barrel_transaction_solution.remove(barrel_pos);
	    }else if(currency_delta != 0) {
	    	StonkCompanionClient.barrel_transaction_validity.put(barrel_pos, false);	
	    	double abs_currency_delta = Math.abs(currency_delta);
	    	if(StonkCompanionClient.is_compressed_only) {
		    	StonkCompanionClient.barrel_transaction_solution.put(barrel_pos, "%s %s %s".formatted(currency_delta<0 ? "Take" : "Add", StonkCompanionClient.df1.format(Math.abs(currency_delta)), currency_str));	
	    	}else {
		    	StonkCompanionClient.barrel_transaction_solution.put(barrel_pos, "%s %d %s %s %s".formatted(currency_delta<0 ? "Take" : "Add", (int)(abs_currency_delta/64), hyper_str, StonkCompanionClient.df1.format(abs_currency_delta%64), currency_str));
	    	}
	    }
	}*/
	
	/*private void onClickActionAdd(String barrel_pos, String taken_item_name, int item_qty_taken, String put_item_name, int item_qty_put) {
		
		if (StonkCompanionClient.barrel_prices.get(barrel_pos) == null) return;
		
		int currency_type = StonkCompanionClient.barrel_prices.get(barrel_pos).currency_type;
		String label = StonkCompanionClient.barrel_prices.get(barrel_pos).label;
		
		if(StonkCompanionClient.barrel_actions.get(barrel_pos) == null) {
			StonkCompanionClient.barrel_actions.put(barrel_pos, new double[]{0.0, 0.0});
		}
		
		double[] barrel_actions = StonkCompanionClient.barrel_actions.get(barrel_pos);
		
		if(item_qty_taken != 0) {
			String taken_item_name_lc = taken_item_name.toLowerCase();
			
			if(currency_type==1 && taken_item_name_lc.equals("hyperexperience")) {
				barrel_actions[1] -= 64*item_qty_taken;
			}else if(currency_type==1 && taken_item_name_lc.equals("concentrated experience")) {
				barrel_actions[1] -= item_qty_taken;
			}else if(currency_type==1 && taken_item_name_lc.equals("experience bottle")) {
				barrel_actions[1] -= (double)(item_qty_taken)/8.0;
			}else if(currency_type==2 && taken_item_name_lc.equals("hyper crystalline shard")) {
				barrel_actions[1] -= 64*item_qty_taken;
			}else if(currency_type==2 && taken_item_name_lc.equals("compressed crystalline shard")) {
				barrel_actions[1] -= item_qty_taken;
			}else if(currency_type==2 && taken_item_name_lc.equals("crystalline shard")) {
				barrel_actions[1] -= (double)(item_qty_taken)/8.0;
			}else if(currency_type==3 && taken_item_name_lc.equals("hyperchromatic archos ring")) {
				barrel_actions[1] -= 64*item_qty_taken;
			}else if(currency_type==3 && taken_item_name_lc.equals("archos ring")) {
				barrel_actions[1] -= item_qty_taken;
			}else {
				
				if(label.toLowerCase().startsWith("64x") || label.toLowerCase().contains("stack")) {
					barrel_actions[0] -= (double)(item_qty_taken)/64.0;
				}else {
					barrel_actions[0] -= item_qty_taken;
				}
			}
		}
		
		if(item_qty_put != 0) {
			String taken_item_name_lc = put_item_name.toLowerCase();
					
			if(currency_type==1 && taken_item_name_lc.equals("hyperexperience")) {
				barrel_actions[1] += 64*item_qty_put;
			}else if(currency_type==1 && taken_item_name_lc.equals("concentrated experience")) {
				barrel_actions[1] += item_qty_put;
			}else if(currency_type==1 && taken_item_name_lc.equals("experience bottle")) {
				barrel_actions[1] += (double)(item_qty_put)/8.0;
			}else if(currency_type==2 && taken_item_name_lc.equals("hyper crystalline shard")) {
				barrel_actions[1] += 64*item_qty_put;
			}else if(currency_type==2 && taken_item_name_lc.equals("compressed crystalline shard")) {
				barrel_actions[1] += item_qty_put;
			}else if(currency_type==2 && taken_item_name_lc.equals("crystalline shard")) {
				barrel_actions[1] += (double)(item_qty_put)/8.0;
			}else if(currency_type==3 && taken_item_name_lc.equals("hyperchromatic archos ring")) {
				barrel_actions[1] += 64*item_qty_put;
			}else if(currency_type==3 && taken_item_name_lc.equals("archos ring")) {
				barrel_actions[1] += item_qty_put;
			}else {
				
				if(label.toLowerCase().startsWith("64x") || label.toLowerCase().contains("stack")) {
					barrel_actions[0] += (double)(item_qty_put)/64.0;
				}else {
					barrel_actions[0] += item_qty_put;
				}
			}
		}
		
	}*/
	

	@Inject(at = @At(value = "TAIL"), method = "onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V", cancellable = true)
	private void onInventoryPKT(InventoryS2CPacket packet, CallbackInfo ci) {

		//StonkCompanionClient.LOGGER.error("InventoryS2CPacket recieved");
		
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
		// Check for if you are within a certain bounds. Currently don't care about that.
		/*if(StonkCompanionClient.nw_coords[0] > barrelx
				   || StonkCompanionClient.se_coords[0] < barrelx
				   || StonkCompanionClient.nw_coords[1] > barrelz
				   || StonkCompanionClient.se_coords[1] < barrelz) {
			return;	
		}*/
		
		/*if(StonkCompanionClient.is_mistrade_checking) {
			
			String[] fair_price_results = StonkCompanionClient.detectFairPrice(list_of_slots);
			
			if (fair_price_results == null || !StonkCompanionClient.fairprice_detection) {
				StonkCompanionClient.fairprice_currency_str = "N/A";
				StonkCompanionClient.fairprice_val = 0.0;
			}else {
				double interpolated_price = Double.parseDouble(fair_price_results[0]);
				int currency_type = (int)Double.parseDouble(fair_price_results[1]);
				
				if(currency_type > 0) {		
					StonkCompanionClient.fairprice_currency_str = StonkCompanionClient.currency_type_to_compressed_text.get(currency_type);
		
				    StonkCompanionClient.fairprice_val = interpolated_price;
				}else if(currency_type < 0) {
					
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

				    StonkCompanionClient.fairprice_currency_str = String.format("1 %s -> %s %s|1 %s -> %s %s",
				    	currency_one,
				    	StonkCompanionClient.df1.format(interpolated_price), 
				    	currency_two,
				    	currency_two,
				    	StonkCompanionClient.df1.format(1.0/interpolated_price), 
				    	currency_one);
				    
				    StonkCompanionClient.fairprice_val = -1;
				    
				}
			}
		}*/
		
		if(StonkCompanionClient.is_mistrade_checking) {
			
			int barrelx = StonkCompanionClient.last_right_click.getX();
			int barrely = StonkCompanionClient.last_right_click.getY();
			int barrelz = StonkCompanionClient.last_right_click.getZ();	
			String barrel_pos = String.format("x%d/y%d/z%d", barrelx, barrely, barrelz);
					
			int currency_type = -1;
			String label = "";
			// First line in a barrel. For stonk that is ask, for Forex that is curr 1
			String ask_price = "";
			String bid_price = "";
			double ask_price_compressed = -1;
			double bid_price_compressed = -1;
			boolean is_forex = false;
			
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
						if (!item.getNbt().contains("BlockEntityTag") || !item.getNbt().getCompound("BlockEntityTag").contains("back_text") || !item.getNbt().getCompound("BlockEntityTag").getCompound("back_text").contains("messages")) {
							continue;
						}
						if (!item.getNbt().contains("BlockEntityTag") || !item.getNbt().getCompound("BlockEntityTag").contains("front_text") || !item.getNbt().getCompound("BlockEntityTag").getCompound("front_text").contains("messages")) {
							continue;
						}
						
						NbtList sign_info = item.getNbt().getCompound("BlockEntityTag").getCompound("front_text").getList("messages", NbtElement.STRING_TYPE);
						
						if(sign_info.size() > 2 && sign_info.get(0) != null) {				
							if(sign_info.get(0).asString().contains("\"text\"")) {
								label = sign_info.get(0).asString().replace("{\"text\":\"", "").replace("\"}", "").trim();
							}else {
								label = sign_info.get(0).asString().replace("\"", "").trim();
							}
						}
						
						for(NbtElement _e : sign_info) {
							
							String _sign_line = _e.asString().toLowerCase();
							
							if(_sign_line.contains("\"text\"")) {
								_sign_line = _sign_line.replace("{\"text\":\"", "").replace("\"}", "").trim();
							}else {
								_sign_line = _sign_line.replace("\"", "").trim();
							}	
							
							int find_ask = _sign_line.indexOf("buy for");
							int find_bid = _sign_line.indexOf("sell for");
							int find_forex = _sign_line.indexOf("currency");
							int find_arrow = _sign_line.indexOf("→");
							
							if (find_forex != -1) {
								// Out of a lack of desire to standardize arbitrary forex pairs of any type, I will define them as special cases.
								// If we end up with many special cases, we can figure out a standard.
								is_forex = true;

								if (_sign_line.toLowerCase().contains("r1r2")) {
									currency_type = 1;

								} else if (_sign_line.toLowerCase().contains("r1r3")) {
									currency_type = 2;

								} else if (_sign_line.toLowerCase().contains("r2r3")) {
									currency_type = 3;

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
						if (currency_type != -1 && ask_price_compressed != -1 && bid_price_compressed != -1) {
							break;
						}
					}
				}
			}
			
			// StonkCompanionClient.LOGGER.info("Checked Barrel: " + currency_type + " " + ask_price_compressed + " " + bid_price_compressed);
			
			if (currency_type == -1 || ask_price_compressed == -1 || bid_price_compressed == -1) {
				/*
				 *     rat        _..----.._    _
            	 * 				.'  .--.    "-.(0)_
				 * '-.__.-'"'=:|   ,  _)_ \__ . c\'-..
             	 *    			'''------'---''---'-"
				 */
			}else {	
				// Current assumption. Barrel doesn't change.
				if(!StonkCompanionClient.barrel_prices.containsKey(barrel_pos)) {
					
					// StonkCompanionClient.LOGGER.info("Created barrel at " + barrel_pos);
					if(is_forex) {
						StonkCompanionClient.barrel_prices.put(barrel_pos, new ForexBarrel(label, barrel_pos, ask_price, bid_price, ask_price_compressed, bid_price_compressed, currency_type));
					}else {
						StonkCompanionClient.barrel_prices.put(barrel_pos, new StonkBarrel(label, barrel_pos, ask_price, bid_price, ask_price_compressed, bid_price_compressed, currency_type));
					}
					
					/*if(StonkCompanionClient.is_verbose_logging) {
						
						if(StonkCompanionClient.action_timestamp == -1) {
							StonkCompanionClient.action_timestamp = Instant.now().getEpochSecond();
						}
						
						String new_interaction = StonkCompanionClient.barrel_prices.get(barrel_pos).toString() + "\n";
						
						StonkCompanionClient.action_buffer += new_interaction;
						
						// StonkCompanionClient.LOGGER.info(StonkCompanionClient.barrel_prices.get(barrel_pos).toString());
					}*/
					
				}else {
					/*Barrel open_barrel = StonkCompanionClient.barrel_prices.get(barrel_pos);
					switch(open_barrel.barrel_type) {
					case STONK:
						StonkBarrel closing_stonk_barrel = (StonkBarrel) open_barrel;
						if(closing_stonk_barrel.compressed_ask_price != ask_price_compressed || closing_stonk_barrel.compressed_bid_price != bid_price_compressed) {
								
							closing_stonk_barrel.ask_price = ask_price;
							closing_stonk_barrel.bid_price = bid_price;
							closing_stonk_barrel.compressed_ask_price = ask_price_compressed;
							closing_stonk_barrel.compressed_bid_price = bid_price_compressed;
						}
						break;
					default:
						break;
					}*/
				}
				
				if(StonkCompanionClient.fairprice_detection) {
					StonkCompanionClient.barrel_prices.get(barrel_pos).calulateFairPrice(list_of_slots);
					
					Barrel _barrel = StonkCompanionClient.barrel_prices.get(barrel_pos);
					
					if(!Barrel.current_barrel_category.equals(_barrel.category) || Barrel.current_barrel_number  != _barrel.barrel_number) {
						Barrel.previous_barrel_category = Barrel.current_barrel_category;
						Barrel.previous_barrel_number = Barrel.current_barrel_number;
						Barrel.previous_barrel_fairprice = Barrel.current_barrel_fairprice;
						
						Barrel.current_barrel_category = _barrel.category;
						Barrel.current_barrel_number = _barrel.barrel_number;
						Barrel.current_barrel_fairprice = _barrel.fairprice_dir;
						
						StonkCompanionClient.barrel_prices.get(barrel_pos).calulateFairPrice(list_of_slots);
					}				
					
					
				}else {
					StonkCompanionClient.barrel_prices.get(barrel_pos).generateGuiText();
				}
				
				StonkCompanionClient.barrel_pos_found = barrel_pos;
				// StonkCompanionClient.is_there_barrel_price = true;
				
				if(StonkCompanionClient.action_been_done) checkIfDesync(list_of_slots);
				
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
