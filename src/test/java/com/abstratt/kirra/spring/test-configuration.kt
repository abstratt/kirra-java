package com.abstratt.kirra.spring

import com.abstratt.kirra.spring.api.KirraSpringAPIMarker
import com.abstratt.kirra.spring.testing.sample.SampleMarker
import com.abstratt.kirra.spring.userprofile.UserProfileMarker
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.data.jpa.repository.config.EnableJpaRepositories


@Configuration()
@ComponentScan(basePackageClasses = [UserProfileMarker::class, KirraSpringMarker::class, KirraSpringAPIMarker::class, SampleMarker::class])
@EntityScan(basePackageClasses = [UserProfileMarker::class, KirraSpringMarker::class, KirraSpringAPIMarker::class, SampleMarker::class])
@EnableJpaRepositories(basePackageClasses = [UserProfileMarker::class, KirraSpringMarker::class, KirraSpringAPIMarker::class, SampleMarker::class])
@PropertySource("classpath:application.properties")
open class TestConfig {
/*    @Bean
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
    }*/

    @Bean
    open fun propertySourcesPlaceholderConfigurer(): PropertySourcesPlaceholderConfigurer {
        val configurer = PropertySourcesPlaceholderConfigurer()
        configurer.setIgnoreUnresolvablePlaceholders(true)
        return configurer
    }
}
