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

data class IntState(val number: Int? = null)

data class StringState(val name: String = "Initial")

data class SubState(val value: Int)

data class TestCustomAppState(val subState: SubState) {
    constructor(subState: Int = 0) : this(SubState(subState))
}

data class NoOpAction(val unit: Unit = Unit) : Action
data class SetIntAction(val value: Int?) : Action
data class SetStringAction(var value: String) : Action
data class SetCustomSubStateAction(val value: Int) : Action

fun intReducer(action: Action, state: IntState?): IntState {
    val oldState = state ?: IntState()

    return when (action) {
        is SetIntAction -> oldState.copy(number = action.value)
        else -> oldState

    }
}

fun stringReducer(action: Action, state: StringState?): StringState {
    val oldState = state ?: StringState()

    return when (action) {
        is SetStringAction -> oldState.copy(name = action.value)
        else -> oldState
    }
}

fun customAppStateReducer(action: Action, state: TestCustomAppState?): TestCustomAppState {
    val oldState = state ?: TestCustomAppState()

    return when (action) {
        is SetCustomSubStateAction -> oldState.copy(oldState.subState.copy(action.value))
        else -> oldState
    }

}

class FakeSubscriberWithHistory<T> : Subscriber<T> {
    var states: MutableList<T> = mutableListOf()

    override fun newState(state: T) {
        this.states.add(state)
    }
}

/**
 * A subscriber contains another sub-subscribers [Subscriber], which could be subscribe/unsubscribe at some point
 */
class ViewSubscriberTypeA(var store: Store<StringState>) : Subscriber<StringState> {
    private val viewB by lazy { ViewSubscriberTypeB(store) }
    private val viewC by lazy { ViewSubscriberTypeC() }

    override fun newState(state: StringState) {
        when (state.name) {
            "subscribe" -> store.subscribe(viewC)
            "unsubscribe" -> store.unsubscribe(viewB)
            else -> Unit
        }
    }
}

class ViewSubscriberTypeB(store: Store<StringState>) : Subscriber<StringState> {
    init {
        store.subscribe(this)
    }

    override fun newState(state: StringState) {
        // do nothing
    }
}

class ViewSubscriberTypeC : Subscriber<StringState> {
    override fun newState(state: StringState) {
        // do nothing
    }
}

class DispatchingSubscriber(var store: Store<IntState>) : Subscriber<IntState> {

    override fun newState(state: IntState) {
        // Test if we've already dispatched this action to
        // avoid endless recursion
        if (state.number != 5) {
            this.store.dispatch(SetIntAction(5))
        }
    }
}

