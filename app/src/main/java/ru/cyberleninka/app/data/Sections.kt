package ru.cyberleninka.app.data

import ru.cyberleninka.app.BuildConfig

/**
 * Раздел КиберЛенинки, на который «заточено» конкретное приложение.
 * Значения приходят из product flavor (см. app/build.gradle.kts).
 */
object Sections {

    val title: String = BuildConfig.SECTION_TITLE

    val slugs: List<String> = BuildConfig.SECTION_SLUGS
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    /** Рубрики, входящие в раздел (в порядке рубрикатора). */
    val categories: List<Category> get() = Categories.all.filter { it.slug in slugs }
}
