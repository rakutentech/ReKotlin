/**
 * Created by Taras Vozniuk on 10/08/2017.
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

package org.rekotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SubscriberTests {

    /**
     * it allows to pass a state selector closure
     */
    @Test
    fun testAllowsSelectorClosure() {
        val store = Store(::testReducer, TestAppState())
        val subscriber = TestFilteredSubscriber<Int?>()

        store.subscribe(subscriber) { select { testValue } }

        store.dispatch(SetValueAction(3))

        assertEquals(3, subscriber.recievedValue)

        store.dispatch(SetValueAction(null))

        assertEquals(null, subscriber.recievedValue)
    }

    /**
     * it supports complex state selector closures
     */
    @Test
    fun testComplexStateSelector() {
        val store = Store(reducer = ::complexAppStateReducer, state = TestComplexAppState())
        val subscriber = TestSelectiveSubscriber()

        store.subscribe(subscriber) {
            select { Pair(testValue, otherState?.name) }
        }
        store.dispatch(SetValueAction(5))
        store.dispatch(SetOtherStateAction(OtherState("TestName", 99)))

        assertEquals(5, subscriber.recievedValue.first)
        assertEquals("TestName", subscriber.recievedValue.second)
    }

    /**
     * it does not notify subscriber for unchanged substate state when using `skipRepeats`.
     */
    @Test
    fun testUnchangedStateSelector() {
        val store = Store(::testReducer, TestAppState(3))
        val subscriber = TestFilteredSubscriber<Int?>()

        store.subscribe(subscriber) {
            select {
                testValue
            }.skipRepeats { oldState, newState ->
                oldState == newState
            }
        }

        assertEquals(3, subscriber.recievedValue)

        store.dispatch(SetValueAction(3))

        assertEquals(3, subscriber.recievedValue)
        assertEquals(1, subscriber.newStateCallCount)
    }

    /**
     * it does not notify subscriber for unchanged substate state when using the default
     * `skipRepeats` implementation.
     */
    @Test
    fun testUnchangedStateSelectorDefaultSkipRepeats() {
        val store = Store(::stringStateReducer, TestStringAppState())
        val subscriber = TestFilteredSubscriber<String>()

        store.subscribe(subscriber) {
            select { testValue }.skipRepeats()
        }

        assertEquals("Initial", subscriber.recievedValue)

        store.dispatch(SetValueStringAction("Initial"))

        assertEquals("Initial", subscriber.recievedValue)
        assertEquals(1, subscriber.newStateCallCount)
    }

    /**
     * it skips repeated state values by when `skipRepeats` returns `true`.
     */
    @Test
    fun testSkipsStateUpdatesForCustomEqualityChecks() {
        val state = TestCustomAppState(5)
        val store = Store(::customAppStateReducer, state)
        val subscriber = TestFilteredSubscriber<TestCustomSubstate>()

        store.subscribe(subscriber) {
            select { substate }
                .skipRepeats { oldState, newState -> oldState.value == newState.value }
        }

        assertEquals(5, subscriber.recievedValue?.value)

        store.dispatch(SetCustomSubstateAction(5))

        assertEquals(5, subscriber.recievedValue?.value)
        assertEquals(1, subscriber.newStateCallCount)
    }

    @Test
    fun testPassesOnDuplicateSubstateUpdatesByDefault() {
        val store = Store(::stringStateReducer, TestStringAppState())
        val subscriber = TestFilteredSubscriber<String>()

        store.subscribe(subscriber) {
            select { testValue }
        }

        assertEquals("Initial", subscriber.recievedValue)

        store.dispatch(SetValueStringAction("Initial"))

        assertEquals("Initial", subscriber.recievedValue)
        assertEquals(2, subscriber.newStateCallCount)
    }

    @Test
    fun testSkipsStateUpdatesForEquatableStateByDefault() {
        val store = Store(::stringStateReducer, TestStringAppState())
        val subscriber = TestFilteredSubscriber<TestStringAppState>()

        store.subscribe(subscriber)

        assertEquals("Initial", subscriber.recievedValue?.testValue)

        store.dispatch(SetValueStringAction("Initial"))

        assertEquals("Initial", subscriber.recievedValue?.testValue)
        assertEquals(1, subscriber.newStateCallCount)
    }

    @Test
    fun testPassesOnDuplicateStateUpdatesInCustomizedStore() {
        val store = Store(::stringStateReducer, TestStringAppState(), skipRepeats = false)
        val subscriber = TestFilteredSubscriber<TestStringAppState>()

        store.subscribe(subscriber)

        assertEquals("Initial", subscriber.recievedValue?.testValue)

        store.dispatch(SetValueStringAction("Initial"))

        assertEquals("Initial", subscriber.recievedValue?.testValue)
        assertEquals(2, subscriber.newStateCallCount)
    }

    @Test
    fun testSkipWhen() {
        val state = TestCustomAppState(5)
        val store = Store(::customAppStateReducer, state)
        val subscriber = TestFilteredSubscriber<TestCustomSubstate>()

        store.subscribe(subscriber) {
            select { substate }
                .skip { oldState, newState -> oldState.value == newState.value }
        }

        assertEquals(5, subscriber.recievedValue?.value)

        store.dispatch(SetCustomSubstateAction(5))

        assertEquals(5, subscriber.recievedValue?.value)
        assertEquals(1, subscriber.newStateCallCount)
    }

    @Test
    fun testOnlyWhen() {
        val state = TestCustomAppState(5)
        val store = Store(::customAppStateReducer, state)
        val subscriber = TestFilteredSubscriber<TestCustomSubstate>()

        store.subscribe(subscriber) {
            select { substate }
                .only { oldState, newState -> oldState.value != newState.value }
        }

        assertEquals(5, subscriber.recievedValue?.value)

        store.dispatch(SetCustomSubstateAction(5))

        assertEquals(5, subscriber.recievedValue?.value)
        assertEquals(1, subscriber.newStateCallCount)
    }
}

internal class TestFilteredSubscriber<T> : Subscriber<T> {
    var recievedValue: T? = null
    var newStateCallCount = 0

    override fun newState(state: T) {
        recievedValue = state
        newStateCallCount += 1
    }
}

/**
 * Example of how you can select a substate. The return value from
 *`selectSubstate` and the argument for `newState` need to match up.
 */
class TestSelectiveSubscriber : Subscriber<Pair<Int?, String?>> {
    var recievedValue: Pair<Int?, String?> = Pair(null, null)

    override fun newState(state: Pair<Int?, String?>) {
        recievedValue = state
    }
}

data class TestComplexAppState(
        val testValue: Int? = null,
        val otherState: OtherState? = null
) : State

data class OtherState(val name: String?, val age: Int?)

fun complexAppStateReducer(action: Action, state: TestComplexAppState?): TestComplexAppState {
    val oldState = state ?: TestComplexAppState()

    return when (action) {
        is SetValueAction -> {
            oldState.copy(testValue = action.value)
        }
        is SetOtherStateAction -> {
            oldState.copy(otherState = action.otherState)
        }
        else -> oldState
    }

}

internal data class SetOtherStateAction(val otherState: OtherState) : Action