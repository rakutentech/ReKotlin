package org.rekotlin.sample

import android.net.Uri
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.rekotlin.*
import org.rekotlin.router.NavigationState
import org.rekotlin.router.navigationReducer

/**
 * Here we define the Redux application model:
 * * The application state
 * * The state reducer
 * * Actions that mutate the state
 *
 * Most actions trigger synchronous state changes, however [FetchRandomUser] is an example of how to
 * do asynchronous operations within Redux. It relies on the [thunkMiddleware].
 */

val appStore = store(::appReducer, null, thunkMiddleware())

data class AppState(
        val user: UserState = UserState(),
        val navigation: NavigationState = NavigationState(),
        val history: List<User> = emptyList()
)

data class User(val name: String, val imageUrl: Uri?)

data class UserState(
        val user: User = User("nemo", null),
        val loading: Boolean = false
)

data class SetUserAction(val user: User) : Action
data class SetLoadingAction(val loading: Boolean) : Action

fun historyReducer(action: Action, oldState: List<User>?): List<User> {
    val list = oldState ?: emptyList()
    return if(action is SetUserAction) list + action.user else list
}

fun userReducer(action: Action, oldState: UserState?): UserState {
    val state = oldState ?: UserState()

    return when(action) {
        is SetUserAction -> state.copy(user = action.user)
        is SetLoadingAction -> state.copy(loading = action.loading)
        else -> state
    }
}

fun appReducer(action: Action, oldState: AppState?): AppState {
    val state = oldState ?: AppState()

    return state.copy(
            user = userReducer(action, state.user),
            navigation = navigationReducer(action, state.navigation),
            history = historyReducer(action, state.history)
    )
}

class FetchRandomUser(
        private val scope: CoroutineScope,
        private val dispatcher: CoroutineDispatcher
): Thunk<AppState> {
    override fun invoke(dispatch: DispatchFunction, getState: () -> AppState?) {
        dispatch(SetLoadingAction(true))
        scope.launch {
            val randomUser = withContext(dispatcher) {
                val response = Fuel.get("https://randomuser.me/api/").awaitString()
                val person = (JSONObject(response)
                        .getJSONArray("results")[0] as JSONObject)
                val url = person
                        .getJSONObject("picture")
                        .getString("large")
                val name = person
                        .getJSONObject("name")
                        .getString("first")
                Pair(name, url.asUri())
            }

            dispatch(SetLoadingAction(false))
            dispatch(SetUserAction(User(randomUser.first, randomUser.second)))
        }
    }
}
