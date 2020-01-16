package com.abstratt.kirra.spring

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