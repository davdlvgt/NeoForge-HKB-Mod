package de.davidvogt.hkbmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.davidvogt.hkbmod.HKBMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final String KEY_CATEGORY = "key.categories." + HKBMod.MODID;

    public static final KeyMapping OPEN_RESEARCH_SCREEN = new KeyMapping(
            "key." + HKBMod.MODID + ".open_research_screen",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    );
}
