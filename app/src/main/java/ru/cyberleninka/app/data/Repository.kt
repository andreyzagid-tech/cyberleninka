package ru.cyberleninka.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Repository private constructor(context: Context) {

    val db: Db = Db(context)
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO)
    private var indexJob: Job? = null

    @Volatile
    var indexRunning: Boolean = false
        private set

    @Volatile
    var indexState: String = ""
        private set

    @Volatile
    var indexDone: Int = 0
        private set

    private fun pdfFile(slug: String) =
        File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "pdf/$slug.pdf")

    fun pdfFileOf(slug: String): File = pdfFile(slug)

    fun hasPdf(slug: String) = pdfFile(slug).exists()

    suspend fun search(query: String): SearchResult {
        val remote = try {
            Api.search(query)
        } catch (e: Exception) {
            val local = db.searchLocal(query)
            if (local.isEmpty()) throw e
            return SearchResult(local.size, local, emptyList(), emptyList())
        }
        db.upsertArticles(remote.articles, withText = false)
        val merged = remote.articles.map { a ->
            val stored = db.article(a.slug)
            if (stored?.text != null) a.copy(text = stored.text) else a
        }
        return remote.copy(articles = merged)
    }

    suspend fun categoryPage(slug: String, page: Int): List<Article> {
        val list = Api.categoryPage(slug, page)
        db.upsertArticles(list)
        return list
    }

    data class Full(
        val article: Article,
        val text: String,
        val references: String,
        val pdfUrl: String
    )

    suspend fun full(slug: String): Full {
        val stored = db.article(slug)
        val cached = stored?.text
        if (cached != null && cached.length > 500) {
            return Full(stored, cached, db.getMeta("ref.$slug"), Api.BASE + "/article/n/$slug.pdf")
        }
        val detail = Api.article(slug)
        val head = buildString {
            appendLine(detail.article.title)
            if (detail.article.authorsLine.isNotBlank()) appendLine(detail.article.authorsLine)
            appendLine()
        }
        val body = head + detail.text
        db.upsertArticles(listOf(detail.article.copy(text = body)), withText = true)
        db.setMeta("ref.$slug", detail.references)
        return Full(detail.article.copy(text = body), body, detail.references, detail.pdfUrl)
    }

    fun startIndexDownload(
        onlyCategory: String?,
        onUpdate: (done: Int, state: String) -> Unit
    ) {
        if (indexJob?.isActive == true) return
        indexJob = scope.launch {
            indexRunning = true
            try {
                val cats = if (onlyCategory == null) Sections.categories else Categories.all.filter { it.slug == onlyCategory }
                val stampKey = if (onlyCategory == null) "crawl.all" else "crawl.${onlyCategory}"
                val startCatIndex = db.getMeta("$stampKey.catIndex", "0").toIntOrNull() ?: 0
                val startPage = db.getMeta("$stampKey.page", "1").toIntOrNull() ?: 1

                var done = db.count()
                indexDone = done
                for (ci in startCatIndex until cats.size) {
                    val cat = cats[ci]
                    var page = if (ci == startCatIndex) startPage else 1
                    while (true) {
                        ensureActive()
                        val batch = try {
                            Api.categoryPage(cat.slug, page)
                        } catch (e: Exception) {
                            delay(2000)
                            if (page > 1 && db.getMeta("$stampKey.page", "1").toInt() == page) page++
                            continue
                        }
                        if (batch.isEmpty()) break
                        done += batch.count { db.article(it.slug) == null }
                        db.upsertArticles(batch)
                        indexDone = done
                        db.setMeta("$stampKey.catIndex", ci.toString())
                        db.setMeta("$stampKey.page", page.toString())
                        val state = "${cat.title} · стр. $page · всего $done"
                        indexState = state
                        onUpdate(done, state)
                        if (batch.size < 20) break
                        page++
                        delay(400)
                    }
                    db.setMeta("$stampKey.page", "1")
                    delay(250)
                }
                db.setMeta(stampKey + ".finished", stamp())
                indexState = "Индекс обновлён: $done статей"
                onUpdate(done, indexState)
            } catch (e: Exception) {
                val msg = if (e is kotlinx.coroutines.CancellationException) "Остановлено" else "Ошибка: ${e.message}"
                indexState = msg
                onUpdate(db.count(), msg)
            } finally {
                indexRunning = false
            }
        }
    }

    fun stopIndexDownload() {
        indexJob?.cancel()
        indexJob = null
        indexRunning = false
        indexState = "Остановлено"
    }

    suspend fun downloadPdf(slug: String, onProgress: (Long, Long) -> Unit): File {
        val file = pdfFile(slug)
        if (file.exists() && file.length() > 1000) return file
        val url = Api.BASE + "/article/n/$slug.pdf"
        Http.download(url, file, onProgress)
        return file
    }

    fun markDownloaded() = db.setMeta("download.last", stamp())

    fun lastDownload(): String = db.getMeta("download.last", "ещё не запускалось")

    private fun stamp(): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context.applicationContext).also { instance = it }
            }
    }
}
