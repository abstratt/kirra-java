package com.abstratt.kirra.pojo

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

interface IRoleEntity : IBaseEntity{
    @ImplementationOp
    fun getRole() : IUserRole
}
enum class CapabilityTarget() {
    Property, Relationship, Operation, Entity, Instance
}

enum class Capability(vararg val targets : CapabilityTarget) {
    Create(CapabilityTarget.Entity),
    Delete(CapabilityTarget.Instance),
    List(CapabilityTarget.Entity),
    Read(CapabilityTarget.Instance, CapabilityTarget.Property, CapabilityTarget.Relationship),
    Update(CapabilityTarget.Instance, CapabilityTarget.Property, CapabilityTarget.Relationship),
    Call(CapabilityTarget.Operation),
    None(*CapabilityTarget.values());

    companion object {
        fun allCapabilities(vararg filterBy: CapabilityTarget): Iterable<Capability> =
                ALL_CAPABILITIES.filter { it.targets.intersect(filterBy.asIterable()).isNotEmpty() }
    }
}

val ALL_CAPABILITIES : Array<Capability> = EnumSet.allOf(Capability::class.java).filter { it != Capability.None }.toTypedArray()
val ALL_CAPABILITIES_SET = ALL_CAPABILITIES.toSet()

interface AccessConstraints {
    fun allow(vararg rule : Pair<KProperty<*>, (IRoleEntity) -> Boolean>) : Map<KProperty<*>, (IRoleEntity) -> Boolean> =
        rule.asIterable().toMap()
}

abstract class Constraint<E : IBaseEntity, RE : IRoleEntity>(
        val capabilities: Set<Capability>,
        val roles: Set<KClass<RE>>,
        val accessPredicate : ((E?, RE) -> Boolean)?
)

class BehaviorConstraint<E : IBaseEntity, RE : IRoleEntity>(val operation : KFunction<*>, roles : Set<KClass<RE>>, capabilities : Set<Capability>, condition : ((E?, RE) -> Boolean)?) : Constraint<E, RE>(capabilities, roles, condition)

class DataConstraint<E : IBaseEntity, RE : IRoleEntity>(val property : KProperty<*>, roles : Set<KClass<RE>>, capabilities : Set<Capability>, condition : ((E?, RE) -> Boolean)?) : Constraint<E, RE>(capabilities, roles, condition)

class EntityConstraint<E : IBaseEntity, RE : IRoleEntity>(roles : Set<KClass<RE>>, capabilities : Set<Capability>, condition : ((E?, RE) -> Boolean)?) : Constraint<E, RE>(capabilities, roles, condition)

fun <E : IBaseEntity, RE : IRoleEntity> constraint(
        roles : Set<KClass<RE>>, capabilities: Set<Capability>, condition : ((E?, RE) -> Boolean)? = null) =
        EntityConstraint(roles, capabilities, condition)


fun <E : IBaseEntity, RE : IRoleEntity> constraint(property : KProperty<*>,
                                                   roles : Set<KClass<RE>>, capabilities: Set<Capability>, condition : ((E?, RE) -> Boolean)? = null) =
        DataConstraint(property, roles, capabilities, condition)


fun <E : IBaseEntity, RE : IRoleEntity> constraint(function : KFunction<*>,
                                                   roles : Set<KClass<RE>>, capabilities: Set<Capability>, condition : ((E?, RE) -> Boolean)? = null) =
        BehaviorConstraint(function, roles, capabilities, condition)

fun <E : IBaseEntity, RE : IRoleEntity> constraint(clazz : KClass<E>,
                                                   roles : Set<KClass<RE>>, capabilities: Set<Capability>, condition : ((E?, RE) -> Boolean)? = null) =
        EntityConstraint(roles, capabilities, condition)


inline fun can(vararg capabilities : Capability) = capabilities.toSet()
inline fun can(capabilities : Iterable<Capability>) = capabilities.toSet()

inline fun <RE: IRoleEntity>roles(vararg roles : KClass<RE>) : Set<KClass<RE>> = roles.toSet()

inline fun <E : IBaseEntity, RE: IRoleEntity>provided(noinline predicate : ((E?, RE) -> Boolean)) : ((E?, RE) -> Boolean) = predicate

open class AccessControl<E : IBaseEntity, RE : IRoleEntity>(vararg  val constraints : Constraint<E, out RE>)