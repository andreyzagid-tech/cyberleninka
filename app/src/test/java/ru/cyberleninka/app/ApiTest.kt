package ru.cyberleninka.app

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.cyberleninka.app.data.Api
import ru.cyberleninka.app.data.Html

class ApiTest {

    @Test
    fun search_returnsArticlesWithCleanTitles() = runBlocking {
        val res = Api.search("машинное обучение")
        assertTrue("found should be > 0, was ${res.found}", res.found > 0)
        assertTrue("articles should not be empty", res.articles.isNotEmpty())
        val a = res.articles.first()
        assertFalse("title must not contain tags: ${a.title}", a.title.contains("<"))
        assertFalse("title must not contain entities: ${a.title}", a.title.contains("&quot;"))
        assertTrue("title must be non blank", a.title.isNotBlank())
        assertTrue("slug must be non blank", a.slug.isNotBlank())
        println("found=${res.found} first=${a.title} (${a.year})")
    }

    @Test
    fun categoryPage_returnsTwentyArticles() = runBlocking {
        val list = Api.categoryPage("mathematics", 1)
        assertTrue("expected ~20 articles, got ${list.size}", list.size >= 15)
        val a = list.first()
        assertTrue("year should be parsed", a.year in 1900..2026)
        assertTrue("title non blank", a.title.isNotBlank())
        assertTrue("slug non blank", a.slug.isNotBlank())
        println("page1=${list.size} first=${a.title} ${a.year} ${a.authorsLine}")
    }

    @Test
    fun categoryPage_secondPageDiffers() = runBlocking {
        val p1 = Api.categoryPage("sociology", 1).map { it.slug }.toSet()
        val p2 = Api.categoryPage("sociology", 2).map { it.slug }.toSet()
        assertTrue("page 2 should contain other articles", (p2 - p1).isNotEmpty())
    }

    @Test
    fun article_returnsFullText() = runBlocking {
        val list = Api.categoryPage("biological-sciences", 1)
        val slug = list.first().slug
        val detail = Api.article(slug)
        assertTrue("text should be long", detail.text.length > 800)
        assertTrue("title non blank", detail.article.title.isNotBlank())
        assertTrue("pdf url present", detail.pdfUrl.endsWith(slug + ".pdf") || detail.pdfUrl.contains(slug))
        println("text=${detail.text.length} refs=${detail.references.length} pdf=${detail.pdfUrl}")
    }

    @Test
    fun html_conversion() {
        val text = Html.toText("<div class=\"title\">Заголовок &quot;тест&quot;</div><p>Абзац</p><br><p>Второй</p><script>bad()</script>")
        assertEquals("Заголовок \"тест\"\n\nАбзац\n\nВторой", text)
    }

    @Test
    fun html_articleBodyLooksReadable() = runBlocking {
        val slug = Api.categoryPage("chemical-sciences", 1).first().slug
        val text = Api.article(slug).text
        assertTrue("text should be long: ${text.length}", text.length > 800)
        assertFalse("no html attributes expected", text.contains("class="))
        assertFalse("no html comments expected", text.contains("<!--"))
        assertFalse("no script expected", text.contains("function"))
        assertTrue("must have paragraphs", text.count { c -> c == '\n' } > 10)
    }
}
