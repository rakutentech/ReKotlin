package org.rekotlin

/**
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * A marker for anything that can be dispatched to a [Store]
 */
interface Dispatchable

/**
 * Marker interface for actions dispatched to a [Store].
 *
 * Actions initiate state changes in the store, once the state change all subscribes of the store
 * will be notified with the new, changed state.
 *
 * All actions need to be marked with this interface.
 */
interface Action : Dispatchable

/**
 * Marker interface for effects dispatched to a [Store].
 *
 * Effects are ephemeral, they do not change the store's state, however listeners can react to them.
 * Effects are a concept with ephemeral application behavior in mind, such as triggering an
 * animation, showing a toast or showing a snackbar.
 *
 * All effects need to be marked with this interface.
 */
interface Effect : Dispatchable

/**
 * A function that produces the next state, based on the previous state and an incoming [Action].
 */
typealias Reducer<State> = (action: Action, state: State?) -> State

/**
 * A function that dispatches an [Action] or [Effect].
 *
 * You typically get one of these from [Store.dispatchFunction], it may be useful for dependency
 * injection.
 */
typealias DispatchFunction = (Dispatchable) -> Unit

/**
 * A middleware intercepts all actions dispatched to the store before they reach the reducers.
 *
 * Middlewares have access to the previous state, can drop actions or dispatch others (potentially
 * multiple others. This makes middlewares very powerful. Any middleware will affect all actions
 * dispatched to the store, so use them for cross-cutting concerns only.
 */
typealias Middleware<State> = (DispatchFunction, () -> State?) -> (DispatchFunction) -> DispatchFunction

/**
 * A state subscriber, waiting for state changes.
 *
 * Subscribe to the [Store] via [Store.subscribe].
 */
interface Subscriber<State> {
    /**
     * Whenever the state changes the store will notify the subscriber by calling [newState].
     *
     * @param state: new state
     */
    fun newState(state: State)
}

/**
 * An effect listener, waiting for effects dispatched through the store.
 *
 * Subscribe to the [Store] via [Store.subscribe].
 */
interface Listener<Effect> {
    /**
     * Whenever an effect is dispatched the store will notify the listener by calling [onEffect].
     *
     * @param effect: effect to react to
     */
    fun onEffect(effect: Effect)
}

/**
 * A Store that allows to dispatch actions to it.
 *
 * This can be a helpful facade to hide the [SubscribeStore] functionality.
 */
interface DispatchStore {
    /**
     * Dispatch an [Action] or [Effect] to the store.
     *
     * An Action is the simplest way to initiate state changes.
     * This passes the action to [Middleware], delegates to the [Reducer] to determine the new state.
     * and eventually notifies [Subscriber]s of (sub-)state changes (if any).
     *
     * Effects are one time events that do not change the state.
     * This notifies all [Effect] listeners.
     *
     * This <b>will not</b> pass anything to [Middleware], delegate to [Reducer] or change state.
     *
     *
     * Example of dispatching an action:
     *
     * ```
     * // dispatch an action
     * store.dispatch(CounterAction.IncreaseCounter)
     * // dispatch an effect
     * store.dispatch(AnimationEffect
     * ```
     *
     *
     * @param dispatchable an action or effect to dispatch to the store
     */
    fun dispatch(dispatchable: Dispatchable)

    /**
     * The dispatch function to dispatch [Action]s or [Effect]s.
     * Use the property if you want to use only a dispatch function and not the full store, for
     * example for dependency injection.
     */
    val dispatchFunction: DispatchFunction
}

/**
 * A store that allows subscribing to its state changes.
 *
 * This can be a helpful facade to hide the [DispatchStore] functionality.
 */
interface SubscribeStore<State> {
    /**
     * Subscribes the state changes of this store.
     * Subscribers will receive a call to [Subscriber.newState] whenever the state changes.
     *
     * @param subscriber: [Subscriber] that will receive store updates
     */
    fun <S : Subscriber<State>> subscribe(subscriber: S)

    /**
     * Subscribe a subscriber to this store.
     *
     * Subscribers will receive a call to [Subscriber.newState] whenever the state changes and
     * the [selector] decides to forward the state update.
     *
     * @param subscriber Subscriber that will receive store updates
     * @param selector A closure that receives a [Subscription] and can transform it by selecting
     * sub-states or skipping updates conditionally.
     */
    fun <SelectedState, S : Subscriber<SelectedState>> subscribe(
        subscriber: S,
        selector: Subscription<State>.() -> Subscription<SelectedState>
    )

    /**
     * Unsubscribe a subscriber.
     *
     * The subscriber will no longer receive updates from this store.
     *
     * @param subscriber: [Subscriber] that will be unsubscribed
     */
    fun <SelectedState> unsubscribe(subscriber: Subscriber<SelectedState>)

    /**
     * Subscribe to all effects dispatched to this store.
     * Listeners will receive a call to [Listener.onEffect] whenever an effect is dispatched
     *
     * @param listener: the [Listener] that will receive effects
     */
    fun subscribe(listener: Listener<Effect>)

    /**
     * Subscribe to selected effects dispatched to this store.
     * Listeners will receive a call to [Listener.onEffect] whenever an effect is dispatched and
     * the [selector] converts it into a non-null value.
     *
     * @param listener: the [Listener] that will receive effects
     * @param selector: selector closure that selects a subset of effects from all effects
     * dispatched to this store
     */
    fun <E : Effect> subscribe(listener: Listener<E>, selector: (Effect) -> E?)

    /**
     * Unsubscribe a listener.
     *
     * The listener will no longer receive effects.
     *
     * @param listener: [Listener] that will be unsubscribed
     */
    fun <E : Effect> unsubscribe(listener: Listener<E>)
}

/**
 * A root store that can spawn child stores.
 *
 * Children can bootstrap from the root store without the root knowing the children at compile time.
 *
 * This is useful when an application is structured like a tree, with a single root module, that
 * owns shared state used by other features, for example in the diagram below
 *
 * <pre>
 * +---+ +---+ +---+
 * | F | | F | | F |
 * | e | | e | | e |
 * | a | | a | | a |
 * | t | | t | | t |
 * |   | |   | |   |
 * | 1 | | 2 | | 3 |
 * +---+ +---+ +---+
 * +---------------+
 * |     Root      |
 * +---------------+
 * </pre>
 *
 * Root, feature 1, feature 2 and feature 3 can be different gradle modules, with the features
 * depending on the root. If the root injects a [RootStore] into the feature modules, each feature
 * can use [RootStore.childStore] or [RootStore.plus] to create its own child store.
 *
 * ```
 * val rootStore: RootStore<RootState> = ... // dependency injection however you like
 * val featureStore: Store<Pair<RootState, FeatureState>> =
 *          rootStore.childStore(::childReducer, initialChildState)
 * // or shorthand (without initial state) you can use the `+` operator
 * val featureStore: Store<Pair<RootState, FeatureState>> = rootStore + ::childReducer
 * ```
 *
 * Any [Action] or [Effect] dispatched to a child will pass through the parent and all its children.
 * Any [Action] or [Effect] dispatched to the parent will pass trhough the parent and all its children.
 *
 * Subscribers to child stores only see the local view of root state + child state, not the global
 * application state.
 *
 * Subscribers to the root store only see the root state.
 *
 * A limitation of child stores is that they don't have their own middlewares. You have to
 * declare all [Middleware]s in the root store, all [Action]s passed to any of the stores (parent or
 * child) will pass through the root store's [Middleware]s before reaching any reducers.
 */
interface RootStore<State> : Store<State> {
    /**
     * A shorthand for [childStore] without an intital state.
     *
     * ```
     * val rootStore: RootStore<RootState> = ... // dependency injection however you like
     * val featureStore: Store<Pair<RootState, FeatureState>> = rootStore + ::childReducer
     * ```
     *
     * @param childReducer: reducer for the child state.
     */
    operator fun <ChildState> plus(childReducer: Reducer<ChildState>) = childStore(childReducer)

    /**
     * Create a child store with an additional reducer and an initial state.
     *
     * The [RootStore] defines the parent state, which you cannot change at child store creation time.
     *
     * ```
     * val rootStore: RootStore<RootState> = ... // dependency injection however you like
     * val featureStore: Store<Pair<RootState, FeatureState>> =
     *          rootStore.childStore(::childReducer, initialChildState)
     * ```
     *
     * @param childReducer: reducer for the child state.
     * @param initialChildState: optional initial child state. if omitted the child reducer will
     * receive a [ReKotlinInit] action.
     */
    fun <ChildState> childStore(
        childReducer: Reducer<ChildState>,
        initialChildState: ChildState? = null
    ): Store<Pair<State, ChildState>>
}

/**
 * The [Store] contract, it is the central piece of Redux.
 *
 * The store maintains the application state. State changes are initiated by [Store.dispatch]ing
 * [Action]s. State transitions are executed by [Reducer]s, the application can observe the state
 * changes by [Store.subscribe]ing [Subscriber]s to it.
 *
 * In addition to state the [Store] allows to dispatch and listen for [Effect]s.
 * [Effect]s are similar to [Action]s, but the key difference is that they do not trigger state
 * changes. They are intended to implement ephemeral events such a animations, snackbars, toasts etc.
 * To listen for [Effect]s implement [Listener] and [Store.subscribe] it.
 * To dispatch effects use [Store.dispatch].
 *
 * [Middleware]s can intercept all actions and change the application behavior fundamentally.
 * One such example is the Thunk middleware.
 * // TODO: move thunk to lib
 *
 * To create a store use the [store] function.
 *
 * Applications should have a single store that models the entire application state.
 * If that is not enough for you take a look at [RootStore].
 *
 */
interface Store<State> : DispatchStore, SubscribeStore<State> {

    /**
     * The current state stored in the store.
     */
    val state: State
}

/**
 * Create a new state subscriber.
 *
 * ```
 * val subscriber = subscriber { state -> /* do something with the state */ }
 * store.subscribe(subscriber)
 * // .. sometime later
 * store.unsubscribe(subscriber)
 * ```
 */
inline fun <S> subscriber(crossinline block: (S) -> Unit) = object : Subscriber<S> {
    override fun newState(state: S) = block(state)
}

/**
 * Create new effect listeners.
 *
 * ```
 * val listener = listener { effect -> /* do something with the effect */ }
 * store.subscribe(listener)
 * // .. sometime later
 * store.unsubscribe(listener)
 * ```
 */
inline fun <E : Effect> listener(crossinline block: (E) -> Unit) = object : Listener<E> {
    override fun onEffect(effect: E) = block(effect)
}

/**
 * Create a new store.
 *
 * See [Store] for more details.
 */
fun <State> store(
    reducer: Reducer<State>,
    state: State? = null,
    vararg middleware: Middleware<State> = arrayOf()
): Store<State> =
        ParentStore(reducer, state, middleware.toList(), true)

/**
 * Create a new root store.
 *
 * See [RootStore] for the what, when and why of it all.
 */
fun <State> rootStore(
    reducer: Reducer<State>,
    state: State? = null,
    vararg middleware: Middleware<State> = arrayOf()
): RootStore<State> =
        ParentStore(reducer, state, middleware.toList(), true)

/**
 * Middleware that supports the use of Thunks.
 *
 * Are the basic building blocks that support asynchronous behavior. A thunk can execute arbitrary
 * code when dispatched to the store. This allows you to execute network calls, do disk IO or long
 * running computations in the background. The Redux contract of state-change-only-through-actions
 * still holds, from within a thunk you can dispatch actions to initiate state changes.
 *
 * @see https://github.com/reduxjs/redux-thunk
 * @see https://github.com/ReSwift/ReSwift-Thunk
 */
@Suppress("UNCHECKED_CAST")
fun <State> thunkMiddleware(): Middleware<State> =
        { dispatch, getState ->
            { next ->
                { action ->
                    when (val thunk = action as? Thunk<State>) {
                        is Thunk<State> -> thunk.invoke(dispatch, getState)
                        else -> next(action)
                    }
                }
            }
        }

// TODO: KDoc
interface Thunk<State> : Action {
    fun invoke(dispatch: DispatchFunction, getState: () -> State?)
}

// TODO: KDoc
fun <State> thunk(body: (dispatch: DispatchFunction, getState: () -> State?) -> Unit) = object : Thunk<State> {
    override fun invoke(dispatch: DispatchFunction, getState: () -> State?) = body(dispatch, getState)
}