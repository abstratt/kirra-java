package com.abstratt.kirra.spring

annotation class Named (
        val label : String = "",
        val description : String = "",
        val name : String = "",
        val symbol : String = ""
)

@Target(AnnotationTarget.FUNCTION)
annotation class ActionOperation

@Target(AnnotationTarget.FUNCTION)
annotation class QueryOperation