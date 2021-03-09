package org.rekotlin

import java.util.IdentityHashMap
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

// <editor-fold desc="State 1">
private data class State1(
    val string: String = "",
    val string2: String = ""
)

private data class SetState1String(val value: String) : Action
private data class SetState1String2(val value: String) : Action

private fun state1Reducer(action: Action, oldState: State1?): State1 {
    val state = oldState ?: State1()
    return when (action) {
        is SetState1String -> state.copy(string = action.value)
        is SetState1String2 -> state.copy(string2 = action.value)
        else -> state
    }
}

// </editor-fold>

// <editor-fold desc="State 2">
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

private data class SetState2Integer(val value: Int) : Action
// </editor-fold>

// <editor-fold desc="State 3">
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

private data class SetState3Boolean(val value: Boolean?) : Action
// </editor-fold>

class CompositeStoreTests {

    // Middlewares
    private val state1Middleware = CountingMiddleware<State1>()
    private val state2Middleware = CountingMiddleware<State2>()
    private val state3Middleware = CountingMiddleware<State3>()

    // Stores
    private val store1 = store(::state1Reducer, State1(), state1Middleware, LoggingMiddleware("store 1"), thunkMiddleware())
    private val store2 = store(::state2Reducer, State2(), state2Middleware, LoggingMiddleware("store 2"), thunkMiddleware())
    private val store3 = store(::state3Reducer, State3(), state3Middleware, LoggingMiddleware("store 3"), thunkMiddleware())

    // Composite Store
    private data class CompositeState(
        val state1: State1,
        val state2: State2,
        val state3: State3
    )

    private val compositeStore = composeStores(store1, store2, store3, LoggingMiddleware("store 123"), thunkMiddleware()) { a, b, c ->
        CompositeState(
            state1 = a,
            state2 = b,
            state3 = c
        )
    }

    @AfterEach
    fun `invariant - never dispatch duplicate Actions or Effects`() {
        assert(state1Middleware.duplicateCount == 0) { "state1Middleware.duplicateCount != 0 (${state1Middleware.duplicateCount})\n > ${state1Middleware.duplicates}" }
        assert(state2Middleware.duplicateCount == 0) { "state2Middleware.duplicateCount != 0 (${state2Middleware.duplicateCount})\n > ${state2Middleware.duplicates}" }
        assert(state3Middleware.duplicateCount == 0) { "state3Middleware.duplicateCount != 0 (${state3Middleware.duplicateCount})\n > ${state3Middleware.duplicates}" }
    }

    // <editor-fold desc="composite store">
    @Test
    fun `should receive current state on subscribe`() {
        val subscriber = TestSubscriber<CompositeState>()

        compositeStore.subscribe(subscriber)

        assert(subscriber.receivedInitialState) { "subscriber.receivedInitialState != true" }
        assert(subscriber.callCount == 0) { "subscriber.callCount != 0 (${subscriber.callCount})" }
    }

    @Test
    fun `should notify composite store listener on effect dispatch to composite store`() {
        val listener = CountingListener<TestEffect>().also { compositeStore.listen(it) }

        compositeStore.dispatch(TestEffect)

        assert(listener.callCount == 1) { "listener.callCount != 1 (${listener.callCount})" }
    }

    @Test
    fun `should notify all stores subscriber on action dispatch to composite store`() {
        val subscriber = TestSubscriber<CompositeState>().also { compositeStore.subscribe(it) }

        compositeStore.dispatch(SetState1String("test"))

        assert(subscriber.receivedInitialState) { "subscriber.receivedInitialState != true" }
        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }

    @Test
    fun `should notify all stores on dispatch action to composite store`() {
        compositeStore.dispatch(SetState2Integer(value = 123))

        assert(state1Middleware.callCount == 1) { "state1Middleware.callCount != 1 ${state1Middleware.callCount}" }
        assert(state2Middleware.callCount == 1) { "state2Middleware.callCount != 1 ${state2Middleware.callCount}" }
        assert(state3Middleware.callCount == 1) { "state3Middleware.callCount != 1 ${state3Middleware.callCount}" }
    }

    @Test
    fun `should notify subscriber to composite store and subscriber to store with identical state`() {
        val subscriber1 = TestSubscriber<State1>().also { store1.subscribe(it) }
        val subscriber2 = TestSubscriber<State2>().also { store2.subscribe(it) }
        val subscriber3 = TestSubscriber<State3>().also { store3.subscribe(it) }
        val testSubscriber = TestSubscriber<CompositeState>().also { compositeStore.subscribe(it) }

        compositeStore.dispatch(SetState1String(value = "new value!"))
        assert(subscriber1.lastState == testSubscriber.lastState?.state1) { "${subscriber1.lastState} != ${testSubscriber.lastState?.state1}" }
        assert(store1.state == compositeStore.state.state1) { "${store1.state} != ${compositeStore.state.state1}" }

        compositeStore.dispatch(SetState2Integer(value = 123))
        assert(subscriber2.lastState == testSubscriber.lastState?.state2) { "${subscriber2.lastState} != ${testSubscriber.lastState?.state2}" }
        assert(store2.state == compositeStore.state.state2) { "${store2.state} != ${compositeStore.state.state2}" }

        compositeStore.dispatch(SetState3Boolean(value = true))
        assert(subscriber3.lastState == testSubscriber.lastState?.state3) { "${subscriber3.lastState} != ${testSubscriber.lastState?.state3}" }
        assert(store3.state == compositeStore.state.state3) { "${store3.state} != ${compositeStore.state.state3}" }
    }

    @Test
    fun `should not notify subscribers of store 2 & 3 when dispatching an action that only affects store 1 to the composite store`() {
        val subscriber2 = TestSubscriber<State2>().also { store2.subscribe(it) }
        val subscriber3 = TestSubscriber<State3>().also { store3.subscribe(it) }

        compositeStore.dispatch(SetState1String("new value!"))
        assert(subscriber2.callCount == 0) { "subscriber2.callCount != 0 ${subscriber2.callCount}" }
        assert(subscriber3.callCount == 0) { "subscriber3.callCount != 0 ${subscriber3.callCount}" }
    }

    @Test
    fun `should not notify subscribers of store 1 & 3 when dispatching an action that only affects store 2 to the composite store`() {
        val subscriber1 = TestSubscriber<State1>().also { store1.subscribe(it) }
        val subscriber3 = TestSubscriber<State3>().also { store3.subscribe(it) }

        compositeStore.dispatch(SetState2Integer(123))

        assert(subscriber1.callCount == 0) { "subscriber1.callCount != 1 ${subscriber1.callCount}" }
        assert(subscriber3.callCount == 0) { "subscriber3.callCount != 0 ${subscriber3.callCount}" }
    }

    @Test
    fun `should not notify subscribers of store 1 & 2 when dispatching an action that only affects store 3 to the composite store`() {
        val subscriber1 = TestSubscriber<State1>().also { store1.subscribe(it) }
        val subscriber2 = TestSubscriber<State2>().also { store2.subscribe(it) }

        compositeStore.dispatch(SetState3Boolean(true))

        assert(subscriber1.callCount == 0) { "subscriber1.callCount != 1 ${subscriber1.callCount}" }
        assert(subscriber2.callCount == 0) { "subscriber2.callCount != 1 ${subscriber2.callCount}" }
    }

    @Test
    fun `composite store should always notify its own subscribers when an a store's state changes`() {
        val testSubscriber = TestSubscriber<CompositeState>().also {
            compositeStore.subscribe(it)
        }

        compositeStore.dispatch(SetState1String("new value!"))
        assert(testSubscriber.callCount == 1) { "testSubscriber.callCount != 1 ${testSubscriber.callCount}" }

        compositeStore.dispatch(SetState2Integer(123))
        assert(testSubscriber.callCount == 2) { "testSubscriber.callCount != 2 ${testSubscriber.callCount}" }

        compositeStore.dispatch(SetState3Boolean(true))
        assert(testSubscriber.callCount == 3) { "testSubscriber.callCount != 2 ${testSubscriber.callCount}" }
    }
    // </editor-fold>

    // <editor-fold desc="composite store select">

    data class SelectState(
        val string: String,
        val int: Int,
        val bool: Boolean?
    )

    @Test
    fun `should be able to subscribe with select and unsubscribe on composite store`() {
        val subscriber = TestSubscriber<SelectState>()
        compositeStore.subscribe(subscriber) {
            select {
                SelectState(
                    state1.string,
                    state2.integer,
                    state3.boolean
                )
            }
        }

        compositeStore.dispatch(SetState1String(value = "new value!"))
        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }

        compositeStore.unsubscribe(subscriber)
        compositeStore.dispatch(SetState1String(value = "another value!"))
        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }

        assert(compositeStore.state.state1 == store1.state)
        assert(compositeStore.state.state2 == store2.state)
        assert(compositeStore.state.state3 == store3.state)
    }

    @Test
    fun `should not trigger subscriber multiple times when subscribed with select`() {
        val subscriber = TestSubscriber<SelectState>()
        compositeStore.subscribe(subscriber) {
            select {
                SelectState(
                    state1.string,
                    state2.integer,
                    state3.boolean
                )
            }
        }

        compositeStore.dispatch(SetState1String(value = "new value!"))
        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }

        compositeStore.dispatch(SetState1String(value = "new value!"))
        compositeStore.dispatch(SetState1String2(value = "other string"))
        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }
    // </editor-fold>

    // <editor-fold desc="Effects & Actions dispatched to store > composite store">
    @Test
    fun `should trigger composite store listener when effect dispatched to store`() {
        val listener = CountingListener<TestEffect>().also { compositeStore.listen(it) }

        store1.dispatch(TestEffect)

        assert(listener.callCount == 1) { "listener.callCount != 1 (${listener.callCount})" }
    }

    @Test
    fun `should trigger composite store subscriber when action dispatched to store`() {
        val subscriber = TestSubscriber<CompositeState>().also { compositeStore.subscribe(it) }

        store1.dispatch(SetState1String(value = "test"))

        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }

    @Test
    fun `should not notify composite store subscriber when dispatching an action that does not cause a state change`() {
        val subscriber = TestSubscriber<CompositeState>().also { compositeStore.subscribe(it) }

        store1.dispatch(SetState2Integer(value = 123))

        assert(subscriber.callCount == 0) { "subscriber.callCount != 0 (${subscriber.callCount})" }
    }
    // </editor-fold>

    // <editor-fold desc="Effects & Actions dispatched to composite store > store">
    @Test
    fun `should trigger store listener when effect dispatched to composite store`() {
        val listener1 = CountingListener<TestEffect>().also { store1.listen(it) }

        compositeStore.dispatch(TestEffect)

        assert(listener1.callCount == 1) { "listener.callCount != 1 (${listener1.callCount})" }
    }

    @Test
    fun `should trigger store subscriber when action dispatched to composite store`() {
        val subscriber1 = TestSubscriber<State1>().also { store1.subscribe(it) }

        compositeStore.dispatch(SetState1String(value = "test"))

        assert(subscriber1.callCount == 1) { "subscriber.callCount != 1 (${subscriber1.callCount})" }
    }

    @Test
    fun `should not trigger store subscriber when unrelated action dispatched to composite store`() {
        val subscriber1 = TestSubscriber<State1>().also { store1.subscribe(it) }

        compositeStore.dispatch(SetState2Integer(value = 123))

        assert(subscriber1.callCount == 0) { "subscriber.callCount != 0 (${subscriber1.callCount})" }
    }
    // </editor-fold>

    // <editor-fold desc="Effects & Actions dispatched to store > store">
    @Test
    fun `should not trigger store2 listener when effect dispatched to store1`() {
        val listener = CountingListener<TestEffect>().also { store2.listen(it) }

        store1.dispatch(TestEffect)

        assert(listener.callCount == 0) { "listener.callCount != 0 (${listener.callCount})" }
    }

    @Test
    fun `should not trigger store2 subscriber when action dispatched to store1`() {
        val subscriber = TestSubscriber<State2>().also { store2.subscribe(it) }

        store1.dispatch(SetState1String(value = "test"))

        assert(subscriber.callCount == 0) { "subscriber.callCount != 0 (${subscriber.callCount})" }
    }

    @Test
    fun `should not trigger store2 subscriber when unrelated action dispatched to store1`() {
        val subscriber = TestSubscriber<State2>().also { store2.subscribe(it) }

        store1.dispatch(SetState2Integer(value = 123))

        assert(subscriber.callCount == 0) { "subscriber.callCount != 0 (${subscriber.callCount})" }
    }
    // </editor-fold>

    // <editor-fold desc="more composite stores">
    private data class TestState12(
        val state1: State1,
        val state2: State2
    )

    private val compositeStore12 = composeStores(store1, store2, LoggingMiddleware("store 12"), thunkMiddleware()) { a, b ->
        TestState12(
            state1 = a,
            state2 = b
        )
    }

    private data class TestState23(
        val state2: State2,
        val state3: State3
    )

    private val compositeStore23 = composeStores(store2, store3, LoggingMiddleware("store 23"), thunkMiddleware()) { a, b ->
        TestState23(
            state2 = a,
            state3 = b
        )
    }
    // </editor-fold>

    // <editor-fold desc="composite store (12) > composite store (23)">
    @Test
    fun `should trigger other composite store listener when effect dispatched to composite store`() {
        val listener12 = CountingListener<TestEffect>().also { compositeStore12.listen(it) }

        compositeStore23.dispatch(TestEffect)

        assert(listener12.callCount == 1) { "listener.callCount != 1 (${listener12.callCount})" }
    }

    @Test
    fun `should trigger other composite store subscriber when action dispatched to composite store`() {
        val subscriber12 = TestSubscriber<TestState12>().also { compositeStore12.subscribe(it) }

        compositeStore23.dispatch(SetState2Integer(value = 123))

        assert(subscriber12.callCount == 1) { "subscriber.callCount != 1 (${subscriber12.callCount})" }
    }

    @Test
    fun `should not trigger other composite store subscriber when unrelated action dispatched to composite store`() {
        val subscriber12 = TestSubscriber<TestState12>().also { compositeStore12.subscribe(it) }

        compositeStore23.dispatch(SetState3Boolean(value = true))

        assert(subscriber12.callCount == 0) { "subscriber.callCount != 0 (${subscriber12.callCount})" }
    }
    // </editor-fold>

    // <editor-fold desc="middleware">
    @Test
    fun `should only call middleware once when composite store includes duplicate stores from composite stores`() {
        data class TestState23And12(
            val state2: State2,
            val state3: State3,
            val state1: State1
        )

        val testStore23And12 = composeStores(
            store2,
            store3,
            compositeStore12,
            LoggingMiddleware("store 2, 3 and 12"),
            thunkMiddleware()
        ) { a, b, c ->
            TestState23And12(
                state2 = a,
                state3 = b,
                state1 = c.state1
            )
        }

        val subscriber = TestSubscriber<State2>().also { store2.subscribe(it) }

        testStore23And12.dispatch(SetState2Integer(value = 123))

        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }

    @Test
    fun `should only call middleware once when many composite store includes multiple duplicate stores`() {
        data class State13(
            val state1: State1,
            val state3: State3
        )

        val store13 = composeStores(
            store1,
            store3,
            LoggingMiddleware("store 1 and 3"),
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
            val testState: CompositeState
        )

        val store23And13And123 = composeStores(
            store2,
            store3,
            compositeStore,
            store13,
            LoggingMiddleware("store 2, 3, 123 and 13"),
            thunkMiddleware()
        ) { a, b, c, d ->
            TestState2And3And13And123(
                state2 = a,
                state3 = b,
                state13 = d,
                testState = c
            )
        }

        val subscriber = TestSubscriber<State3>().also { store3.subscribe(it) }

        store23And13And123.dispatch(SetState3Boolean(value = true))

        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
    }
    // </editor-fold>

    // <editor-fold desc="thunk middleware">
    @Test
    fun `should affect relevant stores when thunk dispatched to composite store`() {
        val store3Subscriber = TestSubscriber<State3> {
            println("Got $it")
            if (it.boolean == null) {
                store3.dispatch(SetState3Boolean(value = true))
            }
        }
        store3.subscribe(store3Subscriber)

        val store2Thunk1 = testThunk<State2> { _, _ ->
            compositeStore.dispatch(SetState3Boolean(value = null))
        }
        val store2Thunk2 = testThunk<State2> { _, _ ->
            compositeStore.dispatch(SetState3Boolean(value = null))
        }

        val testStoreThunk = testThunk<CompositeState> { _, _ ->
            store2.dispatch(store2Thunk1)
        }
        val store1Thunk = testThunk<State1> { _, _ ->
            store2.dispatch(store2Thunk2)
        }

        compositeStore.dispatch(testStoreThunk)
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
    fun `should update store3 state when composite thunk dispatched to composite store then thunk dispatch`() {
        val store3Subscriber = TestSubscriber<State3> {
            if (it.boolean == null) {
                store3.dispatch(SetState3Boolean(value = true))
            }
        }
        store3.subscribe(store3Subscriber)

        val thunk = testThunk<CompositeState> { dispatch, _ ->
            dispatch(SetState1String("Loading..."))
            dispatch(SetState3Boolean(value = null))
        }

        println("\n=== testStore.dispatch(store1Thunk) ===")
        compositeStore.dispatch(thunk)
        assert(store3.state.boolean == true) { "store3.state.boolean != true (${store3.state.boolean})" }
    }

    @Test
    fun `should trigger compositeStore12 subscriber from store2 when thunk23 dispatched to compositeStore23`() {
        val initialValue = compositeStore23.state.state2.integer
        val add = 10

        val store23Thunk = testThunk<TestState23> { dispatch, getState ->
            dispatch(SetState2Integer(value = (getState()?.state2?.integer ?: -1) + add))
        }

        val subscriber = TestSubscriber<TestState12>()
        compositeStore12.subscribe(subscriber)
        compositeStore23.dispatch(store23Thunk)

        assert(subscriber.callCount == 1) { "subscriber.callCount != 1 (${subscriber.callCount})" }
        assert(store2.state.integer == initialValue + add) { "store2.state.integer != initialValue+add (${store2.state.integer})" }
    }

    @Test
    fun `should throw cast exception when thunk23 dispatched from store12`() {
        val store23Thunk = testThunk<TestState23> { _, getState ->
            println("getState=${getState()}")
        }

        assertThrows(ClassCastException::class.java) {
            compositeStore12.dispatch(store23Thunk)
        }
    }

    @Test
    fun `should throw cast exception when thunk23 dispatched from dispatch12`() {
        val store23Thunk = testThunk<TestState23> { _, getState ->
            println("getState23=${getState()}")
        }
        val store12Thunk = testThunk<TestState12> { dispatch12, getState ->
            println("getState12=${getState()}")
            dispatch12(store23Thunk)
        }

        assertThrows(ClassCastException::class.java) {
            compositeStore12.dispatch(store12Thunk)
        }
    }
    // </editor-fold>
}

private class TestSubscriber<S>(private val block: (S) -> Unit = {}) : Subscriber<S> {

    val receivedInitialState: Boolean
        get() = _history.isNotEmpty()
    val lastState: S?
        get() = _history.lastOrNull()
    val callCount
        get() = _history.size - 1
    val history: List<S>
        get() = _history

    private val _history: MutableList<S> = mutableListOf()

    override fun newState(state: S) {
        _history.add(state)
        block(state)
    }
}

private inline fun <reified EffectType : Effect> SubscribeStore<*>.listen(listener: Listener<EffectType>) =
    subscribe(listener) { it as? EffectType }

private class CountingListener<E>(private val block: (E) -> Unit = {}) : Listener<E> {
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

private class LoggingMiddleware<S>(private val label: String) : Middleware<S> {
    override fun invoke(dispatch: DispatchFunction, getState: () -> S?): (DispatchFunction) -> DispatchFunction = { next ->
        { action ->
            println(">> $label ($action, ${getState()})")
            next(action)
        }
    }
}

private class CountingMiddleware<S> : Middleware<S> {
    private val dispatchableHistory = IdentityHashMap<Dispatchable, Int>()

    var callCount = 0
    val duplicates get() = dispatchableHistory.filter { it.value != 1 }
    val duplicateCount get() = duplicates.size

    override fun invoke(dispatch: DispatchFunction, getState: () -> S?): (DispatchFunction) -> DispatchFunction = { next ->
        { action ->
            callCount++
            dispatchableHistory[action] = (dispatchableHistory[action] ?: 0) + 1
            next(action)
        }
    }
}
