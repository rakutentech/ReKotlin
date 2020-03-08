package org.rekotlin.sample

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.history_screen.view.historyList

/**
 * A view class that hides the android specific UI code from our application logic.
 */
class HistoryScreen(parent: ViewGroup) {
    val view: ViewGroup = parent.inflate(R.layout.history_screen)
    private val recycler = view.historyList
    private val adapter = HistoryAdapter()

    init {
        recycler.layoutManager = LinearLayoutManager(parent.context)
        recycler.adapter = adapter
    }

    fun updateHistory(history: List<User>) {
        adapter.history = history
    }
}

private data class HistoryViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

private class HistoryAdapter : RecyclerView.Adapter<HistoryViewHolder>() {
    var history = emptyList<User>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder =
            HistoryViewHolder(parent.inflate(R.layout.history_cell))

    override fun getItemCount(): Int = history.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.textView.text = history[position].name
    }
}
