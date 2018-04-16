package com.abstratt.kirra.spring

import com.abstratt.kirra.spring.user.UserRole
import org.springframework.boot.SpringApplication
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PropertiesLoaderUtils
import kotlin.collections.LinkedHashSet
import kotlin.reflect.KClass

abstract class KirraJavaApplication {
    var applicationUserRoles: Set<UserRole>

    constructor(applicationUserRoles: Iterable<UserRole>) {
        this.applicationUserRoles = applicationUserRoles.toSet()
    }
}

fun <C : KirraJavaApplication> runApplication(clazz : KClass<C>, args : Array<String>) {
    val springApplication = SpringApplication(clazz.java)
/*
    val commonProperties = PropertiesLoaderUtils.loadProperties(ClassPathResource(COMMON_PROPERTIES, clazz.java.classLoader))
    springApplication.setDefaultProperties(commonProperties)
*/
    springApplication.run(*args)
}
