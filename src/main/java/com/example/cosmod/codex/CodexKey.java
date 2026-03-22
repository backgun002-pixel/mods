package com.example.cosmod.codex;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class CodexKey {

    public static KeyMapping OPEN_CODEX;

    public static void register() {
        KeyMapping.Category cat = createCategory("key.categories.cosmod");

        OPEN_CODEX = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.cosmod.codex", GLFW.GLFW_KEY_Y, cat));
    }

    private static KeyMapping.Category createCategory(String name) {
        try {
            for (var ctor : KeyMapping.Category.class.getDeclaredConstructors()) {
                ctor.setAccessible(true);
                var params = ctor.getParameterTypes();
                if (params.length == 1 && params[0] == String.class)
                    return (KeyMapping.Category) ctor.newInstance(name);
                if (params.length == 2 && params[0] == String.class && params[1] == int.class)
                    return (KeyMapping.Category) ctor.newInstance(name, 999);
            }
        } catch (Exception ignored) {}
        try {
            var f = KeyMapping.class.getDeclaredField("CATEGORIES");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            var cats = (java.util.Collection<KeyMapping.Category>) f.get(null);
            if (!cats.isEmpty()) return cats.iterator().next();
        } catch (Exception ignored) {}
        return null;
    }
}
