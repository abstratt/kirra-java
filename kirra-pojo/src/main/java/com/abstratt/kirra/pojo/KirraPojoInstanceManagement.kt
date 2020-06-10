package com.abstratt.kirra.pojo

import com.abstratt.kirra.*
import kotlin.reflect.full.isSubclassOf

/**
 * An instance management implementation that works with Java applications.
 */
abstract class KirraPojoInstanceManagement(val kirraMetamodel: KirraMetamodel, val instanceBridge: KirraInstanceBridge, val schemaManagement: SchemaManagement) : InstanceManagement {
    override fun createInstance(instance: Instance): Instance {
        val asEntity: IBaseEntity = instanceBridge.fromInstance(instance)
        val asService: IBaseService<IBaseEntity> = getEntityService(instance.typeRef)
        val created = asService.create(asEntity)
        val toConvert = created
        val createdInstance = instanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
        return createdInstance
    }

    override fun getInstance(namespace: String, entityName: String, externalId: String, dataProfile: InstanceManagement.DataProfile?): Instance {
        val found = retrieveJavaInstance(namespace, entityName, externalId)
        KirraException.ensure(found != null, KirraException.Kind.OBJECT_NOT_FOUND, {null})
        val toConvert = found!!
        return instanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
    }

    override fun getRelationshipDomain(entity: Entity, objectId: String, relationship: Relationship): MutableList<Instance> {
        //TODO-RC honor relationship constraints
        return getInstances(relationship.typeRef.entityNamespace, relationship.typeRef.typeName, InstanceManagement.DataProfile.Slim)
    }

    override fun updateInstance(instance: Instance): Instance {
        val asEntity : IBaseEntity = instanceBridge.fromInstance(instance)
        asEntity.assignInstanceId(instance.objectId.toLong())
        val asService : IBaseService<IBaseEntity> = getEntityService(instance.typeRef)
        val updatedEntity = asService.update(asEntity)
        KirraException.ensure(updatedEntity != null, KirraException.Kind.OBJECT_NOT_FOUND, {null})
        val toConvert = updatedEntity!!
        val updatedInstance = instanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
        return updatedInstance
    }
    protected fun getEntityService(typeRef: TypeRef) : IBaseService<IBaseEntity> =
            kirraMetamodel.getEntityService(typeRef)!!

    protected fun retrieveJavaInstance(typeRef : TypeRef, externalId: String): IBaseEntity? =
            retrieveJavaInstance(typeRef.namespace, typeRef.typeName, externalId)

    protected fun retrieveJavaInstance(namespace: String, entityName: String, externalId: String): IBaseEntity? {
        val typeRef = TypeRef(namespace, entityName, TypeRef.TypeKind.Entity)
        val asService: IBaseService<IBaseEntity> = getEntityService(typeRef)
        val found = asService.findById(externalId.toLong())
        return found
    }

    override fun unlinkInstances(relationship: Relationship?, sourceId: String?, destinationId: InstanceRef?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteInstance(entityNamespace: String, entityName: String, id: String) {
        val typeRef = TypeRef(entityNamespace, entityName, TypeRef.TypeKind.Entity)
        val asService : IBaseService<IBaseEntity> = getEntityService(typeRef)
        asService.delete(id.toLong())
    }

    override fun linkInstances(relationship: Relationship?, sourceId: String?, destinationId: InstanceRef?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun zap() {
        throw UnsupportedOperationException("zap not supported")
    }

    override fun getEnabledEntityActions(entity: Entity?): MutableList<Operation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    protected fun getAccessControl(typeRef: TypeRef): AccessControl<*, *>? {
        val entityClass = kirraMetamodel.getEntityClass(typeRef)!!
        val accessControl = entityClass.kotlin.nestedClasses.find { it.isSubclassOf(AccessControl::class) }?.objectInstance as? AccessControl<*, *>
        return accessControl
    }

    protected fun getInstanceCapabilities(accessControl: AccessControl<*, *>, entity: Entity, instance: IBaseEntity?, currentUserRoles: List<IRoleEntity>): InstanceCapabilities {
        val constraints = accessControl.constraints


        val entityConstraints = constraints.filterIsInstance(EntityConstraint::class.java)
        val behaviorConstraints = constraints.filterIsInstance(BehaviorConstraint::class.java)
        val dataConstraints = constraints.filterIsInstance(DataConstraint::class.java)
        val actionConstraints = behaviorConstraints.filter { kirraMetamodel.getOperationKind(it.operation, true) == Operation.OperationKind.Action }.groupBy { it.operation }
        val attributeConstraints = dataConstraints.filter { entity.getProperty(it.property.name) != null }.groupBy { it.property }
        val relationshipConstraints = dataConstraints.filter { entity.getRelationship(it.property.name) != null }.groupBy { it.property }

        val instanceCapabilities = computeCapabilities(instance, currentUserRoles, listOf(CapabilityTarget.Instance), ConstraintLayer(entityConstraints))
        val actionCapabilities = actionConstraints.map { Pair(it.key.name, computeCapabilities(instance, currentUserRoles, listOf(CapabilityTarget.Operation), ConstraintLayer(entityConstraints), ConstraintLayer(it.value))) }.toMap()
        val attributeCapabilities = attributeConstraints.map { Pair(it.key.name, computeCapabilities(instance, currentUserRoles, listOf(CapabilityTarget.Property), ConstraintLayer(entityConstraints), ConstraintLayer(it.value))) }.toMap()
        val relationshipCapabilities = relationshipConstraints.map { Pair(it.key.name, computeCapabilities(instance, currentUserRoles, listOf(CapabilityTarget.Relationship), ConstraintLayer(entityConstraints), ConstraintLayer(it.value))) }.toMap()
        return toInstanceCapabilities(instanceCapabilities, attributeCapabilities, relationshipCapabilities, actionCapabilities)
    }
    protected fun toEntityCapabilities(entityCapabilities: List<Capability>, queryCapabilities: Map<String, List<Capability>>, staticActionCapabilities: Map<String, List<Capability>>): EntityCapabilities =
            EntityCapabilities(
                    toCapabilityNames(entityCapabilities),
                    queryCapabilities.map { Pair(it.key, toCapabilityNames(it.value)) }.toMap(),
                    staticActionCapabilities.map { Pair(it.key, toCapabilityNames(it.value)) }.toMap()
            )

    private fun toInstanceCapabilities(instanceCapabilities: List<Capability>, attributeCapabilities: Map<String, List<Capability>>, relationshipCapabilities: Map<String, List<Capability>>, actionCapabilities: Map<String, List<Capability>>): InstanceCapabilities =
            InstanceCapabilities(
                    toCapabilityNames(instanceCapabilities),
                    attributeCapabilities.map { Pair(it.key, toCapabilityNames(it.value)) }.toMap(),
                    relationshipCapabilities.map { Pair(it.key, toCapabilityNames(it.value)) }.toMap(),
                    actionCapabilities.map { Pair(it.key, toCapabilityNames(it.value)) }.toMap()
            )

    private fun toCapabilityNames(capability: List<Capability>) =
            capability.map { it.name }

    protected fun allEntityCapabilities(entity: Entity): EntityCapabilities =
            EntityCapabilities(Capability.allCapabilities(CapabilityTarget.Entity).map { it.name },
                    entity.operations.filter { it.kind == Operation.OperationKind.Finder && !it.isInstanceOperation}.map { Pair(it.name, Capability.allCapabilities(CapabilityTarget.Operation).map { it.name }) }.toMap(),
                    entity.operations.filter { it.kind == Operation.OperationKind.Action && !it.isInstanceOperation}.map { Pair(it.name, Capability.allCapabilities(CapabilityTarget.Operation).map { it.name }) }.toMap())

    protected fun allInstanceCapabilities(entity: Entity): InstanceCapabilities =
            InstanceCapabilities(Capability.allCapabilities(CapabilityTarget.Instance).map { it.name },
                    entity.properties.filter { true }.map { Pair(it.name, Capability.allCapabilities(CapabilityTarget.Property).map { it.name }) }.toMap(),
                    entity.relationships.filter { true }.map { Pair(it.name, Capability.allCapabilities(CapabilityTarget.Relationship).map { it.name }) }.toMap(),
                    entity.operations.filter { it.kind == Operation.OperationKind.Action && it.isInstanceOperation}.map { Pair(it.name, Capability.allCapabilities(CapabilityTarget.Operation).map { it.name }) }.toMap())

    override fun filterInstances(criteria : MutableMap<String, MutableList<Any>>?, namespace : String, name : String, profile : InstanceManagement.DataProfile?): MutableList<Instance> =
        getInstances(namespace, name, profile)

    override fun getInstances(namespace: String, entityName: String, dataProfile: InstanceManagement.DataProfile?): MutableList<Instance> {
        val typeRef = TypeRef(namespace, entityName, TypeRef.TypeKind.Entity)
        val asService : IBaseService<IBaseEntity> = getEntityService(typeRef)
        val listed = asService.list()
        val elements = listed.instances
        return instanceBridge.toInstances(elements).toMutableList()
    }

    protected abstract fun getNativeUserRoles(): List<IRoleEntity>

    override fun getCurrentUser(): Instance? =
            getNativeCurrentUser()?.let {
                val toConvert = it
                instanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Slim)
            }

    protected abstract fun getNativeCurrentUser(): IUserProfile?
    override fun getCurrentUserRoles(): List<Instance> =
            getNativeUserRoles().map {
                instanceBridge.toInstance(it, InstanceManagement.DataProfile.Empty)
            }

    override fun isRestricted(): Boolean {
        return true
    }

    protected fun getEntityCapabilities(accessControl: AccessControl<*, *>): EntityCapabilities {
        val constraints = accessControl.constraints

        val currentUserRoles = getNativeUserRoles()

        val entityConstraints = constraints.filterIsInstance(EntityConstraint::class.java).filter { it.capabilities.any { it.targets.contains(CapabilityTarget.Entity) } }
        val functionConstraints = constraints.filterIsInstance(BehaviorConstraint::class.java)
        val queryConstraints = functionConstraints.filter { kirraMetamodel.getOperationKind(it.operation, false) == Operation.OperationKind.Finder }.groupBy { it.operation }
        val actionConstraints = functionConstraints.filter { kirraMetamodel.getOperationKind(it.operation, false) == Operation.OperationKind.Action }.groupBy { it.operation }

        val entityCapabilities = computeCapabilities(null, currentUserRoles, listOf(CapabilityTarget.Entity), ConstraintLayer(entityConstraints))
        val queryCapabilities = queryConstraints.map { Pair(it.key.name, computeCapabilities(null, currentUserRoles, listOf(CapabilityTarget.Operation), ConstraintLayer(entityConstraints), ConstraintLayer(it.value))) }.toMap()
        val staticActionCapabilities = actionConstraints.map { Pair(it.key.name, computeCapabilities(null, currentUserRoles, listOf(CapabilityTarget.Operation), ConstraintLayer(entityConstraints), ConstraintLayer(it.value))) }.toMap()
        return toEntityCapabilities(entityCapabilities, queryCapabilities, staticActionCapabilities)
    }

    override fun getInstanceCapabilities(typeRef: TypeRef, objectId: String): InstanceCapabilities {
        val entity = schemaManagement.getEntity(typeRef)
        val instance = retrieveJavaInstance(typeRef.entityNamespace, typeRef.typeName, objectId)

        val accessControl = getAccessControl(typeRef)
        if (accessControl === null) {
            return allInstanceCapabilities(entity)
        }
        return getInstanceCapabilities(accessControl, entity, instance, getNativeUserRoles())
    }

}
