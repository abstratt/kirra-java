package com.abstratt.kirra.spring

import com.abstratt.easyalpha.cart.ShoppingCartMarker
import com.abstratt.kirra.spring.api.KirraSpringAPIMarker
import com.abstratt.kirra.spring.testing.sample.SampleMarker
import com.abstratt.kirra.spring.userprofile.UserProfileMarker
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer

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


@Configuration
@KirraApplicationConfiguration(basePackageClasses = [UserProfileMarker::class, KirraSpringMarker::class, KirraSpringAPIMarker::class, SampleMarker::class, ShoppingCartMarker::class])
@PropertySource("classpath:application.properties")
open class TestConfig {
    @Bean
    open fun propertySourcesPlaceholderConfigurer(): PropertySourcesPlaceholderConfigurer {
        val configurer = PropertySourcesPlaceholderConfigurer()
        configurer.setIgnoreUnresolvablePlaceholders(true)
        return configurer
    }
}
