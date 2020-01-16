package com.abstratt.kirra.spring

import com.abstratt.kirra.Relationship
import java.time.LocalDate
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction6

annotation class Named (
        val label : String = "",
        val description : String = "",
        val name : String = "",
        val symbol : String = ""
)

@Target(AnnotationTarget.FUNCTION)
annotation class ActionOp

@Target(AnnotationTarget.FUNCTION)
annotation class QueryOp

@Target(AnnotationTarget.FUNCTION)
annotation class RelationshipAccessor(
    val style : Relationship.Style = Relationship.Style.LINK,
    val derived : Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
annotation class DomainAccessor (
    val methodName : String = "",
    val parameterName : String = ""
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Domain (
    val accessor : String
)


@Target(AnnotationTarget.FUNCTION)
annotation class ImplementationOp

@Target(AnnotationTarget.PROPERTY)
annotation class MnemonicProperty
