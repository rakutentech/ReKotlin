package org.rekotlin

import java.util.concurrent.CopyOnWriteArrayList

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

internal class ChildStore<State>(
        parentDispatchFunction: DispatchFunction,
        private val reducer: Reducer<State>,
        state: State?
) : Store<State>  {

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

    private val dispatchAction: DispatchAction = this::defaultDispatch

    private val dispatchEffect: DispatchEffect = { effect ->
        listeners.forEach { it.onEffect(effect) }
    }

    // make sure dispatches go through the parent
    override val dispatchFunction: DispatchFunction = parentDispatchFunction
    override fun dispatch(dispatchable: Dispatchable) = dispatchFunction(dispatchable)

    // the parent will call back into the [delegateDispatchFunction]
    internal val delegateDispatchFunction: DispatchFunction = { dispatchable: Dispatchable ->
        when (dispatchable) {
            is Action -> dispatchAction(dispatchable)
            is Effect -> dispatchEffect(dispatchable)
        }
    }

    internal val subscriptions: MutableList<SubscriptionBox<State, Any>> = CopyOnWriteArrayList()
    internal val listeners: MutableList<ListenerBox<*>> = CopyOnWriteArrayList()

    init {
        if (state != null) {
            this._state = state
        } else {
            this.delegateDispatchFunction(ReKotlinInit)
        }
    }

    override fun <S : Subscriber<State>> subscribe(subscriber: S) =
            subscribe(subscriber, ::stateIdentity)

    override fun <SelectedState, S : Subscriber<SelectedState>> subscribe(
            subscriber: S,
            selector: Subscription<State>.() -> Subscription<SelectedState>
    ) {
        unsubscribe(subscriber)

        val actualSelector = compose(selector, Subscription<SelectedState>::skipRepeatsTransform)

        val box = SubscriptionBox(actualSelector, subscriber)

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

    private var noInterruptionsPlease = false

    private fun <T> noInterruptions(work: () -> T): T {
        if (noInterruptionsPlease) {
            throw Exception(
                    "ReKotlin:ConcurrentMutationError - " +
                            "Action has been dispatched while a previous action is being processed. " +
                            "A reducer is dispatching an action, " +
                            "or you are using ReKotlin in a concurrent context (e.g. multithreaded)."
            )
        }

        this.noInterruptionsPlease = true
        val newState = work()
        this.noInterruptionsPlease = false

        return newState
    }

    private fun defaultDispatch(dispatchable: Dispatchable) =
            when (dispatchable) {
                is Action -> _state = noInterruptions { reducer(dispatchable, _state) }
                is Effect -> listeners.forEach { it.onEffect(dispatchable) }
                else -> Unit
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