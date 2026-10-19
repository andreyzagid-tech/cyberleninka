package ru.cyberleninka.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.cyberleninka.app.R
import ru.cyberleninka.app.data.Article
import ru.cyberleninka.app.data.Repository

class ArticleAdapter(
    private val showAnnotation: Boolean = true,
    var onClick: (Article) -> Unit = {},
    var onStar: (Article) -> Unit = {}
) : RecyclerView.Adapter<ArticleAdapter.VH>() {

    private val items = ArrayList<Article>()
    private val repo by lazy { Repository.get(appContext) }

    private lateinit var appContext: android.content.Context

    fun attach(context: android.content.Context): ArticleAdapter {
        if (!::appContext.isInitialized) appContext = context.applicationContext
        return this
    }

    fun submit(list: List<Article>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun append(list: List<Article>) {
        val start = items.size
        items.addAll(list)
        notifyItemRangeInserted(start, list.size)
    }

    fun itemAt(pos: Int): Article? = items.getOrNull(pos)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_article, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.title)
        private val authors: TextView = view.findViewById(R.id.authors)
        private val meta: TextView = view.findViewById(R.id.meta)
        private val annotation: TextView = view.findViewById(R.id.annotation)
        private val star: ImageButton = view.findViewById(R.id.star)

        fun bind(a: Article) {
            title.text = a.title
            authors.text = a.authorsLine.ifBlank { "Авторы не указаны" }
            val parts = ArrayList<String>()
            if (a.year > 0) parts.add(a.year.toString())
            if (a.journal.isNotBlank()) parts.add(a.journal)
            if (!a.text.isNullOrBlank()) parts.add("• оффлайн")
            meta.text = parts.joinToString("  ·  ")
            if (showAnnotation && a.annotation.isNotBlank()) {
                annotation.visibility = View.VISIBLE
                annotation.text = a.annotation
            } else {
                annotation.visibility = View.GONE
            }
            val marked = if (::appContext.isInitialized) runCatching { repo.db.isBookmarked(a.slug) }.getOrDefault(false) else false
            star.setImageResource(if (marked) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
            star.setOnClickListener { onStar(a) }
            itemView.setOnClickListener { onClick(a) }
        }
    }
}
