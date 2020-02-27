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

internal class ListenerBox<E: Effect>(
        val listener: Listener<E>,
        private val selector: (Effect) -> E?
): Listener<Effect> {
    override fun onEffect(effect: Effect) {
        selector(effect)?.let { listener.onEffect(it) }
    }
}

/**
 * A box around subscriptions and subscribers.
 *
 * Acts as a type-erasing wrapper around a subscription and its transformed subscription.
 * The transformed subscription has a type argument that matches the selected sub state of the
 * subscriber; however that type cannot be exposed to the store.
 */
internal class SubscriptionBox<State, SelectedState>(
        transform: Subscription<State>.() -> Subscription<SelectedState>,
        internal val subscriber: Subscriber<SelectedState>
) {
    private val subscription = Subscription<State>()

    init {
        subscription.transform().observe { _, newState -> subscriber.newState(newState) }
    }

    internal fun newValues(old: State?, new: State) = subscription.newValues(old, new)
}