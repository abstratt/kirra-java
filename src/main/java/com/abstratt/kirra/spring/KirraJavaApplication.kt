package com.abstratt.kirra.spring

import com.abstratt.kirra.spring.api.KirraSpringAPIMarker
import com.abstratt.kirra.spring.user.UserRole
import com.abstratt.kirra.spring.userprofile.UserProfileMarker
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PropertiesLoaderUtils
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
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


@EntityScan
@ComponentScan
@EnableJpaRepositories
@EnableTransactionManagement
@Configuration
@SpringBootApplication
annotation class KirraApplicationConfiguration (
    val basePackageClasses: Array<KClass<*>> = []
)
