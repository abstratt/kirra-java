package com.abstratt.kirra.statemachine

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty

/**
 * A state machine is made of a set of states and set of transitions.
 *
 * A state defines an optional pair of entry and exit behaviors.
 *
 * A transition defines a source state and a destination state, 
 * a set of trigger events, an optional guard predicate, 
 * and an optional effect behavior.
 */
open class StateMachine<ST : StateToken, SE : StateEvent, SC : StateContext<ST, SE>> {

    val context: SC

    private val configuration: StateMachineConfiguration<ST, SE>

    constructor(context: SC, stateProperty: KMutableProperty<ST?>,
                states: Iterable<State<ST, SC>>,
                transitions: Iterable<Transition<ST, SE, SC>>) : this(context, StateMachineConfiguration(stateProperty, states, transitions))

    constructor(context : SC, configuration : StateMachineConfiguration<ST, SE>) {
        this.context = context
        this.configuration = configuration
    }
    inline fun <reified ST : StateToken> getTokenType() : KClass<ST> = ST::class

    fun publish(event: StateEvent) : Boolean {
        val stateToken = currentStateToken
        val triggered = configuration.transitions.filter {
            it.from == stateToken &&
            it.isEligible(event) &&
            (it as Transition<ST, SE, SC>).tryToActivate(this)
        }.isNotEmpty()
        return triggered
    }

    val currentStateToken : ST
        get() = configuration.stateProperty.getter.call(context) ?: configuration.initialState.token

    val currentState: State<ST, SC>
        get() = asState(currentStateToken)

    fun asState(stateToken : ST) : State<ST, SC> =
        configuration.asState(stateToken) as State<ST, SC>
    
    fun advance(to: ST) = 
        configuration.stateProperty.setter.call(context, to)
}


/**
 * Something that has a state.
 */
interface StateContext<ST : StateToken, SE : StateEvent> {
    val stateMachineConfiguration : StateMachineConfiguration<ST, SE> get() =
        this::class.nestedClasses
            .mapNotNull { it.objectInstance }
            .filter { StateMachineConfiguration::class.isInstance(it) }
            .map { it as StateMachineConfiguration<ST, SE> }
            .first()

    fun SE.publish() {
        val stateMachine = StateMachine(this@StateContext, stateMachineConfiguration)
        stateMachine.publish(this)
    }
    val initialStateToken get() = stateMachineConfiguration.initialStateToken
}

open class StateMachineConfiguration<ST : StateToken, SE : StateEvent> (
        val stateProperty : KMutableProperty<ST?>,
        val activeStates : Iterable<State<ST, *>>,
        val transitions : Iterable<Transition<ST, SE, *>>
) {
    init {
        assert(stateProperty.returnType.javaClass.isEnum, { "${stateProperty.name} must be an enumeration" })
        assert(!tokens.isEmpty(), { "${stateProperty.name} must be a non-empty enumeration" })
    }
    val initialState by lazy { asState(initialStateToken) }

    val initialStateToken by lazy { tokens.first() }

    val states by lazy {
        tokens
            .map { token ->
                activeStates.find { it.token == token } ?: State(token)
            }
    }
    private val tokens : List<ST> get() {
        val returnType = stateProperty.returnType
        val clazz : KClass<ST> = (returnType.classifier as KClass<ST>)
        val javaClass = clazz.java
        val enumConstants = javaClass.enumConstants
        return enumConstants.map { it as ST }.toList()}

    fun asState(stateToken : StateToken) : State<ST, *> =
        states.find { it.token == stateToken } as State<ST, *>

}

class State<ST : StateToken, SC : StateContext<ST, *>> (
        val token : ST,
        val onEntry : ((SC) -> Unit)? = null,
        val onExit : ((SC) -> Unit)? = null
) {
    val name: String get() = token.name
}

class Transition<ST : StateToken, SE : StateEvent, SC : StateContext<ST, SE>>(
        val from : ST,
        val to : ST,
        val triggers : Iterable<SE>,
        val guard : ((SC) -> Boolean)? = null,
        val effect : ((SC) -> Unit)? = null
) {
    fun isEligible(event : StateEvent) = triggers.contains(event)

    fun tryToActivate(stateMachine: StateMachine<ST, SE, SC>) : Boolean {
        val guardPassed = guard?.invoke(stateMachine.context) ?: true
        if (!guardPassed) {
            return false
        }
        val context = stateMachine.context
        return stateMachine.currentState?.run {
            onEntry?.invoke(context)
            effect?.invoke(context)
            stateMachine.advance(to)
            onExit?.invoke(context)
            true
        } ?: false
    }
}

interface StateEvent

interface StateToken {
    val name : String
}



