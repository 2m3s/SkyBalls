package com.epic60869.sky2m.features.core;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Whether a MoulConfig keybind setting (a key, or a mouse button stored the MoulConfig way) is held. The Minecraft 26.2
 * and 26.3 versions of this class differ (GLFW and SDL); features use this so their own code is the same in both.
 */
public final class BindKeys {
    private BindKeys() {}

    /** Whether the bind is set at all. */
    public static boolean isSet(int bind) {
        return bind != GLFW.GLFW_KEY_UNKNOWN && bind != 0;
    }

    public static boolean down(int bind) {
        if (!isSet(bind) || Minecraft.getInstance().getWindow() == null) return false;
        long window = Minecraft.getInstance().getWindow().handle();
        // MoulConfig stores mouse buttons as -100 + the GLFW button.
        if (bind >= -100 && bind <= -100 + GLFW.GLFW_MOUSE_BUTTON_LAST) {
            return GLFW.glfwGetMouseButton(window, bind + 100) == GLFW.GLFW_PRESS;
        }
        return bind >= GLFW.GLFW_KEY_SPACE && bind <= GLFW.GLFW_KEY_LAST && GLFW.glfwGetKey(window, bind) == GLFW.GLFW_PRESS;
    }
}
