package org.rekotlin.router

import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.rekotlin.Action
import org.rekotlin.Store
import org.rekotlin.store

class FakeAppState(var navigationState: NavigationState = NavigationState())

fun appReducer(action: Action, state: FakeAppState?): FakeAppState {
    val fakeAppState = FakeAppState()
    fakeAppState.navigationState = navigationReducer(action, state?.navigationState)
    return fakeAppState
}

class FakeRoutable(
    private val pop: (RouteSegment, Boolean) -> Unit = { _, _ -> },
    private val change: (RouteSegment, RouteSegment, Boolean) -> Unit = { _, _, _ -> },
    private val push: (RouteSegment, Boolean) -> Unit = { _, _ -> },
    private val childRoutable: Routable? = null
) : Routable {
    override fun popRouteSegment(routeSegment: RouteSegment, animated: Boolean) =
        pop(routeSegment, animated)

    override fun changeRouteSegment(from: RouteSegment, to: RouteSegment, animated: Boolean): Routable {
        change(from, to, animated)
        return childRoutable ?: this
    }

    override fun pushRouteSegment(routeSegment: RouteSegment, animated: Boolean): Routable {
        push(routeSegment, animated)
        return childRoutable ?: this
    }
}

class RoutingCallTests {

    private val store: Store<FakeAppState> = store(::appReducer, FakeAppState())

    @Test
    fun `should not push route segment when no route is dispatched`() {
        // Given
        var pushRouteCalled = false
        val routable = FakeRoutable(push = { _, _ -> pushRouteCalled = true })

        // When
        val router = router(routable)
        store.subscribe(router) { select { navigationState } }

        // Then
        pushRouteCalled.shouldBeFalse()
    }

    @Test
    fun `should push route segment with identifier to root when an initial route is dispatched`() {
        // Given
        var pushedSegment: RouteSegment? = null
        val routable = FakeRoutable(push = { segment, _ -> pushedSegment = segment })

        val action = SetRouteAction(Route("root"))
        store.dispatch(action)

        // when
        val router = router(routable)
        store.subscribe(router) { select { navigationState } }

        // Then
        pushedSegment.shouldNotBeNull()
        pushedSegment!!.id shouldEqual "root"
    }

    @Test
    fun `should push root and child segment when a set route 2 segments is dispatched`() {

        // Given
        val action = SetRouteAction(Route("root", "child"))
        store.dispatch(action)

        var rootSegment: RouteSegment? = null
        var childSegment: RouteSegment? = null

        val child = FakeRoutable(
            push = { segment, _ -> childSegment = segment }
        )

        val root = FakeRoutable(
            push = { segment, _ -> rootSegment = segment },
            childRoutable = child
        )

        // When
        val router = router(root)
        store.subscribe(router) { select { navigationState } }

        // Then
        rootSegment shouldHaveId "root"
        childSegment shouldHaveId "child"
    }
}

class RouteArgsTests {
    private val store: Store<FakeAppState> = store(::appReducer)

    @Test
    fun `should pass route args to push when set via SetRouteAction`() {
        // Given
        var pushedSegment: RouteSegment? = null
        val routable = FakeRoutable(
            push = { segment, _ -> pushedSegment = segment }
        )
        val router = router(routable)
        store.subscribe(router) { select { navigationState } }

        // When
        val action = SetRouteAction(Route("main" to 1))
        store.dispatch(action)

        // Then
        pushedSegment shouldHaveArgs 1
    }

    @Test
    fun `should pass route args to segment routables for longer route`() {
        // Given
        var rootSegment: RouteSegment? = null
        var childSegment: RouteSegment? = null
        var grandChildSegment: RouteSegment? = null

        val grandChildRoutable = FakeRoutable(
            push = { segment, _ -> grandChildSegment = segment }
        )
        val childRoutable = FakeRoutable(
            push = { segment, _ -> childSegment = segment },
            childRoutable = grandChildRoutable
        )
        val routable = FakeRoutable(
            push = { segment, _ -> rootSegment = segment },
            childRoutable = childRoutable
        )

        val router = router(routable)
        store.subscribe(router) { select { navigationState } }

        // When
        val action = SetRouteAction(
            Route(
                "root" to 1,
                "child" to 2,
                "grandchild" to 3
            )
        )
        store.dispatch(action)

        // Then
        rootSegment shouldHaveArgs 1
        childSegment shouldHaveArgs 2
        grandChildSegment shouldHaveArgs 3
    }
}

class RoutingAnimationTests {

    private val store: Store<FakeAppState> = store(::appReducer)
    private val animated: MutableList<Boolean> = mutableListOf()

    @BeforeEach
    fun setup() {
        val routable = FakeRoutable(
            push = { _, a -> animated.add(a) }
        )

        val router = router(routable)
        store.subscribe(router) { select { navigationState } }
    }

    @Test
    fun `should push animated when dispatch route change with animate as true`() {
        // Given
        val actionArray = Route("root", "child")
        val action = SetRouteAction(actionArray, animated = true)

        // When
        store.dispatch(action)

        // Then
        animated.forEach { it.shouldBeTrue() }
    }

    @Test
    fun `should not push animation when route change with animate is false`() {
        // Given
        val actionArray = Route("root", "child")
        val action = SetRouteAction(actionArray, animated = false)
        // When
        store.dispatch(action)

        // Then
        animated.forEach { it.shouldBeFalse() }
    }

    @Test
    fun `should push animation by default`() {
        // Given

        val actionArray = Route("root", "child")
        val action = SetRouteAction(actionArray)
        // When
        store.dispatch(action)

        // Then
        animated.forEach { it.shouldBeTrue() }
    }
}
