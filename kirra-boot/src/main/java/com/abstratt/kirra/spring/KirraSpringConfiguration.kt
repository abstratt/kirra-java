package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import com.abstratt.kirra.pojo.IBaseEntity
import com.abstratt.kirra.pojo.IBaseService
import com.abstratt.kirra.spring.api.SecurityService
import com.abstratt.kirra.spring.boot.KirraSpringMetamodel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
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

        /* This is contributed here to introduce some strict ordering and avoid cycles. */
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

    @PersistenceContext
    private lateinit var entityManager : EntityManager

    @Bean
    open fun instanceManagement() : InstanceManagement =
        KirraSpringInstanceManagement(kirraSpringMetamodel, kirraSpringInstanceBridge, schemaManagement, securityService, entityManager)

    @Autowired
    private fun createServices(schema : Schema) {
        schema.allEntities.forEach { entity -> createService(entity) }
    }

    private fun createService(entity: Entity) {
        val entityRef = entity.typeRef
        val serviceName = entity.name.decapitalize() + "Service"
        val entityJavaClass = kirraSpringMetamodel.getEntityClass(entityRef)
        val serviceJavaClass : KClass<IBaseService<*>> = kirraSpringMetamodel.getEntityServiceClass(entityRef)

        val existingServiceBeanNames = applicationContext.beanFactory.getBeanNamesForType(serviceJavaClass.java, false, false)

        if (existingServiceBeanNames.isEmpty()) {
            logger.info("Creating generic service for ${entity.name} as ${serviceName}")
            assert(entityJavaClass != null, { "No entity class for ${entity.typeRef}"})
            val entityClass : KClass<IBaseEntity> = entityJavaClass!!.kotlin
            val genericService = GenericService(entityClass)
            val repository = kirraRepositoryRegistry.findOrCreateRepository<IBaseEntity>(entity.name, entityClass)
            genericService.repository = repository
            applicationContext.beanFactory.registerSingleton(serviceName, genericService)
            logger.debug("Service created for ${entity.name}")
        } else {
            logger.debug("No service created for ${entity.name}, as custom implementation(s) '${existingServiceBeanNames}' exist(s)")
        }
    }
}