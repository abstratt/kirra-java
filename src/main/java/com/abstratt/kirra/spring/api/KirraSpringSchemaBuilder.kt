package com.abstratt.kirra.spring.api

import com.abstratt.kirra.*
import com.abstratt.kirra.Entity
import com.abstratt.kirra.spring.Named
import org.apache.commons.lang3.StringUtils
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.domain.EntityScanPackages
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import javax.persistence.*
import javax.persistence.metamodel.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.cast
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.jvm.kotlinProperty

@Lazy
@Component
class KirraSpringSchemaBuilder : SchemaBuilder {

    companion object {
        private val logger = LoggerFactory.getLogger(KirraSpringSchemaBuilder::class.java.name)
    }

    @Autowired
    lateinit private var kirraSpringApplication : KirraSpringApplication

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    private val reflections by lazy {
        val entityPackageNames = kirraSpringApplication.javaPackages
        val configuration = ConfigurationBuilder.build(entityPackageNames)
        Reflections(configuration)
    }

    private val entityClasses by lazy {
        reflections.getTypesAnnotatedWith(javax.persistence.Entity::class.java).associateBy { it.name }
    }

    private val entitiesByPackage by lazy {
        entityClasses.values.groupBy { it.`package`.name }
    }

    private val metamodel by lazy {
        entityManagerFactory.metamodel
    }

    private val jpaEntities by lazy {
        metamodel.entities.associateBy { it.name }
    }

    override fun build(): Schema {
        val schema = Schema()
        schema.namespaces = buildNamespaces(entitiesByPackage)
        return schema
    }

    fun buildNamespaces(packages: Map<String, List<Class<*>>>): MutableList<Namespace>? {
        return packages.map { buildNamespace(it.key, it.value) }.toMutableList()
    }

    fun buildNamespace(packageName: String, classes: List<Class<*>>): Namespace {
        val namespace = Namespace(packageName)
        namespace.entities = getEntities(namespace.name).toMutableList()
        return namespace
    }

    fun getEntities(namespaceName: String): Iterable<Entity> {
        return jpaEntities.map { buildEntity(namespaceName, it.value) }
    }

    fun buildEntity(namespaceName: String, entityType: EntityType<*>): Entity {
        val newEntity = Entity()
        setName(newEntity, entityType.javaType.kotlin, { entityType.name })
        newEntity.namespace = entityType.javaType.`package`.name
        newEntity.properties = this.getProperties(entityType)
        newEntity.relationships = this.getRelationships(entityType)
        newEntity.operations = this.getOperations(entityType)
        return newEntity
    }

    private fun getOperations(entityType: EntityType<*>): List<Operation>? {
        val allFunctions = entityType.javaType.kotlin.functions
        return allFunctions.map { buildOperation(it) }.filter { it != null }
    }

    private fun buildOperation(kotlinFunction: KFunction<*>): Operation {
        val operation = Operation()
        operation.name = kotlinFunction.name
        return operation
    }

    private fun getRelationships(entityClass: EntityType<*>): List<Relationship> {
        return entityClass.attributes.filter {
            isRelationship(it)
        }.map { this.buildRelationship(it) }
    }

    private fun getProperties(entityClass: EntityType<*>): List<Property> {
        return entityClass.attributes.filter {
            !isRelationship(it) && it is SingularAttribute && !it.isId
        }.map { this.buildProperty(it) }
    }

    private fun isRelationship(attribute: Attribute<*, *>): Boolean {
        return attribute.isAssociation || entityClasses.containsKey(attribute.javaType.name)
    }

    private fun buildProperty(attribute: Attribute<*, *>): Property {
        val property = Property()
        property.name = attribute.name
        property.typeRef = getPropertyTypeRef(attribute)
        if (property.typeRef.kind == TypeRef.TypeKind.Enumeration) {
            property.enumerationLiterals = buildPropertyEnumerationLiterals(getJavaType(attribute) as Class<Enum<*>>).associateBy { it.name }
        }
        return property
    }

    private fun buildPropertyEnumerationLiterals(javaType: Class<out Enum<*>>): List<EnumerationLiteral> {
        val enumConstants = javaType.enumConstants
        val result = enumConstants.map {
            buildEnumerationLiteral(it)
        }
        return result
    }

    private fun buildEnumerationLiteral(enumeration: Enum<*>): EnumerationLiteral {
        val literal = EnumerationLiteral()
        literal.name = enumeration.name
        return literal
    }

    private fun buildRelationship(attribute: Attribute<*, *>): Relationship {
        val relationship = Relationship()
        setName(relationship, memberToAnnotated(attribute.javaMember), { attribute.name })
        relationship.typeRef = getEntityTypeRef(attribute)
        val opposite = findOpposite(attribute)
        relationship.style = getRelationshipStyle(attribute, opposite)
        relationship.isEditable = relationship.style != Relationship.Style.PARENT && attribute.isEditable()
        relationship.isInitializable = relationship.style != Relationship.Style.PARENT && attribute.isInitializable()
        relationship.isMultiple = attribute.isMultiple()
        relationship.isRequired = relationship.style == Relationship.Style.PARENT || attribute.isRequired()
        val multiple = attribute is CollectionAttribute<*, *>
        relationship.isMultiple = multiple
        if (opposite != null) {
            relationship.opposite = opposite.name
            relationship.isOppositeReadOnly = relationship.style == Relationship.Style.CHILD || opposite.isReadOnly()
            relationship.isOppositeRequired = relationship.style == Relationship.Style.CHILD || opposite.isRequired()

        }
        return relationship
    }

    private fun memberToAnnotated(javaMember: Member?): KAnnotatedElement =
        if (javaMember is Method)
            javaMember.kotlinFunction as KAnnotatedElement
        else if (javaMember is Field)
            javaMember.kotlinProperty as KAnnotatedElement
        else
            throw IllegalArgumentException(javaMember.toString())


    private fun getRelationshipStyle(relationship: Attribute<*, *>, opposite: Attribute<*, *>?): Relationship.Style {
        val thisClass = relationship.declaringType.javaType
        //TODO-RC use mappedBy to avoid ambiguity (two relationships between the same types)
        val relationshipAnnotation = relationship.findRelationshipAnnotation()
        // find other end to figure out whether it is a parent (and hence this should be a child)
        val otherType = getJavaType(relationship)
        val otherEnd = findOpposite(relationship)
        val otherEndAnnotation = otherEnd?.findRelationshipAnnotation()

        return when (relationshipAnnotation) {
            is OneToMany -> if (relationshipAnnotation.orphanRemoval) Relationship.Style.CHILD else Relationship.Style.LINK
            is OneToOne -> if (relationshipAnnotation.orphanRemoval) Relationship.Style.CHILD else Relationship.Style.LINK
            is ManyToMany, is ManyToOne -> when (otherEndAnnotation) {
                is OneToMany -> if (otherEndAnnotation.orphanRemoval) Relationship.Style.PARENT else Relationship.Style.LINK
                is OneToOne -> if (otherEndAnnotation.orphanRemoval) Relationship.Style.PARENT else Relationship.Style.LINK
                else -> Relationship.Style.LINK
            }
            else -> Relationship.Style.LINK
        }
    }

    private fun findOpposite(relationship: Attribute<*, *>): Attribute<*, *>? {
        val thisClass = relationship.declaringType.javaType
        val otherType = getJavaType(relationship)
        val otherEntity = metamodel.entity(otherType)!!
        val relationshipAnnotation = relationship.findRelationshipAnnotation()!!
        val mappedBy = relationshipAnnotation.getMappedBy()
        if (mappedBy != null) {
            // this relationship declared a mappedBy - just find the opposite attribute by name
            return otherEntity.getAttribute(mappedBy)
        }
        val oppositeWithMatchingMappedBy = otherEntity.attributes.find { it.findRelationshipAnnotation()?.getMappedBy() == relationship.name }
        if (oppositeWithMatchingMappedBy != null) {
            return oppositeWithMatchingMappedBy
        }
        val filtered = otherEntity.attributes.filter { getJavaType(it) == thisClass && it.findRelationshipAnnotation() != null }
        if (filtered.size <= 1) {
            return filtered.firstOrNull()
        }
        return null
    }

    private fun Annotation.getMappedBy(): String? {
        return this.getValue(OneToMany::mappedBy)
    }

    private fun <C, T> Annotation.getValue(property: KProperty1<C, T>): T? {
        val annotationFunctions = this.annotationClass.java.methods
        val method = annotationFunctions.find { it.name == property.name }
        return method?.invoke(this) as T?
    }


    fun getJavaType(attribute: Attribute<*, *>): Class<*> =
            if (attribute.isCollection)
                ((attribute as CollectionAttribute<*, *>).elementType.javaType)
            else
                attribute.javaType

    private fun Attribute<*, *>.findRelationshipAnnotation(): Annotation? {
        return javaMember.findAnnotations { it is OneToMany || it is ManyToOne || it is ManyToMany || it is OneToOne }.firstOrNull()
    }

    public fun Member.findAnnotations(predicate: (Annotation) -> Boolean): Collection<Annotation> =
            (this as AccessibleObject).annotations.filter { predicate(it) }

    public fun AccessibleObject.findAnnotation(annotationClass : Class<*>): Annotation? =
            this.annotations.find { it.annotationClass == annotationClass }

    private fun Attribute<*, *>.findAnnotations(predicate: (Annotation) -> Boolean): Collection<Annotation> =
            this.javaMember.findAnnotations(predicate)

    private fun <T : Annotation> Attribute<*, *>.findAnnotations(type: KClass<out T>): Collection<T> =
            this.javaMember.findAnnotations { type.isInstance(it) }.map { type.cast(it) }

    public fun <T : Annotation> Attribute<*, *>.findAnnotation(type: KClass<out T>): T? =
            this.findAnnotations(type).firstOrNull()

    inline private fun <R, reified T : Annotation> Attribute<*,*>.getAnnotationValue(property: KProperty1<T, R>): R? =
        findAnnotation(T::class)?.getValue(property)


    private fun getEntityTypeRef(attribute: Attribute<*, *>): TypeRef? =
            when (attribute) {
                is SingularAttribute -> {
                    val type = attribute.type
                    getTypeRef(type)
                }
                is CollectionAttribute<*, *> -> {
                    getTypeRef(attribute.elementType)
                }
                else -> null
            }

    private fun getTypeRef(type: Type<out Any>, kind : TypeRef.TypeKind = TypeRef.TypeKind.Entity): TypeRef {
        return getTypeRef(type.javaType, kind)
    }

    private fun getTypeRef(javaType: Class<*>, kind : TypeRef.TypeKind = TypeRef.TypeKind.Entity): TypeRef {
        val javaPackage = javaType.`package`
        return TypeRef(javaPackage.name, javaType.simpleName, kind)
    }


    private fun getPropertyTypeRef(attribute: Attribute<*, *>): TypeRef {
        val javaType = getJavaType(attribute)
        val typeRef = when (javaType.simpleName) {
            Integer::class.simpleName, Int::class.simpleName, Long::class.simpleName, "int","long"-> TypeRef("kirra" , "Integer", TypeRef.TypeKind.Primitive)
            Float::class.simpleName, Double::class.simpleName, "double", "float" -> TypeRef("kirra" , "Double", TypeRef.TypeKind.Primitive)
            "String" -> TypeRef("kirra" , "String", TypeRef.TypeKind.Primitive)
            "Boolean" -> TypeRef("kirra" , "Boolean", TypeRef.TypeKind.Primitive)
            else -> when {
                javaType.isEnum -> getTypeRef(javaType, TypeRef.TypeKind.Enumeration)
                attribute.findAnnotation(Lob::class) != null ->  TypeRef("kirra" , "Blob", TypeRef.TypeKind.Blob)
                else -> getTypeRef(javaType, TypeRef.TypeKind.Tuple)
            }
        }
        return typeRef
    }

    private fun <X, Y> Attribute<X, Y>.isEditable(): Boolean =
            getAnnotationValue(Column::updatable) ?: true

    private fun <X, Y> Attribute<X, Y>.isInitializable(): Boolean =
            getAnnotationValue(Column::insertable) ?: true

    private fun <X, Y> Attribute<X, Y>.isReadOnly(): Boolean =
            !isInitializable() && !isEditable()

    private fun <X, Y> Attribute<X, Y>.isMultiple(): Boolean =
            this is CollectionAttribute<*,*>

    private fun <X, Y> Attribute<X, Y>.isRequired(): Boolean =
            this is SingularAttribute && !this.isOptional

    private fun setName(namedElement : NamedElement<*>, annotatedElement : KAnnotatedElement, nameProvider : () -> String) {
        val named = annotatedElement.findAnnotation<Named>()
        namedElement.description = named?.description
        namedElement.label = named?.label
        namedElement.symbol = named?.symbol
        namedElement.name = StringUtils.trimToNull(named?.name) ?: nameProvider.invoke()
    }
}

