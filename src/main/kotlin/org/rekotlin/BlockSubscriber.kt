package org.rekotlin

/**
 * BlockSubscriber allows subscription to a single state. This methodology is useful when subscribing multiple states
 * within single class.
 *
 * Ex
 * val subscriber : BlockSubscriber<MyState>= BlockSubscriber { myState -> }
 * store.subscribe(subscriber)
 */
class BlockSubscriber<S>(private val block: (S) -> Unit) : StoreSubscriber<S> {

    override fun newState(state: S) {
        block.invoke(state)
    }
}