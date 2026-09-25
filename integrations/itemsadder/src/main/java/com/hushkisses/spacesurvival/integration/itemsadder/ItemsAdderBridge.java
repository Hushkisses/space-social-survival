package com.hushkisses.spacesurvival.integration.itemsadder;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;

public final class ItemsAdderBridge {

    private static final String CUSTOM_STACK_CLASS = "dev.lone.itemsadder.api.CustomStack";

    public boolean isAvailable() {
        try {
            Class.forName(CUSTOM_STACK_CLASS);
            return true;
        } catch (ClassNotFoundException exception) {
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
