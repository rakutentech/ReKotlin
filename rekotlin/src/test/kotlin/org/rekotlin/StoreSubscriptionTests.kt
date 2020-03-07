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

package org.rekotlin

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class StoreSubscriptionTests {

    @Test
    fun `should notify subscriber of state changes after subscribing`() {
        val store = store(::intReducer, IntState())
        val subscriber = FakeSubscriber<IntState>()
        store.subscribe(subscriber)
        val baseline = subscriber.callCount

        store.dispatch(IntAction(333))

        assert(subscriber.callCount == baseline + 1)
        assert(subscriber.lastState?.number == 333)

        store.dispatch(IntAction(1337))

        assert(subscriber.callCount == baseline + 2)
        assert(subscriber.lastState?.number == 1337)
    }

    @Test
    fun `should prevent duplicate subscriber silently`() {
        val store = store(::intReducer, IntState())
        val subscriber = FakeSubscriber<IntState>()
        store.subscribe(subscriber)
        store.subscribe(subscriber)

        val callCount = subscriber.callCount

        store.dispatch(IntAction(333))

        assert(subscriber.callCount == callCount + 1)
    }

    @Test
    fun `should prevent duplicate subscriber silently with substate selectors`() {
        val store = store(::intReducer, IntState())
        val subscriber = FakeSubscriber<Int?>()
        store.subscribe(subscriber) { select { number } }
        store.subscribe(subscriber) { select { number } }

        val callCount = subscriber.callCount

        store.dispatch(IntAction(333))

        assert(subscriber.callCount == callCount + 1)
    }

    /**
     * it replaces the subscription of an existing subscriber with the new one.
     */
    @Test
    fun `should prevent duplicate subscriber silently even when subscribing with and without state selector`() {
        val store = store(::intReducer, IntState())
        val subscriber = FakeSubscriber<IntState>()
        store.subscribe(subscriber)
        store.subscribe(subscriber) { skip { old, new -> old.number == new.number } }
        val callCount = subscriber.callCount

        store.dispatch(IntAction(1))

        assert(subscriber.callCount == callCount + 1) // still only subscribed once
    }

    @Test
    fun `should skip repeats automatically`() {
        val store = store(::intReducer, IntState())
        val subscriber = FakeSubscriber<IntState>()
        store.subscribe(subscriber)
        val action = IntAction(777)
        store.dispatch(action)
        val callCount = subscriber.callCount

        // dispatching actions should be idempotent
        store.dispatch(action)
        store.dispatch(action)
        store.dispatch(action)

        // no state change, therefore no more calls
        assert(subscriber.callCount == callCount)
    }

    @Test
    fun `should pass initial state to subscriber upon subscribing`() {
        val initalState = IntState(1723)
        val store = store(::intReducer, initalState)
        val subscriber = FakeSubscriber<IntState>()

        store.subscribe(subscriber)

        assert(subscriber.lastState == initalState)
    }

    @Test
    fun `should pass initial state to subscriber upon subscribing, even if store was initialized with null state`() {
        val initalState = null
        val store = store(::intReducer, initalState)
        val subscriber = FakeSubscriber<IntState>()

        store.subscribe(subscriber)

        assert(subscriber.callCount == 1)
    }

    @Test
    fun `should allow to dispatch from a subscriber callback`() {
        val store = ParentStore(::intReducer, IntState())
        val subscriber = subscriber<IntState> { state ->
            // Test if we've already dispatched this action to
            // avoid endless recursion
            if (state.number != 5) {
                store.dispatch(IntAction(5))
            }
        }
        store.subscribe(subscriber)

        store.dispatch(IntAction(2))

        assert(store.state.number == 5)
    }

    @Test
    fun `should not notify subscriber after unsubscribe`() {
        val store = store(::intReducer, IntState(0))
        val subscriber = FakeSubscriber<IntState>()
        store.subscribe(subscriber)

        store.dispatch(IntAction(1))
        store.dispatch(IntAction(2))
        store.unsubscribe(subscriber)
        store.dispatch(IntAction(3))
        store.dispatch(IntAction(4))

        assert(subscriber.history.map { it.number } == listOf(0, 1, 2))
    }

    @Test
    fun `should allow to re-subscribe after unsubscribe`() {
        val store = store(::intReducer, IntState(0))
        val subscriber = FakeSubscriber<IntState>()
        store.subscribe(subscriber)

        store.dispatch(IntAction(1))
        store.dispatch(IntAction(2))
        store.unsubscribe(subscriber)
        store.dispatch(IntAction(3))
        store.dispatch(IntAction(4))
        store.dispatch(IntAction(5))
        store.subscribe(subscriber)
        store.dispatch(IntAction(6))

        assert(subscriber.history.map { it.number } == listOf(0, 1, 2, 5, 6))
    }

    @Test
    fun `should allow to subscribe new subscriber during new state callback to subscriber`() {
        val store = store(::stringReducer, StringState("initial"))
        val subOne = FakeSubscriber<StringState>()
        val subTwo = FakeSubscriber<StringState> { store.subscribe(subOne) }

        store.subscribe(subTwo)

        assert(subOne.callCount == 1)
        assert(subTwo.callCount == 1)

        // implicitly test this doesn't crash
    }

    class SelfUnSubscriber<T>(private val store: SubscribeStore<T>) : Subscriber<T> {
        val callCount get() = _callCount
        var _callCount = 0
        override fun newState(state: T) {
            _callCount += 1
            store.unsubscribe(this)
        }
    }

    @Test
    fun `should allow to unsubscribe during new state callback`() {
        val store = store(::stringReducer, StringState("initial"))
        val subscriber = SelfUnSubscriber(store)

        store.subscribe(subscriber)

        store.dispatch(StringAction(""))
        store.dispatch(StringAction(""))
        store.dispatch(StringAction(""))

        assert(subscriber.callCount == 1)
    }

    class SelfSubscriber<T>(private val store: SubscribeStore<T>) : Subscriber<T> {
        override fun newState(state: T) {
            store.subscribe(this)
        }
    }

    @Test
    fun `should not allow to re-subscribe oneself in new state callback to subscriber`() {
        val store = store(::stringReducer, StringState("initial"))
        val subscribre = SelfSubscriber(store)

        try {
            store.subscribe(subscribre)
            fail<Any>("expected stack overflow")
        } catch (e: StackOverflowError) {
            assertNotNull(e)
        }
    }
}
