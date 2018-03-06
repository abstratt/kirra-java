package com.abstratt.kirra.spring

import com.abstratt.kirra.spring.api.KirraSpringAPIMarker
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.orm.jpa.LocalEntityManagerFactoryBean
import org.hibernate.cfg.Environment
import org.hibernate.dialect.PostgreSQL92Dialect
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import com.abstratt.kirra.spring.testing.sample.SampleMarker
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import java.util.*


@Configuration
@ComponentScan(basePackageClasses = [KirraSpringMarker::class, KirraSpringAPIMarker::class, SampleMarker::class])
@EntityScan(basePackageClasses = [KirraSpringMarker::class, KirraSpringAPIMarker::class,SampleMarker::class])
@EnableJpaRepositories(basePackageClasses = [KirraSpringMarker::class, KirraSpringAPIMarker::class,SampleMarker::class])
open class TestConfig {
    @Bean
    open fun entityManagerFactory() : LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.setPackagesToScan(SampleMarker::class.java.`package`.name, KirraSpringMarker::class.java.`package`.name)
        em.persistenceProvider = HibernatePersistenceProvider()
        val vendorAdapter = HibernateJpaVendorAdapter()
        em.jpaVendorAdapter = vendorAdapter
        val properties = Properties()
        properties[Environment.DIALECT] = PostgreSQL92Dialect::class.java.name
        em.setJpaProperties(properties)
        return em
    }
}


