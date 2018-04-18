package com.abstratt.kirra.spring

import com.abstratt.kirra.Operation
import com.abstratt.kirra.TypeRef
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
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*


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


    fun namespaceToPackageName(namespace : String) : String = kirraSpringApplication.javaPackages.find { it.endsWith(".${namespace}") } ?: namespace
    fun getEntityClass(namespace: String, entityName: String): Class<BaseEntity>? {
        val packageName = namespaceToPackageName(namespace)
        val found = entitiesByPackage[packageName]?.find { it.simpleName == entityName }
        return found
    }
    fun getEntityClass(typeRef: TypeRef) = getEntityClass(typeRef.namespace, typeRef.typeName)

    fun getEnumClass(typeRef: TypeRef) : Class<Enum<*>>? {
        val packageName = namespaceToPackageName(typeRef.namespace)
        val enumClassName = "${packageName}.${typeRef.typeName.replace('+', '$')}"
        try {
            val enumClass = Class.forName(enumClassName) as Class<Enum<*>>
            return enumClass
        } catch (e : ClassNotFoundException) {
            return null
        }
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

    fun getInstanceFunctions(entityType: KClass<*>): List<KFunction<*>> {
        val instanceFunctions = entityType.functions.filter {
            isKirraOperation(it)
        }
        return instanceFunctions
    }
    fun isKirraOperation(it: KFunction<*>) =
            it.visibility == KVisibility.PUBLIC
            && (it.findAnnotation<ActionOp>() != null || it.findAnnotation<QueryOp>() != null)

    fun getOperationKind(kotlinFunction: KFunction<*>, instanceOperation: Boolean): Operation.OperationKind {
/*
        if (instanceOperation) {
            return Operation.OperationKind.Action
        }
        if (kotlinFunction.annotations.findAnnotationByType(QueryOperation::class) != null) {
            return Operation.OperationKind.Finder
        }
        if (kotlinFunction.annotations.findAnnotationByType(ActionOperation::class) != null) {
            return Operation.OperationKind.Action
        }
        val transactional = kotlinFunction.annotations.findAnnotationByType(Transactional::class)
        if (transactional != null) {
            return if (transactional.readOnly == true) Operation.OperationKind.Finder else Operation.OperationKind.Action
        }
        return Operation.OperationKind.Action
*/


        if (kotlinFunction.annotations.findAnnotationByType(QueryOp::class) != null) {
            return Operation.OperationKind.Finder
        }
        if (kotlinFunction.annotations.findAnnotationByType(ActionOp::class) != null) {
            return Operation.OperationKind.Action
        }
        throw IllegalArgumentException("Not a kirra operation: ${kotlinFunction.name}")
    }

    fun isMultiple(returnType: KType): Boolean {
        val kClass = returnType.classifier as KClass<*>
        return kClass.isSubclassOf(Iterable::class) || kClass.isSubclassOf(java.lang.Iterable::class)
    }

    fun isMnemonicProperty(javaMember: KCallable<Any?>): Boolean = javaMember.findAnnotation<MnemonicProperty>() != null
    fun getAllKotlinProperties(entityAsKotlinClass: KClass<out Any>): Iterable<KProperty<*>> {
        val inherited = entityAsKotlinClass.superclasses.filter { it.isSubclassOf(BaseEntity::class) }.map { getAllKotlinProperties(it)}.flatten()
        val declared = entityAsKotlinClass.java.declaredFields.map { it.kotlinProperty }.filterNotNull()
        return inherited + declared
    }
}


fun <R> KFunction<R>.isImplementationMethod(): Boolean =
        when {
            setOf("hashCode", "equals", "copy", "toString").contains(this.name) -> true
            this.name.matches(Regex("component[0-9]*")) -> true
            this.findAnnotation<ImplementationOp>() != null -> true
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
