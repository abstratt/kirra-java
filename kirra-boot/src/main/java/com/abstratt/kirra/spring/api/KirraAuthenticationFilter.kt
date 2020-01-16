package com.abstratt.kirra.spring.api

import com.abstratt.kirra.spring.user.KirraSpringAuthenticationProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationDetailsSource
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class KirraAuthenticationFilter : OncePerRequestFilter() {

    private var authenticationDetailsSource: AuthenticationDetailsSource<HttpServletRequest, *> = WebAuthenticationDetailsSource()

    @Autowired
    lateinit var kirraAuthenticationProvider : KirraSpringAuthenticationProvider

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val authenticationHeader: String? = request.getHeader("Authorization")
        if (authenticationHeader == null || !authenticationHeader.startsWith("Custom ")) {
            filterChain.doFilter(request, response)
            return
        }
        val encodedCredentials = authenticationHeader.removePrefix("Custom ")
        val decodedCredentials = String(Base64.getDecoder().decode(encodedCredentials))
        val split = decodedCredentials.split(":")
        val (username, password) = Pair(split[0], split.drop(1).joinTo(StringBuilder()).toString())
        val authRequest = UsernamePasswordAuthenticationToken(username, password)
        authRequest.details = this.authenticationDetailsSource.buildDetails(request)
        try {
            val authResult = kirraAuthenticationProvider.authenticate(authRequest)
            SecurityContextHolder.getContext().authentication = authResult
        } catch (e: AuthenticationException) {
            SecurityContextHolder.clearContext()
        }
        filterChain.doFilter(request, response)
    }
}
