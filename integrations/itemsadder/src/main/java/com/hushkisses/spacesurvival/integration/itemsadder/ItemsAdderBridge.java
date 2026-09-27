package com.hushkisses.spacesurvival.integration.itemsadder;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;

public final class ItemsAdderBridge {

    private static final String CUSTOM_STACK_CLASS = "dev.lone.itemsadder.api.CustomStack";
    private static final String FONT_IMAGE_CLASS =
            "dev.lone.itemsadder.api.FontImages.FontImageWrapper";

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
