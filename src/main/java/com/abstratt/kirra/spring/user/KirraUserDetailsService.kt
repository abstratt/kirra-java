package com.abstratt.kirra.spring.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import java.util.Optional.ofNullable


@Component
class KirraUserDetailsService : UserDetailsService {
    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    override fun loadUserByUsername(username: String): KirraUserDetails =
            ofNullable(applicationUserService.findUserByUsername(username))
                    .map { it.toUserDetails() }
                    .orElseThrow { UsernameNotFoundException(username) }
}

class KirraUserDetails(var userId : Long, username: String, password: String, authorities: List<GrantedAuthority>) : User(username, password, authorities)

