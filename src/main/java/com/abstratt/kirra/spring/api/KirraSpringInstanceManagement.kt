package com.abstratt.kirra.spring.api

import com.abstratt.kirra.*
import com.abstratt.kirra.spring.BaseEntity
import com.abstratt.kirra.spring.BaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import sun.plugin.liveconnect.SecurityContextHelper
import kotlin.reflect.KMutableProperty
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

    override fun createInstance(instance: Instance): Instance {
        val asEntity : BaseEntity = fromInstance(instance)
        val asService : BaseService<BaseEntity,*> = getEntityService(instance.typeRef)
        val created = asService.create(asEntity)
        val createdInstance = toInstance(created)
        return createdInstance
    }

    override fun getInstance(namespace: String, entityName: String, externalId: String, dataProfile: InstanceManagement.DataProfile?): Instance {
        val entityClass : Class<BaseEntity>? = kirraSpringMetamodel.getEntityClass(namespace, entityName)
        val asService : BaseService<BaseEntity,*> = getEntityService(TypeRef(namespace, entityName, TypeRef.TypeKind.Entity))
        val found = asService.findById(externalId.toLong())
        KirraException.ensure(found != null, KirraException.Kind.OBJECT_NOT_FOUND, {null})
        return toInstance(found!!)
    }

    override fun updateInstance(instance: Instance): Instance {
        val asEntity : BaseEntity = fromInstance(instance)
        asEntity.id = instance.objectId.toLong()
        val asService : BaseService<BaseEntity,*> = getEntityService(instance.typeRef)
        val updatedEntity = asService.update(asEntity)
        KirraException.ensure(updatedEntity != null, KirraException.Kind.OBJECT_NOT_FOUND, {null})
        val updatedInstance = toInstance(updatedEntity!!)
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
        return null
    }

    override fun getCurrentUserRoles(): MutableList<Instance> {
        return emptyList<Instance>().toMutableList()
    }

    override fun filterInstances(criteria : MutableMap<String, MutableList<Any>>?, namespace : String, name : String, profile : InstanceManagement.DataProfile?): MutableList<Instance> {
        return getInstances(namespace, name, profile);
    }

    override fun getInstances(namespace: String, entityName: String, dataProfile: InstanceManagement.DataProfile?): MutableList<Instance> {
        val entityClass : Class<BaseEntity>? = kirraSpringMetamodel.getEntityClass(namespace, entityName)
        val asService : BaseService<BaseEntity,*> = getEntityService(TypeRef(namespace, entityName, TypeRef.TypeKind.Entity))
        val listed = asService.list()
        return listed.content.map { toInstance(it) }.toMutableList()
    }

    //public List<Instance> filterInstances(Map<String, List<Object>> criteria, String namespace, String name, DataProfile profile) {}

    override fun isRestricted(): Boolean {
        return false
    }

    override fun executeOperation(operation: Operation, externalId: String?, arguments: MutableList<*>?): MutableList<*> {
        val kirraEntity = schemaManagement.getEntity(operation.owner)
        val entityClass : Class<BaseEntity> = kirraSpringMetamodel.getEntityClass(kirraEntity.entityNamespace, kirraEntity.name)!!
        if (externalId != null) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        val service = getEntityService(operation.owner)
        val serviceClass = entityClass.classLoader.loadClass(entityClass.name + "Service").kotlin
        val serviceOperation = serviceClass.functions.find { it.name == operation.name }!!
        val matchedArguments = arguments!!.mapIndexed { i, argument -> Pair(operation.parameters[i].name, mapArgumentToJava(operation.parameters[i], argument))}.toMap()
        val kotlinMatchedArguments = matchedArguments.map {
            val parameter = serviceOperation.valueParameters.find { p -> p.name == it.key }
            KirraException.ensure(parameter != null, KirraException.Kind.ELEMENT_NOT_FOUND, { "Missing parameter: ${it.key}" })
            Pair(parameter!!, it.value)
        }.toMap()
        val allArgs = mapOf(Pair(serviceOperation.instanceParameter!!, service)) + kotlinMatchedArguments
        val result = serviceOperation.callBy(allArgs)
        return if (result == null)
            emptyList<Any>().toMutableList()
        else if (result is Iterable<*>)
            result.toMutableList()
        else
            listOf(result).toMutableList()
    }

    private fun mapArgumentToJava(parameter: Parameter, argument: Any?): Any? {
        if (parameter.typeRef.kind == TypeRef.TypeKind.Entity) {
            val entityClass = kirraSpringMetamodel.getEntityClass(parameter.typeRef)
            if (entityClass != null && (argument is Instance || argument is InstanceRef)) {
                val jpaInstance = entityClass.newInstance()
                val objectId : String? = when (argument) {
                    is Instance -> argument.objectId
                    is InstanceRef -> argument.objectId
                    else -> null
                }
                jpaInstance.id = objectId?.toLong()
                return jpaInstance
            }
        }
        return argument
    }

    override fun getParameterDomain(entity: Entity?, externalId: String?, action: Operation?, parameter: Parameter?): MutableList<Instance> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun newInstance(namespace: String, entityName: String): Instance {
        val entityClass : Class<BaseEntity>? = kirraSpringMetamodel.getEntityClass(namespace, entityName)
        KirraException.ensure(entityClass != null, KirraException.Kind.ENTITY, { "${namespace}.${entityName}"})
        return toInstance(entityClass!!.newInstance())
    }

    private fun <E : BaseEntity> fromInstance(newInstance: Instance) : E {
        val kirraEntity = schemaManagement.getEntity(newInstance.typeRef)
        val javaClass : Class<E> = kirraSpringMetamodel.getEntityClass(newInstance.entityNamespace, newInstance.entityName)!! as Class<E>
        val entityType = kirraSpringMetamodel.getJpaEntity(javaClass)!!
        val asEntity = javaClass.newInstance() as E
        newInstance.values.forEach { propertyName : String, propertyValue : Any? ->
            val entityProperty = javaClass.kotlin.memberProperties.find { it.name == propertyName }
            if (entityProperty is KMutableProperty<*>) {
                entityProperty.setter.call(asEntity, propertyValue)
            }
        }
        return asEntity
    }

    private fun <E : BaseEntity> toInstance(newInstance: E) : Instance {
        // convert Java class instance to Kirra instance
        val entityType = kirraSpringMetamodel.getJpaEntity(newInstance.javaClass)!!
        val attributes = kirraSpringMetamodel.getAttributes(entityType)

        val properties = newInstance::class.memberProperties.associateBy { it.name }
        val instance =  Instance(kirraSpringMetamodel.getTypeRef(newInstance.javaClass), null)
        attributes.forEach {
            val ktProperty = properties[it.name]!!
            val propertyValue = ktProperty.call(newInstance)
            instance.setValue(it.name, propertyValue)
        }
        instance.objectId = newInstance.id?.let { it.toString() }
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