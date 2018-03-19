package com.abstratt.kirra.spring.user

import com.abstratt.kirra.spring.*
import org.springframework.data.jpa.repository.Query
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import javax.persistence.Entity

@Target(AnnotationTarget.CLASS)
annotation class Role

interface RoleEntity {
    var user : ApplicationUser?
    fun getRole() : UserRole
}

interface UserRole {
    fun toAuthority() : String = "ROLE_${(this as Enum<*>).name}"
}

interface RoleRepository<E : RoleEntity> {
    fun findByApplicationUserUsername(username : String)
}

@Entity
class ApplicationUser(
        id: Long? = null,
        var username: String? = null,
        var password: String? = null
) : BaseEntity(id) {
    fun updatePassword(newPassword : String) {
        password = newPassword
    }
    fun readPassword() : String? =
            password

    fun toUserDetails(): KirraUserDetails =
            KirraUserDetails(this.id!!, this.username!!, this.readPassword()!!, this.authorities())

    fun authorities(): List<GrantedAuthority> =
            emptyList()

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

