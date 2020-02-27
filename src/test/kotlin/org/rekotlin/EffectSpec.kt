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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

object AppState

@Suppress("UNUSED_PARAMETER")
fun reducer(action: Action, state: AppState?) = state ?: AppState

object TestEffect : Effect
object TestEffect2 : Effect

class TestListener<E: Effect>(private val block: (E) -> Unit): Listener<E> {
    override fun onEffect(effect: E) = block(effect)
}

class SimpleSubscriber<S>(private val block: (S) -> Unit): Subscriber<S> {
    override fun newState(state: S) = block(state)
}

internal class EffectTest {

    private val store = Store(::reducer, null)

    @Test
    fun `should not subscribe any listeners when subscribing to state`() {
        store.subscribe(SimpleSubscriber {})

        assertEquals(0, store.listeners.count())
    }

    @Test
    fun `should subscribe listeners`() {
        val listener = TestListener<Effect> { }
        store.subscribe(listener)

        assertEquals(1, store.listeners.count())
    }

    @Test
    fun `should register a listener only once`() {
        val listener = TestListener<Effect> { }

        store.subscribe(listener)
        store.subscribe(listener)

        assertEquals(1, store.listeners.count())
    }

    @Test
    fun `should dispatch effects to listeners`() {
        var effect: Effect? = null
        val listener = TestListener<Effect> { effect = it }
        store.subscribe(listener)

        store.dispatch(TestEffect)

        assertNotNull(effect)
    }

    @Test
    fun `should unsubscribe listener`() {
        var effect: Effect? = null
        val listener = TestListener<Effect> { effect = it }
        store.subscribe(listener)

        store.unsubscribe(listener)
        store.dispatch(TestEffect)

        assertNull(effect)
    }

    @Test
    fun `should subscribe listener with selector` () {
        val listener = TestListener<TestEffect> { }

        store.subscribe(listener) { it as? TestEffect }

        assertEquals(1, store.listeners.count())
    }

    @Test
    fun `should unsubscribe listener that subscribed with selector` () {
        val listener = TestListener<TestEffect> { }
        store.subscribe(listener) { it as? TestEffect }

        store.unsubscribe(listener)

        assertEquals(0, store.listeners.count())
    }

    @Test
    fun `should not pass effects that were not selected by the selector` () {
        var effect: Effect? = null
        val listener = TestListener<TestEffect> { effect = it }
        store.subscribe(listener) { it as? TestEffect }

        store.dispatch(TestEffect2)

        assertNull(effect)
    }

    @Test
    fun `should only pass effects that are selected by the selector` () {
        val effects: MutableList<Effect> = mutableListOf()
        val listener = TestListener<TestEffect> { effects.add(it)}
        store.subscribe(listener) { it as? TestEffect }

        store.dispatch(TestEffect)
        store.dispatch(TestEffect2)

        assertEquals(1, effects.count())
        assertTrue(effects[0] is TestEffect)
    }

    @Test
    fun `should allow to dispatch another effect in effect listener` () {
        var effect: TestEffect2? = null
        store.subscribe(TestListener { store.dispatch(TestEffect2) }) { it as? TestEffect }
        store.subscribe(TestListener { effect = it }) { it as? TestEffect2 }

        store.dispatch(TestEffect)

        assertNotNull(effect)
        // also implictily assert that this doesn't cause a failure
    }

    @Test
    fun `should fail to dispatch the same effect in effect listener` () {
        store.subscribe(TestListener { store.dispatch(TestEffect) }) { it as? TestEffect }

        try {
            store.dispatch(TestEffect)
            fail<Unit>("Should Overflow!")
        } catch (e: StackOverflowError) {
            assertNotNull(e)
        }
    }
}
