package com.abstratt.kirra.spring;

import com.abstratt.kirra.KirraApplication
import com.abstratt.kirra.pojo.IBaseEntity
import com.abstratt.kirra.pojo.toPackageNames
import kotlin.reflect.KClass

class KirraSpringApplication(name : String, val javaPackages: Array<String>) : KirraApplication(name) {
    constructor(name : String, javaClasses: Array<KClass<out IBaseEntity>>) : this(name, toPackageNames(javaClasses))
}
