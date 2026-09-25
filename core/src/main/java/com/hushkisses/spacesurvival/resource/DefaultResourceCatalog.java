package com.hushkisses.spacesurvival.resource;

import java.util.EnumMap;
import java.util.Map;

public final class DefaultResourceCatalog {

    private DefaultResourceCatalog() {
    }

    public static Map<ResourceType, ResourceDefinition> create() {
        EnumMap<ResourceType, ResourceDefinition> map = new EnumMap<>(ResourceType.class);
        put(map, ResourceType.REPAIR_PARTS, "수리 부품", ResourceStoragePreference.EITHER);
        put(map, ResourceType.CIRCUITS, "회로판", ResourceStoragePreference.EITHER);
        put(map, ResourceType.POWER_CELLS, "전력 셀", ResourceStoragePreference.SHARED);
        put(map, ResourceType.FUEL, "연료", ResourceStoragePreference.SHARED);
        put(map, ResourceType.MEDICAL_SUPPLIES, "의료 물자", ResourceStoragePreference.EITHER);
        put(map, ResourceType.BIO_SAMPLES, "생체 샘플", ResourceStoragePreference.EITHER);
        put(map, ResourceType.DATA_CORES, "데이터 코어", ResourceStoragePreference.EITHER);
        return Map.copyOf(map);
    }

    private static void put(
            Map<ResourceType, ResourceDefinition> map,
            ResourceType type,
            String displayName,
            ResourceStoragePreference preference
    ) {
        map.put(type, new ResourceDefinition(type, displayName, preference));
    }
}
