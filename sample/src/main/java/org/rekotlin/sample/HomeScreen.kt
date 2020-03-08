package org.rekotlin.sample

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.home_screen.view.*
import org.rekotlin.Subscriber

/**
 * A state observer that translates state changes into UI update calls
 */
class HomeScreenPresenter(private val view: HomeScreen) : Subscriber<AppState> {
    override fun newState(state: AppState) {
        if (state.user.user.imageUrl != null) {
            view.setImage(state.user.user.imageUrl)
        }
        view.setName(state.user.user.name)
        view.setLoading(state.user.loading)
    }
}

/**
 * A view class that hides the android specific UI code from our application logic.
 */
class HomeScreen(parent: ViewGroup) {
    val view : ViewGroup = parent.inflate(R.layout.home_screen)
    private val name : TextView = view.name
    private val image = view.image
    private val random = view.random
    private val goToHistory = view.goToHistory
    private val progress = view.progressBar

    fun setName(name: String) {
        this.name.text = name
    }

    fun setImage(url: Uri) {
        this.image.loadImageFromUrl(url)
    }

    fun setLoading(loading: Boolean) {
        this.progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    fun random(onClick: () -> Unit) {
        this.random.setOnClickListener { onClick() }
    }

    fun goToHistory(onClick: () -> Unit) {
        this.goToHistory.setOnClickListener { onClick() }
    }
}
