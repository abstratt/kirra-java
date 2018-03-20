package com.abstratt.kirra.spring

import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Component

@Component
class KirraRepositoryRegistry {

    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

    fun <E : BaseEntity> getRepository(entityName : String) : Repository<Long, E>? {
        val repositoryName = "${entityName.decapitalize()}Repository"
        val repository =
            try {
                applicationContext.getBean(repositoryName, JpaRepository::class.java) as JpaRepository<BaseEntity, Long>
            } catch (e : NoSuchBeanDefinitionException) {
                null
            }
        return repository as? JpaRepository<Long, E>
    }
}