package ru.cyberleninka.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import ru.cyberleninka.app.R
import ru.cyberleninka.app.data.Repository

class LibraryFragment : Fragment() {

    private lateinit var repo: Repository
    private lateinit var adapter: ArticleAdapter
    private lateinit var info: TextView
    private lateinit var empty: TextView
    private var mode: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        repo = Repository.get(requireContext())
        val v = inflater.inflate(R.layout.fragment_library, container, false)
        val tabs = v.findViewById<TabLayout>(R.id.tabs)
        info = v.findViewById(R.id.info)
        empty = v.findViewById(R.id.empty)
        val swipe = v.findViewById<SwipeRefreshLayout>(R.id.swipe)
        val list = v.findViewById<RecyclerView>(R.id.list)

        tabs.addTab(tabs.newTab().setText(R.string.tab_bookmarks))
        tabs.addTab(tabs.newTab().setText(R.string.tab_history))
        tabs.addTab(tabs.newTab().setText(R.string.tab_all))

        adapter = ArticleAdapter(showAnnotation = false).attach(requireContext())
        adapter.onClick = { a ->
            startActivity(Intent(requireContext(), ReaderActivity::class.java).apply {
                putExtra(ReaderActivity.EXTRA_SLUG, a.slug)
            })
        }
        adapter.onStar = { a ->
            repo.db.toggleBookmark(a.slug)
            reload()
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                mode = tab.position
                reload()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        swipe.setOnRefreshListener { reload() }
        return v
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        val data = when (mode) {
            0 -> repo.db.bookmarks()
            1 -> repo.db.history()
            else -> repo.db.all()
        }
        adapter.submit(data)
        empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
        swipeRefreshing(false)
        info.text = getString(
            R.string.offline_counts,
            repo.db.count(),
            repo.db.countWithText(),
            repo.lastDownload()
        )
    }

    private fun swipeRefreshing(value: Boolean) {
        view?.findViewById<SwipeRefreshLayout>(R.id.swipe)?.isRefreshing = value
    }
}
