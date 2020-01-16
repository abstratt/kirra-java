package com.abstratt.kirra.spring.api;

import com.abstratt.kirra.rest.resources.KirraCorsFilter
import com.abstratt.kirra.rest.resources.KirraJaxRsApplication
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

import javax.ws.rs.ApplicationPath;

@Component
@ApplicationPath("/api/")
class CustomJaxRsApplication: KirraJaxRsApplication {
    constructor() {
        println("Creating the JAX-RS application")
    }
    override fun getClasses(): MutableSet<Class<*>> {
        val classes = LinkedHashSet(super.getClasses())
        return classes
    }
}