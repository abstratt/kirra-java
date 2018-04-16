package com.abstratt.kirra.spring.user

import com.abstratt.kirra.spring.KirraJavaApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class KirraSpringAuthenticationProvider : AuthenticationProvider {
    @Autowired
    lateinit var userProfileService: UserProfileService

    @Autowired
    lateinit var kirraJavaApplication : KirraJavaApplication

    override fun supports(authentication: Class<*>?): Boolean {
        val result = UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)
        return result
    }

    override fun authenticate(authentication: Authentication?): Authentication {
        val token = authentication as UsernamePasswordAuthenticationToken
        val username = token.name
        val password = token.credentials as String
        val found = userProfileService.findUserByUsernameAndPassword(username, password)
        if (found == null) {
            throw BadCredentialsException("Bad credentials")
        }
        val authorities = kirraJavaApplication.applicationUserRoles.map { it.toGrantedAuthority() }

        val successfulAuthentication = UsernamePasswordAuthenticationToken(username, password, authorities)
        return successfulAuthentication
    }
}