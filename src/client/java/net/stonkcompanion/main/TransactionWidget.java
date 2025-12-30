package net.stonkcompanion.main;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.stonkcompanion.mixin.client.HandledScreenAccessor;

public class TransactionWidget extends AbstractParentElement implements Drawable, Selectable, Element{

    private static final MinecraftClient client = MinecraftClient.getInstance();
	
    private final List<Element> childrens = new ArrayList<>();
    
    private final Screen parent;
	
    public void init() {
    	clear();
    	//StonkCompanionClient.LOGGER.info("Init widget.");
    }
    
    public TransactionWidget(Screen parent) {
    	this.parent = parent;
    }
    
    public void onParentClosed() {
    	clear();
    }
    
    private void clear() {
    	childrens.clear();
    }
    
	@Override
	public void appendNarrations(NarrationMessageBuilder var1) {
		// No		
	}

	@Override
	public SelectionType getType() {
		return SelectionType.FOCUSED;
	}

	@Override
	public void render(DrawContext draw_context, int mouseX, int mouseY, float delta) {
		renderBackground(draw_context);
	}
	
    public Rectangle getDimension() {
    	
        final int width = 140;
        final int height = 160;
        
        int x = ((HandledScreenAccessor) parent).getX() - width;
        int y = ((HandledScreenAccessor) parent).getY();

        return new Rectangle(x, y, width, height);
    }
	
	public void renderBackground(DrawContext drawContext) {
		Rectangle dimension = getDimension();
		
        final int bgColour = client.options.getTextBackgroundColor(0.3f);

        drawContext.fill(dimension.x, dimension.y, (int) dimension.getMaxX(), (int) dimension.getMaxY(), bgColour);
    }

	@Override
	public List<? extends Element> children() {
		return childrens;
	}

}
