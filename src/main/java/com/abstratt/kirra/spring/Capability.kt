package com.abstratt.kirra.spring

import com.abstratt.kirra.spring.user.RoleEntity
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty


enum class Capability(val instance: Boolean) {
    Create(false),
    Delete(true),
    List(false),
    Read(true),
    Update(true),
    StaticCall(false),
    Call(true)
}

val ALL_CAPABILITIES : Array<Capability> = EnumSet.allOf(Capability::class.java).toTypedArray()

interface AccessConstraints {
    fun allow(vararg rule : Pair<KProperty<*>, (RoleEntity) -> Boolean>) : Map<KProperty<*>, (RoleEntity) -> Boolean> =
        rule.asIterable().toMap()
}

typealias CapabilitySet = Set<Capability>
typealias RoleSet = Set<KClass<out RoleEntity>>


abstract class Constraint(
        val capabilities: Set<Capability>,
        val roles: Set<KClass<out RoleEntity>>,
        val accessPredicate : ((RoleEntity) -> Boolean)?
)

class FunctionConstraint(val operation : KFunction<*>, roles : RoleSet, capabilities : CapabilitySet, condition : ((RoleEntity) -> Boolean)?) : Constraint(capabilities, roles, condition)

class PropertyConstraint(val property : KProperty<*>, roles : RoleSet, capabilities : CapabilitySet, condition : ((RoleEntity) -> Boolean)?) : Constraint(capabilities, roles, condition)

class EntityConstraint(roles : RoleSet, capabilities : CapabilitySet, condition : ((RoleEntity) -> Boolean)?) : Constraint(capabilities, roles, condition)

fun constraint(
        roles : RoleSet, capabilities: CapabilitySet, condition : ((RoleEntity) -> Boolean)? = null) =
        EntityConstraint(roles, capabilities, condition)


fun constraint(property : KProperty<*>,
               roles : RoleSet, capabilities: CapabilitySet, condition : ((RoleEntity) -> Boolean)? = null) =
        PropertyConstraint(property, roles, capabilities, condition)


fun constraint(function : KFunction<*>,
               roles : RoleSet, capabilities: CapabilitySet, condition : ((RoleEntity) -> Boolean)? = null) =
        FunctionConstraint(function, roles, capabilities, condition)

inline fun can(vararg capabilities : Capability) = capabilities.toSet()

inline fun <RE: RoleEntity>roles(vararg roles : KClass<out RE>) : RoleSet = roles.toSet()

inline fun <RE: RoleEntity>provided(noinline predicate : ((RE) -> Boolean)) : ((RE) -> Boolean) = predicate

fun constraints(vararg constraints : Constraint) {}

typealias Constraints = Iterable<Constraint>