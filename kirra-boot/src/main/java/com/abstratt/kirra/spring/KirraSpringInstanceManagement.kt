package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import com.abstratt.kirra.pojo.*
import com.abstratt.kirra.spring.api.SecurityService
import com.abstratt.kirra.spring.boot.KirraSpringMetamodel
import org.springframework.aop.support.AopUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.lang.reflect.Method
import javax.persistence.EntityManager
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.isSubclassOf

@Service
@Transactional
open class KirraSpringInstanceManagement (
        metamodel: KirraSpringMetamodel,
        instanceBridge: KirraInstanceBridge,
        schemaManagement: SchemaManagement,
        private val securityService: SecurityService,
        private val entityManager : EntityManager
) : KirraPojoInstanceManagement(metamodel, instanceBridge, schemaManagement) {
    val kirraSpringMetamodel = metamodel

    @Secured
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    override fun createInstance(instance: Instance) =
        super.createInstance(instance)

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getInstance(namespace: String, entityName: String, externalId: String, dataProfile: InstanceManagement.DataProfile?) =
        super.getInstance(namespace, entityName, externalId, dataProfile)

    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    override fun updateInstance(instance: Instance) = super.updateInstance(instance)

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getEnabledEntityActions(entity: Entity?) = super.getEnabledEntityActions(entity)

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getRelationshipDomain(entity: Entity, objectId: String, relationship: Relationship) = super.getRelationshipDomain(entity, objectId, relationship)

    override fun saveContext() {
        entityManager.flush()
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


    override fun getNativeUserRoles() = securityService.getCurrentUserRoles() ?: emptyList()

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getCurrentUser(): Instance? = super.getCurrentUser()

    override fun getNativeCurrentUser() = securityService.getCurrentUser()

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getInstances(namespace: String, entityName: String, dataProfile: InstanceManagement.DataProfile?) = super.getInstances(namespace, entityName, dataProfile)

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun executeQuery(operation: Operation, externalId: String?, arguments: MutableList<*>?, pageRequest: InstanceManagement.PageRequest?): MutableList<*> {
        val pageRequest = pageRequest?.let {
            PageRequest.of(it.first?.toInt() ?: 0, it.maximum ?: 100)
        }
        var customImplArguments : Map<String, Any?> = mapOf(Pair("pageRequest", pageRequest))

        val result = doExecuteOperation(operation, externalId, arguments, customImplArguments)
        return result.second
    }

    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    override fun executeOperation(operation: Operation, externalId: String?, arguments: MutableList<*>?): MutableList<*> {
        val service = getEntityService(operation.owner)

        val entityInstance = externalId?.let { service.findById(it.toLong()) }

        val (javaInstance, result) = doExecuteOperation(operation, externalId, arguments)

        if (javaInstance is IBaseEntity) {
            service.update(javaInstance)
        }

        return result
    }

    data class MatchedOperation(
        val javaInstance : Any, val matchedModelArguments :Map<String, Any?>
    )

    private fun doExecuteOperation(operation: Operation, externalId: String?, arguments: MutableList<*>?, customImplArguments : Map<String, Any?> = emptyMap()): Pair<Any, MutableList<*>> {
        val (entityInstance, matchedModelArguments, pair) = matchOperation(operation, externalId, arguments, customImplArguments)

        val (javaInstance, actualMethod) = pair

        val isServiceMethod = javaInstance is BaseService<*,*>
        BusinessException.ensure(javaInstance != null, ErrorCode.UNKNOWN_OBJECT)

        val boundMethod = actualMethod

        val offset = if (operation.isInstanceOperation && isServiceMethod) 1 else 0
        var actualParameterIndex = -1
        val kotlinMatchedModelArguments = matchedModelArguments.map { namedValue ->
            val index = operation.parameters.indexOfFirst { it.name == namedValue.key }
            KirraException.ensure(index >= 0, KirraException.Kind.ELEMENT_NOT_FOUND, { "Missing parameter: ${namedValue.key}" })
            val kirraParameter = operation.parameters[index]
            val ktParameter = boundMethod.parameters[offset + ++actualParameterIndex]
            val ktValue = namedValue.value
            Pair(ktParameter!!, ktValue)
        }

        val kotlinMatchedImplArguments = customImplArguments.map { namedImplArgument ->
            Pair(boundMethod.parameters[++actualParameterIndex], namedImplArgument.value)
        }.filterNotNull()

        val kotlinMatchedArguments = (kotlinMatchedModelArguments + kotlinMatchedImplArguments).toMutableList()

        if (isServiceMethod) {
            // a service may implement an instance operation - in that case, the entity instance is the first parameter
            if (operation.isInstanceOperation) {
                // first parameter holds the target instance, by convention
                val serviceParameter = actualMethod.parameters[0]
                kotlinMatchedArguments.add(0, Pair(serviceParameter, entityInstance))
            }
        }
        val callResult = boundMethod.invoke(javaInstance, *(kotlinMatchedArguments.map { it.second }.toTypedArray()))

        val asList : MutableList<*> = if (callResult == null || callResult is Unit)
            emptyList<Any>().toMutableList()
        else if (callResult is Iterable<*>)
            callResult.toMutableList()
        else
            listOf(callResult).toMutableList()
        val result = asList.map { if (it is IBaseEntity) {
            val toConvert = it
            instanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
        } else it }.toMutableList()

        return Pair(javaInstance!!, result)
    }

    protected fun matchOperation(operation: Operation, externalId: String?, arguments: MutableList<*>?, customImplArguments: Map<String, Any?>): Triple<IBaseEntity?, Map<String, Any?>, Pair<Any?, Method>> {
        val kirraEntity = schemaManagement.getEntity(operation.owner)
        val entityClass: Class<IBaseEntity> = kirraMetamodel.getEntityClass(kirraEntity.entityNamespace, kirraEntity.name)!!
        val service = getEntityService(operation.owner)

        val entityInstance = externalId?.let { service.findById(it.toLong()) }

        val matchedModelArguments = matchArguments(arguments, operation)
        val matchedArguments = matchedModelArguments + customImplArguments

        // it will be null if the service does not have a function matching the Kirra operation name
        val serviceImplClass = AopUtils.getTargetClass(service)

        val serviceImplementationMethod = findMatchingMethod(serviceImplClass.kotlin, operation.name, matchedArguments)
        // we need the proxy function though
        val serviceMethod = serviceImplementationMethod?.let { impl ->
            service::class.java.methods.find {
                // same name and parameter types
                it.name == operation.name && it.parameterTypes.toList() == impl!!.parameterTypes.toList()
            }
        }

        val entityMethod = findMatchingMethod(entityClass.kotlin, operation.name, matchedModelArguments)

        KirraException.ensure(entityMethod != null || serviceImplementationMethod != null, KirraException.Kind.ELEMENT_NOT_FOUND, { "No operation named ${operation.name}" })

        val pair = if (entityMethod == null)
            Pair(service, serviceMethod!!)
        else
            Pair(entityInstance, entityMethod!!)
        return Triple(entityInstance, matchedModelArguments, pair)
    }

    protected fun findMatchingMethod(kClass: KClass<out Any>, operationName: String?, matchedArguments: Map<String, Any?>): Method? {
        val matchingName = kClass.java.methods.filter { it.name == operationName }
        return matchingName.find { canInvokeWith(it, matchedArguments) }
    }

    /**
     * Can invoke a function if all required parameters have matching arguments, and
     * all arguments are expected (by name and type).
     */
    protected fun canInvokeWith(toCall: Method, matchedArguments: Map<String, Any?>): Boolean {
        val requiredParameters = toCall.parameters
        if (matchedArguments.size != requiredParameters.size) {
            return false
        }
        matchedArguments.toList().forEachIndexed { index, argument ->
            if (!requiredParameters[index].type.kotlin.isInstance(argument.second)) {
                return false
            }
        }
        return true
    }

    protected fun matchArguments(arguments: MutableList<*>?, operation: Operation): Map<String, Any?> {
        return arguments!!
                .mapIndexed { i, argument ->
                    Pair(
                            operation.parameters[i].name,
                            instanceBridge
                                    .mapKirraValueToJava(
                                            operation.parameters[i],
                                            argument
                                    )
                    )
                }.filter { it.second != null }.toMap()
    }

    override fun getParameterDomain(entity: Entity, externalId: String?, action: Operation, parameter: Parameter): MutableList<Instance> {
        val accessorPair = kirraMetamodel.findDomainAccessor(action, parameter)
        if (accessorPair != null) {
            val (accessorClass, accessorMethod) = accessorPair
            val entityDefinedAccessor = accessorClass.isSubclassOf(IBaseEntity::class)
            val found = action.isInstanceOperation.ifTrue {
                retrieveJavaInstance(entity.typeRef, externalId!!)
            }
            val pageRequest : PageRequest? = defaultPageRequest()
            val entityService = getEntityService(entity.typeRef)
            val domain =
                    when {
                        entityDefinedAccessor -> fromNativePage(accessorMethod.call(found, pageRequest))
                        action.isInstanceOperation -> fromNativePage(accessorMethod.call(entityService, found, pageRequest))
                        else -> fromNativePage(accessorMethod.call(entityService, pageRequest))
                    }
            return instanceBridge.toInstances(domain.instances).toMutableList()
        }
        return getInstances(parameter.typeRef.entityNamespace, parameter.typeRef.typeName, InstanceManagement.DataProfile.Slim)
    }

    fun fromNativePage(nativePage : Any) : IInstancePage<IBaseEntity> =
            InstancePage<IBaseEntity>(nativePage as Page<IBaseEntity>)

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun newInstance(namespace: String, entityName: String): Instance {
        val entityClass : Class<IBaseEntity>? = kirraMetamodel.getEntityClass(namespace, entityName)
        KirraException.ensure(entityClass != null, KirraException.Kind.ENTITY, { "${namespace}.${entityName}"})
        val toConvert = entityClass!!.newInstance()
        return instanceBridge.toInstance(toConvert, InstanceManagement.DataProfile.Full)
    }

    override fun validateInstance(toValidate: Instance?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    override fun getRelatedInstances(namespace: String, entityName: String, externalId: String, relationshipName: String, dataProfile: InstanceManagement.DataProfile?): MutableList<Instance> {
        val entityRef = TypeRef(namespace, entityName, TypeRef.TypeKind.Entity)
        val entity = schemaManagement.getEntity(entityRef)
        val entityClass = kirraMetamodel.getEntityClass(entityRef)
        val found = retrieveJavaInstance(namespace, entityName, externalId)
        KirraException.ensure(found != null, KirraException.Kind.OBJECT_NOT_FOUND, {null})
        val relationshipKProperty = found!!::class.memberProperties.firstOrNull { it.name == relationshipName }
        val entityService = getEntityService(entityRef)
        var relatedObjects : Iterable<IBaseEntity>? = null
        if (relationshipKProperty != null) {
            relatedObjects = entityService.getRelated(externalId.toLong(), relationshipKProperty as KProperty1<IBaseEntity, IBaseEntity>)
        } else {
            val relationshipAccessor = kirraMetamodel.getRelationshipAccessor(entity.getRelationship(relationshipName))
            KirraException.ensure(relationshipAccessor != null, KirraException.Kind.ELEMENT_NOT_FOUND, null)
            relatedObjects = relationshipAccessor!!.call(entityService, found) as? Iterable<IBaseEntity>
        }
        return instanceBridge.toInstances(relatedObjects!!).toMutableList()
    }
}


