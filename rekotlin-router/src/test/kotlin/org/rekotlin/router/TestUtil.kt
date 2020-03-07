package org.rekotlin.router

import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBeNull

// extensions to make the tests more concise and expressive

infix fun RouteSegment?.shouldHaveArgs(args: Any) {
    this.shouldNotBeNull()
    this.args.shouldNotBeNull()
    this.args shouldEqual args
}

infix fun RouteSegment?.shouldHaveId(id: String) {
    this.shouldNotBeNull()
    this.id shouldEqual id
}
