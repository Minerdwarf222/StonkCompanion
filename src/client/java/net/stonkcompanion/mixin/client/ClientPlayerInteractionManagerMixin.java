package net.stonkcompanion.mixin.client;

import java.util.HashMap;
import java.util.List;

import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import net.stonkcompanion.main.Barrel;
import net.stonkcompanion.main.Barrel.BarrelTypes;
import net.stonkcompanion.main.StonkCompanionClient;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
	@Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V")
	private void stonkCompanionclickSlotInject(int syncId, int slot_id, int button, SlotActionType action_type, PlayerEntity player, CallbackInfo info) {
		stonkCompanionMouseClickInjectHelper(player.currentScreenHandler, slot_id, button, action_type, player);
	}
	
	private void stonkCompanionMouseClickInjectHelper(ScreenHandler screen_handler, int slot_id, int button, SlotActionType action_type, PlayerEntity player) {
		
		// Just a bunch of guard checks. First do we even care then is this a barrel and lastly are we in plots.
		if(!StonkCompanionClient.is_mistrade_checking) return;
		try {
			if(screen_handler != null && screen_handler.getType() != ScreenHandlerType.GENERIC_9X3) return;
		} catch (UnsupportedOperationException e){
			// StonkCompanionClient.LOGGER.error("ScreenHandler type is null.");
		}
		// TODO: Is this really needed and if so what is a better way since this doesn't work.
		// if(!screen.getTitle().getString().equals("Barrel")) return;
		if(!StonkCompanionClient.getShard().equals("plots")) return;
		if(!StonkCompanionClient.anti_monu) return;
		if(StonkCompanionClient.anti_monu_is_not_barrel) return;
		if(StonkCompanionClient.last_right_click == null) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click) == null) return;
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click).getType() != BlockEntityType.BARREL) return;
		
        DefaultedList<Slot> defaulted_list = screen_handler.slots;
        
        if(slot_id < 0 || slot_id >= defaulted_list.size()) return;

        Slot slot = defaulted_list.get(slot_id);
        
        /*ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
        for (Slot slot : defaulted_list) {
            list.add(slot.getStack().copy());
        }*/
		
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
		
		int barrelx = StonkCompanionClient.last_right_click.getX();
		int barrely = StonkCompanionClient.last_right_click.getY();
		int barrelz = StonkCompanionClient.last_right_click.getZ();	
		String barrel_pos = String.format("x%d/y%d/z%d", barrelx, barrely, barrelz);
		
		ItemStack player_itemstk = screen_handler.getCursorStack();
		ItemStack active_slot = slot.getStack();
		
		if (player_itemstk == null) return;
		
		String player_item_str = (player_itemstk != null) ? player_itemstk.getTranslationKey() : "None";
		String active_item_str = (active_slot != null) ? active_slot.getTranslationKey() : "None";
		
		boolean is_player_inv = slot.inventory.getClass() == PlayerInventory.class;
		String player_inv = is_player_inv ? "Player" : "Not Player";
		
		ScreenHandler container = screen_handler;
		List<Slot> list_of_items = container.slots.stream().filter(_slot -> _slot.inventory.getClass() != PlayerInventory.class).toList();
		List<Slot> list_of_player_items = container.slots.stream().filter(_slot -> _slot.inventory.getClass() == PlayerInventory.class).toList();
		
		if (list_of_items.size() != 27) return;
		
		if(StonkCompanionClient.is_verbose_logging) StonkCompanionClient.LOGGER.info(player_inv + " slot. Slot ID: " + slot_id + " Button: " + button + " Action Type: " + action_type.name() + " Player Cursor: x" + player_itemstk.getCount() + " " + player_item_str + " Active Slot Item: x" + active_slot.getCount() + " " + active_item_str);

		// Ignore the action if it is just two empty stacks.
		/* if (player_itemstk.isEmpty() && !slot.hasStack()) {
			return;
		}*/
		
		if(action_type == SlotActionType.PICKUP_ALL) {
			
			Item player_item = player_itemstk.getItem();
			int player_itemstk_qty = player_itemstk.getCount();
			int item_qty_taken = 0;
			
			String player_item_name = getItemName(player_itemstk);
			
			StonkCompanionClient.barrel_changes.clear();
			
			// Check barrel for loose items. Loose being non max.
			for(int i = 0; i < 27; i++) {
				if(player_itemstk_qty >= player_item.getMaxCount())	break;		
				if(!list_of_items.get(i).hasStack()) continue;
				
				ItemStack item = list_of_items.get(i).getStack();
				
				if(item.getCount() == item.getMaxCount()) continue;
				
				if(ItemStack.canCombine(player_itemstk, item)) {
					int item_qty = item.getCount();
					
					int _taken = (player_itemstk_qty+item_qty <= player_item.getMaxCount()) ? item_qty : player_item.getMaxCount() - player_itemstk_qty;
					
					if(player_itemstk_qty+item_qty <= player_item.getMaxCount()) {
						StonkCompanionClient.barrel_changes.put(i, null);
					}else {
						StonkCompanionClient.barrel_changes.put(i, list_of_items.get(i).getStack().copyWithCount(item_qty-_taken));
					}
					
					item_qty_taken += _taken;
					player_itemstk_qty += _taken;						
				}
			}
			
			// Check player inv for loose items.
			for(int i = 0; i < list_of_player_items.size(); i++) {
				if(player_itemstk_qty >= player_item.getMaxCount()) break;
				if(!list_of_player_items.get(i).hasStack()) continue;
				
				ItemStack item = list_of_player_items.get(i).getStack();
				
				if(item.getCount() == item.getMaxCount()) continue;
				
				if(ItemStack.canCombine(player_itemstk, item)) {
					int item_qty = item.getCount();
					
					int _taken = (player_itemstk_qty+item_qty <= player_item.getMaxCount()) ? item_qty : player_item.getMaxCount() - player_itemstk_qty;
					
					player_itemstk_qty += _taken;						
				}				
			}
			
			// Check barrel inv for max. Don't care about player inv so that can be skipped.
			for(int i = 0; i < 27; i++) {
				if(player_itemstk_qty >= player_item.getMaxCount())	break;		
				if(!list_of_items.get(i).hasStack()) continue;
				
				ItemStack item = list_of_items.get(i).getStack();
				
				if(item.getCount() < item.getMaxCount()) continue;
				
				if(ItemStack.canCombine(player_itemstk, item)) {
					int item_qty = item.getCount();
					
					int _taken = (player_itemstk_qty+item_qty <= player_item.getMaxCount()) ? item_qty : player_item.getMaxCount() - player_itemstk_qty;
					
					if(player_itemstk_qty+item_qty <= player_item.getMaxCount()) {
						StonkCompanionClient.barrel_changes.put(i, null);
					}else {
						StonkCompanionClient.barrel_changes.put(i, list_of_items.get(i).getStack().copyWithCount(item_qty-_taken));
					}
					
					item_qty_taken += _taken;
					player_itemstk_qty += _taken;						
				}
			}
			
			if(item_qty_taken != 0) {
				onClickInjectHelper(barrel_pos, player_item_name, item_qty_taken, true, list_of_items);
			}
		}else if(action_type == SlotActionType.QUICK_MOVE) {
			
			if(is_player_inv) {
				
				Item active_slot_item = active_slot.getItem();
				int active_slot_itemstk_qty = active_slot.getCount();
				int item_qty_put = 0;
				
				String active_slot_item_name = getItemName(active_slot);
				
				StonkCompanionClient.barrel_changes.clear();
				
				for(int i = 0; i < 27; i++) {
					
					Slot _slot = list_of_items.get(i);
					
					if(active_slot_itemstk_qty <= 0) break;		
					if(!_slot.hasStack()) {
						item_qty_put += active_slot_itemstk_qty;
						break;
					}
					
					ItemStack item = _slot.getStack();
					
					if(ItemStack.canCombine(active_slot, item)) {
						int item_qty = item.getCount();
						
						int _taken = (active_slot_itemstk_qty+item_qty <= active_slot_item.getMaxCount()) ? active_slot_itemstk_qty : active_slot_item.getMaxCount() - item_qty;
						
						if(active_slot_itemstk_qty+item_qty <= active_slot.getMaxCount()) {
							StonkCompanionClient.barrel_changes.put(i, item.copyWithCount(item.getMaxCount()));
						}else {
							StonkCompanionClient.barrel_changes.put(i, item.copyWithCount(item_qty+_taken));
						}
						
						item_qty_put += _taken;
						active_slot_itemstk_qty = active_slot_itemstk_qty - _taken;
					}
				}
				
				if(item_qty_put != 0) {
					onClickInjectHelper(barrel_pos, active_slot_item_name, item_qty_put, false, list_of_items);	
				}
			}else {
				
				Item active_slot_item = active_slot.getItem();
				int active_slot_itemstk_qty = active_slot.getCount();
				int item_qty_taken = 0;
				
				String active_slot_item_name = getItemName(active_slot);
				
				StonkCompanionClient.barrel_changes.clear();
				
				for(Slot _slot : list_of_player_items) {
					if(active_slot_itemstk_qty <= 0) break;		
					if(!_slot.hasStack()) {
						item_qty_taken += active_slot_itemstk_qty;
						break;
					}
					
					ItemStack item = _slot.getStack();
					
					if(ItemStack.canCombine(active_slot, item)) {

						int item_qty = item.getCount();
						
						int _taken = (active_slot_itemstk_qty+item_qty <= active_slot_item.getMaxCount()) ? active_slot_itemstk_qty : active_slot_item.getMaxCount() - item_qty;
						
						item_qty_taken += _taken;
						active_slot_itemstk_qty = active_slot_itemstk_qty - _taken;
					}
				}
				
				if(item_qty_taken != 0) {
					
					StonkCompanionClient.barrel_changes.put(slot_id, active_slot_itemstk_qty <= 0 ? null : active_slot.copyWithCount(active_slot_itemstk_qty));
					
					onClickInjectHelper(barrel_pos, active_slot_item_name, item_qty_taken, true, list_of_items);
				}
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
				int player_itemstk_qty = player_itemstk.getCount();
				
				if (player_itemstk.isEmpty()) {
					
					int item_qty_taken = active_slot_itemstk_qty;
					
					String active_slot_item_name = getItemName(active_slot);
					
					if(item_qty_taken != 0) {
						
						StonkCompanionClient.barrel_changes.clear();
						StonkCompanionClient.barrel_changes.put(slot_id, null);
						
						onClickInjectHelper(barrel_pos, active_slot_item_name, item_qty_taken, true, list_of_items);				
					}
				}else if(active_slot.isEmpty()) {
					
					int item_qty_put = player_itemstk_qty;

					String player_item_name = getItemName(player_itemstk);
					
					if(item_qty_put != 0) {
						
						StonkCompanionClient.barrel_changes.clear();
						StonkCompanionClient.barrel_changes.put(slot_id, player_itemstk);
						
						onClickInjectHelper(barrel_pos, player_item_name, item_qty_put, false, list_of_items);					
					}
				}else if(!ItemStack.canCombine(player_itemstk, active_slot)) {

					int item_qty_taken = active_slot_itemstk_qty;
					int item_qty_put = player_itemstk_qty;
					
					String active_slot_item_name = getItemName(active_slot);
					String player_item_name = getItemName(player_itemstk);
					
					if (item_qty_put != 0) {
						StonkCompanionClient.barrel_changes.clear();
						StonkCompanionClient.barrel_changes.put(slot_id, player_itemstk);
					}
					
					onClickInjectHelper(barrel_pos, active_slot_item_name, item_qty_taken, player_item_name, item_qty_put, list_of_items);					
				}else {
					if(active_slot_item.getMaxCount() == active_slot_itemstk_qty) {
						return;
					}else if(active_slot_itemstk_qty + player_itemstk_qty <= active_slot_item.getMaxCount()) {
						
						int item_qty_put = player_itemstk_qty;
											
						String player_item_name = getItemName(player_itemstk);
						
						if (item_qty_put != 0) {
							StonkCompanionClient.barrel_changes.clear();
							StonkCompanionClient.barrel_changes.put(slot_id, active_slot.copyWithCount(active_slot_itemstk_qty + player_itemstk_qty));
						}
						
						onClickInjectHelper(barrel_pos, player_item_name, item_qty_put, false, list_of_items);						
					}else if(active_slot_itemstk_qty + player_itemstk_qty > active_slot_item.getMaxCount()){
						
						int item_qty_put = active_slot_item.getMaxCount() - active_slot_itemstk_qty;

						String player_item_name = getItemName(player_itemstk);
						
						if (item_qty_put != 0) {
							StonkCompanionClient.barrel_changes.clear();
							StonkCompanionClient.barrel_changes.put(slot_id, active_slot.copyWithCount(active_slot_item.getMaxCount()));
						}
						
						onClickInjectHelper(barrel_pos, player_item_name, item_qty_put, false, list_of_items);						
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
				int player_itemstk_qty = player_itemstk.getCount();
				
				if (player_itemstk.isEmpty()) {
					
					int item_qty_taken = (int)(Math.ceil(active_slot_itemstk_qty/2.0));
					
					String active_slot_item_name = getItemName(active_slot);
					
					if(active_slot_item_name.startsWith("Tesseract of Knowledge (u)")) {
						if(mc.player.experienceLevel >= 25) {
							active_slot_item_name = "Repair Anvil";
							item_qty_taken = 1;
							
							if (item_qty_taken != 0) {
								StonkCompanionClient.barrel_changes.clear();
							}
							
							onClickInjectHelper(barrel_pos, active_slot_item_name, item_qty_taken, false, list_of_items);	
						}
					}else {	
						
						if (item_qty_taken != 0) {
							StonkCompanionClient.barrel_changes.clear();
							StonkCompanionClient.barrel_changes.put(slot_id, active_slot.copyWithCount(active_slot.getCount()/2));
						}
						
						onClickInjectHelper(barrel_pos, active_slot_item_name, item_qty_taken, true, list_of_items);	
					}
				}else if(active_slot.isEmpty()) {
					
					int item_qty_put = 1;
					
					String player_item_name = getItemName(player_itemstk);
					
					if (item_qty_put != 0) {
						StonkCompanionClient.barrel_changes.clear();
						StonkCompanionClient.barrel_changes.put(slot_id, player_itemstk.copyWithCount(1));
					}
					
					onClickInjectHelper(barrel_pos, player_item_name, item_qty_put, false, list_of_items);					
				}else if(!ItemStack.canCombine(player_itemstk, active_slot)) {
					
					int item_qty_taken = active_slot_itemstk_qty;
					int item_qty_put = player_itemstk_qty;
					
					String active_slot_item_name = getItemName(active_slot);	
					String player_item_name = getItemName(player_itemstk);
					
					if(player_item_name.equals("Repair Anvil") && active_slot_item_name.startsWith("Tesseract of Knowledge (u)")) {
						onClickInjectHelper(barrel_pos, player_item_name, item_qty_put, false, list_of_items);	
					}else if(player_item_name.startsWith("Tesseract of Knowledge (u)") && !active_slot_item_name.equals("Repair Anvil")) {
						
						// How do I handle repairing an item?
						/*if(active_slot.isDamaged()) {
							onClickInjectHelper(barrel_pos, "Repair Anvil", 1, false);	
						}	*/					
					}else {
						if (item_qty_put != 0) {
							StonkCompanionClient.barrel_changes.clear();
							StonkCompanionClient.barrel_changes.put(slot_id, player_itemstk);
						}
						onClickInjectHelper(barrel_pos, active_slot_item_name, item_qty_taken, player_item_name, item_qty_put, list_of_items);	
					}
				}else {
				
					if(active_slot_item.getMaxCount() == active_slot_itemstk_qty) {
						return;
					}
					
					int item_qty_put = 1;
					
					String player_item_name = getItemName(player_itemstk);
					
					if (item_qty_put != 0) {
						StonkCompanionClient.barrel_changes.clear();
						StonkCompanionClient.barrel_changes.put(slot_id, active_slot.copyWithCount(active_slot.getCount()+1));
					}
					
					onClickInjectHelper(barrel_pos, player_item_name, item_qty_put, false, list_of_items);					
				}
				
			}
			
			
		}else if(action_type == SlotActionType.SWAP && !is_player_inv) {
			
			// 1-9 buttons
			if (button >= 0 && button <= 8) {
				int active_slot_itemstk_qty = active_slot.getCount();
				ItemStack hotbar_slot = container.slots.get(54 + button).getStack();
				int player_itemstk_qty = hotbar_slot.getCount();
				int item_qty_taken = active_slot_itemstk_qty;
				int item_qty_put = player_itemstk_qty;
				
				// button 0 = hotbar slot 1 = slot 54
				// button 8 = hotbar slot 8 = slot 62
				
				String active_slot_item_name = getItemName(active_slot);
				String player_item_name = getItemName(hotbar_slot);
				
				if (item_qty_taken != 0) {
					StonkCompanionClient.barrel_changes.clear();
					StonkCompanionClient.barrel_changes.put(slot_id, hotbar_slot);
				}
				
				onClickInjectHelper(barrel_pos, active_slot_item_name, item_qty_taken, player_item_name, item_qty_put, list_of_items);
			// offhand swap
			}else if (!StonkCompanionClient.has_offhandswap_off && button == 40) {
				int active_slot_itemstk_qty = active_slot.getCount();
				ItemStack offhand_slot = mc.player.getOffHandStack();
				int offhand_itemstk_qty = offhand_slot.getCount();
				
				int item_qty_taken = active_slot_itemstk_qty;
				int item_qty_put = offhand_itemstk_qty;
				
				// button 0 = hotbar slot 1 = slot 54
				// button 8 = hotbar slot 8 = slot 62
				
				String active_slot_item_name = getItemName(active_slot);
				String offhand_item_name = getItemName(offhand_slot);
				
				StonkCompanionClient.LOGGER.info(offhand_item_name + " " + item_qty_put);
				
				if (item_qty_taken != 0) {
					StonkCompanionClient.barrel_changes.clear();
					StonkCompanionClient.barrel_changes.put(slot_id, offhand_slot);
				}
				
				onClickInjectHelper(barrel_pos, active_slot_item_name, item_qty_taken, offhand_item_name, item_qty_put, list_of_items);
			}
			
		}else if(action_type == SlotActionType.THROW && !is_player_inv) {
			
			int active_slot_itemstk_qty = active_slot.getCount();
			int item_qty_taken = (button == 0) ? 1 : active_slot_itemstk_qty;
			
			String active_slot_item_name = getItemName(active_slot);
			
			if (item_qty_taken != 0) {
				StonkCompanionClient.barrel_changes.clear();
				if(button == 0) {
					StonkCompanionClient.barrel_changes.put(slot_id, active_slot.copyWithCount(active_slot.getCount()-1));
				}else {
					StonkCompanionClient.barrel_changes.put(slot_id, null);
				}
			}			
			
			onClickInjectHelper(barrel_pos, active_slot_item_name, item_qty_taken, true, list_of_items);			
		}else if(action_type == SlotActionType.QUICK_CRAFT) {
			
			/* Handling quick_craft :doom:
			 * How to handle it thoughts:
			 * Once the quick_craft is committed an action is sent for each slot quick_crafted into.
			 * These slots will always be empty or have the same item in them otherwise it won't send.
			 * The game will attempt to distribute the item across the slots with math.floor(item_qty/slot count) in each slot.
			 * 	If a slot already has some of that item, then it just adds that amount up until the slot is full, the rest stay in cursor I think.
			 * So I need to have some timer going on that starts when the first quick_craft action is detected, then stops 0.5s after the last quick_craft is detected.
			 * Where it will then calculate what happened.
			 */
			
			StonkCompanionClient.barrel_changes.clear();
			StonkCompanionClient.action_been_done = false;

			int player_itemstk_qty = player_itemstk.getCount();

			String player_item_name = getItemName(player_itemstk);
			
			int active_slot_itemstk_qty = 0;
			if(slot.hasStack()){
				active_slot_itemstk_qty = active_slot.getCount();
			}
			
			if(!player_itemstk.isStackable() || (player_itemstk.getItem() == Items.LIME_STAINED_GLASS && player_item_name.startsWith("Tesseract of Knowledge (u)"))) {
				// StonkCompanionClient.LOGGER.info("Not stackable.");
				if(!is_player_inv) {
					onClickInjectHelper(barrel_pos, player_item_name, player_itemstk_qty, false, list_of_items);			
				}
			}else {
				if(StonkCompanionClient.is_quick_crafting) {
					StonkCompanionClient.time_since_start_of_quick_craft = 0;
					if(!is_player_inv) {
						StonkCompanionClient.quick_craft_slot_qty.put(slot_id, active_slot_itemstk_qty);
					}else {
						StonkCompanionClient.quick_craft_in_player_inv++;
					}
				}else {
					StonkCompanionClient.is_quick_crafting = true;
					StonkCompanionClient.quick_craft_button = button;
					StonkCompanionClient.quick_craft_barrel_pos = barrel_pos;
					StonkCompanionClient.quick_craft_item_max_stack = player_itemstk.getMaxCount();
					StonkCompanionClient.quick_craft_item_name = player_item_name;
					StonkCompanionClient.quick_craft_item_qty = player_itemstk_qty;
					StonkCompanionClient.time_since_start_of_quick_craft = 0;
					StonkCompanionClient.quick_craft_itemstk = player_itemstk;
					if(!is_player_inv) {
						StonkCompanionClient.quick_craft_slot_qty.put(slot_id, active_slot_itemstk_qty);
					}else {
						StonkCompanionClient.quick_craft_in_player_inv++;
					}
				}
			}
			
			// onClickInjectHelper(barrel_pos, player_item_name, item_qty_put, false);	
		}
		
	}
	
	// Given an item it returns the item name.
	private String getItemName(ItemStack given_item) {
		
		if(given_item.getNbt() == null || !given_item.getNbt().contains("Monumenta")) {
			return given_item.getItem().getTranslationKey().substring(given_item.getItem().getTranslationKey().lastIndexOf('.')+1);
		}else {
			
			String item_name = given_item.getNbt().getCompound("plain").getCompound("display").getString("Name");
			
			if(given_item.getItem() == Items.LIME_STAINED_GLASS && item_name.equals("Tesseract of Knowledge (u)")) {
				item_name += "|" + given_item.getNbt().getCompound("Monumenta").getCompound("PlayerModified").getInt("Charges");
			}
			
			return item_name;				
		}
	}
	
	private void onClickInjectHelper(String barrel_pos, String item_name, int item_qty, boolean is_taking, List<Slot> list_of_items) {
		if(is_taking) onClickInjectHelper(barrel_pos, item_name, item_qty, "", 0, list_of_items);
		if(!is_taking) onClickInjectHelper(barrel_pos, "", 0, item_name, item_qty, list_of_items);
	}
	
	// The intent of this is to just be a one stop function for all the click events instead of having like 15 of the same very similar things.
	private void onClickInjectHelper(String barrel_pos, String taken_item_name, int item_qty_taken, String put_item_name, int item_qty_put, List<Slot> list_of_items) {
		
		// Nothing happened.
		if(item_qty_taken == 0 && item_qty_put == 0) return;
			
		/*StonkCompanionClient.barrel_transactions.putIfAbsent(barrel_pos, new HashMap<String, Integer>());
		StonkCompanionClient.barrel_timeout.put(barrel_pos, 0);*/
		
		if(taken_item_name.startsWith("Tesseract of Knowledge (u)")) {
			item_qty_taken = Integer.parseInt(taken_item_name.substring(27));
			taken_item_name = "Repair Anvil";
		}
		if(put_item_name.startsWith("Tesseract of Knowledge (u)")) {
			item_qty_put= Integer.parseInt(put_item_name.substring(27));
			put_item_name = "Repair Anvil";
		}
			
		onClickActionAdd(barrel_pos, taken_item_name, item_qty_taken, put_item_name, item_qty_put);
		onClickActionMistradeCheck(barrel_pos);
		
		StonkCompanionClient.action_been_done = true;
		
		if(StonkCompanionClient.barrel_prices.containsKey(barrel_pos)) {
			Barrel active_barrel = StonkCompanionClient.barrel_prices.get(barrel_pos);
			active_barrel.time_since_last_movement = 0;
			if(item_qty_put != 0) {
				active_barrel.barrel_transactions.put(put_item_name, active_barrel.barrel_transactions.getOrDefault(put_item_name, 0) + item_qty_put);
				active_barrel.previous_action_name_put = put_item_name;
				active_barrel.previous_action_qty_put = item_qty_put;
				// StonkCompanionClient.barrel_transactions.get(barrel_pos).put(put_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(put_item_name, 0) + item_qty_put);
				// StonkCompanionClient.previous_action_qty_put = item_qty_put;
				// StonkCompanionClient.previous_action_name_put = put_item_name;
			}else {
				active_barrel.previous_action_name_put = "";
				active_barrel.previous_action_qty_put = 0;
				/*StonkCompanionClient.previous_action_qty_put = 0;
				StonkCompanionClient.previous_action_name_put = "";*/
			}
			if(item_qty_taken != 0) {
				active_barrel.barrel_transactions.put(taken_item_name, active_barrel.barrel_transactions.getOrDefault(taken_item_name, 0) - item_qty_taken);
				active_barrel.previous_action_name_take = taken_item_name;
				active_barrel.previous_action_qty_take = item_qty_taken;
				
				/*StonkCompanionClient.barrel_transactions.get(barrel_pos).put(taken_item_name, StonkCompanionClient.barrel_transactions.get(barrel_pos).getOrDefault(taken_item_name, 0) - item_qty_taken);
				StonkCompanionClient.previous_action_qty_take = item_qty_taken;
				StonkCompanionClient.previous_action_name_take = taken_item_name;*/
			}else {
				active_barrel.previous_action_name_take = "";
				active_barrel.previous_action_qty_take = 0;
				/*StonkCompanionClient.previous_action_qty_take = 0;
				StonkCompanionClient.previous_action_name_take = "";*/
			}
		}
		
		if(StonkCompanionClient.is_verbose_logging && item_qty_put != 0) StonkCompanionClient.LOGGER.info("Player put " + item_qty_put + " of " + put_item_name + " into the barrel.");
		if(StonkCompanionClient.is_verbose_logging && item_qty_taken != 0) StonkCompanionClient.LOGGER.info("Player took " + item_qty_taken + " of " + taken_item_name + " from the barrel.");
		
	}
	
	private void onClickActionMistradeCheck(String barrel_pos) {
		
		if (StonkCompanionClient.barrel_prices.get(barrel_pos) == null) return;
		
		StonkCompanionClient.barrel_prices.get(barrel_pos).validateTransaction();
		/*if (StonkCompanionClient.barrel_actions.get(barrel_pos) == null) return;
		
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
	    	// TODO: Turn this into an array of two strings.
	    	if(StonkCompanionClient.is_compressed_only) {
		    	StonkCompanionClient.barrel_transaction_solution.put(barrel_pos, "%s %s %s".formatted(currency_delta<0 ? "Take" : "Add", StonkCompanionClient.df1.format(Math.abs(currency_delta)), currency_str));	
	    	}else {
		    	StonkCompanionClient.barrel_transaction_solution.put(barrel_pos, "%s %d %s %s %s".formatted(currency_delta<0 ? "Take" : "Add", (int)(abs_currency_delta/64), hyper_str, StonkCompanionClient.df1.format(abs_currency_delta%64), currency_str));
	    	}
	    }*/
	}
	
	private void onClickActionAdd(String barrel_pos, String taken_item_name, int item_qty_taken, String put_item_name, int item_qty_put) {
		
		if (StonkCompanionClient.barrel_prices.get(barrel_pos) == null) return;
		StonkCompanionClient.barrel_prices.get(barrel_pos).onClickActionAdd(taken_item_name, item_qty_taken, put_item_name, item_qty_put);
		
		/*int currency_type = StonkCompanionClient.barrel_prices.get(barrel_pos).currency_type;
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
		}*/
		
	}
	
}
