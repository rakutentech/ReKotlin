---
title: "Quick Start"
date: 2021-05-07T15:18:06+09:00
weight: 1
draft: false
summary: Setup ReKotlin dependency and start sending your first Actions!
---

First add the ReKotlin dependency to your build script.

```groovy
dependencies {
  implementation 'io.github.rakutentech.rekotlin:rekotlin:2.1.0'
}
```

For a very simple app, that maintains a counter that can be increased and decreased, you can define the app state as following:

```kotlin
typealias AppState = Int?
```

To change the state you define actions, one for increasing and one for decreasing the state. For the simple actions in this example we can define empty data classes that conform to action:

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