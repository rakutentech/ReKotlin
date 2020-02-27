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

import org.junit.jupiter.api.Test

class MiddlewareTests {

    private val decoratingMiddleware: Middleware<Any> = { _, _ ->
        { next ->
            { action ->
                (action as? StringAction)?.let {

                    next(IntAction(7))
                } ?: next(action)
            }
        }
    }

    private val dropAllMiddleware: Middleware<Any> = { _, _ ->
        { _ ->
            { _ -> }
        }
    }

    @Test
    fun `should modify actions before they reach the reducer`() {
        val reducer = FakeReducer(StringState())
        val store = store(reducer, StringState(), decoratingMiddleware)
        val action = StringAction("OK")

        store.dispatch(action)

        assert(action != reducer.lastAction)
    }

    @Test
    fun `should be able to drop actions`() {
        val reducer = FakeReducer(StringState())
        val store = store(reducer, StringState(), dropAllMiddleware)
        val action = StringAction("OK")

        store.dispatch(action)
        store.dispatch(action)
        store.dispatch(action)

        assert(reducer.actions.isEmpty())
    }

    private val initializingMiddleware: Middleware<StringState> = { dispatch, getState ->
        { next ->
            { action ->
                val state = getState()

                if (action !is StringAction && (state == null || state.name == "initial")) {
                    dispatch(StringAction("Initialized!"))
                }
                next(action)
            }
        }
    }

    @Test
    fun `should be able to execute conditional logic based on reading state`() {
        val reducer = FakeReducer(StringState("")) { a, s ->
            val state = s ?: StringState()

            if (a is StringAction) {
                state.copy(name = a.value)
            } else {
                state
            }
        }
        val store = store(reducer, StringState("initial"), initializingMiddleware)
        val baseline = reducer.actions.size

        store.dispatch(TestAction)

        assert(reducer.actions.size == baseline + 2) // dispatched additional action based on state

        store.dispatch(TestAction)

        assert(reducer.actions.size == baseline + 3) // no additional actions, based on changed state
    }
}
