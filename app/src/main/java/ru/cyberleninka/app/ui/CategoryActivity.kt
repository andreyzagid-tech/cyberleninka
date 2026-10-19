package ru.cyberleninka.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import ru.cyberleninka.app.R
import ru.cyberleninka.app.ads.Ads
import ru.cyberleninka.app.data.Article
import ru.cyberleninka.app.data.Repository

class CategoryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SLUG = "slug"
        const val EXTRA_TITLE = "title"
    }

    private lateinit var repo: Repository
    private lateinit var adapter: ArticleAdapter
    private lateinit var info: TextView
    private lateinit var empty: TextView
    private lateinit var swipe: SwipeRefreshLayout
    private var slug: String = ""
    private var page: Int = 1
    private var loading: Boolean = false
    private var reachedEnd: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)
        applySystemBarInsets()
        repo = Repository.get(this)
        slug = intent.getStringExtra(EXTRA_SLUG).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = title.ifBlank { slug }
        toolbar.setNavigationOnClickListener { finish() }

        info = findViewById(R.id.info)
        empty = findViewById(R.id.empty)
        swipe = findViewById(R.id.swipe)

        adapter = ArticleAdapter(showAnnotation = false).attach(this)
        adapter.onClick = { open(it.slug) }
        adapter.onStar = { a ->
            repo.db.toggleBookmark(a.slug)
            adapter.notifyDataSetChanged()
        }
        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = rv.layoutManager as LinearLayoutManager
                if (lm.findLastVisibleItemPosition() >= adapter.itemCount - 3) loadNext()
            }
        })

        swipe.setOnRefreshListener {
            page = 1
            reachedEnd = false
            loadNext()
        }

        findViewById<FloatingActionButton>(R.id.fab_crawl).setOnClickListener {
            repo.startIndexDownload(slug) { done, s ->
                runOnUiThread { info.text = s }
            }
            Toast.makeText(this, getString(R.string.crawl_category_started, title), Toast.LENGTH_LONG).show()
        }

        loadNext()
    }

    override fun onResume() {
        super.onResume()
        Ads.showBanner(this)
    }

    private fun open(slug: String) {
        startActivity(Intent(this, ReaderActivity::class.java).apply {
            putExtra(ReaderActivity.EXTRA_SLUG, slug)
        })
    }

    private fun loadNext() {
        if (loading || reachedEnd) return
        loading = true
        swipe.isRefreshing = page == 1
        info.text = getString(R.string.loading)
        lifecycleScope.launch {
            try {
                val batch: List<Article> = repo.categoryPage(slug, page)
                if (batch.isEmpty()) {
                    reachedEnd = true
                } else if (page == 1) {
                    adapter.submit(batch)
                } else {
                    adapter.append(batch)
                }
                if (batch.size < 20) reachedEnd = true
                page++
                info.text = if (reachedEnd) "${title()} — всего ${adapter.itemCount}" else "${title()} — стр. ${page - 1}"
                empty.visibility = if (adapter.itemCount == 0) android.view.View.VISIBLE else android.view.View.GONE
            } catch (e: Exception) {
                info.text = getString(R.string.reader_error, e.message ?: e.javaClass.simpleName)
            } finally {
                loading = false
                swipe.isRefreshing = false
            }
        }
    }

    private fun title(): String = intent.getStringExtra(EXTRA_TITLE).orEmpty()
}
