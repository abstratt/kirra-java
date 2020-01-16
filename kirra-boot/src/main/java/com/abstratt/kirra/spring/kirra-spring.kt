package com.abstratt.kirra.spring

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import kotlin.reflect.KClass

fun defaultPageRequest(page: Int? = 0, limit: Int? = 10) = PageRequest(
    page?:0, limit?:9999, Sort(Sort.Order("id"))
)


fun toPackageNames(javaClasses : Array<KClass<out BaseEntity>>) : Array<String> =
        javaClasses.map { it.java.`package`.name }.toSet().toTypedArray()




