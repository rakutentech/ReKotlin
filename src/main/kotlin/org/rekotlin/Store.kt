package org.rekotlin

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by Taras Vozniuk on 31/07/2017.
 * Copyright © 2017 GeoThings. All rights reserved.
 *
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
 * Initial Action that is dispatched as soon as the store is created.
 * Reducers respond to this action by configuring their initial state.
 */
class ReKotlinInit : Action

class Store<State : org.rekotlin.State>(
    private val reducer: Reducer<State>,
    state: State?,
    middleware: List<Middleware<State>> = emptyList(),
    private val skipRepeats: Boolean = true
) : StoreType<State> {

    private var _state: State? = state
        set(value) {
            val oldValue = field
            field = value

            value?.let {
                subscriptions.forEach { it.newValues(oldValue, value) }
            }
        }

    override val state: State
        get() = _state!!

    @Suppress("NAME_SHADOWING")
    override val dispatchAction: DispatchAction = middleware
        .reversed()
            .fold(this::defaultDispatch as DispatchAction,
                    { dispatch, middleware -> middleware(this::dispatch, this::state )(dispatch) })

    internal val subscriptions: MutableList<SubscriptionBox<State, Any>> = CopyOnWriteArrayList()
    internal val listeners: MutableList<ListenerBox<*>> = CopyOnWriteArrayList()

    init {
        if (state != null) {
            this._state = state
        } else {
            this.dispatch(ReKotlinInit())
        }
    }

    override fun <S : Subscriber<State>> subscribe(subscriber: S) =
            subscribe(subscriber, ::stateIdentity)

    override fun <SelectedState, S : Subscriber<SelectedState>> subscribe(
            subscriber: S,
            selector: Subscription<State>.() -> Subscription<SelectedState>
    ) {
        unsubscribe(subscriber)

        val actualSelector = if (skipRepeats) {
            compose(selector, Subscription<SelectedState>::skipRepeatsTransform)
        } else {
            selector
        }

        val box = SubscriptionBox(Subscription(), actualSelector, subscriber)

        // each subscriber has its own potentially different SelectedState that doesn't have to conform to StateType
        @Suppress("UNCHECKED_CAST")
        this.subscriptions.add(box as SubscriptionBox<State, Any>)

        this._state?.let {
            box.newValues(null, it)
        }
    }

    override fun <SelectedState> unsubscribe(subscriber: Subscriber<SelectedState>) {
        val index = this.subscriptions.indexOfFirst { it.subscriber === subscriber }
        if (index != -1) {
            this.subscriptions.removeAt(index)
        }
    }

    private fun defaultDispatch(action: Action) {
        this._state = noInterruptions { reducer(action, this._state) }
    }

    private var isDispatching = false

    private fun <T> noInterruptions(work: () -> T): T {
        if (isDispatching) {
            throw Exception(
                    "ReKotlin:ConcurrentMutationError - " +
                            "Action has been dispatched while a previous action is being processed. " +
                            "A reducer is dispatching an action, " +
                            "or you are using ReKotlin in a concurrent context (e.g. multithreaded)."
            )
        }

        this.isDispatching = true
        val newState = work()
        this.isDispatching = false

        return newState
    }

    override fun dispatch(action: Action) = dispatchAction(action)

    // effects

    override fun dispatch(effect: Effect) = dispatchEffect(effect)

    override val dispatchEffect: DispatchEffect = { effect ->
            listeners.forEach { it.onEffect(effect) }
        }

    override fun subscribe(listener: Listener<Effect>) = subscribe(listener, ::effectIdentity)

    override fun <E : Effect> unsubscribe(listener: Listener<E>) {
        val index = this.listeners.indexOfFirst { it.listener === listener }
        if (index != -1) {
            this.listeners.removeAt(index)
        }
    }

    override fun <E : Effect> subscribe(listener: Listener<E>, selector: (Effect) -> E?) {
        unsubscribe(listener)

        listeners.add(ListenerBox(listener, selector))
    }

}

internal class ListenerBox<E: Effect>(
        val listener: Listener<E>,
        private val selector: (Effect) -> E?
): Listener<Effect> {
    override fun onEffect(effect: Effect) {
        selector(effect)?.let { listener.onEffect(it) }
    }
}

private fun <T> Subscription<T>.skipRepeatsTransform(): Subscription<T> = this.skipRepeats()
private fun <T: State> stateIdentity(sub: Subscription<T>) = sub
private fun <T: Effect> effectIdentity(effect: T) = effect

private fun <T, S> compose(first: T.() -> S, second: S.() -> S): T.() -> S = { first().second() }