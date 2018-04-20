package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import com.abstratt.kirra.statemachine.StateMachineInstance
import com.abstratt.kirra.statemachine.StateToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

@Component
class KirraSpringInstanceBridge {

    @Autowired
    private lateinit var schemaManagement: SchemaManagement

    @Autowired
    private lateinit var kirraSpringMetamodel: KirraSpringMetamodel

    fun toInstances(elements: Iterable<BaseEntity>): Iterable<Instance> {
        return elements.map {
            val toConvert = it
            toInstance(toConvert, InstanceManagement.DataProfile.Full)
        }.toMutableList()
    }

    /**
     * Converts a JPA instance to a Kirra instance.
     */
    fun <E : BaseEntity> toInstance(toConvert : E, dataProfile : InstanceManagement.DataProfile = InstanceManagement.DataProfile.Slim) : Instance {
        // convert Java class instance to Kirra instance
        val entityTypeRef = getTypeRef(toConvert::class.java)
        val kirraEntity = schemaManagement.getEntity(entityTypeRef)
        val properties = toConvert::class.memberProperties
        val instance =  Instance(getTypeRef(toConvert.javaClass), toConvert.id?.let { it.toString() })

        if (dataProfile != InstanceManagement.DataProfile.Empty) {
            properties.forEach { ktProperty ->
                val propertyRead = collectPropertyValue(ktProperty, kirraEntity, toConvert, { value -> instance.setValue(ktProperty.name, value) })
                if (!propertyRead && dataProfile == InstanceManagement.DataProfile.Full) {
                    collectRelationshipValue(ktProperty, kirraEntity, toConvert, { value -> instance.setSingleRelated(ktProperty.name, toInstance(value, InstanceManagement.DataProfile.Slim))})
                }
            }
        }
        kirraEntity.mnemonicSlot
        instance.shorthand = extractShorthand(toConvert, kirraEntity)
        return instance
    }

    private fun <E> collectRelationshipValue(ktProperty: KProperty1<out E, Any?>, kirraEntity: Entity, toConvert: E, collector: (BaseEntity) -> Unit): Boolean {
        val kirraRelationship = kirraEntity.getRelationship(ktProperty.name)
        if (kirraRelationship != null && !kirraRelationship.isMultiple) {
            val link = ktProperty.call(toConvert) as BaseEntity?
            if (link != null)
                collector.invoke(link)
            return true
        }
        return false
    }

    private fun <E : BaseEntity> collectPropertyValue(ktPropertyName: KProperty1<out E, Any?>, kirraEntity: Entity, toConvert: E, collector : (Any?)-> Unit): Boolean {
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

    fun extractShorthand(javaInstance : BaseEntity, kirraEntity : Entity) : String {
        var shorthand : String? = null
        val ktProperty = javaInstance::class.memberProperties.firstOrNull { it.name == kirraEntity.mnemonicSlot } ?: javaInstance::class.memberProperties.first()
        if (ktProperty != null) {
            val propertyRead = collectPropertyValue(ktProperty, kirraEntity, javaInstance, { value -> shorthand = value?.toString() })
            if (!propertyRead) {
                collectRelationshipValue(ktProperty, kirraEntity, javaInstance, { value -> shorthand = toInstance(value, InstanceManagement.DataProfile.Slim)?.shorthand })
            }
        }
        return shorthand ?: javaInstance::class.simpleName + "@"  + javaInstance.id
    }

    fun <E : BaseEntity?> getJavaInstance(newInstance: Instance?) : E? {
        if (newInstance == null) {
            return null
        }
        val javaClass = kirraSpringMetamodel.getEntityClass(newInstance.entityNamespace, newInstance.entityName) as? Class<E>
        KirraException.ensure(javaClass != null, KirraException.Kind.INTERNAL, { "No entity class found for ${newInstance.typeRef}" })
        val javaInstance = javaClass!!.newInstance()!!
        if (!newInstance.isNew) {
            javaInstance.id = newInstance.objectId.toLong()
        }
        return javaInstance
    }


    /**
     * Converts a Kirra Instance to a persistent JPA entity instance.
     */
    public fun <E : BaseEntity> fromInstance(newInstance: Instance) : E {
        val kirraEntity = schemaManagement.getEntity(newInstance.typeRef)
        val entityClass = kirraSpringMetamodel.getEntityClass(newInstance.typeRef)!!
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

    fun mapJavaValueToKirra(element: TypedElement<*>, javaValue: Any?) : Any? {
        if (element.typeRef.kind == TypeRef.TypeKind.Entity) {
            val entityClass = kirraSpringMetamodel.getEntityClass(element.typeRef)
            if (entityClass != null && javaValue is BaseEntity) {
                return InstanceRef(element.typeRef.entityNamespace, element.typeRef.typeName, javaValue.id.toString())
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
        } else if (element.typeRef.kind == TypeRef.TypeKind.Enumeration) {
            val enumClass = kirraSpringMetamodel.getEnumClass(element.typeRef)
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

}