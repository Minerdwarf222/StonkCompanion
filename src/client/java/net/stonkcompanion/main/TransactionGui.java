package net.stonkcompanion.main;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;

// https://github.com/Njol/UnofficialMonumentaMod/blob/mc1.20.4/src/main/java/ch/njol/unofficialmonumentamod/features/calculator/CalculatorWidget.java

public class TransactionGui {

	public static final TransactionGui INSTANCE = new TransactionGui();
	
	public static TransactionWidget recentWidget = null;
	
	@SuppressWarnings("resource")
	public boolean shouldRender() {
		
		//StonkCompanionClient.LOGGER.warn("Should Render?");
		if(!StonkCompanionClient.is_showing_gui) return false;
		if(!StonkCompanionClient.is_mistrade_checking) return false;
		if(!StonkCompanionClient.getShard().equals("plots")) return false;
		if(!(MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen)) return false;
		if(StonkCompanionClient.last_right_click == null) return false;
		//StonkCompanionClient.LOGGER.warn("last_right_click is not null.");
		if(StonkCompanionClient.anti_monu_is_not_barrel) return false;
		//StonkCompanionClient.LOGGER.warn("anti_monu_is_not_barrel is false.");
		MinecraftClient mc = MinecraftClient.getInstance();
		if(mc.player.getWorld() != null)
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click) == null) return false;
		//StonkCompanionClient.LOGGER.warn("There is a block.");
		if(mc.player.getWorld().getBlockEntity(StonkCompanionClient.last_right_click).getType() != BlockEntityType.BARREL) return false;
		//StonkCompanionClient.LOGGER.warn("The block is a barrel.");
		// if(!StonkCompanionClient.anti_monu_inv_init) return false;
		// StonkCompanionClient.LOGGER.warn("anti_monu_inv_init is true.");
		
		return true;
	}
	
}
