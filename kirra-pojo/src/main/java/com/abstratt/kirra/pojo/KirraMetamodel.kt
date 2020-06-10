package com.abstratt.kirra.pojo

import com.abstratt.kirra.Operation
import com.abstratt.kirra.Parameter
import com.abstratt.kirra.Relationship
import com.abstratt.kirra.TypeRef
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.kotlinProperty

abstract open class KirraMetamodel() {

    abstract val kirraApplication: KirraPojoApplication

    fun <BS : IBaseService<*>> getEntityServiceClass(typeRef: TypeRef): KClass<BS> =
            try {
                Class.forName(getEntityClass(typeRef)!!.name + "Service").kotlin as KClass<BS>
            } catch (e: ClassNotFoundException) {
                getDefaultServiceClass()
            }

    abstract fun <BS : IBaseService<*>> getDefaultServiceClass(): KClass<BS>

    abstract fun getEntityService(typeRef: TypeRef): IBaseService<IBaseEntity>?

    protected val reflections by lazy {
        val entityPackageNames = kirraApplication.javaPackages
        val configuration = ConfigurationBuilder.build(entityPackageNames)
        Reflections(configuration)
    }

    protected val entityClasses by lazy {
        findEntityClasses().associate { Pair(it.name, it as Class<IBaseEntity>) }
    }

    abstract protected fun findEntityClasses(): Set<Class<*>>

    val entitiesByPackage by lazy {
        entityClasses.values.groupBy { it.`package`.name }
    }

    fun isEntityClass(clazz: KClassifier): Boolean =
            clazz is KClass<*> && entityClasses.containsKey(clazz.qualifiedName)

    fun isEntityClass(clazz: Class<*>): Boolean =
            entityClasses.containsKey(clazz.name)

    fun findDomainAccessor(operation: Operation, parameter: Parameter): Pair<KClass<*>, KFunction<IInstancePage<*>>>? {
        val domainAccessorInClass = findDomainAccessorInClass(getEntityClass(operation.owner)!!.kotlin, operation, parameter)
        return domainAccessorInClass
                ?: findDomainAccessorInClass(getEntityServiceClass<IBaseService<*>>(operation.owner), operation, parameter)
    }

    private fun findDomainAccessorInClass(kClass: KClass<*>?, operation: Operation, parameter: Parameter): Pair<KClass<*>, KFunction<IInstancePage<*>>>? {
        if (kClass == null)
            return null
//        val kFunction = kClass.functions.find { it.name == operation.name}
//        if (kFunction == null)
//            return null
//        val kParameter = kFunction.valueParameters.find { it.name == parameter.name }
//        if (kParameter == null)
//            return null
//        val domainAnnotation = kParameter.findAnnotation<Domain>()
        val accessorName = /*domainAnnotation?.let { it.accessor } ?: */ "${operation.name}_${parameter.name}"
        val found = kClass.functions.find {
            (it.name == accessorName || it.isDomainAccessorFor(parameter.name)) && it.isPageReturning()
        } as KFunction<IInstancePage<*>>?
        return found?.let { kClass to found }
    }

    protected open fun KFunction<*>.isPageReturning(): Boolean {
        return returnType.isSubtypeOf(IInstancePage::class.starProjectedType)
    }

    protected open fun KFunction<*>.isDomainAccessorFor(parameterName: String): Boolean {
        return findAnnotation<DomainAccessor>()?.let { it.parameterName == parameterName } ?: false
    }

    fun namespaceToPackageName(namespace: String): String = kirraApplication.javaPackages.find { it.endsWith(".${namespace}") }
            ?: namespace

    fun getEntityClass(typeRef: TypeRef) : Class<IBaseEntity>? {
        val packageName = namespaceToPackageName(typeRef.namespace)
        val found = entitiesByPackage[packageName]?.find { it.simpleName == typeRef.typeName }
        return found
    }

    fun getEnumClass(typeRef: TypeRef): Class<Enum<*>>? {
        val packageName = namespaceToPackageName(typeRef.namespace)
        val enumClassName = "${packageName}.${typeRef.typeName.replace('+', '$')}"
        try {
            val enumClass = Class.forName(enumClassName) as Class<Enum<*>>
            return enumClass
        } catch (e: ClassNotFoundException) {
            return null
        }
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
        val inherited = entityAsKotlinClass.superclasses.filter { it.isSubclassOf(IBaseEntity::class) }.map { getAllKotlinProperties(it) }.flatten()
        val declared = entityAsKotlinClass.java.declaredFields.map { it.kotlinProperty }.filterNotNull()
        return inherited + declared
    }

    fun isRelationshipAccessor(function: KFunction<*>): Boolean {
        val result = function.findAnnotation<RelationshipAccessor>() != null &&
                function.valueParameters.size == 1 &&
                (function.valueParameters[0].type?.classifier?.let { it as KClass<*> }?.let { isEntityClass(it) }
                        ?: false) &&
                (
                        (
                                function.returnType.isSubtypeOf(Iterable::class.starProjectedType) &&
                                        IBaseEntity::class.isSuperclassOf(function.returnType.arguments[0].type?.classifier as KClass<*>)
                                ) || (
                                IBaseEntity::class.isSuperclassOf(function.returnType.classifier as KClass<*>)
                                )
                        )
        return result
    }

    fun getRelationshipAccessor(relationship: Relationship): KCallable<IBaseEntity>? {
        val entityService = getEntityService(relationship.owner)
        if (entityService != null) {
            val relationshipAccessor = getRelationshipAccessorInService(entityService, relationship)
            if (relationshipAccessor != null)
                return relationshipAccessor as KFunction<IBaseEntity>
        }
        val relatedEntityService = getEntityService(relationship.typeRef)
        if (relatedEntityService != null) {
            val relationshipAccessor = getRelationshipAccessorInService(relatedEntityService, relationship)
            if (relationshipAccessor != null)
                return BoundFunction<IBaseEntity>(relatedEntityService, relationshipAccessor)
        }
        return null
    }

    private fun <T : IBaseEntity> getRelationshipAccessorInService(entityService: IBaseService<T>, relationship: Relationship): KFunction<IBaseEntity>? {
        val memberFunctions = findMemberFunctions(entityService)
        val relationshipAccessor = memberFunctions.firstOrNull {
            it.name == relationship.name && isRelationshipAccessor(it)
        }
        return relationshipAccessor as? KFunction<IBaseEntity>
    }

    abstract fun findMemberFunctions(instance: IBaseService<*>): Iterable<KFunction<*>>

    fun getRelationshipAccessors(entityService: IBaseService<IBaseEntity>): List<KFunction<IBaseEntity>> {
        val memberFunctions = findMemberFunctions(entityService)
        val relationshipAccessors = memberFunctions.filter {
            isRelationshipAccessor(it)
        }
        return relationshipAccessors as List<KFunction<IBaseEntity>>
    }

    abstract fun isEntity(javaType: KClass<out Any>): Boolean
}