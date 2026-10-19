package ru.cyberleninka.app.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.cyberleninka.app.R
import ru.cyberleninka.app.ads.Ads
import ru.cyberleninka.app.data.Api
import ru.cyberleninka.app.data.Repository

class ReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SLUG = "slug"
    }

    private lateinit var repo: Repository
    private lateinit var titleView: TextView
    private lateinit var authorsView: TextView
    private lateinit var journalView: TextView
    private lateinit var annotationView: TextView
    private lateinit var bodyView: TextView
    private lateinit var referencesView: TextView
    private lateinit var statusView: TextView
    private lateinit var progress: ProgressBar
    private lateinit var toolbar: MaterialToolbar
    private var slug: String = ""
    private var pdfUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)
        applySystemBarInsets()
        repo = Repository.get(this)
        slug = intent.getStringExtra(EXTRA_SLUG).orEmpty()

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        titleView = findViewById(R.id.title)
        authorsView = findViewById(R.id.authors)
        journalView = findViewById(R.id.journal)
        annotationView = findViewById(R.id.annotation)
        bodyView = findViewById(R.id.body)
        referencesView = findViewById(R.id.references)
        statusView = findViewById(R.id.status)
        progress = findViewById(R.id.progress)

        repo.db.touchHistory(slug)
        Ads.onArticleOpened(this)
        progress.visibility = View.VISIBLE
        statusView.text = getString(R.string.loading)
        lifecycleScope.launch {
            try {
                val full = repo.full(slug)
                render(full.article.title, full.article.authorsLine, full.article.journalLine, full.article.year,
                    full.article.annotation, full.text, full.references)
                pdfUrl = full.pdfUrl
                repo.markDownloaded()
            } catch (e: Exception) {
                progress.visibility = View.GONE
                statusView.text = getString(R.string.reader_error, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private fun render(
        title: String,
        authors: String,
        journal: String,
        year: Int,
        annotation: String,
        text: String,
        references: String
    ) {
        titleView.text = title
        authorsView.text = authors.ifBlank { "Авторы не указаны" }
        journalView.text = listOfNotNull(
            if (year > 0) year.toString() else null,
            journal.ifBlank { null },
            getString(R.string.source)
        ).joinToString("  ·  ")
        if (annotation.isNotBlank()) {
            annotationView.visibility = View.VISIBLE
            annotationView.text = annotation
        }
        bodyView.text = text
        if (references.isNotBlank()) {
            referencesView.visibility = View.VISIBLE
            referencesView.text = references
        }
        progress.visibility = View.GONE
        statusView.text = getString(R.string.text_saved)
        toolbar.title = getString(R.string.reader)
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.reader, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_bookmark)?.isChecked = runCatching { repo.db.isBookmarked(slug) }.getOrDefault(false)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_bookmark -> {
                val now = repo.db.toggleBookmark(slug)
                item.isChecked = now
                Toast.makeText(
                    this,
                    if (now) R.string.bookmark else R.string.unbookmark,
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }
            R.id.action_pdf -> {
                savePdf()
                return true
            }
            R.id.action_open -> {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${Api.BASE}/article/n/$slug")))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.no_browser, Toast.LENGTH_LONG).show()
                }
                return true
            }
            R.id.action_share -> {
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${titleView.text}\n${Api.BASE}/article/n/$slug")
                        },
                        getString(R.string.share)
                    )
                )
                return true
            }
            R.id.action_copy -> {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("url", "${Api.BASE}/article/n/$slug"))
                Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun savePdf() {
        statusView.text = getString(R.string.loading)
        lifecycleScope.launch {
            try {
                val file = repo.downloadPdf(slug) { got, total ->
                    if (total > 0) runOnUiThread { statusView.text = "PDF: ${got * 100 / total}%" }
                }
                statusView.text = getString(R.string.pdf_saved, file.name)
                val uri = FileProvider.getUriForFile(this@ReaderActivity, "$packageName.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this@ReaderActivity, R.string.no_pdf_app, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                statusView.text = getString(R.string.pdf_error, e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
