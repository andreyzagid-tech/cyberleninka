package ru.cyberleninka.app.data

data class Article(
    val slug: String,
    val title: String,
    val annotation: String = "",
    val year: Int = 0,
    val journal: String = "",
    val authors: List<String> = emptyList(),
    val category: String = "",
    val text: String? = null
) {
    val authorsLine: String get() = authors.joinToString(", ")
    val journalLine: String get() = if (journal.isBlank()) "cyberleninka.ru" else journal
}

data class SearchResult(
    val found: Int,
    val articles: List<Article>,
    val years: List<Pair<String, Int>>,
    val journals: List<Pair<String, Int>>
)

data class Category(
    val slug: String,
    val title: String
)

object Categories {
    val all: List<Category> = listOf(
        Category("agricultural-biotechnology", "Сельскохозяйственные биотехнологии"),
        Category("agriculture-forestry-and-fisheries", "Сельское хозяйство, лесное хозяйство и рыболовство"),
        Category("animal-and-dairy-science", "Животноводство и молочное дело"),
        Category("arts-history-of-arts-performing-arts-music", "Искусство. История искусств. Сценительное искусство. Музыка"),
        Category("basic-medicine", "Фундаментальная медицина"),
        Category("biological-sciences", "Биологические науки"),
        Category("chemical-engineering", "Химическая технология"),
        Category("chemical-sciences", "Химические науки"),
        Category("civil-engineering", "Строительство и архитектура"),
        Category("clinical-medicine", "Клиническая медицина"),
        Category("computer-and-information-sciences", "Компьютерные и информационные науки"),
        Category("earth-and-related-environmental-sciences", "Науки о Земле и смежные экологические науки"),
        Category("economics-and-business", "Экономика и бизнес"),
        Category("educational-sciences", "Образование"),
        Category("electrical-electronic-information-engineering", "Электроника, электротехника и информационная инженерия"),
        Category("environmental-biotechnology", "Экологическая биотехнология"),
        Category("environmental-engineering", "Инженерия окружающей среды"),
        Category("health-biotechnology", "Медицинская биотехнология"),
        Category("health-sciences", "Медицина и здравоохранение"),
        Category("history-and-archaeology", "История и археология"),
        Category("industrial-biotechnology", "Промышленная биотехнология"),
        Category("languages-and-literature", "Языки и литература"),
        Category("law", "Право"),
        Category("materials-engineering", "Материаловедение и инженерия материалов"),
        Category("mathematics", "Математика"),
        Category("mechanical-engineering", "Механика и машиностроение"),
        Category("media-and-communications", "Медиа и коммуникации"),
        Category("medical-engineering", "Медицинская техника"),
        Category("nano-technology", "Нанотехнологии"),
        Category("philosophy-ethics-and-religion", "Философия, этика и религиоведение"),
        Category("physical-sciences", "Физические науки"),
        Category("political-science", "Политология"),
        Category("psychology", "Психология"),
        Category("social-and-economic-geography", "Социально-экономическая география"),
        Category("sociology", "Социология"),
        Category("veterinary-science", "Ветеринария")
    )

    fun bySlug(slug: String): Category? = all.firstOrNull { it.slug == slug }
}
