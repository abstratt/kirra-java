package com.abstratt.kirra.spring.api

import com.abstratt.kirra.*
import com.abstratt.kirra.spring.BaseEntity
import com.abstratt.kirra.spring.GenericService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*
import kotlin.reflect.KClass

@Configuration
open class KirraSpringConfiguration {

    companion object {
        private val logger = LoggerFactory.getLogger(KirraSpringConfiguration::class.java.name)
    }

    @Autowired
    lateinit private var kirraSpringMetamodel: KirraSpringMetamodel

    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

    @Autowired
    private lateinit var schemaBuilder: SchemaBuilder

    @Bean
    open fun schema() : Schema =
        schemaBuilder.build()

    @Bean
    open fun schemaManagement(@Autowired schema : Schema) : SchemaManagement =
        SchemaManagementSnapshot(schema)

    @Autowired
    private fun createServices(schema : Schema) {
        schema.allEntities.forEach { entity -> createService(entity) }
    }

    private fun createService(entity: Entity) {
        val serviceName = entity.name.decapitalize() + "Service"
        val repositoryName = entity.name.decapitalize() + "Repository"
        val existingService = applicationContext.beanFactory.getSingleton(serviceName)
        if (existingService == null) {
            logger.info("Creating service for for ${entity.name}")
            val entityJavaClass = kirraSpringMetamodel.getEntityClass(entity.entityNamespace, entity.name)
            val entityClass : KClass<BaseEntity> = entityJavaClass!!.kotlin
            val genericService = GenericService(entityClass)
            val customRepository = applicationContext.getBean(repositoryName, JpaRepository::class.java) as JpaRepository<BaseEntity, Long>
            KirraException.ensure(customRepository != null, KirraException.Kind.SCHEMA, { "No repository found registered as ${repositoryName}" })
            genericService.repository = customRepository!!
            applicationContext.beanFactory.registerSingleton(serviceName, genericService)
        } else {
            logger.debug("No service created for ${entity.name}")
        }
    }
}