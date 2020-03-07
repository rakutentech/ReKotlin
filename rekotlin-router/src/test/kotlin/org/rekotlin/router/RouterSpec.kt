package org.rekotlin.router

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

internal class RouterTests {

    private val root = "root"
    private val counter = "counter"
    private val stats = "stats"
    private val info = "info"

    @Test
    fun `should calculate transitions from an empty route to a multi segment route`() {
        // Given
        val oldRoute = Route()
        val newRoute = Route(root, stats)

        // When
        val routingActions = routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        routingActions.count() shouldBe 2
        val pushes = routingActions.map {
            it shouldBeInstanceOf Push::class
            it as Push
        }
        pushes[0].routableIndex shouldEqual 0
        pushes[0].segmentToPush shouldHaveId root

        pushes[1].routableIndex shouldEqual 1
        pushes[1].segmentToPush shouldHaveId stats
    }

    @Test
    fun `should generate a change action on the last common subroute`() {
        // Given
        val oldRoute = Route(root, counter)
        val newRoute = Route(root, stats)

        // When
        val routingActions = routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        val change = routingActions.find { it is Change } as? Change

        change.shouldNotBeNull()
        change.routableIndex shouldEqual 1
        change.segmentTeReplace shouldHaveId counter
        change.newSegment shouldHaveId stats
    }

    @Test
    fun `should generate a Change action on the last common subroute, also for routes of different length`() {
        // Given
        val oldRoute = Route(root, counter)
        val newRoute = Route(root, stats, info)

        // When
        val routingActions = routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        routingActions.count() shouldBe 2

        val change = routingActions.find { it is Change } as? Change
        val push = routingActions.findLast { it is Push } as? Push

        change.shouldNotBeNull()
        change.routableIndex shouldEqual 1
        change.segmentTeReplace shouldHaveId counter
        change.newSegment shouldHaveId stats

        push.shouldNotBeNull()
        push.routableIndex shouldEqual 2
        push.segmentToPush shouldHaveId info
    }

    @Test
    fun `should generate a Change action on root when root element changes`() {
        // Given
        val oldRoute = Route(root)
        val newRoute = Route(stats)

        // When
        val routingActions = routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        routingActions.count() shouldEqual 1

        val change = routingActions.find { it is Change } as? Change

        change.shouldNotBeNull()
        change.routableIndex shouldEqual 0
        change.segmentTeReplace shouldHaveId root
        change.newSegment shouldHaveId stats
    }

    @Test
    fun `should generate a pop action followed by a change action on root when whole route changes`() {
        // Given
        val oldRoute = Route(root, counter)
        val newRoute = Route(stats)

        // When
        val routingActions = routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        routingActions.count() shouldEqual 2

        val change = routingActions.find { it is Change } as? Change
        val pop = routingActions.find { it is Pop } as? Pop

        change.shouldNotBeNull()
        change.routableIndex shouldEqual 0
        change.segmentTeReplace shouldHaveId root
        change.newSegment shouldHaveId stats

        pop.shouldNotBeNull()
        pop.routableIndex shouldEqual 1
        pop.segmentToPop shouldHaveId counter
    }

    @Test
    fun `should calculate no actions for transition from empty route to empty route`() {
        // Given
        val oldRoute = Route()
        val newRoute = Route()
        // When
        val routingActions = routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        routingActions.count() shouldEqual 0
    }

    @Test
    fun `should calculate actions for transitions between identical routes with different arguments`() {
        // Given
        val oldRoute = Route(root) + RouteSegment(counter to 1)
        val newRoute = Route(root) + RouteSegment(counter to 2)

        // When
        val routingActions = routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        routingActions.count() shouldEqual 1
    }

    @Test
    fun `should calculate actions for transitions between identical routes with different arguments, one of them null`() {
        // Given
        val oldRoute = Route(root) + RouteSegment(counter to 1)
        val newRoute = Route(root, counter)

        // When
        val routingActions = routingActionsForTransitionFrom(oldRoute, newRoute)

        println(routingActions)

        // Then
        routingActions.count() shouldEqual 1
    }

    @Test
    fun `should calculate no actions for transitions between identical, non-empty routes`() {

        // Given
        val oldRoute = Route(root, counter)
        val newRoute = Route(root, counter)

        // When
        val routingActions = routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        routingActions.count() shouldEqual 0
    }

    @Test
    fun `should calculate transitions with multiple pops`() {

        // Given
        val oldRoute = Route(root, stats, counter)
        val newRoute = Route(root)

        // When
        val routingActions = routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        routingActions.count() shouldEqual 2

        val pops = routingActions.filter { it is Pop }.map { it as Pop }

        pops.count() shouldEqual 2

        pops[0].run {
            routableIndex shouldEqual 2
            segmentToPop shouldHaveId counter
        }

        pops[1].run {
            routableIndex shouldEqual 1
            segmentToPop shouldHaveId stats
        }
    }

    @Test
    fun `should calculate transitions with multiple pushes`() {

        // Given
        val oldRoute = Route(root)
        val newRoute = Route(root, stats, counter)

        // When
        val routingActions = routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        routingActions.count() shouldEqual 2

        val pushes = routingActions.filter { it is Push }.map { it as Push }

        pushes.count() shouldEqual 2

        pushes[0].run {
            routableIndex shouldEqual 1
            segmentToPush shouldHaveId stats
        }

        pushes[1].run {
            routableIndex shouldEqual 2
            segmentToPush shouldHaveId counter
        }
    }
}
