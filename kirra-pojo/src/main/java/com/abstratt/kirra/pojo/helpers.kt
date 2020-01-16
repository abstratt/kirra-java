package com.abstratt.kirra.pojo

import com.abstratt.kirra.TypeRef
import org.apache.commons.lang3.StringUtils
import kotlin.reflect.KClass

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
