package com.abstratt.kirra.spring.api

import com.abstratt.kirra.rest.common.Paths
import com.abstratt.kirra.spring.KirraJavaApplication
import com.abstratt.kirra.spring.user.KirraSpringAuthenticationProvider
import com.abstratt.kirra.spring.user.KirraUserDetailsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
open class KirraAPISecurityConfiguration : WebSecurityConfigurerAdapter() {
    @Autowired
    lateinit var userDetailsService : KirraUserDetailsService

    @Autowired
    lateinit var kirraJavaApplication : KirraJavaApplication

    @Autowired
    lateinit var kirraAuthenticationProvider : KirraSpringAuthenticationProvider

    @Autowired
    lateinit var kirraAuthenticationFilter: KirraAuthenticationFilter
    @Autowired
    lateinit var kirraContextFilter: KirraContextFilter

    @Autowired
    lateinit var kirraAccessDeniedHandler : KirraAccessDeniedHandler

    @Autowired
    lateinit var customCorsFilter: CustomCorsFilter

    @Autowired
    private lateinit var kirraAuthenticationEntryPoint: KirraAuthenticationEntryPoint

    @Throws(Exception::class)
    protected open override fun configure(http: HttpSecurity) {
        val securePaths = listOf(Paths.SESSION, Paths.ACTIONS, Paths.BLOBS, Paths.DATA, Paths.INSTANCES, Paths.RELATIONSHIPS, Paths.FINDERS, Paths.DOMAIN, Paths.METRICS).map { "/**/$it/**" }
        http
                .anonymous().authorities("ROLE_ANONYMOUS").and()
                .csrf().disable()
                .userDetailsService(userDetailsService)
                .authorizeRequests()
                    .antMatchers(HttpMethod.OPTIONS,"/**")
                        .permitAll()
                    .antMatchers(*(securePaths.toTypedArray()) )
                        .hasAnyAuthority(*(kirraJavaApplication.applicationUserRoles.map { it.toAuthorityName() }.toTypedArray()))
                    .antMatchers("/*")
                        .permitAll()
                .and()
                    .exceptionHandling()
                        .accessDeniedHandler(kirraAccessDeniedHandler)
                        .authenticationEntryPoint(kirraAuthenticationEntryPoint)
                .and()
                    .httpBasic()
                    .authenticationEntryPoint(kirraAuthenticationEntryPoint)
                .and()
                    .authenticationProvider(kirraAuthenticationProvider)
                .addFilterBefore(customCorsFilter, BasicAuthenticationFilter::class.java)
                .addFilterBefore(kirraAuthenticationFilter, BasicAuthenticationFilter::class.java)
                .addFilterBefore(kirraContextFilter, KirraAuthenticationFilter::class.java)
    }
}