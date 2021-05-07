---
title: "Getting Real"
date: 2021-05-07T15:18:06+09:00
weight: 2
draft: false
summary: A more realistic usage of ReKotlin
---

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
