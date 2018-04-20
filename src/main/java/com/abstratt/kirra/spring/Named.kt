package com.abstratt.kirra.spring

import com.abstratt.kirra.Relationship

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
    val style : Relationship.Style = Relationship.Style.LINK
)

@Target(AnnotationTarget.FUNCTION)
annotation class ImplementationOp

@Target(AnnotationTarget.PROPERTY)
annotation class MnemonicProperty
