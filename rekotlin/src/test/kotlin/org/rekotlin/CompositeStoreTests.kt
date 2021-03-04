package org.rekotlin

import java.util.IdentityHashMap
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class CompositeStoreTests {

    // State1
    private data class State1(
        val string: String = "",
        val string2: String = ""
    )

    private fun state1Reducer(action: Action, oldState: State1?): State1 {
        val state = oldState ?: State1()
        return when (action) {
            is SetState1String -> state.copy(string = action.value)
            is SetState1String2 -> state.copy(string2 = action.value)
            else -> state
        }
    }

    private val state1Middleware = testMiddleware<State1> { action, getState ->
        println(">> state1Middleware($action, ${getState()})")
    }

    // State2
    private data class State2(
        val integer: Int = 0
    )

    private fun state2Reducer(action: Action, oldState: State2?): State2 {
        val state = oldState ?: State2()
        return when (action) {
            is SetState2Integer -> state.copy(integer = action.value)
            else -> state
        }
    }

    private val state2Middleware = testMiddleware<State2> { action, getState ->
        println(">> state2Middleware($action, ${getState()})")
    }

    // State3
    private data class State3(
        val boolean: Boolean? = false
    )

    private fun state3Reducer(action: Action, oldState: State3?): State3 {
        val state = oldState ?: State3()
        return when (action) {
            is SetState3Boolean -> state.copy(boolean = action.value)
            else -> state
        }
    }

    private val state3Middleware = testMiddleware<State3> { action, getState ->
        println(">> state3Middleware($action, ${getState()})")
    }

    // Actions
    private data class SetState1String(val value: String) : Action
    private data class SetState1String2(val value: String) : Action
    private data class SetState2Integer(val value: Int) : Action
    private data class SetState3Boolean(val value: Boolean?) : Action

    // Stores
    private val store1 = store(::state1Reducer, State1(), state1Middleware, thunkMiddleware())
    private val store2 = store(::state2Reducer, State2(), state2Middleware, thunkMiddleware())
    private val store3 = store(::state3Reducer, State3(), state3Middleware, thunkMiddleware())

    // Composite Store
    private data class TestState(
        val state1: State1,
        val state2: State2,
        val state3: State3
    )

    private val testStore = composeStores(store1, store2, store3, thunkMiddleware()) { a, b, c ->
        TestState(
            state1 = a,
            state2 = b,
            state3 = c
        )
    }

    data class SelectState(
        val string: String,
        val int: Int,
        val bool: Boolean?
    )

    // Composite2 Stores

    private data class TestState12(
        val state1: State1,
        val state2: State2
    )

    private data class TestState23(
        val state2: State2,
        val state3: State3
    )

    private val testStore12 = composeStores(store1, store2, thunkMiddleware()) { a, b ->
        TestState12(
            state1 = a,
            state2 = b
        )
    }
    private val testStore23 = composeStores(store2, store3, thunkMiddleware()) { a, b ->
        TestState23(
            state2 = a,
            state3 = b
        )
    }

    @AfterEach
    fun afterEach() {
        assert(state1Middleware.duplicateCount == 0) { "state1Middleware.duplicateCount != 0 (${state1Middleware.duplicateCount})\n > ${state1Middleware.duplicates}" }
        assert(state2Middleware.duplicateCount == 0) { "state2Middleware.duplicateCount != 0 (${state2Middleware.duplicateCount})\n > ${state2Middleware.duplicates}" }
        assert(state3Middleware.duplicateCount == 0) { "state3Middleware.duplicateCount != 0 (${state3Middleware.duplicateCount})\n > ${state3Middleware.duplicates}" }
    }

    //
    // composite store
    //

    @Test
    fun `initial subscribe receives current state`() {
        val subscriber = TestSubscriber<TestState>()
        testStore.subscribe(subscriber)

        assert(subscriber.receivedInitialState) { "subscriber.receivedInitialState != true" }
        assert(subscriber.callCount == 0) { "subscriber.callCount != 0 (${subscriber.callCount})" }
    }

    @Test
    fun `effect dispatched to composite store triggers composite store listener`() {
        val listener = TestListener2<TestEffect>()
        testStore.listen(listener)
        testStore.dispatch(TestEffect)

        assert(listener.callCount == 1) { "listener.callCount != 1 (${listener.callCount})" }
    }

    @Test
    fun `action dispatched to composite store triggers composite store subscriber`() {
        val subscriber = TestSubscriber<TestState>()
        testStore.subscribe(subscriber)
        testStore.dispatch(SetState1String("test"))

        assert(subscriber.receivedInitialState) { "subscriber.receivedInitialState != true" }
        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }

    @Test
    fun `actions dispatched to composite store updates relevant stores and calls relevant subscribers`() {
        val subscriber1 = TestSubscriber<State1>()
        val subscriber2 = TestSubscriber<State2>()
        val subscriber3 = TestSubscriber<State3>()
        val testSubscriber = TestSubscriber<TestState>()

        store1.subscribe(subscriber1)
        store2.subscribe(subscriber2)
        store3.subscribe(subscriber3)
        testStore.subscribe(testSubscriber)

        testStore.dispatch(SetState1String(value = "new value!"))
        assert(subscriber1.callCount == 1) { "subscriber1.callCount != 1 ${subscriber1.callCount}" }
        assert(subscriber2.callCount == 0) { "subscriber2.callCount != 0 ${subscriber2.callCount}" }
        assert(subscriber3.callCount == 0) { "subscriber3.callCount != 0 ${subscriber3.callCount}" }
        assert(testSubscriber.callCount == 1) { "testSubscriber.callCount != 1 ${testSubscriber.callCount}" }
        assert(subscriber1.lastState == testStore.state.state1) { "${subscriber1.lastState} != ${testStore.state.state1}" }

        testStore.dispatch(SetState2Integer(value = 123))
        assert(subscriber1.callCount == 1) { "subscriber1.callCount != 1 ${subscriber1.callCount}" }
        assert(subscriber2.callCount == 1) { "subscriber2.callCount != 1 ${subscriber2.callCount}" }
        assert(subscriber3.callCount == 0) { "subscriber3.callCount != 0 ${subscriber3.callCount}" }
        assert(testSubscriber.callCount == 2) { "testSubscriber.callCount != 2 ${testSubscriber.callCount}" }
        assert(subscriber2.lastState == testStore.state.state2) { "${subscriber2.lastState} != ${testStore.state.state2}" }

        testStore.dispatch(SetState3Boolean(value = true))
        assert(subscriber1.callCount == 1) { "subscriber1.callCount != 1 ${subscriber1.callCount}" }
        assert(subscriber2.callCount == 1) { "subscriber2.callCount != 1 ${subscriber2.callCount}" }
        assert(subscriber3.callCount == 1) { "subscriber3.callCount != 1 ${subscriber3.callCount}" }
        assert(testSubscriber.callCount == 3) { "testSubscriber.callCount != 2 ${testSubscriber.callCount}" }
        assert(subscriber3.lastState == testStore.state.state3) { "${subscriber3.lastState} != ${testStore.state.state3}" }
    }

    @Test
    fun `effect dispatched to composite store triggers effect dispatch and triggers composite store listeners`() {
        val listener1 = TestListener2<TestEffect>()
        val listener2 = TestListener2<TestEffect2>()

        testStore.listen(listener1)
        testStore.listen(listener2)
        store2.listenTo<TestEffect> { testStore.dispatch(TestEffect2) }

        testStore.dispatch(TestEffect)

        assert(listener1.callCount == 1) { "listener1.callCount != 1 (${listener1.callCount})" }
        assert(listener2.callCount == 1) { "listener2.callCount != 1 (${listener2.callCount})" }
    }

    //
    // composite store select
    //

    @Test
    fun `subscribe with select and unsubscribe`() {
        val subscriber = TestSubscriber<SelectState>()
        testStore.subscribe(subscriber) {
            select {
                SelectState(
                    state1.string,
                    state2.integer,
                    state3.boolean
                )
            }
        }

        testStore.dispatch(SetState1String(value = "new value!"))
        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }

        testStore.unsubscribe(subscriber)
        testStore.dispatch(SetState1String(value = "another value!"))
        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }

        assert(testStore.state.state1 == store1.state)
        assert(testStore.state.state2 == store2.state)
        assert(testStore.state.state3 == store3.state)
    }

    @Test
    fun `subscribe with select doesn't send duplicates`() {
        val subscriber = TestSubscriber<SelectState>()
        testStore.subscribe(subscriber) {
            select {
                SelectState(
                    state1.string,
                    state2.integer,
                    state3.boolean
                )
            }
        }

        testStore.dispatch(SetState1String(value = "new value!"))
        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }

        testStore.dispatch(SetState1String(value = "new value!"))
        testStore.dispatch(SetState1String2(value = "other string"))
        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }

    //
    // composite store > store
    //

    @Test
    fun `effect dispatched to store triggers composite store listener`() {
        val listener = TestListener2<TestEffect>()
        testStore.listen(listener)
        store1.dispatch(TestEffect)

        assert(listener.callCount == 1) { "listener.callCount != 1 (${listener.callCount})" }
    }

    @Test
    fun `action dispatched to store triggers composite store subscriber`() {
        val subscriber = TestSubscriber<TestState>()
        testStore.subscribe(subscriber)
        store1.dispatch(SetState1String(value = "test"))

        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }

    @Test
    fun `unrelated action dispatched to store does not trigger composite store subscriber`() {
        val subscriber = TestSubscriber<TestState>()
        testStore.subscribe(subscriber)
        store1.dispatch(SetState2Integer(value = 123))

        assert(subscriber.callCount == 0) { "subscriber.callCount != 0 (${subscriber.callCount})" }
    }

    //
    // store > composite store
    //

    @Test
    fun `effect dispatched to composite store triggers store listener`() {
        val listener = TestListener2<TestEffect>()
        store1.listen(listener)
        testStore.dispatch(TestEffect)

        assert(listener.callCount == 1) { "listener.callCount != 1 (${listener.callCount})" }
    }

    @Test
    fun `action dispatched to composite store triggers store subscriber`() {
        val subscriber = TestSubscriber<State1>()
        store1.subscribe(subscriber)
        testStore.dispatch(SetState1String(value = "test"))

        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }

    @Test
    fun `unrelated action dispatched to composite store does not trigger store subscriber`() {
        val subscriber = TestSubscriber<State1>()
        store1.subscribe(subscriber)
        testStore.dispatch(SetState2Integer(value = 123))

        assert(subscriber.callCount == 0) { "subscriber.callCount != 0 (${subscriber.callCount})" }
    }

    //
    // store > store
    //

    @Test
    fun `effect dispatched to store does not trigger other store listener`() {
        val listener = TestListener2<TestEffect>()
        store2.listen(listener)
        store1.dispatch(TestEffect)

        assert(listener.callCount == 0) { "listener.callCount != 0 (${listener.callCount})" }
    }

    @Test
    fun `action dispatched to store does not trigger other store subscriber`() {
        val subscriber = TestSubscriber<State2>()
        store2.subscribe(subscriber)
        store1.dispatch(SetState1String(value = "test"))

        assert(subscriber.callCount == 0) { "subscriber.callCount != 0 (${subscriber.callCount})" }
    }

    @Test
    fun `unrelated action dispatched to store does not trigger other store subscriber`() {
        val subscriber = TestSubscriber<State2>()
        store2.subscribe(subscriber)
        store1.dispatch(SetState2Integer(value = 123))

        assert(subscriber.callCount == 0) { "subscriber.callCount != 0 (${subscriber.callCount})" }
    }

    //
    // composite store (1) > composite store (2)
    //

    @Test
    fun `effect dispatched to composite store triggers other composite store listener`() {
        val listener = TestListener2<TestEffect>()
        testStore12.listen(listener)
        testStore23.dispatch(TestEffect)

        assert(listener.callCount == 1) { "listener.callCount != 1 (${listener.callCount})" }
    }

    @Test
    fun `action dispatched to composite store triggers other composite store subscriber`() {
        val subscriber = TestSubscriber<TestState12>()
        testStore12.subscribe(subscriber)
        testStore23.dispatch(SetState2Integer(value = 123))

        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }

    @Test
    fun `unrelated action dispatched to composite store does not trigger other composite store subscriber`() {
        val subscriber = TestSubscriber<TestState12>()
        testStore12.subscribe(subscriber)
        testStore23.dispatch(SetState3Boolean(value = true))

        assert(subscriber.callCount == 0) { "subscriber.callCount != 0 (${subscriber.callCount})" }
    }

    //
    // middleware
    //

    @Test
    fun `cross composite store calls middleware once for duplicate stores`() {
        data class TestState23And12(
            val state2: State2,
            val state3: State3,
            val state1: State1
        )

        val testStore23And12 = composeStores(
            store2,
            store3,
            testStore12,
            thunkMiddleware()
        ) { a, b, c ->
            TestState23And12(
                state2 = a,
                state3 = b,
                state1 = c.state1
            )
        }

        val subscriber = TestSubscriber<State2>()
        store2.subscribe(subscriber)

        testStore23And12.dispatch(SetState2Integer(value = 123))

        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }

    @Test
    fun `cross composite store calls middleware once for duplicate stores2`() {
        data class State13(
            val state1: State1,
            val state3: State3
        )

        val testStore13 = composeStores(
            store1,
            store3,
            thunkMiddleware()
        ) { a, b ->
            State13(
                state1 = a,
                state3 = b
            )
        }

        data class TestState2And3And13And123(
            val state2: State2,
            val state3: State3,
            val state13: State13,
            val testState: TestState
        )

        val testStore2And3And13And123 = composeStores(
            store2,
            store3,
            testStore,
            testStore13,
            thunkMiddleware()
        ) { a, b, c, d ->
            TestState2And3And13And123(
                state2 = a,
                state3 = b,
                state13 = d,
                testState = c
            )
        }

        val subscriber = TestSubscriber<State3>()
        store3.subscribe(subscriber)

        testStore2And3And13And123.dispatch(SetState3Boolean(value = true))

        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }

    //
    // middleware (thunk)
    //

    @Test
    fun `thunk dispatched to composite store affects relevant stores`() {
        val store3Subscriber = TestSubscriber<State3> {
            println("Got $it")
            if (it.boolean == null) {
                store3.dispatch(SetState3Boolean(value = true))
            }
        }
        store3.subscribe(store3Subscriber)

        val store2Thunk1 = testThunk<State2> { _, _ ->
            testStore.dispatch(SetState3Boolean(value = null))
        }
        val store2Thunk2 = testThunk<State2> { _, _ ->
            testStore.dispatch(SetState3Boolean(value = null))
        }

        val testStoreThunk = testThunk<TestState> { _, _ ->
            store2.dispatch(store2Thunk1)
        }
        val store1Thunk = testThunk<State1> { _, _ ->
            store2.dispatch(store2Thunk2)
        }

        testStore.dispatch(testStoreThunk)
        assert(testStoreThunk.callCount == 1) { "testStoreThunk.callCount != 1 (${testStoreThunk.callCount})" }
        assert(store2Thunk1.callCount == 1) { "store2Thunk1.callCount != 1 (${store2Thunk1.callCount})" }

        store1.dispatch(store1Thunk)
        assert(store1Thunk.callCount == 1) { "store1Thunk.callCount != 1 (${store1Thunk.callCount})" }
        assert(store2Thunk2.callCount == 1) { "store2Thunk2.callCount != 1 (${store2Thunk2.callCount})" }

        // false(init) -> null -> true -> null -> true
        val actual = store3Subscriber.history.map { it.boolean }
        val expectedHistory = listOf(false, null, true, null, true)
        assert(actual == expectedHistory) { "store3Subscriber.history != expectedHistory ($actual != $expectedHistory)" }
        assert(store3.state.boolean == true) { "store3.state.boolean != null (${store3.state.boolean})" }
    }

    @Test
    fun `thunk dispatched to composite store then thunk dispatch affects relevant store`() {
        val store3Subscriber = TestSubscriber<State3> {
            if (it.boolean == null) {
                store3.dispatch(SetState3Boolean(value = true))
            }
        }
        store3.subscribe(store3Subscriber)

        val thunk = testThunk<TestState> { dispatch, _ ->
            dispatch(SetState1String("Loading..."))
            dispatch(SetState3Boolean(value = null))
        }

        println("\n=== testStore.dispatch(store1Thunk) ===")
        testStore.dispatch(thunk)
        assert(store3.state.boolean == true) { "store3.state.boolean != true (${store3.state.boolean})" }
    }

    @Test
    fun `thunk dispatched to composite store triggers other composite store listener`() {
        val initialValue = testStore23.state.state2.integer
        val add = 10

        val store23Thunk = testThunk<TestState23> { dispatch, getState ->
            dispatch(SetState2Integer(value = (getState()?.state2?.integer ?: -1) + add))
        }

        val subscriber = TestSubscriber<TestState12>()
        testStore12.subscribe(subscriber)
        testStore23.dispatch(store23Thunk)

        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
        assert(store2.state.integer == initialValue + add) { "store2.state.integer != initialValue+add (${store2.state.integer})" }
    }
}

private class TestSubscriber<S>(private val block: (S) -> Unit = {}) : Subscriber<S> {
    var receivedInitialState: Boolean = false
        private set
    val lastState: S?
        get() = _history.lastOrNull()
    val callCount
        get() = _history.size - 1
    val history: List<S>
        get() = _history

    private val _history: MutableList<S> = mutableListOf()

    override fun newState(state: S) {
        if (_history.isEmpty()) {
            receivedInitialState = true
        }

        _history.add(state)
        block(state)
    }
}

private inline fun <reified EffectType : Effect> SubscribeStore<*>.listen(listener: Listener<EffectType>) =
    subscribe(listener) { it as? EffectType }

private class TestListener2<E>(private val block: (E) -> Unit = {}) : Listener<E> {
    val callCount
        get() = _history.size

    private val _history: MutableList<E> = mutableListOf()

    override fun onEffect(effect: E) {
        _history.add(effect)
        block(effect)
    }
}

private fun <S : Any> testThunk(
    block: ((DispatchFunction, () -> S?) -> Unit)? = null
) = TestThunk(block)

private class TestThunk<S : Any>(private val block: ((DispatchFunction, () -> S?) -> Unit)? = null) : Thunk<S> {
    var callCount = 0
        private set

    override fun invoke(dispatch: DispatchFunction, getState: () -> S?) {
        callCount++
        block?.invoke(dispatch, getState)
    }
}

private fun <S> testMiddleware(
    next: (action: Dispatchable, getState: () -> S?) -> Unit
) = TestMiddleware(next)

private class TestMiddleware<S>(
    private val next: (action: Dispatchable, getState: () -> S?) -> Unit
) : Middleware<S> {
    private val dispatchableHistory = IdentityHashMap<Dispatchable, Int>()

    var callCount = 0
    val duplicates get() = dispatchableHistory.filter { it.value != 1 }
    val duplicateCount get() = duplicates.size

    override fun invoke(dispatch: DispatchFunction, getState: () -> S?): (DispatchFunction) -> DispatchFunction = { next ->
            { action ->
                callCount++
                dispatchableHistory[action] = (dispatchableHistory[action] ?: 0) + 1

                this.next(action, getState)
                next(action)
            }
        }
}
