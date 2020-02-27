package org.rekotlin

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

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

class FakeReducer<T>(private val initial: T): Reducer<T> {
    val lastAction: Action?
        get() = if(_actions.isNotEmpty()) _actions.last() else null
    val actions: List<Action>
        get() = _actions
    private val _actions: MutableList<Action> = mutableListOf()
    override fun invoke(action: Action, state: T?): T {
        _actions.add(action)
        return state ?: initial
    }
}

object TestAction: Action

class ChildStoreActionsTests {
    private val parentReducer = FakeReducer("")
    private val childReducer = FakeReducer(1)

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun `dispatching action to parent should reach parent and child`() {
        val parent = ParentStore(parentReducer, "")
        val child = parent.childStore(childReducer, 7)

        parent.dispatch(TestAction)

        assertNotNull(parentReducer.lastAction)
        assertNotNull(childReducer.lastAction)
    }

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun `dispatching action to child should reach parent and child`() {
        val parent = ParentStore(parentReducer, "default")
        val child = parent.childStore(childReducer, 7)

        child.dispatch(TestAction)

        assertNotNull(parentReducer.lastAction)
        assertNotNull(childReducer.lastAction)
    }

    @Test
    fun `dispatching effect to parent should reach parent and child`() {
        val parent = ParentStore(parentReducer, "default")
        val child = parent.childStore(childReducer, 7)

        var parentEffect: Effect? = null
        var childEffect: Effect? = null

        parent.subscribe(listener { parentEffect = it })
        child.subscribe(listener { childEffect = it })

        parent.dispatch(TestEffect)

        assertNotNull(parentEffect)
        assertNotNull(childEffect)
    }

    @Test
    fun `dispatching effect to child should reach parent and child`() {
        val parent = ParentStore(parentReducer, "default")
        val child = parent.childStore(childReducer, 7)

        var parentEffect: Effect? = null
        var childEffect: Effect? = null

        parent.subscribe(listener { parentEffect = it })
        child.subscribe(listener { childEffect = it })

        child.dispatch(TestEffect)

        assertNotNull(parentEffect)
        assertNotNull(childEffect)
    }

    object ReplacedAction : Action

    private fun <T> replacingMiddleware(): Middleware<T> = { _, _ ->
        { next ->
            {
                next(ReplacedAction)
            }
        }
    }

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun `middleware should operate on actions dispatched to parent`() {
        val parent = ParentStore(parentReducer, "default", listOf(replacingMiddleware()))
        val child = parent.childStore(childReducer, 7)

        parent.dispatch(TestAction)

        assert(parentReducer.lastAction is ReplacedAction)
        assert(childReducer.lastAction is ReplacedAction)
    }

    @Test
    fun `middleware should operate on actions dispatched to child`() {
        val parent = ParentStore(parentReducer, "default", listOf(replacingMiddleware()))
        val child = parent.childStore(childReducer, 7)

        child.dispatch(TestAction)

        assert(parentReducer.lastAction is ReplacedAction)
        assert(childReducer.lastAction is ReplacedAction)
    }

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun `should delegate actions to multiple children when dispatched to parent`() {
        val parent = ParentStore(parentReducer, "default", listOf(replacingMiddleware()))
        val child1 = parent.childStore(childReducer, 1)
        val child2Reducer = FakeReducer(1)
        val child3Reducer = FakeReducer(1)
        val child2 = parent.childStore(child2Reducer, 2)
        val child3 = parent.childStore(child3Reducer, 3)

        parent.dispatch(TestAction)

        assertNotNull(parentReducer.lastAction)
        assertNotNull(childReducer.lastAction)
        assertNotNull(child2Reducer.lastAction)
        assertNotNull(child3Reducer.lastAction)
    }

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun `should delegate actions to multiple children when dispatched to a child`() {
        val parent = ParentStore(parentReducer, "default", listOf(replacingMiddleware()))
        val child1 = parent.childStore(childReducer, 1)
        val child2Reducer = FakeReducer(1)
        val child3Reducer = FakeReducer(1)
        val child2 = parent.childStore(child2Reducer, 2)
        val child3 = parent.childStore(child3Reducer, 3)

        child1.dispatch(TestAction)

        assert(parentReducer.actions.size == 1)
        assert(childReducer.actions.size == 1)
        assert(child2Reducer.actions.size == 1)
        assert(child3Reducer.actions.size == 1)

        child2.dispatch(TestAction)

        assert(parentReducer.actions.size == 2)
        assert(childReducer.actions.size == 2)
        assert(child2Reducer.actions.size == 2)
        assert(child3Reducer.actions.size == 2)

        child3.dispatch(TestAction)

        assert(parentReducer.actions.size == 3)
        assert(childReducer.actions.size == 3)
        assert(child2Reducer.actions.size == 3)
        assert(child3Reducer.actions.size == 3)
    }

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun `creating child store without initial state should dispatch init action to child store`() {
        val parent = ParentStore(parentReducer, "")
        val child = parent.childStore(childReducer)

        assert(parentReducer.actions.isEmpty())
        assert(childReducer.actions.size == 1)
        assert(childReducer.lastAction is ReKotlinInit)
    }

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun `creating child store with initial state should not dispatch any action to any store`() {
        val parent = ParentStore(parentReducer, "")
        val child = parent.childStore(childReducer, 7)

        assert(parentReducer.actions.isEmpty())
        assert(childReducer.actions.isEmpty())
    }
}