package com.abstratt.kirra.spring.api;

import com.abstratt.kirra.KirraApplication
import com.abstratt.kirra.spring.BaseEntity
import org.reflections.util.ConfigurationBuilder
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.domain.EntityScanPackages
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

class KirraSpringApplication(name : String, val javaPackages: Array<String>) : KirraApplication(name) {
    constructor(name : String, javaClasses: Array<KClass<out BaseEntity>>) : this(name, toPackageNames(javaClasses))

}
