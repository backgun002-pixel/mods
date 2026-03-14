package com.example.cosmod.skill;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class SkillKey {

    public static KeyMapping SKILL_1;
    public static KeyMapping SKILL_2;
    public static KeyMapping SKILL_3;

    public static void register() {
        KeyMapping.Category cat = createCategory("key.categories.cosmod");

        SKILL_1 = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.cosmod.skill1", GLFW.GLFW_KEY_R, cat));
        SKILL_2 = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.cosmod.skill2", GLFW.GLFW_KEY_F, cat));
        SKILL_3 = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.cosmod.skill3", GLFW.GLFW_KEY_G, cat));
    }

    private static KeyMapping.Category createCategory(String name) {
        try {
            for (var ctor : KeyMapping.Category.class.getDeclaredConstructors()) {
                ctor.setAccessible(true);
                var params = ctor.getParameterTypes();
                if (params.length == 1 && params[0] == String.class) {
                    return (KeyMapping.Category) ctor.newInstance(name);
                }
                if (params.length == 2 && params[0] == String.class && params[1] == int.class) {
                    return (KeyMapping.Category) ctor.newInstance(name, 999);
                }
            }
        } catch (Exception e) {}
        try {
            var field = KeyMapping.class.getDeclaredField("CATEGORIES");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var cats = (java.util.Collection<KeyMapping.Category>) field.get(null);
            if (!cats.isEmpty()) return cats.iterator().next();
        } catch (Exception ignored) {}
        return null;
    }
}
