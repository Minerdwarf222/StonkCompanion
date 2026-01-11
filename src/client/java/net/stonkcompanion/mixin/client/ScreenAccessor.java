package net.stonkcompanion.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;

@Mixin(Screen.class)
public interface ScreenAccessor {
    @Invoker("remove")
    void doRemove(Element child);

    @Invoker("addDrawableChild")
    <T extends Element & Drawable & Selectable> T doAddDrawableChild(T drawableElement);	
}