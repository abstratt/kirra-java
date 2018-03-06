package com.abstratt.kirra.spring

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity

//@Configuration
//@EnableGlobalMethodSecurity(prePostEnabled = true)
//class KirraMethodSecurityConfig {
//    @Bean
//    fun methodSecurityService(): MethodSecurityService {
//        return MethodSecurityServiceImpl()
//    }
//
//    @Autowired
//    @Throws(Exception::class)
//    fun registerGlobal(auth: AuthenticationManagerBuilder) {
//        auth
//                .inMemoryAuthentication()
//                .withUser("user").password("password").roles("USER").and()
//                .withUser("admin").password("password").roles("USER", "ADMIN")
//    }
//}