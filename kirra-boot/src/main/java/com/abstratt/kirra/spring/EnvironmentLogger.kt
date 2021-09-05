package com.abstratt.kirra.spring

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.AbstractEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component


@Component
class EnvironmentLogger {
    companion object {
        private val logger = LoggerFactory.getLogger(EnvironmentLogger::class.qualifiedName)
    }
    @Autowired
    private val environment: Environment? = null

    @PostConstruct
    fun introspect() {
        val propertySources = (environment as AbstractEnvironment).propertySources
        val allPropertyNames = propertySources.iterator().asSequence()
                .filter { it is EnumerablePropertySource }
                .map { (it as EnumerablePropertySource).propertyNames.asIterable() }
                .flatten()

        allPropertyNames.forEach { key -> logger.debug(key + "=" + environment.getProperty(key)) }
    }
}
