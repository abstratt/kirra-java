package com.abstratt.kirra.spring

import com.abstratt.kirra.KirraApplication
import com.abstratt.kirra.spring.api.KirraSpringApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Bean
import org.springframework.orm.jpa.LocalEntityManagerFactoryBean
import org.hibernate.cfg.AvailableSettings.DIALECT
import org.hibernate.cfg.AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER
import org.hibernate.cfg.AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER
import org.hibernate.MultiTenancyStrategy
import org.hibernate.cfg.AvailableSettings.MULTI_TENANT
import org.hibernate.cfg.Environment
import org.hibernate.dialect.PostgreSQL92Dialect
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.orm.jpa.JpaVendorAdapter
import org.hibernate.jpa.HibernatePersistenceProvider
import sample.Order
import sample.Person
import java.util.*
import kotlin.reflect.KClass


@Configuration
@Profile("integration")
@ComponentScan(basePackages = arrayOf("com.abstratt.kirra.spring"))
open class TestConfig {
    open val kirraSpringApplication : KirraSpringApplication
    @Bean
    get() {
        val entityClasses : Array<KClass<out BaseEntity>> = arrayOf(Order::class, Person::class)
        return KirraSpringApplication("test", entityClasses)
    }

    @Bean
    open fun entityManagerFactory() : LocalEntityManagerFactoryBean {
        val em = LocalEntityManagerFactoryBean()
        em.persistenceProvider = HibernatePersistenceProvider()
        em.persistenceUnitName = "test"
        val vendorAdapter = HibernateJpaVendorAdapter()
        em.jpaVendorAdapter = vendorAdapter
        val properties = Properties()
        properties[Environment.DIALECT] = PostgreSQL92Dialect::class.java.name
        em.setJpaProperties(properties)
        return em
    }
}


