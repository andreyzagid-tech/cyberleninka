package ru.cyberleninka.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import ru.cyberleninka.app.R
import ru.cyberleninka.app.data.Repository

class SearchFragment : Fragment() {

    private lateinit var repo: Repository
    private lateinit var adapter: ArticleAdapter
    private lateinit var info: TextView
    private lateinit var empty: TextView
    private lateinit var swipe: SwipeRefreshLayout
    private var query: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        repo = Repository.get(requireContext())
        val v = inflater.inflate(R.layout.fragment_search, container, false)
        val field = v.findViewById<TextInputEditText>(R.id.query)
        info = v.findViewById(R.id.info)
        empty = v.findViewById(R.id.empty)
        swipe = v.findViewById(R.id.swipe)
        val list = v.findViewById<RecyclerView>(R.id.list)

        adapter = ArticleAdapter().attach(requireContext())
        adapter.onClick = { open(it.slug) }
        adapter.onStar = { a ->
            repo.db.toggleBookmark(a.slug)
            adapter.notifyDataSetChanged()
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch(field.text?.toString().orEmpty().trim())
                true
            } else false
        }
        empty.setOnClickListener { field.requestFocus() }
        swipe.setOnRefreshListener { runSearch(query, quiet = true) }
        return v
    }

    private fun runSearch(q: String, quiet: Boolean = false) {
        if (q.isBlank()) {
            swipe.isRefreshing = false
            empty.visibility = View.VISIBLE
            return
        }
        query = q
        if (!quiet) swipe.isRefreshing = true
        info.visibility = View.VISIBLE
        info.text = getString(R.string.loading)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = repo.search(q)
                val root = view ?: return@launch
                adapter.submit(res.articles)
                info.text = getString(R.string.search_result, res.found, res.articles.size)
                empty.visibility = if (res.articles.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                val root = view ?: return@launch
                info.text = getString(R.string.search_error, e.message ?: e.javaClass.simpleName)
                empty.visibility = View.VISIBLE
            } finally {
                view?.let { swipe.isRefreshing = false }
            }
        }
    }

    private fun open(slug: String) {
        startActivity(Intent(requireContext(), ReaderActivity::class.java).apply {
            putExtra(ReaderActivity.EXTRA_SLUG, slug)
        })
    }
}
