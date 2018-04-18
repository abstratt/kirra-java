package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import com.abstratt.kirra.spring.api.SecurityService
import com.abstratt.kirra.spring.user.UserProfileService
import com.abstratt.kirra.spring.user.RoleEntity
import com.abstratt.kirra.spring.user.RoleService
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import java.util.TreeSet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod

@Component
class KirraSpringInstanceManagement : InstanceManagement {
    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var kirraSpringApplication : KirraSpringApplication

    @Autowired
    private lateinit var kirraSpringMetamodel: KirraSpringMetamodel

    @Autowired
    private lateinit var kirraSpringInstanceBridge: KirraSpringInstanceBridge

    @Autowired
    private lateinit var schemaManagement: SchemaManagement

    @Autowired
    private lateinit var userProfileService: UserProfileService

    @Autowired
    private lateinit var roleService: RoleService

    @Autowired
    private lateinit var securityService: SecurityService

    @Secured
    override fun createInstance(instance: Instance): Instance {
        val asEntity : BaseEntity = kirraSpringInstanceBridge.fromInstance(instance)
        val asService : BaseService<BaseEntity,*> = getEntityService(instance.typeRef)
        val created = asService.create(asEntity)
        val toConvert = created
        val createdInstance = kirraSpringInstanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
        return createdInstance
    }

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
            applicationContext.getBean(typeRef.typeName.decapitalize() + "Service", BaseService::class.java) as BaseService<BaseEntity, *>

    override fun linkInstances(relationship: Relationship?, sourceId: String?, destinationId: InstanceRef?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun zap() {
        throw UnsupportedOperationException("zap not supported")
    }

    override fun getEnabledEntityActions(entity: Entity?): MutableList<Operation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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

        val constraints = accessControl.constraints

        val currentUserRoles = securityService.getCurrentUserRoles() ?: emptyList()
        val roleClasses = currentUserRoles.map { it::class }

        val entityConstraints = constraints.filterIsInstance(EntityConstraint::class.java).filter { it.capabilities.any { it.targets.contains(CapabilityTarget.Entity) } }
        val functionConstraints = constraints.filterIsInstance(BehaviorConstraint::class.java)
        val queryConstraints = functionConstraints.filter { kirraSpringMetamodel.getOperationKind(it.operation, false) == Operation.OperationKind.Finder }.groupBy { it.operation }
        val actionConstraints = functionConstraints.filter { kirraSpringMetamodel.getOperationKind(it.operation, false) == Operation.OperationKind.Action }.groupBy { it.operation }

        val entityCapabilities = mapConstraintsToCapabilities(null, currentUserRoles, entityConstraints, roleClasses, listOf(CapabilityTarget.Entity))
        val queryCapabilities = queryConstraints.map { Pair(it.key.name, mapConstraintsToCapabilities(null, currentUserRoles,it.value + entityConstraints, roleClasses, listOf(CapabilityTarget.StaticOperation))) }.toMap()
        val staticActionCapabilities = actionConstraints.map { Pair(it.key.name, mapConstraintsToCapabilities(null, currentUserRoles, it.value + entityConstraints, roleClasses, listOf(CapabilityTarget.StaticOperation))) }.toMap()
        return EntityCapabilities(entityCapabilities, queryCapabilities, staticActionCapabilities)
    }

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

    private fun <CON : Constraint<*,*>>  mapConstraintsToCapabilities(instance : BaseEntity?, roles : List<RoleEntity>, constraints: List<CON>, roleClasses: List<KClass<out RoleEntity>>, targets : Iterable<CapabilityTarget>): List<String> {
        val roleConstraints = constraints.filter {
            // do any roles match?
            it.roles.any { expected -> roleClasses.any { actual -> actual.isSubclassOf(expected) }}
        }
        val userConstraints : List<CON> = roleConstraints.filter {
            constraintFilter(instance, roles).invoke(it as Constraint<BaseEntity, RoleEntity>)
        }
        val asCapabilities = userConstraints.map {
            it.capabilities.filter {
                it.targets.intersect(targets).isNotEmpty()
            }.map { it.name }
        }
        return asCapabilities.flatten().toCollection(TreeSet()).toList()
    }

    private fun <CON : Constraint<E, RE>, E : BaseEntity, RE : RoleEntity> constraintFilter(instance: E?, roles : List<RE>): (CON) -> Boolean =
            { it : CON -> it.accessPredicate == null || roles.any { role -> checkPredicate(it, instance, role) }  }

    private fun <CON : Constraint<E, RE>, E : BaseEntity, RE : RoleEntity> checkPredicate(constraint: CON, instance: E?, role: RE): Boolean {
        val result = constraint.accessPredicate!!(instance, role)
        return result
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

        val constraints = accessControl.constraints

        val currentUserRoles = securityService.getCurrentUserRoles() ?: emptyList()
        val roleClasses = currentUserRoles.map { it::class }

        val entityConstraints = constraints.filterIsInstance(EntityConstraint::class.java)
        val behaviorConstraints = constraints.filterIsInstance(BehaviorConstraint::class.java)
        val dataConstraints = constraints.filterIsInstance(DataConstraint::class.java)
        val actionConstraints = behaviorConstraints.filter { kirraSpringMetamodel.getOperationKind(it.operation, true) == Operation.OperationKind.Action }.groupBy { it.operation }
        val attributeConstraints = dataConstraints.filter { entity.getProperty(it.property.name) != null }.groupBy { it.property }
        val relationshipConstraints = dataConstraints.filter { entity.getRelationship(it.property.name) != null }.groupBy { it.property }

        val instanceCapabilities = mapConstraintsToCapabilities(instance, currentUserRoles, entityConstraints, roleClasses, listOf(CapabilityTarget.Instance))
        val actionCapabilities = actionConstraints.map { Pair(it.key.name, mapConstraintsToCapabilities(instance, currentUserRoles, it.value + entityConstraints, roleClasses, listOf(CapabilityTarget.Operation))) }.toMap()
        val attributeCapabilities = attributeConstraints.map { Pair(it.key.name, mapConstraintsToCapabilities(instance, currentUserRoles, it.value + entityConstraints, roleClasses, listOf(CapabilityTarget.Property))) }.toMap()
        val relationshipCapabilities = relationshipConstraints.map { Pair(it.key.name, mapConstraintsToCapabilities(instance, currentUserRoles, it.value + entityConstraints, roleClasses, listOf(CapabilityTarget.Relationship))) }.toMap()
        return InstanceCapabilities(instanceCapabilities, attributeCapabilities, relationshipCapabilities, actionCapabilities)
    }

    override fun getCurrentUser(): Instance? =
            securityService.getCurrentUser()?.let {
                val toConvert = it
                kirraSpringInstanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Empty)
            }

    override fun getCurrentUserRoles(): List<Instance> =
        (securityService.getCurrentUserRoles() ?: emptyList()).map {
            val toConvert = it
            kirraSpringInstanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Empty)
        }

    override fun filterInstances(criteria : MutableMap<String, MutableList<Any>>?, namespace : String, name : String, profile : InstanceManagement.DataProfile?): MutableList<Instance> {
        return getInstances(namespace, name, profile);
    }

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

    override fun executeQuery(operation: Operation, externalId: String?, arguments: MutableList<*>?, pageRequest: InstanceManagement.PageRequest?): MutableList<*> {
        val (service, javaInstance: Any?, result) = doExecuteOperation(operation, externalId, arguments)
        return result
    }

    override fun executeOperation(operation: Operation, externalId: String?, arguments: MutableList<*>?): MutableList<*> {
        val (service, javaInstance: Any?, result) = doExecuteOperation(operation, externalId, arguments)

        if (javaInstance is BaseEntity) {
            service.update(javaInstance)
        }

        return result
    }

    private fun doExecuteOperation(operation: Operation, externalId: String?, arguments: MutableList<*>?): Triple<BaseService<BaseEntity, *>, Any?, MutableList<*>> {
        val kirraEntity = schemaManagement.getEntity(operation.owner)
        val entityClass: Class<BaseEntity> = kirraSpringMetamodel.getEntityClass(kirraEntity.entityNamespace, kirraEntity.name)!!
        val service = getEntityService(operation.owner)

        val entityInstance = externalId?.let { service.findById(it.toLong()) }

        val serviceImplementationFunction = AopUtils.getTargetClass(service).kotlin.functions.find { it.name == operation.name && kirraSpringMetamodel.isKirraOperation(it) }
        val serviceFunction = serviceImplementationFunction?.let { impl ->
            service::class.functions.find {
                it.name == operation.name && it.javaMethod!!.parameterTypes.toList() == impl.javaMethod!!.parameterTypes.toList()
            }
        }

        val entityFunction = entityClass.kotlin.functions.find { it.name == operation.name && kirraSpringMetamodel.isKirraOperation(it) }

        val (javaInstance, actualFunction) = if (serviceFunction != null) Pair(service, serviceFunction!!) else Pair(entityInstance, entityFunction!!)
        BusinessException.ensure(javaInstance != null, ErrorCode.UNKNOWN_OBJECT)

        val boundFunction = BoundFunction(javaInstance!!, actualFunction)
        val matchedArguments = arguments!!.mapIndexed { i, argument -> Pair(operation.parameters[i].name, kirraSpringInstanceBridge.mapKirraValueToJava(operation.parameters[i], argument)) }.toMap()
        val offset = if (operation.isInstanceOperation && serviceFunction != null) 1 else 0
        val kotlinMatchedArguments = matchedArguments.map { namedValue ->
            val index = operation.parameters.indexOfFirst { it.name == namedValue.key }
            KirraException.ensure(index >= 0, KirraException.Kind.ELEMENT_NOT_FOUND, { "Missing parameter: ${namedValue.key}" })
            val kirraParameter = operation.parameters[index]
            val parameter = boundFunction.valueParameters[index + offset]
            val convertedValue = kirraSpringInstanceBridge.mapKirraValueToJava(kirraParameter, namedValue.value)
            Pair(parameter!!, convertedValue)
        }.toMap().toMutableMap()
        if (serviceFunction != null && operation.isInstanceOperation)
            kotlinMatchedArguments[serviceFunction.valueParameters[0]] = entityInstance
        if (serviceFunction != null && operation.kind == Operation.OperationKind.Finder)
            kotlinMatchedArguments[serviceFunction.valueParameters[0]] = entityInstance

        val callResult = boundFunction.callBy(kotlinMatchedArguments)
        val asList = if (callResult == null)
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

    override fun newInstance(namespace: String, entityName: String): Instance {
        val entityClass : Class<BaseEntity>? = kirraSpringMetamodel.getEntityClass(namespace, entityName)
        KirraException.ensure(entityClass != null, KirraException.Kind.ENTITY, { "${namespace}.${entityName}"})
        val toConvert = entityClass!!.newInstance()
        return kirraSpringInstanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
    }

    override fun validateInstance(toValidate: Instance?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRelatedInstances(namespace: String, entityName: String, externalId: String, relationshipName: String, dataProfile: InstanceManagement.DataProfile?): MutableList<Instance> {
        val found = retrieveJavaInstance(namespace, entityName, externalId)
        KirraException.ensure(found != null, KirraException.Kind.OBJECT_NOT_FOUND, {null})
        val toConvert = found!!
        val relationshipKProperty = toConvert::class.memberProperties.firstOrNull { it.name == relationshipName }
        KirraException.ensure(relationshipKProperty != null, KirraException.Kind.ELEMENT_NOT_FOUND, {null})
        val relatedObjects = relationshipKProperty!!.call(toConvert) as Iterable<BaseEntity> ?: emptyList()
        return kirraSpringInstanceBridge.toInstances(relatedObjects).toMutableList()
    }

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
