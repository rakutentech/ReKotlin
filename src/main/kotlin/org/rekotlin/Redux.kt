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
 * Marker Interface for states managed by the store.
 */
interface State // TODO: do we really need a marker interface?

typealias Reducer<State> = (action: Action, state: State?) -> State

typealias DispatchAction = (Action) -> Unit

typealias DispatchEffect = (Effect) -> Unit

typealias DispatchFunction = (Dispatchable) -> Unit

/**
 * A middleware that can intercept actions that are dispatched to the store before they reach the reducers.
 *
 * Middlewares have access to the previous state, can drop actions or dispatch others (potentially
 * multiple others. This makes middlewares very powerful. Any middleware will affect all actions
 * dispatched to the store, so use them for cross-cutting concerns only.
 */
//typealias Middleware<State> = (DispatchAction, () -> State?) -> (DispatchAction) -> DispatchAction

typealias Middleware<State> = (DispatchFunction, () -> State?) -> (DispatchFunction) -> DispatchFunction

/**
 * A state subscriber, waiting for state changes.
 *
 * Subscribe to the [Store] via [Store.subscribe].
 */
interface Subscriber<State> {
    fun newState(state: State)
}

/**
 * An effect listener, waiting for effects dispatched through the store.
 *
 * Subscribe to the [Store] via [Store.subscribe].
 */
interface Listener<Effect> {
    fun onEffect(effect: Effect)
}

/**
 * Defines the interface of Stores in ReKotlin. `Store` is the default implementation of this
 * interface. Applications have a single store that stores the entire application state.
 * Stores receive actions and use reducers combined with these actions, to calculate state changes.
 * Upon every state update a store informs all of its subscribers.
 */
interface StoreType<State : org.rekotlin.State> {

    /**
     * The current state stored in the store.
     */
    val state: State


    /**
     * Dispatch an [Action] or [Effect] to the store.
     *
     * An Action is the simplest way to initiate [State] changes.
     * This passes the action to [Middleware], delegates to the [Reducer] to determine the new [State]
     * and eventually notifies [Subscriber]s of (sub-)state changes (if any).
     *
     * Effects are one time events that do not change the [State].
     * This notifies all [Effect] listeners.
     *
     * This <b>will not</b> pass anything to [Middleware], delegate to [Reducer] or change [State].
     *
     *
     * Example of dispatching an action:
     * <pre>
     * <code>
     *     // dispatch an action
     *     store.dispatch(CounterAction.IncreaseCounter)
     *     // dispatch an effect
     *     store.dispatch(AnimationEffect
     * </code>
     * </pre>
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
    fun <E: Effect> subscribe(listener: Listener<E>, selector: (Effect) -> E?)

    /**
     * Unsubscribe a listener.
     *
     * The listener will no longer receive effects.
     *
     * @param listener: [Listener] that will be unsubscribed
     */
    fun <E: Effect> unsubscribe(listener: Listener<E>)
}