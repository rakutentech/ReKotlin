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
 * A subscription to the [Store].
 *
 * With the subscription you can select sub-states to subscribe to and define custom rules for
 * when you want to receive (or skip) state updates.
 *
 * ```
 * data class AppState(val number: Int, val title: String)
 * val store: Store<AppState> = /* initialize */
 * // subscribe to a substate
 * store.subscribe(subscriber) { select { number } }
 * // skip updates under custom conditions
 * store.subscribe(subscriber) { skip { old, new -> old.number == new.number } }
 * // only receive updates under custom conditions
 * store.subscribe(subscriber) { only { old, new -> (old.number + new.number) % 2 == 0 } }
 * ```
 */
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

    /**
     * A caller can observe new values of this subscription through the provided closure.
     * Subscriptions only support a single observer.
     */
    internal fun observe(observer: (State?, State) -> Unit) {
        this.observer = observer
    }

    // Public API

    /**
     * Select a sub-state of the the store's state.
     *
     * ```
     * data class AppState(val number: Int, val title: String)
     * val store: Store<AppState> = /* initialize */
     *
     * store.subscribe(numberSubscriber) { select { number } }
     * store.subscribe(titleSubscriber) { select { title } }
     * ```
     *
     * @param selector A closure that maps a state to a selected substate
     */
    fun <SelectedState> select(selector: (State.() -> SelectedState)): Subscription<SelectedState> =
            _select(selector)

    /**
     * Skips state under custom conditions.
     *
     * The inverse of [only].
     *
     * ```
     * data class AppState(val number: Int, val title: String)
     * val store: Store<AppState> = /* initialize */
     *
     * store.subscribe(subscriber) { skip { old, new -> old.number == new.number } }
     * ```
     *
     * @param condition: Determines whether to skip a state update or not.
     */
    fun skip(condition: (old: State, new: State) -> Boolean): Subscription<State> =
            Subscription { sink ->
                observe { old, new ->
                    if (old == null || !condition(old, new)) {
                        sink(old, new)
                    }
                }
            }

    /**
     * Updates state only under custom conditions.
     *
     * The inverse of [skip].
     *
     * ```
     * data class AppState(val number: Int, val title: String)
     * val store: Store<AppState> = /* initialize */
     *
     * store.subscribe(subscriber) { only { old, new -> (old.number + new.number) % 2 == 0 } }
     * ```
     *
     * @param condition: Determines whether to update subscribers for a state update.
     */
    fun only(condition: (old: State, new: State) -> Boolean): Subscription<State> =
            skip { old, new -> !condition(old, new) }
}
