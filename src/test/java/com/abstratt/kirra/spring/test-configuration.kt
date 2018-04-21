package com.abstratt.kirra.spring

import com.abstratt.kirra.spring.api.KirraSpringAPIMarker
import com.abstratt.kirra.spring.testing.sample.SampleMarker
import com.abstratt.kirra.spring.userprofile.UserProfileMarker
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.*
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@Profile(KirraAppProfile.TESTING)
open class KirraAppFlywayTestingConfiguration {
    @Bean
    open fun cleanMigrateStrategy(): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { flyway ->
            flyway.clean()
            flyway.migrate()
        }
    }
}


@Configuration()
@ComponentScan(basePackageClasses = [UserProfileMarker::class, KirraSpringMarker::class, KirraSpringAPIMarker::class, SampleMarker::class])
@EntityScan(basePackageClasses = [UserProfileMarker::class, KirraSpringMarker::class, KirraSpringAPIMarker::class, SampleMarker::class])
@EnableJpaRepositories(basePackageClasses = [UserProfileMarker::class, KirraSpringMarker::class, KirraSpringAPIMarker::class, SampleMarker::class])
@PropertySource("classpath:application.properties")
open class TestConfig {
    @Bean
    open fun propertySourcesPlaceholderConfigurer(): PropertySourcesPlaceholderConfigurer {
        val configurer = PropertySourcesPlaceholderConfigurer()
        configurer.setIgnoreUnresolvablePlaceholders(true)
        return configurer
    }
}
