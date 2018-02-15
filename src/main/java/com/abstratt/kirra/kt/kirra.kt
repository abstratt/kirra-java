package com.abstratt.kirra.kt

import kotlin.reflect.KClass

/**
 * Represents a multi-valued parent-child relationship.
 */
class Children<E : Any>(
    val entityClass : KClass<E>
) {
    companion object {
        /**
         * Creates a multi-valued parent-child relationship based on the given type.
         */
        fun <E : Any> of(childEntity : KClass<E>) : Children<E> = Children(childEntity)
    }
}

annotation class Named (
    val label : String = "",
    val description : String = "",
    val name : String = "",
    val symbol : String = ""
)
