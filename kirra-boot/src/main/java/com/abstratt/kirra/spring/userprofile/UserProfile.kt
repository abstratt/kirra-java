package com.abstratt.kirra.spring.userprofile

import com.abstratt.kirra.pojo.IUserProfile
import com.abstratt.kirra.spring.*
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import javax.persistence.Entity

interface UserProfileMarker

@Entity
class UserProfile(
        id: Long? = null,
        var username: String? = null,
        var password: String? = null
//        @OneToMany(mappedBy = "user", targetEntity = RoleEntity::class)
//        var roles : Set<RoleEntity>
) : BaseEntity(id), IUserProfile {

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
@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
open class UserProfileService : BaseService<UserProfile, UserProfileRepository>(UserProfile::class) {

    fun findUserByUsername(username: String) = repository.findByUsername(username)

    fun findUserByUsernameAndPassword(username: String, password: String) = repository.findByUsernameAndPassword(username, password)

    @Transactional
    override fun update(toUpdate: UserProfile): UserProfile? {
        val existingInstance = repository.findById(toUpdate.id!!)
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
