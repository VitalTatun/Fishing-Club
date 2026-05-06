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
        ),
        FishingReport(
            type = FishingType.HAUL,
            name = "Трофейный Сазан",
            water = Water(waterName = "Озеро Нарочь", latitude = 54.8, longitude = 26.7),
            photo = listOf(R.drawable.example),
            fishingTime = calendar.apply { set(2024, Calendar.JULY, 15) }.time,
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
            type = FishingType.HAUL,
            name = "Ночной хищник",
            water = Water(waterName = "Река Днепр", latitude = 52.1, longitude = 30.2),
            photo = emptyList(),
            fishingTime = calendar.apply { set(2024, Calendar.AUGUST, 5) }.time,
            weight = 5.2,
            fish = listOf(Fish(name = "Судак", count = 1)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.WOBBLER),
            comment = "Взял на глубине, в самой темноте. Мощный удар!",
            user = sampleUser,
            fishingFromTheShore = false,
            isPublic = false
        ),
        FishingReport(
            type = FishingType.FISHING_LOG,
            name = "Летний зной на Березине",
            water = Water(waterName = "Река Березина", latitude = 53.9, longitude = 28.5),
            photo = listOf(R.drawable.example, R.drawable.example),
            fishingTime = calendar.apply { set(2024, Calendar.JULY, 20) }.time,
            weight = 4.8,
            fish = listOf(Fish(name = "Лещ", count = 5), Fish(name = "Густера", count = 10)),
            fishingMethod = FishingMethod.FEEDER,
            bait = listOf(Bait.BARLEY, Bait.MAGGOT),
            comment = "Приехал на реку еще затемно, надеясь занять перспективное место, но, как оказалось, не я один такой предприимчивый. Пришлось обосноваться на менее знакомом участке. Начал с активного закорма: две пачки тяжелой речной прикормки, немного мелассы и резаный опарыш. Первые два часа — полная тишина, только мелкая верховодка периодически теребила кончик фидера. Но стоило солнцу подняться чуть выше деревьев, как последовал мощный отстрел квивертипа. Первый лещ на килограмм в подсаке! Следом пошла густера, да такая бойкая, что скучать не давала. К полудню жара стала невыносимой, рыба ушла на глубину, и поклевки прекратились. Тем не менее, выездом крайне доволен, река как всегда порадовала своей красотой и порцией адреналина. В следующий раз попробую приехать с вечера, чтобы захватить самый пик ночного клева.",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = true
        ),
        FishingReport(
            type = FishingType.HAUL,
            name = "Осенний монстр",
            water = Water(waterName = "Заславское вдхр.", latitude = 54.0, longitude = 27.3),
            photo = listOf(R.drawable.example),
            fishingTime = calendar.apply { set(2024, Calendar.SEPTEMBER, 12) }.time,
            weight = 12.5,
            fish = listOf(Fish(name = "Карп", count = 1)),
            fishingMethod = FishingMethod.FEEDER,
            bait = listOf(Bait.CORN),
            comment = "Это была одна из тех рыбалок, которые запоминаются на всю жизнь. Осень, легкий туман над водой, тишина, нарушаемая только всплесками далекой рыбы. Поклевка была совершенно неожиданной — фрикцион просто взвизгнул, и удилище согнулось в дугу. Я сразу понял, что на том конце серьезный противник. Минут 15 мы просто перетягивали канат: я отыгрывал метр, он забирал три. Руки начали дрожать от напряжения, а сердце билось где-то в горле. Когда в первый раз показался бок этого монстра, я даже испугался, что подсак окажется мал. Но удача была на моей стороне. Этот золотистый красавец на 12.5 кг заставил меня попотеть, как никогда раньше. После быстрой фотосессии и взвешивания, трофей был бережно отпущен обратно в родную стихию. Эмоции зашкаливают до сих пор, ради таких моментов мы и ездим на рыбалку!",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = true
        )
    )
}
