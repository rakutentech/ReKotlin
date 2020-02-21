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

data class TestAppState(val testValue: Int? = null) : State

data class TestStringAppState(val testValue: String = "Initial") : State

data class TestCustomSubstate(val value: Int) : State

data class TestCustomAppState(val substate: TestCustomSubstate) : State {
    constructor(substateValue: Int = 0) : this(TestCustomSubstate(substateValue))
}

data class NoOpAction(val unit: Unit = Unit) : Action
data class SetValueAction(val value: Int?) : Action {
    companion object {
        val type = "SetValueAction"
    }
}

data class SetValueStringAction(var value: String) : Action {
    companion object {
        val type = "SetValueStringAction"
    }
}

data class SetCustomSubstateAction(val value: Int) : Action {
    companion object {
        val type = "SetCustomSubstateAction"
    }
}

fun testReducer(action: Action, state: TestAppState?): TestAppState {
    var oldState = state ?: TestAppState()

    return when (action) {
        is SetValueAction -> oldState.copy(testValue = action.value)
        else -> oldState

    }
}

fun stringStateReducer(action: Action, state: TestStringAppState?): TestStringAppState {
    val oldState = state ?: TestStringAppState()

    return when (action) {
        is SetValueStringAction -> oldState.copy(testValue = action.value)
        else -> oldState
    }
}

fun customAppStateReducer(action: Action, state: TestCustomAppState?): TestCustomAppState {
    val oldState = state ?: TestCustomAppState()

    return when (action) {
        is SetCustomSubstateAction -> oldState.copy(oldState.substate.copy(action.value))
        else -> oldState
    }

}

class TestStoreSubscriber<T> : Subscriber<T> {
    var receivedStates: MutableList<T> = mutableListOf()

    override fun newState(state: T) {
        this.receivedStates.add(state)
    }
}

/**
 * A subscriber contains another sub-subscribers [Subscriber], which could be subscribe/unsubscribe at some point
 */
class ViewSubscriberTypeA(var store: Store<TestStringAppState>) : Subscriber<TestStringAppState> {
    private val viewB by lazy { ViewSubscriberTypeB(store) }
    private val viewC by lazy { ViewSubscriberTypeC() }

    override fun newState(state: TestStringAppState) {
        when (state.testValue) {
            "subscribe" -> store.subscribe(viewC)
            "unsubscribe" -> store.unsubscribe(viewB)
            else -> Unit
        }
    }
}

class ViewSubscriberTypeB(store: Store<TestStringAppState>) : Subscriber<TestStringAppState> {
    init {
        store.subscribe(this)
    }

    override fun newState(state: TestStringAppState) {
        // do nothing
    }
}

class ViewSubscriberTypeC : Subscriber<TestStringAppState> {
    override fun newState(state: TestStringAppState) {
        // do nothing
    }
}

class DispatchingSubscriber(var store: Store<TestAppState>) : Subscriber<TestAppState> {

    override fun newState(state: TestAppState) {
        // Test if we've already dispatched this action to
        // avoid endless recursion
        if (state.testValue != 5) {
            this.store.dispatch(SetValueAction(5))
        }
    }
}

class CallbackStoreSubscriber<T>(val handler: (T) -> Unit) : Subscriber<T> {
    override fun newState(state: T) {
        handler(state)
    }
}
