package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import com.abstratt.kirra.spring.user.ApplicationUserService
import com.abstratt.kirra.spring.user.RoleService
import javafx.application.Application
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters

@Component
class KirraSpringInstanceManagement : InstanceManagement {
    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var kirraSpringApplication : KirraSpringApplication

    @Autowired
    private lateinit var kirraSpringMetamodel: KirraSpringMetamodel

    @Autowired
    private lateinit var schemaManagement: SchemaManagement

    @Autowired
    private lateinit var applicationUserService : ApplicationUserService

    @Autowired
    private lateinit var roleService: RoleService

    @Secured
    override fun createInstance(instance: Instance): Instance {
        val asEntity : BaseEntity = fromInstance(instance)
        val asService : BaseService<BaseEntity,*> = getEntityService(instance.typeRef)
        val created = asService.create(asEntity)
        val createdInstance = created.toInstance()
        return createdInstance
    }

    override fun getInstance(namespace: String, entityName: String, externalId: String, dataProfile: InstanceManagement.DataProfile?): Instance {
        val entityClass : Class<BaseEntity>? = kirraSpringMetamodel.getEntityClass(namespace, entityName)
        val asService : BaseService<BaseEntity,*> = getEntityService(TypeRef(namespace, entityName, TypeRef.TypeKind.Entity))
        val found = asService.findById(externalId.toLong())
        KirraException.ensure(found != null, KirraException.Kind.OBJECT_NOT_FOUND, {null})
        return found!!.toInstance()
    }

    override fun updateInstance(instance: Instance): Instance {
        val asEntity : BaseEntity = fromInstance(instance)
        asEntity.id = instance.objectId.toLong()
        val asService : BaseService<BaseEntity,*> = getEntityService(instance.typeRef)
        val updatedEntity = asService.update(asEntity)
        KirraException.ensure(updatedEntity != null, KirraException.Kind.OBJECT_NOT_FOUND, {null})
        val updatedInstance = updatedEntity!!.toInstance()
        return updatedInstance
    }


    private fun getEntityService(typeRef: TypeRef) =
            applicationContext.getBean(typeRef.typeName.decapitalize() + "Service", BaseService::class.java) as BaseService<BaseEntity, *>

    override fun linkInstances(relationship: Relationship?, sourceId: String?, destinationId: InstanceRef?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun zap() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEnabledEntityActions(entity: Entity?): MutableList<Operation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRelationshipDomain(entity: Entity?, objectId: String?, relationship: Relationship?): MutableList<Instance> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveContext() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCurrentUser(): Instance? {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? String
        return principal?.let { applicationUserService.findUserByUsername(it)?.toInstance() }
    }

    override fun getCurrentUserRoles(): List<Instance> {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? String
        if (principal == null) {
            return emptyList<Instance>().toMutableList()
        }
        val currentUser = applicationUserService.findUserByUsername(principal)
        if (currentUser == null) {
            return emptyList<Instance>()
        }
        val roles = roleService.findRoleObjects(currentUser)
        return roles.map { it.toInstance() }
    }

    override fun filterInstances(criteria : MutableMap<String, MutableList<Any>>?, namespace : String, name : String, profile : InstanceManagement.DataProfile?): MutableList<Instance> {
        return getInstances(namespace, name, profile);
    }

    override fun getInstances(namespace: String, entityName: String, dataProfile: InstanceManagement.DataProfile?): MutableList<Instance> {
        val entityClass : Class<BaseEntity>? = kirraSpringMetamodel.getEntityClass(namespace, entityName)
        val asService : BaseService<BaseEntity,*> = getEntityService(TypeRef(namespace, entityName, TypeRef.TypeKind.Entity))
        val listed = asService.list()
        return listed.content.map { it.toInstance() }.toMutableList()
    }

    //public List<Instance> filterInstances(Map<String, List<Object>> criteria, String namespace, String name, DataProfile profile) {}

    override fun isRestricted(): Boolean {
        return false
    }

    override fun executeOperation(operation: Operation, externalId: String?, arguments: MutableList<*>?): MutableList<*> {
        val kirraEntity = schemaManagement.getEntity(operation.owner)
        val entityClass : Class<BaseEntity> = kirraSpringMetamodel.getEntityClass(kirraEntity.entityNamespace, kirraEntity.name)!!
        val service = getEntityService(operation.owner)

        if (externalId != null) {
            val targetJavaInstance = service.findById(externalId.toLong())
            //val entityInstanceOperation = BoundFunction(targetJavaInstance, entityClass.kotlin.functions.find { it.name == operation.name }!!)
        }


        val functionDefiningClass = if (externalId != null) entityClass.kotlin else entityClass.classLoader.loadClass(entityClass.name + "Service").kotlin
        val javaInstance: Any? = if (externalId != null) service.findById(externalId.toLong()) else service

        BusinessException.ensure(javaInstance != null, ErrorCode.UNKNOWN_OBJECT)

        val function = BoundFunction(javaInstance!!, functionDefiningClass.functions.find { it.name == operation.name }!!)
        val matchedArguments = arguments!!.mapIndexed { i, argument -> Pair(operation.parameters[i].name, mapKirraValueToJava(operation.parameters[i], argument))}.toMap()
        val kotlinMatchedArguments = matchedArguments.map {
            val parameter = function.valueParameters.find { p -> p.name == it.key }
            KirraException.ensure(parameter != null, KirraException.Kind.ELEMENT_NOT_FOUND, { "Missing parameter: ${it.key}" })
            Pair(parameter!!, it.value)
        }.toMap()
        val result = function.callBy(kotlinMatchedArguments)

        if (javaInstance is BaseEntity) {
            service.update(javaInstance)
        }
        return if (result == null)
            emptyList<Any>().toMutableList()
        else if (result is Iterable<*>)
            result.toMutableList()
        else
            listOf(result).toMutableList()
    }

    private fun mapJavaValueToKirra(element: TypedElement<*>, javaValue: Any?) : Any? {
        if (element.typeRef.kind == TypeRef.TypeKind.Entity) {
            val entityClass = kirraSpringMetamodel.getEntityClass(element.typeRef)
            if (entityClass != null && javaValue is BaseEntity) {
                return InstanceRef(element.typeRef.entityNamespace, element.typeRef.typeName, javaValue.id.toString())
            }
            return null
        }
        if (element.typeRef.kind == TypeRef.TypeKind.Enumeration) {
            if (javaValue is StateMachine<*,*,*>) {
                return javaValue.state?.name
            }
            if (javaValue is Enum<*>) {
                return javaValue.name
            }
            return null
        }
        return javaValue
    }


    private fun mapKirraValueToJava(element: TypedElement<*>, kirraValue: Any?) : Any? {
        if (element.typeRef.kind == TypeRef.TypeKind.Entity) {
            val entityClass = kirraSpringMetamodel.getEntityClass(element.typeRef)
            if (entityClass != null && (kirraValue is Instance || kirraValue is InstanceRef)) {
                val jpaInstance = entityClass.newInstance()
                val objectId : String? = when (kirraValue) {
                    is Instance -> kirraValue.objectId
                    is InstanceRef -> kirraValue.objectId
                    else -> null
                }
                jpaInstance.id = objectId?.toLong()
                return jpaInstance
            }
        }
        return kirraValue
    }

    override fun getParameterDomain(entity: Entity?, externalId: String?, action: Operation?, parameter: Parameter?): MutableList<Instance> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun newInstance(namespace: String, entityName: String): Instance {
        val entityClass : Class<BaseEntity>? = kirraSpringMetamodel.getEntityClass(namespace, entityName)
        KirraException.ensure(entityClass != null, KirraException.Kind.ENTITY, { "${namespace}.${entityName}"})
        return entityClass!!.newInstance().toInstance()
    }

    private fun <E : BaseEntity> fromInstance(newInstance: Instance) : E {
        val kirraEntity = schemaManagement.getEntity(newInstance.typeRef)
        val entityClass = kirraSpringMetamodel.getEntityClass(newInstance.typeRef)!!
        val entityType = kirraSpringMetamodel.getJpaEntity(entityClass)!!
        val asEntity : E = getJavaInstance(newInstance)!!
        newInstance.values.forEach { propertyName : String, propertyValue : Any? ->
            val kirraProperty = kirraEntity.getProperty(propertyName)
            val entityProperty = entityClass.kotlin.memberProperties.find { it.name == propertyName }
            if (entityProperty is KMutableProperty<*>) {
                val javaValue = mapKirraValueToJava(kirraProperty, propertyValue)
                entityProperty.setter.call(asEntity, javaValue)
            }
        }
        newInstance.links.forEach { propertyName : String, instanceRef : Instance? ->
            val entityProperty = entityClass.kotlin.memberProperties.find { it.name == propertyName }
            if (entityProperty is KMutableProperty<*>) {
                val relatedInstance : BaseEntity? = getJavaInstance(instanceRef)
                entityProperty.setter.call(asEntity, relatedInstance)
            }
        }
        return asEntity
    }

    private fun <E : BaseEntity> getJavaInstance(newInstance: Instance?) : E? {
        if (newInstance == null) {
            return null
        }
        val javaClass = kirraSpringMetamodel.getEntityClass(newInstance.entityNamespace, newInstance.entityName)!! as Class<E>
        val javaInstance = javaClass.newInstance()
        if (!newInstance.isNew) {
            javaInstance.id = newInstance.objectId.toLong()
        }
        return javaInstance
    }

    private fun <E : BaseEntity> E.toInstance() : Instance {
        val toConvert = this
        // convert Java class instance to Kirra instance
        val entityTypeRef = kirraSpringMetamodel.getTypeRef(toConvert::class.java)
        val kirraEntity = schemaManagement.getEntity(entityTypeRef)

        val entityType = kirraSpringMetamodel.getJpaEntity(toConvert.javaClass)!!
        val attributes = kirraSpringMetamodel.getAttributes(entityType)

        val properties = toConvert::class.memberProperties.associateBy { it.name }
        val instance =  Instance(kirraSpringMetamodel.getTypeRef(toConvert.javaClass), null)
        attributes.forEach {
            val ktProperty = properties[it.name]!!
            val kirraProperty = kirraEntity.getProperty(it.name)
            val propertyValue = ktProperty.call(toConvert)
            val javaValue = mapJavaValueToKirra(kirraProperty, propertyValue)
            instance.setValue(it.name, javaValue)
        }
        instance.objectId = toConvert.id?.let { it.toString() }
        instance.typeRef = entityTypeRef
        return instance
    }

    override fun validateInstance(toValidate: Instance?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRelatedInstances(namespace: String?, name: String?, externalId: String?, relationship: String?, dataProfile: InstanceManagement.DataProfile?): MutableList<Instance> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unlinkInstances(relationship: Relationship?, sourceId: String?, destinationId: InstanceRef?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteInstance(instance: Instance?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteInstance(namespace: String?, name: String?, id: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}

class BoundFunction<R>(val instance : Any, val baseFunction : KFunction<R>) : KFunction<R> by baseFunction {
    override fun call(vararg args: Any?): R =
            baseFunction.call(*(listOf(instance) + args.asList()).toTypedArray())

    override fun callBy(args: Map<KParameter, Any?>): R =
            baseFunction.callBy(mapOf(Pair(baseFunction.instanceParameter!!, instance)) + args)

    override val parameters: List<KParameter>
        get() = listOf(baseFunction.instanceParameter!!) + baseFunction.parameters
}
