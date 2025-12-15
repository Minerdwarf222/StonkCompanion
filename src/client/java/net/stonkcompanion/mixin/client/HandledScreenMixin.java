package net.stonkcompanion.mixin.client;

import java.util.HashMap;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
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
	
	@Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V")
	private void stonkCompanionOnMouseClickInject(Slot slot, int slot_id, int button, SlotActionType action_type, CallbackInfo info) {
		stonkCompanionMouseClickInjectHelper((HandledScreen<?>) (Object) this, slot, slot_id, button, action_type);
	}
	
	private void stonkCompanionMouseClickInjectHelper(HandledScreen<?> screen, Slot slot, int slot_id, int button, SlotActionType action_type) {
		
		// Just a bunch of guard checks. First do we even care then is this a barrel and lastly are we in plots.
		if(!StonkCompanionClient.is_mistrade_checking) return;
		if(screen.getClass() != GenericContainerScreen.class) return;
		if(!screen.getTitle().getString().equals("Barrel")) return;
		if(!StonkCompanionClient.getShard().equals("plots")) return;
		if(slot == null) return;
		
		// Now that we have this. We need to figure out and log what is going on in the barrel.
		// What we care about is what is being put into and taken out of the barrel.
		// So if it is a barrel slot we care about every action.
		// If it is an inventory slot we only care about a select few actions. Which ones? To do.
		//	I think it is just shift clicking and pickup_all.

		// Here is current plan:
		// First. Ignore dragging as it can be dealt with later.
		// Also ignoring monu items like pcrys/shulkers/etc. because :pain:
		// Actions:
		//	From anywhere:
		//		pickup_all (Pickup all): Starting from Slot ID 0, picks up the same item until the cursor slot is full.
		//			Picks up amt as follows: if cursor slot qty + slot qty leq stack size then entire slot is picked up else the difference of stack size - cursor slot qty is picked up from slot.
		// 	From Player Inv:
		//		quick_move (Shift click): Moves whatever is in that slot into the opened inventory if there is room. :suffer:
		//	For barrel:
		//		pickup, button 0 (Left click):
		//			If nothing is in the cursor slot then the whole stack is picked up.
		//			If a different item is in the cursor slot then the two slots are swapped.
		//			If the same item is in the cursor slot then:
		//				If the inventory slot is full nothing happens
		//				If the inventory slot + cursor slot < stack size then the entire cursor slot is put in.
		//				If the inventory slot + cursor slot > stack size then items are taken from cursor and put into inventory slot till stack size.
		//		pickup, button 1 (Right click):
		//			If nothing is in the cursor slot then Math.ceil(half) is taken from the inventory slot.
		//			If a different item is in the cursor slot then the two slots are swapped.
		//			If the same item is in the cursor slot then 1 is taken from cursor slot and put into inventory slot until inventory slot hits stack size.
		//		quick_move (Shift Click): Moves whatever is in that slot into the player inventory if there is room :suffer:
		//		swap, button 0-8 (hotbar): Swaps whatever is in that inventory slot with the corresponding player hotbar slot.
		//		swap, button 40 (Offhand): Swaps whatever is in that inventory slot with the player's offhand. Monu has a command to disable this.
		//		throw, button 0 (Normal throw): Throws a single item from the inventory slot out.
		//		throw, button 1 (Ctrl throw): Throws the entire inventory slot out.
		//		quick_craft (???): I don't understand this, but sometimes it basically just does pickup left-click inventory slot + cursor slot > stack size does.
		//			:sob: quick_craft is dragging
		
		if(StonkCompanionClient.last_right_click == null) return;
		
		int barrelx = StonkCompanionClient.last_right_click.getX();
		int barrely = StonkCompanionClient.last_right_click.getY();
		int barrelz = StonkCompanionClient.last_right_click.getZ();	
		String barrel_pos = String.format("x%d/y%d/z%d", barrelx, barrely, barrelz);
		
		ItemStack player_itemstk = screen.getScreenHandler().getCursorStack();
		ItemStack active_slot = slot.getStack();
		
		if (player_itemstk == null) return;
		
		String player_item_str = (player_itemstk != null) ? player_itemstk.getTranslationKey() : "None";
		String player_inv = (slot.inventory.getClass() == PlayerInventory.class) ? "Player" : "Not Player";
		
		boolean is_player_inv = slot.inventory.getClass() == PlayerInventory.class;
		
		ScreenHandler container = screen.getScreenHandler();
		List<Slot> list_of_items = container.slots.stream().filter(_slot -> _slot.inventory.getClass() != PlayerInventory.class).toList();
		List<Slot> list_of_player_items = container.slots.stream().filter(_slot -> _slot.inventory.getClass() == PlayerInventory.class).toList();
		
		if (list_of_items.size() != 27) return;
		
		StonkCompanionClient.LOGGER.info(player_inv + " slot. Slot ID: " + slot_id + " Button: " + button + " Action Type: " + action_type.name() + " Player Cursor: " + player_item_str);

		// Ignore the action if it is just two empty stacks.
		if (player_itemstk.isEmpty() && !slot.hasStack()) {
			return;
		}
		
		if(action_type == SlotActionType.PICKUP_ALL) {
			
			Item player_item = player_itemstk.getItem();
			int player_itemstk_qty = player_itemstk.getCount();
			int item_qty_taken = 0;
			
			String player_item_name = "";
			
			if(player_itemstk.getNbt() == null || !player_itemstk.getNbt().contains("Monumenta")) {
				player_item_name = player_itemstk.getItem().getName().getString();
			}else {
				player_item_name = player_itemstk.getNbt().getCompound("plain").getCompound("display").getString("Name");				
			}
			
			for(int i = 0; i < 27; i++) {
				if(player_itemstk_qty >= player_item.getMaxCount())	break;		
				if(!list_of_items.get(i).hasStack()) continue;
				
				ItemStack item = list_of_items.get(i).getStack();
				
				if(player_item.equals(item.getItem())) {
					int item_qty = item.getCount();
					
					int _taken = (player_itemstk_qty+item_qty <= player_item.getMaxCount()) ? item_qty : player_item.getMaxCount() - player_itemstk_qty;
					
					item_qty_taken += _taken;
					player_itemstk_qty += _taken;
				}
			}
			
			StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
			StonkCompanionClient.barrel_transactions.get(barrel_pos).put(player_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(player_item_name, 0) - item_qty_taken);
			
			StonkCompanionClient.LOGGER.info("Player took " + item_qty_taken + " of " + player_item.getName().getString() + " from the barrel.");
			
		}else if(action_type == SlotActionType.QUICK_MOVE) {
			
			if(is_player_inv) {
				
				Item active_slot_item = active_slot.getItem();
				int active_slot_itemstk_qty = active_slot.getCount();
				int item_qty_put = 0;
				
				String active_slot_item_name = "";
				
				if(active_slot.getNbt() == null || !active_slot.getNbt().contains("Monumenta")) {
					active_slot_item_name = active_slot.getItem().getName().getString();
				}else {
					active_slot_item_name = active_slot.getNbt().getCompound("plain").getCompound("display").getString("Name");				
				}
				
				for(Slot _slot : list_of_items) {
					if(active_slot_itemstk_qty <= 0) break;		
					if(!_slot.hasStack()) {
						item_qty_put += active_slot_itemstk_qty;
						break;
					}
					
					ItemStack item = _slot.getStack();
					
					if(active_slot_item.equals(item.getItem())) {
						int item_qty = item.getCount();
						
						int _taken = (active_slot_itemstk_qty+item_qty <= active_slot_item.getMaxCount()) ? active_slot_itemstk_qty : active_slot_item.getMaxCount() - item_qty;
						
						item_qty_put += _taken;
						active_slot_itemstk_qty = active_slot_itemstk_qty - _taken;
					}
				}
				
				StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
				StonkCompanionClient.barrel_transactions.get(barrel_pos).put(active_slot_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(active_slot_item_name, 0) + item_qty_put);
				
				StonkCompanionClient.LOGGER.info("Player put " + item_qty_put + " of " + active_slot_item.getName().getString() + " into the barrel.");
				
			}else {
				
				Item active_slot_item = active_slot.getItem();
				int active_slot_itemstk_qty = active_slot.getCount();
				int item_qty_taken = 0;
				
				String active_slot_item_name = "";
				
				if(active_slot.getNbt() == null || !active_slot.getNbt().contains("Monumenta")) {
					active_slot_item_name = active_slot.getItem().getName().getString();
				}else {
					active_slot_item_name = active_slot.getNbt().getCompound("plain").getCompound("display").getString("Name");				
				}
				
				for(Slot _slot : list_of_player_items) {
					if(active_slot_itemstk_qty <= 0) break;		
					if(!_slot.hasStack()) {
						item_qty_taken += active_slot_itemstk_qty;
						break;
					}
					
					ItemStack item = _slot.getStack();
					
					if(active_slot_item.equals(item.getItem())) {
						int item_qty = item.getCount();
						
						int _taken = (active_slot_itemstk_qty+item_qty <= active_slot_item.getMaxCount()) ? active_slot_itemstk_qty : active_slot_item.getMaxCount() - item_qty;
						
						item_qty_taken += _taken;
						active_slot_itemstk_qty = active_slot_itemstk_qty - _taken;
					}
				}
				
				StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
				StonkCompanionClient.barrel_transactions.get(barrel_pos).put(active_slot_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(active_slot_item_name, 0) - item_qty_taken);
				
				StonkCompanionClient.LOGGER.info("Player took " + item_qty_taken + " of " + active_slot_item.getName().getString() + " from the barrel.");
				
			}
			
		}else if(action_type == SlotActionType.PICKUP && !is_player_inv) {
			
			//		pickup, button 0 (Left click):
			//			If nothing is in the cursor slot then the whole stack is picked up.
			//			If a different item is in the cursor slot then the two slots are swapped.
			//			If the same item is in the cursor slot then:
			//				If the inventory slot is full nothing happens
			//				If the inventory slot + cursor slot < stack size then the entire cursor slot is put in.
			//				If the inventory slot + cursor slot > stack size then items are taken from cursor and put into inventory slot till stack size.
			//			If nothing is in the active slot then the whole cursor stack is put in.

			if (button == 0) {
				
				Item active_slot_item = active_slot.getItem();
				int active_slot_itemstk_qty = active_slot.getCount();
				Item player_item = player_itemstk.getItem();
				int player_itemstk_qty = player_itemstk.getCount();
				
				if (player_itemstk.isEmpty()) {
					
					int item_qty_taken = active_slot_itemstk_qty;
					
					String active_slot_item_name = "";
					
					if(active_slot.getNbt() == null || !active_slot.getNbt().contains("Monumenta")) {
						active_slot_item_name = active_slot.getItem().getName().getString();
					}else {
						active_slot_item_name = active_slot.getNbt().getCompound("plain").getCompound("display").getString("Name");				
					}
					
					StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
					StonkCompanionClient.barrel_transactions.get(barrel_pos).put(active_slot_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(active_slot_item_name, 0) - item_qty_taken);
					
					StonkCompanionClient.LOGGER.info("Player took " + item_qty_taken + " of " + active_slot_item.getName().getString() + " from the barrel.");
					
				}else if(active_slot.isEmpty()) {
					
					int item_qty_put = player_itemstk_qty;

					String player_item_name = "";
					
					if(player_itemstk.getNbt() == null || !player_itemstk.getNbt().contains("Monumenta")) {
						player_item_name = player_itemstk.getItem().getName().getString();
					}else {
						player_item_name = player_itemstk.getNbt().getCompound("plain").getCompound("display").getString("Name");				
					}
					
					StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
					StonkCompanionClient.barrel_transactions.get(barrel_pos).put(player_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(player_item_name, 0) + item_qty_put);
					
					StonkCompanionClient.LOGGER.info("Player put " + item_qty_put + " of " + player_item.getName().getString() + " into the barrel.");
					
				}else if(!player_itemstk.getItem().equals(active_slot.getItem())) {

					int item_qty_taken = active_slot_itemstk_qty;
					int item_qty_put = player_itemstk_qty;
					
					String active_slot_item_name = "";
					
					if(active_slot.getNbt() == null || !active_slot.getNbt().contains("Monumenta")) {
						active_slot_item_name = active_slot.getItem().getName().getString();
					}else {
						active_slot_item_name = active_slot.getNbt().getCompound("plain").getCompound("display").getString("Name");				
					}
					
					String player_item_name = "";
					
					if(player_itemstk.getNbt() == null || !player_itemstk.getNbt().contains("Monumenta")) {
						player_item_name = player_itemstk.getItem().getName().getString();
					}else {
						player_item_name = player_itemstk.getNbt().getCompound("plain").getCompound("display").getString("Name");				
					}
					
					StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
					StonkCompanionClient.barrel_transactions.get(barrel_pos).put(player_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(player_item_name, 0) + item_qty_put);
					StonkCompanionClient.barrel_transactions.get(barrel_pos).put(active_slot_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(active_slot_item_name, 0) - item_qty_taken);
					
					StonkCompanionClient.LOGGER.info("Player took " + item_qty_taken + " of " + active_slot_item.getName().getString() + " from the barrel.");
					StonkCompanionClient.LOGGER.info("Player put " + item_qty_put + " of " + player_item.getName().getString() + " into the barrel.");
					
				}else {
					
					if(active_slot_item.getMaxCount() == active_slot_itemstk_qty) {
						return;
					}else if(active_slot_itemstk_qty + player_itemstk_qty <= active_slot_item.getMaxCount()) {
						
						int item_qty_put = player_itemstk_qty;
											
						String player_item_name = "";
						
						if(player_itemstk.getNbt() == null || !player_itemstk.getNbt().contains("Monumenta")) {
							player_item_name = player_itemstk.getItem().getName().getString();
						}else {
							player_item_name = player_itemstk.getNbt().getCompound("plain").getCompound("display").getString("Name");				
						}
						
						StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
						StonkCompanionClient.barrel_transactions.get(barrel_pos).put(player_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(player_item_name, 0) + item_qty_put);
						
						StonkCompanionClient.LOGGER.info("Player put " + item_qty_put + " of " + player_item.getName().getString() + " into the barrel.");
						
					}else if(active_slot_itemstk_qty + player_itemstk_qty > active_slot_item.getMaxCount()){
						
						int item_qty_put = active_slot_item.getMaxCount() - active_slot_itemstk_qty;

						String player_item_name = "";
						
						if(player_itemstk.getNbt() == null || !player_itemstk.getNbt().contains("Monumenta")) {
							player_item_name = player_itemstk.getItem().getName().getString();
						}else {
							player_item_name = player_itemstk.getNbt().getCompound("plain").getCompound("display").getString("Name");				
						}
						
						StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
						StonkCompanionClient.barrel_transactions.get(barrel_pos).put(player_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(player_item_name, 0) + item_qty_put);
						
						StonkCompanionClient.LOGGER.info("Player put " + item_qty_put + " of " + player_item.getName().getString() + " into the barrel.");
						
					}
					
				}
				
			}else if(button == 1) {
				//		pickup, button 1 (Right click):
				//			If nothing is in the cursor slot then Math.ceil(half) is taken from the inventory slot.
				//			If a different item is in the cursor slot then the two slots are swapped.
				//			If the same item is in the cursor slot then 1 is taken from cursor slot and put into inventory slot until inventory slot hits stack size.
				//			If nothing is in active slot then 1 is taken from cursor slot and put into inventory slot.
				
				Item active_slot_item = active_slot.getItem();
				int active_slot_itemstk_qty = active_slot.getCount();
				Item player_item = player_itemstk.getItem();
				int player_itemstk_qty = player_itemstk.getCount();
				
				if (player_itemstk.isEmpty()) {
					
					int item_qty_taken = (int)(Math.ceil(active_slot_itemstk_qty/2.0));
					
					String active_slot_item_name = "";
					
					if(active_slot.getNbt() == null || !active_slot.getNbt().contains("Monumenta")) {
						active_slot_item_name = active_slot.getItem().getName().getString();
					}else {
						active_slot_item_name = active_slot.getNbt().getCompound("plain").getCompound("display").getString("Name");				
					}
					
					StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
					StonkCompanionClient.barrel_transactions.get(barrel_pos).put(active_slot_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(active_slot_item_name, 0) - item_qty_taken);
					
					
					StonkCompanionClient.LOGGER.info("Player took " + item_qty_taken + " of " + active_slot_item.getName().getString() + " from the barrel.");
					
				}else if(active_slot.isEmpty()) {
					
					int item_qty_put = 1;
					
					String player_item_name = "";
					
					if(player_itemstk.getNbt() == null || !player_itemstk.getNbt().contains("Monumenta")) {
						player_item_name = player_itemstk.getItem().getName().getString();
					}else {
						player_item_name = player_itemstk.getNbt().getCompound("plain").getCompound("display").getString("Name");				
					}
					
					StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
					StonkCompanionClient.barrel_transactions.get(barrel_pos).put(player_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(player_item_name, 0) + item_qty_put);
					
					StonkCompanionClient.LOGGER.info("Player put " + item_qty_put + " of " + player_item.getName().getString() + " into the barrel.");
					
				}else if(!player_itemstk.getItem().equals(active_slot.getItem())) {
					
					int item_qty_taken = active_slot_itemstk_qty;
					int item_qty_put = player_itemstk_qty;
					
					String active_slot_item_name = "";
					
					if(active_slot.getNbt() == null || !active_slot.getNbt().contains("Monumenta")) {
						active_slot_item_name = active_slot.getItem().getName().getString();
					}else {
						active_slot_item_name = active_slot.getNbt().getCompound("plain").getCompound("display").getString("Name");				
					}
					
					String player_item_name = "";
					
					if(player_itemstk.getNbt() == null || !player_itemstk.getNbt().contains("Monumenta")) {
						player_item_name = player_itemstk.getItem().getName().getString();
					}else {
						player_item_name = player_itemstk.getNbt().getCompound("plain").getCompound("display").getString("Name");				
					}
					
					StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
					StonkCompanionClient.barrel_transactions.get(barrel_pos).put(player_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(player_item_name, 0) + item_qty_put);
					StonkCompanionClient.barrel_transactions.get(barrel_pos).put(active_slot_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(active_slot_item_name, 0) - item_qty_taken);
					
					StonkCompanionClient.LOGGER.info("Player took " + item_qty_taken + " of " + active_slot_item.getName().getString() + " from the barrel.");
					StonkCompanionClient.LOGGER.info("Player put " + item_qty_put + " of " + player_item.getName().getString() + " into the barrel.");
					
				}else {
				
					if(active_slot_item.getMaxCount() == active_slot_itemstk_qty) {
						return;
					}
					
					int item_qty_put = 1;
					
					String player_item_name = "";
					
					if(player_itemstk.getNbt() == null || !player_itemstk.getNbt().contains("Monumenta")) {
						player_item_name = player_itemstk.getItem().getName().getString();
					}else {
						player_item_name = player_itemstk.getNbt().getCompound("plain").getCompound("display").getString("Name");				
					}
					
					StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
					StonkCompanionClient.barrel_transactions.get(barrel_pos).put(player_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(player_item_name, 0) + item_qty_put);

					StonkCompanionClient.LOGGER.info("Player put " + item_qty_put + " of " + player_item.getName().getString() + " into the barrel.");
					
				}
				
			}
			
			
		}else if(action_type == SlotActionType.SWAP && !is_player_inv) {
			
			// Offhand swap is not considered atm.
			if (button >= 0 && button <= 8) {
				Item active_slot_item = active_slot.getItem();
				int active_slot_itemstk_qty = active_slot.getCount();
				ItemStack hotbar_slot = container.slots.get(54 + button).getStack();
				Item player_item = hotbar_slot.getItem();
				int player_itemstk_qty = hotbar_slot.getCount();
				int item_qty_taken = active_slot_itemstk_qty;
				int item_qty_put = player_itemstk_qty;
				
				// button 0 = hotbar slot 1 = slot 54
				// button 8 = hotbar slot 8 = slot 62
				
				String active_slot_item_name = "";
				
				if(active_slot.getNbt() == null || !active_slot.getNbt().contains("Monumenta")) {
					active_slot_item_name = active_slot.getItem().getName().getString();
				}else {
					active_slot_item_name = active_slot.getNbt().getCompound("plain").getCompound("display").getString("Name");				
				}
				
				String player_item_name = "";
				
				if(hotbar_slot.getNbt() == null || !hotbar_slot.getNbt().contains("Monumenta")) {
					player_item_name = hotbar_slot.getItem().getName().getString();
				}else {
					player_item_name = hotbar_slot.getNbt().getCompound("plain").getCompound("display").getString("Name");				
				}
				
				StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
				StonkCompanionClient.barrel_transactions.get(barrel_pos).put(player_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(player_item_name, 0) + item_qty_put);
				StonkCompanionClient.barrel_transactions.get(barrel_pos).put(active_slot_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(active_slot_item_name, 0) - item_qty_taken);
				
				StonkCompanionClient.LOGGER.info("Player took " + item_qty_taken + " of " + active_slot_item.getName().getString() + " from the barrel.");
				StonkCompanionClient.LOGGER.info("Player put " + item_qty_put + " of " + player_item.getName().getString() + " into the barrel.");
			}
			
		}else if(action_type == SlotActionType.THROW && !is_player_inv) {
			
			Item active_slot_item = active_slot.getItem();
			int active_slot_itemstk_qty = active_slot.getCount();
			int item_qty_taken = (button == 0) ? 1 : active_slot_itemstk_qty;
			
			String active_slot_item_name = "";
			
			if(active_slot.getNbt() == null || !active_slot.getNbt().contains("Monumenta")) {
				active_slot_item_name = active_slot.getItem().getName().getString();
			}else {
				active_slot_item_name = active_slot.getNbt().getCompound("plain").getCompound("display").getString("Name");				
			}
			
			StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
			StonkCompanionClient.barrel_transactions.get(barrel_pos).put(active_slot_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(active_slot_item_name, 0) - item_qty_taken);
			
			
			StonkCompanionClient.LOGGER.info("Player took " + item_qty_taken + " of " + active_slot_item.getName().getString() + " from the barrel.");
			
		}
		
	}
	
	private void stonkCompanionOnCloseCheck(HandledScreen<?> screen) 
	{
		
		if(!StonkCompanionClient.checkpointing && !StonkCompanionClient.fairprice_detection && !StonkCompanionClient.is_mistrade_checking) return;
		if(screen.getClass() != GenericContainerScreen.class) return;
		if(!screen.getTitle().getString().equals("Barrel")) return;
		
		//String containerType = screen.getTitle().getString();
		
		ScreenHandler container = screen.getScreenHandler();
		List<Slot> list_of_items = container.slots.stream().filter(slot -> slot.inventory.getClass() != PlayerInventory.class).toList();
		
		if(list_of_items.size() != 27) return;
		
		if(StonkCompanionClient.is_mistrade_checking && StonkCompanionClient.last_right_click != null && StonkCompanionClient.getShard().equals("plots")) {
			handlingMistradesClose(list_of_items);
		}
		
		if(StonkCompanionClient.checkpointing && StonkCompanionClient.last_right_click != null && StonkCompanionClient.anti_monu) {
			StonkCompanionClient.anti_monu = false;
			stonkCompanionCreateCheckpoint(list_of_items);
		}
		
		if(StonkCompanionClient.fairprice_detection && StonkCompanionClient.getShard().equals("plots")) {
			detectFairPrice(list_of_items);
		}
		
	}
	
	private void handlingMistradesClose(List<Slot> items) {
		
		int barrelx = StonkCompanionClient.last_right_click.getX();
		int barrely = StonkCompanionClient.last_right_click.getY();
		int barrelz = StonkCompanionClient.last_right_click.getZ();	
		String barrel_pos = String.format("x%d/y%d/z%d", barrelx, barrely, barrelz);
			
		if(StonkCompanionClient.barrel_transactions.containsKey(barrel_pos)) {
			StonkCompanionClient.barrel_timeout.put(barrel_pos, 0);
		}
		
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
			
			if(item.getNbt() == null || !item.getNbt().contains("Monumenta")) {
				item_name = item.getItem().getName().getString();
				
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
				}
			}
		}
		
		StonkCompanionClient.LOGGER.info("Checked Barrel: " + currency_type + " " + ask_price_compressed + " " + bid_price_compressed);
		
		if (currency_type == -1 || ask_price_compressed == -1 || bid_price_compressed == -1) {
			return;
		}
		
		StonkCompanionClient.LOGGER.info("Created barrel at " + barrel_pos);
		
		StonkCompanionClient.barrel_prices.put(barrel_pos, new Barrel(label, barrel_pos, ask_price, bid_price, ask_price_compressed, bid_price_compressed, currency_type));
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
						continue;
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
