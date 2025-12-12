package net.stonkcompanion.main;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import net.stonkcompanion.suggestions.StonkCompanionCommandsSuggestions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;

import net.stonkcompanion.mixin.client.PlayerListHudAccessor;

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
	
	// I need to find a better way to do this. Oh well
	public static boolean fairprice_detection = false;
	
	// Shamelessly stolen from UMM.
	private static final Pattern shardGetterPattern = Pattern.compile(".*<(?<shard>[-\\w\\d]*)>.*");
	public static String cachedShard = null;
	private static long lastUpdateTimeShard = 0;
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	
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
		
		String s = _s.toLowerCase();
		
		if(s.endsWith("har") || s.endsWith("ar")) {
			return 3;
		}else if(s.endsWith("hcs") || s.endsWith("ccs") || s.endsWith("cs")) {
			return 2;
		}else if(s.endsWith("hxp") || s.endsWith("cxp") || s.endsWith("xp")) {
			return 1;
		}else {
			return -1;
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
			mult = 1/8;
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
	
	//@SuppressWarnings("resource")
	@SuppressWarnings("resource")
	@Override
	public void onInitializeClient() {

		try {
			Files.createDirectory(FabricLoader.getInstance().getConfigDir().resolve("StonkCompanion"));
		} catch (IOException e) {
			LOGGER.error("StonkCompanion failed to create the StonkCompanion config directory!");
		}
		
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
		
	    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("StonkCompanion")
		.then(argument("command", StringArgumentType.string())
				.suggests(StonkCompanionCommandsSuggestions.commandsSUGGESTION_PROVIDER)
	    		.executes(context -> {
	    			String given_command = StringArgumentType.getString(context, "command");
	    			if (given_command.equals("ToggleCoreprotect")){
		    			context.getSource().sendFeedback(Text.literal(change_coreprotect ? "Stopped changing coreprotect." : "Changing coreprotect."));
		    			change_coreprotect = !change_coreprotect;
	    			}else if(given_command.equals("ToggleCheckpointing")) {
		    			context.getSource().sendFeedback(Text.literal(checkpointing ? "Stopped getting checkpoints." : "Getting checkpoints."));
		    			checkpointing = !checkpointing;
		    			if(!checkpointing) {
		    				writeCheckpoints();
		    				checkpoints = new JsonObject();
		    			}
	    			}else if(given_command.equals("ToggleFairPrice")) {
	    				context.getSource().sendFeedback(Text.literal(fairprice_detection ? "Stopped detecting FairStonk." : "Detecting FairStonk."));
	    				fairprice_detection = !fairprice_detection;
	    			}
	    			return 1;
	    		}
	    	))));
		
	}

}