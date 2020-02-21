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
 * A box around subscriptions and subscribers.
 *
 * Acts as a type-erasing wrapper around a subscription and its transformed subscription.
 * The transformed subscription has a type argument that matches the selected substate of the
 * subscriber; however that type cannot be exposed to the store.
 *
 * The box subscribes either to the original subscription, or if available to the transformed
 * subscription and passes any values that come through this subscriptions to the subscriber.
 *
 */
internal class SubscriptionBox<State, SelectedState>(
        private val subscription: Subscription<State>,
        transform: (Subscription<State>) -> Subscription<SelectedState>,
//        transform: SubscriptionTransform<State, SelectedState>,
        internal val subscriber: Subscriber<SelectedState>
) where State : org.rekotlin.State {
    init {
        transform(subscription).observe { _, newState -> subscriber.newState(newState) }
    }

    internal fun newValues(old: State?, new: State) = subscription.newValues(old, new)
}

class Subscription<State> {

    internal constructor()

    /**
     * Initializes a subscription with a closure.
     *
     * The closure provides a way to send new values over this subscription.
     *
     * @param sink a closure that will forward all values to observers of this subscription.
     */
    private constructor(sink: ((State?, State) -> Unit) -> Unit) {
        sink { old, new -> newValues(old, new) }
    }

    @Suppress("FunctionName")
    private fun <SelectedState> _select(selector: ((State) -> SelectedState)): Subscription<SelectedState> =

        Subscription { selectedStateSink ->
            /**
             * this [observe] is in the parent Subscription<State>.
             * it will pass the selected sub state to the child Subscription<SelectedState>.
             */
            this.observe { old, new ->
                selectedStateSink(old?.let(selector), selector(new))
            }
        }

    private var observer: ((old: State?, new: State) -> Unit)? = null

    /**
     * Sends new values over this subscription. Observers will be notified of these new values.
     */
    internal fun newValues(old: State?, new: State) = observer?.invoke(old, new)


    // A caller can observe new values of this subscription through the provided closure.
    // - Note: subscriptions only support a single observer.
    internal fun observe(observer: (State?, State) -> Unit) {
        this.observer = observer
    }


    // Public API

    /**
     * Provides a subscription that selects a substate of the state of the original subscription.
     * @param selector A closure that maps a state to a selected substate
     */
    fun <SelectedState> select(selector: ((State) -> SelectedState)): Subscription<SelectedState> =
        _select(selector)

    /**
     * Provides a subscription that skips certain state updates of the original subscription.
     * @param isRepeat A closure that determines whether a given state update is a repeat and
     * thus should be skipped and not forwarded to subscribers.
     *
     */
    fun skipRepeats(isRepeat: (oldState: State, newState: State) -> Boolean): Subscription<State> =
        Subscription { sink ->
            observe { old, new ->
                if (old == null || !isRepeat(old, new)) {
                    sink(old, new)
                }
            }
        }

    /**
     * Provides a subscription that skips repeated updates of the original subscription
     * Repeated updates determined by structural equality
     */
    fun skipRepeats(): Subscription<State> = skipRepeats { old, new -> old == new }

    /**
     * Provides a subscription that skips certain state updates of the original subscription.
     *
     * This is identical to `skipRepeats` and is provided simply for convenience.
     * @param when A closure that determines whether a given state update is a repeat and
     * thus should be skipped and not forwarded to subscribers.
     */
    fun skip(`when`: (oldState: State, newState: State) -> Boolean): Subscription<State> =
        skipRepeats(`when`)

    /**
     * Provides a subscription that only updates for certain state changes.
     *
     * This is effectively the inverse of skipRepeats(:)
     * @param whenBlock A closure that determines whether a given state update should notify
     */
    fun only(whenBlock: (old: State, new: State) -> Boolean): Subscription<State> =
        skipRepeats { old, new -> !whenBlock(old, new) }
}
