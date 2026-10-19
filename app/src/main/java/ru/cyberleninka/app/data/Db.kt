package ru.cyberleninka.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Db(context: Context) : SQLiteOpenHelper(context.applicationContext, "cyber.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE articles(
              slug TEXT PRIMARY KEY,
              title TEXT NOT NULL,
              authors TEXT,
              year INTEGER,
              journal TEXT,
              category TEXT,
              annotation TEXT,
              text TEXT,
              refs TEXT,
              updated INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_articles_year ON articles(year)")
        db.execSQL("CREATE INDEX idx_articles_cat ON articles(category)")
        db.execSQL("CREATE INDEX idx_articles_title ON articles(title)")
        db.execSQL("CREATE TABLE bookmarks(slug TEXT PRIMARY KEY, added INTEGER)")
        db.execSQL("CREATE TABLE history(slug TEXT PRIMARY KEY, seen INTEGER)")
        db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    // ---------- articles ----------

    fun upsertArticles(list: List<Article>, withText: Boolean = false): Int {
        if (list.isEmpty()) return 0
        val db = writableDatabase
        db.beginTransaction()
        var n = 0
        try {
            for (a in list) {
                val cv = ContentValues().apply {
                    put("slug", a.slug)
                    put("title", a.title)
                    put("authors", a.authorsLine)
                    put("year", a.year)
                    put("journal", a.journal)
                    put("category", a.category)
                    put("annotation", a.annotation)
                    put("text", a.text ?: existingText(db, a.slug))
                    put("updated", System.currentTimeMillis())
                }
                val id = db.insertWithOnConflict("articles", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                if (id >= 0) n++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return n
    }

    private fun existingText(db: SQLiteDatabase, slug: String): String? {
        db.rawQuery("SELECT text FROM articles WHERE slug=?", arrayOf(slug)).use { c ->
            return if (c.moveToFirst()) c.getString(0) else null
        }
    }

    fun saveText(slug: String, text: String) {
        val cv = ContentValues().apply {
            put("text", text)
            put("updated", System.currentTimeMillis())
        }
        writableDatabase.update("articles", cv, "slug=?", arrayOf(slug))
    }

    fun saveFullText(slug: String, text: String, references: String = "") {
        val cv = ContentValues().apply {
            put("text", text)
            put("refs", references)
            put("updated", System.currentTimeMillis())
        }
        writableDatabase.update("articles", cv, "slug=?", arrayOf(slug))
    }

    fun article(slug: String): Article? = readableDatabase.rawQuery(
        "SELECT slug,title,authors,year,journal,category,annotation,text FROM articles WHERE slug=?",
        arrayOf(slug)
    ).use { c ->
        if (!c.moveToFirst()) null else c.toArticle()
    }

    fun searchLocal(query: String, limit: Int = 200): List<Article> {
        val q = "%${query.trim()}%"
        return readableDatabase.rawQuery(
            "SELECT slug,title,authors,year,journal,category,annotation,text FROM articles " +
                "WHERE title LIKE ? OR authors LIKE ? ORDER BY year DESC LIMIT $limit",
            arrayOf(q, q)
        ).use { c -> c.mapAll() }
    }

    fun all(limit: Int = 500, offset: Int = 0): List<Article> = readableDatabase.rawQuery(
        "SELECT slug,title,authors,year,journal,category,annotation,text FROM articles " +
            "ORDER BY updated DESC LIMIT $limit OFFSET $offset",
        null
    ).use { c -> c.mapAll() }

    fun byCategory(category: String, limit: Int = 500, offset: Int = 0): List<Article> =
        readableDatabase.rawQuery(
            "SELECT slug,title,authors,year,journal,category,annotation,text FROM articles " +
                "WHERE category=? ORDER BY year DESC LIMIT $limit OFFSET $offset",
            arrayOf(category)
        ).use { c -> c.mapAll() }

    fun count(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM articles", null).use { c ->
        if (c.moveToFirst()) c.getInt(0) else 0
    }

    fun countWithText(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM articles WHERE text IS NOT NULL AND LENGTH(text)>500", null)
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun slugsWithoutText(limit: Int): List<String> = readableDatabase.rawQuery(
        "SELECT slug FROM articles WHERE (text IS NULL OR LENGTH(text)<500) LIMIT $limit",
        null
    ).use { c ->
        val out = ArrayList<String>()
        while (c.moveToNext()) out.add(c.getString(0))
        out
    }

    // ---------- bookmarks & history ----------

    fun isBookmarked(slug: String): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM bookmarks WHERE slug=?", arrayOf(slug)
    ).use { it.moveToFirst() }

    fun toggleBookmark(slug: String): Boolean {
        val db = writableDatabase
        return if (isBookmarked(slug)) {
            db.delete("bookmarks", "slug=?", arrayOf(slug))
            false
        } else {
            db.insertWithOnConflict(
                "bookmarks", null,
                ContentValues().apply { put("slug", slug); put("added", System.currentTimeMillis()) },
                SQLiteDatabase.CONFLICT_REPLACE
            )
            true
        }
    }

    fun bookmarks(): List<Article> = readableDatabase.rawQuery(
        "SELECT a.slug,a.title,a.authors,a.year,a.journal,a.category,a.annotation,a.text FROM articles a " +
            "JOIN bookmarks b ON b.slug=a.slug ORDER BY b.added DESC",
        null
    ).use { c -> c.mapAll() }

    fun touchHistory(slug: String) {
        writableDatabase.insertWithOnConflict(
            "history", null,
            ContentValues().apply { put("slug", slug); put("seen", System.currentTimeMillis()) },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun history(limit: Int = 100): List<Article> = readableDatabase.rawQuery(
        "SELECT a.slug,a.title,a.authors,a.year,a.journal,a.category,a.annotation,a.text FROM articles a " +
            "JOIN history h ON h.slug=a.slug ORDER BY h.seen DESC LIMIT $limit",
        null
    ).use { c -> c.mapAll() }

    // ---------- meta ----------

    fun setMeta(key: String, value: String) {
        writableDatabase.insertWithOnConflict(
            "meta", null,
            ContentValues().apply { put("k", key); put("v", value) },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getMeta(key: String, def: String = ""): String = readableDatabase.rawQuery(
        "SELECT v FROM meta WHERE k=?", arrayOf(key)
    ).use { c -> if (c.moveToFirst()) c.getString(0) ?: def else def }

    fun clearAll() {
        writableDatabase.run {
            delete("articles", null, null)
            delete("bookmarks", null, null)
            delete("history", null, null)
            delete("meta", null, null)
        }
    }

    // ---------- cursor helpers ----------

    private fun Cursor.toArticle(): Article {
        val authors = getString(2).orEmpty()
        return Article(
            slug = getString(0),
            title = getString(1).orEmpty().ifBlank { "Без названия" },
            authors = if (authors.isBlank()) emptyList() else authors.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            year = getInt(3),
            journal = getString(4).orEmpty(),
            category = getString(5).orEmpty(),
            annotation = getString(6).orEmpty(),
            text = if (isNull(7)) null else getString(7)
        )
    }

    private fun Cursor.mapAll(): List<Article> {
        val out = ArrayList<Article>()
        while (moveToNext()) out.add(toArticle())
        return out
    }
}
