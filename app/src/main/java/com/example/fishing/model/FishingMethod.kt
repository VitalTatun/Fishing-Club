package com.example.fishing.model

enum class FishingMethod(val russianName: String) {
    NONE("Не указан"),
    BOBBER("Поплавок"),
    SPINNING("Спиннинг"),
    FEEDER("Фидер"),
    FLY_FISHING("Нахлыст");

    companion object {
        val methodsAndBaits: Map<FishingMethod, List<Bait>> = mapOf(
            BOBBER to listOf(
                Bait.WORM, Bait.MAGGOT, Bait.BLOODWORM, Bait.BARLEY,
                Bait.CORN, Bait.BREAD, Bait.POTATO, Bait.SEMOLINA
            ),
            FEEDER to listOf(
                Bait.WORM, Bait.MAGGOT, Bait.BLOODWORM, Bait.BARLEY,
                Bait.CORN, Bait.BREAD, Bait.POTATO, Bait.SEMOLINA
            ),
            SPINNING to listOf(
                Bait.SPOONBAIT, Bait.WOBBLER, Bait.EDIBLE_RUBBER
            ),
            FLY_FISHING to listOf(
                Bait.FLY, Bait.GRASSHOPPER, Bait.BUTTERFLY
            )
        )
    }
}
