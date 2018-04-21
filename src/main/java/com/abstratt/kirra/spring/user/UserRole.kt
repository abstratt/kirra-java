package com.abstratt.kirra.spring.user

import com.abstratt.kirra.spring.*
import com.abstratt.kirra.spring.userprofile.UserProfile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import javax.persistence.*
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Role

@MappedSuperclass
abstract class RoleEntity(
    id : Long?,
    @ManyToOne
    var user : UserProfile? = null
) : BaseEntity(id) {
    @ImplementationOp
    abstract fun getRole() : UserRole
}

interface UserRole {
    fun toAuthorityName() : String = "ROLE_${(this as Enum<*>).name}"
    fun toGrantedAuthority() : GrantedAuthority = SimpleGrantedAuthority(toAuthorityName())
    fun roleName() : String {
        if (this is Enum<*>) {
            return this.name
        }
        return this::class.simpleName!!
    }
}

@NoRepositoryBean
interface RoleRepository<E : RoleEntity> : BaseRepository<E> {
    fun findByUser(user : UserProfile) : E?
}

@Service
open class RoleService {
    @Autowired
    lateinit var kirraJavaApplication: KirraJavaApplication

    @Autowired
    lateinit var kirraRepositoryRegistry: KirraRepositoryRegistry

    fun findAuthorities(user : UserProfile): List<GrantedAuthority> =
            findRoleObjects(user).map { it.getRole().toGrantedAuthority() }

    fun findRoleObjects(user : UserProfile): List<RoleEntity> =
            kirraJavaApplication.applicationUserRoles.map { asRole<RoleEntity>(user, it) }.filterNotNull()

    fun <RE: RoleEntity> asRole(user : UserProfile, role : UserRole) : RE? = asRole(user, role.roleName())

    fun <RE: RoleEntity> asRole(user : UserProfile, roleEntityClass : KClass<RE>) : RE? = asRole(user, roleEntityClass.simpleName!!)

    private fun <RE : RoleEntity> asRole(user: UserProfile, roleName: String): RE? {
        val roleRepository = kirraRepositoryRegistry.getRepository<RoleEntity>(roleName) as? RoleRepository<RE>
        assert(roleRepository != null)
        val found = roleRepository?.findByUser(user)
        return found
    }

    fun toUserDetails(user : UserProfile): KirraUserDetails =
            KirraUserDetails(user.id!!, user.username!!, user.readPassword()!!, findAuthorities(user))
}


@Service
@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
open class UserProfileService : BaseService<UserProfile, UserProfileRepository>(UserProfile::class) {

    fun findUserByUsername(username: String) = repository.findByUsername(username)

    fun findUserByUsernameAndPassword(username: String, password: String) = repository.findByUsernameAndPassword(username, password)

    @Transactional
    override fun update(toUpdate: UserProfile): UserProfile? {
        val existingInstance = repository.findById(toUpdate.id)
        if (existingInstance.isPresent) {
            if (toUpdate.readPassword() != null) {
                existingInstance.get().updatePassword(toUpdate.readPassword()!!)
                return repository.save(existingInstance.get())
            }
        }
        return existingInstance.get()
    }

    @Transactional
    override fun create(toCreate: UserProfile) : UserProfile {
        BusinessException.ensure(toCreate.readPassword() != null, ErrorCode.INVALID_OR_MISSING_DATA, "password")
        return super.create(toCreate)
    }
}

@Repository
interface UserProfileRepository : BaseRepository<UserProfile> {
    fun findByUsername(username: String): UserProfile?
    fun findByUsernameAndPassword(username: String, password : String): UserProfile?
}

