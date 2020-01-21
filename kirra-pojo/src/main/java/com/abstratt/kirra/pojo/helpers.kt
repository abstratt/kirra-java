package com.abstratt.kirra.pojo

import com.abstratt.kirra.TypeRef
import org.apache.commons.lang3.StringUtils
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Member
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.superclasses

fun <E> getTypeRef(javaClass: Class<E>, kind : TypeRef.TypeKind = TypeRef.TypeKind.Entity): TypeRef =
        TypeRef(packageNameToNamespace(javaClass.`package`.name), javaClass.simpleName, kind)

fun <E : IBaseEntity> KClass<E>.getTypeRef(kind : TypeRef.TypeKind = TypeRef.TypeKind.Entity): TypeRef =
        getTypeRef(this.java, kind)

fun packageNameToNamespace(packageName : String) : String = packageName.split('.').last()

fun getLabel(name: String): String =
        StringUtils.splitByCharacterTypeCamelCase(name).map { it.capitalize() }.joinToString(" ", "", "")

fun <T> Boolean.ifTrue(block : () -> T) : T? = if (this) block() else null

fun toPackageNames(javaClasses : Array<KClass<out IBaseEntity>>) : Array<String> =
        javaClasses.map { it.java.`package`.name }.toSet().toTypedArray()

fun <T : Annotation> Iterable<T>.findAttributeAnnotationsByType(predicate: (Annotation) -> Boolean): List<T> =
        this.filter(predicate)

fun <T : Annotation> Iterable<Annotation>.findAnnotationByType(annotationClass : KClass<T>): T? =
        this.find { annotationClass.isInstance(it) } as T?

fun <T : Annotation> Iterable<Annotation>.findAnnotationsByType(annotationClass : KClass<*>): List<T> =
        this.findAttributeAnnotationsByType { annotationClass.isInstance(it) }.map { it as T }

fun Member.getAnnotations(): Iterable<Annotation> =
        (this as AccessibleObject).annotations.asIterable()

fun <T : Annotation> KClass<*>.hasAnnotation(annotationClass : KClass<T>): Boolean {
    if (this.isCompanion)
        return this.java.enclosingClass.kotlin.hasAnnotation(annotationClass)
    val annotations = this.annotations
    val hasEntityAnnotation = annotations.findAnnotationByType(annotationClass) != null
    if (hasEntityAnnotation != null)
        return true
    val extendsEntity = this.superclasses.any { it.hasAnnotation(annotationClass) }
    return extendsEntity
}

fun <C, T> Annotation.getValue(property: KProperty1<C, T>): T? {
    val annotationFunctions = this.annotationClass.java.methods
    val method = annotationFunctions.find { it.name == property.name }
    return method?.invoke(this) as T?
}



fun <R> KFunction<R>.isImplementationMethod(): Boolean =
        when {
            setOf("hashCode", "equals", "copy", "toString").contains(this.name) -> true
            this.name.matches(Regex("component[0-9]*")) -> true
            this.findAnnotation<ImplementationOp>() != null -> true
            else -> false
        }

fun <T : Annotation> KClass<*>.isAnnotatedWith(annotationClass : KClass<T>): Boolean {
    if (this.isCompanion)
        return this.java.enclosingClass.kotlin.isAnnotatedWith(annotationClass)
    val annotations = this.annotations
    val hasEntityAnnotation = annotations.findAnnotationByType(annotationClass) != null
    if (hasEntityAnnotation != null)
        return true
    val extendsEntity = this.superclasses.any { it.isAnnotatedWith(annotationClass) }
    return extendsEntity
}
