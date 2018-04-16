package com.abstratt.kirra.spring.userprofile

import com.abstratt.kirra.spring.AccessControl
import com.abstratt.kirra.spring.BaseEntity
import com.abstratt.kirra.spring.user.RoleEntity
import javax.persistence.Entity

@Entity
class UserProfile(
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