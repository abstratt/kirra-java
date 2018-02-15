package com.abstratt.kirra.spring.api;

import com.abstratt.kirra.KirraApplication
import com.abstratt.kirra.spring.BaseEntity
import kotlin.reflect.KClass

class KirraSpringApplication(name : String, val javaPackages: Array<String>) : KirraApplication(name) {
    constructor(name : String, javaClasses: Array<KClass<out BaseEntity>>) : this(name, toPackageNames(javaClasses))
}