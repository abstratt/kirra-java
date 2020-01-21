package com.abstratt.kirra.spring;

import com.abstratt.kirra.KirraApplication
import com.abstratt.kirra.pojo.IBaseEntity
import com.abstratt.kirra.pojo.KirraPojoApplication
import com.abstratt.kirra.pojo.toPackageNames
import kotlin.reflect.KClass

class KirraSpringApplication(name : String, javaPackages: Array<String>) : KirraPojoApplication(name, javaPackages)
