package com.hushkisses.spacesurvival.paper.facility;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.facility.action.FacilityActionId;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class FacilityMenuHolder implements InventoryHolder {

    private final FacilityId facilityId;
    private final Map<Integer, FacilityActionId> slotActions = new LinkedHashMap<>();
    private final Inventory inventory;

    public FacilityMenuHolder(
            FacilityId facilityId,
            int size,
            String title
    ) {
        this.facilityId = Objects.requireNonNull(facilityId, "facilityId");
        this.inventory = Bukkit.createInventory(
                this,
                size,
                Objects.requireNonNull(title, "title")
        );
    }

    public FacilityId facilityId() {
        return facilityId;
    }

    public void bind(int slot, FacilityActionId actionId) {
        slotActions.put(slot, Objects.requireNonNull(actionId, "actionId"));
    }

    public FacilityActionId actionAt(int slot) {
        return slotActions.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
