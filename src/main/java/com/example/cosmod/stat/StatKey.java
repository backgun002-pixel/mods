package com.example.cosmod.stat;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class StatKey {

    public static KeyMapping OPEN_STAT;

    public static void register() {
        KeyMapping.Category cat = null;
        try {
            for (var ctor : KeyMapping.Category.class.getDeclaredConstructors()) {
                ctor.setAccessible(true);
                var params = ctor.getParameterTypes();
                if (params.length == 1 && params[0] == String.class)
                    cat = (KeyMapping.Category) ctor.newInstance("key.categories.cosmod");
                else if (params.length == 2 && params[0] == String.class && params[1] == int.class)
                    cat = (KeyMapping.Category) ctor.newInstance("key.categories.cosmod", 999);
                if (cat != null) break;
            }
        } catch (Exception ignored) {}

        if (cat == null) {
            try {
                var field = KeyMapping.class.getDeclaredField("CATEGORIES");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                var cats = (java.util.Collection<KeyMapping.Category>) field.get(null);
                if (!cats.isEmpty()) cat = cats.iterator().next();
            } catch (Exception ignored) {}
        }

        OPEN_STAT = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.cosmod.stat", GLFW.GLFW_KEY_O, cat));
    }
}
