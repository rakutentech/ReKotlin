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
 * All actions that want to be able to be dispatched to a store need to conform to this protocol
 * Currently it is just a marker protocol with no requirements.
 */
interface Action

interface Effect

/**
 * Marker Interface for states managed by the store.
 * TODO: do we really need a marker interface?
 */
interface State

typealias Reducer<State> = (action: Action, state: State?) -> State

typealias DispatchAction = (Action) -> Unit

typealias DispatchEffect = (Effect) -> Unit

typealias Middleware<State> = (DispatchAction, () -> State?) -> (DispatchAction) -> DispatchAction

interface Subscriber<State> {
    fun newState(state: State)
}

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
     * Dispatch an action - the simplest way to initiate state changes.
     *
     * This passes the action to middlewares, delegates to the reducer to determine the new state
     * and eventually notifies subscribers of (sub-)state changes (if any).
     *
     *
     * Example of dispatching an action:
     * <pre>
     * <code>
     * store.dispatch( CounterAction.IncreaseCounter )
     * </code>
     * </pre>
     *
     * @param action The action to dispatch to the store
     */
    fun dispatch(action: Action)

    /**
     * Dispatch A one time effect that does not change the state.
     *
     * This notifies all subscribers to effects.
     *
     * This <b>will not</b> pass anything to middlewares, delegate to reducers or change state.
     */
    fun dispatch(effect: Effect)

    /**
     * The dispatch function used to dispatch [Action]s.
     * Use the property if you want to use only a dispatch function and not the full store, for
     * example for dependency injection.
     */
    val dispatchAction: DispatchAction

    /**
     * The dispatch function used to dispatch [Effects]s.
     * Use the property if you want to use only a dispatch function and not the full store, for
     * example for dependency injection.
     */
    val dispatchEffect: DispatchEffect

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
            selector: (Subscription<State>) -> Subscription<SelectedState>
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