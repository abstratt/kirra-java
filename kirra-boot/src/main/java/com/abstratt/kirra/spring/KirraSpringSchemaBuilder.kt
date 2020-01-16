package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import com.abstratt.kirra.Entity
import com.abstratt.kirra.Parameter
import com.abstratt.kirra.spring.user.RoleEntity
import com.abstratt.kirra.spring.userprofile.UserProfile
import com.abstratt.kirra.statemachine.StateMachineInstance
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.lang.reflect.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import javax.persistence.*
import javax.persistence.metamodel.*
import javax.persistence.metamodel.Type
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*
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
        val delayedTasks : MutableList<(Map<TypeRef, Entity>) -> Unit> = LinkedList()
        val namespaces = buildNamespaces(kirraSpringMetamodel.entitiesByPackage, delayedTasks)
        val allEntities: Map<TypeRef, Entity> = namespaces.map { it.entities }.flatten().associateBy { it.typeRef }
        delayedTasks.forEach { task -> task(allEntities) }
        schema.namespaces = namespaces
        schema.applicationName = kirraSpringApplication.name
        schema.applicationLabel = getLabel(schema.applicationName)
        return schema
    }

    fun buildNamespaces(packages: Map<String, List<Class<*>>>, delayedTasks: MutableList<(Map<TypeRef, Entity>) -> Unit>): List<Namespace> {
        return packages.map { buildNamespace(it.key, it.value, delayedTasks) }
    }

    fun buildNamespace(packageName: String, classes: List<Class<*>>, delayedTasks: MutableList<(Map<TypeRef, Entity>) -> Unit>): Namespace {
        val namespace = Namespace(packageNameToNamespace(packageName))
        val namespaceEntityTypes : List<EntityType<*>> = classes.filter{!it.kotlin.isAbstract}.map {
            kirraSpringMetamodel.getJpaEntity(it)!!
        }
        namespace.entities = buildEntities(namespace.name, namespaceEntityTypes, delayedTasks).toMutableList()
        return namespace
    }

    fun buildEntities(namespaceName: String, entityTypes: Iterable<EntityType<*>>, delayedTasks: MutableList<(Map<TypeRef, Entity>) -> Unit>): Iterable<Entity> {
        val entities = entityTypes.map { buildEntity(namespaceName, it) }

        delayedTasks.add({ entitiesByTypeRef -> addInvertedRelationships(entitiesByTypeRef) })

        return entities
    }

    private fun addInvertedRelationships(entities: Map<TypeRef, Entity>) {
        val invertedRelationships = entities.map {
            val serviceClass = kirraSpringMetamodel.getEntityServiceClass<BaseService<*,*>>(it.key)
            serviceClass.functions.filter {
                kirraSpringMetamodel.isRelationshipAccessor(it)
            }
        }.flatten().map { buildInvertedRelationship(it) }.groupBy { it.owner }

        invertedRelationships.forEach {
            val entity = entities[it.key]
            // will be null if other entity not registered by the Kirra app
            if (entity != null)
                entity!!.relationships = entity!!.relationships + it.value
        }
    }

    fun buildEntity(namespaceName: String, entityType: EntityType<*>): Entity {
        val newEntity = Entity()
        val entityAsJavaClass = entityType.javaType as Class<BaseEntity>
        val entityAsKotlinClass = entityAsJavaClass.kotlin
        val tmpObject = entityAsKotlinClass.createInstance()

        setName(newEntity, ClassNameProvider(entityAsKotlinClass))
        newEntity.namespace = namespaceName
        val storedProperties = this.buildStoredProperties(entityType, tmpObject)
        val derivedProperties = entityAsKotlinClass.memberProperties.filter { it.javaField == null && !(it is KMutableProperty<*>) }.map { buildDerivedProperty(it, tmpObject) }
        val allProperties = storedProperties + derivedProperties
        newEntity.properties = allProperties

        val storedRelationships = this.buildStoredRelationships(entityType)
        val derivedRelationships = entityAsKotlinClass.memberProperties.filter { kirraSpringMetamodel.isEntityClass(it.returnType.javaClass) && it.javaField == null && !(it is KMutableProperty<*>) }.map { buildDerivedRelationship(it) }
        val allOperations = this.buildInstanceOperations(entityType).toMutableList()
        try {
            val serviceClass = entityAsJavaClass.classLoader.loadClass(entityAsJavaClass.name + "Service")
            val serviceBean = applicationContext.getBean(entityType.name.decapitalize() + "Service", serviceClass)
            val serviceFunctions = serviceClass.methods.filter {
                it.kotlinFunction != null && (
                    kirraSpringMetamodel.isKirraOperation(it.kotlinFunction!!)
                )
            }.map { it.kotlinFunction!! }
            val serviceOperations = serviceFunctions.filter { kirraSpringMetamodel.isKirraOperation(it) }
            val serviceFunctionsAreInstanceOrEntity = serviceOperations.groupBy {
                it.valueParameters.firstOrNull()?.type?.classifier == entityAsKotlinClass
            }

            val entityOperations = serviceFunctionsAreInstanceOrEntity[false]
                    ?.map { buildOperation(it,false) } ?: emptyList()
            // service-defined instance actions
            val additionalInstanceOperations = serviceFunctionsAreInstanceOrEntity[true]
                    ?.map { buildOperation(it,true, { p -> p.index > 1 }) } ?: emptyList()
            allOperations.addAll(additionalInstanceOperations)
            allOperations.addAll(entityOperations)
        } catch (e : ClassNotFoundException) {
            // no service class
            logger.info("No service class for {}", newEntity.typeRef)
        }
        val allRelationships = storedRelationships + derivedRelationships
        newEntity.relationships = allRelationships
        newEntity.operations = allOperations
        newEntity.isConcrete = !entityAsKotlinClass.isAbstract
        newEntity.isTopLevel = newEntity.isConcrete || newEntity.operations.any { it.kind == Operation.OperationKind.Finder }
        newEntity.isStandalone = true
        newEntity.isUserVisible = true
        newEntity.isRole = entityAsKotlinClass.isSubclassOf(UserProfile::class)
        newEntity.isRole = entityAsKotlinClass.isSubclassOf(RoleEntity::class)
        newEntity.isInstantiable = newEntity.isConcrete && !newEntity.properties.any { it.isRequired && !it.isInitializable } && !newEntity.relationships.any { it.isRequired && !it.isInitializable }
        val fieldBackedProperties = kirraSpringMetamodel.getAllKotlinProperties(entityAsKotlinClass)
        newEntity.orderedDataElements = fieldBackedProperties
                .map {it.name}
                .filter {
                    (newEntity.getProperty(it) ?: newEntity.getRelationship(it))?.let {
                        it.isUserVisible && !it.isMultiple
                    } ?: false
                }
        newEntity.mnemonicSlot = newEntity.properties.firstOrNull { it.isMnemonic }?.name ?:
            newEntity.orderedDataElements.firstOrNull {
                (newEntity.getProperty(it) ?: newEntity.getRelationship(it)?.takeIf { !it.isMultiple })?.isUserVisible ?: false
            }
        if (newEntity.mnemonicSlot != null) {
            val mnemonicElement : DataElement = newEntity.getProperty(newEntity.mnemonicSlot) ?: newEntity.getRelationship(newEntity.mnemonicSlot)
            if (mnemonicElement != null) {
                mnemonicElement.isMnemonic = true
                newEntity.orderedDataElements.remove(newEntity.mnemonicSlot)
                newEntity.orderedDataElements.add(0, newEntity.mnemonicSlot)
            }
        }
        logger.info("Built entity ${newEntity.typeRef} from ${entityAsJavaClass.name}")
        return newEntity
    }

    private fun buildInvertedRelationship(relationshipAccessor: KFunction<*>): Relationship {
        val relationship = Relationship()
        val accessorAnnotation = relationshipAccessor.findAnnotation<RelationshipAccessor>()!!
        setName(relationship, CallableNameProvider(relationshipAccessor))
        val relationshipType = relationshipAccessor.returnType
        relationship.typeRef = getTypeRef(relationshipType)
        val oppositeName = relationshipAccessor.valueParameters[0].name!!
        // set the owner so inverted relationships can be placed in the entity under which they are supposed to be defined
        relationship.owner = getTypeRef(relationshipAccessor.valueParameters[0].type)
        relationship.style = accessorAnnotation.style
        relationship.isEditable = relationship.style != Relationship.Style.PARENT
        relationship.isInitializable = relationship.style != Relationship.Style.PARENT
        relationship.isMultiple = (relationshipType.classifier as KClass<*>).isSubclassOf(Iterable::class)
        relationship.isRequired = false
        if (oppositeName != null) {
            relationship.opposite = oppositeName
            relationship.isOppositeReadOnly = true
            relationship.isOppositeRequired = true
        }
        relationship.isHasDefault = false
        relationship.isInitializable = false
        relationship.isEditable = false
        relationship.isDerived = accessorAnnotation.derived
        relationship.isUserVisible = isUserVisible(relationshipAccessor.javaMethod)
        logger.info("Built relationship ${relationship.name} from ${relationshipAccessor}")
        return relationship
    }

    private fun buildDerivedRelationship(ktProperty: KProperty<*>): Relationship {
        TODO("buildDerivedRelationship not implemented yet" )
    }

    private fun buildInstanceOperations(entityType: EntityType<*>): List<Operation> {
        val entityFunctions = kirraSpringMetamodel.getInstanceFunctions(entityType.javaType.kotlin)
        return entityFunctions.map { buildOperation(it as KFunction<*>,true) }
    }

    private fun buildOperation(kotlinFunction: KFunction<*>, instanceOperation : Boolean, parameterFilter : (p : KParameter) -> Boolean = { true }): Operation {
        val operation = Operation()
        setName(operation, CallableNameProvider(kotlinFunction))
        operation.isInstanceOperation = instanceOperation
        operation.parameters = kotlinFunction.valueParameters.filter { !isInternalParameter(it) && parameterFilter.invoke(it) }.map { buildParameter(it) }
        operation.kind = getOperationKind(kotlinFunction, instanceOperation)
        operation.typeRef = kotlinFunction.takeUnless { it.returnType.classifier == Unit::class}?.let { getTypeRef(KotlinFunction(it))}

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
        return kirraSpringMetamodel.getOperationKind(kotlinFunction, instanceOperation)
    }

    private fun buildParameter(kotlinParameter: KParameter): Parameter {
        val parameter = Parameter()
        setName(parameter, ParameterNameProvider(kotlinParameter))
        parameter.typeRef = getTypeRef(KotlinParameter(kotlinParameter))
        parameter.direction = Parameter.Direction.In
        parameter.isUserVisible = true
        parameter.inAllSets = true
        return parameter
    }

    private fun buildStoredRelationships(entityClass: EntityType<*>): List<Relationship> {
        return kirraSpringMetamodel.getRelationships(entityClass).map { this.buildRelationship(it) }
    }

    private fun buildStoredProperties(entityType: EntityType<*>, tmpObject: Any): List<Property> {
        return kirraSpringMetamodel.getAttributes(entityType).map { this.buildStoredProperty(it, tmpObject) }
    }

    private fun buildStoredProperty(attribute: Attribute<*, *>, tmpObject : Any): Property {
        val javaMember = attribute.javaMember
        val kotlinElement = (if (javaMember is Field) javaMember.kotlinProperty else if (javaMember is Method) javaMember.kotlinFunction else null) as KCallable
        val hasDefault = javaMember is Field && javaMember.kotlinProperty?.visibility == KVisibility.PUBLIC && javaMember.kotlinProperty?.call(tmpObject) != null

        val property = Property()
        setName(property, CallableNameProvider(kotlinElement))
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
        property.isMnemonic = kirraSpringMetamodel.isMnemonicProperty(kotlinElement)
        logger.info("Built property ${property.name} from ${javaMember}")
        return property
    }

    private fun buildDerivedProperty(kProperty : KProperty<*>, tmpObject : Any): Property {
        val javaMember = kProperty.javaGetter
        val kotlinElement = kProperty
        val property = Property()
        setName(property, CallableNameProvider(kotlinElement))
        property.typeRef = getTypeRef(KotlinDerivedProperty(kProperty))
        property.isMultiple = kirraSpringMetamodel.isMultiple(kProperty.returnType)
        property.isHasDefault = false
        property.isInitializable = false
        property.isEditable = false
        property.isRequired = false
        property.isDerived = true
        property.isAutoGenerated = true
        property.isUnique = false
        property.isUserVisible = isUserVisible(kProperty.javaGetter)
        property.isMnemonic = kirraSpringMetamodel.isMnemonicProperty(kotlinElement)
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
        setName(literal, EnumNameProvider(enumeration))
        return literal
    }

    private fun buildRelationship(attribute: Attribute<*, *>): Relationship {
        val relationship = Relationship()
        val javaMember = attribute.javaMember
        setName(relationship, CallableNameProvider(memberToAnnotated(javaMember) as KCallable<*>))
        relationship.typeRef = getEntityTypeRef(attribute)
        val opposite = findOpposite(attribute)
        relationship.style = getRelationshipStyle(attribute, opposite)
        relationship.isEditable = relationship.style != Relationship.Style.PARENT && attribute.isEditable()
        relationship.isInitializable = relationship.style != Relationship.Style.PARENT && attribute.isInitializable()
        relationship.isMultiple = attribute.isMultiple()
        relationship.isRequired = relationship.style == Relationship.Style.PARENT || attribute.isRequired()
        relationship.isMultiple = attribute.isMultiple()
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
        return isUserVisible(javaMember)
    }

    private fun isUserVisible(javaMember: Member?): Boolean {
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

    private fun getTypeRef(type: Type<out Any>, kind : TypeRef.TypeKind = TypeRef.TypeKind.Entity): TypeRef =
        getTypeRef(type.javaType, kind)

    private fun getTypeRef(type : KType, kind : TypeRef.TypeKind = TypeRef.TypeKind.Entity): TypeRef =
        if ((type.classifier as KClass<*>).isSubclassOf(Iterable::class))
            getTypeRef(type.arguments[0].type!!)
        else
            getTypeRef((type.classifier as KClass<*>).java)


    private fun getTypeRef(javaType: Class<*>, kind : TypeRef.TypeKind = TypeRef.TypeKind.Entity): TypeRef {
        if (Iterable::class.isSuperclassOf(javaType.kotlin)) {
            val actualClass = (javaType.kotlin.typeParameters.first().upperBounds.first().classifier as KClass<*>).java
            return getTypeRef(actualClass)
        }
        val javaPackage = javaType.`package`
        val simpleName = if (javaType.isMemberClass) (javaType.enclosingClass.simpleName + "+" + javaType.simpleName) else javaType.simpleName
        return TypeRef(packageNameToNamespace(javaPackage.name), simpleName, kind)
    }

    abstract class KotlinDataElement<T : KAnnotatedElement>(val element : T) {
        abstract fun getJavaType() : Class<*>
        fun getAnnotations() : Iterable<Annotation> = element.annotations
    }

    open inner class KotlinFunction(function : KFunction<*>) : KotlinDataElement<KFunction<*>>(function) {
        override fun getJavaType(): Class<*> {
            val returnType = element.returnType.classifier as KClass<*>
            return if (Iterable::class.java.isAssignableFrom(returnType.java)) (element.returnType.arguments[0].type?.classifier as KClass<*>).java else returnType.javaObjectType
        }
    }

    inner class KotlinParameter(parameter : KParameter) : KotlinDataElement<KParameter>(parameter) {
        override fun getJavaType(): Class<*> = (element.type.classifier as KClass<*>).javaObjectType
    }

    inner class KotlinProperty(val attribute : Attribute<*,*>, property : KAnnotatedElement) : KotlinDataElement<KAnnotatedElement>(property) {
        override fun getJavaType(): Class<*> = getJavaType(attribute)
    }

    inner class KotlinDerivedProperty(property : KProperty<*>) : KotlinFunction(property.getter!!)

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

    private fun setName(namedElement : NamedElement<*>, nameProvider : NameProvider) {
        namedElement.name = nameProvider.getName()
        namedElement.description = nameProvider.getDescription()
        namedElement.label = nameProvider.getLabel()
        namedElement.symbol = nameProvider.getSymbol()
    }

    interface NameProvider {
        fun getName() : String
        fun getLabel() : String = getLabel(getName())
        fun getSymbol() : String = getName()
        fun getDescription() : String? = null
    }

    abstract class AnnotationBasedNameProvider<AE : KAnnotatedElement> (val annotatedElement: AE) : NameProvider {
        abstract fun getElementName() : String

        override fun getName(): String {
            val named = annotatedElement.annotations.findAnnotationByType(Named::class)
            return StringUtils.trimToNull(named?.name) ?: getElementName()
        }

        override fun getLabel(): String {
            val named = annotatedElement.annotations.findAnnotationByType(Named::class)
            return named?.let { StringUtils.trimToNull(it.label) } ?: super.getLabel()
        }
        override fun getSymbol(): String {
            val named = annotatedElement.annotations.findAnnotationByType(Named::class)
            return StringUtils.trimToNull(named?.symbol) ?: super.getLabel()
        }
        override fun getDescription(): String? {
            val named = annotatedElement.annotations.findAnnotationByType(Named::class)
            return named?.let { StringUtils.trimToNull(it.description) } ?: super.getDescription()
        }
    }

    class ClassNameProvider(element: KClass<*>) : AnnotationBasedNameProvider<KClass<*>>(element) {
        override fun getElementName(): String =
            annotatedElement.simpleName!!
    }

    class CallableNameProvider(element: KCallable<*>) : AnnotationBasedNameProvider<KCallable<*>>(element) {
        override fun getElementName(): String =
                annotatedElement.name!!
    }

    class ParameterNameProvider(element: KParameter) : AnnotationBasedNameProvider<KParameter>(element) {
        override fun getElementName(): String =
                annotatedElement.name!!
    }


    class EnumNameProvider(val enum : Enum<*>) : NameProvider {
        override fun getName(): String =
            enum.name
    }
}

