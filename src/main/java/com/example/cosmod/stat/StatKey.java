package com.example.cosmod.stat;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class StatKey {
    public static KeyMapping OPEN_STAT;

    public static void register() {
        OPEN_STAT = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.cosmod.stat", GLFW.GLFW_KEY_O, "key.categories.misc"));
    }
}
