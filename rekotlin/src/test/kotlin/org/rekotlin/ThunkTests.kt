package org.rekotlin

import org.junit.jupiter.api.Test

private class FakeState
private class FakeAction : Action
@Suppress("UNUSED_PARAMETER")
private fun fakeReducer(action: Action, state: FakeState?) = state ?: FakeState()

@Suppress("SimplifyBooleanWithConstants")
class ThunkTests {

    @Test
    fun `Thunk middleware executes the Thunk when it receives a Thunk, and does not call next`() {
        val middleware: Middleware<FakeState> = thunkMiddleware()
        val dispatch: DispatchFunction = { _ -> }
        val getState: () -> FakeState? = { null }
        var nextCalled = false
        val next: DispatchFunction = { _ -> nextCalled = true }
        var thunkBodyCalled = false
        val thunk = thunk<FakeState> { _, _ -> thunkBodyCalled = true }

        middleware(dispatch, getState)(next)(thunk)

        assert(thunkBodyCalled == true)
        assert(nextCalled == false)
    }

    @Test
    fun `Thunk middleware calls next when it receives a non-thunk Action`() {
        val middleware: Middleware<FakeState> = thunkMiddleware()
        val dispatch: DispatchFunction = { _ -> }
        val getState: () -> FakeState? = { null }
        var nextCalled = false
        val next: DispatchFunction = { _ -> nextCalled = true }
        val action = FakeAction()

        middleware(dispatch, getState)(next)(action)

        assert(nextCalled == true)
    }

    @Test
    fun `Store calls the Thunk's body when dispatched`() {
        val store: Store<FakeState> = store(::fakeReducer, null, thunkMiddleware())
        var thunkBodyCalled = false
        val thunk = thunk<FakeState> { _, _ -> thunkBodyCalled = true }

        store.dispatch(thunk)

        assert(thunkBodyCalled == true)
    }
}
