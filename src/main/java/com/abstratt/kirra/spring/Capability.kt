package com.abstratt.kirra.spring

import com.abstratt.kirra.spring.user.RoleEntity
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty


enum class CapabilityTarget() {
    Property, Relationship, StaticOperation, Operation, Entity, Instance
}

enum class Capability(vararg val targets : CapabilityTarget) {
    Create(CapabilityTarget.Entity),
    Delete(CapabilityTarget.Instance),
    List(CapabilityTarget.Entity),
    Read(CapabilityTarget.Instance, CapabilityTarget.Property, CapabilityTarget.Relationship),
    Update(CapabilityTarget.Instance, CapabilityTarget.Property, CapabilityTarget.Relationship),
    StaticCall(CapabilityTarget.StaticOperation),
    Call(CapabilityTarget.Operation),
    None(*CapabilityTarget.values());

    companion object {
        fun allCapabilities(vararg filterBy: CapabilityTarget): Iterable<Capability> =
                ALL_CAPABILITIES.filter { it.targets.intersect(filterBy.asIterable()).isNotEmpty() }
    }
}

val ALL_CAPABILITIES : Array<Capability> = EnumSet.allOf(Capability::class.java).filter { it != Capability.None }.toTypedArray()

interface AccessConstraints {
    fun allow(vararg rule : Pair<KProperty<*>, (RoleEntity) -> Boolean>) : Map<KProperty<*>, (RoleEntity) -> Boolean> =
        rule.asIterable().toMap()
}

abstract class Constraint<E : BaseEntity, RE : RoleEntity>(
        val capabilities: Set<Capability>,
        val roles: Set<KClass<RE>>,
        val accessPredicate : ((E?, RE) -> Boolean)?
)

class BehaviorConstraint<E : BaseEntity, RE : RoleEntity>(val operation : KFunction<*>, roles : Set<KClass<RE>>, capabilities : Set<Capability>, condition : ((E?, RE) -> Boolean)?) : Constraint<E, RE>(capabilities, roles, condition)

class DataConstraint<E : BaseEntity, RE : RoleEntity>(val property : KProperty<*>, roles : Set<KClass<RE>>, capabilities : Set<Capability>, condition : ((E?, RE) -> Boolean)?) : Constraint<E, RE>(capabilities, roles, condition)

class EntityConstraint<E : BaseEntity, RE : RoleEntity>(roles : Set<KClass<RE>>, capabilities : Set<Capability>, condition : ((E?, RE) -> Boolean)?) : Constraint<E, RE>(capabilities, roles, condition)

fun <E : BaseEntity, RE : RoleEntity> constraint(
        roles : Set<KClass<RE>>, capabilities: Set<Capability>, condition : ((E?, RE) -> Boolean)? = null) =
        EntityConstraint(roles, capabilities, condition)


fun <E : BaseEntity, RE : RoleEntity> constraint(property : KProperty<*>,
                                                 roles : Set<KClass<RE>>, capabilities: Set<Capability>, condition : ((E?, RE) -> Boolean)? = null) =
        DataConstraint(property, roles, capabilities, condition)


fun <E : BaseEntity, RE : RoleEntity> constraint(function : KFunction<*>,
                                                 roles : Set<KClass<RE>>, capabilities: Set<Capability>, condition : ((E?, RE) -> Boolean)? = null) =
        BehaviorConstraint(function, roles, capabilities, condition)

fun <E : BaseEntity, RE : RoleEntity> constraint(clazz : KClass<E>,
                                                 roles : Set<KClass<RE>>, capabilities: Set<Capability>, condition : ((E?, RE) -> Boolean)? = null) =
        EntityConstraint(roles, capabilities, condition)


inline fun can(vararg capabilities : Capability) = capabilities.toSet()

inline fun <RE: RoleEntity>roles(vararg roles : KClass<RE>) : Set<KClass<RE>> = roles.toSet()

inline fun <E : BaseEntity, RE: RoleEntity>provided(noinline predicate : ((E?, RE) -> Boolean)) : ((E?, RE) -> Boolean) = predicate

open class AccessControl<E : BaseEntity, RE : RoleEntity>(vararg  val constraints : Constraint<E, out RE>)