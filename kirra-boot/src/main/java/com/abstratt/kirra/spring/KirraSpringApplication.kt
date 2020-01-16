package com.abstratt.kirra.spring;

import com.abstratt.kirra.KirraApplication
import kotlin.reflect.KClass

class KirraSpringApplication(name : String, val javaPackages: Array<String>) : KirraApplication(name) {
    constructor(name : String, javaClasses: Array<KClass<out BaseEntity>>) : this(name, toPackageNames(javaClasses))
}
