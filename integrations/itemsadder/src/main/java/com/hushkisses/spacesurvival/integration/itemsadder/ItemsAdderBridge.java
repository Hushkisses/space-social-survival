package com.hushkisses.spacesurvival.integration.itemsadder;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public final class ItemsAdderBridge {

    private static final String CUSTOM_STACK_CLASS = "dev.lone.itemsadder.api.CustomStack";
    private static final String FONT_IMAGE_CLASS =
            "dev.lone.itemsadder.api.FontImages.FontImageWrapper";
    private static final String HUD_HOLDER_CLASS =
            "dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper";
    private static final String CUSTOM_HUD_CLASS =
            "dev.lone.itemsadder.api.FontImages.PlayerCustomHudWrapper";

    public boolean isAvailable() {
        try {
            Class.forName(CUSTOM_STACK_CLASS);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public Optional<String> fontImage(String namespacedId, int pixelOffset) {
        try {
            Class<?> fontImageClass = Class.forName(FONT_IMAGE_CLASS);
            Object wrapper = fontImageClass
                    .getConstructor(String.class)
                    .newInstance(namespacedId);

            Method exists = fontImageClass.getMethod("exists");
            Object existsResult = exists.invoke(wrapper);
            if (!(existsResult instanceof Boolean available) || !available) {
                return Optional.empty();
            }

            Method setOffset = fontImageClass.getMethod("setOffset", int.class);
            Object shifted = setOffset.invoke(wrapper, pixelOffset);

            Method getString = fontImageClass.getMethod("getString");
            Object result = getString.invoke(shifted);
            return result instanceof String value && !value.isBlank()
                    ? Optional.of(value)
                    : Optional.empty();
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }


    public boolean showCustomHud(
            Player player,
            String hudNamespacedId,
            String fontImageNamespacedId,
            int xOffset
    ) {
        try {
            Class<?> holderClass = Class.forName(HUD_HOLDER_CLASS);
            Class<?> hudClass = Class.forName(CUSTOM_HUD_CLASS);
            Class<?> fontImageClass = Class.forName(FONT_IMAGE_CLASS);

            Object holder = holderClass
                    .getConstructor(Player.class)
                    .newInstance(player);
            Object hud = hudClass
                    .getConstructor(holderClass, String.class)
                    .newInstance(holder, hudNamespacedId);

            Method hudExists = hudClass.getMethod("exists");
            Object hudExistsResult = hudExists.invoke(hud);
            if (!(hudExistsResult instanceof Boolean exists) || !exists) {
                return false;
            }

            Object fontImage = fontImageClass
                    .getConstructor(String.class)
                    .newInstance(fontImageNamespacedId);
            Method imageExists = fontImageClass.getMethod("exists");
            Object imageExistsResult = imageExists.invoke(fontImage);
            if (!(imageExistsResult instanceof Boolean imageAvailable) || !imageAvailable) {
                return false;
            }

            hudClass.getMethod("setOffsetX", int.class).invoke(hud, xOffset);
            hudClass.getMethod("setFontImages", List.class)
                    .invoke(hud, List.of(fontImage));
            hudClass.getMethod("setVisible", boolean.class).invoke(hud, true);
            return true;
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean hideCustomHud(Player player, String hudNamespacedId) {
        try {
            Class<?> holderClass = Class.forName(HUD_HOLDER_CLASS);
            Class<?> hudClass = Class.forName(CUSTOM_HUD_CLASS);

            Object holder = holderClass
                    .getConstructor(Player.class)
                    .newInstance(player);
            Object hud = hudClass
                    .getConstructor(holderClass, String.class)
                    .newInstance(holder, hudNamespacedId);

            Method hudExists = hudClass.getMethod("exists");
            Object hudExistsResult = hudExists.invoke(hud);
            if (!(hudExistsResult instanceof Boolean exists) || !exists) {
                return false;
            }

            hudClass.getMethod("setVisible", boolean.class).invoke(hud, false);
            return true;
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException exception) {
            return false;
        }
    }

    public Optional<ItemStack> createItem(String namespacedId) {
        try {
            Class<?> customStackClass = Class.forName(CUSTOM_STACK_CLASS);
            Method getInstance = customStackClass.getMethod("getInstance", String.class);
            Object customStack = getInstance.invoke(null, namespacedId);
            if (customStack == null) {
                return Optional.empty();
            }

            Method getItemStack = customStack.getClass().getMethod("getItemStack");
            Object result = getItemStack.invoke(customStack);
            if (result instanceof ItemStack itemStack) {
                return Optional.of(itemStack.clone());
            }
            return Optional.empty();
        } catch (ReflectiveOperationException | LinkageError exception) {
            return Optional.empty();
        }
    }
}
