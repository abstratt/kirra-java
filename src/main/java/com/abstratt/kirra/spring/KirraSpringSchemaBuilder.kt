package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import com.abstratt.kirra.Entity
import com.abstratt.kirra.Parameter
import com.abstratt.kirra.statemachine.StateMachineInstance
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.lang.reflect.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.persistence.*
import javax.persistence.metamodel.*
import javax.persistence.metamodel.Type
import kotlin.reflect.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.valueParameters
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.jvm.kotlinProperty

@Component
class KirraSpringSchemaBuilder : SchemaBuilder {

    companion object {
        private val logger = LoggerFactory.getLogger(KirraSpringSchemaBuilder::class.java.name)
    }

    @Autowired
    lateinit private var kirraSpringMetamodel: KirraSpringMetamodel

    @Autowired
    lateinit private var kirraSpringApplication : KirraSpringApplication

    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext


    override fun build(): Schema {
        val schema = Schema()
        schema.namespaces = buildNamespaces(kirraSpringMetamodel.entitiesByPackage)
        return schema
    }

    fun buildNamespaces(packages: Map<String, List<Class<*>>>): MutableList<Namespace>? {
        return packages.map { buildNamespace(it.key, it.value) }.toMutableList()
    }

    fun buildNamespace(packageName: String, classes: List<Class<*>>): Namespace {
        val namespace = Namespace(kirraSpringMetamodel.packageNameToNamespace(packageName))
        val namespaceEntityTypes : List<EntityType<*>> = classes.filter{!it.kotlin.isAbstract}.map {
            kirraSpringMetamodel.getJpaEntity(it)!!
        }
        namespace.entities = buildEntities(namespace.name, namespaceEntityTypes).toMutableList()
        return namespace
    }

    fun buildEntities(namespaceName: String, entityTypes: Iterable<EntityType<*>>): Iterable<Entity> {
        return entityTypes.map { buildEntity(namespaceName, it) }
    }

    fun buildEntity(namespaceName: String, entityType: EntityType<*>): Entity {
        val newEntity = Entity()
        val entityAsJavaClass = entityType.javaType
        setName(newEntity, entityAsJavaClass.kotlin, { entityType.name })
        newEntity.namespace = namespaceName
        newEntity.properties = this.buildProperties(entityType)
        newEntity.relationships = this.buildRelationships(entityType)
        val allOperations = this.buildInstanceOperations(entityType).toMutableList()
        try {
            val serviceClass = entityAsJavaClass.classLoader.loadClass(entityAsJavaClass.name + "Service")
            val serviceBean = applicationContext.getBean(entityType.name.decapitalize() + "Service", serviceClass)
            val entityOperations = serviceClass.methods.filter { Modifier.isPublic(it.modifiers) && it.kotlinFunction != null && it.declaringClass.kotlin != BaseService::class}.map { buildEntityOperation(it.kotlinFunction!!) }
            allOperations.addAll(entityOperations)
        } catch (e : ClassNotFoundException) {
            // no service class
            logger.info("No service class for {}", newEntity.typeRef)
        }
        newEntity.operations = allOperations
        logger.info("Built entity ${newEntity.typeRef} from ${entityAsJavaClass.name}")
        return newEntity
    }

    private fun buildEntityOperation(serviceMethod: KFunction<*>): Operation {
        return buildOperation(serviceMethod,false)
    }

    private fun buildInstanceOperations(entityType: EntityType<*>): List<Operation> {
        val entityFunctions = kirraSpringMetamodel.getInstanceFunctions(entityType.javaType.kotlin)
        return entityFunctions.map { buildOperation(it as KFunction<*>, true) }
    }

    private fun buildOperation(kotlinFunction: KFunction<*>, instanceOperation : Boolean): Operation {
        val operation = Operation()
        operation.name = kotlinFunction.name
        operation.isInstanceOperation = instanceOperation
        operation.parameters = kotlinFunction.valueParameters.filter { !isInternalParameter(it) }.map { buildParameter(it) }
        operation.kind = getOperationKind(kotlinFunction, instanceOperation)
        logger.info("Built operation ${operation.name} from ${kotlinFunction}")
        return operation
    }

    private fun isInternalParameter(toCheck : KParameter) : Boolean {
        val parameterType = toCheck.type.withNullability(false)
        val isInternal = when (parameterType.classifier) {
            Pageable::class -> true
            else -> false
        }
        return isInternal
    }

    private fun getOperationKind(kotlinFunction: KFunction<*>, instanceOperation: Boolean): Operation.OperationKind {
        if (instanceOperation)
            return Operation.OperationKind.Action
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
    }

    private fun buildParameter(kotlinParameter: KParameter): Parameter {
        val parameter = Parameter()
        parameter.name = kotlinParameter.name
        parameter.direction = Parameter.Direction.In
        parameter.typeRef = getTypeRef(KotlinParameter(kotlinParameter))
        return parameter
    }

    private fun buildRelationships(entityClass: EntityType<*>): List<Relationship> {
        return kirraSpringMetamodel.getRelationships(entityClass).map { this.buildRelationship(it) }
    }

    private fun buildProperties(entityType: EntityType<*>): List<Property> {
        val javaType = entityType.javaType
        val kClass = javaType.kotlin
        val tmpObject = kClass.createInstance()
        return kirraSpringMetamodel.getAttributes(entityType).map { this.buildProperty(it, tmpObject) }
    }

    private fun buildProperty(attribute: Attribute<*, *>, tmpObject : Any): Property {
        val javaMember = attribute.javaMember
        val hasDefault = javaMember is Field && javaMember.kotlinProperty?.call(tmpObject) != null

        val property = Property()
        property.name = attribute.name
        property.typeRef = getTypeRef(KotlinProperty(attribute, (if (javaMember is Field) (javaMember.kotlinProperty!!) else (javaMember as Method).kotlinFunction!!)))
        if (property.typeRef.kind == TypeRef.TypeKind.Enumeration) {
            property.enumerationLiterals = buildPropertyEnumerationLiterals(attribute, getJavaType(attribute) as Class<Enum<*>>).associateBy { it.name }
        }
        property.isMultiple = attribute.isMultiple()
        property.isHasDefault = hasDefault
        property.isInitializable = attribute.isInitializable()
        property.isEditable = attribute.isEditable()
        property.isRequired = attribute.isRequired()
        property.isDerived = !property.isInitializable && !property.isEditable
        property.isAutoGenerated = !property.isInitializable && !property.isEditable
        property.isUnique = attribute.findAnnotationByType(Column::class)?.unique ?: false
        property.isUserVisible = isUserVisible(attribute)
        logger.info("Built property ${property.name} from ${javaMember}")
        return property
    }

    private fun buildPropertyEnumerationLiterals(attribute : Attribute<*, *>, javaType: Class<*>): List<EnumerationLiteral> {
        val enumConstants : Array<out Enum<*>>? = if (javaType.isEnum)
            javaType.enumConstants as Array<out Enum<*>>
        else
            if (StateMachineInstance::class.java.isAssignableFrom(javaType))
                getStateMachineTokenClass(attribute).enumConstants  as Array<out Enum<*>>
            else
                null as? Array<out Enum<*>>?

        val result = enumConstants?.map {
            buildEnumerationLiteral(it)
        }
        return result ?: emptyList()
    }

    private fun getStateMachineTokenClass(attribute: Attribute<*, *>) =
            ((attribute.javaType.genericSuperclass) as ParameterizedType).actualTypeArguments[0] as Class<*>

    private fun buildEnumerationLiteral(enumeration: Enum<*>): EnumerationLiteral {
        val literal = EnumerationLiteral()
        literal.name = enumeration.name
        return literal
    }

    private fun buildRelationship(attribute: Attribute<*, *>): Relationship {
        val relationship = Relationship()
        val javaMember = attribute.javaMember
        setName(relationship, memberToAnnotated(javaMember), { attribute.name })
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
        relationship.isHasDefault = false
        relationship.isInitializable = attribute.isInitializable()
        relationship.isEditable = attribute.isEditable()
        relationship.isDerived = !relationship.isInitializable && !relationship.isEditable
        relationship.isUserVisible = isUserVisible(attribute)
        logger.info("Built relationship ${relationship.name} from ${javaMember}")
        return relationship
    }

    private fun isUserVisible(attribute: Attribute<*, *>) : Boolean {
        val javaMember = attribute.javaMember
        val member = if (javaMember is Field) javaMember.kotlinProperty?.getter?.javaMethod else javaMember
        return member?.let { Modifier.isPublic(it.modifiers) }
                ?: false
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
        val otherEntity = kirraSpringMetamodel.getJpaEntity(otherType)!!
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
        return StringUtils.trimToNull(this.getValue(OneToMany::mappedBy))
    }

    private fun <C, T> Annotation.getValue(property: KProperty1<C, T>): T? {
        val annotationFunctions = this.annotationClass.java.methods
        val method = annotationFunctions.find { it.name == property.name }
        return method?.invoke(this) as T?
    }

    fun getJavaType(attribute: Attribute<*, *>): Class<*> =
            if (attribute.isCollection)
                ((attribute as CollectionAttribute<*, *>).elementType.javaType)
            else if (attribute.persistentAttributeType == Attribute.PersistentAttributeType.EMBEDDED && StateMachineInstance::class.java.isAssignableFrom(attribute.javaType))
                getStateMachineTokenClass(attribute)
            else
                attribute.javaType

    private fun Attribute<*, *>.findRelationshipAnnotation(): Annotation? =
        javaMember.getAnnotations().findAttributeAnnotationsByType { it: Annotation -> it is OneToMany || it is ManyToOne || it is ManyToMany || it is OneToOne }.firstOrNull()


    inline private fun <R, reified T : Annotation> Attribute<*,*>.getAnnotationValue(property: KProperty1<T, R>): R? =
        findAnnotationByType(T::class)?.getValue(property)


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
        val simpleName = if (javaType.isMemberClass) (javaType.enclosingClass.simpleName + "+" + javaType.simpleName) else javaType.simpleName
        return TypeRef(kirraSpringMetamodel.packageNameToNamespace(javaPackage.name), simpleName, kind)
    }

    abstract class KotlinDataElement<T : KAnnotatedElement>(val element : T) {
        abstract fun getJavaType() : Class<*>
        fun getAnnotations() : Iterable<Annotation> = element.annotations
    }

    inner class KotlinParameter(parameter : KParameter) : KotlinDataElement<KParameter>(parameter) {
        override fun getJavaType(): Class<*> {
            val classifier = element.type.classifier
            return (classifier as KClass<*>).javaObjectType
        }
    }

    inner class KotlinProperty(val attribute : Attribute<*,*>, property : KAnnotatedElement) : KotlinDataElement<KAnnotatedElement>(property) {
        override fun getJavaType(): Class<*> = getJavaType(attribute)
    }

    private fun getTypeRef(dataElement: KotlinDataElement<*>): TypeRef {
        val javaType = dataElement.getJavaType().kotlin
        val typeName = javaType.qualifiedName
        val typeRef = when (typeName) {
            Integer::class.qualifiedName,
            Int::class.qualifiedName,
            Long::class.qualifiedName,
            "int",
            "long"
            -> TypeRef("kirra" , "Integer", TypeRef.TypeKind.Primitive)
            Float::class.qualifiedName,
            Double::class.qualifiedName,
            "double",
            "float"
            -> TypeRef("kirra" , "Double", TypeRef.TypeKind.Primitive)
            String::class.qualifiedName -> TypeRef("kirra" , "String", TypeRef.TypeKind.Primitive)
            Boolean::class.qualifiedName -> TypeRef("kirra" , "Boolean", TypeRef.TypeKind.Primitive)
            LocalDate::class.qualifiedName -> TypeRef("kirra" , "Date", TypeRef.TypeKind.Primitive)
            LocalDateTime::class.qualifiedName -> TypeRef("kirra" , "DateTime", TypeRef.TypeKind.Primitive)
            LocalTime::class.qualifiedName -> TypeRef("kirra" , "Time", TypeRef.TypeKind.Primitive)
            else -> when {
                javaType.java.isEnum() -> getTypeRef(javaType.java, TypeRef.TypeKind.Enumeration)
                dataElement.getAnnotations().findAnnotationByType(Lob::class) != null ->  TypeRef("kirra" , "Blob", TypeRef.TypeKind.Blob)
                javaType.isEntity() -> getTypeRef(javaType.java, TypeRef.TypeKind.Entity)
                else -> getTypeRef(javaType.java, TypeRef.TypeKind.Tuple)
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
        val named = annotatedElement.annotations.findAnnotationByType(Named::class)
        namedElement.description = named?.description
        namedElement.label = named?.label
        namedElement.symbol = named?.symbol
        namedElement.name = StringUtils.trimToNull(named?.name) ?: nameProvider.invoke()
    }

}

