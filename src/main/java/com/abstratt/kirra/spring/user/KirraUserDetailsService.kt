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
    lateinit var userProfileService: UserProfileService
    @Autowired
    lateinit var roleService: RoleService

    override fun loadUserByUsername(username: String): KirraUserDetails =
            ofNullable(userProfileService.findUserByUsername(username))
                    .map { roleService.toUserDetails(it) }
                    .orElseThrow { UsernameNotFoundException(username) }
}

class KirraUserDetails(var userId : Long, username: String, password: String, authorities: List<GrantedAuthority>) : User(username, password, authorities)

