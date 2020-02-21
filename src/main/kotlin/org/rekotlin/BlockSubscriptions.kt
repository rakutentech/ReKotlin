package org.rekotlin

/**
 * BlockSubscriber allows subscribing to multiple states in a single class.
 *
 * Ex
 * val subscriber : BlockSubscriber<MyState>= BlockSubscriber { myState -> }
 * val subscriber2 : BlockSubscriber<MyState2>= BlockSubscriber { myState2 -> }
 *
 * store.subscribe(subscriber)
 * store.subscribe(subscriber2)
 */
class BlockSubscriber<S>(private val block: (S) -> Unit) : Subscriber<S> {

    override fun newState(state: S) {
        block.invoke(state)
    }
}

/**
 * Utility class for holding multiple subscriptions which then can be by store for un-subscription.
 */
class BlockSubscriptions {

    var blockSubscriberList = arrayListOf<BlockSubscriber<*>>()

    fun <T> add(subscriber: BlockSubscriber<T>) {
        blockSubscriberList.add(subscriber)
    }
}