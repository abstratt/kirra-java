package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import com.abstratt.kirra.spring.api.SecurityService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.PersistenceContext
import kotlin.reflect.KClass

@Configuration
@EnableTransactionManagement
open class KirraSpringConfiguration {

    companion object {
        private val logger = LoggerFactory.getLogger(KirraSpringConfiguration::class.java.name)

        @Bean
        @Autowired
        fun schema(schemaBuilder: SchemaBuilder) : Schema =
                schemaBuilder.build()
    }


    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired
    lateinit private var kirraSpringMetamodel: KirraSpringMetamodel

    @Autowired
    lateinit private var kirraSpringInstanceBridge: KirraSpringInstanceBridge

    @Autowired
    lateinit private var kirraRepositoryRegistry: KirraRepositoryRegistry

    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

    @Autowired
    private lateinit var securityService: SecurityService

    @Autowired
    private lateinit var schemaManagement: SchemaManagement

    @Bean
    open fun instanceManagement() : InstanceManagement =
        KirraSpringInstanceManagement(kirraSpringMetamodel, kirraSpringInstanceBridge, schemaManagement, securityService)

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