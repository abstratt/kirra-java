package com.abstratt.kirra.spring

import kotlin.reflect.KClass

/**
 * A state machine is made of a set of states and set of transitions.
 *
 * A state defines an optional pair of entry and exit behaviors.
 *
 * A transition defines a source state and a destination state, 
 * a set of trigger events, an optional guard predicate, 
 * and an optional effect behavior.
 */
class StateMachine<ST : StateToken, C : StateContext<ST>>(
    val context : C
) {
    inline fun <reified ST : StateToken> getTokenType() : KClass<ST> = ST::class

    fun publish(context : C, event: StateEvent) : Boolean {
        val state = currentState()
        val triggered = context.stateMachineConfiguration().transitions?.filter {
            it.from == context.getState() && 
            it.isEligible(event) && 
            it.tryToActivate(this as StateMachine<ST, StateContext<ST>>)
        }?.isNotEmpty() ?: false
        return triggered
    }

    fun currentState(): State<ST>? = context.getState()?.let { findState(it) }

    fun findState(token : ST) : State<ST>? = 
        context.stateMachineConfiguration().states
            ?.find { it.token == token }
            ?.let { it as State<ST> }
    
    fun advance(to: ST) = 
        context.setState(to)
}


/**
 * Something that has a state.
 */
interface StateContext<ST : StateToken> {
    fun getState() : ST?
    fun setState(newState : ST?)
    fun stateMachineConfiguration() : StateMachineConfiguration<ST>
    fun stateMachine() : StateMachine<ST, StateContext<ST>> =
        StateMachine(this)
}

class StateMachineConfiguration<ST : StateToken> (
    val states : Array<State<ST>>,
    val transitions : Array<Transition<ST>>
)

interface StateEvent

class State<out ST : StateToken> (
        val token : ST,
        val onEntry : (() -> Unit)? = null,
        val onExit : (() -> Unit)? = null
)

interface StateToken {
    val name : String
}

fun <ST : StateToken> KClass<ST>.initial(): ST? =
    if (this.java.isEnum) this.java.enumConstants?.first() else null


class Transition<ST : StateToken>(
        val from : ST,
        val to : ST,
        val triggers : Iterable<StateEvent>,
        val guard : (() -> Boolean)? = null,
        val effect : (() -> Unit)? = null
) {
    fun isEligible(event : StateEvent) = triggers.contains(event)
    fun tryToActivate(stateMachine: StateMachine<ST, *>) : Boolean {
        if (!(guard?.invoke() ?: false)) {
            return false
        }
        stateMachine.currentState()?.onEntry?.invoke()
        effect?.invoke()
        stateMachine.advance(this.to)
        stateMachine.currentState()?.onExit?.invoke()
        return true
    }
}


