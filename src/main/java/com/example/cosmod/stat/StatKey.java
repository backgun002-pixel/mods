package com.example.cosmod.stat;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class StatKey {
    public static KeyMapping OPEN_STAT;

    public static void register() {
        // SkillKey와 같은 카테고리 재사용
        KeyMapping.Category cat = null;
        try {
            var field = KeyMapping.class.getDeclaredField("CATEGORIES");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var cats = (java.util.Collection<KeyMapping.Category>) field.get(null);
            if (!cats.isEmpty()) cat = cats.iterator().next();
        } catch (Exception ignored) {}

        OPEN_STAT = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.cosmod.stat", GLFW.GLFW_KEY_O, cat));
    }
}
