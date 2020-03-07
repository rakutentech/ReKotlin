package org.rekotlin.router

import org.rekotlin.Action

/**
 * Navigation State that you have to embed in your application state.
 *
 * ```
 * data class AppState(
 *      override val navigationState: NavigationState,
 *      // other application states such as....
 *      val otherState: OtherState,
 *      val yetAnotherState: YetAnthorState
 * )
 * ```
 */
data class NavigationState(
    val route: Route = Route(),
    val animated: Boolean = true
)

/**
 * The action you dispatch to change route.
 *
 * ```
 * store.dispatch(SetRouteAction(Route("root", "feature", "screen")))
 * ```
 */
data class SetRouteAction(val route: Route, val animated: Boolean = true) : Action

/**
 * The navigationReducer handles the [NavigationState], concerned with current navigation state.
 *
 * Delegate to this reducer from the top-level reducer of you application.
 *
 * ```
 * fun appReducer(action: Action, oldState: AppState?) : AppState {
 *     // Initialize default state if we have none yet
 *     val state = oldState ?: AppState()
 *
 *     return state.copy(
 *         navigationState = navigationReducer(action, state.navigationState),
 *         // other application state reducers such as....
 *         otherState = otherReducer(action, state.otherState),
 *         yetAnotherState = yetAnotherReducer(action, state.yetAnotherState)
 *     )
 * }
 * ```
 */
fun navigationReducer(action: Action, oldState: NavigationState?): NavigationState {
    val state = oldState ?: NavigationState()

    return when (action) {
        is SetRouteAction -> state.copy(
                route = action.route,
                animated = action.animated
        )
        else -> state
    }
}
