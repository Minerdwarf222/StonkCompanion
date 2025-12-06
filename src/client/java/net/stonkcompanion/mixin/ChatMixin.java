package net.stonkcompanion.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
//import net.minecraft.item.ItemStack;
//import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.stonkcompanion.main.StonkCompanionClient;

@Environment(EnvType.CLIENT)
@Mixin(ChatHud.class)
public class ChatMixin {

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V"), method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", cancellable = true)
	private void onAddMessage(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator, CallbackInfo ci) {

		String message_str = message.getString();

		if(message_str.startsWith("§f◀ Page") || message_str.startsWith("§fPage §f1")) {
			return;
		}

		if(!(message_str.contains("§c-")) && !(message_str.contains("§a+")) && !(message_str.contains("§f")) && !(message_str.contains("§r"))) return;

		if(!((message_str.contains("§c-") || message_str.contains("§a+")) && (message_str.contains("added")||message_str.contains("removed")) && !message_str.contains("-----") && !message_str.contains("§m") || !message_str.contains("<") && message_str.contains("§7^"))) {
			return;
		}
		if(!((message_str.contains("§c-") || message_str.contains("§a+")) && (message_str.contains("added")||message_str.contains("removed")) && !message_str.contains("-----") && !message_str.contains("§m"))) {
			return;
		}
		
		if((message_str.contains("§c-") || message_str.contains("§a+")) && (message_str.contains("added")||message_str.contains("removed")) && !message_str.contains("-----") && !message_str.contains("§m")) {
			
			List<Text> parts_of_msg = message.getSiblings();

			List<Text> item_interaction = parts_of_msg.get(3).getSiblings();

			String base_item = "";
			String name_item = "";

			if (item_interaction.size() == 0){

				HoverEvent item_transaction = parts_of_msg.get(3).getStyle().getHoverEvent();				   
				String temp_name_item = item_transaction.getValue(HoverEvent.Action.SHOW_TEXT).toString();
				String backup_name_item = temp_name_item;
				temp_name_item = temp_name_item.replace("literal{", "");
				temp_name_item = temp_name_item.substring(0, temp_name_item.indexOf("}"));
				   
				base_item = parts_of_msg.get(3).getString();
				name_item = temp_name_item;
				
				for (Text text : item_interaction) {
					StonkCompanionClient.LOGGER.info(text.toString());
				}	
				StonkCompanionClient.LOGGER.info("");
			}
		}		
	}
}
