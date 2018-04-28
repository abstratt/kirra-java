package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import com.abstratt.kirra.spring.api.SecurityService
import org.springframework.aop.support.AopUtils
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod

@Service
@Transactional
open class KirraSpringInstanceManagement (
    private val kirraSpringMetamodel: KirraSpringMetamodel,
    private val kirraSpringInstanceBridge: KirraSpringInstanceBridge,
    private val schemaManagement: SchemaManagement,
    private val securityService: SecurityService
) : InstanceManagement {

    @Secured
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    override fun createInstance(instance: Instance): Instance {
        val asEntity : BaseEntity = kirraSpringInstanceBridge.fromInstance(instance)
        val asService : BaseService<BaseEntity,*> = getEntityService(instance.typeRef)
        val created = asService.create(asEntity)
        val toConvert = created
        val createdInstance = kirraSpringInstanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
        return createdInstance
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getInstance(namespace: String, entityName: String, externalId: String, dataProfile: InstanceManagement.DataProfile?): Instance {
        val found = retrieveJavaInstance(namespace, entityName, externalId)
        KirraException.ensure(found != null, KirraException.Kind.OBJECT_NOT_FOUND, {null})
        val toConvert = found!!
        return kirraSpringInstanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
    }

    private fun retrieveJavaInstance(namespace: String, entityName: String, externalId: String): BaseEntity? {
        val entityClass: Class<BaseEntity>? = kirraSpringMetamodel.getEntityClass(namespace, entityName)
        val asService: BaseService<BaseEntity, *> = getEntityService(TypeRef(namespace, entityName, TypeRef.TypeKind.Entity))
        val found = asService.findById(externalId.toLong())
        return found
    }

    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    override fun updateInstance(instance: Instance): Instance {
        val asEntity : BaseEntity = kirraSpringInstanceBridge.fromInstance(instance)
        asEntity.id = instance.objectId.toLong()
        val asService : BaseService<BaseEntity,*> = getEntityService(instance.typeRef)
        val updatedEntity = asService.update(asEntity)
        KirraException.ensure(updatedEntity != null, KirraException.Kind.OBJECT_NOT_FOUND, {null})
        val toConvert = updatedEntity!!
        val updatedInstance = kirraSpringInstanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
        return updatedInstance
    }

    private fun getEntityService(typeRef: TypeRef) =
            kirraSpringMetamodel.getEntityService(typeRef)

    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    override fun linkInstances(relationship: Relationship?, sourceId: String?, destinationId: InstanceRef?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun zap() {
        throw UnsupportedOperationException("zap not supported")
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getEnabledEntityActions(entity: Entity?): MutableList<Operation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getRelationshipDomain(entity: Entity, objectId: String, relationship: Relationship): MutableList<Instance> {
        //TODO-RC honor relationship constraints
        return getInstances(relationship.typeRef.entityNamespace, relationship.typeRef.typeName, InstanceManagement.DataProfile.Slim)
    }

    override fun saveContext() {
    }

    override fun getEntityCapabilities(typeRef: TypeRef): EntityCapabilities {
        val entity = schemaManagement.getEntity(typeRef)
        val queries = entity.operations.filter {
            it.kind == Operation.OperationKind.Finder && !it.isInstanceOperation
        }
        val staticActions = entity.operations.filter {
            it.kind == Operation.OperationKind.Action && !it.isInstanceOperation
        }

        val accessControl = getAccessControl(typeRef)

        if (accessControl == null) {
            return allEntityCapabilities(entity)
        }

        return getEntityCapabilities(accessControl)
    }

    private fun getEntityCapabilities(accessControl: AccessControl<*, *>): EntityCapabilities {
        val constraints = accessControl.constraints

        val currentUserRoles = securityService.getCurrentUserRoles() ?: emptyList()

        val entityConstraints = constraints.filterIsInstance(EntityConstraint::class.java).filter { it.capabilities.any { it.targets.contains(CapabilityTarget.Entity) } }
        val functionConstraints = constraints.filterIsInstance(BehaviorConstraint::class.java)
        val queryConstraints = functionConstraints.filter { kirraSpringMetamodel.getOperationKind(it.operation, false) == Operation.OperationKind.Finder }.groupBy { it.operation }
        val actionConstraints = functionConstraints.filter { kirraSpringMetamodel.getOperationKind(it.operation, false) == Operation.OperationKind.Action }.groupBy { it.operation }

        val entityCapabilities = computeCapabilities(null, currentUserRoles, listOf(CapabilityTarget.Entity), ConstraintLayer(entityConstraints))
        val queryCapabilities = queryConstraints.map { Pair(it.key.name, computeCapabilities(null, currentUserRoles, listOf(CapabilityTarget.StaticOperation), ConstraintLayer(entityConstraints), ConstraintLayer(it.value))) }.toMap()
        val staticActionCapabilities = actionConstraints.map { Pair(it.key.name, computeCapabilities(null, currentUserRoles, listOf(CapabilityTarget.StaticOperation), ConstraintLayer(entityConstraints), ConstraintLayer(it.value))) }.toMap()
        return toEntityCapabilities(entityCapabilities, queryCapabilities, staticActionCapabilities)
    }

    private fun toCapabilityNames(capability: List<Capability>) =
            capability.map { it.name }

    private fun allEntityCapabilities(entity: Entity): EntityCapabilities =
            EntityCapabilities(Capability.allCapabilities(CapabilityTarget.Entity).map { it.name },
                    entity.operations.filter { it.kind == Operation.OperationKind.Finder && !it.isInstanceOperation}.map { Pair(it.name, Capability.allCapabilities(CapabilityTarget.StaticOperation).map { it.name }) }.toMap(),
                    entity.operations.filter { it.kind == Operation.OperationKind.Action && !it.isInstanceOperation}.map { Pair(it.name, Capability.allCapabilities(CapabilityTarget.StaticOperation).map { it.name }) }.toMap())

    private fun allInstanceCapabilities(entity: Entity): InstanceCapabilities =
            InstanceCapabilities(Capability.allCapabilities(CapabilityTarget.Instance).map { it.name },
                    entity.properties.filter { true }.map { Pair(it.name, Capability.allCapabilities(CapabilityTarget.Property).map { it.name }) }.toMap(),
                    entity.relationships.filter { true }.map { Pair(it.name, Capability.allCapabilities(CapabilityTarget.Relationship).map { it.name }) }.toMap(),
                    entity.operations.filter { it.kind == Operation.OperationKind.Action && it.isInstanceOperation}.map { Pair(it.name, Capability.allCapabilities(CapabilityTarget.StaticOperation).map { it.name }) }.toMap())

    private fun getAccessControl(typeRef: TypeRef): AccessControl<*, *>? {
        val entityClass = kirraSpringMetamodel.getEntityClass(typeRef)!!
        val accessControl = entityClass.kotlin.nestedClasses.find { it.isSubclassOf(AccessControl::class) }?.objectInstance as? AccessControl<*, *>
        return accessControl
    }


    override fun getInstanceCapabilities(typeRef: TypeRef, objectId: String): InstanceCapabilities {
        val entity = schemaManagement.getEntity(typeRef)
        val instance = retrieveJavaInstance(typeRef.entityNamespace, typeRef.typeName, objectId)

        val entityClass = kirraSpringMetamodel.getEntityClass(typeRef)!!
        val entityType = kirraSpringMetamodel.getJpaEntity(entityClass)!!
        val relationships = kirraSpringMetamodel.getRelationships(entityType).associateBy { it.name }
        val attributes = kirraSpringMetamodel.getAttributes(entityType).associateBy { it.name }
        val actions = entity.operations.filter {
            it.kind == Operation.OperationKind.Action && it.isInstanceOperation
        }
        val accessControl = getAccessControl(typeRef)

        if (accessControl == null) {
            return allInstanceCapabilities(entity)
        }

        return getInstanceCapabilities(accessControl, entity, instance)
    }

    private fun getInstanceCapabilities(accessControl: AccessControl<*, *>, entity: Entity, instance: BaseEntity?): InstanceCapabilities {
        val constraints = accessControl.constraints

        val currentUserRoles = securityService.getCurrentUserRoles() ?: emptyList()


        val entityConstraints = constraints.filterIsInstance(EntityConstraint::class.java)
        val behaviorConstraints = constraints.filterIsInstance(BehaviorConstraint::class.java)
        val dataConstraints = constraints.filterIsInstance(DataConstraint::class.java)
        val actionConstraints = behaviorConstraints.filter { kirraSpringMetamodel.getOperationKind(it.operation, true) == Operation.OperationKind.Action }.groupBy { it.operation }
        val attributeConstraints = dataConstraints.filter { entity.getProperty(it.property.name) != null }.groupBy { it.property }
        val relationshipConstraints = dataConstraints.filter { entity.getRelationship(it.property.name) != null }.groupBy { it.property }

        val instanceCapabilities = computeCapabilities(instance, currentUserRoles, listOf(CapabilityTarget.Instance), ConstraintLayer(entityConstraints))
        val actionCapabilities = actionConstraints.map { Pair(it.key.name, computeCapabilities(instance, currentUserRoles, listOf(CapabilityTarget.Operation), ConstraintLayer(entityConstraints), ConstraintLayer(it.value))) }.toMap()
        val attributeCapabilities = attributeConstraints.map { Pair(it.key.name, computeCapabilities(instance, currentUserRoles, listOf(CapabilityTarget.Property), ConstraintLayer(entityConstraints), ConstraintLayer(it.value))) }.toMap()
        val relationshipCapabilities = relationshipConstraints.map { Pair(it.key.name, computeCapabilities(instance, currentUserRoles, listOf(CapabilityTarget.Relationship), ConstraintLayer(entityConstraints), ConstraintLayer(it.value))) }.toMap()
        return toInstanceCapabilities(instanceCapabilities, attributeCapabilities, relationshipCapabilities, actionCapabilities)
    }

    private fun toEntityCapabilities(entityCapabilities: List<Capability>, queryCapabilities: Map<String, List<Capability>>, staticActionCapabilities: Map<String, List<Capability>>): EntityCapabilities =
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

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getCurrentUser(): Instance? =
            securityService.getCurrentUser()?.let {
                val toConvert = it
                kirraSpringInstanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
            }

    override fun getCurrentUserRoles(): List<Instance> =
        (securityService.getCurrentUserRoles() ?: emptyList()).map {
            val toConvert = it
            kirraSpringInstanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Empty)
        }

    override fun filterInstances(criteria : MutableMap<String, MutableList<Any>>?, namespace : String, name : String, profile : InstanceManagement.DataProfile?): MutableList<Instance> =
        getInstances(namespace, name, profile)

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getInstances(namespace: String, entityName: String, dataProfile: InstanceManagement.DataProfile?): MutableList<Instance> {
        val entityClass : Class<BaseEntity>? = kirraSpringMetamodel.getEntityClass(namespace, entityName)
        val asService : BaseService<BaseEntity,*> = getEntityService(TypeRef(namespace, entityName, TypeRef.TypeKind.Entity))
        val listed = asService.list()
        val elements = listed.content
        return kirraSpringInstanceBridge.toInstances(elements).toMutableList()
    }

    //public List<Instance> filterInstances(Map<String, List<Object>> criteria, String namespace, String name, DataProfile profile) {}

    override fun isRestricted(): Boolean {
        return true
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun executeQuery(operation: Operation, externalId: String?, arguments: MutableList<*>?, pageRequest: InstanceManagement.PageRequest?): MutableList<*> {
        val pageRequest = pageRequest?.let {
            PageRequest.of(it.first?.toInt() ?: 0, it.maximum ?: 100)
        }
        var customImplArguments : Map<String, Any?> = mapOf(Pair("pageRequest", pageRequest))

        val (service, javaInstance: Any?, result) = doExecuteOperation(operation, externalId, arguments, customImplArguments)
        return result
    }

    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    override fun executeOperation(operation: Operation, externalId: String?, arguments: MutableList<*>?): MutableList<*> {
        val (service, javaInstance: Any?, result) = doExecuteOperation(operation, externalId, arguments)

        if (javaInstance is BaseEntity) {
            service.update(javaInstance)
        }

        return result
    }

    private fun doExecuteOperation(operation: Operation, externalId: String?, arguments: MutableList<*>?, customImplArguments : Map<String, Any?> = emptyMap()): Triple<BaseService<BaseEntity, *>, Any?, MutableList<*>> {
        val kirraEntity = schemaManagement.getEntity(operation.owner)
        val entityClass: Class<BaseEntity> = kirraSpringMetamodel.getEntityClass(kirraEntity.entityNamespace, kirraEntity.name)!!
        val service = getEntityService(operation.owner)

        val entityInstance = externalId?.let { service.findById(it.toLong()) }

        // it will be null if the service does not have a function matching the Kirra operation name
        val serviceImplementationFunction = AopUtils.getTargetClass(service).kotlin.functions.find { it.name == operation.name && kirraSpringMetamodel.isKirraOperation(it) }
        val serviceFunction = serviceImplementationFunction?.let { impl ->
            service::class.functions.find {
                it.name == operation.name && it.javaMethod!!.parameterTypes.toList() == impl.javaMethod!!.parameterTypes.toList()
            }
        }

        val entityFunction = entityClass.kotlin.functions.find { it.name == operation.name && kirraSpringMetamodel.isKirraOperation(it) }

        val (javaInstance, actualFunction, functionParameterNames) = if (serviceFunction != null) Triple(service, serviceFunction!!, serviceImplementationFunction.valueParameters.map { it.name }) else Triple(entityInstance, entityFunction!!, entityFunction.valueParameters.map { it.name })
        BusinessException.ensure(javaInstance != null, ErrorCode.UNKNOWN_OBJECT)

        val boundFunction = BoundFunction(javaInstance!!, actualFunction)
        val matchedModelArguments = arguments!!.mapIndexed { i, argument -> Pair(operation.parameters[i].name, kirraSpringInstanceBridge.mapKirraValueToJava(operation.parameters[i], argument)) }.toMap()
        val matchedArguments = matchedModelArguments + customImplArguments
        val offset = if (operation.isInstanceOperation && serviceFunction != null) 1 else 0
        val kotlinMatchedModelArguments = matchedModelArguments.map { namedValue ->
            val index = operation.parameters.indexOfFirst { it.name == namedValue.key }
            KirraException.ensure(index >= 0, KirraException.Kind.ELEMENT_NOT_FOUND, { "Missing parameter: ${namedValue.key}" })
            val kirraParameter = operation.parameters[index]
            val ktParameter = boundFunction.valueParameters[offset + index]
            val ktValue = namedValue.value
            Pair(ktParameter!!, ktValue)
        }.toMap().toMutableMap()

        val kotlinMatchedImplArguments = customImplArguments.map { namedImplArgument ->
            val index = functionParameterNames.indexOf(namedImplArgument.key)
            Pair(boundFunction.valueParameters[index], namedImplArgument.value)
        }.filterNotNull()

        val kotlinMatchedArguments = (kotlinMatchedModelArguments + kotlinMatchedImplArguments).toMutableMap()

        if (serviceFunction != null) {
            val serviceParameter = serviceFunction.valueParameters[0]
            // a service may implement an instance operation - in that case, the entity instance is the first parameter
            if (operation.isInstanceOperation)
                kotlinMatchedArguments[serviceParameter] = entityInstance
        }

        val callResult = boundFunction.callBy(kotlinMatchedArguments)
        val asList = if (callResult == null || callResult is Unit)
            emptyList<Any>().toMutableList()
        else if (callResult is Iterable<*>)
            callResult.toMutableList()
        else
            listOf(callResult).toMutableList()
        val result = asList.map { if (it is BaseEntity) {
            val toConvert = it
            kirraSpringInstanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
        } else it }.toMutableList()

        return Triple(service, javaInstance, result)
    }

    override fun getParameterDomain(entity: Entity, externalId: String, action: Operation, parameter: Parameter): MutableList<Instance> {
        //TODO-RC honor parameter constraints
        return getInstances(parameter.typeRef.entityNamespace, parameter.typeRef.typeName, InstanceManagement.DataProfile.Slim)
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun newInstance(namespace: String, entityName: String): Instance {
        val entityClass : Class<BaseEntity>? = kirraSpringMetamodel.getEntityClass(namespace, entityName)
        KirraException.ensure(entityClass != null, KirraException.Kind.ENTITY, { "${namespace}.${entityName}"})
        val toConvert = entityClass!!.newInstance()
        return kirraSpringInstanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
    }

    override fun validateInstance(toValidate: Instance?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getRelatedInstances(namespace: String, entityName: String, externalId: String, relationshipName: String, dataProfile: InstanceManagement.DataProfile?): MutableList<Instance> {
        val entityRef = TypeRef(namespace, entityName, TypeRef.TypeKind.Entity)
        val entityClass = kirraSpringMetamodel.getEntityClass(entityRef)
        val found = retrieveJavaInstance(namespace, entityName, externalId)
        KirraException.ensure(found != null, KirraException.Kind.OBJECT_NOT_FOUND, {null})
        val relationshipKProperty = found!!::class.memberProperties.firstOrNull { it.name == relationshipName }
        val entityService = getEntityService(entityRef)
        var relatedObjects : Iterable<BaseEntity>? = null
        if (relationshipKProperty != null) {
            relatedObjects = entityService.getRelated(externalId.toLong(), relationshipKProperty as KProperty1<BaseEntity, BaseEntity>)
        } else {
            val relationshipAccessor = kirraSpringMetamodel.getRelationshipAccessor(entityService, entityClass!!, relationshipName)
            KirraException.ensure(relationshipAccessor != null, KirraException.Kind.ELEMENT_NOT_FOUND, null)
            relatedObjects = relationshipAccessor!!.call(entityService, found) as? Iterable<BaseEntity>
        }
        return kirraSpringInstanceBridge.toInstances(relatedObjects!!).toMutableList()
    }

    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    override fun unlinkInstances(relationship: Relationship?, sourceId: String?, destinationId: InstanceRef?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteInstance(entityNamespace: String, entityName: String, id: String) {
        val entityClass : Class<BaseEntity>? = kirraSpringMetamodel.getEntityClass(entityNamespace, entityName)
        val asService : BaseService<BaseEntity,*> = getEntityService(TypeRef(entityNamespace, entityName, TypeRef.TypeKind.Entity))
        asService.delete(id.toLong())
    }
}

class BoundFunction<R>(val instance : Any, val baseFunction : KFunction<R>) : KFunction<R> by baseFunction {
    override fun call(vararg args: Any?): R =
            baseFunction.call(*(listOf(instance) + args.asList()).toTypedArray())

    override fun callBy(args: Map<KParameter, Any?>): R {
        val instanceParameter = baseFunction.instanceParameter!!
        val targetObject = mapOf(Pair(instanceParameter, instance))
        val combinedArguments = targetObject + args
        return baseFunction.callBy(combinedArguments)
    }

    override val parameters: List<KParameter>
        get() = listOf(baseFunction.instanceParameter!!) + baseFunction.parameters
}


