package net.stonkcompanion.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.stonkcompanion.main.StonkCompanionClient;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
        	
        	ConfigBuilder builder = ConfigBuilder.create()
        			.setParentScreen(parent)
        			.setTitle(Text.literal("StonkCompanion Config"));
        	
			builder.setSavingRunnable(() -> {
				
				StonkCompanionClient.writeConfig();
				
			});
			
			ConfigCategory general = builder.getOrCreateCategory(Text.of("Config"));	
			ConfigEntryBuilder entry_builder = builder.entryBuilder();
			
			general.addEntry(entry_builder.startBooleanToggle(Text.of("Mistrade Check"), StonkCompanionClient.is_mistrade_checking)
					.setDefaultValue(true)
					.setTooltip(Text.of("Check for mistrades or not."))
					.setSaveConsumer(new_value -> StonkCompanionClient.is_mistrade_checking = new_value)
					.build()
					);
			
			general.addEntry(entry_builder.startBooleanToggle(Text.of("Fairprice Detection"), StonkCompanionClient.fairprice_detection)
					.setDefaultValue(true)
					.setTooltip(Text.of("Calculate fairprice or not."))
					.setSaveConsumer(new_value -> StonkCompanionClient.fairprice_detection = new_value)
					.build()
					);
			
			general.addEntry(entry_builder.startBooleanToggle(Text.of("Compressed Only"), StonkCompanionClient.is_compressed_only)
					.setDefaultValue(false)
					.setTooltip(Text.of("Show some values in compressed only versus hyper and compressed."))
					.setSaveConsumer(new_value -> StonkCompanionClient.is_compressed_only = new_value)
					.build()
					);
			
			general.addEntry(entry_builder.startBooleanToggle(Text.of("Show Text"), StonkCompanionClient.is_showing_text)
					.setDefaultValue(false)
					.setTooltip(Text.of("Send mistrade check and fairprice as chat messages."))
					.setSaveConsumer(new_value -> StonkCompanionClient.is_showing_text = new_value)
					.build()
					);
			
			general.addEntry(entry_builder.startBooleanToggle(Text.of("Show GUI"), StonkCompanionClient.is_showing_gui)
					.setDefaultValue(true)
					.setTooltip(Text.of("Show a gui when looking in supported barrels in plots shard with mistrade checking on."))
					.setSaveConsumer(new_value -> StonkCompanionClient.is_showing_gui = new_value)
					.build()
					);
			
			general.addEntry(entry_builder.startBooleanToggle(Text.of("Offhand Swap Off"), StonkCompanionClient.has_offhandswap_off)
					.setDefaultValue(false)
					.setTooltip(Text.of("Toggle to true if you have turned being able to swap your offhand off in peb."))
					.setSaveConsumer(new_value -> StonkCompanionClient.has_offhandswap_off = new_value)
					.build()
					);
			
			general.addEntry(entry_builder.startBooleanToggle(Text.of("Verbose Logging"), StonkCompanionClient.is_verbose_logging)
					.setDefaultValue(true)
					.setTooltip(Text.of("Print barrel actions to log file."))
					.setSaveConsumer(new_value -> StonkCompanionClient.is_verbose_logging = new_value)
					.build()
					);
        	
			Screen screen = builder.build();
			return screen;

        };
    }	
}
