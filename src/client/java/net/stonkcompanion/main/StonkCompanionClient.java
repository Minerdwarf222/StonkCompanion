package net.stonkcompanion.main;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;

public class StonkCompanionClient implements ClientModInitializer{

	public static final String MOD_ID = "stonkcompanion";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static String top_dir = FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion").toString();
	private boolean change_coreprotect = false;

	// Checkpoint vars. Probs move elsewhere later.
	public static boolean checkpointing = false;
	public static BlockPos last_right_click = null;
	public static boolean anti_monu = false;
	public static long open_barrel_time;
	public static String open_barrel_values = "";
	public static JsonObject checkpoints = new JsonObject();
	
	public void writeCheckpoints() {
		
		String current_timestamp = ""+Instant.now().getEpochSecond();
		
		if (checkpoints.isEmpty()) {
			return;
		}
		
		try (FileWriter writer = new FileWriter(top_dir+"/"+current_timestamp+"_checkpoints.json")){
			Gson gson = new GsonBuilder().create();
			gson.toJson(checkpoints, writer);
		} catch (IOException e) {
			LOGGER.error("StonkCompanion failed to create the checkpoints json!");
		}
	}
	
	@SuppressWarnings("resource")
	@Override
	public void onInitializeClient() {

		try {
			Files.createDirectory(FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion"));
		} catch (IOException e) {
			LOGGER.error("StonkCompanion failed to create the StonkCompanion config directory!");
		}
		
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("StonkCompanionToggleCheckpointing")
	    		.executes(context -> {
	    			context.getSource().sendFeedback(Text.literal(checkpointing ? "Stopped getting checkpoints." : "Getting checkpoints."));
	    			checkpointing = !checkpointing;
	    			if(!checkpointing) {
	    				writeCheckpoints();
	    				checkpoints = new JsonObject();
	    			}
	    			return 1;
	    		}
	    	)));
		
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
	    	
    		if(!world.isClient) return ActionResult.PASS;
    	
    		last_right_click =  hitResult.getBlockPos();
        
    		anti_monu = true;
    	
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

				temp_name_item = temp_name_item.replace("literal{", "");
				temp_name_item = temp_name_item.substring(0, temp_name_item.indexOf("}"));
				   
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
		
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("StonkCompanionToggleCoreprotect")
	    		.executes(context -> {
	    			context.getSource().sendFeedback(Text.literal(change_coreprotect ? "Stopped changing coreprotect." : "Changing coreprotect."));
	    			change_coreprotect = !change_coreprotect;
	    			return 1;
	    		}
	    )));
		
	}

}