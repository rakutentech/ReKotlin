package org.rekotlin.sample

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import org.rekotlin.router.Route
import org.rekotlin.router.SetRouteAction
import org.rekotlin.router.router

/**
 * The main activity is responsible for wiring together the android framework and our application
 * at runtime. Note that the activity is not responsible for presentation or business logic.
 * It is pure adapter code adapting our application to the android APIs.
 */
class MainActivity : AppCompatActivity() {
    private val store = appStore
    private val routable by lazy { MainRouter(store, MainScope(), Dispatchers.IO, root) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * Explicitly configuring a main thread handler allows you to dispatch [SetRouteAction] from
         * any thread, e.g. even in a [Thunk] that's operating on a background thread.
         *
         * If that is a good idea is up to you. 😉
         */
        val handler = Handler(Looper.getMainLooper())
        val router = router(routable) { handler.post(it) }
        store.subscribe(router) { select { navigation } }
        store.dispatch(SetRouteAction(Route("home")))

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (store.state.navigation.route.segments.last().id == "home") {
                    finish()
                }
                store.dispatch(SetRouteAction(Route("home")))
            }
        })
    }
}
