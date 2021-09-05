package com.abstratt.kirra.spring

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile(KirraAppProfile.DEVELOPMENT)
class KirraAppFlywayDevelopmentConfiguration {
    @Bean
    fun cleanMigrateStrategy(): FlywayMigrationStrategy =
        FlywayMigrationStrategy { flyway ->
            flyway.clean()
            flyway.migrate()
        }
}
