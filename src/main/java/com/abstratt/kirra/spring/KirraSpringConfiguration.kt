package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
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
        open fun schema(schemaBuilder: SchemaBuilder) : Schema =
                schemaBuilder.build()
    }

    @Autowired
    lateinit private var kirraSpringMetamodel: KirraSpringMetamodel

    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

    @PersistenceContext
    private lateinit var entityManager : EntityManager

    @Bean
    open fun schemaManagement(@Autowired schema : Schema) : SchemaManagement =
        SchemaManagementSnapshot(schema)

    @Autowired
    private fun createServices(schema : Schema) {
        schema.allEntities.forEach { entity -> createService(entity, entityManager) }
    }

    private fun createService(entity: Entity, entityManager: EntityManager) {
        val serviceName = entity.name.decapitalize() + "Service"
        val repositoryName = entity.name.decapitalize() + "Repository"
        val existingService = applicationContext.beanFactory.getSingleton(serviceName)
        if (existingService == null) {
            logger.info("Creating service for for ${entity.name}")
            val entityJavaClass = kirraSpringMetamodel.getEntityClass(entity.entityNamespace, entity.name)
            val entityClass : KClass<BaseEntity> = entityJavaClass!!.kotlin
            val genericService = GenericService(entityClass)
            val repository =
                try {
                    applicationContext.getBean(repositoryName, JpaRepository::class.java) as JpaRepository<BaseEntity, Long>
                } catch (e : NoSuchBeanDefinitionException) {
                    SimpleJpaRepository<BaseEntity, Long>(entityClass.java, entityManager)
                }
            genericService.repository = repository
            applicationContext.beanFactory.registerSingleton(serviceName, genericService)
        } else {
            logger.debug("No service created for ${entity.name}")
        }
    }
}