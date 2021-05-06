package org.rekotlin

import java.util.IdentityHashMap
import java.util.concurrent.CopyOnWriteArrayList

internal typealias Compose<State> = (Array<out Store<*>>) -> State

/**
 * Factory function to create a composite store.
 *
 * A composite store aggregates the states of all the composed stores (also referred to as
 * "original stores" to clearly differentiate composed vs composite).
 * The composite store doesn't have a state & reducer of its own.
 * It keeps a projection of all the states of the composed stores.
 *
 * ```
 * +---------+                  +-----------------+
 * | Store 1 | -- state 1 --->  | Composite Store |
 * +---------+                  |                 |
 *                              |   | state 1 |   |
 * +---------+                  |   | state 2 |   |                        +------------+
 * | Store 2 | -- state 2 --->  |   v state 3 v   | -- composite state --> | subscriber |
 * +---------+                  |                 |                        +------------+
 *                              | =v= compose =v= |
 * +---------+                  |                 |
 * | Store 3 | -- state 3 --->  | composite state |
 * +---------+                  +-----------------+
 *```
 *
 * ### Composite Store State
 *
 * Whenever state changes in one of the original stores the composite store updates its state
 * projection and notifies all of its subscribers.
 *
 * ### Actions & Effects
 *
 * [Action]s and [Effect]s dispatched to the composite store propagate to the original stores
 * (store 1, 2 and 3 in the diagram above).
 *
 * The composite store is not aware of [Action]s dispatched to one of the composed stores, however
 * it will notice state changes that result from those actions.
 *
 * [Effect]s dispatched to composed stores will propagate to subscribers of the composite store.
 *
 * ### Middlewares
 *
 * The composite store holds its own middlewares. These middlewares only act on [Action]s dispatched
 * to the composite store, never on [Action]s that are only dispatched to one of the original stores.
 * For example when you dispatch an action to store 1 (in the diagram above) the middlewares in
 * the composite store will not act on that action, only the middlewares of store 1.
 *
 * In other words:
 * - composing stores creates a hierarchy of middlewares.
 * - composing stores enables isolation of middlewares.
 *
 * ### Usage examples
 *
 * ```kotlin
 * val featureStore: Store<FeatureState> =  // store specific to your feature.
 *                                          // may have feature specific middleware.
 * val authStore: Store<AuthStare>       =  // shared login state store.
 *
 * data class CompositeState(val auth: AuthState, feature: FeatureState)
 * val compositeStore: Store<Pair<AuthState, FeatureState>> = composeStores(
 *              authStore,
 *              featureStore,
 *              thunkMiddleware()
 * ) { authState, featureState ->
 *      CompositeSate(authState, featureState)
 * }
 * // in the auth feature
 * authStore.dispatch(LogoutAction)
 * compositeStore.dispatch(FeatureAnimationEffect)
 * ```
 *
 * ### Modularized Applications
 *
 * Composite store allows you to break down the global state model into sub-states and compose them
 * at runtime. This enables you to break applications into multiple modules, with Redux broken down
 * at module boundaries. But the stores are still integrated in the sense that they can send
 * [Dispatchable] to all subscribers of stores that are composed.
 *
 * For example a base module can contain shared behavior (like authentication state) and feature
 * modules can compose on top of that. So the stores in the code snippet above could live in
 * different modules.
 *
 * ```
 * +- Base Module ---+                  +- Feature 1 Module ------+
 * | owns authStore ------ inject ----> | create compositeStore 1 |
 * +------- | -------+                  +-------------------------+
 *          |
 *          |                           +- Feature 2 Module ------+
 *          +------------- inject ----> | create compositeStore 2 |
 *                                      +-------------------------+
 * ```
 *
 * Both feature modules can react to auth state or send actions & effects to the authStore (via
 * their feature-local composite store).
 *
 * @see [composeStores]
 */
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
            store.subscribeProjected{ _state = compose(stores) }
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
        set(value) {
            val oldValue = field
            field = value

            value?.let {
                subscriptions.forEach { it.newValues(oldValue, value) }
            }
        }
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

    override fun <S : Subscriber<State>> subscribe(subscriber: S) = subscribe(subscriber, ::stateIdentity)

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
}

private inline fun <State> Store<State>.subscribeProjected(crossinline subscriber: (State) -> Unit) =
    subscribe(subscriber { subscriber(it) })

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
