package com.hushkisses.spacesurvival.paper.item;

import com.hushkisses.spacesurvival.resource.ResourceType;
import org.bukkit.inventory.ItemStack;

public interface ResourceItemProvider {
    ItemStack create(ResourceType resourceType, int amount);
    boolean customItemsAvailable();
}
