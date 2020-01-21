package com.abstratt.kirra.pojo

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.instanceParameter

class BoundFunction<R>(val instance : Any, val baseFunction : KFunction<R>) : KFunction<R> by baseFunction {
    override fun call(vararg args: Any?): R =
            baseFunction.call(*(listOf(instance) + args.asList()).toTypedArray())

    override fun callBy(args: Map<KParameter, Any?>): R {
        val instanceParameter = baseFunction.instanceParameter!!
        val targetObject = mapOf(Pair(instanceParameter, instance))
        val combinedArguments = targetObject + args
        return baseFunction.callBy(combinedArguments)
    }

    override val parameters: List<KParameter>
        get() = listOf(baseFunction.instanceParameter!!) + baseFunction.parameters

    override val name: String
        get() = baseFunction.name

}
