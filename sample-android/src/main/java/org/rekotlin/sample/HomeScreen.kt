package org.rekotlin.sample

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

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
    val view: ViewGroup = parent.inflate(R.layout.home_screen)
    private val name: TextView = view.findViewById(R.id.name)
    private val image: ImageView = view.findViewById(R.id.image)
    private val random: Button = view.findViewById(R.id.random)
    private val goToHistory: Button = view.findViewById(R.id.goToHistory)
    private val progress: ProgressBar = view.findViewById(R.id.progressBar)

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
