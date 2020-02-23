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

internal val firstMiddleware: Middleware<State> = { _, _ ->
    { next ->
        { action ->
            (action as? SetStringAction)?.let {
                it.value += " First Middleware"
                next(action)
            } ?: next(action)
        }
    }
}

internal val secondMiddleware: Middleware<State> = { _, _ ->
    { next ->
        { action ->
            (action as? SetStringAction)?.let {
                it.value += " Second Middleware"
                next(action)
            } ?: next(action)
        }
    }
}

internal val dispatchingMiddleware: Middleware<State> = { dispatch, _ ->
    { next ->
        { action ->
            (action as? SetIntAction)?.let {
                dispatch(SetStringAction("${it.value ?: 0}"))
            }

            next(action)
        }
    }
}

internal val stateAccessingMiddleware: Middleware<StringState> = { dispatch, getState ->
    { next ->
        { action ->

            val appState = getState()
            val stringAction = action as? SetStringAction

            // avoid endless recursion by checking if we've dispatched exactly this action
            if (appState?.name == "OK" && stringAction?.value != "Not OK") {
                // dispatch a new action
                dispatch(SetStringAction("Not OK"))

                // and swallow the current one
                dispatch(NoOpAction())
            } else {
                next(action)
            }
        }
    }
}

internal class StoreMiddlewareTests {

    /**
     * it can decorate dispatch function
     */
    @Test
    fun testDecorateDispatch() {

        val store = Store(
            reducer = ::stringReducer,
            state = StringState(),
            middleware = listOf(firstMiddleware, secondMiddleware)
        )

        val subscriber = FakeSubscriberWithHistory<StringState>()
        store.subscribe(subscriber)

        val action = SetStringAction("OK")
        store.dispatch(action)

        assertEquals("OK First Middleware Second Middleware", store.state.name)
    }

    /**
     * it can dispatch actions
     */
    @Test
    fun testCanDispatch() {

        val store = Store(
            reducer = ::stringReducer,
            state = StringState(),
            middleware = listOf(firstMiddleware, secondMiddleware, dispatchingMiddleware)
        )

        val subscriber = FakeSubscriberWithHistory<StringState>()
        store.subscribe(subscriber)

        val action = SetIntAction(10)
        store.dispatch(action)

        assertEquals("10 First Middleware Second Middleware", store.state.name)
    }

    /**
     * it middleware can access the store's state
     */
    @Test
    fun testMiddlewareCanAccessState() {

        var state = StringState()
        state = state.copy(name = "OK")

        val store = Store(
            reducer = ::stringReducer,
            state = state,
            middleware = listOf(stateAccessingMiddleware)
        )

        store.dispatch(SetStringAction("Action That Won't Go Through"))
        assertEquals("Not OK", store.state.name)
    }
}
