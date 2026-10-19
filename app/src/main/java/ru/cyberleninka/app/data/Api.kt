package ru.cyberleninka.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

object Http {
    private const val UA =
        "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    suspend fun get(url: String, referer: String? = null): String = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20000
            readTimeout = 60000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
            setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9")
            setRequestProperty("Accept-Encoding", "gzip")
            if (referer != null) setRequestProperty("Referer", referer)
        }
        val code = try {
            conn.responseCode
        } catch (e: Exception) {
            conn.disconnect()
            throw NetException("Нет связи с сервером: ${e.message}")
        }
        if (code !in 200..299) {
            conn.disconnect()
            throw NetException("Сервер ответил кодом $code")
        }
        val stream = conn.inputStream
        val raw = (if (conn.contentEncoding?.contains("gzip") == true) GZIPInputStream(stream) else stream)
            .readBytes()
        conn.disconnect()
        String(raw, Charsets.UTF_8)
    }

    suspend fun postJson(url: String, body: String): String = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "gzip")
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = try {
                conn.errorStream?.readTextSafe().orEmpty()
            } catch (e: Exception) {
                ""
            }
            conn.disconnect()
            throw NetException("Сервер ответил кодом $code $err")
        }
        val stream = conn.inputStream
        val raw = (if (conn.contentEncoding?.contains("gzip") == true) GZIPInputStream(stream) else stream)
            .readBytes()
        conn.disconnect()
        String(raw, Charsets.UTF_8)
    }

    suspend fun download(url: String, target: java.io.File, onProgress: (Long, Long) -> Unit = { _, _ -> }) =
        withContext(Dispatchers.IO) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20000
                readTimeout = 120000
                setRequestProperty("User-Agent", UA)
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                throw NetException("Не удалось скачать PDF (код $code)")
            }
            val total = conn.contentLength.toLong()
            var got = 0L
            target.parentFile?.mkdirs()
            val tmp = java.io.File(target.parentFile, target.name + ".part")
            conn.inputStream.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        got += n
                        onProgress(got, total)
                    }
                }
            }
            conn.disconnect()
            if (target.exists()) target.delete()
            tmp.renameTo(target)
        }

    private fun InputStream.readTextSafe(): String =
        try {
            BufferedReader(InputStreamReader(this, Charsets.UTF_8)).readText()
        } catch (e: Exception) {
            ""
        }
}

class NetException(message: String) : Exception(message)

object Api {
    const val BASE = "https://cyberleninka.ru"
    private const val SEARCH_URL = "$BASE/api/search"

    data class ArticleDetail(
        val article: Article,
        val text: String,
        val references: String,
        val pdfUrl: String
    )

    suspend fun search(query: String): SearchResult {
        val body = JSONObject()
            .put("q", query)
            .put("mode", "articles")
            .toString()
        val raw = Http.postJson(SEARCH_URL, body)
        val json = JSONObject(raw)
        val list = ArrayList<Article>()
        val arr = json.optJSONArray("articles") ?: org.json.JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val link = o.optString("link")
            val slug = link.substringAfterLast("/article/").substringAfter("n/").substringAfter("c/")
            if (slug.isBlank()) continue
            val authors = ArrayList<String>()
            val authorsArr = o.optJSONArray("authors")
            if (authorsArr != null) {
                for (a in 0 until authorsArr.length()) {
                    val s = authorsArr.optString(a).trim()
                    if (s.isNotEmpty()) authors.add(s)
                }
            }
            list.add(
                Article(
                    slug = slug,
                    title = Html.toText(o.optString("name")).ifBlank { "Без названия" },
                    annotation = Html.toText(o.optString("annotation")),
                    year = o.optInt("year", 0),
                    journal = Html.toText(o.optString("journal")),
                    authors = authors
                )
            )
        }
        return SearchResult(
            found = json.optInt("found", list.size),
            articles = list,
            years = facets(json.optJSONObject("agg_year")),
            journals = facets(json.optJSONObject("agg_journal"))
        )
    }

    private fun facets(node: JSONObject?): List<Pair<String, Int>> {
        if (node == null) return emptyList()
        val out = ArrayList<Pair<String, Int>>()
        val keys = node.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = node.opt(k)
            val n = when (v) {
                is Int -> v
                is Number -> v.toInt()
                is JSONObject -> v.optInt("count", 0)
                else -> 0
            }
            out.add(k to n)
        }
        return out.sortedByDescending { it.second }.take(12)
    }

    suspend fun categoryPage(slug: String, page: Int): List<Article> {
        val url = if (page <= 1) "$BASE/article/c/$slug" else "$BASE/article/c/$slug?page=$page"
        val html = Http.get(url)
        val out = ArrayList<Article>()
        val re = Regex(
            "/article/n/([A-Za-z0-9\\-_.]+)\"\\s*>\\s*<div class=\"title\">(.*?)</div>" +
                "[\\s\\S]*?<span>\\s*(\\d{4})\\s*/\\s*([^<]*)</span>"
        )
        for (m in re.findAll(html)) {
            val slugA = m.groupValues[1]
            val title = Html.toText(m.groupValues[2])
            if (title.isBlank()) continue
            out.add(
                Article(
                    slug = slugA,
                    title = title,
                    year = m.groupValues[3].toIntOrNull() ?: 0,
                    authors = m.groupValues[4].split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    category = Categories.bySlug(slug)?.title.orEmpty()
                )
            )
        }
        return out
    }

    suspend fun article(slug: String): ArticleDetail {
        val html = Http.get("$BASE/article/n/$slug")
        fun meta(name: String): String =
            Regex("<meta name=\"" + Regex.escape(name) + "\" content=\"([^\"]*)\"")
                .find(html)?.groupValues?.get(1).orEmpty()

        val title = Html.unescape(meta("citation_title")).ifBlank { slug }
        val year = meta("citation_publication_date").substringBefore("/").trim().toIntOrNull() ?: 0
        val journal = Html.unescape(meta("citation_journal_title"))
        val authors = Regex("<meta name=\"citation_author\" content=\"([^\"]*)\"")
            .findAll(html)
            .map { Html.unescape(it.groupValues[1]).trim() }
            .filter { it.isNotEmpty() }
            .toList()
        val pdf = meta("citation_pdf_url").ifBlank { "$BASE/article/n/$slug.pdf" }

        val start = html.indexOf("itemprop=\"articleBody\"")
        var body: String
        val references: String
        if (start >= 0) {
            val endCandidates = listOf("class=\"infoblock", "class=\"footer", "class=\"content-footer")
                .mapNotNull { html.indexOf(it, start).takeIf { i -> i > start } }
            val end = endCandidates.minOrNull() ?: html.length
            body = Html.toText(html.substring(start, end))
        } else {
            body = ""
        }
        val refStart = body.indexOf("Список литературы")
        if (refStart >= 0) {
            references = body.substring(refStart)
            body = body.substring(0, refStart)
        } else {
            references = ""
        }

        val article = Article(
            slug = slug,
            title = title,
            annotation = Html.toText(meta("eprints.abstract")),
            year = year,
            journal = journal,
            authors = authors
        )
        return ArticleDetail(article, body, references, pdf)
    }
}

object Html {
    fun toText(html: String): String {
        if (html.isEmpty()) return ""
        var s = html
        s = Regex("(?is)<script.*?</script>").replace(s, " ")
        s = Regex("(?is)<style.*?</style>").replace(s, " ")
        s = Regex("(?is)<!--.*?-->").replace(s, " ")
        s = Regex("(?i)</(p|div|li|tr|h[1-6]|section|blockquote)>").replace(s, "\n\n")
        s = Regex("(?i)<br\\s*/?>").replace(s, "\n")
        s = Regex("(?i)</td>").replace(s, "\t")
        s = Regex("(?s)<[^>]+>").replace(s, " ")
        s = unescape(s)
        s = s.replace("\u00a0", " ")
        s = Regex("[ \\t]{2,}").replace(s, " ")
        s = Regex(" *\n *").replace(s, "\n")
        s = Regex("\n{3,}").replace(s, "\n\n")
        s = Regex("(?s)<[^<>]*$").replace(s, "")
        return s.trim()
    }

    fun unescape(s: String): String {
        if (s.indexOf('&') < 0) return s
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '&') {
                val semi = s.indexOf(';', i + 1)
                if (semi in (i + 1)..(i + 12)) {
                    val ent = s.substring(i + 1, semi)
                    val rep = when {
                        ent.startsWith("#x") || ent.startsWith("#X") ->
                            ent.drop(2).toIntOrNull(16)?.let { codePointToString(it) }
                        ent.startsWith("#") ->
                            ent.drop(1).toIntOrNull()?.let { codePointToString(it) }
                        else -> NAMED[ent.lowercase()]
                    }
                    if (rep != null) {
                        out.append(rep)
                        i = semi + 1
                        continue
                    }
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    private fun codePointToString(cp: Int): String? =
        if (cp in 1..0x10FFFF) String(Character.toChars(cp)) else null

    private val NAMED = mapOf(
        "quot" to "\"",
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "nbsp" to "\u00a0",
        "laquo" to "\u00ab",
        "raquo" to "\u00bb",
        "ndash" to "\u2013",
        "mdash" to "\u2014",
        "hellip" to "\u2026",
        "rsquo" to "\u2019",
        "lsquo" to "\u2018",
        "ldquo" to "\u201c",
        "rdquo" to "\u201d",
        "deg" to "\u00b0",
        "plusmn" to "\u00b1",
        "times" to "\u00d7",
        "middot" to "\u00b7",
        "bull" to "\u2022",
        "copy" to "\u00a9",
        "reg" to "\u00ae",
        "trade" to "\u2122"
    )
}
