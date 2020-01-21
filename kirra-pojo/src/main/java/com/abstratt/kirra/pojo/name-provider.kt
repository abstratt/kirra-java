package com.abstratt.kirra.pojo

import org.apache.commons.lang3.StringUtils
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

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
