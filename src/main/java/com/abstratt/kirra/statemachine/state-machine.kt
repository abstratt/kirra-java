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
open class StateMachine<ST : StateToken, SE : StateEvent> (
        val stateProperty : KMutableProperty<ST?>,
        val activeStates : Iterable<State<ST, *>>,
        val transitions : Iterable<Transition<ST, SE, *>>
) {
    init {
        assert(stateProperty.returnType.javaClass.isEnum, { "${stateProperty.name} must be an enumeration" })
        assert(!tokens.isEmpty(), { "${stateProperty.name} must be a non-empty enumeration" })
    }

    fun currentStateToken(context: StateContext<ST, SE>): ST =
            stateProperty.getter.call(context) ?: initialState.token

    fun <SC : StateContext<ST, SE>> currentState(context: SC): State<ST, SC> =
            asState(currentStateToken(context)) as State<ST, SC>

    fun <SC : StateContext<ST, SE>> advance(context: SC, to: ST) =
            stateProperty.setter.call(context, to)

    val initialState by lazy { asState(initialStateToken) }

    val initialStateToken by lazy {
        val initialState = tokens.first()
        initialState
    }

    val states by lazy {
        tokens
                .map { token ->
                    activeStates.find { it.token == token } ?: State(token)
                }
    }
    private val tokens: List<ST>
        get() {
            val returnType = stateProperty.returnType
            val clazz: KClass<ST> = (returnType.classifier as KClass<ST>)
            val javaClass = clazz.java
            val enumConstants = javaClass.enumConstants
            val asTokens = enumConstants.map { it as ST }.toList()
            return asTokens
        }
    fun asState(stateToken : StateToken) : State<ST, *> =
            states.find { it.token == stateToken } as State<ST, *>
}

fun <ST : StateToken, SE: StateEvent, SC : StateContext<ST,SE>> SC.findStateMachine(): StateMachine<ST, SE> =
        this::class.nestedClasses
                .mapNotNull { it.objectInstance }
                .filter { StateMachine::class.isInstance(it) }
                .map { it as StateMachine<ST, SE> }
                .first()

class State<ST : StateToken, SC : StateContext<ST, *>> (
        val token : ST,
        private val onEntry : ((SC) -> Unit)? = null,
        private val onExit : ((SC) -> Unit)? = null
) {
    val name: String get() = token.name

    fun callOnEntry(stateMachineInstance: StateMachineInstance<ST, *, SC>) {
        onEntry?.invoke(stateMachineInstance.context)
    }

    fun callOnExit(stateMachineInstance: StateMachineInstance<ST, *, SC>) {
        onExit?.invoke(stateMachineInstance.context)
    }
}

class Transition<ST : StateToken, SE : StateEvent, SC : StateContext<ST, SE>>(
        val from : ST,
        val to : ST,
        private val triggers : Iterable<SE>,
        private val guard : ((SC) -> Boolean)? = null,
        private val effect : ((SC) -> Unit)? = null
) {
    fun isEligible(event : StateEvent) = triggers.contains(event)

    fun tryToActivate(stateMachineInstance: StateMachineInstance<ST, SE, SC>) : Boolean {
        return stateMachineInstance.currentState?.let { oldState ->
            if (checkGuard(stateMachineInstance)) {
                stateMachineInstance.currentState.callOnExit(stateMachineInstance)
                callEffect(stateMachineInstance)
                stateMachineInstance.advance(to)
                stateMachineInstance.currentState.callOnEntry(stateMachineInstance)
                true
            } else
                false
        } ?: false
    }

    private fun callEffect(stateMachineInstance: StateMachineInstance<ST, SE, SC>) {
        effect?.invoke(stateMachineInstance.context)
    }

    private fun checkGuard(stateMachineInstance: StateMachineInstance<ST, SE, SC>) =
            guard?.invoke(stateMachineInstance.context) ?: true
}

interface StateEvent

interface StateToken {
    val name : String
}

/**
 * An instance of a state machine.
 */
open class StateMachineInstance<ST : StateToken, SE : StateEvent, SC : StateContext<ST, SE>>(val context : SC) {

    private val stateMachine: StateMachine<ST, SE> by lazy { context.findStateMachine() }

    fun publish(event: StateEvent) : Boolean {
        val stateToken = currentStateToken
        val triggered = stateMachine.transitions.filter {
            it.from == stateToken &&
            it.isEligible(event) &&
            (it as Transition<ST, SE, SC>).tryToActivate(this)
        }.isNotEmpty()
        return triggered
    }

    val currentStateToken : ST
        get() = stateMachine.currentStateToken(this.context)

    val currentState: State<ST, SC>
        get() = stateMachine.currentState(this.context)

    fun advance(to: ST) =
        stateMachine.advance(context, to)
}


/**
 * Something that has a state.
 */
interface StateContext<ST : StateToken, SE : StateEvent> {
    fun SE.publish() {
        val stateMachine = StateMachineInstance(this@StateContext)
        stateMachine.publish(this)
    }
}



