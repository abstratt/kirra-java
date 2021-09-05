package com.abstratt.kirra.spring.api;

import jakarta.annotation.PostConstruct
import org.glassfish.jersey.server.ResourceConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
public class JerseyConfig : ResourceConfig() {
    @Autowired
    private lateinit var jaxrsApplication : javax.ws.rs.core.Application

    @PostConstruct
    fun init () {
        val customJerseyProperties = mapOf(Pair("jersey.config.server.resource.validation.ignoreErrors", true))
        addProperties(customJerseyProperties)
        val classes = jaxrsApplication.classes
        registerClasses(classes)
    }
}