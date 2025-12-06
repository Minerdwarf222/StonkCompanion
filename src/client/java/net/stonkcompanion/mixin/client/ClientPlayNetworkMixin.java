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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.stonkcompanion.main.StonkCompanionClient;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkMixin {

	@Inject(at = @At(value = "TAIL"), method = "onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V", cancellable = true)
	private void onInventoryPKT(InventoryS2CPacket packet, CallbackInfo ci) {

		if(!StonkCompanionClient.checkpointing) return;
		if(StonkCompanionClient.last_right_click == null) return;
		
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
		
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity playerEntity = client.player;
		if (packet.getSyncId() != 0 && packet.getSyncId() == playerEntity.currentScreenHandler.syncId) {
			
			StonkCompanionClient.open_barrel_time = Instant.now().getEpochSecond();
			
			List<Slot> list_of_slots = playerEntity.currentScreenHandler.slots.stream().filter(slot -> slot.inventory.getClass() != PlayerInventory.class).toList();
			
			if(list_of_slots.size() != 27) return;
			
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
			
			StonkCompanionClient.open_barrel_values = barrel_inventory.toString();
		}
	}
}
