package net.stonkcompanion.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.stonkcompanion.main.TransactionGui;
import net.stonkcompanion.main.TransactionWidget;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractParentElement {
	@Shadow protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement);
	
	@Unique
	void initializeWidget() {
		//initialize calculator widget if it should be added.
		if (TransactionGui.INSTANCE.shouldRender()) {
			TransactionWidget transaction_gui = new TransactionWidget((Screen) (Object) this);
			transaction_gui.init();
			TransactionGui.recentWidget = transaction_gui;
			addDrawableChild(transaction_gui);
		}
	}

	@Inject(at = @At("HEAD"), method = "close")
	void onClose(CallbackInfo ci) {
		//remove calculator from opened screen.
		if (TransactionGui.recentWidget != null) {
			TransactionGui.recentWidget.onParentClosed();
			TransactionGui.recentWidget = null;
		}
	}

	@Inject(at = @At("TAIL"), method = "init(Lnet/minecraft/client/MinecraftClient;II)V")
	void onInit(MinecraftClient client, int width, int height, CallbackInfo ci) {
		initializeWidget();
	}
	@Inject(at = @At("TAIL"), method = "resize")
	void onResize(MinecraftClient client, int width, int height, CallbackInfo ci) {
		initializeWidget();
	}
	
}