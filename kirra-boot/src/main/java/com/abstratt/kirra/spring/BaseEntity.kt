package com.abstratt.kirra.spring

import com.abstratt.kirra.rest.common.KirraContext
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist

@MappedSuperclass
abstract class BaseEntity(
    @Id @GeneratedValue open var id: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        val basicCheck = super.equals(other)
        if (basicCheck) {
            return true
        }
        return this::class.isInstance(other) && (other as BaseEntity).id == id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: super.hashCode()
    }
}
