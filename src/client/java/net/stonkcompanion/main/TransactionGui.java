package net.stonkcompanion.main;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;

// https://github.com/Njol/UnofficialMonumentaMod/blob/mc1.20.4/src/main/java/ch/njol/unofficialmonumentamod/features/calculator/CalculatorWidget.java

public class TransactionGui {

	public static final TransactionGui INSTANCE = new TransactionGui();
	
	public static TransactionWidget recentWidget = null;
	
	public boolean shouldRender() {
		
		//StonkCompanionClient.LOGGER.warn("Should Render?");
		
		if(StonkCompanionClient.last_right_click == null) return false;
		//StonkCompanionClient.LOGGER.warn("last_right_click is not null.");
		if(!StonkCompanionClient.anti_monu) return false;
		//StonkCompanionClient.LOGGER.warn("anti_monu is true.");
		MinecraftClient mc = MinecraftClient.getInstance();
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click) == null) return false;
		//StonkCompanionClient.LOGGER.warn("There is a block.");
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click).getType() != BlockEntityType.BARREL) return false;
		//StonkCompanionClient.LOGGER.warn("The block is a barrel.");
		// if(!StonkCompanionClient.anti_monu_inv_init) return false;
		// StonkCompanionClient.LOGGER.warn("anti_monu_inv_init is true.");
		
		return StonkCompanionClient.getShard().equals("plots") && MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen && StonkCompanionClient.is_mistrade_checking;
	}
	
}
