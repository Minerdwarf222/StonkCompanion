package net.stonkcompanion.main;

import java.awt.Rectangle;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.stonkcompanion.mixin.client.HandledScreenAccessor;

public class TransactionWidget extends AbstractParentElement implements Drawable, Selectable, Element{

    private static final MinecraftClient client = MinecraftClient.getInstance();
	
    private final List<Element> childrens = new ArrayList<>();
    private final List<Drawable> drawables = new ArrayList<>();
    private final List<Selectable> selectables = new ArrayList<>();
    
    private final Screen parent;
    
    private final int background_rect_width = 165;
    private final int background_rect_height = 190;
	
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
        drawables.clear();
        selectables.clear();
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
		
		if(StonkCompanionClient.barrel_pos_found.isBlank()) return;
		
		Barrel given_barrel = StonkCompanionClient.barrel_prices.get(StonkCompanionClient.barrel_pos_found);
		
		if(given_barrel == null) return;
		if(given_barrel.gui_text == null) return;
		
		Rectangle dimension = getDimension();
		
		renderBackground(draw_context);
		
		int font_height = client.textRenderer.fontHeight;
		int y_diff_text = 1;
		
		int left_indent = 5;
		
		// int red_color = 0xffff0000;
		int yellow_color = 0xffffff00;
		int green_color = 0xff00ff00;
		int light_blue_color = 0xff00ffff;
		
		draw_context.drawHorizontalLine(dimension.x+1, dimension.x + dimension.width - 1, dimension.y+y_diff_text, light_blue_color);
		y_diff_text = 5;
		
		MutableText stonk_companion = Text.literal("Stonk");
		stonk_companion.withColor(yellow_color);
		stonk_companion.append(Text.literal("Co").withColor(green_color));
		stonk_companion.append(Text.literal("mpanion").withColor(light_blue_color));
		
		draw_context.drawCenteredTextWithShadow(client.textRenderer, stonk_companion, (int)dimension.getCenterX(), dimension.y+y_diff_text, yellow_color);
		//draw_context.drawTextWithShadow(client.textRenderer, "Stonk", dimension.x+45, dimension.y+y_diff_text, yellow_color);
		//draw_context.drawTextWithShadow(client.textRenderer, "Co", dimension.x+45+client.textRenderer.getWidth("Stonk"), dimension.y+y_diff_text, green_color);
		//draw_context.drawTextWithShadow(client.textRenderer, "mpanion", dimension.x+45+client.textRenderer.getWidth("StonkCo"), dimension.y+y_diff_text, light_blue_color);
		y_diff_text += font_height + 1;
		
		
		// Current issue. How do I detect lines that need to be centered? I could just do a boolean array or smth idk.
		for(Text[] given_text_segment : given_barrel.gui_text) {
			if(given_text_segment == null) continue;
			boolean printed_line = false;
			for(Text given_text : given_text_segment) {
				if(given_text == null) continue;
				if(!printed_line) {
					draw_context.drawHorizontalLine(dimension.x+1, dimension.x + dimension.width - 1, dimension.y+y_diff_text, light_blue_color);
					y_diff_text += 2 + 1;
					printed_line = true;
				}
				
				draw_context.drawTextWithShadow(client.textRenderer, given_text, dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
				y_diff_text += font_height + 1;
			}
		}
		
		// draw_context.drawCenteredTextWithShadow(client.textRenderer, given_barrel.label, (int)dimension.getCenterX(), dimension.y+y_diff_text, light_blue_color);
		// draw_context.drawCenteredTextWithShadow(client.textRenderer, StonkCompanionClient.barrel_transaction_validity.get(given_barrel.coords) ? Text.literal("Valid Trade").formatted(Formatting.BOLD) : Text.literal("Invalid Trade").formatted(Formatting.BOLD), (int)dimension.getCenterX(), dimension.y+y_diff_text, light_blue_color);

		// y_diff_text += font_height + 1;
		draw_context.drawHorizontalLine(dimension.x+1, dimension.x + dimension.width - 1, dimension.y+y_diff_text, light_blue_color);		
	}
	
    public Rectangle getDimension() {
        
        int x = ((HandledScreenAccessor) parent).getX() - background_rect_width;
        int y = ((HandledScreenAccessor) parent).getY();

        return new Rectangle(x, y, background_rect_width, background_rect_height);
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
	
    public List<? extends Drawable> getDrawables() {
        return drawables;
    }

    protected  <T extends Drawable & Selectable & Element> T addDrawableChild(T child) {
        this.drawables.add(child);
        return addSelectableChild(child);
    }

    protected Drawable addDrawable(Drawable drawable) {
        this.drawables.add(drawable);
        return drawable;
    }

    protected <T extends Element & Selectable> T addSelectableChild(T child) {
        this.selectables.add(child);
        this.childrens.add(child);
        return child;
    }

}
