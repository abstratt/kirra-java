package com.abstratt.kirra.pojo

import java.util.*
import kotlin.reflect.KClass


data class ConstraintLayer(val constraints: List<Constraint<*, *>>) {
    constructor(vararg constraint: Constraint<*, *>) : this(constraint.toList())
}

data class ConstraintGrant(val constraint: Constraint<*, *>, val granted: Boolean)

data class CapabilityGrant(val capability: Capability, val constraintGrant: ConstraintGrant)

data class RoleGrant(val roleClass: KClass<out IRoleEntity>, val capabilityGrants: MutableCollection<CapabilityGrant>) {
    constructor(roleClass: KClass<out IRoleEntity>, capabilityGrants: Iterable<CapabilityGrant>) : this(roleClass, capabilityGrants.toMutableList())

    fun add(another: RoleGrant) =
        apply { capabilityGrants.addAll(another.capabilityGrants) }
}

data class GrantLayer(val roleGrants : MutableMap<KClass<out IRoleEntity>, RoleGrant>) {
    fun merge(next: GrantLayer): GrantLayer =
        apply { roleGrants.putAll(next.roleGrants) }
}


/**
 * Computes the capabilities for a user with the given roles.
 *
 * The capabilities are obtained from all constraintLayers that can be satisfied for
 * the given roles.
 *
 * A user will have some capability if there is a least one satisfied constraint that
 * provides that capability.
 *
 * Constraints are associated to a (potentially empty) set of roles (and their subclasses).
 *
 * For a role and a capability, if a role has both a constraint that is associated with
 * the role class, and another constraint that is only associated with a super class,
 * the directly associated one wins.
 *
 * Constraints are layered - for any given role, a constraint in an outer layer is
 * overridden by a constraint for the same role in an inner layer.
 *
 * @param instance an optional context instance
 * @param roles current user roles
 * @param targets the capability targets to consider
 * @param constraintLayers layered constraintLayers to evaluate (outer to inner)
 */
fun computeCapabilities(
        instance: IBaseEntity?,
        roles: List<IRoleEntity>,
        targets: Iterable<CapabilityTarget>,
        constraintLayers: List<ConstraintLayer>
): List<Capability> {
    if (constraintLayers.isEmpty())
        return ALL_CAPABILITIES.toList().filter {
            it.targets.intersect(targets).isNotEmpty()
        }
    val roleClasses = roles.map { it::class }

    val layers : Iterable<GrantLayer> = constraintLayers.map { layer ->
        buildLayer(targets, roleClasses, layer.constraints)
    }
    val roleGrants = layers.reduce { collected, next ->
        collected.merge(next)
    }.roleGrants.values

    val matchingGrants = roleGrants
            .map { it.capabilityGrants }
            .flatten()
            .filter {
                it.constraintGrant.granted
                        && constraintFilter(instance, roles).invoke(
                        it.constraintGrant.constraint as Constraint<IBaseEntity, IRoleEntity>
                )
            }

    return matchingGrants.map { it.capability }
            .toSortedSet()
            .toList()
}

/**
 * A layer is a list of role grants.
 */
fun buildLayer(targets: Iterable<CapabilityTarget>, roleClasses: List<KClass<out IRoleEntity>>, constraints: List<Constraint<*, *>>): GrantLayer =
        GrantLayer(
                constraints.map { constraint ->
                    constraint.roles
                            .filter { roleClasses.contains(it) }
                            .map { role ->
                                buildRoleGrant(targets, role, constraint)
                            }
                }.flatten()
                        .groupBy { it.roleClass }
                        .map {
                            Pair(it.key, it.value.reduce { acc, it -> acc.add(it) })
                        }.toMap(LinkedHashMap())
        )

/**
 * Builds a role grant from one constraint.
 *
 * A role grant defines which capability grants are available for a role.
 */
fun buildRoleGrant(
        targets: Iterable<CapabilityTarget>,
        roleClass: KClass<out IRoleEntity>,
        constraint: Constraint<*, *>
): RoleGrant {
    val (granted, capabilities) = if (constraint.capabilities == setOf(Capability.None))
        Pair(false, ALL_CAPABILITIES.toSet())
    else
        Pair(true, constraint.capabilities)
    val capabilityGrants = capabilities
            .filter { it.targets.intersect(targets).isNotEmpty() }
            .map { capability ->
                CapabilityGrant(capability, ConstraintGrant(constraint, granted))
            }
    return RoleGrant(roleClass, capabilityGrants)
}

fun computeCapabilities(
        instance: IBaseEntity?,
        roles: List<IRoleEntity>,
        targets: Iterable<CapabilityTarget>,
        vararg constraints: ConstraintLayer
): List<Capability> = computeCapabilities(instance, roles, targets, constraints.toList())


fun <CON : Constraint<E, RE>, E : IBaseEntity, RE : IRoleEntity> constraintFilter(instance: E?, roles: List<RE>): (CON) -> Boolean =
    { it: CON ->
        it.accessPredicate == null || roles.any { role -> checkPredicate(it, instance, role) }
    }

fun <CON : Constraint<E, RE>, E : IBaseEntity, RE : IRoleEntity> checkPredicate(constraint: CON, instance: E?, role: RE): Boolean =
    constraint.accessPredicate!!(instance, role)

