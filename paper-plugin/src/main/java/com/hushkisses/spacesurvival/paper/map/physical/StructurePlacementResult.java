package com.hushkisses.spacesurvival.paper.map.physical;

public record StructurePlacementResult(
        boolean placed,
        String source,
        String detail
) {
    public static StructurePlacementResult missing(String source) {
        return new StructurePlacementResult(false, source, "NBT 파일 없음");
    }

    public static StructurePlacementResult success(String source, String detail) {
        return new StructurePlacementResult(true, source, detail);
    }

    public static StructurePlacementResult failed(String source, String detail) {
        return new StructurePlacementResult(false, source, detail);
    }
}
