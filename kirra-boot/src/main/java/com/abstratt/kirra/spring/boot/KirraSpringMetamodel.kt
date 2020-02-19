package com.abstratt.kirra.spring.boot

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.pojo.*
import com.abstratt.kirra.spring.GenericService
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import javax.persistence.Entity
import javax.persistence.EntityManagerFactory
import javax.persistence.metamodel.Attribute
import javax.persistence.metamodel.EntityType
import javax.persistence.metamodel.Metamodel
import javax.persistence.metamodel.SingularAttribute
import kotlin.reflect.*
import kotlin.reflect.full.*

@Component
class KirraSpringMetamodel : KirraMetamodel() {

    @Autowired
    lateinit var applicationContext: ConfigurableApplicationContext

    @Autowired
    lateinit private var kirraSpringApplication : KirraPojoApplication

    override val kirraApplication: KirraPojoApplication
        get() = kirraSpringApplication

    override fun <BS : IBaseService<*>> getDefaultServiceClass(): KClass<BS> = GenericService::class as KClass<BS>

    override fun findMemberFunctions(instance: IBaseService<*>): Iterable<KFunction<*>> =
            AopUtils.getTargetClass(instance).kotlin.memberFunctions

    override fun getEntityService(typeRef: TypeRef) : IBaseService<IBaseEntity>? {
        val serviceName = typeRef.typeName.decapitalize() + "Service"
        try {
            return applicationContext.getBean(serviceName, IBaseService::class.java) as IBaseService<IBaseEntity>
        } catch (e : NoSuchBeanDefinitionException) {
            return null
        }
    }

    override fun findEntityClasses(): Set<Class<*>> = reflections.getTypesAnnotatedWith(Entity::class.java)

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    private val jpaMetamodel : Metamodel by lazy {
        entityManagerFactory.metamodel
    }

    fun <T> getJpaEntity(javaType : Class<T>) : EntityType<T>? =
        jpaMetamodel.entity(javaType)

    val jpaEntities : Map<String, EntityType<*>> by lazy {
        jpaMetamodel.entities.associateBy { it.name }
    }

    fun <T> getAttributes(entityClass : EntityType<T>) =
        entityClass.attributes.filter {
            !isRelationship(it) && it is SingularAttribute && !it.isId
        }

    fun isRelationship(attribute: Attribute<*, *>) =
        attribute.isAssociation || isEntityClass(attribute.javaType)

    fun <T> getRelationships(entityClass : EntityType<T>) =
        entityClass.attributes.filter {
            isRelationship(it)
        }

    override fun isEntity(javaType: KClass<out Any>): Boolean =
            javaType.hasAnnotation(Entity::class)

//    override fun introspectEntityClass(typeRef: TypeRef): EntityClassIntrospector {
//        val entityClass = getEntityClass(typeRef)!!
//        val entityType = getJpaEntity(entityClass)!!
//        val relationships = getRelationships(entityType)
//        val attributes = getAttributes(entityType)
//        return EntityClassIntrospector(relationships = relationships, properties = attributes)
//    }


    protected override fun KFunction<*>.isPageReturning() : Boolean {
        return returnType.isSubtypeOf(Page::class.starProjectedType)
    }
}


fun Attribute<*, *>.findAttributeAnnotationsByType(predicate: (Annotation) -> Boolean): Collection<Annotation> =
        this.javaMember.getAnnotations().findAttributeAnnotationsByType(predicate)

fun <T : Annotation> Attribute<*, *>.findAttributeAnnotationsByType(type: KClass<out T>): Collection<T> =
        this.javaMember.getAnnotations().findAttributeAnnotationsByType { it: Annotation -> type.isInstance(it) }.map { type.cast(it) }

fun <T : Annotation> Attribute<*, *>.findAnnotationByType(type: KClass<out T>): T? =
        this.findAttributeAnnotationsByType(type).firstOrNull()

