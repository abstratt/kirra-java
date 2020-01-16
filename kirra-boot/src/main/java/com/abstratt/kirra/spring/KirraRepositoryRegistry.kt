package com.abstratt.kirra.spring

import com.abstratt.kirra.spring.user.GenericRoleEntityRepository
import com.abstratt.kirra.spring.user.RoleEntity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Component
class KirraRepositoryRegistry {


    companion object {
        private val logger = LoggerFactory.getLogger(KirraRepositoryRegistry::class.java.name)
    }

    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

    @Autowired
    lateinit private var kirraSpringMetamodel: KirraSpringMetamodel

    @PersistenceContext
    private lateinit var entityManager : EntityManager


    fun <E : BaseEntity> getRepository(entityName : String) : BaseRepository<E>? {
        val repositoryName = "${entityName.decapitalize()}Repository"
        val repository =
            try {
                applicationContext.getBean(repositoryName, BaseRepository::class.java) as BaseRepository<E>
            } catch (e : NoSuchBeanDefinitionException) {
                null
            }
        return repository
    }

    fun <T : BaseEntity> findOrCreateRepository(name: String, entityClass : KClass<T>): JpaRepository<T, Long> {
        val existingRepository = getRepository<T>(name)
        return existingRepository ?: createGenericRepository(entityClass, entityManager)
    }

    private fun <T : BaseEntity> createGenericRepository(entityClass: KClass<T>, entityManager: EntityManager): GenericRepository<T> {
        throw IllegalStateException("Missing repository for ${entityClass.simpleName}")
        return if (entityClass.isSubclassOf(RoleEntity::class))
            GenericRoleEntityRepository(entityClass as KClass<RoleEntity>, entityManager) as GenericRepository<T>
        else
            GenericRepository(entityClass, entityManager)
    }
}