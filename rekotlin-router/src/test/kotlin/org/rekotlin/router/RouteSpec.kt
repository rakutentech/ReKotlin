package org.rekotlin.router

import org.amshove.kluent.shouldEqual
import org.junit.jupiter.api.Test

class RouteSpec {

    @Test
    fun `should add (+) route segment to the end`() {
        // When
        val newRoute = Route("A", "B") + RouteSegment("C")

        // Then
        newRoute shouldEqual Route("A", "B", "C")
    }

    @Test
    fun `should add (+) duplicate route segment to the end`() {
        // When
        val newRoute = Route("A", "B") + RouteSegment("B")

        // Then
        newRoute shouldEqual Route("A", "B", "B")
    }

    @Test
    fun `should subtract (-) route segment from the end`() {
        // When
        val newRoute = Route("A", "B", "C") - RouteSegment("C")

        // Then
        newRoute shouldEqual Route("A", "B")
    }

    @Test
    fun `should subtract (-) exactly one route segment from the end`() {
        // When
        val newRoute = Route("A", "B", "B") - RouteSegment("B")

        // Then
        newRoute shouldEqual Route("A", "B")
    }

    @Test
    fun `should subtract (-) duplicate route segment from the end`() {
        // When
        val newRoute = Route("A", "B", "A") - RouteSegment("A")

        // Then
        newRoute shouldEqual Route("A", "B")
    }

    @Test
    fun `should subtract (-) duplicate route segment from the end, even if it is not last element`() {
        // When
        val newRoute = Route("A", "B", "A", "C") - RouteSegment("A")

        // Then
        newRoute shouldEqual Route("A", "B", "C")
    }
}
