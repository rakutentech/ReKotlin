package org.rekotlin.sample

import android.view.ViewGroup
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.rekotlin.Store
import org.rekotlin.router.Routable
import org.rekotlin.router.Route
import org.rekotlin.router.RouteSegment
import org.rekotlin.router.SetRouteAction
import org.rekotlin.subscriber

private typealias Pop = () -> Unit

/**
 * The router responsible for initializing views and and orchestrating their presentation.
 * They create & attach/detach views and subscribe & unsubscribe presenters to state changes.
 */

class MainRouter(
    private val store: Store<AppState>,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val root: ViewGroup
) : Routable {
    private val homeScreen by lazy { HomeScreen(root) }
    private val historyScreen by lazy { HistoryScreen(root) }
    private val popStack = mutableMapOf<String, Pop>()

    override fun pushRouteSegment(routeSegment: RouteSegment, animated: Boolean): Routable {
        val pop = when (routeSegment.id) {
            "home" -> showHome()
            "history" -> showHistory()
            else -> null
        }
        pop?.let { popStack[routeSegment.id] = it }
        return this
    }

    override fun popRouteSegment(routeSegment: RouteSegment, animated: Boolean) {
        popStack[routeSegment.id]?.let {
            it()
            popStack.remove(routeSegment.id)
        }
    }

    override fun changeRouteSegment(from: RouteSegment, to: RouteSegment, animated: Boolean): Routable {
        popRouteSegment(from, animated)
        pushRouteSegment(to, animated)
        return this
    }

    private fun showHistory(): Pop {
        root.attach(historyScreen.view)

        val presenter = subscriber<List<User>> {
            historyScreen.updateHistory(it)
        }

        store.subscribe(presenter) { select { history } }

        return {
            root.detach(historyScreen.view)
            store.unsubscribe(presenter)
        }
    }

    private fun showHome(): Pop {
        root.attach(homeScreen.view)

        val presenter = HomeScreenPresenter(homeScreen)
        // in a real application these behaviors would not be in the router
        // they would be in a separate object that encapsulates user input business logic.
        // But alas, this is a sample app, introducing more indirection makes it less readable.
        homeScreen.random { store.dispatch(FetchRandomUser(scope, dispatcher)) }
        homeScreen.goToHistory { store.dispatch(SetRouteAction(Route("history"))) }

        store.subscribe(presenter)

        return {
            root.detach(homeScreen.view)
            store.unsubscribe(presenter)
        }
    }
}
