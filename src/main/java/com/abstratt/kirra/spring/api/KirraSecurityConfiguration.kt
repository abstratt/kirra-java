package com.abstratt.kirra.spring.api

import com.abstratt.kirra.spring.KirraJavaApplication
import com.abstratt.kirra.spring.KirraSpringApplication
import com.abstratt.kirra.spring.user.KirraSpringAuthenticationProvider
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

    @Autowired
    lateinit var kirraJavaApplication : KirraJavaApplication

    @Autowired
    lateinit var kirraAuthenticationProvider : KirraSpringAuthenticationProvider

    @Throws(Exception::class)
    protected open override fun configure(http: HttpSecurity) {
        http
                .anonymous().authorities("ROLE_ANONYMOUS").and()
                .csrf().disable()
                .userDetailsService(userDetailsService)
                .authorizeRequests()
                    .antMatchers("/**")
                    .hasAnyAuthority(*(kirraJavaApplication.applicationUserRoles.map { it.toAuthority() }.toTypedArray()))
                .and()
                    .httpBasic()
                .and()
                    .authenticationProvider(kirraAuthenticationProvider)
    }
}

