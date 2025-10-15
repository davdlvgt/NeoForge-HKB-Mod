package de.davidvogt.hkbmod.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String KEY_CATEGORY_HKBMOD = "key.category.hkbmod";

    public static final KeyMapping DRAGON_FIRE_KEY = new KeyMapping(
            "key.hkbmod.dragon_fire", // Übersetzungsschlüssel
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, // Standard-Taste: R
            KEY_CATEGORY_HKBMOD
    );
}

