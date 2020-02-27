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

import org.junit.jupiter.api.Assertions

data class IntState(val number: Int? = null)
data class StringState(val name: String = "Initial")

data class SubState(val value: Int)

data class TestCustomAppState(val subState: SubState) {
    constructor(subState: Int = 0) : this(SubState(subState))
}

data class IntAction(val value: Int?) : Action
data class StringAction(val value: String) : Action
data class CustomSubStateAction(val value: Int) : Action

fun intReducer(action: Action, state: IntState?): IntState {
    val oldState = state ?: IntState()

    return when (action) {
        is IntAction -> oldState.copy(number = action.value)
        else -> oldState

    }
}

fun stringReducer(action: Action, state: StringState?): StringState {
    val oldState = state ?: StringState()

    return when (action) {
        is StringAction -> oldState.copy(name = action.value)
        else -> oldState
    }
}

fun customAppStateReducer(action: Action, state: TestCustomAppState?): TestCustomAppState {
    val oldState = state ?: TestCustomAppState()

    return when (action) {
        is CustomSubStateAction -> oldState.copy(oldState.subState.copy(action.value))
        else -> oldState
    }

}

object TestAction: Action

class FakeReducer<T>(
        initial: T,
        private val reduce: (Action, T?) -> T = { _, s -> s ?: initial}
): Reducer<T> {
    val lastAction: Action?
        get() = _actions.lastOrNull()
    val actions: List<Action>
        get() = _actions
    private val _actions: MutableList<Action> = mutableListOf()
    override fun invoke(action: Action, state: T?): T {
        _actions.add(action)
        return reduce(action, state)
    }
}

class FakeSubscriber<T>(private val block: (T) -> Unit = {}) : Subscriber<T> {
    val lastState: T?
            get() = _history.lastOrNull()
    val callCount
        get() = _history.size
    val history: List<T>
        get() = _history

    val _history: MutableList<T> = mutableListOf()

    override fun newState(state: T) {
        block(state)
        _history.add(state)
    }
}

fun assert(condition: Boolean) = Assertions.assertTrue(condition)