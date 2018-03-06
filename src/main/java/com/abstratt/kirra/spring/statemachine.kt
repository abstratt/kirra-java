package com.abstratt.kirra.spring

import java.sql.Date
import java.time.LocalDate
import javax.persistence.*
import kotlin.reflect.KClass

/**
 * A state machine is made of a set of states and set of transitions.
 *
 * A state defines an optional pair of entry and exit behaviors.
 *
 * A transition defines a source state and a destination state, a set of trigger events, and an optional guard predicate, and an optional effect behavior.
 */
@MappedSuperclass

abstract class StateMachine<ST : StateToken, C : StateContext<ST>, SMC : StateMachineConfiguration<ST, C>>(
    initialState : ST,
    @Enumerated(EnumType.STRING)
    var state: ST? = initialState
) {
    inline fun <reified ST : StateToken> getTokenType() : KClass<ST> = ST::class

    abstract fun configuration() : SMC

    fun publish(context : C, event: StateEvent) : Boolean {
        val triggered = configuration().transitions()?.filter {
            it.from == state && it.isEligible(event) && it.tryToActivate(context)
        }?.isNotEmpty() ?: false
        return triggered
    }

    fun current(context : C): State<ST, C>? = context.stateMachine.state?.let { getState(context, it ) }

    fun getState(context : C, token : ST) : State<ST, C>? = configuration().states()?.find { it.token == token }?.let { it as State<ST, C> }
}


/**
 * SCmething that has a state.
 */
interface StateContext<ST : StateToken> {
    val stateMachine : StateMachine<ST, StateContext<ST>, StateMachineConfiguration<ST,StateContext<ST>>>
    val current : State<ST, StateContext<ST>>? get() = stateMachine.current(this)
}

interface StateMachineConfiguration<ST : StateToken, SC : StateContext<ST>> {
    fun states() : Array<State<ST, SC>>
    fun transitions() : Array<Transition<ST, SC>>
}


interface StateEvent

class State<out ST : StateToken, C : StateContext<out ST>> (
        val token : ST,
        val onEntry : ((C) -> Unit)? = null,
        val onExit : ((C) -> Unit)? = null
)

interface StateToken {
    val name : String
}

class Transition<ST : StateToken, SC : StateContext<ST>>(
        val from : ST,
        val to : ST,
        val triggers : Iterable<StateEvent>,
        val guard : ((SC) -> Boolean)? = null,
        val effect : ((SC) -> Unit)? = null
) {
    fun isEligible(event : StateEvent) = triggers.contains(event)
    fun tryToActivate(context : SC) : Boolean {
        if (!(guard?.invoke(context) ?: false)) {
            return false
        }
        context.current?.onEntry?.invoke(context)
        effect?.invoke(context)
        context.stateMachine.state = this.to
        context.current?.onExit?.invoke(context)
        return true
    }
}


