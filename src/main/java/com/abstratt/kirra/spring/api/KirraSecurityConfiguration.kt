package com.abstratt.kirra.spring.api

import com.abstratt.kirra.spring.user.KirraUserDetailsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
open class KirraSecurityConfiguration : WebSecurityConfigurerAdapter() {
    @Autowired
    lateinit var userDetailsService : KirraUserDetailsService

    @Throws(Exception::class)
    protected open override fun configure(http: HttpSecurity) {
        http
                .anonymous().authorities("ROLE_ANONYMOUS").and()
                .csrf().disable()
                .userDetailsService(userDetailsService)
                .httpBasic()
    }
}

