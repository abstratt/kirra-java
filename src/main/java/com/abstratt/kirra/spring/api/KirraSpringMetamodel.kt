package com.abstratt.kirra.spring.api

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.spring.BaseEntity
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Member
import javax.persistence.EntityManagerFactory
import javax.persistence.metamodel.Attribute
import javax.persistence.metamodel.EntityType
import javax.persistence.metamodel.Metamodel
import javax.persistence.metamodel.SingularAttribute
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.cast
import kotlin.reflect.full.functions
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaMethod


@Component
class KirraSpringMetamodel {

    @Autowired
    lateinit private var kirraSpringApplication : KirraSpringApplication

    private val reflections by lazy {
        val entityPackageNames = kirraSpringApplication.javaPackages
        val configuration = ConfigurationBuilder.build(entityPackageNames)
        Reflections(configuration)
    }

    private val entityClasses by lazy {
        reflections.getTypesAnnotatedWith(javax.persistence.Entity::class.java).associate { Pair(it.name, it as Class<BaseEntity>) }
    }

    val entitiesByPackage by lazy {
        entityClasses.values.groupBy { it.`package`.name }
    }

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

    fun isEntityClass(clazz: Class<*>): Boolean =
        entityClasses.containsKey(clazz.name)


    fun packageNameToNamespace(packageName : String) : String = packageName.split('.').last()

    fun namespaceToPackageName(namespace : String) : String = kirraSpringApplication.javaPackages.find { it.endsWith(".${namespace}") } ?: namespace
    fun getEntityClass(namespace: String, entityName: String): Class<BaseEntity>? {
        val packageName = namespaceToPackageName(namespace)
        return entitiesByPackage[packageName]?.find { it.simpleName == entityName }
    }
    fun getEntityClass(typeRef: TypeRef) = getEntityClass(typeRef.namespace, typeRef.typeName)

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

    fun getInstanceFunctions(entityType: KClass<*>): List<KFunction<*>> {
        val instanceFunctions = entityType.functions.filter {
            isEntityOperation(it)
        }
        return instanceFunctions
    }
    private fun isEntityOperation(it: KFunction<*>) =
            !it.isImplementationMethod() && it.visibility == KVisibility.PUBLIC && it.javaMethod?.declaringClass?.kotlin?.isEntity() ?: false

    private fun isServiceOperation(it: KFunction<*>) =
            !it.isImplementationMethod() && it.visibility == KVisibility.PUBLIC && it.javaMethod?.declaringClass?.kotlin?.isService() ?: false


    fun <E> getTypeRef(javaClass: Class<E>): TypeRef =
            TypeRef(packageNameToNamespace(javaClass.`package`.name), javaClass.simpleName, TypeRef.TypeKind.Entity)
}


fun <R> KFunction<R>.isImplementationMethod(): Boolean =
        when {
            setOf("hashCode", "equals", "copy", "toString").contains(this.name) -> true
            this.name.matches(Regex("component[0-9]*")) -> true
            else -> false
        }

fun KClass<*>.isEntity(): Boolean {
    if (this.isCompanion)
        return this.java.enclosingClass.kotlin.isEntity()
    val annotations = this.annotations
    val entityAnnotation = annotations.findAnnotationByType(javax.persistence.Entity::class)
    if (entityAnnotation != null)
        return true
    val extendsEntity = this.superclasses.any { it.isEntity() }
    return extendsEntity
}

fun KClass<*>.isService(): Boolean {
    val annotations = this.annotations
    val serviceAnnotation = annotations.findAnnotationByType(Service::class)
    if (serviceAnnotation != null)
        return true
    val extendsService = this.superclasses.any { it.isService() }
    return extendsService
}

fun <T : Annotation> Iterable<T>.findAttributeAnnotationsByType(predicate: (Annotation) -> Boolean): List<T> =
        this.filter(predicate)

fun <T : Annotation> Iterable<Annotation>.findAnnotationByType(annotationClass : KClass<T>): T? =
        this.find { annotationClass.isInstance(it) } as T?

fun <T : Annotation> Iterable<Annotation>.findAnnotationsByType(annotationClass : KClass<*>): List<T> =
        this.findAttributeAnnotationsByType { annotationClass.isInstance(it) }.map { it as T }

fun Attribute<*, *>.findAttributeAnnotationsByType(predicate: (Annotation) -> Boolean): Collection<Annotation> =
        this.javaMember.getAnnotations().findAttributeAnnotationsByType(predicate)

fun <T : Annotation> Attribute<*, *>.findAttributeAnnotationsByType(type: KClass<out T>): Collection<T> =
        this.javaMember.getAnnotations().findAttributeAnnotationsByType { it: Annotation -> type.isInstance(it) }.map { type.cast(it) }

fun <T : Annotation> Attribute<*, *>.findAnnotationByType(type: KClass<out T>): T? =
        this.findAttributeAnnotationsByType(type).firstOrNull()

fun Member.getAnnotations(): Iterable<Annotation> =
        (this as AccessibleObject).annotations.asIterable()
