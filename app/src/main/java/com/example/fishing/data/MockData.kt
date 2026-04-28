package com.example.fishing.data

import com.example.fishing.R
import com.example.fishing.model.*
import java.util.*

object MockData {
    private val sampleUser = User(name = "Виталий", image = "", email = "vital@example.com")
    private val calendar = Calendar.getInstance()

    val sampleReports = listOf(
        FishingReport(
            type = FishingType.FISHING_LOG,
            name = "Смеркалось...",
            water = Water(waterName = "Водохранилище Крылово", latitude = 54.32344, longitude = 54.23425),
            photo = listOf(R.drawable.example, R.drawable.example, R.drawable.example),
            fishingTime = calendar.apply { set(2023, Calendar.AUGUST, 22) }.time,
            weight = 3.2,
            fish = listOf(
                Fish(name = "Карась", count = 2),
                Fish(name = "Окунь", count = 2),
                Fish(name = "Лещ", count = 2),
                Fish(name = "Подлещик", count = 2)
            ),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(Bait.BLOODWORM, Bait.MAGGOT),
            comment = "В этот раз разведал неглубокую часть водохранилища и поймал парочку красивых рыб! Замешав вечерком плотву с орехом от Feeder.by с утра поехал на мелководную часть вдх посмотреть как там обстоят дела с рыбкой. Мишаня с утра ловил подлещиков а у меня ни поклевки ни с ближней, ни с дальней точки! Думал в чем дело, где рыба?) Подсыпав секретного порошка от сенсас и пшикнув трапером увидел красивый загиб и лещик полторашка оказался в подсаке! В том году на этой локации таких рыбок не было, поэтому я неслабо был удивлен! Спустя час отвлекся от удилищ и по приходу вижу как дистанция красиво качает! Подсек и на том конце сидел такой же красивый лещик! Такая успешная разведка!",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = false
        ),
        FishingReport(
            type = FishingType.FISHING_LOG,
            name = "Утренняя щука",
            water = Water(waterName = "Река Кама", latitude = 55.0, longitude = 60.0),
            photo = emptyList(),
            fishingTime = calendar.apply { set(2024, Calendar.MAY, 10) }.time,
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
            type = FishingType.FISHING_LOG,
            name = "Карасиный рай",
            water = Water(waterName = "Чистый пруд", latitude = 53.0, longitude = 27.0),
            photo = emptyList(),
            fishingTime = calendar.apply { set(2024, Calendar.JUNE, 1) }.time,
            weight = 1.5,
            fish = listOf(Fish(name = "Карась", count = 10)),
            fishingMethod = FishingMethod.FEEDER,
            bait = listOf(Bait.CORN, Bait.WORM),
            comment = "Клевало как из пулемета!",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = true
        )
    )
}
