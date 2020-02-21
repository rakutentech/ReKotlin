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
                subscriptions.forEach {
                    it.newValues(oldValue, value)
                }
            }
        }

    override val state: State
        get() {
            return _state!!
        }

    @Suppress("NAME_SHADOWING")
    override val dispatchFunction: DispatchFunction = middleware
        .reversed()
            .fold(this::defaultDispatch as DispatchFunction,
                    { dispatch, middleware -> middleware(this::dispatch, this::state )(dispatch) })

    // TODO: make this private?
    internal val subscriptions: MutableList<SubscriptionBox<State, Any>> = CopyOnWriteArrayList()

    init {
        if (state != null) {
            this._state = state
        } else {
            this.dispatch(ReKotlinInit())
        }
    }

    override fun <S : StoreSubscriber<State>> subscribe(subscriber: S) {
        this.subscribe(subscriber, if (skipRepeats) ::skipRepeatsTransform else ::identity)
    }

    // TODO: add subscription as receiver for transform to make for fluent API
    override fun <SelectedState, S : StoreSubscriber<SelectedState>> subscribe(
        subscriber: S,
        transform: SubscriptionTransform<State, SelectedState>
    ) {
        // If the same subscriber is already registered with the store, replace the existing
        // subscription with the new one.
        val index = this.subscriptions.indexOfFirst { it.subscriber === subscriber }
        if (index != -1) {
            this.subscriptions.removeAt(index)
        }

        val box = SubscriptionBox(Subscription(), transform, subscriber)

        // each subscriber has its own potentially different SelectedState that doesn't have to conform to StateType
        @Suppress("UNCHECKED_CAST")
        this.subscriptions.add(box as SubscriptionBox<State, Any>)

        this._state?.let {
            box.newValues(null, it)
        }
    }

    override fun <SelectedState> unsubscribe(subscriber: StoreSubscriber<SelectedState>) {
        val index = this.subscriptions.indexOfFirst { it.subscriber === subscriber }
        if (index != -1) {
            this.subscriptions.removeAt(index)
        }
    }

    // TODO: replace this with varargs unsubscribe
    fun unsubscribe(blockSubscriptions: BlockSubscriptions) =
        blockSubscriptions.blockSubscriberList.forEach { unsubscribe(it) }

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

    override fun dispatch(action: Action) = dispatchFunction(action)

//    override fun dispatch(actionCreator: ActionCreator<State, StoreType<State>>) {
//        actionCreator(this.state, this)?.let {
//            this.dispatch(it)
//        }
//    }

//    override fun dispatch(asyncActionCreator: AsyncActionCreator<State, StoreType<State>>) {
//        this.dispatch(asyncActionCreator, null)
//    }
//
//    override fun dispatch(
//        asyncActionCreator: AsyncActionCreator<State, StoreType<State>>,
//        callback: DispatchCallback<State>?
//    ) {
//        asyncActionCreator(this.state, this) { actionProvider ->
//            val action = actionProvider(this.state, this)
//
//            action?.let {
//                this.dispatch(it)
//                callback?.invoke(this.state)
//            }
//        }
//    }
}

internal fun <T: State> skipRepeatsTransform(sub: Subscription<T>) = sub.skipRepeats()
internal fun <T: State> identity(sub: Subscription<T>) = sub