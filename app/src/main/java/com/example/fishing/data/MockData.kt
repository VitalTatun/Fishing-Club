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
        ),
        FishingReport(
            type = FishingType.FISHING_LOG,
            name = "Бесконечный зимний день",
            water = Water(waterName = "Минское море", latitude = 53.9, longitude = 27.3),
            photo = emptyList(),
            fishingTime = calendar.apply { set(2025, Calendar.JANUARY, 15) }.time,
            weight = 2.1,
            fish = listOf(Fish(name = "Плотва", count = 30), Fish(name = "Окунь", count = 5)),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(Bait.BLOODWORM),
            comment = """
                Зимняя рыбалка — это всегда испытание воли и снаряжения. В тот день мороз крепчал с каждой минутой, а ветер так и норовил утащить палатку в сторону дамбы. Но разве это может остановить истинного любителя подледного лова?

                Начал я с того, что пробурил около десяти лунок на разных глубинах, от четырех до семи метров. Закормил каждую чистым лиманом с добавлением сухарей. Первые полчаса — тишина. Только пар изо рта и хруст снега под сапогами. Решил начать обход. В первой лунке — пусто, во второй — один легкий тычок, и всё. А вот на третьей, самой глубокой, кивок вдруг плавно пошел вверх. Сердце екнуло. Подсечка! И вот она, первая плотвица, сверкает серебром на скупом зимнем солнце.

                И тут началось... Клев был такой интенсивный, что я едва успевал опускать мормышку. Плотва шла мерная, "батончики" грамм по 150-200. Периодически проскакивал окунь, заставляя мормышку танцевать активнее. Время пролетело незаметно. Когда я в очередной раз взглянул на часы, оказалось, что прошло уже четыре часа! 
                
                Но самое интересное началось ближе к вечеру. Ветер внезапно стих, и над озером повисла звенящая тишина. В такие моменты кажется, что ты один во всей вселенной. Я сидел над лункой, завороженный игрой света на льду. Вдруг кивок не просто дрогнул, а резко прижался к самому льду. Это был не окунь и не плотва. Леска 0.08 звенела как струна. Я понимал, что форсировать нельзя — оборвет в мгновение ока. 

                Минут десять я водил рыбу под лункой, пытаясь завести голову в 130-й диаметр. Когда, наконец, на поверхности показалась морда приличного подлещика, я понял, что день прожит не зря. Этот красавец стал достойным завершением моей рыбалки. 
                
                Домой возвращался уже в сумерках. Уставший, продрогший, но абсолютно счастливый. Сумка с рыбой приятно оттягивала плечо, а в голове уже зрели планы на следующие выходные. Ведь рыбалка — это не только улов, это состояние души, это единение с природой, которого так не хватает в городском шуме.

                Каждый раз, собирая ящик и сматывая удочки, я обещаю себе приехать сюда снова. Потому что каждый выезд — это новая история, новые эмоции и новые открытия. Даже если улова не будет совсем, сам процесс, эта тишина и ожидание поклевки стоят того, чтобы проснуться в пять утра и отправиться в морозную даль.
                
                За эти годы я понял одну простую истину: на рыбалке время не засчитывается в счет жизни. И это, пожалуй, самое главное открытие, которое я сделал. Надеюсь, мой следующий отчет будет еще более захватывающим, а рыба — еще крупнее!
            """.trimIndent(),
            user = sampleUser,
            fishingFromTheShore = false,
            isPublic = true
        )
    )
}
