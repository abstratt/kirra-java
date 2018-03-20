package com.abstratt.kirra.spring.user

import com.abstratt.kirra.spring.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import javax.persistence.*

@Target(AnnotationTarget.CLASS)
annotation class Role

@MappedSuperclass
abstract class RoleEntity(
    id : Long?,
    @ManyToOne
    var user : ApplicationUser? = null
) : BaseEntity(id) {
    abstract fun getRole() : UserRole
}

interface UserRole {
    fun toAuthorityName() : String = "ROLE_${(this as Enum<*>).name}"
    fun toGrantedAuthority() : GrantedAuthority = SimpleGrantedAuthority(toAuthorityName())
    fun roleName() : String {
        if (this is Enum<*>) {
            this.name
        }
        return this::class.simpleName!!
    }
}

interface RoleRepository<E : RoleEntity> {
    fun findByUser(user : ApplicationUser) : E?
}

@Entity
class ApplicationUser(
        id: Long? = null,
        var username: String? = null,
        var password: String? = null
//        @OneToMany(mappedBy = "user", targetEntity = RoleEntity::class)
//        var roles : Set<RoleEntity>
) : BaseEntity(id) {

    fun updatePassword(newPassword : String) {
        password = newPassword
    }
    fun readPassword() : String? =
            password

/*
    fun toUserDetails(): KirraUserDetails =
            KirraUserDetails(this.id!!, this.username!!, this.readPassword()!!, this.authorities())

    fun authorities(): List<GrantedAuthority> =
            listOf()
*/

}

@Service
open class RoleService {
    @Autowired
    lateinit var kirraJavaApplication: KirraJavaApplication

    @Autowired
    lateinit var kirraRepositoryRegistry: KirraRepositoryRegistry

    fun findAuthorities(user : ApplicationUser): List<GrantedAuthority> =
            findRoleObjects(user).map { it.getRole().toGrantedAuthority() }

    fun findRoleObjects(user : ApplicationUser): List<RoleEntity> =
            kirraJavaApplication.applicationUserRoles.map { asRole<RoleEntity>(user, it) }.filterNotNull()

    fun <RE: RoleEntity> asRole(user : ApplicationUser, role : UserRole) : RE? {
        val roleRepository = kirraRepositoryRegistry.getRepository<RoleEntity>(role.roleName()) as? RoleRepository<RE>
        return roleRepository?.findByUser(user)
    }

    fun toUserDetails(user : ApplicationUser): KirraUserDetails =
            KirraUserDetails(user.id!!, user.username!!, user.readPassword()!!, findAuthorities(user))
}


@Service
open class ApplicationUserService : BaseService<ApplicationUser, ApplicationUserRepository>(ApplicationUser::class) {

    fun findUserByUsername(username: String) = repository.findByUsername(username)

    fun findUserByUsernameAndPassword(username: String, password: String) = repository.findByUsernameAndPassword(username, password)

    override fun update(toUpdate: ApplicationUser): ApplicationUser? {
        val existingInstance = repository.findById(toUpdate.id)
        if (existingInstance.isPresent) {
            if (toUpdate.readPassword() != null) {
                existingInstance.get().updatePassword(toUpdate.readPassword()!!)
                return repository.save(existingInstance.get())
            }
        }
        return existingInstance.get()
    }

    override fun create(toCreate: ApplicationUser) : ApplicationUser {
        BusinessException.ensure(toCreate.readPassword() != null, ErrorCode.INVALID_OR_MISSING_DATA, "password")
        return super.create(toCreate)
    }
}

@Repository
interface ApplicationUserRepository : BaseRepository<ApplicationUser> {
    fun findByUsername(username: String): ApplicationUser?
    fun findByUsernameAndPassword(username: String, password : String): ApplicationUser?
}

