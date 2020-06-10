package com.abstratt.kirra.spring

import com.abstratt.kirra.spring.api.AbstractSecurityService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("test")
@Component
open class TestSecurityService : AbstractSecurityService() {
    var selectedUsername : String? = null

    override fun getCurrentUsername(): String? = selectedUsername
}