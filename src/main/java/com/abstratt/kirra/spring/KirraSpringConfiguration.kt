package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.reflect.KClass

@Configuration
open class KirraSpringConfiguration {

    companion object {
        private val logger = LoggerFactory.getLogger(KirraSpringConfiguration::class.java.name)

        @Bean
        @Autowired
        fun schema(schemaBuilder: SchemaBuilder) : Schema =
                schemaBuilder.build()
    }

    @Autowired
    lateinit private var kirraSpringMetamodel: KirraSpringMetamodel

    @Autowired
    lateinit private var kirraRepositoryRegistry: KirraRepositoryRegistry

    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

    @Bean
    open fun schemaManagement(@Autowired schema : Schema) : SchemaManagement =
        SchemaManagementSnapshot(schema)

    @Autowired
    private fun createServices(schema : Schema) {
        schema.allEntities.forEach { entity -> createService(entity) }
    }

    private fun createService(entity: Entity) {
        val serviceName = entity.name.decapitalize() + "Service"
        val existingService = applicationContext.beanFactory.getSingleton(serviceName)
        if (existingService == null) {
            logger.info("Creating service for ${entity.name} as ${serviceName}")
            val entityJavaClass = kirraSpringMetamodel.getEntityClass(entity.entityNamespace, entity.name)
            assert(entityJavaClass != null, { "No entity class for ${entity.typeRef}"})
            val entityClass : KClass<BaseEntity> = entityJavaClass!!.kotlin
            val genericService = GenericService(entityClass)
            val repository = kirraRepositoryRegistry.findOrCreateRepository<BaseEntity>(entity.name, entityClass)
            genericService.repository = repository
            applicationContext.beanFactory.registerSingleton(serviceName, genericService)
            logger.debug("Service created for ${entity.name}")
        } else {
            logger.debug("No service created for ${entity.name}")
        }
    }
}