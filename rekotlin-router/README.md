# ReKotlin-Router

[![License MIT](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](https://github.com/ReSwift/ReSwift/blob/master/LICENSE.md)
[![Download](https://maven-badges.herokuapp.com/maven-central/io.github.rakutentech.rekotlin/rekotlin-router/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.rakutentech.rekotlin/rekotlin-router)
[![CircleCI](https://circleci.com/gh/rakutentech/ReKotlin.svg?style=svg)](https://circleci.com/gh/rakutentech/ReKotlin)
[![codecov](https://codecov.io/gh/rakutentech/ReKotlin/branch/master/graph/badge.svg)](https://codecov.io/gh/rakutentech/ReKotlin)

A declarative router for [ReKotlin](https://github.com/rakutentech/ReKotlin). Declare routes like URL paths on the web.

With ReKotlin-Router you can navigate your app by defining the target route in the form of a URL-path-like sequence of identifiers:

```kotlin
val route = Route("login", "repoList") // mirrors /login/repolist URL path
store.dispatch(SetRouteAction(action))

val nextRoute = route + RouteSegment("repoDetail", 123L) // Route with parameter, similar to URL query parameters
store.dispatch(SetRouteAction(nextRoute))
```

## About ReKotlin-Router

In ReKotlin **all** state changes are triggred by actions - this includes changes to the navigation state. As a consequence the current navigation state is part of the overall app state. Moreovere you use actions to trigger navigation changes.

## Quickstart

First add the rekotlin dependency to your build script.

```groovy
dependencies {
  implementation 'io.github.rakutentech.rekotlin:rekotlin-router:1.0.0'
}
```

Next add the navigation state to your application state.

```kotlin
import org.rekotlin.router.NavigationState

data class AppState(
    override val navigationState: NavigationState,
    // other application states such as....
    val otherState: OtherState,
    val yetAnotherState: YetAnthorState
)
```

Next use the navigation state reducer in your application's top level reducer. Delegate all navigation state changes to the libraries reducer.

```kotlin
import org.rekotlin.router.navigationReducer

fun appReducer(action: Action, oldState: AppState?) : AppState {
    // Initialize default state if we have none yet
    val state = oldState ?: AppState()

    return state.copy(
        navigationState = navigationReducer(action, state.navigationState),
        // other application state reducers such as....
        otherState = otherReducer(action, state.otherState),
        yetAnotherState = yetAnotherReducer(action, state.yetAnotherState)
    )
}
```

Next create a `router` and subscribe it to your `store`, so it can react to navigation state changes. In order to create a `router` you need to pass a `Routable` - it is the root of your routing tree - and a `MainThreadHandler` to allow the library to reliably execute on the main thread to iniate navigation changes.

```kotlin
// create router
val router = router(rootRoutable = /* your root routable */, Handler(Looper.getMainLooper())::post)
// subscribe it to navigationState changes
store.subscribe(router) { select { navigationState } }
```

Now you're all set for routing 🎉, you can set a route by dispatching a `SetRouteAction`

```kotlin
store.dispatch(SetRouteAction(Route("home", "user")))
```

But where does the actual UI routing happing you might ask - which brings us to the `Routable` interface!

## Implementing `Routable`

A Route is similar to a URL path, it is a sequence of identifiers e.g. `["home", "user", "user-detail"]` - in a web url this might be `/home/user/user-detail/`. In ReKotlin-Router we use `Routable`s to implement the UI interaction to each route segment.

Each route segment mas to a `Routable` - that routable is responsible for that segment. For example

* `"home" -> HomeRoutable`
* `"user" -> UserRoutable`
* `"user-detail" -> UserDetailRoutable`

The `Routable` needs to present a child, hide a child or replace a child with another child. In our example the `HomeRoutable` needs to present the `UserRoutable`.

Each `Routable` implements the following interface:

```kotlin
interface Routable {
    fun pushRouteSegment(routeSegment: RouteSegment, animated: Boolean = false): Routable
    fun popRouteSegment(routeSegment: RouteSegment, animated: Boolean = false)
    fun changeRouteSegment(from: RouteSegment, to: RouteSegment, animated: Boolean = false): Routable
}
```

The `Routable` you pass to `router`  is the root of your navigation tree, it is responsible for the first route segment, e.g. `"home"`.

Whenever a `Routable` pushes a new route segment, it returns a new `Routable`. That `Routeable` will be responsible for managing the presented segment. For example if you want to navigate from `["home"]` to `["home", "users"]` the `HomeRoutable` segment will be asked to present the `"user"` segment. `HomeRoutable` should present the user detail UI and return the `UserRoutable`

If your navigation stack uses a modal presentation for this transition, the implementation of `Routable` for the `"Root"` segment might look like this:

```kotlin
class HomeRoutable: Routable {
    private val userRoutable: UserRoutable = /* dependency injection */

    override fun popRouteSegment(routeSegment: RouteSegment, animated: Boolean) =
        TODO("not implemented")

    override fun pushRouteSegment(routeSegment: RouteSegment, animated: Boolean) =
        if(routeSegment.id == "user") {
            userRoutable
        } else {
            /* default child routable */
        }

    override fun changeRouteSegment(from: RouteSegment, to: RouteSegment, animated: Boolean): Routable =
       TODO("not implemented")
}
```

## Changing the Current Route

To change the current route you dispatch a `SetRouteAction` with an absolute route.

```kotlin
val action = SetRouteAction(Route("home", "user"))
store.dispatch(action)
```

Similar to URL query parameters `Route`s can have parameters.

```kotlin
val userDetailSegment = RouteSegment("userDetail" to mapOf("id" to 123, "name" to "nemo"))
val route = Route("home", "user") + userDetailSegment
store.dispatch(SetRouteAction(route))
```

The `Routable` will receive a `RouteSegment` with all arguments.

## Contributing

There's still a lot of work to do here! We would love to see you involved!

### Submitting patches

The best way to submit a patch is to [fork the project on github](https://help.github.com/articles/fork-a-repo/) then send us a
[pull request](https://help.github.com/articles/creating-a-pull-request/) via [github](https://github.com).

Before submitting the pull request, make sure all existing tests are passing, and add the new test if it is required.
