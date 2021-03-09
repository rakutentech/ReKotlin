package org.rekotlin

import java.util.IdentityHashMap
import java.util.concurrent.CopyOnWriteArrayList

fun <State> compositeStore(
    vararg stores: Store<*>,
    middleware: List<Middleware<State>> = emptyList(),
    skipRepeats: Boolean = true,
    compose: Compose<State>
): Store<State> = CompositeStore(
    *stores,
    middleware = middleware,
    skipRepeats = skipRepeats,
    compose = compose
)

private class CompositeStore<State>(
    private vararg val stores: Store<*>,
    middleware: List<Middleware<State>> = emptyList(),
    private val skipRepeats: Boolean = true,
    private val compose: Compose<State>
) : Store<State> {

    private val subscriptions: MutableList<SubscriptionBox<State, *>> = CopyOnWriteArrayList()
    private val listeners: MutableList<ListenerBox<out Effect>> = CopyOnWriteArrayList()
    private val middlewares: MutableList<Middleware<State>> = CopyOnWriteArrayList(middleware)

    private val effectDispatcher = EffectDispatcher()
    private val dispatchEffect: DispatchEffect = { effect ->
        effectDispatcher.dispatch(effect) {
            stores.forEach { it.dispatch(effect) }
            listeners.forEach { it.onEffect(effect) }
        }
    }

    private var dispatchAction: DispatchAction = buildDispatchAction()

    init {
        stores.forEach { store ->
            store.subscribeProjected { _ ->
                val prevState = _state
                val newState = compose(stores)
                _state = newState
                subscriptions.forEach {
                    it.newValues(prevState, newState)
                }
            }
            store.subscribe(listener { effect ->
                if (!effect.isDispatching) {
                    listeners.forEach {
                        it.onEffect(effect)
                    }
                }
            })
        }
    }

    private var _state: State? = null
    override val state: State
        get() = _state!!

    override fun dispatch(dispatchable: Dispatchable) = dispatchFunction(dispatchable)
    override val dispatchFunction: DispatchFunction
        get() = { dispatchable: Dispatchable ->
            when (dispatchable) {
                is Effect -> dispatchEffect(dispatchable)
                is Action -> dispatchAction(dispatchable)
            }
        }

    operator fun plusAssign(middleware: Middleware<State>) {
        middlewares += middleware
        dispatchAction = buildDispatchAction()
    }

    operator fun minusAssign(middleware: Middleware<State>) {
        middlewares -= middleware
        dispatchAction = buildDispatchAction()
    }

    private fun buildDispatchAction() =
        middlewares.reversed()
            .fold(this::defaultDispatch as DispatchFunction,
                { dispatch, middleware ->
                    middleware(this::dispatch, this::state)(dispatch)
                }
            )

    private fun defaultDispatch(dispatchable: Dispatchable) =
        when (dispatchable) {
            is Dispatcher -> dispatchable.dispatchTo(stores)
            else -> Dispatcher(dispatchable).dispatchTo(stores)
        }

    override fun <S : Subscriber<State>> subscribe(subscriber: S) = subscribe(subscriber, { this })
    override fun <SelectedState, S : Subscriber<SelectedState>> subscribe(
        subscriber: S,
        selector: Subscription<State>.() -> Subscription<SelectedState>
    ) {
        unsubscribe(subscriber)

        val actualSelector = when {
            skipRepeats -> compose(selector, Subscription<SelectedState>::skipRepeats)
            else -> selector
        }

        val box = SubscriptionBox(actualSelector, subscriber)
        subscriptions.add(box)

        _state?.let {
            box.newValues(null, it)
        }
    }

    override fun <SelectedState> unsubscribe(subscriber: Subscriber<SelectedState>) {
        val index = subscriptions.indexOfFirst { it.subscriber === subscriber }
        if (index != -1) {
            subscriptions.removeAt(index)
        }
    }

    override fun subscribe(listener: Listener<Effect>) = subscribe(listener) { it }
    override fun <E : Effect> subscribe(listener: Listener<E>, selector: (Effect) -> E?) {
        unsubscribe(listener)
        listeners.add(ListenerBox(listener, selector))
    }

    override fun <E : Effect> unsubscribe(listener: Listener<E>) {
        val index = listeners.indexOfFirst { it.listener === listener }
        if (index != -1) {
            listeners.removeAt(index)
        }
    }

    private val Effect.isDispatching get() = effectDispatcher.isDispatching(this)

    private inline fun <State> Store<State>.subscribeProjected(crossinline subscriber: (State) -> Unit) =
        subscribe(subscriber { subscriber(it) })
}

private class EffectDispatcher {
    private val dispatching = mutableListOf<Effect>()

    fun isDispatching(effect: Effect) = dispatching.any { it === effect }

    fun dispatch(effect: Effect, body: () -> Unit) {
        dispatching += effect
        body()
        dispatching -= effect
    }
}

private class Dispatcher(private val dispatchable: Dispatchable) : Dispatchable {
    private val dispatched = IdentityHashMap<Store<*>, Unit>()

    fun dispatchTo(stores: Array<out Store<*>>) {
        stores.forEach { store ->
            if (store !in dispatched) {
                dispatched += store to Unit
                if (store is CompositeStore<*>) {
                    store.dispatch(this)
                } else {
                    store.dispatch(dispatchable)
                }
            }
        }
    }
}
