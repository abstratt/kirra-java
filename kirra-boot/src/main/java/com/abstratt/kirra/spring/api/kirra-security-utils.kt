package com.abstratt.kirra.spring.api

import com.abstratt.kirra.spring.user.RoleEntity
import com.abstratt.kirra.spring.user.RoleService
import com.abstratt.kirra.spring.userprofile.UserProfile
import com.abstratt.kirra.spring.userprofile.UserProfileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

interface SecurityService {
    fun <R : RoleEntity> getCurrentUserAs(roleEntityClass: KClass<R>): R?
    fun getCurrentUser(): UserProfile?
    fun getCurrentUserRoles(): List<RoleEntity>?
    fun getCurrentUsername(): String?
}

abstract class AbstractSecurityService : SecurityService {
    @Autowired
    lateinit var userProfileService: UserProfileService

    @Autowired
    lateinit var roleService: RoleService

    override fun <R : RoleEntity> getCurrentUserAs(roleEntityClass : KClass<R>): R? =
            getCurrentUser()?.let { roleService.asRole(it, roleEntityClass) }

    override fun getCurrentUserRoles(): List<RoleEntity>? =
            getCurrentUser()?.let { roleService.findRoleObjects(it) }

    override fun getCurrentUser(): UserProfile? {
        val principal = getCurrentUsername()
        return principal?.let { userProfileService.findUserByUsername(it) }
    }

}

@Service
@Profile("!test")
class KirraSecurityService : AbstractSecurityService() {
    override fun getCurrentUsername() = SecurityContextHolder.getContext().authentication?.principal as? String
}