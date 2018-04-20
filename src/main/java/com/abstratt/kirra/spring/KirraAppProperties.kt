package com.abstratt.kirra.spring

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "kirra.app")
class KirraAppProperties {
    lateinit var name : String
}

