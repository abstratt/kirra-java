package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import com.abstratt.kirra.statemachine.StateMachineInstance
import com.abstratt.kirra.statemachine.StateToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.reflect.KMutableProperty
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
            properties.forEach {
                val kirraProperty = kirraEntity.getProperty(it.name)
                if (kirraProperty != null) {
                    val propertyValue = it.call(toConvert)
                    val javaValue = mapJavaValueToKirra(kirraProperty, propertyValue)
                    instance.setValue(it.name, javaValue)
                } else if (dataProfile == InstanceManagement.DataProfile.Full) {
                    val kirraRelationship = kirraEntity.getRelationship(it.name)
                    if (kirraRelationship != null && !kirraRelationship.isMultiple) {
                        val link = it.call(toConvert) as BaseEntity?
                        if (link != null)
                            instance.setSingleRelated(it.name, toInstance(link, InstanceManagement.DataProfile.Slim))
                    }
                }
            }
        }
        return instance
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