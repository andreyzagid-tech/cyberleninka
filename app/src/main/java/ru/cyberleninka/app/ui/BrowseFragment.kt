package ru.cyberleninka.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import ru.cyberleninka.app.R
import ru.cyberleninka.app.data.Categories
import ru.cyberleninka.app.data.Repository
import ru.cyberleninka.app.data.Sections

class BrowseFragment : Fragment() {

    private lateinit var repo: Repository
    private lateinit var state: TextView
    private lateinit var counts: TextView
    private lateinit var btnCrawl: MaterialButton
    private lateinit var btnStop: MaterialButton
    private val handler = Handler(Looper.getMainLooper())
    private val poller = object : Runnable {
        override fun run() {
            updateState()
            handler.postDelayed(this, 700)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        repo = Repository.get(requireContext())
        val v = inflater.inflate(R.layout.fragment_browse, container, false)
        state = v.findViewById(R.id.index_state)
        counts = v.findViewById(R.id.index_counts)
        btnCrawl = v.findViewById(R.id.btn_crawl_all)
        btnStop = v.findViewById(R.id.btn_stop)
        val list = v.findViewById<RecyclerView>(R.id.list)

        val adapter = CategoryAdapter { cat ->
            startActivity(Intent(requireContext(), CategoryActivity::class.java).apply {
                putExtra(CategoryActivity.EXTRA_SLUG, cat.slug)
                putExtra(CategoryActivity.EXTRA_TITLE, cat.title)
            })
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        btnCrawl.setOnClickListener {
            repo.startIndexDownload(null) { done, s ->
                handler.post { state.text = s }
            }
            handler.post { state.text = getString(R.string.crawl_started) }
        }
        btnStop.setOnClickListener {
            repo.stopIndexDownload()
            state.text = getString(R.string.crawl_stopped)
        }
        v.findViewById<TextView>(R.id.index_hint).setOnClickListener { showAboutMenu() }
        return v
    }

    private fun showAboutMenu() {
        val items = arrayOf(
            getString(R.string.about_privacy),
            getString(R.string.clear_cache)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.about_more)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> openPrivacy()
                    else -> confirmClear()
                }
            }
            .show()
    }

    private fun openPrivacy() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_url))))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.no_browser, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmClear() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_cache)
            .setMessage(R.string.confirm_clear)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                repo.stopIndexDownload()
                repo.db.clearAll()
                updateState()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        handler.post(poller)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(poller)
    }

    private fun updateState() {
        val running = repo.indexRunning
        btnCrawl.isEnabled = !running
        btnStop.isEnabled = running
        if (!running && repo.indexState.isBlank()) state.text = getString(R.string.offline_state)
        else if (running || repo.indexState.isNotBlank()) state.text = repo.indexState
        counts.text = getString(
            R.string.offline_counts,
            repo.db.count(),
            repo.db.countWithText(),
            repo.lastDownload()
        )
    }
}

class CategoryAdapter(
    private val onClick: (ru.cyberleninka.app.data.Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private val items = Sections.categories

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.title)
        private val count: TextView = view.findViewById(R.id.count)

        fun bind(cat: ru.cyberleninka.app.data.Category) {
            title.text = cat.title
            count.text = "cyberleninka.ru/article/c/${cat.slug}"
            itemView.setOnClickListener { onClick(cat) }
        }
    }
}
