package net.stonkcompanion.suggestions;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;

public class StonkCompanionCommandsSuggestions {

	public static CompletableFuture<Suggestions> getCommandsSuggestions(CommandContext<FabricClientCommandSource> context,
			SuggestionsBuilder builder) throws CommandSyntaxException {
		
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"ToggleCoreprotect")) {
			builder.suggest("ToggleCoreprotect");
		}
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"ToggleCheckpointing")) {
			builder.suggest("ToggleCheckpointing");
		}
		
		return builder.buildFuture();
	}
	
	
    public final static SuggestionProvider<FabricClientCommandSource> commandsSUGGESTION_PROVIDER = (context, builder) -> {
		return getCommandsSuggestions(context,builder);
    };
	
}
