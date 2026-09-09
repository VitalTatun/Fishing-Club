package com.example.fishing.data

import com.example.fishing.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

object MockData {
    val sampleUserId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val sampleUser = User(id = sampleUserId, name = "Виталий", image = "", email = "vital@example.com")

    private fun zoned(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.systemDefault()).toInstant()
    }

    val sampleReports = listOf(
        FishingReport(
            userId = sampleUserId,
            type = FishingType.FISHING_LOG,
            name = "Смеркалось...",
            water = Water(waterName = "Водохранилище Крылово", latitude = 53.998, longitude = 27.285, isPaid = true),
            photo = emptyList(),
            fishingStartAt = zoned(2023, 8, 22, 6, 30),
            fishingEndAt = zoned(2023, 8, 22, 15, 45),
            weight = 3.2,
            fish = listOf(
                Fish(name = "Карась", count = 2),
                Fish(name = "Окунь", count = 2),
                Fish(name = "Лещ", count = 2),
                Fish(name = "Подлещик", count = 2)
            ),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(Bait.BLOODWORM, Bait.MAGGOT),
            comment = "В этот раз разведал неглубокую часть водохранилища и поймал парочку красивых рыб!",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = false
        ),
        FishingReport(
            userId = sampleUserId,
            type = FishingType.FISHING_LOG,
            name = "Утренняя щука",
            water = Water(waterName = "Заславское вдхр. (Дамба)", latitude = 53.978, longitude = 27.352, isPaid = true),
            photo = emptyList(),
            fishingStartAt = zoned(2024, 5, 10, 5, 0),
            fishingEndAt = zoned(2024, 5, 10, 11, 30),
            weight = 3.5,
            fish = listOf(Fish(name = "Щука", count = 1)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.SPOONBAIT),
            comment = "Отличный клев на рассвете.",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = true
        ),
        FishingReport(
            userId = sampleUserId,
            type = FishingType.FISHING_LOG,
            name = "Карасиный рай",
            water = Water(waterName = "Чистый пруд", latitude = 53.965, longitude = 27.310, isPaid = true),
            photo = emptyList(),
            fishingStartAt = zoned(2024, 6, 1, 7, 0),
            fishingEndAt = zoned(2024, 6, 1, 14, 0),
            weight = 1.5,
            fish = listOf(Fish(name = "Карась", count = 10)),
            fishingMethod = FishingMethod.FEEDER,
            bait = listOf(Bait.CORN, Bait.WORM),
            comment = "Клевало как из пулемета!",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = true
        ),
        FishingReport(
            userId = sampleUserId,
            type = FishingType.HAUL,
            name = "Трофейный Сазан",
            water = Water(waterName = "Заславское вдхр. (Остров)", latitude = 53.992, longitude = 27.320, isPaid = true),
            photo = emptyList(),
            fishingStartAt = zoned(2024, 7, 15, 4, 30),
            fishingEndAt = zoned(2024, 7, 15, 21, 0),
            weight = 8.4,
            fish = listOf(Fish(name = "Сазан", count = 1)),
            fishingMethod = FishingMethod.FEEDER,
            bait = listOf(Bait.CORN),
            comment = "Боролся минут 20, но вытащил! Мой личный рекорд.",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = true
        ),
        FishingReport(
            userId = sampleUserId,
            type = FishingType.HAUL,
            name = "Ночной хищник",
            water = Water(waterName = "Заславское вдхр. (Семково)", latitude = 54.015, longitude = 27.360, isPaid = true),
            photo = emptyList(),
            fishingStartAt = zoned(2024, 8, 5, 22, 0),
            fishingEndAt = zoned(2024, 8, 6, 3, 30),
            weight = 5.2,
            fish = listOf(Fish(name = "Судак", count = 1)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.WOBBLER),
            comment = "Взял на глубине, в самой темноте. Мощный удар! Рыбалка через полночь.",
            user = sampleUser,
            fishingFromTheShore = false,
            isPublic = false
        ),
        FishingReport(
            userId = sampleUserId,
            type = FishingType.FISHING_LOG,
            name = "Летний зной на Заславском",
            water = Water(waterName = "Заславское вдхр. (Ратомский залив)", latitude = 53.958, longitude = 27.345, isPaid = true),
            photo = emptyList(),
            fishingStartAt = zoned(2024, 7, 20, 5, 30),
            fishingEndAt = zoned(2024, 7, 20, 13, 0),
            weight = 4.8,
            fish = listOf(Fish(name = "Лещ", count = 5), Fish(name = "Густера", count = 10)),
            fishingMethod = FishingMethod.FEEDER,
            bait = listOf(Bait.BARLEY, Bait.MAGGOT),
            comment = "Приехал на реку еще затемно, надеясь занять перспективное место.",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = true
        ),
        FishingReport(
            userId = sampleUserId,
            type = FishingType.HAUL,
            name = "Осенний монстр",
            water = Water(waterName = "Заславское вдхр. (Каналы)", latitude = 53.985, longitude = 27.275, isPaid = true),
            photo = emptyList(),
            fishingStartAt = zoned(2024, 9, 12, 6, 0),
            fishingEndAt = zoned(2024, 9, 12, 18, 30),
            weight = 12.5,
            fish = listOf(Fish(name = "Карп", count = 1)),
            fishingMethod = FishingMethod.FEEDER,
            bait = listOf(Bait.CORN),
            comment = "Это была одна из тех рыбалок, которые запоминаются на всю жизнь.",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = true
        ),
        FishingReport(
            userId = sampleUserId,
            type = FishingType.FISHING_LOG,
            name = "Бесконечный зимний день",
            water = Water(waterName = "Минское море (Центр)", latitude = 53.990, longitude = 27.330, isPaid = false),
            photo = emptyList(),
            fishingStartAt = zoned(2025, 1, 15, 8, 0),
            fishingEndAt = zoned(2025, 1, 15, 16, 30),
            weight = 2.1,
            fish = listOf(Fish(name = "Плотва", count = 30), Fish(name = "Окунь", count = 5)),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(Bait.BLOODWORM),
            comment = "Зимняя рыбалка — это всегда испытание воли и снаряжения.",
            user = sampleUser,
            fishingFromTheShore = false,
            isPublic = true
        )
    )
}
