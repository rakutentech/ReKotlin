# ReKotlin

[![License MIT](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](https://github.com/ReSwift/ReSwift/blob/master/LICENSE.md)
[![Download](https://maven-badges.herokuapp.com/maven-central/io.github.rakutentech.rekotlin/rekotlin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.rakutentech.rekotlin/rekotlin)
[![CircleCI](https://circleci.com/gh/rakutentech/ReKotlin.svg?style=svg)](https://circleci.com/gh/rakutentech/ReKotlin)
[![codecov](https://codecov.io/gh/rakutentech/ReKotlin/branch/master/graph/badge.svg)](https://codecov.io/gh/rakutentech/ReKotlin)

## Quick Start

First add the rekotlin dependency to your build script.

```gradle
dependencies {
  implementation 'io.github.rakutentech.rekotlin:rekotlin:2.1.0'
}
```

For a very simple app, that maintains a counter that can be increased and decreased, you can define the app state as following:

```kotlin
typealias AppState = Int?
```

To change the satte you define actions, one for increasing and one for decreasing the state. For the simple actions in this example we can define empty data classes that conform to action:

```kotlin
data class IncrementAction(val amount: Int = 1): Action
data class DecrementAction(val amount: Int = 1): Action
```

The reducer needs to handle actions to create the changed state:

```kotlin
fun reducer(action: Action, oldState: AppState?): AppState {
    // if no state has been provided, create the default state
    val state = oldState ?: 0

    return when(action){
        is IncrementAction ->  state + action.amount
        is DecrementAction -> state - action.amount
    }
}
```

Reducers must be pure functions without side effects.

To maintain our application state we need to wire it all up in a store:

```kotlin
val store = store(
     reducer = ::reducer,
     state = 7 // the initial state is optional
)
// or more concisely
val store = store(::reducer)
```

Lastly, your want to observe the state in order to render it to the user.

```kotlin
class CounterViewComponent(
    private val label: TextView,
    private val button: Button,
    private val store: Store<AppState>
) : Subscriber<AppState> {

    init {
        store.subscribe(this) // (1)
        // you will need to call store.unsubscribe to stop being notified of state changes,
        // for example in an onPause or onDestroy lifecycle callback.

        button.setOnClickListener { store.dispach(IncrementAction()) } // (2)
    }

    override fun newState(state: AppState) { // (3)
        label.text = "We are counting.... so far we have: ${state}"
    }
}
```

Let's look in more detail at the numbered lines:

1. Subscribing to the store will cause the `CounterViewComponent` to receive state updates from now on. At subscribe time it will be called once to receive the current state.
2. When the user clicks the button we will dispatch an action, triggering a state change in our store - which will notify `CounterViewComponent` once the reducers have determined the new state!
3. Here we render the application state for the user, whenever the state changes, we re-render it (or what has changed - it's really up to you)

Here you go, your first fully functional unidirectional data flow! 🎉

### Getting Real

Your application will have more than a meager integer as state, a more realistic state will be a tree of data classes, which contain substates. To work with sub states you can use multiple subscribers and select only what each subscriber cares about (single responsibility 😉)

```kotlin
data class AppState(
    val userState: UserState,
    val networkState: NetworkState,
    val otherState: OtherState,
    val yetAnotherState: YetAnotherState // you get the idea
)

// each state in turn is a data class, we'll skip the details for brevity

val store : Store<AppStore> = // create as above

val userStateSubscriber = subscriber { userState: UserState ->
    /* do what you have to do */
}

val syntheticSubStateSubscriber = subscriber { synthSubState: Pair<UserState, OtherState> ->
    /* do what you have to do */
}

store.subscribe(userStateSubscriber) { appState -> appState.userState } // select only the user state
store.subscribe(syntheticSubStateSubscriber) { appState ->
        Pair(appState.userState, appState.otherState) // transform into synthetic sub-state
}
```

## Full Conceptual Model

ReKotlin relies on a few principles:

- **`Store`**: maintains your entire app state in the form of a single data structure. This state can only be modified by dispatching Actions to the store. Whenever the state in the store changes, the store will notify all observers.
- **`Action`**: a declarative description of a state change. Actions don't contain any code, they are consumed by the store (or rather its reducers).
- **`Reducer`**: a pure function, implements the state transitinon on the current action and the current app state, creating the next app state
- **`Effect`**: a declarative description of an ephemeral effect that takes place. Like an action that does not change the state.
- **`Subscriber`**: anybody waiting for state updates or for effects.
- **`Middleware`**: TODO

![](docs/img/reswift_concept.png)

## Why ReKotlin?

<!-- TODO: rework this part -->

Model-View-Controller (MVC) is not a holistic application architecture. Typical apps defer a lot of complexity to controllers since MVC doesn't offer other solutions for state management, one of the most complex issues in app development.

Apps built upon MVC often end up with a lot of complexity around state management and propagation. We need to use callbacks, delegations, Key-Value-Observation and notifications to pass information around in our apps and to ensure that all the relevant views have the latest state.

This approach involves a lot of manual steps and is thus error prone and doesn't scale well in complex code bases.

It also leads to code that is difficult to understand at a glance, since dependencies can be hidden deep inside of view controllers. Lastly, you mostly end up with inconsistent code, where each developer uses the state propagation procedure they personally prefer. You can circumvent this issue by style guides and code reviews but you cannot automatically verify the adherence to these guidelines.

ReKotlin attempts to solve these problems by placing strong constraints on the way applications can be written. This reduces the room for programmer error and leads to applications that can be easily understood - by inspecting the application state data structure, the actions and the reducers.

This architecture provides further benefits beyond improving your code base:

- Stores, Reducers, Actions and extensions such as [ReKotlin Router](https://github.com/ReKotlin/rekotlin-router)  are entirely platform independent - you can easily use the same business logic and share it between apps for multiple platforms

The ReKotlin tooling is still in a very early stage, but aforementioned prospects excite us and hopefully others in the community as well!

## Contributing

<!-- TODO: proper contributers.md -->
