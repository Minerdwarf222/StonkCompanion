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
		
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"togglecoreprotect")) {
			builder.suggest("togglecoreprotect");
		}
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"togglecheckpointing")) {
			builder.suggest("togglecheckpointing");
		}
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"togglefairprice")) {
			builder.suggest("togglefairprice");
		}
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"togglemistradecheck")) {
			builder.suggest("togglemistradecheck");
		}
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"mistradecheck")) {
			builder.suggest("mistradecheck"); 
		}
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"toggleverboselogging")) {
			builder.suggest("toggleverboselogging");
		}
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"clearreports")) {
			builder.suggest("clearreports");
		}
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"togglecompressed")) {
			builder.suggest("togglecompressed");
		}
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"toggleshowingtext")) {
			builder.suggest("toggleshowingtext");
		}
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"toggleshowinggui")) {
			builder.suggest("toggleshowinggui");
		}
		if(CommandSource.shouldSuggest(builder.getRemainingLowerCase(),"togglehavingoffhandswapon")) {
			builder.suggest("togglehavingoffhandswapon");
		}
		
		return builder.buildFuture();
	}
	
	
    public final static SuggestionProvider<FabricClientCommandSource> commandsSUGGESTION_PROVIDER = (context, builder) -> {
		return getCommandsSuggestions(context,builder);
    };
	
}
