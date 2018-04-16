package com.abstratt.kirra.spring

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
annotation class ImplementationOp