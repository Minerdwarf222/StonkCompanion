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
import net.stonkcompanion.mixin.client.HandledScreenAccessor;

public class TransactionWidget extends AbstractParentElement implements Drawable, Selectable, Element{

    private static final MinecraftClient client = MinecraftClient.getInstance();
	
    private final List<Element> childrens = new ArrayList<>();
    private final List<Drawable> drawables = new ArrayList<>();
    private final List<Selectable> selectables = new ArrayList<>();
    
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
		
		if(!StonkCompanionClient.is_there_barrel_price) return;
		
		Barrel given_barrel = StonkCompanionClient.barrel_prices.get(StonkCompanionClient.barrel_pos_found);
		
		int time_left = -1;
		
		if(StonkCompanionClient.barrel_timeout.containsKey(StonkCompanionClient.barrel_pos_found)) {
			time_left = (int)((StonkCompanionClient.transaction_lifetime - StonkCompanionClient.barrel_timeout.get(StonkCompanionClient.barrel_pos_found))/20);
		}
		
		if(given_barrel == null) return;
		
		Rectangle dimension = getDimension();
		
		renderBackground(draw_context);
		
		int font_height = client.textRenderer.fontHeight;
		int y_diff_text = 1;
		
		int left_indent = 5;
		
		int red_color = 0xffff0000;
		int yellow_color = 0xffffff00;
		int green_color = 0xff00ff00;
		int light_blue_color = 0xff00ffff;
		
		draw_context.drawHorizontalLine(dimension.x+1, dimension.x + dimension.width - 1, dimension.y+y_diff_text, light_blue_color);
		y_diff_text = 5;
		
		MutableText stonk_companion = Text.literal("Stonk");
		stonk_companion.withColor(yellow_color);
		stonk_companion.append(Text.literal("Co").withColor(green_color));
		stonk_companion.append(Text.literal("mpanion").withColor(light_blue_color));
		
		draw_context.drawTextWithShadow(client.textRenderer, stonk_companion, dimension.x+45, dimension.y+y_diff_text, yellow_color);
		//draw_context.drawTextWithShadow(client.textRenderer, "Stonk", dimension.x+45, dimension.y+y_diff_text, yellow_color);
		//draw_context.drawTextWithShadow(client.textRenderer, "Co", dimension.x+45+client.textRenderer.getWidth("Stonk"), dimension.y+y_diff_text, green_color);
		//draw_context.drawTextWithShadow(client.textRenderer, "mpanion", dimension.x+45+client.textRenderer.getWidth("StonkCo"), dimension.y+y_diff_text, light_blue_color);
		y_diff_text += font_height + 1;
		draw_context.drawHorizontalLine(dimension.x+1, dimension.x + dimension.width - 1, dimension.y+y_diff_text, light_blue_color);
		//draw_context.drawTextWithShadow(client.textRenderer, "===========================", dimension.x+1, dimension.y+1+y_diff_text, light_blue_color);
		y_diff_text += 2 + 1;
		
		MutableText buy_for = Text.literal("Buy For: ");
		buy_for = buy_for.withColor(green_color);
		buy_for.append(Text.literal("%s".formatted(given_barrel.ask_str)).withColor(light_blue_color));

		draw_context.drawTextWithShadow(client.textRenderer, buy_for, dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
		//draw_context.drawTextWithShadow(client.textRenderer, "Buy For: %s".formatted(given_barrel.ask_str), dimension.x+5, dimension.y+y_diff_text, green_color);
		y_diff_text += font_height + 1;
		
		MutableText sell_for = Text.literal("Sell For: ");
		sell_for = sell_for.withColor(red_color);
		sell_for.append(Text.literal("%s".formatted(given_barrel.bid_str)).withColor(light_blue_color));
		
		draw_context.drawTextWithShadow(client.textRenderer, sell_for, dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
		//draw_context.drawTextWithShadow(client.textRenderer, "Sell For: %s".formatted(given_barrel.bid_str), dimension.x+5, dimension.y+y_diff_text, red_color);
		
		if(!StonkCompanionClient.fairprice_currency_str.equals("N/A")) {
			y_diff_text += font_height + 1;
			draw_context.drawTextWithShadow(client.textRenderer, "Fair Stonk Price: %.2f %s".formatted(StonkCompanionClient.fairprice_val, StonkCompanionClient.fairprice_currency_str), dimension.x+5, dimension.y+y_diff_text, light_blue_color);
		}
		y_diff_text += font_height + 1;
		draw_context.drawHorizontalLine(dimension.x+1, dimension.x + dimension.width - 1, dimension.y+y_diff_text, light_blue_color);
		//draw_context.drawTextWithShadow(client.textRenderer, "===========================", dimension.x+1, dimension.y+y_diff_text, light_blue_color);
		if (StonkCompanionClient.barrel_actions.containsKey(given_barrel.coords) && (StonkCompanionClient.barrel_actions.get(given_barrel.coords)[0] != 0 || StonkCompanionClient.barrel_actions.get(given_barrel.coords)[1] != 0)) {
			y_diff_text += 2 + 1;
			draw_context.drawTextWithShadow(client.textRenderer, "Recent Interactions:", dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
			if(StonkCompanionClient.barrel_actions.get(given_barrel.coords)[0] != 0) {
				y_diff_text += font_height + 1;
				draw_context.drawTextWithShadow(client.textRenderer, "%s %.2f Mat%s".formatted((StonkCompanionClient.barrel_actions.get(given_barrel.coords)[0] < 0) ? "Removed" : "Added", Math.abs(StonkCompanionClient.barrel_actions.get(given_barrel.coords)[0]), Math.abs(StonkCompanionClient.barrel_actions.get(given_barrel.coords)[0]) == 1 ? "" : "s"), dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
			}
			if(StonkCompanionClient.barrel_actions.get(given_barrel.coords)[1] != 0) {
				y_diff_text += font_height + 1;
				draw_context.drawTextWithShadow(client.textRenderer, "%s %.2f %s".formatted((StonkCompanionClient.barrel_actions.get(given_barrel.coords)[1] < 0) ? "Removed" : "Added", Math.abs(StonkCompanionClient.barrel_actions.get(given_barrel.coords)[1]), StonkCompanionClient.currency_type_to_compressed_text.get(given_barrel.currency_type)), dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
			}
			y_diff_text += font_height + 1;
			draw_context.drawHorizontalLine(dimension.x+1, dimension.x + dimension.width - 1, dimension.y+y_diff_text, light_blue_color);
			// draw_context.drawTextWithShadow(client.textRenderer, "===========================", dimension.x+1, dimension.y+y_diff_text, light_blue_color);
		}
		if(StonkCompanionClient.barrel_transaction_validity.containsKey(given_barrel.coords)) {
			y_diff_text += 2 + 1;
			draw_context.drawTextWithShadow(client.textRenderer, StonkCompanionClient.barrel_transaction_validity.get(given_barrel.coords) ? "Valid" : "Mistrade Detected", dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
			if(!StonkCompanionClient.barrel_transaction_validity.get(given_barrel.coords) && StonkCompanionClient.barrel_transaction_solution.containsKey(given_barrel.coords)) {
				y_diff_text += font_height + 1;
				draw_context.drawTextWithShadow(client.textRenderer, "Suggested Fix:", dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
				y_diff_text += font_height + 1;
				draw_context.drawTextWithShadow(client.textRenderer, StonkCompanionClient.barrel_transaction_solution.get(given_barrel.coords), dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
			}
			//y_diff_text += font_height + 1;
			//draw_context.drawTextWithShadow(client.textRenderer, "OR Take / Add Y", dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
			if (time_left != -1) {
				int seconds_since_last_interaction = (int)StonkCompanionClient.barrel_timeout.get(StonkCompanionClient.barrel_pos_found)/20;
				ZonedDateTime current_time = ZonedDateTime.now().minusSeconds(seconds_since_last_interaction);	
				y_diff_text += font_height + 1;
				draw_context.drawHorizontalLine(dimension.x+1, dimension.x + dimension.width - 1, dimension.y+y_diff_text, light_blue_color);
				y_diff_text += 2 + 1;
				draw_context.drawTextWithShadow(client.textRenderer, "Last Interaction: %s".formatted(current_time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))), dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
				y_diff_text += font_height + 1;
				draw_context.drawTextWithShadow(client.textRenderer, "Refund period ends in: %d:%d".formatted((int)time_left/60, time_left%60), dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
			}
			y_diff_text += font_height + 1;
			draw_context.drawHorizontalLine(dimension.x+1, dimension.x + dimension.width - 1, dimension.y+y_diff_text, light_blue_color);
			y_diff_text += 2 + 1;
			draw_context.drawTextWithShadow(client.textRenderer, "If this report is in error, type:", dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
			y_diff_text += font_height + 1;
			draw_context.drawTextWithShadow(client.textRenderer, "/StonkCompanion ClearReports", dimension.x+left_indent, dimension.y+y_diff_text, light_blue_color);
			y_diff_text += font_height + 1;
			draw_context.drawHorizontalLine(dimension.x+1, dimension.x + dimension.width - 1, dimension.y+y_diff_text, light_blue_color);
		}
		
	}
	
    public Rectangle getDimension() {
    	
        final int width = 165;
        final int height = 170;
        
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
