package org.rekotlin.router

import org.rekotlin.Subscriber

// public API

// TODO: KDoc
typealias UiThreadHandler = (() -> Unit) -> Unit

/**
 * Create the application router. Don't forget to subscribe it to the store.
 *
 * ```kotlin
 * val router = router(rootRoutable)
 * store.subscribe(router) { select { navigationState } }
 * ```
 *
 * @param rootRoutable: The [Routable] at the root of your navigation tree
 * @param uiThreadHandler: optional function that is invoked to execute on the UI thread
 */
fun router(
    rootRoutable: Routable,
    uiThreadHandler: UiThreadHandler = { it() }
): Subscriber<NavigationState> =
        Router(rootRoutable, uiThreadHandler)

// internal model

internal sealed class RoutingAction

internal data class Push(
    val routableIndex: Int,
    val segmentToPush: RouteSegment
) : RoutingAction()

internal data class Pop(
    val routableIndex: Int,
    val segmentToPop: RouteSegment
) : RoutingAction()

internal data class Change(
    val routableIndex: Int,
    val segmentTeReplace: RouteSegment,
    val newSegment: RouteSegment
) : RoutingAction()

internal class Router(
    rootRoutable: Routable,
    private val uiThreadHandler: UiThreadHandler = { it() }
//        private val mainThreadHandler: Handler = Handler(Looper.getMainLooper()) // for testing
) : Subscriber<NavigationState> {

    private var previousRoute: Route = Route()

    // TODO: Collections.synchronizedList vs CopyOnWriteArrayList
    // var routables: List<Routable> = Collections.synchronizedList(arrayListOf<Routable>())
    private val routables: MutableList<Routable> = mutableListOf()

    init {
        this.routables.add(rootRoutable)
    }

    override fun newState(state: NavigationState) {
        val routingActions = routingActionsForTransitionFrom(previousRoute, state.route)
        if (routingActions.isNotEmpty()) {
            routingActions.forEach { routingAction ->
                routingSerialActionHandler(routingAction, state)
            }
            previousRoute = state.route // do we need a deep copy?
        }
    }

    private fun routingSerialActionHandler(action: RoutingAction, state: NavigationState) {
        synchronized(lock = routables) {
            when (action) {
                is Pop -> uiThreadHandler {
                    routables[action.routableIndex].popRouteSegment(action.segmentToPop, state.animated)
                    routables.removeAt(action.routableIndex + 1)
                }

                is Push -> uiThreadHandler {
                    val newRoutable =
                            routables[action.routableIndex].pushRouteSegment(
                                    action.segmentToPush, state.animated)
                    routables.add(newRoutable)
                }

                is Change -> uiThreadHandler {
                    routables[action.routableIndex + 1] =
                            routables[action.routableIndex].changeRouteSegment(
                                    from = action.segmentTeReplace,
                                    to = action.newSegment,
                                    animated = state.animated)
                }
            }
        }
    }
}

// Route Transformation Logic
private fun largestCommonSubroute(oldRoute: Route, newRoute: Route): Int {
    var index = -1

    while (
            index + 1 < newRoute.count && index + 1 < oldRoute.count &&
            newRoute[index + 1] == oldRoute[index + 1]
    ) {
        index += 1
    }

    return index + 1
}

internal fun routingActionsForTransitionFrom(oldRoute: Route, newRoute: Route): List<RoutingAction> {

    if (oldRoute.segments == newRoute.segments) {
        return emptyList()
    }

    val routingActions = mutableListOf<RoutingAction>()

    // Find the last common subroute between two routes
    val commonSubroute = largestCommonSubroute(oldRoute, newRoute)

    // Keeps track which element of the routes we are working on
    // We start at the end of the old route
    var index = oldRoute.count - 1

    // Pop all route segments of the old route that are no longer in the new route
    // Stop one element ahead of the commonSubroute. When we are one element ahead of the
    // common subroute we have three options:
    //
    // 1. The old route had an element after the commonSubroute and the new route does not
    //    we need to pop the route segment after the commonSubroute
    // 2. The old route had no element after the commonSubroute and the new route does, we
    //    we need to push the route segment(s) after the commonSubroute
    // 3. The new route has a different element after the commonSubroute, we need to replace
    //    the old route element with the new one
    while (index > commonSubroute) {
        val routeSegmentToPop = oldRoute[index]

        val popAction = Pop(index - 1 + 1, routeSegmentToPop)
        routingActions.add(popAction)
        index -= 1
    }

    // This is the 3. case:
    // "The new route has a different element after the commonSubroute, we need to replace
    //  the old route element with the new one"
    if ((oldRoute.count > commonSubroute) && (newRoute.count > commonSubroute)) {
        val changeAction =
                Change(commonSubroute, oldRoute[commonSubroute], newRoute[commonSubroute])

        routingActions.add(changeAction)
    }
    // This is the 1. case:
    // "The old route had an element after the commonSubroute and the new route does not
    //  we need to pop the route segment after the commonSubroute"
    else if (oldRoute.count > newRoute.count) {
        val popAction = Pop(index, oldRoute[index])

        // routingActions = routingActions.plus(popAction)
        routingActions.add(popAction)
        index -= 1
    }

    // Push remainder of elements in new Route that weren't in old Route, this covers
    // the 2. case:
    // "The old route had no element after the commonSubroute and the new route does,
    //  we need to push the route segment(s) after the commonSubroute"

    while (index < newRoute.count - 1) {
        val routeSegmentToPush = newRoute[index + 1]

        val pushAction = Push(index + 1, routeSegmentToPush)

        routingActions.add(pushAction)
        index += 1
    }

    return routingActions
}
