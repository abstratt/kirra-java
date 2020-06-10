package com.abstratt.kirra.pojo

import com.abstratt.kirra.*
import com.abstratt.kirra.pojo.IBaseEntity
import com.abstratt.kirra.pojo.KirraMetamodel
import com.abstratt.kirra.pojo.getTypeRef
import com.abstratt.kirra.statemachine.StateToken
import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Responsible for converting values between Kirra Java application and the Kirra API.
 *
 * @see InstanceManagement
 * @see Instance
 */
abstract class KirraInstanceBridge {

    abstract val schemaManagement: SchemaManagement

    abstract val kirraMetamodel: KirraMetamodel

    fun toInstances(elements: Iterable<IBaseEntity>): Iterable<Instance> {
        return elements.map {
            val toConvert = it
            toInstance(toConvert, InstanceManagement.DataProfile.Full)
        }.toMutableList()
    }

    /**
     * Converts a JPA instance to a Kirra instance.
     */
    fun <E : IBaseEntity> toInstance(toConvert : E, dataProfile : InstanceManagement.DataProfile = InstanceManagement.DataProfile.Slim) : Instance {
        // convert Java class instance to Kirra instance
        val entityTypeRef = getTypeRef(toConvert::class.java)
        val entityService = kirraMetamodel.getEntityService(entityTypeRef)
        val kirraEntity = schemaManagement.getEntity(entityTypeRef)
        val properties = toConvert::class.memberProperties
        val instance =  Instance(getTypeRef(toConvert.javaClass), toConvert.instanceId?.let { it.toString() })

        if (dataProfile != InstanceManagement.DataProfile.Empty) {
            properties.forEach { ktProperty ->
                val propertyRead = collectPropertyValue(ktProperty, kirraEntity, toConvert, { value -> instance.setValue(ktProperty.name, value) })
                if (!propertyRead && dataProfile == InstanceManagement.DataProfile.Full) {
                    collectRelationshipValue(ktProperty, kirraEntity, toConvert, { value -> instance.setSingleRelated(ktProperty.name, toInstance(value, InstanceManagement.DataProfile.Slim))})
                }
            }
            if (dataProfile == InstanceManagement.DataProfile.Full) {
                val relationships = kirraEntity.relationships
                        .filter { !it.isMultiple }
                        .associate { Pair(it, kirraMetamodel.getRelationshipAccessor(it)) }
                        .filter { it.value != null } as Map<Relationship, KCallable<*>>

                relationships.forEach { relationship, accessor ->
                    collectRelationshipValue(accessor, kirraEntity, toConvert, { value ->
                        instance.setSingleRelated(accessor.name, toInstance(value, InstanceManagement.DataProfile.Slim))
                    })
                }
            }
        }
        kirraEntity.mnemonicSlot
        instance.shorthand = extractShorthand(toConvert, kirraEntity)
        return instance
    }

    private fun <E> collectRelationshipValue(callable: KCallable<out E>, kirraEntity: Entity, toConvert: E, collector: (IBaseEntity) -> Unit): Boolean {
        val kirraRelationship = kirraEntity.getRelationship(callable.name)
        if (kirraRelationship != null && !kirraRelationship.isMultiple) {
            val link = callable.call(toConvert) as IBaseEntity?
            if (link != null)
                collector.invoke(link)
            return true
        }
        return false
    }

    private fun <E : IBaseEntity> collectPropertyValue(ktPropertyName: KProperty1<out E, Any?>, kirraEntity: Entity, toConvert: E, collector : (Any?)-> Unit): Boolean {
        val slotName = ktPropertyName.name
        val kirraProperty = kirraEntity.getProperty(slotName)
        if (kirraProperty != null) {
            val propertyValue = ktPropertyName.call(toConvert)
            val javaValue = mapJavaValueToKirra(kirraProperty, propertyValue)
            collector.invoke(javaValue)
            return true
        }
        return false
    }

    fun extractShorthand(javaInstance : IBaseEntity, kirraEntity : Entity) : String {
        var shorthand : String? = null
        val ktProperty = javaInstance::class.memberProperties.firstOrNull { it.name == kirraEntity.mnemonicSlot } ?: javaInstance::class.memberProperties.first()
        val propertyRead = collectPropertyValue(ktProperty, kirraEntity, javaInstance, { value -> shorthand = value?.toString() })
        if (!propertyRead) {
            collectRelationshipValue(ktProperty, kirraEntity, javaInstance, { value -> shorthand = toInstance(value, InstanceManagement.DataProfile.Slim)?.shorthand })
        }
        return shorthand ?: javaInstance::class.simpleName + "@"  + javaInstance.instanceId
    }

    fun <E : IBaseEntity?> getJavaInstance(newInstance: Instance?) : E? {
        if (newInstance == null) {
            return null
        }
        val typeRef = newInstance.typeRef
        val javaClass = kirraMetamodel.getEntityClass(typeRef) as? Class<E>
        KirraException.ensure(javaClass != null, KirraException.Kind.INTERNAL, { "No entity class found for ${newInstance.typeRef}" })
        val javaInstance = javaClass!!.newInstance()!!
        if (!newInstance.isNew) {
            javaInstance.assignInstanceId(newInstance.objectId.toLong())
        }
        return javaInstance
    }


    /**
     * Converts a Kirra Instance to a persistent JPA entity instance.
     */
    public fun <E : IBaseEntity> fromInstance(newInstance: Instance) : E {
        val kirraEntity = schemaManagement.getEntity(newInstance.typeRef)
        val entityClass = kirraMetamodel.getEntityClass(newInstance.typeRef)!!
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
                val relatedInstance : IBaseEntity? = getJavaInstance(instanceRef)
                entityProperty.setter.call(asEntity, relatedInstance)
            }
        }
        return asEntity
    }

    fun mapJavaValueToKirra(element: TypedElement<*>, javaValue: Any?) : Any? {
        if (element.typeRef.kind == TypeRef.TypeKind.Entity) {
            val entityClass = kirraMetamodel.getEntityClass(element.typeRef)
            if (entityClass != null && javaValue is IBaseEntity) {
                return InstanceRef(element.typeRef.entityNamespace, element.typeRef.typeName, javaValue.instanceId.toString())
            }
            return null
        }
        if (element.typeRef.kind == TypeRef.TypeKind.Enumeration) {
            if (javaValue is StateToken) {
                return javaValue.name
            }
            if (javaValue is Enum<*>) {
                return javaValue.name
            }
            return null
        }
        return javaValue
    }


    fun mapKirraValueToJava(element: TypedElement<*>, kirraValue: Any?) : Any? {
        if (element.typeRef.kind == TypeRef.TypeKind.Entity) {
            val entityClass = kirraMetamodel.getEntityClass(element.typeRef)
            if (entityClass != null && (kirraValue is Instance || kirraValue is InstanceRef)) {
                val jpaInstance = entityClass.newInstance()
                val objectId : String? = when (kirraValue) {
                    is Instance -> kirraValue.objectId
                    is InstanceRef -> kirraValue.objectId
                    else -> null
                }
                jpaInstance.assignInstanceId(objectId?.toLong())
                return jpaInstance
            }
        } else if (element.typeRef.kind == TypeRef.TypeKind.Enumeration) {
            val enumClass = kirraMetamodel.getEnumClass(element.typeRef)
            if (enumClass != null) {
                val result = enumClass.enumConstants.find { it.name == kirraValue }
                return result
            }
        } else if (element.typeRef.kind == TypeRef.TypeKind.Primitive) {
            if (kirraValue is String) {
                return when (element.typeRef.typeName) {
                    "Integer" -> kirraValue.toLong()
                    "Double" -> kirraValue.toDouble()
                    "Boolean" -> kirraValue.toBoolean()
                    "String" -> kirraValue
                    else -> kirraValue
                }
            }
        }

        return kirraValue
    }

    fun toExternalId(id: Long?): String? = id?.toString()

}