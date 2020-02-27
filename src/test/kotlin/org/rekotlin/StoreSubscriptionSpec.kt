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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class StoreSubscriptionTests {

    // this is not going to work in JVM.
    // WeakReference also can't solve it since gc collects non-deterministically
    // TODO: Discuss with ReSwift community for this inconsistency
    /*
    /**
     * It does not strongly capture an observer
     */
    @Test
    fun testStrongCapture(){
        store = Store(reducer::handleAction, TestAppState())
        var subscriber: TestSubscriber? = TestSubscriber()

        store.subscribe(subscriber!!)
        assertEquals(1, store.subscriptions.map { it.subscriber != null }.count())

        @Suppress("UNUSED_VALUE")
        subscriber = null
        assertEquals(0, store.subscriptions.map { it.subscriber != null }.count())
    }
    */

    /**
     * it removes subscribers before notifying state changes
     */
    @Test
    fun testRemoveSubscribers() {
        val store = ParentStore(::intReducer, IntState())
        val subscriber1 = FakeSubscriberWithHistory<IntState>()
        val subscriber2 = FakeSubscriberWithHistory<IntState>()

        store.subscribe(subscriber1)
        store.subscribe(subscriber2)
        store.dispatch(IntAction(3))
        assertEquals(2, store.subscriptions.count())
        assertEquals(3, subscriber1.states.lastOrNull()?.number)
        assertEquals(3, subscriber2.states.lastOrNull()?.number)

        // dereferencing won't remove the subscriber(like in ReSwift)
        // subscriber1 = null
        store.unsubscribe(subscriber1)
        store.dispatch(IntAction(5))
        assertEquals(1, store.subscriptions.count())
        assertEquals(5, subscriber2.states.lastOrNull()?.number)

        // dereferencing won't remove the subscriber(like in ReSwift)
        // subscriber1 = null
        store.unsubscribe(subscriber2)
        store.dispatch(IntAction(8))
        assertEquals(0, store.subscriptions.count())
    }

    /**
     * it replaces the subscription of an existing subscriber with the new one.
     */
    @Test
    fun testDuplicateSubscription() {
        val store = ParentStore(::intReducer, IntState())
        val subscriber = FakeSubscriberWithHistory<IntState>()

        // initial subscription
        store.subscribe(subscriber)
        // Subsequent subscription that skips repeated updates.
        store.subscribe(subscriber) { skipRepeats { oldState, newState -> oldState.number == newState.number } }

        // One initial state update for every subscription.
        assertEquals(2, subscriber.states.count())

        store.dispatch(IntAction(3))
        store.dispatch(IntAction(3))
        store.dispatch(IntAction(3))
        store.dispatch(IntAction(3))

        assertEquals(3, subscriber.states.count())
    }

    /**
     * it dispatches initial value upon subscription
     */
    @Test
    fun testDispatchInitialValue() {
        val store = ParentStore(::intReducer, IntState())
        val subscriber = FakeSubscriberWithHistory<IntState>()

        store.subscribe(subscriber)
        store.dispatch(IntAction(3))

        assertEquals(3, subscriber.states.lastOrNull()?.number)
    }

    /**
     * it allows dispatching from within an observer
     */
    @Test
    fun testAllowDispatchWithinObserver() {
        val store = ParentStore(::intReducer, IntState())
        val subscriber = DispatchingSubscriber(store)

        store.subscribe(subscriber)
        store.dispatch(IntAction(2))

        assertEquals(5, store.state.number)
    }

    /**
     * it does not dispatch value after subscriber unsubscribes
     */
    @Test
    fun testDontDispatchToUnsubscribers() {
        val store = ParentStore(::intReducer, IntState())
        val subscriber = FakeSubscriberWithHistory<IntState>()

        store.dispatch(IntAction(5))
        store.subscribe(subscriber)
        store.dispatch(IntAction(10))

        store.unsubscribe(subscriber)
        // Following value is missed due to not being subscribed:
        store.dispatch(IntAction(15))
        store.dispatch(IntAction(25))

        store.subscribe(subscriber)
        store.dispatch(IntAction(20))

        assertEquals(4, subscriber.states.count())
        assertEquals(5, subscriber.states[subscriber.states.count() - 4].number)
        assertEquals(10, subscriber.states[subscriber.states.count() - 3].number)
        assertEquals(25, subscriber.states[subscriber.states.count() - 2].number)
        assertEquals(20, subscriber.states[subscriber.states.count() - 1].number)
    }

    /**
     * it ignores identical subscribers
     */
    @Test
    fun testIgnoreIdenticalSubscribers() {
        val store = ParentStore(::intReducer, IntState())
        val subscriber = FakeSubscriberWithHistory<IntState>()

        store.subscribe(subscriber)
        store.subscribe(subscriber)

        assertEquals(1, store.subscriptions.count())
    }

    /**
     * it ignores identical subscribers that provide substate selectors
     */
    @Test
    fun testIgnoreIdenticalSubstateSubscribers() {
        val store = ParentStore(::intReducer, IntState())
        val subscriber = FakeSubscriberWithHistory<IntState>()

        store.subscribe(subscriber) { this }
        store.subscribe(subscriber) { this }

        assertEquals(1, store.subscriptions.count())
    }

    @Test
    fun testSubscribeDuringOnNewState() {
        // setup
        val store = ParentStore(::stringReducer, StringState())

        val subscribeA = ViewSubscriberTypeA(store)
        store.subscribe(subscribeA)

        // execute
        store.dispatch(StringAction("subscribe"))

        // no crash
    }

    @Test
    fun testUnSubscribeDuringOnNewState() {
        // setup
        val store = ParentStore(reducer = ::stringReducer, state = StringState())

        val subscriberA = ViewSubscriberTypeA(store)
        val subscriberC = ViewSubscriberTypeC()
        store.subscribe(subscriberA)
        store.subscribe(subscriberC)

        // execute
        store.dispatch(StringAction("unsubscribe"))

        // no crash
    }
}